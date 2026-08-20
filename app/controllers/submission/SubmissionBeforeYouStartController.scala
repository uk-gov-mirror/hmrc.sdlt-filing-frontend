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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.tasklist.SubmissionTaskList
import views.html.submission.SubmissionBeforeYouStartView

import javax.inject.Inject

class SubmissionBeforeYouStartController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       activatedIdentify: ActivatedIdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       resubmissionCheck: ResubmissionCheckAction,
                                       crossFlowCheck: CrossFlowCheckAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: SubmissionBeforeYouStartView
                                     ) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (activatedIdentify andThen getData andThen requireData andThen resubmissionCheck andThen crossFlowCheck) {
    implicit request =>
      val submissionAlreadyStarted = request.userAnswers.fullReturn.exists(_.submission.isDefined)

      if (!submissionAlreadyStarted && !request.userAnswers.fullReturn.exists(SubmissionTaskList.canStartSubmission)) {
        Redirect(controllers.routes.ReturnTaskListController.onPageLoad())
      } else {
        Ok(view())
      }
  }
}
