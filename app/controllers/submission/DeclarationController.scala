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
package controllers.submission

import controllers.actions.*
import models.Mode
import pages.submission.WhoAreYouSubmittingForPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.crossflow.fields.CrossFlowValidationService
import services.submission.ChrisSubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.tasklist.SubmissionTaskList
import views.html.submission.DeclarationView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class DeclarationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       activatedIdentify: ActivatedIdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       resubmissionCheck: ResubmissionCheckAction,
                                       chrisSubmissionService: ChrisSubmissionService,
                                       implicit val crossFlowValidationService: CrossFlowValidationService,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: DeclarationView
                                     ) extends FrontendBaseController with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (activatedIdentify andThen getData andThen requireData andThen resubmissionCheck) {
    implicit request =>

      val submissionAlreadyStarted = request.userAnswers.fullReturn.exists(_.submission.isDefined)
      implicit val userAnswers = request.userAnswers

      if (!submissionAlreadyStarted && !request.userAnswers.fullReturn.exists(SubmissionTaskList.canStartSubmission)) {
        Redirect(controllers.routes.ReturnTaskListController.onPageLoad())
      } else {
        request.userAnswers.get(WhoAreYouSubmittingForPage) match {

          case Some(declarationFor) => Ok(view(declarationFor.toString, mode))
          case None =>
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
      }

  }

  def onSubmit(): Action[AnyContent] = (activatedIdentify andThen getData andThen requireData andThen resubmissionCheck).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val submissionAlreadyStarted = request.userAnswers.fullReturn.exists(_.submission.isDefined)

      if (!submissionAlreadyStarted && !request.userAnswers.fullReturn.exists(SubmissionTaskList.canStartSubmission)) {
        Future.successful(Redirect(controllers.routes.ReturnTaskListController.onPageLoad()))
      } else {
        request.userAnswers.get(WhoAreYouSubmittingForPage) match {

          case Some(_) =>
            chrisSubmissionService.submitInBackground(request.userAnswers)
            Future.successful(Redirect(controllers.submission.routes.LoadingScreenController.show))

          case None =>
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
      }

  }
}