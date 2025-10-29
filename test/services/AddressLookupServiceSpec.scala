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

import base.SpecBase
import config.AddressLookupConfiguration
import connectors.AddressLookupConnector
import models.address.{Address, AddressLookupJourneyIdentifier, Country, MandatoryFieldsConfigModel}
import models.requests.DataRequest
import models.UserAnswers
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.PurchaserAddressPage
import play.api.mvc.Call
import play.api.test.FakeRequest
import repositories.SessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class AddressLookupServiceSpec extends SpecBase with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val request: FakeRequest[_] = FakeRequest()

  val testAddress: Address = Address(
    "16 Coniston Court",
    Some("Holland road"),
    None,
    None,
    None,
    Some("BN3 1JU"),
    Some(Country(Some("UK"), Some("United Kingdom"))),
    false
  )

  val testCall: Call = Call("GET", "http://localhost:9028/lookup-address/journey")

  val testMandatoryFieldsConfigModel = MandatoryFieldsConfigModel(line1 = Some(true),
    line2 = Some(true),
    line3 = Some(true),
    town = Some(true),
    postcode = Some(true))

  "AddressLookupService" - {

    "getAddressById" - {

      "must return Address when connector returns successfully" in {
        val mockConnector = mock[AddressLookupConnector]
        val mockAlfConfig = mock[AddressLookupConfiguration]
        val mockSessionRepository = mock[SessionRepository]
        val testId = "test-address-id"

        when(mockConnector.getAddress(eqTo(testId))(any()))
          .thenReturn(Future.successful(testAddress))

        val service = new AddressLookupService(mockConnector, mockAlfConfig, mockSessionRepository)
        val result = service.getAddressById(testId).futureValue

        result mustBe testAddress
        verify(mockConnector, times(1)).getAddress(eqTo(testId))(any())
      }

      "must handle connector failure when address lookup fails" in {
        val mockConnector = mock[AddressLookupConnector]
        val mockAlfConfig = mock[AddressLookupConfiguration]
        val mockSessionRepository = mock[SessionRepository]
        val testId = "invalid-id"

        when(mockConnector.getAddress(eqTo(testId))(any()))
          .thenReturn(Future.failed(new UpstreamErrorResponse("Address not found", 400, 400, Map())))

        val service = new AddressLookupService(mockConnector, mockAlfConfig, mockSessionRepository)

        whenReady(service.getAddressById(testId).failed) { exception =>
          exception mustBe a[UpstreamErrorResponse]
          exception.getMessage mustBe "Address not found"
        }

        verify(mockConnector, times(1)).getAddress(eqTo(testId))(any())
      }

      "must call connector with correct addressId" in {
        val mockConnector = mock[AddressLookupConnector]
        val mockAlfConfig = mock[AddressLookupConfiguration]
        val mockSessionRepository = mock[SessionRepository]
        val testId = "ABC-123-XYZ"

        when(mockConnector.getAddress(eqTo(testId))(any()))
          .thenReturn(Future.successful(testAddress))

        val service = new AddressLookupService(mockConnector, mockAlfConfig, mockSessionRepository)
        service.getAddressById(testId).futureValue

        verify(mockConnector, times(1)).getAddress(eqTo(testId))(any())
      }

      "must handle different address ID formats" in {
        val mockConnector = mock[AddressLookupConnector]
        val mockAlfConfig = mock[AddressLookupConfiguration]
        val mockSessionRepository = mock[SessionRepository]
        val addressIds = List("123", "ABC-123", "test-address-id-with-dashes")

        addressIds.foreach { testId =>
          when(mockConnector.getAddress(eqTo(testId))(any()))
            .thenReturn(Future.successful(testAddress))

          val service = new AddressLookupService(mockConnector, mockAlfConfig, mockSessionRepository)
          val result = service.getAddressById(testId).futureValue

          result mustBe testAddress
          verify(mockConnector, times(1)).getAddress(eqTo(testId))(any())
          reset(mockConnector)
        }
      }

      "must handle empty address ID" in {
        val mockConnector = mock[AddressLookupConnector]
        val mockAlfConfig = mock[AddressLookupConfiguration]
        val mockSessionRepository = mock[SessionRepository]
        val testId = ""

        when(mockConnector.getAddress(eqTo(testId))(any()))
          .thenReturn(Future.failed(new IllegalArgumentException("Address ID cannot be empty")))

        val service = new AddressLookupService(mockConnector, mockAlfConfig, mockSessionRepository)

        whenReady(service.getAddressById(testId).failed) { exception =>
          exception mustBe an[IllegalArgumentException]
        }
      }
    }

    "getJourneyUrl" - {

      val journeyId = AddressLookupJourneyIdentifier.prelimQuestionsAddress
      val continueUrl = Call("GET", "/continue-url")

      "must return Call when connector returns successfully with default parameters" in {
        val mockConnector = mock[AddressLookupConnector]
        val mockAlfConfig = mock[AddressLookupConfiguration]
        val mockSessionRepository = mock[SessionRepository]
        val mockConfigModel = mock[models.address.AddressLookupConfigurationModel]

        when(mockAlfConfig.apply(eqTo(journeyId), eqTo(continueUrl), eqTo(false), eqTo(None), any())(any()))
          .thenReturn(mockConfigModel)
        when(mockConnector.getOnRampUrl(eqTo(mockConfigModel))(any(), any()))
          .thenReturn(Future.successful(testCall))

        val service = new AddressLookupService(mockConnector, mockAlfConfig, mockSessionRepository)
        val result = service.getJourneyUrl(journeyId, continueUrl, mandatoryFieldsConfigModel = testMandatoryFieldsConfigModel).futureValue

        result mustBe testCall
        verify(mockAlfConfig, times(1)).apply(eqTo(journeyId), eqTo(continueUrl), eqTo(false), eqTo(None), any())(any())
        verify(mockConnector, times(1)).getOnRampUrl(eqTo(mockConfigModel))(any(), any())
      }

      "must return Call when useUkMode is true" in {
        val mockConnector = mock[AddressLookupConnector]
        val mockAlfConfig = mock[AddressLookupConfiguration]
        val mockSessionRepository = mock[SessionRepository]
        val mockConfigModel = mock[models.address.AddressLookupConfigurationModel]

        when(mockAlfConfig.apply(eqTo(journeyId), eqTo(continueUrl), eqTo(true), eqTo(None), any())(any()))
          .thenReturn(mockConfigModel)
        when(mockConnector.getOnRampUrl(eqTo(mockConfigModel))(any(), any()))
          .thenReturn(Future.successful(testCall))

        val service = new AddressLookupService(mockConnector, mockAlfConfig, mockSessionRepository)
        val result = service.getJourneyUrl(journeyId, continueUrl, useUkMode = true, mandatoryFieldsConfigModel = testMandatoryFieldsConfigModel).futureValue

        result mustBe testCall
        verify(mockAlfConfig, times(1)).apply(eqTo(journeyId), eqTo(continueUrl), eqTo(true), eqTo(None), any())(any())
      }

      "must return Call when optName is provided" in {
        val mockConnector = mock[AddressLookupConnector]
        val mockAlfConfig = mock[AddressLookupConfiguration]
        val mockSessionRepository = mock[SessionRepository]
        val mockConfigModel = mock[models.address.AddressLookupConfigurationModel]
        val testName = Some("John Doe")

        when(mockAlfConfig.apply(eqTo(journeyId), eqTo(continueUrl), eqTo(false), eqTo(testName), any())(any()))
          .thenReturn(mockConfigModel)
        when(mockConnector.getOnRampUrl(eqTo(mockConfigModel))(any(), any()))
          .thenReturn(Future.successful(testCall))

        val service = new AddressLookupService(mockConnector, mockAlfConfig, mockSessionRepository)
        val result = service.getJourneyUrl(journeyId, continueUrl, optName = testName, mandatoryFieldsConfigModel = testMandatoryFieldsConfigModel).futureValue

        result mustBe testCall
        verify(mockAlfConfig, times(1)).apply(eqTo(journeyId), eqTo(continueUrl), eqTo(false), eqTo(testName), any())(any())
      }

      "must return Call with all optional parameters provided" in {
        val mockConnector = mock[AddressLookupConnector]
        val mockAlfConfig = mock[AddressLookupConfiguration]
        val mockSessionRepository = mock[SessionRepository]
        val mockConfigModel = mock[models.address.AddressLookupConfigurationModel]
        val testName = Some("Jane Smith")
        val mockMandatoryFieldsConfigModel = mock[models.address.MandatoryFieldsConfigModel]

        when(mockAlfConfig.apply(eqTo(journeyId), eqTo(continueUrl), eqTo(true), eqTo(testName), eqTo(testMandatoryFieldsConfigModel))(any()))
          .thenReturn(mockConfigModel)
        when(mockConnector.getOnRampUrl(eqTo(mockConfigModel))(any(), any()))
          .thenReturn(Future.successful(testCall))

        val service = new AddressLookupService(mockConnector, mockAlfConfig, mockSessionRepository)
        val result = service.getJourneyUrl(journeyId, continueUrl, useUkMode = true, optName = testName, mandatoryFieldsConfigModel = testMandatoryFieldsConfigModel).futureValue

        result mustBe testCall
        verify(mockAlfConfig, times(1)).apply(eqTo(journeyId), eqTo(continueUrl), eqTo(true), eqTo(testName), eqTo(testMandatoryFieldsConfigModel))(any())
        verify(mockConnector, times(1)).getOnRampUrl(eqTo(mockConfigModel))(any(), any())
      }

      "must handle connector failure when getting on-ramp URL fails" in {
        val mockConnector = mock[AddressLookupConnector]
        val mockAlfConfig = mock[AddressLookupConfiguration]
        val mockSessionRepository = mock[SessionRepository]
        val mockConfigModel = mock[models.address.AddressLookupConfigurationModel]

        when(mockAlfConfig.apply(eqTo(journeyId), eqTo(continueUrl), eqTo(false), eqTo(None), any())(any()))
          .thenReturn(mockConfigModel)
        when(mockConnector.getOnRampUrl(eqTo(mockConfigModel))(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("ALF service unavailable")))

        val service = new AddressLookupService(mockConnector, mockAlfConfig, mockSessionRepository)

        whenReady(service.getJourneyUrl(journeyId, continueUrl, mandatoryFieldsConfigModel = testMandatoryFieldsConfigModel).failed) { exception =>
          exception mustBe a[RuntimeException]
          exception.getMessage mustBe "ALF service unavailable"
        }

        verify(mockConnector, times(1)).getOnRampUrl(eqTo(mockConfigModel))(any(), any())
      }

      "must handle different journey identifiers" in {
        val mockConnector = mock[AddressLookupConnector]
        val mockAlfConfig = mock[AddressLookupConfiguration]
        val mockSessionRepository = mock[SessionRepository]
        val mockConfigModel = mock[models.address.AddressLookupConfigurationModel]

        val journeyIds = AddressLookupJourneyIdentifier.values.toList

        journeyIds.foreach { testJourneyId =>
          when(mockAlfConfig.apply(eqTo(testJourneyId), eqTo(continueUrl), eqTo(false), eqTo(None), any())(any()))
            .thenReturn(mockConfigModel)
          when(mockConnector.getOnRampUrl(eqTo(mockConfigModel))(any(), any()))
            .thenReturn(Future.successful(testCall))

          val service = new AddressLookupService(mockConnector, mockAlfConfig, mockSessionRepository)
          val result = service.getJourneyUrl(testJourneyId, continueUrl, mandatoryFieldsConfigModel = testMandatoryFieldsConfigModel).futureValue

          result mustBe testCall
          verify(mockAlfConfig, times(1)).apply(eqTo(testJourneyId), eqTo(continueUrl), eqTo(false), eqTo(None), any())(any())
          reset(mockAlfConfig, mockConnector)
        }
      }
    }

    "saveAddressDetails" - {

      val emptyUserAnswers = UserAnswers("test-id")

      "must save address to session repository when successful" in {
        val mockConnector = mock[AddressLookupConnector]
        val mockAlfConfig = mock[AddressLookupConfiguration]
        val mockSessionRepository = mock[SessionRepository]

        val dataRequest = DataRequest(FakeRequest(), "test-id", emptyUserAnswers)
        val updatedAnswers = emptyUserAnswers.set(PurchaserAddressPage, testAddress).success.value

        when(mockSessionRepository.set(eqTo(updatedAnswers)))
          .thenReturn(Future.successful(true))

        val service = new AddressLookupService(mockConnector, mockAlfConfig, mockSessionRepository)
        val result = service.saveAddressDetails(testAddress)(dataRequest, ec).futureValue

        result mustBe true
        verify(mockSessionRepository, times(1)).set(any())
      }

      "must return false when session repository fails to save" in {
        val mockConnector = mock[AddressLookupConnector]
        val mockAlfConfig = mock[AddressLookupConfiguration]
        val mockSessionRepository = mock[SessionRepository]

        val dataRequest = DataRequest(FakeRequest(), "test-id", emptyUserAnswers)

        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(false))

        val service = new AddressLookupService(mockConnector, mockAlfConfig, mockSessionRepository)
        val result = service.saveAddressDetails(testAddress)(dataRequest, ec).futureValue

        result mustBe false
        verify(mockSessionRepository, times(1)).set(any())
      }

      "must handle session repository failure" in {
        val mockConnector = mock[AddressLookupConnector]
        val mockAlfConfig = mock[AddressLookupConfiguration]
        val mockSessionRepository = mock[SessionRepository]

        val dataRequest = DataRequest(FakeRequest(), "test-id", emptyUserAnswers)

        when(mockSessionRepository.set(any()))
          .thenReturn(Future.failed(new RuntimeException("Database connection failed")))

        val service = new AddressLookupService(mockConnector, mockAlfConfig, mockSessionRepository)

        whenReady(service.saveAddressDetails(testAddress)(dataRequest, ec).failed) { exception =>
          exception mustBe a[RuntimeException]
          exception.getMessage mustBe "Database connection failed"
        }

        verify(mockSessionRepository, times(1)).set(any())
      }

      "must update existing address in user answers" in {
        val mockConnector = mock[AddressLookupConnector]
        val mockAlfConfig = mock[AddressLookupConfiguration]
        val mockSessionRepository = mock[SessionRepository]

        val existingAddress = testAddress.copy(postcode = Some("W1A 1AA"))
        val userAnswersWithAddress = emptyUserAnswers.set(PurchaserAddressPage, existingAddress).success.value
        val dataRequest = DataRequest(FakeRequest(), "test-id", userAnswersWithAddress)

        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        val service = new AddressLookupService(mockConnector, mockAlfConfig, mockSessionRepository)
        val result = service.saveAddressDetails(testAddress)(dataRequest, ec).futureValue

        result mustBe true
        verify(mockSessionRepository, times(1)).set(any())
      }

      "must save address with different field combinations" in {
        val mockConnector = mock[AddressLookupConnector]
        val mockAlfConfig = mock[AddressLookupConfiguration]
        val mockSessionRepository = mock[SessionRepository]

        val addresses = List(
          testAddress,
          testAddress.copy(line2 = None),
          testAddress.copy(postcode = None),
          testAddress.copy(country = None),
          Address("Simple Line", None, None, None, None, None, None, false)
        )

        addresses.foreach { address =>
          val dataRequest = DataRequest(FakeRequest(), "test-id", emptyUserAnswers)

          when(mockSessionRepository.set(any()))
            .thenReturn(Future.successful(true))

          val service = new AddressLookupService(mockConnector, mockAlfConfig, mockSessionRepository)
          val result = service.saveAddressDetails(address)(dataRequest, ec).futureValue

          result mustBe true
          verify(mockSessionRepository, times(1)).set(any())
          reset(mockSessionRepository)
        }
      }

      "must handle failure when setting address in user answers fails" in {
        val mockConnector = mock[AddressLookupConnector]
        val mockAlfConfig = mock[AddressLookupConfiguration]
        val mockSessionRepository = mock[SessionRepository]

        val dataRequest = DataRequest(FakeRequest(), "test-id", emptyUserAnswers)

        when(mockSessionRepository.set(any()))
          .thenReturn(Future.successful(true))

        val service = new AddressLookupService(mockConnector, mockAlfConfig, mockSessionRepository)
        val result = service.saveAddressDetails(testAddress)(dataRequest, ec).futureValue

        result mustBe true
      }
    }
  }
}