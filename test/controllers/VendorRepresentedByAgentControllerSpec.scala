package controllers

import base.SpecBase
import forms.VendorRepresentedByAgentFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.VendorRepresentedByAgentPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.VendorRepresentedByAgentView

import scala.concurrent.Future
import constants.FullReturnConstants

class VendorRepresentedByAgentControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new VendorRepresentedByAgentFormProvider()
  val form = formProvider()

  lazy val vendorRepresentedByAgentRoute: String = routes.VendorRepresentedByAgentController.onPageLoad(NormalMode).url

  "VendorRepresentedByAgent Controller" - {
    val vendorName = "TODO the vendor"

    "onPageLoad" - {
      "when no existing data is found" - {

        "must redirect to Journey Recovery for a GET if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          }
        }
      }

      "fullReturn missing" - {
        "must redirect to genericErrorPage when fullReturn is missing" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.VendorRepresentedByAgentErrorController.onPageLoad().url
          }
        }
      }

      "full return exists" - {
        "must return OK and the correct view for a GET when returnAgent field does not exist" in {

          val userAnswers = emptyUserAnswers.copy(fullReturn = Some(FullReturnConstants.minimalFullReturn))

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)
            val result = route(application, request).value

            val view = application.injector.instanceOf[VendorRepresentedByAgentView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, NormalMode, vendorName)(request, messages(application)).toString
          }
        }

        "must return OK and the correct view for a GET when returnAgent field exists but is None" in {

          val userAnswers = emptyUserAnswers.copy(fullReturn = Some(FullReturnConstants.incompleteFullReturn))

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)
            val result = route(application, request).value

            val view = application.injector.instanceOf[VendorRepresentedByAgentView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, NormalMode, vendorName)(request, messages(application)).toString
          }
        }

        "must return OK and the correct view for a GET when returnAgent list is empty" in {

          val fullReturnEmptyAgentList = FullReturnConstants.minimalFullReturn.copy(returnAgent = Some(Seq.empty))
          val userAnswers = emptyUserAnswers.copy(fullReturn = Some(fullReturnEmptyAgentList))

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)
            val result = route(application, request).value

            val view = application.injector.instanceOf[VendorRepresentedByAgentView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, NormalMode, vendorName)(request, messages(application)).toString
          }
        }

        "must return OK and the correct view for a GET when returnAgent exists but agentType is None" in {

          val fullReturnAgentTypeNone = {
            FullReturnConstants.minimalFullReturn.copy(returnAgent = Some(Seq(FullReturnConstants.completeReturnAgent.copy(agentType = None))))
          }
          val userAnswers = emptyUserAnswers.copy(fullReturn = Some(fullReturnAgentTypeNone))

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)
            val result = route(application, request).value

            val view = application.injector.instanceOf[VendorRepresentedByAgentView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, NormalMode, vendorName)(request, messages(application)).toString
          }
        }

        "must return OK and the correct view for a GET when single returnAgent exists but agentType is not VENODR" in {

          val fullReturnWithNonVendorAgent = FullReturnConstants.completeFullReturn.copy(
            returnAgent = Some(Seq(FullReturnConstants.completeReturnAgent.copy(agentType = Some("SOLICITOR"))))
          )
          val userAnswers = emptyUserAnswers.copy(fullReturn = Some(fullReturnWithNonVendorAgent))

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)
            val result = route(application, request).value

            val view = application.injector.instanceOf[VendorRepresentedByAgentView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, NormalMode, vendorName)(request, messages(application)).toString
          }
        }

        "must return OK and the correct view for a GET when multiple returnAgents exist but agentType is not VENODR" in {

          val multipleReturnAgents = Seq(
            FullReturnConstants.completeReturnAgent.copy(agentType = Some("SOLICITOR")),
            FullReturnConstants.completeReturnAgent.copy(agentType = Some("AGENT")),
            FullReturnConstants.completeReturnAgent.copy(agentType = Some(""))
          )

          val fullReturn = FullReturnConstants.completeFullReturn.copy(returnAgent = Some(multipleReturnAgents))

          val userAnswers = emptyUserAnswers.copy(fullReturn = Some(fullReturn))

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)
            val result = route(application, request).value

            val view = application.injector.instanceOf[VendorRepresentedByAgentView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form, NormalMode, vendorName)(request, messages(application)).toString
          }
        }

        "must populate the view correctly on a GET when the question has previously been answered" in {

          val vendorReturnAgent = FullReturnConstants.completeReturnAgent.copy(agentType = Some("SOLICITOR"))

          val fullReturn = FullReturnConstants.completeFullReturn.copy(returnAgent = Some(Seq(vendorReturnAgent)))

          val userAnswers = emptyUserAnswers
            .copy(fullReturn = Some(fullReturn))
            .set(VendorRepresentedByAgentPage, true).success.value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)
            val result = route(application, request).value

            val view = application.injector.instanceOf[VendorRepresentedByAgentView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form.fill(true), NormalMode, vendorName)(request, messages(application)).toString
          }
        }

        "must redirect to the error page when agentType is VENDOR and main vendor is NOT represented" in {

          val vendorReturnAgent = FullReturnConstants.completeReturnAgent.copy(agentType = Some("VENDOR"))

          val mainVendor = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN001"),
            isRepresentedByAgent = Some("false")
          )

          val fullReturn = FullReturnConstants.completeFullReturn.copy(
            returnAgent = Some(Seq(vendorReturnAgent)),
            vendor = Some(Seq(mainVendor)),
            returnInfo = Some(FullReturnConstants.completeReturnInfo.copy(mainVendorID = Some("VEN001")))
          )

          val userAnswers = emptyUserAnswers.copy(fullReturn = Some(fullReturn))

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.VendorRepresentedByAgentErrorController.onPageLoad().url
          }
        }

        "must redirect to error page when multiple returnAgents of type VENDOR and main vendor not represented" in {

          val multipleReturnAgents = Seq(
            FullReturnConstants.completeReturnAgent.copy(agentType = Some("SOLICITOR")),
            FullReturnConstants.completeReturnAgent.copy(agentType = Some("VENDOR")),
            FullReturnConstants.completeReturnAgent.copy(agentType = Some("VENDOR"))
          )

          val mainVendor = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN001"),
            isRepresentedByAgent = Some("no")
          )

          val vendorTwo = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN002"),
            isRepresentedByAgent = Some("yes")
          )

          val vendorThree = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN003"),
            isRepresentedByAgent = Some("no")
          )

          val fullReturn = FullReturnConstants.completeFullReturn.copy(
            returnAgent = Some(multipleReturnAgents),
            vendor = Some(Seq(vendorTwo, mainVendor, vendorThree)),
            returnInfo = Some(FullReturnConstants.completeReturnInfo.copy(mainVendorID = Some("VEN001")))
          )

          val userAnswers = emptyUserAnswers.copy(fullReturn = Some(fullReturn))

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.VendorRepresentedByAgentErrorController.onPageLoad().url
          }
        }

        "must redirect to the error page when agentType is VENDOR and main vendor is NOT represented multiple vendors" in {

          val vendorReturnAgent = FullReturnConstants.completeReturnAgent.copy(agentType = Some("VENDOR"))

          val mainVendor = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN001"),
            isRepresentedByAgent = Some("NO")
          )

          val vendorOne = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN002"),
            isRepresentedByAgent = Some("YES")
          )

          val vendorTwo = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN003"),
            isRepresentedByAgent = Some("YES")
          )

          val vendorThree = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN004"),
            isRepresentedByAgent = Some("YES")
          )

          val fullReturn = FullReturnConstants.completeFullReturn.copy(
            returnAgent = Some(Seq(vendorReturnAgent)),
            vendor = Some(Seq(vendorOne, vendorTwo, vendorThree, mainVendor)),
            returnInfo = Some(FullReturnConstants.completeReturnInfo.copy(mainVendorID = Some("VEN001")))
          )

          val userAnswers = emptyUserAnswers.copy(fullReturn = Some(fullReturn))

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.VendorRepresentedByAgentErrorController.onPageLoad().url
          }
        }

        "must redirect to the error page when agentType is VENDOR but main vendor ID does not match any vendors" in {

          val vendorReturnAgent = FullReturnConstants.completeReturnAgent.copy(agentType = Some("VENDOR"))

          val vendorOne = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN002"),
            isRepresentedByAgent = Some("YES")
          )

          val vendorTwo = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN003"),
            isRepresentedByAgent = Some("YES")
          )

          val vendorThree = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN004"),
            isRepresentedByAgent = Some("YES")
          )

          val fullReturn = FullReturnConstants.completeFullReturn.copy(
            returnAgent = Some(Seq(vendorReturnAgent)),
            vendor = Some(Seq(vendorOne, vendorTwo, vendorThree)),
            returnInfo = Some(FullReturnConstants.completeReturnInfo.copy(mainVendorID = Some("VEN001")))
          )

          val userAnswers = emptyUserAnswers.copy(fullReturn = Some(fullReturn))

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.VendorRepresentedByAgentErrorController.onPageLoad().url
          }
        }

        "must redirect to error page when multiple returnAgents of type VENDOR and vendor list missing" in {

          val multipleReturnAgents = Seq(
            FullReturnConstants.completeReturnAgent.copy(agentType = Some("SOLICITOR")),
            FullReturnConstants.completeReturnAgent.copy(agentType = Some("VENDOR")),
            FullReturnConstants.completeReturnAgent.copy(agentType = Some("VENDOR"))
          )

          val fullReturn = FullReturnConstants.completeFullReturn.copy(
            returnAgent = Some(multipleReturnAgents),
            vendor = None,
            returnInfo = Some(FullReturnConstants.completeReturnInfo.copy(mainVendorID = Some("VEN001")))
          )

          val userAnswers = emptyUserAnswers.copy(fullReturn = Some(fullReturn))

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.VendorRepresentedByAgentErrorController.onPageLoad().url
          }
        }

        "must redirect to the error page when agentType is VENDOR but main vendor ID equals None" in {

          val vendorReturnAgent = FullReturnConstants.completeReturnAgent.copy(agentType = Some("VENDOR"))

          val vendorOne = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN002"),
            isRepresentedByAgent = Some("YES")
          )

          val vendorTwo = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN003"),
            isRepresentedByAgent = Some("YES")
          )

          val vendorThree = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN004"),
            isRepresentedByAgent = Some("YES")
          )

          val fullReturn = FullReturnConstants.completeFullReturn.copy(
            returnAgent = Some(Seq(vendorReturnAgent)),
            vendor = Some(Seq(vendorOne, vendorTwo, vendorThree)),
            returnInfo = Some(FullReturnConstants.completeReturnInfo.copy(mainVendorID = None))
          )

          val userAnswers = emptyUserAnswers.copy(fullReturn = Some(fullReturn))

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.VendorRepresentedByAgentErrorController.onPageLoad().url
          }
        }

        "must redirect to check your answers when agentType is VENDOR and main vendor is represented" in {

          val vendorReturnAgent = FullReturnConstants.completeReturnAgent.copy(agentType = Some("VENDOR"))

          val mainVendor = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN001"),
            isRepresentedByAgent = Some("YES")
          )

          val fullReturn = FullReturnConstants.completeFullReturn.copy(
            returnAgent = Some(Seq(vendorReturnAgent)),
            vendor = Some(Seq(mainVendor)),
            returnInfo = Some(FullReturnConstants.completeReturnInfo.copy(mainVendorID = Some("VEN001")))
          )

          val userAnswers = emptyUserAnswers.copy(fullReturn = Some(fullReturn))

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.IndexController.onPageLoad().url
          }
        }

        "must redirect to check your answers when agentType is VENDOR for the main vendor and is represented multiple vendors" in {

          val vendorReturnAgent = FullReturnConstants.completeReturnAgent.copy(agentType = Some("VENDOR"))

          val mainVendor = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN001"),
            isRepresentedByAgent = Some("yes")
          )

          val vendorOne = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN002"),
            isRepresentedByAgent = Some("no")
          )

          val vendorTwo = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN003"),
            isRepresentedByAgent = Some("YES")
          )

          val vendorThree = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN004"),
            isRepresentedByAgent = Some("YES")
          )

          val fullReturn = FullReturnConstants.completeFullReturn.copy(
            returnAgent = Some(Seq(vendorReturnAgent)),
            vendor = Some(Seq(vendorOne, vendorTwo, vendorThree, mainVendor)),
            returnInfo = Some(FullReturnConstants.completeReturnInfo.copy(mainVendorID = Some("VEN001")))
          )

          val userAnswers = emptyUserAnswers.copy(fullReturn = Some(fullReturn))

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.IndexController.onPageLoad().url
          }
        }

        "must redirect to check your answers when main vendor is represented even if vendor list contains duplicates" in {

          val vendorReturnAgent = FullReturnConstants.completeReturnAgent.copy(agentType = Some("VENDOR"))

          val mainVendor = FullReturnConstants.completeVendor.copy(
            vendorID = Some("VEN001"),
            isRepresentedByAgent = Some("YES")
          )

          val fullReturn = FullReturnConstants.completeFullReturn.copy(
            returnAgent = Some(Seq(vendorReturnAgent)),
            vendor = Some(Seq(mainVendor, mainVendor, mainVendor)),
            returnInfo = Some(FullReturnConstants.completeReturnInfo.copy(mainVendorID = Some("VEN001")))
          )

          val userAnswers = emptyUserAnswers.copy(fullReturn = Some(fullReturn))

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, vendorRepresentedByAgentRoute)
            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.IndexController.onPageLoad().url
          }
        }

      }
    }

      "onSubmit" - {

        "when valid data is submitted" - {

          "must redirect to the next page when 'yes' is selected" in {

            val mockSessionRepository = mock[SessionRepository]

            when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers))
                .overrides(
                  bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
                  bind[SessionRepository].toInstance(mockSessionRepository)
                )
                .build()

            running(application) {
              val request =
                FakeRequest(POST, vendorRepresentedByAgentRoute)
                  .withFormUrlEncodedBody(("value", "true"))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual onwardRoute.url
            }
          }

          "must redirect to check your answers when 'no' is selected" in {

            val mockSessionRepository = mock[SessionRepository]

            when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

            val application =
              applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

            running(application) {
              val request =
                FakeRequest(POST, vendorRepresentedByAgentRoute)
                  .withFormUrlEncodedBody(("value", "false"))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.IndexController.onPageLoad().url
            }
          }

        }

        "when invalid data is submitted" - {

          "must return a Bad Request and errors when invalid data is submitted" in {

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

            running(application) {
              val request =
                FakeRequest(POST, vendorRepresentedByAgentRoute)
                  .withFormUrlEncodedBody(("value", ""))

              val boundForm = form.bind(Map("value" -> ""))

              val view = application.injector.instanceOf[VendorRepresentedByAgentView]

              val result = route(application, request).value

              status(result) mustEqual BAD_REQUEST
              contentAsString(result) mustEqual view(boundForm, NormalMode, vendorName)(request, messages(application)).toString
            }
          }

        }

        "when no existing data is found" - {

          "must redirect to Journey Recovery for a POST if no existing data is found" in {

            val application = applicationBuilder(userAnswers = None).build()

            running(application) {
              val request =
                FakeRequest(POST, vendorRepresentedByAgentRoute)
                  .withFormUrlEncodedBody(("value", "true"))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
            }
          }

        }
      }
    }
  }