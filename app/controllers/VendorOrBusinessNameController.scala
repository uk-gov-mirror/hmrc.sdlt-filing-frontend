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

import controllers.actions.*
import forms.VendorOrBusinessNameFormProvider

import javax.inject.Inject
import models.{Mode, VendorName}
import navigation.Navigator
import pages.VendorOrBusinessNamePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.VendorOrBusinessNameView

import scala.concurrent.{ExecutionContext, Future}

class VendorOrBusinessNameController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: VendorOrBusinessNameFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: VendorOrBusinessNameView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      
//      val vendorOrBusiness: String = request.userAnswers.get(VendorIsIndividualPage) match {
//        case Some(value) => if (value.toString == "Individual") "Individual" else "Business"
//        case _ => ""
//      }
      
      val vendorOrBusiness: String = "Individual"

      val preparedForm = request.userAnswers.get(VendorOrBusinessNamePage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, vendorOrBusiness))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val vendorOrBusiness: String = "Individual"

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, vendorOrBusiness))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(VendorOrBusinessNamePage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield {
            Redirect(navigator.nextPage(VendorOrBusinessNamePage, mode, updatedAnswers))
          }
      )
  }
}
