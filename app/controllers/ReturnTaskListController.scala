/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers

import config.FrontendAppConfig
import controllers.actions.*
import models.{GetReturnByRefRequest, UserAnswers}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.FullReturnService
import services.crossflow.fields.CrossFlowValidationService
import services.crossflow.{ReturnSection, SectionStatus}
import services.land.LandService
import services.pdf.PDFGenerationService
import services.purchaser.PurchaserService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{FullName, LeaseHelper, PropertyTypeHelper}
import viewmodels.tasklist.*
import views.html.ReturnTaskListView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnTaskListController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          identify: IdentifierAction,
                                          activatedIdentify: ActivatedIdentifierAction,
                                          fullReturnService: FullReturnService,
                                          getData: DataRetrievalAction,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: ReturnTaskListView,
                                          sessionRepository: SessionRepository,
                                          pdfGenerationService: PDFGenerationService,
                                          crossFlowService: CrossFlowValidationService,
                                          purchaserService: PurchaserService,
                                          landService: LandService
                                        )(implicit ec: ExecutionContext, frontendAppConfig: FrontendAppConfig)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(returnId: Option[String] = None): Action[AnyContent] = (identify andThen getData).async {
    implicit request =>
      val effectiveReturnId = returnId.orElse(request.userAnswers.flatMap(_.returnId))
      effectiveReturnId.fold(
        Future.successful(Redirect(controllers.routes.NoReturnReferenceController.onPageLoad()))
      ) { id =>
        for {
          fullReturn  <- fullReturnService.getFullReturn(GetReturnByRefRequest(returnResourceRef = id, storn = request.storn))
          userAnswers  = UserAnswers(id = request.userId, returnId = Some(id), fullReturn = Some(fullReturn), storn = request.storn)
          _           <- sessionRepository.set(userAnswers)
        } yield {
          val maybeSubmissionObject = userAnswers.fullReturn.flatMap(_.submission)
          val purchaserName: Option[String] = {
            purchaserService.getMainPurchaser(userAnswers).flatMap { purchaser => purchaser.companyName.orElse(
              FullName.optionalFullName(purchaser.forename1, purchaser.forename2, purchaser.surname))
            }
          }
          val landAddress1: Option[String] = landService.getMainLand(userAnswers).flatMap(_.address1)

          if(maybeSubmissionObject.isDefined) {
            Redirect(controllers.submission.routes.SubmissionBeforeYouStartController.onPageLoad())
          } else {
            val statuses = crossFlowService.sectionStatuses(userAnswers)
            val transactionStatus = statuses.getOrElse(ReturnSection.Transaction,
              SectionStatus(ReturnSection.Transaction, false, Nil, Nil, Nil))
            val landStatus = statuses.getOrElse(ReturnSection.Land,
              SectionStatus(ReturnSection.Land, false, Nil, Nil, Nil))
            val leaseStatus = statuses.getOrElse(ReturnSection.Lease,
              SectionStatus(ReturnSection.Lease, false, Nil, Nil, Nil))

            val sections = List(
              Some(PurchaserTaskList.build(fullReturn)),
              if(PurchaserTaskList.isPurchaserComplete(fullReturn)) Some(PurchaserAgentTaskList.build(fullReturn)) else None,
              Some(VendorTaskList.build(fullReturn)),
              if(VendorTaskList.isVendorComplete(fullReturn)) Some(VendorAgentTaskList.build(fullReturn)) else None,
              Some(LandTaskList.build(fullReturn, landStatus)),
              if (PropertyTypeHelper.isResidentialProperty(fullReturn)) Some(UkResidencyTaskList.build(fullReturn)) else None,
              Some(TransactionTaskList.build(fullReturn, transactionStatus)),
              if (LeaseHelper.isLeaseType(fullReturn)) Some(LeaseTaskList.build(fullReturn, leaseStatus)) else None,
              Some(TaxCalculationTaskList.build(fullReturn)),
              Some(SubmissionTaskList.build(fullReturn, landStatus, transactionStatus, leaseStatus))
            ).flatten

            Ok(view(purchaserName, landAddress1, sections: _*))
          }
        }
      }
  }

  def downloadPdf: Action[AnyContent] = (activatedIdentify andThen getData).async {
    implicit request =>
      request.userAnswers.flatMap(_.returnId) match {
        case None =>
          Future.successful(Redirect(controllers.routes.NoReturnReferenceController.onPageLoad()))

        case Some(id) =>
          fullReturnService.getFullReturn(GetReturnByRefRequest(returnResourceRef = id, storn = request.storn))
            .flatMap { fullReturn =>
              val returnId = fullReturn.returnInfo.flatMap(_.returnID).getOrElse("sdlt-return")
              pdfGenerationService.generatePdf(fullReturn)
                .map { pdfBytes =>
                  Ok(pdfBytes)
                    .as("application/pdf")
                    .withHeaders(
                      "Content-Disposition" -> s"""attachment; filename="sdlt-return-$returnId.pdf"""",
                      "Content-Length"      -> pdfBytes.length.toString,
                      "Cache-Control"       -> "",
                      "Pragma"              -> ""
                    )
                }
            }
            .recover {
              case ex =>
                InternalServerError(s"Failed to generate PDF: ${ex.getMessage}")
            }
      }
  }
}