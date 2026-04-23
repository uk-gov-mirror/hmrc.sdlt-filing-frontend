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

package controllers.purchaserAgent

import connectors.StampDutyLandTaxConnector
import controllers.actions.*
import models.AgentType.Purchaser
import models.purchaserAgent.PurchaserAgentSessionQuestions
import models.{CreateReturnAgentRequest, NormalMode, ReturnVersionUpdateRequest, UpdateReturnAgentRequest, UserAnswers}
import pages.purchaserAgent.PurchaserAgentOverviewPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsSuccess
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import repositories.SessionRepository
import services.purchaserAgent.PurchaserAgentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.purchaserAgent.*
import views.html.purchaserAgent.PurchaserAgentCheckYourAnswersView
import uk.gov.hmrc.http.HeaderCarrier
import services.checkAnswers.CheckAnswersService
import viewmodels.checkAnswers.summary.SummaryRowResult

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PurchaserAgentCheckYourAnswersController @Inject()(
                                                          override val messagesApi: MessagesApi,
                                                          identify: IdentifierAction,
                                                          getData: DataRetrievalAction,
                                                          requireData: DataRequiredAction,
                                                          sessionRepository: SessionRepository,
                                                          backendConnector: StampDutyLandTaxConnector,
                                                          val controllerComponents: MessagesControllerComponents,
                                                          view: PurchaserAgentCheckYourAnswersView,
                                                          purchaserAgentService: PurchaserAgentService,
                                                          checkAnswersService: CheckAnswersService
                                                        )(implicit ex: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      for {
        result <- sessionRepository.get(request.userAnswers.id)
      } yield {

        val isDataEmpty = result.exists(_.data.value.isEmpty)

        if (isDataEmpty) {
          Redirect(controllers.purchaserAgent.routes.PurchaserAgentBeforeYouStartController.onPageLoad(NormalMode))
        } else {
          val rowResults = Seq(
            Some(PurchaserAgentNameSummary.row(request.userAnswers)),
            Some(PurchaserAgentAddressSummary.row(request.userAnswers)),
            Some(AddContactDetailsForPurchaserAgentSummary.row(request.userAnswers)),
            PurchaserAgentsContactDetailsSummary.row(request.userAnswers),
            Some(AddPurchaserAgentReferenceNumberSummary.row(request.userAnswers)),
            PurchaserAgentReferenceSummary.row(request.userAnswers),
            Some(PurchaserAgentAuthorisedSummary.row(request.userAnswers))
          ).flatten

          checkAnswersService.redirectOrRender(rowResults) match {
            case Left(call) => Redirect(call)
            case Right(summaryList) => Ok(view(summaryList))
          }
        }
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>

    sessionRepository.get(request.userAnswers.id).flatMap {
      case Some(userAnswers) =>
        (userAnswers.data \ "purchaserAgentCurrent").validate[PurchaserAgentSessionQuestions] match {
          case JsSuccess(sessionData, _) if purchaserAgentService.purchaserAgentSessionQuestionsValidation(sessionData) =>
            request.userAnswers.get(PurchaserAgentOverviewPage).map { returnAgentId =>
              updateReturnAgent(userAnswers)
            }.getOrElse(createReturnAgent(userAnswers))

          case _ =>
            Future.successful(
              Redirect(controllers.purchaserAgent.routes.PurchaserAgentCheckYourAnswersController.onPageLoad())
            )
        }

      case None =>
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  private def updateReturnAgent(userAnswers: UserAnswers)(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] = {
    for {
      updateRequest <- ReturnVersionUpdateRequest.from(userAnswers)
      version <- backendConnector.updateReturnVersion(updateRequest)
      updateReturnAgentRequest <- UpdateReturnAgentRequest.from(userAnswers, Purchaser) if version.newVersion.isDefined
      updateReturnAgentReturn <- backendConnector.updateReturnAgent(updateReturnAgentRequest) if version.newVersion.isDefined
    } yield {
      if (updateReturnAgentReturn.updated) {
        Redirect(controllers.purchaserAgent.routes.PurchaserAgentOverviewController.onPageLoad())
          .flashing("purchaserAgentUpdated" -> updateReturnAgentRequest.name)
      } else {
        Redirect(controllers.purchaserAgent.routes.PurchaserAgentCheckYourAnswersController.onPageLoad())
      }
    }
  }

  private def createReturnAgent(userAnswers: UserAnswers)(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] = {
    for {
      createReturnAgentRequest <- CreateReturnAgentRequest.from(userAnswers, Purchaser)
      createReturnAgentReturn <- backendConnector.createReturnAgent(createReturnAgentRequest)
    } yield {
      if (createReturnAgentReturn.returnAgentID.nonEmpty) {
        Redirect(controllers.purchaserAgent.routes.PurchaserAgentOverviewController.onPageLoad())
          .flashing("purchaserAgentCreated" -> createReturnAgentRequest.name)
      } else {
        Redirect(controllers.purchaserAgent.routes.PurchaserAgentCheckYourAnswersController.onPageLoad())
      }
    }
  }
}
