/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.transaction

import connectors.StampDutyLandTaxConnector
import controllers.actions.*
import models.{CheckMode, Lease, ReturnVersionUpdateRequest, Transaction, UserAnswers}
import models.land.LandTypeOfProperty
import models.lease.{CreateLeaseRequest, DeleteLeaseRequest, LeaseSessionQuestions}
import models.prelimQuestions.TransactionType
import models.prelimQuestions.TransactionType.GrantOfLease
import models.transaction.{ReasonForRelief, TransactionSessionQuestions, UpdateTransactionRequest}
import pages.transaction.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, JsSuccess}
import play.api.mvc.*
import repositories.SessionRepository
import services.checkAnswers.CheckAnswersService
import services.transaction.PopulateTransactionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.summary.SummaryRowResult
import viewmodels.checkAnswers.transaction.*
import views.html.transaction.TransactionCheckYourAnswersView
import services.crossflow.fields.CrossFlowValidationService
import services.crossflow.*
import services.taxCalculation.UpdateTaxCalcService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.util.control.NonFatal

@Singleton
class TransactionCheckYourAnswersController @Inject()(
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  statusCheck: CheckSubmissionStatusAction,
  sessionRepository: SessionRepository,
  checkAnswersService: CheckAnswersService,
  backendConnector: StampDutyLandTaxConnector,
  populateTransactionService: PopulateTransactionService,
  crossFlow: CrossFlowValidationService,
  val controllerComponents: MessagesControllerComponents,
  view: TransactionCheckYourAnswersView,
  updateTaxCalcService: UpdateTaxCalcService
)(implicit ex: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData andThen statusCheck).async {
    implicit request =>
      sessionRepository.get(request.userAnswers.id).flatMap {
        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

        case Some(userAnswers) =>
          if (userAnswers.returnId.isEmpty) {
            Future.successful(Redirect(controllers.routes.ReturnTaskListController.onPageLoad()))
          } else {
            val isTransactionDataEmpty =
              (userAnswers.data \ "transactionCurrent").asOpt[JsObject].forall(_.values.isEmpty)

            if (isTransactionDataEmpty) {
              populateFromTransaction(userAnswers)
            } else {
              Future.successful(renderOrRedirect(userAnswers))
            }
          }
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData andThen statusCheck).async { implicit request =>

    sessionRepository.get(request.userAnswers.id).flatMap {
      case Some(userAnswers) =>
        (userAnswers.data \ "transactionCurrent").validate[TransactionSessionQuestions] match {
          case JsSuccess(sessionData, _) => updateTransaction(userAnswers)
          case _ =>
            Future.successful(
              Redirect(controllers.transaction.routes.TransactionCheckYourAnswersController.onPageLoad())
            )
        }
      case None =>
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  private def handleLeaseDecision(userAnswers: UserAnswers)(implicit hc: HeaderCarrier, request: Request[_]): Future[Unit] = {
    val isLeaseDefined = userAnswers.fullReturn.flatMap(_.lease).isDefined
    val transactionType = userAnswers.get(TypeOfTransactionPage)

    val leaseDecision: String = transactionType match {
      case Some(GrantOfLease) if isLeaseDefined => "noAction"
      case Some(GrantOfLease) if !isLeaseDefined => "createLease"
      case Some(_) if isLeaseDefined => "deleteLease"
      case _ => "noAction"
    }

    leaseDecision match {
      case "createLease" =>
        (userAnswers.data \ "leaseCurrent").validate[LeaseSessionQuestions] match {
          case JsSuccess(_, _) =>
            for {
              lease <- Lease.from(userAnswers)
              createLeaseRequest <- CreateLeaseRequest.from(userAnswers, lease)
              _ <- backendConnector.createLease(createLeaseRequest)
            } yield ()
          case _ => Future.unit
        }

      case "deleteLease" =>
        for {
          req <- DeleteLeaseRequest.from(userAnswers)
          _ <- backendConnector.deleteLease(req)
        } yield ()

      case _ => Future.unit
    }
  }

  private def updateTransaction(userAnswers: UserAnswers)(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] = {
    for {
      transaction <- Transaction.from(userAnswers)
      updateReturnVersionRequest <- ReturnVersionUpdateRequest.from(userAnswers)
      versionResult <-
        backendConnector
          .updateReturnVersion(updateReturnVersionRequest)
          .map(Right(_))
          .recover { case NonFatal(_) =>
            Left(Redirect(controllers.routes.UpdateReturnVersionErrorController.onPageLoad()))
          }
      result <- versionResult match {
        case Left(errorRedirect) =>
          Future.successful(errorRedirect)

        case Right(updateReturnVersionReturn) =>
          for {
            _ <- handleLeaseDecision(userAnswers)
            outcome <-
              if (updateReturnVersionReturn.newVersion.isDefined) {
                for {
                  updateTransactionRequest <- UpdateTransactionRequest.from(userAnswers, transaction)
                  updateTransactionReturn <- backendConnector.updateTransaction(updateTransactionRequest)
                  _ <- maybeUpdateTransactionTaxCalc(userAnswers)
                } yield
                  if (updateTransactionReturn.updated) {
                    Redirect(controllers.routes.ReturnTaskListController.onPageLoad())
                  } else {
                    Redirect(controllers.transaction.routes.TransactionCheckYourAnswersController.onPageLoad())
                  }
              } else {
                Future.successful(
                  Redirect(controllers.transaction.routes.TransactionCheckYourAnswersController.onPageLoad())
                )
              }
          } yield outcome
      }
    } yield result
  }
  
  private def maybeUpdateTransactionTaxCalc(userAnswers: UserAnswers)(implicit hc: HeaderCarrier, request: Request[_]): Future[Unit] =
    if (updateTaxCalcService.transactionDataMatches(userAnswers)) {
      for {
        req <- updateTaxCalcService.updateTaxCalcRequest(userAnswers)
        _ <- backendConnector.updateTaxCalculationInfo(req)
      } yield ()
    } else {
      Future.successful(())
    }
    
  private def populateFromTransaction(userAnswers: UserAnswers)(implicit request: Request[_]): Future[Result] =
    userAnswers.fullReturn.flatMap(_.transaction) match {
      case None =>
        Future.successful(Redirect(controllers.transaction.routes.TransactionBeforeYouStartController.onPageLoad()))
      case Some(transaction) =>
        populateTransactionService.populateTransactionInSession(transaction, userAnswers) match {
          case Success(populated) =>
            sessionRepository.set(populated).map(_ => renderOrRedirect(populated))
          case Failure(_) =>
            Future.successful(Redirect(controllers.transaction.routes.TransactionBeforeYouStartController.onPageLoad()))
        }
    }

  private def renderOrRedirect(ua: UserAnswers)(implicit request: Request[_]): Result =
    checkAnswersService.redirectOrRender(buildRowResults(ua)) match {
      case Left(call) => Redirect(call)
      case Right(summaryList) => Ok(view(summaryList))
    }

  private def callForFailure(failure: CrossFlowFailure): Call =
    failure.targets.headOption.map(_.page) match {
      case Some(Pages.EffectiveDate) => controllers.transaction.routes.TransactionEffectiveDateController.onPageLoad(CheckMode)
      case Some(Pages.ContractDate) => controllers.transaction.routes.TransactionDateOfContractController.onPageLoad(CheckMode)
      case Some(Pages.ReliefReason) => controllers.transaction.routes.ReasonForReliefController.onPageLoad(CheckMode)
      case Some(Pages.UseOfProperty) => controllers.transaction.routes.TransactionUseOfLandOrPropertyController.onPageLoad(CheckMode)
      case _ => controllers.transaction.routes.TransactionCheckYourAnswersController.onPageLoad()
    }

  private def buildRowResults(ua: UserAnswers)(implicit request: Request[_]): Seq[SummaryRowResult] = {
    val failures = crossFlow.failuresAffecting(ReturnSection.Transaction, ua)

    val crossFlowMissing: Seq[SummaryRowResult] =
      failures.map(f => SummaryRowResult.Missing(callForFailure(f)))

    crossFlowMissing ++ Seq(
      Some(TypeOfTransactionSummary.row(ua)),
      Some(TransactionEffectiveDateSummary.row(ua)),
      Some(TransactionAddDateOfContractSummary.row(ua)),
      TransactionDateOfContractSummary.row(ua),
      Option.when(!isGrantOfLease(ua))(TotalConsiderationOfTransactionSummary.row(ua)),
      Option.when(!isGrantOfLease(ua))(TransactionVatIncludedSummary.row(ua)),
      if (!isGrantOfLease(ua)) TransactionVatAmountSummary.row(ua) else None,
      Option.when(!isGrantOfLease(ua))(TransactionFormsOfConsiderationSummary.row(ua)),
      Some(TransactionLinkedTransactionsSummary.row(ua)),
      TotalConsiderationOfLinkedTransactionSummary.row(ua),
      Some(PurchaserEligibleToClaimReliefSummary.row(ua)),
      ReasonForReliefSummary.row(ua),
      Option.when(isPartExchange(ua))(IsPurchaserRegisteredWithCISSummary.row(ua)),
      if (cisCheck(ua) && isPartExchange(ua)) TransactionCisNumberSummary.row(ua) else None,
      AddRegisteredCharityNumberSummary.row(ua),
      CharityRegisteredNumberSummary.row(ua),
      TransactionPartialReliefSummary.row(ua),
      ClaimingPartialReliefAmountSummary.row(ua),
      Some(ConsiderationsAffectedUncertainSummary.row(ua)),
      if (considerationsAffectedUncertainCheck(ua)) Some(TransactionDeferringPaymentSummary.row(ua)) else None,
      if (propertyTypeCheck(ua)) Some(TransactionUseOfLandOrPropertySummary.row(ua)) else None,
      Some(SaleOfBusinessSummary.row(ua)),
      if (saleOfBusinessCheck(ua)) Some(TransactionSaleOfBusinessAssetsSummary.row(ua)) else None,
      if (saleOfBusinessCheck(ua)) Some(TotalAssetsConsiderationSummary.row(ua)) else None,
      Some(Cap1OrNsbcSummary.row(ua)),
      if (cap1Check(ua)) Some(TransactionRulingFollowedSummary.row(ua)) else None,
      Some(TransactionRestrictionsCovenantsAndConditionsSummary.row(ua)),
      if (restrictionsCheck(ua)) Some(DescriptionOfRestrictionsSummary.row(ua)) else None,
      Some(IsLandOrPropertyExchangedSummary.row(ua)),
      if (landExchangedCheck(ua)) Some(TransactionAddressSummary.row(ua)) else None,
      Some(TransactionExercisingAnOptionSummary.row(ua))
    ).flatten
  }

  private def isGrantOfLease(ua: UserAnswers): Boolean =
    ua.get(TypeOfTransactionPage).contains(TransactionType.GrantOfLease)

  private def isPartExchange(ua: UserAnswers): Boolean =
    ua.get(ReasonForReliefPage).contains(ReasonForRelief.PartExchange)

  private def cisCheck(ua: UserAnswers): Boolean =
    ua.get(IsPurchaserRegisteredWithCISPage).contains(true)

  private def propertyTypeCheck(ua: UserAnswers): Boolean =
    val mainLandId = ua.fullReturn.flatMap(_.returnInfo).flatMap(_.mainLandID)
    val typeOfProperty = ua.fullReturn.flatMap(_.land).flatMap(_.find(l => l.landID == mainLandId)).flatMap(_.propertyType)

    typeOfProperty match {
      case Some(LandTypeOfProperty.Mixed.toString | LandTypeOfProperty.NonResidential.toString) => true
      case _ => false
    }

  private def saleOfBusinessCheck(ua: UserAnswers): Boolean =
    ua.get(SaleOfBusinessPage).contains(true)

  private def cap1Check(ua: UserAnswers): Boolean =
    ua.get(Cap1OrNsbcPage).contains(true)

  private def restrictionsCheck(ua: UserAnswers): Boolean =
    ua.get(TransactionRestrictionsCovenantsAndConditionsPage).contains(true)

  private def landExchangedCheck(ua: UserAnswers): Boolean =
    ua.get(IsLandOrPropertyExchangedPage).contains(true)
    
  private def considerationsAffectedUncertainCheck(ua: UserAnswers): Boolean =
    ua.get(ConsiderationsAffectedUncertainPage).contains(true)

}
