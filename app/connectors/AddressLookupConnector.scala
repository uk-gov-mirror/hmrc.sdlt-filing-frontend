/*
 * Copyright 2024 HM Revenue & Customs
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

package connectors

import config.FrontendAppConfig
import models.address.Address.addressLookupReads
import models.address.{Address, AddressLookupConfigurationModel}
import play.api.http.HeaderNames.*
import play.api.http.HttpVerbs.*
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.mvc.{Call, Request}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps, UpstreamErrorResponse}
import utils.LoggingUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

@Singleton
class AddressLookupConnector @Inject()(val http: HttpClientV2, appConfig: FrontendAppConfig)
                                      (implicit ec: ExecutionContext) extends LoggingUtil {

  implicit val reads: Address.addressLookupReads.type = Address.addressLookupReads

  def getAddress(id: String)(implicit hc: HeaderCarrier): Future[Address] =
    http.get(url"${appConfig.addressLookupRetrievalUrl(id)}")
      .execute[Address]

  def getOnRampUrl(alfConfig: AddressLookupConfigurationModel)(implicit hc: HeaderCarrier, request: Request[_]): Future[Call] =
    http.post(url"${appConfig.addressLookupJourneyUrl}")
      .withBody(Json.toJson(alfConfig))
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .flatMap {
        case Right(resp) =>
          resp.header(LOCATION).map(location => Future.successful(Call(GET, location))).getOrElse {
            warnLog("[getOnRampUrl] - ERROR: Location header not set in ALF response")
            Future.failed(new ALFLocationHeaderNotSetException)
          }
        case Left(error) =>
          Future.failed(error)
      }
}

private[connectors] class ALFLocationHeaderNotSetException extends NoStackTrace