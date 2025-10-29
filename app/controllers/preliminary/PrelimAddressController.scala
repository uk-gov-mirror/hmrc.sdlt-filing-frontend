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

package controllers.preliminary

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import controllers.routes
import models.address.AddressLookupJourneyIdentifier.prelimQuestionsAddress
import models.address.MandatoryFieldsConfigModel
import models.{Mode, NormalMode}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AddressLookupService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PrelimAddressController @Inject() (
                                  val controllerComponents: MessagesControllerComponents,
                                  identify: IdentifierAction,
                                  getData: DataRetrievalAction,
                                  requireData: DataRequiredAction,
                                  addressLookupService: AddressLookupService
                                )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def redirectToAddressLookup(mode: Mode, changeRoute: Option[String] = None): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val journeyId = prelimQuestionsAddress
      val addressConfig = MandatoryFieldsConfigModel(line1 = Some(true), town = Some(true), postcode = Some(true))

      if(changeRoute.isDefined) {
        addressLookupService.getJourneyUrl(journeyId,
          controllers.preliminary.routes.PrelimAddressController.addressLookupCallbackChange(),
          useUkMode = true,
          mandatoryFieldsConfigModel = addressConfig) map Redirect
      } else {
        addressLookupService.getJourneyUrl(journeyId,
          controllers.preliminary.routes.PrelimAddressController.addressLookupCallback(),
          useUkMode = true,
          mandatoryFieldsConfigModel = addressConfig) map Redirect
      }
  }

  def addressLookupCallback(id: String, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
        for {
          address <- addressLookupService.getAddressById(id)
          updated <- addressLookupService.saveAddressDetails(address)
        } yield if(updated) Redirect(routes.TransactionTypeController.onPageLoad(NormalMode)) else Redirect(routes.JourneyRecoveryController.onPageLoad())
  }

  def addressLookupCallbackChange(id: String, mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      for {
        address <- addressLookupService.getAddressById(id)
        updated <- addressLookupService.saveAddressDetails(address)
      } yield if(updated) Redirect(routes.CheckYourAnswersController.onPageLoad()) else Redirect(routes.JourneyRecoveryController.onPageLoad())
  }
  
  
  
  
}
