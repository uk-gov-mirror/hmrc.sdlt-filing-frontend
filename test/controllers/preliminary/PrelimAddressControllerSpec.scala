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

import base.SpecBase
import models.address.{Address, Country}
import models.NormalMode
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AddressLookupService

import scala.concurrent.Future

class PrelimAddressControllerSpec extends SpecBase with MockitoSugar {

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

  val testAddressLookupCall: Call = Call("GET", "http://localhost:9028/lookup-address/journey")

  lazy val redirectToAddressLookupRoute = controllers.preliminary.routes.PrelimAddressController.redirectToAddressLookup().url
  lazy val redirectToAddressLookupChangeRoute = controllers.preliminary.routes.PrelimAddressController.redirectToAddressLookup(changeRoute = Some("change")).url
  lazy val addressLookupCallbackRoute = controllers.preliminary.routes.PrelimAddressController.addressLookupCallback(id = "test-id").url
  lazy val addressLookupCallbackChangeRoute = controllers.preliminary.routes.PrelimAddressController.addressLookupCallbackChange("test-id").url

  "PrelimAddressController" - {

    "redirectToAddressLookup" - {

      "must redirect to address lookup service when no changeRoute is provided" in {
        val mockAddressLookupService = mock[AddressLookupService]

        when(mockAddressLookupService.getJourneyUrl(any(), any(), any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(testAddressLookupCall))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, redirectToAddressLookupRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual testAddressLookupCall.url

          verify(mockAddressLookupService, times(1))
            .getJourneyUrl(any(), eqTo(controllers.preliminary.routes.PrelimAddressController.addressLookupCallback()), eqTo(true), any(), any())(any(), any(), any())
        }
      }

      "must redirect to address lookup service with change callback when changeRoute is provided" in {
        val mockAddressLookupService = mock[AddressLookupService]

        when(mockAddressLookupService.getJourneyUrl(any(), any(), any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(testAddressLookupCall))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, redirectToAddressLookupChangeRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual testAddressLookupCall.url

          verify(mockAddressLookupService, times(1))
            .getJourneyUrl(any(), eqTo(controllers.preliminary.routes.PrelimAddressController.addressLookupCallbackChange()), eqTo(true), any(), any())(any(), any(), any())
        }
      }

      "must redirect to Journey Recovery when no existing data is found" in {
        val mockAddressLookupService = mock[AddressLookupService]

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, redirectToAddressLookupRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must handle service failure when getting journey URL" in {
        val mockAddressLookupService = mock[AddressLookupService]

        when(mockAddressLookupService.getJourneyUrl(any(), any(), any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Service unavailable")))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, redirectToAddressLookupRoute)

          val result = route(application, request).value

          whenReady(result.failed) { exception =>
            exception mustBe a[RuntimeException]
            exception.getMessage mustBe "Service unavailable"
          }
        }
      }
    }

    "addressLookupCallback" - {

      "must redirect to TransactionTypeController when address is successfully saved" in {
        val mockAddressLookupService = mock[AddressLookupService]

        when(mockAddressLookupService.getAddressById(eqTo("test-id"))(any()))
          .thenReturn(Future.successful(testAddress))
        when(mockAddressLookupService.saveAddressDetails(any())(any(), any()))
          .thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, addressLookupCallbackRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.TransactionTypeController.onPageLoad(NormalMode).url

          verify(mockAddressLookupService, times(1)).getAddressById(eqTo("test-id"))(any())
          verify(mockAddressLookupService, times(1)).saveAddressDetails(any())(any(), any())
        }
      }

      "must redirect to CheckYourAnswers when address save fails" in {
        val mockAddressLookupService = mock[AddressLookupService]

        when(mockAddressLookupService.getAddressById(eqTo("test-id"))(any()))
          .thenReturn(Future.successful(testAddress))
        when(mockAddressLookupService.saveAddressDetails(any())(any(), any()))
          .thenReturn(Future.successful(false))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, addressLookupCallbackRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must handle failure when getting address by ID" in {
        val mockAddressLookupService = mock[AddressLookupService]

        when(mockAddressLookupService.getAddressById(eqTo("test-id"))(any()))
          .thenReturn(Future.failed(new RuntimeException("Address not found")))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, addressLookupCallbackRoute)

          val result = route(application, request).value

          whenReady(result.failed) { exception =>
            exception mustBe a[RuntimeException]
            exception.getMessage mustBe "Address not found"
          }
        }
      }

      "must handle failure when saving address details" in {
        val mockAddressLookupService = mock[AddressLookupService]

        when(mockAddressLookupService.getAddressById(eqTo("test-id"))(any()))
          .thenReturn(Future.successful(testAddress))
        when(mockAddressLookupService.saveAddressDetails(any())(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Database error")))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, addressLookupCallbackRoute)

          val result = route(application, request).value

          whenReady(result.failed) { exception =>
            exception mustBe a[RuntimeException]
            exception.getMessage mustBe "Database error"
          }
        }
      }

      "must redirect to Journey Recovery when no existing data is found" in {
        val mockAddressLookupService = mock[AddressLookupService]

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, addressLookupCallbackRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must handle different address IDs" in {
        val addressIds = List("id-123", "ABC-XYZ", "test-address-id")

        addressIds.foreach { addressId =>
          val mockAddressLookupService = mock[AddressLookupService]

          when(mockAddressLookupService.getAddressById(eqTo(addressId))(any()))
            .thenReturn(Future.successful(testAddress))
          when(mockAddressLookupService.saveAddressDetails(any())(any(), any()))
            .thenReturn(Future.successful(true))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[AddressLookupService].toInstance(mockAddressLookupService)
            )
            .build()

          running(application) {
            val callbackRoute = controllers.preliminary.routes.PrelimAddressController.addressLookupCallback(addressId).url
            val request = FakeRequest(GET, callbackRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.TransactionTypeController.onPageLoad(NormalMode).url

            verify(mockAddressLookupService, times(1)).getAddressById(eqTo(addressId))(any())
          }
        }
      }
    }

    "addressLookupCallbackChange" - {

      "must redirect to CheckYourAnswers when address is successfully saved" in {
        val mockAddressLookupService = mock[AddressLookupService]

        when(mockAddressLookupService.getAddressById(eqTo("test-id"))(any()))
          .thenReturn(Future.successful(testAddress))
        when(mockAddressLookupService.saveAddressDetails(any())(any(), any()))
          .thenReturn(Future.successful(true))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, addressLookupCallbackChangeRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.CheckYourAnswersController.onPageLoad().url

          verify(mockAddressLookupService, times(1)).getAddressById(eqTo("test-id"))(any())
          verify(mockAddressLookupService, times(1)).saveAddressDetails(any())(any(), any())
        }
      }

      "must redirect to CheckYourAnswers when address save fails" in {
        val mockAddressLookupService = mock[AddressLookupService]

        when(mockAddressLookupService.getAddressById(eqTo("test-id"))(any()))
          .thenReturn(Future.successful(testAddress))
        when(mockAddressLookupService.saveAddressDetails(any())(any(), any()))
          .thenReturn(Future.successful(false))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, addressLookupCallbackChangeRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must handle failure when getting address by ID" in {
        val mockAddressLookupService = mock[AddressLookupService]

        when(mockAddressLookupService.getAddressById(eqTo("test-id"))(any()))
          .thenReturn(Future.failed(new RuntimeException("Address not found")))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, addressLookupCallbackChangeRoute)

          val result = route(application, request).value

          whenReady(result.failed) { exception =>
            exception mustBe a[RuntimeException]
            exception.getMessage mustBe "Address not found"
          }
        }
      }

      "must handle failure when saving address details" in {
        val mockAddressLookupService = mock[AddressLookupService]

        when(mockAddressLookupService.getAddressById(eqTo("test-id"))(any()))
          .thenReturn(Future.successful(testAddress))
        when(mockAddressLookupService.saveAddressDetails(any())(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Database error")))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, addressLookupCallbackChangeRoute)

          val result = route(application, request).value

          whenReady(result.failed) { exception =>
            exception mustBe a[RuntimeException]
            exception.getMessage mustBe "Database error"
          }
        }
      }

      "must redirect to Journey Recovery when no existing data is found" in {
        val mockAddressLookupService = mock[AddressLookupService]

        val application = applicationBuilder(userAnswers = None)
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, addressLookupCallbackChangeRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must handle different address IDs" in {
        val addressIds = List("id-123", "ABC-XYZ", "test-address-id")

        addressIds.foreach { addressId =>
          val mockAddressLookupService = mock[AddressLookupService]

          when(mockAddressLookupService.getAddressById(eqTo(addressId))(any()))
            .thenReturn(Future.successful(testAddress))
          when(mockAddressLookupService.saveAddressDetails(any())(any(), any()))
            .thenReturn(Future.successful(true))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[AddressLookupService].toInstance(mockAddressLookupService)
            )
            .build()

          running(application) {
            val callbackChangeRoute = controllers.preliminary.routes.PrelimAddressController.addressLookupCallbackChange(addressId).url
            val request = FakeRequest(GET, callbackChangeRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.CheckYourAnswersController.onPageLoad().url

            verify(mockAddressLookupService, times(1)).getAddressById(eqTo(addressId))(any())
          }
        }
      }
    }
  }
}
