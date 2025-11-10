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

import controllers.actions._
import forms.VendorRepresentedByAgentFormProvider
import javax.inject.Inject
import models._
import navigation.Navigator
import pages.VendorRepresentedByAgentPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.VendorRepresentedByAgentView

import scala.concurrent.{ExecutionContext, Future}

class VendorRepresentedByAgentController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: SessionRepository,
                                         navigator: Navigator,
                                         identify: IdentifierAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: VendorRepresentedByAgentFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: VendorRepresentedByAgentView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  //TODO: Replace with VendorCurrentNamePage when implemented (double check getOrElse message)
  //val vendorName: String = request.userAnswers.get(VendorCurrentNamePage).getOrElse("the vendor")
  val vendorNameStub = "TODO the vendor"

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(VendorRepresentedByAgentPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }
      
      request.userAnswers.fullReturn match {

        case Some(fullReturn) =>

          val hasVendorReturnAgent: Boolean = fullReturn.returnAgent.exists(_.exists(_.agentType.contains("VENDOR")))
          val mainVendorId: Option[String] = fullReturn.returnInfo.flatMap(_.mainVendorID)
          val mainVendor: Option[Vendor] = for {
            id <- mainVendorId
            vendors <- fullReturn.vendor
            mainVendor <- vendors.find(_.vendorID.contains(id))
          } yield mainVendor

          val mainVendorRepresented: Boolean = {
            mainVendor.exists(vendor => vendor.isRepresentedByAgent.exists(v => v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes")))
          }

          if(!hasVendorReturnAgent) {
            Ok(view(preparedForm, mode, vendorNameStub))
          } else if (mainVendorRepresented) {
            //TODO: Update to check your answers when page created
            Redirect(routes.IndexController.onPageLoad())
          } else {
            Redirect(routes.VendorRepresentedByAgentErrorController.onPageLoad())
          }

        case _ =>
          Redirect(routes.VendorRepresentedByAgentErrorController.onPageLoad())
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          //TODO: Replace with VendorCurrentNamePage when implemented
          Future.successful(BadRequest(view(formWithErrors, mode, vendorName = vendorNameStub))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(VendorRepresentedByAgentPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield {
            if(value) {
              //TODO: Update to Vendor Agent's name page when created
              Redirect(navigator.nextPage(VendorRepresentedByAgentPage, mode, updatedAnswers))
            } else {
              //TODO: Update to new CYA page when created
              Redirect(routes.IndexController.onPageLoad())
            }

          }
      )
  }
}
