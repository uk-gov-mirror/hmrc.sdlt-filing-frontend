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

package controllers.vendor

import connectors.StubConnector
import controllers.actions.*
import forms.ConfirmVendorAddressFormProvider
import models.{ConfirmVendorAddress, Mode}
import navigation.Navigator
import pages.ConfirmVendorAddressPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ConfirmVendorAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmVendorAddressController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          sessionRepository: SessionRepository,
                                          navigator: Navigator,
                                          identify: IdentifierAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          formProvider: ConfirmVendorAddressFormProvider,
                                          stubConnector: StubConnector,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: ConfirmVendorAddressView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[ConfirmVendorAddress] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val returnIdOpt = request.userAnswers.returnId

      returnIdOpt match {
        case Some(returnId) =>

          stubConnector.stubGetFullReturn(Some(returnId)).map { fullReturn =>

            val vendorOrBusinessName =
//              request.userAnswers.get(VendorOrBusinessNamePage).map { vn =>
//              Seq(vn.forename1, vn.forename2, Some(vn.name)).flatten.mkString(" ").trim.replaceAll(" +", " ")
//            }.getOrElse
            ("Peter Von Vendor")

            //pull list and find the one with isMainVendorFlag == true (this is main vendor)
            //pull main vendor address to show on page, same check
            val (line1, line2, line3, line4, postcode) = {
              val vendors = fullReturn.vendor.getOrElse(Seq.empty)
              if (vendors.isEmpty) {
                val p = fullReturn.purchaser.flatMap(_.headOption).getOrElse(models.Purchaser())
                (p.address1, p.address2, p.address3, p.address4, p.postcode)
              } else {
                val mainId = fullReturn.returnInfo.flatMap(_.mainVendorID).getOrElse("")
                val v = vendors.find(_.vendorID.contains(mainId))
                  .orElse(vendors.headOption)
                  .getOrElse(models.Vendor())
                (v.address1, v.address2, v.address3, v.address4, v.postcode)
              }
            }

            val preparedForm = request.userAnswers.get(ConfirmVendorAddressPage) match {
              case None => form
              case Some(value) => form.fill(value)
            }

            Ok(view(preparedForm, vendorOrBusinessName, line1, line2, line3, line4, postcode, mode))
          }

        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val fullReturnOpt = request.userAnswers.fullReturn

      val vendorOrBusinessName =
        //              request.userAnswers.get(VendorOrBusinessNamePage).map { vn =>
        //              Seq(vn.forename1, vn.forename2, Some(vn.name)).flatten.mkString(" ").trim.replaceAll(" +", " ")
        //            }.getOrElse
        ("Peter Von Vendor")

      val (line1, line2, line3, line4, postcode) = fullReturnOpt match {
        case Some(fullReturn) =>
          val vendors = fullReturn.vendor.getOrElse(Seq.empty)
          if (vendors.isEmpty) {
            val p = fullReturn.purchaser.flatMap(_.headOption).getOrElse(models.Purchaser())
            (p.address1, p.address2, p.address3, p.address4, p.postcode)
          } else {
            val mainId = fullReturn.returnInfo.flatMap(_.mainVendorID).getOrElse("")
            val v = vendors.find(_.vendorID.contains(mainId)).orElse(vendors.headOption).getOrElse(models.Vendor())
            (v.address1, v.address2, v.address3, v.address4, v.postcode)
          }
        case None =>
          (None, None, None, None, None)
      }

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(
            formWithErrors, vendorOrBusinessName, line1, line2, line3, line4, postcode, mode
          ))),

        //appropriate redirect here to either route to next page on 'Yes' OR address lookup on 'No'
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ConfirmVendorAddressPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield {
            value match {
              case ConfirmVendorAddress.Yes =>
                Redirect(navigator.nextPage(ConfirmVendorAddressPage, mode, updatedAnswers))

                //route once implemented = controllers.vendor.routes.VendorAddressController.redirectToAddressLookup()
              case ConfirmVendorAddress.No =>
                Redirect(controllers.preliminary.routes.PrelimAddressController.redirectToAddressLookup())
            }
          }
      )
  }
}
