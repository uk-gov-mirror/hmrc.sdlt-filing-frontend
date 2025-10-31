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

package connectors

import com.fasterxml.jackson.core.JsonParseException
import com.github.tomakehurst.wiremock.client.WireMock.*
import models.PrelimReturn
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.*
import utils.WireMockHelper

import scala.concurrent.ExecutionContext.Implicits.global

class StubConnectorISpec
  extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneAppPerSuite
    with WireMockHelper {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[_] = FakeRequest()

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.stamp-duty-land-tax-stub.host" -> "localhost",
        "microservice.services.stamp-duty-land-tax-stub.port" -> server.port(),
        "microservice.services.stamp-duty-land-tax-stub.protocol" -> "http"
      )
      .build()

  private lazy val connector = app.injector.instanceOf[StubConnector]

  private val testReturnId = "123456"

  private val completePrelimReturn = PrelimReturn(
    stornId = "12345",
    purchaserIsCompany = "YES",
    surNameOrCompanyName = "Test Company",
    houseNumber = Some(23),
    addressLine1 = "Test Street",
    addressLine2 = Some("Apartment 5"),
    addressLine3 = Some("Building A"),
    addressLine4 = Some("District B"),
    postcode = Some("TE23 5TT"),
    transactionType = "O"
  )

  // Complete JSON with all required fields
  private val prelimReturnJson: JsValue = Json.obj(
    "stornId" -> testReturnId,
    "purchaserIsCompany" -> "YES",
    "surNameOrCompanyName" -> "Test Company",
    "houseNumber" -> 23,
    "addressLine1" -> "Test Street",
    "addressLine2" -> JsNull,
    "addressLine3" -> JsNull,
    "addressLine4" -> JsNull,
    "postcode" -> "TE23 5TT",
    "transactionType" -> "O"
  )

  private val returnIdJson: JsValue = Json.obj(
    "returnId" -> testReturnId
  )

  "StubConnector Integration Tests" - {

    "stubPremlimQuestions" - {

      "must return PrelimReturn when the stub returns 200 OK" in {
        server.stubFor(
          get(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .withQueryParam("returnId", equalTo(testReturnId))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(prelimReturnJson.toString())
            )
        )

        val result = connector.stubPremlimQuestions(testReturnId).futureValue

        result mustBe a[PrelimReturn]

        server.verify(
          getRequestedFor(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .withQueryParam("returnId", equalTo(testReturnId))
        )
      }

      "must handle different returnId values" in {
        val differentReturnIds = List("ABC-123", "TEST-789", "12345")

        differentReturnIds.foreach { returnId =>
          val customJson = Json.obj(
            "stornId" -> returnId,
            "purchaserIsCompany" -> "YES",
            "surNameOrCompanyName" -> "Test Company",
            "houseNumber" -> 23,
            "addressLine1" -> "Test Street",
            "addressLine2" -> JsNull,
            "addressLine3" -> JsNull,
            "addressLine4" -> JsNull,
            "postcode" -> "TE23 5TT",
            "transactionType" -> "O"
          )

          server.stubFor(
            get(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
              .withQueryParam("returnId", equalTo(returnId))
              .willReturn(
                aResponse()
                  .withStatus(200)
                  .withHeader("Content-Type", "application/json")
                  .withBody(customJson.toString())
              )
          )

          val result = connector.stubPremlimQuestions(returnId).futureValue

          result mustBe a[PrelimReturn]

          server.verify(
            getRequestedFor(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
              .withQueryParam("returnId", equalTo(returnId))
          )
        }
      }

      "must throw BadRequestException when stub returns 400 Bad Request" in {
        server.stubFor(
          get(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .withQueryParam("returnId", equalTo(testReturnId))
            .willReturn(
              aResponse()
                .withStatus(400)
                .withBody("Bad Request")
            )
        )

        val result = connector.stubPremlimQuestions(testReturnId).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 400

        server.verify(
          getRequestedFor(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
        )
      }

      "must throw NotFoundException when stub returns 404 Not Found" in {
        server.stubFor(
          get(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .withQueryParam("returnId", equalTo("NONEXISTENT"))
            .willReturn(
              aResponse()
                .withStatus(404)
                .withBody("Not Found")
            )
        )

        val result = connector.stubPremlimQuestions("NONEXISTENT").failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 404

        server.verify(
          getRequestedFor(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
        )
      }

      "must throw UpstreamErrorResponse when stub returns 500 Internal Server Error" in {
        server.stubFor(
          get(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .withQueryParam("returnId", equalTo(testReturnId))
            .willReturn(
              aResponse()
                .withStatus(500)
                .withBody("Internal Server Error")
            )
        )

        val result = connector.stubPremlimQuestions(testReturnId).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500

        server.verify(
          getRequestedFor(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
        )
      }

      "must throw UpstreamErrorResponse when stub returns 502 Bad Gateway" in {
        server.stubFor(
          get(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .withQueryParam("returnId", equalTo(testReturnId))
            .willReturn(
              aResponse()
                .withStatus(502)
                .withBody("Bad Gateway")
            )
        )

        val result = connector.stubPremlimQuestions(testReturnId).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 502
      }

      "must throw UpstreamErrorResponse when stub returns 503 Service Unavailable" in {
        server.stubFor(
          get(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .withQueryParam("returnId", equalTo(testReturnId))
            .willReturn(
              aResponse()
                .withStatus(503)
                .withBody("Service Unavailable")
            )
        )

        val result = connector.stubPremlimQuestions(testReturnId).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 503
      }

      "must include correct headers in the request" in {
        server.stubFor(
          get(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .withQueryParam("returnId", equalTo(testReturnId))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(prelimReturnJson.toString())
            )
        )

        connector.stubPremlimQuestions(testReturnId).futureValue

        server.verify(
          getRequestedFor(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .withHeader("User-Agent", equalTo("sdlt-filing-frontend"))
        )
      }

      "must handle connection errors when service is unavailable" in {
        server.stubFor(
          get(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .withQueryParam("returnId", equalTo(testReturnId))
            .willReturn(
              aResponse()
                .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)
            )
        )

        val result = connector.stubPremlimQuestions(testReturnId).failed.futureValue

        result mustBe a[Throwable]
      }

      "must correctly parse JSON response into PrelimReturn model" in {
        val detailedPrelimReturnJson = Json.obj(
          "stornId" -> "TEST-123",
          "purchaserIsCompany" -> "NO",
          "surNameOrCompanyName" -> "John Doe",
          "houseNumber" -> 23,
          "addressLine1" -> "Test Street",
          "addressLine2" -> "Apartment 5",
          "addressLine3" -> JsNull,
          "addressLine4" -> JsNull,
          "postcode" -> "TE23 5TT",
          "transactionType" -> "R"
        )

        server.stubFor(
          get(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .withQueryParam("returnId", equalTo("TEST-123"))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(detailedPrelimReturnJson.toString())
            )
        )

        val result = connector.stubPremlimQuestions("TEST-123").futureValue

        result mustBe a[PrelimReturn]
      }

      "must handle malformed JSON response" in {
        server.stubFor(
          get(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .withQueryParam("returnId", equalTo(testReturnId))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{invalid json}")
            )
        )

        val result = connector.stubPremlimQuestions(testReturnId).failed.futureValue

        result mustBe a[JsonParseException]
      }

      "must make GET request to correct endpoint" in {
        server.stubFor(
          get(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .withQueryParam("returnId", equalTo(testReturnId))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(prelimReturnJson.toString())
            )
        )

        connector.stubPremlimQuestions(testReturnId).futureValue

        server.verify(
          1,
          getRequestedFor(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
        )
      }

      "must not make multiple requests for a single call" in {
        server.stubFor(
          get(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .withQueryParam("returnId", equalTo(testReturnId))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(prelimReturnJson.toString())
            )
        )

        connector.stubPremlimQuestions(testReturnId).futureValue

        server.verify(
          1,
          getRequestedFor(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
        )
      }
    }

    "stubPrelimReturnId" - {

      "must return returnId when the stub returns 200 OK" in {
        server.stubFor(
          post(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(returnIdJson.toString())
            )
        )

        val result = connector.stubPrelimReturnId(completePrelimReturn).futureValue

        result mustBe a[String]

        server.verify(
          postRequestedFor(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
        )
      }

      "must throw BadRequestException when stub returns 400 Bad Request" in {
        server.stubFor(
          post(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .willReturn(
              aResponse()
                .withStatus(400)
                .withBody("Bad Request")
            )
        )

        val result = connector.stubPrelimReturnId(completePrelimReturn).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 400

        server.verify(
          postRequestedFor(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
        )
      }

      "must throw UpstreamErrorResponse when stub returns 500 Internal Server Error" in {
        server.stubFor(
          post(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .willReturn(
              aResponse()
                .withStatus(500)
                .withBody("Internal Server Error")
            )
        )

        val result = connector.stubPrelimReturnId(completePrelimReturn).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500

        server.verify(
          postRequestedFor(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
        )
      }

      "must throw UpstreamErrorResponse when stub returns 502 Bad Gateway" in {
        server.stubFor(
          post(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .willReturn(
              aResponse()
                .withStatus(502)
                .withBody("Bad Gateway")
            )
        )

        val result = connector.stubPrelimReturnId(completePrelimReturn).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 502
      }

      "must throw UpstreamErrorResponse when stub returns 503 Service Unavailable" in {
        server.stubFor(
          post(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .willReturn(
              aResponse()
                .withStatus(503)
                .withBody("Service Unavailable")
            )
        )

        val result = connector.stubPrelimReturnId(completePrelimReturn).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 503
      }

      "must include correct headers in the request" in {
        server.stubFor(
          post(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(returnIdJson.toString())
            )
        )

        connector.stubPrelimReturnId(completePrelimReturn).futureValue

        server.verify(
          postRequestedFor(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .withHeader("User-Agent", equalTo("sdlt-filing-frontend"))
        )
      }

      "must handle connection errors when service is unavailable" in {
        server.stubFor(
          post(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .willReturn(
              aResponse()
                .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)
            )
        )

        val result = connector.stubPrelimReturnId(completePrelimReturn).failed.futureValue

        result mustBe a[Throwable]
      }

      "must handle malformed JSON response" in {
        server.stubFor(
          post(urlPathEqualTo("/stamp-duty-land-tax-stub/prelim/returns"))
            .willReturn(
              aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{invalid json}")
            )
        )

        val result = connector.stubPrelimReturnId(completePrelimReturn).failed.futureValue
        println(result)

        result mustBe a[UpstreamErrorResponse]
      }
    }
    }
  }