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

package services

import config.AddressLookupConfiguration
import connectors.AddressLookupConnector
import models.address.{Address, AddressLookupJourneyIdentifier, MandatoryFieldsConfigModel}
import models.requests.DataRequest
import pages.PurchaserAddressPage
import play.api.http.HeaderNames.LOCATION
import play.api.http.HttpVerbs.GET
import play.api.mvc.{Call, Request}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddressLookupService @Inject()(addressLookupConnector: AddressLookupConnector,
                                     alfConfig: AddressLookupConfiguration,
                                     sessionRepository: SessionRepository) extends LoggingUtil {

  def getAddressById(id: String)(implicit hc: HeaderCarrier): Future[Address] = addressLookupConnector.getAddress(id)

  def getJourneyUrl(journeyId: AddressLookupJourneyIdentifier.Value,
                    continueUrl: Call,
                    useUkMode: Boolean = false,
                    optName: Option[String] = None,
                    mandatoryFieldsConfigModel: MandatoryFieldsConfigModel)
                   (implicit hc: HeaderCarrier, request: Request[_], executionContext: ExecutionContext): Future[Call] = {
    addressLookupConnector.getOnRampUrl(alfConfig(journeyId,
      continueUrl,
      useUkMode,
      optName,
      mandatoryFieldsConfigModel = mandatoryFieldsConfigModel))
  }
  
  def saveAddressDetails(address: Address)
                        (implicit request: DataRequest[_], ec: ExecutionContext): Future[Boolean] = {
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(PurchaserAddressPage, address))
      result <- sessionRepository.set(updatedAnswers)
    } yield result
  }
  

}
