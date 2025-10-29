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

import base.SpecBase
import config.FrontendAppConfig
import constants.AddressLookupConstants
import models.address.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call
import play.api.test.FakeRequest
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class AddressLookupConnectorSpec extends SpecBase with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val request: FakeRequest[_] = FakeRequest()

  private val testAddressLookupUrl = "http://localhost:9028"

  val testAddress: Address = Address(
    "16 Coniston Court",
    Some("Holland road"),
    None,
    None,
    None,
    Some("BN3 1JU"),
    Some(Country(Some("UK"), Some("United Kingdom"))),
    true
  )

  val testCall: Call = Call("GET", "http://localhost:9028/lookup-address/journey")

  val testJourneyConfig: AddressLookupConfigurationModel = AddressLookupConstants.testAlfConfig

  "AddressLookupConnector" - {

    "getAddress" - {

      "must return Address when request is successful" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val testId = "test-address-id-123"

        when(mockConfig.addressLookupRetrievalUrl(eqTo(testId))).thenReturn(s"$testAddressLookupUrl/api/confirmed?id=$testId")
        when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Address](any(), any()))
          .thenReturn(Future.successful(testAddress))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)
        val result = connector.getAddress(testId).futureValue

        result mustBe testAddress
      }

      "must construct correct URL with address ID parameter" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val testId = "ABC-123-XYZ"

        when(mockConfig.addressLookupRetrievalUrl(eqTo(testId))).thenReturn(s"$testAddressLookupUrl/api/confirmed?id=$testId")
        when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Address](any(), any()))
          .thenReturn(Future.successful(testAddress))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)
        connector.getAddress(testId).futureValue

        verify(mockHttpClient, times(1)).get(any())(any())
        verify(mockConfig, times(1)).addressLookupRetrievalUrl(eqTo(testId))
      }

      "must handle upstream 4xx errors and throw exception" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val testId = "invalid-id"
        val upstreamError = UpstreamErrorResponse("Bad Request", 400)

        when(mockConfig.addressLookupRetrievalUrl(eqTo(testId))).thenReturn(s"$testAddressLookupUrl/api/confirmed?id=$testId")
        when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Address](any(), any()))
          .thenReturn(Future.failed(upstreamError))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)

        whenReady(connector.getAddress(testId).failed) { exception =>
          exception mustBe a[UpstreamErrorResponse]
          exception.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 400
        }

        verify(mockHttpClient, times(1)).get(any())(any())
      }

      "must handle upstream 404 Not Found errors" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val testId = "non-existent-id"
        val upstreamError = UpstreamErrorResponse("Not Found", 404)

        when(mockConfig.addressLookupRetrievalUrl(eqTo(testId))).thenReturn(s"$testAddressLookupUrl/api/confirmed?id=$testId")
        when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Address](any(), any()))
          .thenReturn(Future.failed(upstreamError))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)

        whenReady(connector.getAddress(testId).failed) { exception =>
          exception mustBe a[UpstreamErrorResponse]
          exception.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 404
        }
      }

      "must handle upstream 5xx errors and throw exception" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val testId = "test-id"
        val upstreamError = UpstreamErrorResponse("Internal Server Error", 500)

        when(mockConfig.addressLookupRetrievalUrl(eqTo(testId))).thenReturn(s"$testAddressLookupUrl/api/confirmed?id=$testId")
        when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Address](any(), any()))
          .thenReturn(Future.failed(upstreamError))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)

        whenReady(connector.getAddress(testId).failed) { exception =>
          exception mustBe a[UpstreamErrorResponse]
          exception.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
        }

        verify(mockHttpClient, times(1)).get(any())(any())
      }

      "must handle 503 Service Unavailable errors" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val testId = "test-id"
        val upstreamError = UpstreamErrorResponse("Service Unavailable", 503)

        when(mockConfig.addressLookupRetrievalUrl(eqTo(testId))).thenReturn(s"$testAddressLookupUrl/api/confirmed?id=$testId")
        when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Address](any(), any()))
          .thenReturn(Future.failed(upstreamError))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)

        whenReady(connector.getAddress(testId).failed) { exception =>
          exception mustBe a[UpstreamErrorResponse]
          exception.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 503
        }
      }

      "must handle general exceptions and throw exception" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val testId = "test-id"
        val runtimeException = new RuntimeException("Connection failed")

        when(mockConfig.addressLookupRetrievalUrl(eqTo(testId))).thenReturn(s"$testAddressLookupUrl/api/confirmed?id=$testId")
        when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Address](any(), any()))
          .thenReturn(Future.failed(runtimeException))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)

        whenReady(connector.getAddress(testId).failed) { exception =>
          exception mustBe a[RuntimeException]
          exception.getMessage mustBe "Connection failed"
        }

        verify(mockHttpClient, times(1)).get(any())(any())
      }

      "must handle different address ID formats" in {
        val addressIds = List("123", "ABC-123", "test-address-id-with-dashes")

        addressIds.foreach { testId =>
          val mockHttpClient = mock[HttpClientV2]
          val mockConfig = mock[FrontendAppConfig]
          val mockRequestBuilder = mock[RequestBuilder]

          when(mockConfig.addressLookupRetrievalUrl(eqTo(testId))).thenReturn(s"$testAddressLookupUrl/api/confirmed?id=$testId")
          when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
          when(mockRequestBuilder.execute[Address](any(), any()))
            .thenReturn(Future.successful(testAddress))

          val connector = new AddressLookupConnector(mockHttpClient, mockConfig)
          val result = connector.getAddress(testId).futureValue

          result mustBe testAddress
        }
      }

      "must pass implicit HeaderCarrier to http client" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val testId = "test-id"
        val customHc = HeaderCarrier(sessionId = Some(uk.gov.hmrc.http.SessionId("test-session")))

        when(mockConfig.addressLookupRetrievalUrl(eqTo(testId))).thenReturn(s"$testAddressLookupUrl/api/confirmed?id=$testId")
        when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Address](any(), any()))
          .thenReturn(Future.successful(testAddress))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)
        connector.getAddress(testId)(customHc).futureValue

        verify(mockHttpClient, times(1)).get(any())(any())
      }

      "must call execute method on request builder" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val testId = "test-id"

        when(mockConfig.addressLookupRetrievalUrl(eqTo(testId))).thenReturn(s"$testAddressLookupUrl/api/confirmed?id=$testId")
        when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Address](any(), any()))
          .thenReturn(Future.successful(testAddress))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)
        connector.getAddress(testId).futureValue

        verify(mockRequestBuilder, times(1)).execute[Address](any(), any())
      }
    }

    "getOnRampUrl" - {

      "must return Call when request is successful and Location header is present" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val alfInitSuccessUrl = "http://localhost:9028/lookup-address/journey"
        val mockResponse = mock[HttpResponse]

        when(mockConfig.addressLookupJourneyUrl).thenReturn(s"$testAddressLookupUrl/api/v2/init")
        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.successful(Right(mockResponse)))
        when(mockResponse.header(eqTo("Location"))).thenReturn(Some(alfInitSuccessUrl))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)
        val result = connector.getOnRampUrl(testJourneyConfig).futureValue

        result mustBe Call("GET", alfInitSuccessUrl)
      }

      "must throw ALFLocationHeaderNotSetException when Location header is missing" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val mockResponse = mock[HttpResponse]

        when(mockConfig.addressLookupJourneyUrl).thenReturn(s"$testAddressLookupUrl/api/v2/init")
        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.successful(Right(mockResponse)))
        when(mockResponse.header(eqTo("Location"))).thenReturn(None)

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)
        val result = connector.getOnRampUrl(testJourneyConfig).failed.futureValue

        result mustBe an[ALFLocationHeaderNotSetException]

        verify(mockHttpClient, times(1)).post(any())(any())
      }

      "must throw UpstreamErrorResponse when ALF returns 400 Bad Request" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val upstreamError = UpstreamErrorResponse("Bad Request", 400)

        when(mockConfig.addressLookupJourneyUrl).thenReturn(s"$testAddressLookupUrl/api/v2/init")
        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.successful(Left(upstreamError)))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)
        val result = connector.getOnRampUrl(testJourneyConfig).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 400

        verify(mockHttpClient, times(1)).post(any())(any())
      }

      "must throw UpstreamErrorResponse when ALF returns 500 Internal Server Error" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val upstreamError = UpstreamErrorResponse("Internal Server Error", 500)

        when(mockConfig.addressLookupJourneyUrl).thenReturn(s"$testAddressLookupUrl/api/v2/init")
        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.successful(Left(upstreamError)))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)
        val result = connector.getOnRampUrl(testJourneyConfig).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
      }

      "must throw UpstreamErrorResponse when ALF returns 502 Bad Gateway" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val upstreamError = UpstreamErrorResponse("Bad Gateway", 502)

        when(mockConfig.addressLookupJourneyUrl).thenReturn(s"$testAddressLookupUrl/api/v2/init")
        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.successful(Left(upstreamError)))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)
        val result = connector.getOnRampUrl(testJourneyConfig).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 502
      }

      "must throw UpstreamErrorResponse when ALF returns 503 Service Unavailable" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val upstreamError = UpstreamErrorResponse("Service Unavailable", 503)

        when(mockConfig.addressLookupJourneyUrl).thenReturn(s"$testAddressLookupUrl/api/v2/init")
        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.successful(Left(upstreamError)))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)
        val result = connector.getOnRampUrl(testJourneyConfig).failed.futureValue

        result mustBe an[UpstreamErrorResponse]
        result.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 503
      }

      "must handle general exceptions and throw exception" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val runtimeException = new RuntimeException("Connection failed")

        when(mockConfig.addressLookupJourneyUrl).thenReturn(s"$testAddressLookupUrl/api/v2/init")
        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.failed(runtimeException))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)

        whenReady(connector.getOnRampUrl(testJourneyConfig).failed) { exception =>
          exception mustBe a[RuntimeException]
          exception.getMessage mustBe "Connection failed"
        }

        verify(mockHttpClient, times(1)).post(any())(any())
      }

      "must construct correct URL for journey initialization" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val mockResponse = mock[HttpResponse]
        val alfInitSuccessUrl = "http://localhost:9028/lookup-address/journey"

        when(mockConfig.addressLookupJourneyUrl).thenReturn(s"$testAddressLookupUrl/api/v2/init")
        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.successful(Right(mockResponse)))
        when(mockResponse.header(eqTo("Location"))).thenReturn(Some(alfInitSuccessUrl))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)
        connector.getOnRampUrl(testJourneyConfig).futureValue

        verify(mockHttpClient, times(1)).post(any())(any())
        verify(mockConfig, times(1)).addressLookupJourneyUrl
      }

      "must send journey configuration in request body" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val mockResponse = mock[HttpResponse]
        val alfInitSuccessUrl = "http://localhost:9028/lookup-address/journey"

        when(mockConfig.addressLookupJourneyUrl).thenReturn(s"$testAddressLookupUrl/api/v2/init")
        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.successful(Right(mockResponse)))
        when(mockResponse.header(eqTo("Location"))).thenReturn(Some(alfInitSuccessUrl))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)
        connector.getOnRampUrl(testJourneyConfig).futureValue

        verify(mockRequestBuilder, times(1)).withBody(any())(any(), any(), any())
      }

      "must pass implicit HeaderCarrier to http client" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val mockResponse = mock[HttpResponse]
        val customHc = HeaderCarrier(sessionId = Some(uk.gov.hmrc.http.SessionId("test-session")))
        val alfInitSuccessUrl = "http://localhost:9028/lookup-address/journey"

        when(mockConfig.addressLookupJourneyUrl).thenReturn(s"$testAddressLookupUrl/api/v2/init")
        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.successful(Right(mockResponse)))
        when(mockResponse.header(eqTo("Location"))).thenReturn(Some(alfInitSuccessUrl))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)
        connector.getOnRampUrl(testJourneyConfig)(customHc, request).futureValue

        verify(mockHttpClient, times(1)).post(any())(any())
      }

      "must call execute method on request builder" in {
        val mockHttpClient = mock[HttpClientV2]
        val mockConfig = mock[FrontendAppConfig]
        val mockRequestBuilder = mock[RequestBuilder]
        val mockResponse = mock[HttpResponse]
        val alfInitSuccessUrl = "http://localhost:9028/lookup-address/journey"

        when(mockConfig.addressLookupJourneyUrl).thenReturn(s"$testAddressLookupUrl/api/v2/init")
        when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.successful(Right(mockResponse)))
        when(mockResponse.header(eqTo("Location"))).thenReturn(Some(alfInitSuccessUrl))

        val connector = new AddressLookupConnector(mockHttpClient, mockConfig)
        connector.getOnRampUrl(testJourneyConfig).futureValue

        verify(mockRequestBuilder, times(1)).execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }

      "must handle different Location header URLs" in {
        val urls = List(
          "http://localhost:9028/lookup-address/journey-1",
          "http://localhost:9028/lookup-address/custom-journey",
          "http://localhost:9028/lookup-address/test-123"
        )

        urls.foreach { alfUrl =>
          val mockHttpClient = mock[HttpClientV2]
          val mockConfig = mock[FrontendAppConfig]
          val mockRequestBuilder = mock[RequestBuilder]
          val mockResponse = mock[HttpResponse]

          when(mockConfig.addressLookupJourneyUrl).thenReturn(s"$testAddressLookupUrl/api/v2/init")
          when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
          when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
          when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
            .thenReturn(Future.successful(Right(mockResponse)))
          when(mockResponse.header(eqTo("Location"))).thenReturn(Some(alfUrl))

          val connector = new AddressLookupConnector(mockHttpClient, mockConfig)
          val result = connector.getOnRampUrl(testJourneyConfig).futureValue

          result mustBe Call("GET", alfUrl)
        }
      }
    }
  }
}