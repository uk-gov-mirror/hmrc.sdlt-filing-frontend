/*
 * Copyright 2017 HM Revenue & Customs
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

import com.fasterxml.jackson.core.JsonParseException
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import config.{AddressLookupConfiguration, FrontendAppConfig}
import models.address.AddressLookupJourneyIdentifier.prelimQuestionsAddress
import models.address.{Address, Country, MandatoryFieldsConfigModel}
import org.apache.pekko.http.scaladsl.model.HttpResponse
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Call
import services.AddressLookupService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.WireMockHelper

class AddressLookupConnectorISpec
  extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneAppPerSuite
    with WireMockHelper {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val wireMockPort = 11111

  override protected val server: WireMockServer = new WireMockServer(wireMockConfig().port(wireMockPort))

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.address-lookup-frontend.protocol" -> "http",
        "microservice.services.address-lookup-frontend.host" -> "localhost",
        "microservice.services.address-lookup-frontend.port" -> wireMockPort,
        "features.address-lookup-stub" -> false
      )
      .build()

  private lazy val alfConnector = app.injector.instanceOf[AddressLookupConnector]
  private lazy val addressLookupService = app.injector.instanceOf[AddressLookupService]
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  implicit val messages: Messages = messagesApi.preferred(Seq(Lang("en")))

  private val testAddressId = "addressId"

  // Complete JSON response for a valid address
  private val validAddressJson: JsValue = Json.obj(
    "auditRef" -> "test-audit-ref",
    "address" -> Json.obj(
      "lines" -> Json.arr(
        "16 Coniston Court",
        "Holland road"
      ),
      "postcode" -> "BN3 1JU",
      "country" -> Json.obj(
        "code" -> "UK",
        "name" -> "United Kingdom"
      )
    )
  )

  private val alfInitSuccessUrl = "http://localhost:9028/lookup-address/test-journey-id"

  "AddressLookupConnector Integration Tests" - {

    "getAddress" - {

      "must return Address when ALF returns 200 OK with valid JSON" in {
        server.stubFor(
          get(urlPathEqualTo("/api/v2/confirmed"))
            .withQueryParam("id", equalTo(testAddressId))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(validAddressJson.toString())
            )
        )

        val result = alfConnector.getAddress(testAddressId).futureValue

        result mustBe Address(
          line1 = "16 Coniston Court",
          line2 = Some("Holland road"),
          country = Some(Country(Some("UK"), Some("United Kingdom"))),
          postcode = Some("BN3 1JU"),
          addressValidated = false
        )

        server.verify(
          getRequestedFor(urlPathEqualTo("/api/v2/confirmed"))
            .withQueryParam("id", equalTo(testAddressId))
        )
      }

      "must handle different address formats" in {
        val minimalAddressJson = Json.obj(
          "auditRef" -> "test-audit-ref",
          "address" -> Json.obj(
            "lines" -> Json.arr("Simple Street"),
            "postcode" -> "SW1A 1AA",
            "country" -> Json.obj(
              "code" -> "GB",
              "name" -> "United Kingdom"
            )
          )
        )

        server.stubFor(
          get(urlPathEqualTo("/api/v2/confirmed"))
            .withQueryParam("id", equalTo("address123"))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(minimalAddressJson.toString())
            )
        )

        val result = alfConnector.getAddress("address123").futureValue

        result mustBe a[Address]
        result.line1 mustBe "Simple Street"
        result.postcode mustBe Some("SW1A 1AA")

        server.verify(
          getRequestedFor(urlPathEqualTo("/api/v2/confirmed"))
            .withQueryParam("id", equalTo("address123"))
        )
      }

      "must throw NotFoundException when ALF returns 404 Not Found" in {
        server.stubFor(
          get(urlPathEqualTo("/api/v2/confirmed"))
            .withQueryParam("id", equalTo("NONEXISTENT"))
            .willReturn(
              aResponse()
                .withStatus(404)
                .withBody("Not Found")
            )
        )

        val result = alfConnector.getAddress("NONEXISTENT").failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 404

        server.verify(
          getRequestedFor(urlPathEqualTo("/api/v2/confirmed"))
            .withQueryParam("id", equalTo("NONEXISTENT"))
        )
      }

      "must throw UpstreamErrorResponse when ALF returns 400 Bad Request" in {
        server.stubFor(
          get(urlPathEqualTo("/api/v2/confirmed"))
            .withQueryParam("id", equalTo(testAddressId))
            .willReturn(
              aResponse()
                .withStatus(400)
                .withBody("Bad Request")
            )
        )

        val result = alfConnector.getAddress(testAddressId).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 400

        server.verify(
          getRequestedFor(urlPathEqualTo("/api/v2/confirmed"))
        )
      }

      "must throw UpstreamErrorResponse when ALF returns 500 Internal Server Error" in {
        server.stubFor(
          get(urlPathEqualTo("/api/v2/confirmed"))
            .withQueryParam("id", equalTo(testAddressId))
            .willReturn(
              aResponse()
                .withStatus(500)
                .withBody("Internal Server Error")
            )
        )

        val result = alfConnector.getAddress(testAddressId).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500

        server.verify(
          getRequestedFor(urlPathEqualTo("/api/v2/confirmed"))
        )
      }

      "must throw UpstreamErrorResponse when ALF returns 502 Bad Gateway" in {
        server.stubFor(
          get(urlPathEqualTo("/api/v2/confirmed"))
            .withQueryParam("id", equalTo(testAddressId))
            .willReturn(
              aResponse()
                .withStatus(502)
                .withBody("Bad Gateway")
            )
        )

        val result = alfConnector.getAddress(testAddressId).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 502
      }

      "must throw UpstreamErrorResponse when ALF returns 503 Service Unavailable" in {
        server.stubFor(
          get(urlPathEqualTo("/api/v2/confirmed"))
            .withQueryParam("id", equalTo(testAddressId))
            .willReturn(
              aResponse()
                .withStatus(503)
                .withBody("Service Unavailable")
            )
        )

        val result = alfConnector.getAddress(testAddressId).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 503
      }

      "must handle connection errors when service is unavailable" in {
        server.stubFor(
          get(urlPathEqualTo("/api/v2/confirmed"))
            .withQueryParam("id", equalTo(testAddressId))
            .willReturn(
              aResponse()
                .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)
            )
        )

        val result = alfConnector.getAddress(testAddressId).failed.futureValue

        result mustBe a[Throwable]
      }

      "must handle malformed JSON response" in {
        server.stubFor(
          get(urlPathEqualTo("/api/v2/confirmed"))
            .withQueryParam("id", equalTo(testAddressId))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{invalid json}")
            )
        )

        val result = alfConnector.getAddress(testAddressId).failed.futureValue

        result mustBe a[JsonParseException]
      }

      "must make GET request to correct endpoint" in {
        server.stubFor(
          get(urlPathEqualTo("/api/v2/confirmed"))
            .withQueryParam("id", equalTo(testAddressId))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(validAddressJson.toString())
            )
        )

        alfConnector.getAddress(testAddressId).futureValue

        server.verify(
          1,
          getRequestedFor(urlPathEqualTo("/api/v2/confirmed"))
        )
      }

      "must not make multiple requests for a single call" in {
        server.stubFor(
          get(urlPathEqualTo("/api/v2/confirmed"))
            .withQueryParam("id", equalTo(testAddressId))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(validAddressJson.toString())
            )
        )

        alfConnector.getAddress(testAddressId).futureValue

        server.verify(
          1,
          getRequestedFor(urlPathEqualTo("/api/v2/confirmed"))
        )
      }
    }

    "getOnRampUrl" - {

      implicit val request: play.api.test.FakeRequest[_] = play.api.test.FakeRequest()

      val alfConfig = app.injector.instanceOf[AddressLookupConfiguration]
      val continueUrl = Call("GET", "continueUrl")
      val testMandatoryFieldsConfigModel = MandatoryFieldsConfigModel(line1 = Some(true),
        line2 = Some(true),
        line3 = Some(true),
        town = Some(true),
        postcode = Some(true))

      val journeyModel = alfConfig(prelimQuestionsAddress, continueUrl, useUkMode = true, mandatoryFieldsConfigModel = testMandatoryFieldsConfigModel )

      "must return a Call with URL when Location header is present" in {
        server.stubFor(
          post(urlPathEqualTo("/api/v2/init"))
            .willReturn(
              aResponse()
                .withStatus(202)
                .withHeader("Location", alfInitSuccessUrl)
            )
        )

        val result = alfConnector.getOnRampUrl(journeyModel).futureValue

        result mustBe Call("GET", alfInitSuccessUrl)

        server.verify(
          postRequestedFor(urlPathEqualTo("/api/v2/init"))
        )
      }

      "must handle different Location header URLs" in {
        val customUrl = "http://localhost:9028/lookup-address/custom-journey"

        server.stubFor(
          post(urlPathEqualTo("/api/v2/init"))
            .willReturn(
              aResponse()
                .withStatus(202)
                .withHeader("Location", customUrl)
            )
        )

        val result = alfConnector.getOnRampUrl(journeyModel).futureValue

        result mustBe Call("GET", customUrl)
      }

      "must throw ALFLocationHeaderNotSetException when Location header is missing" in {
        server.stubFor(
          post(urlPathEqualTo("/api/v2/init"))
            .willReturn(
              aResponse()
                .withStatus(202)
              // No Location header
            )
        )

        val result = alfConnector.getOnRampUrl(journeyModel).failed.futureValue

        result mustBe an[ALFLocationHeaderNotSetException]

        server.verify(
          postRequestedFor(urlPathEqualTo("/api/v2/init"))
        )
      }

      "must throw UpstreamErrorResponse when ALF returns 400 Bad Request" in {
        server.stubFor(
          post(urlPathEqualTo("/api/v2/init"))
            .willReturn(
              aResponse()
                .withStatus(400)
                .withBody("Bad Request")
            )
        )

        val result = alfConnector.getOnRampUrl(journeyModel).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 400

        server.verify(
          postRequestedFor(urlPathEqualTo("/api/v2/init"))
        )
      }

      "must throw UpstreamErrorResponse when ALF returns 500 Internal Server Error" in {
        server.stubFor(
          post(urlPathEqualTo("/api/v2/init"))
            .willReturn(
              aResponse()
                .withStatus(500)
                .withBody("Internal Server Error")
            )
        )

        val result = alfConnector.getOnRampUrl(journeyModel).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500

        server.verify(
          postRequestedFor(urlPathEqualTo("/api/v2/init"))
        )
      }

      "must throw UpstreamErrorResponse when ALF returns 502 Bad Gateway" in {
        server.stubFor(
          post(urlPathEqualTo("/api/v2/init"))
            .willReturn(
              aResponse()
                .withStatus(502)
                .withBody("Bad Gateway")
            )
        )

        val result = alfConnector.getOnRampUrl(journeyModel).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 502
      }

      "must throw UpstreamErrorResponse when ALF returns 503 Service Unavailable" in {
        server.stubFor(
          post(urlPathEqualTo("/api/v2/init"))
            .willReturn(
              aResponse()
                .withStatus(503)
                .withBody("Service Unavailable")
            )
        )

        val result = alfConnector.getOnRampUrl(journeyModel).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 503
      }

      "must handle connection errors when service is unavailable" in {
        server.stubFor(
          post(urlPathEqualTo("/api/v2/init"))
            .willReturn(
              aResponse()
                .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)
            )
        )

        val result = alfConnector.getOnRampUrl(journeyModel).failed.futureValue

        result mustBe a[Throwable]
      }

      "must include correct headers in the request" in {
        server.stubFor(
          post(urlPathEqualTo("/api/v2/init"))
            .willReturn(
              aResponse()
                .withStatus(202)
                .withHeader("Location", alfInitSuccessUrl)
            )
        )

        alfConnector.getOnRampUrl(journeyModel).futureValue

        server.verify(
          postRequestedFor(urlPathEqualTo("/api/v2/init"))
            .withHeader("Content-Type", containing("application/json"))
        )
      }

      "must make POST request to correct endpoint" in {
        server.stubFor(
          post(urlPathEqualTo("/api/v2/init"))
            .willReturn(
              aResponse()
                .withStatus(202)
                .withHeader("Location", alfInitSuccessUrl)
            )
        )

        alfConnector.getOnRampUrl(journeyModel).futureValue

        server.verify(
          1,
          postRequestedFor(urlPathEqualTo("/api/v2/init"))
        )
      }

      "must not make multiple requests for a single call" in {
        server.stubFor(
          post(urlPathEqualTo("/api/v2/init"))
            .willReturn(
              aResponse()
                .withStatus(202)
                .withHeader("Location", alfInitSuccessUrl)
            )
        )

        alfConnector.getOnRampUrl(journeyModel).futureValue

        server.verify(
          1,
          postRequestedFor(urlPathEqualTo("/api/v2/init"))
        )
      }
    }
  }
}