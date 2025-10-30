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

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.{PrelimReturn, SessionUserData}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsFalse, JsResult, Json, JsSuccess}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.{PrelimAddressSummary, PurchaserIsIndividualSummary, PurchaserSurnameOrCompanyNameSummary, TransactionTypeSummary}
import viewmodels.govuk.summarylist.*
import views.html.CheckYourAnswersView

import scala.concurrent.ExecutionContext

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            sessionRepository: SessionRepository,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView
                                          )(implicit ex: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      for {
        result <- sessionRepository.get(request.userAnswers.id)
      } yield {

        val isDataEmpty = result.exists(_.data.value.isEmpty)

        if (isDataEmpty) {
          Redirect(controllers.routes.BeforeStartReturnController.onPageLoad())
        } else {
          val summaryList = SummaryListViewModel(
            rows = Seq(
              PurchaserIsIndividualSummary.row(result),
              PurchaserSurnameOrCompanyNameSummary.row(result),
              PrelimAddressSummary.row(result),
              TransactionTypeSummary.row(result)
            )
          )

          Ok(view(summaryList))
        }
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      for {
        result <- sessionRepository.get(request.userAnswers.id)
      } yield {

        val isRequiredDataPresent:Boolean =
          result.get.data.validate[SessionUserData] match {
            case JsSuccess(value, _) => true
            case JsError(_) => false
        }
        
        if (isRequiredDataPresent) {
          val prelimReturn = PrelimReturn.from(result)
          
          Redirect(controllers.routes.ReturnTaskListController.onPageLoad())
          
        } else {
          Redirect(controllers.routes.CheckYourAnswersController.onPageLoad())
        }
      }

  }
}
