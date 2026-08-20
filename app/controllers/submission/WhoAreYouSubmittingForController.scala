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
import forms.submission.WhoAreYouSubmittingForFormProvider
import models.Mode
import models.submission.WhoAreYouSubmittingFor
import navigation.Navigator
import pages.submission.WhoAreYouSubmittingForPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.tasklist.SubmissionTaskList
import views.html.submission.WhoAreYouSubmittingForView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhoAreYouSubmittingForController @Inject()(
                                                  override val messagesApi: MessagesApi,
                                                  sessionRepository: SessionRepository,
                                                  navigator: Navigator,
                                                  activatedIdentify: ActivatedIdentifierAction,
                                                  getData: DataRetrievalAction,
                                                  requireData: DataRequiredAction,
                                                  resubmissionCheck: ResubmissionCheckAction,
                                                  crossFlowCheck: CrossFlowCheckAction,
                                                  formProvider: WhoAreYouSubmittingForFormProvider,
                                                  val controllerComponents: MessagesControllerComponents,
                                                  view: WhoAreYouSubmittingForView
                                                )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[WhoAreYouSubmittingFor] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (activatedIdentify andThen getData andThen requireData andThen resubmissionCheck andThen crossFlowCheck) {
    implicit request =>

      val submissionAlreadyStarted = request.userAnswers.fullReturn.exists(_.submission.isDefined)

      if (!submissionAlreadyStarted && !request.userAnswers.fullReturn.exists(SubmissionTaskList.canStartSubmission)) {
        Redirect(controllers.routes.ReturnTaskListController.onPageLoad())
      } else {
        val preparedForm = request.userAnswers.get(WhoAreYouSubmittingForPage) match {
          case None        => form
          case Some(value) => form.fill(value)
        }

        Ok(view(preparedForm, mode))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (activatedIdentify andThen getData andThen requireData andThen resubmissionCheck andThen crossFlowCheck).async {
    implicit request =>

      val submissionAlreadyStarted = request.userAnswers.fullReturn.exists(_.submission.isDefined)

      if (!submissionAlreadyStarted && !request.userAnswers.fullReturn.exists(SubmissionTaskList.canStartSubmission)) {
        Future.successful(Redirect(controllers.routes.ReturnTaskListController.onPageLoad()))
      } else {
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, mode))),

          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(WhoAreYouSubmittingForPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(WhoAreYouSubmittingForPage, mode, updatedAnswers))
        )
      }
  }
}
