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

package controllers

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency {

  "Check Your Answers Controller" - {

    "must return OK and the correct view for a GET" - {
      "when the UserAnswers is empty" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CheckYourAnswersView]

          val purchaserType = ValueViewModel(
            HtmlContent(
              s"""<a href="/stamp-duty-land-tax-filing/preliminary-questions/who-is-making-the-purchase" class="govuk-link">Enter purchaser type</a>""")
          )
          val purchaserName = ValueViewModel(
            HtmlContent(
              s"""<a href="/stamp-duty-land-tax-filing/preliminary-questions/purchaser-name" class="govuk-link">Enter purchaser name</a>""")
          )
          val propertyAddress = ValueViewModel(
            HtmlContent(
              s"""<a href="/stamp-duty-land-tax-filing/preliminary-questions/address" class="govuk-link">Enter property address</a>""")
          )
          val transactionType = ValueViewModel(
            HtmlContent(
              s"""<a href="/stamp-duty-land-tax-filing/preliminary-questions/transaction-type" class="govuk-link">Enter transaction type</a>""")
          )

          val list = SummaryListViewModel(
            rows = Seq(
              SummaryListRowViewModel(
                key = Key(HtmlContent("Purchaser type")),
                value = purchaserType
              ),
              SummaryListRowViewModel(
                key = Key(HtmlContent("Purchaser name")),
                value = purchaserName
              ),
              SummaryListRowViewModel(
                key = Key(HtmlContent("Property address")),
                value = propertyAddress
              ),
              SummaryListRowViewModel(
                key = Key(HtmlContent("Transaction type")),
                value = transactionType
              )
            )
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(list)(request, messages(application)).toString
        }
      }
    }


    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
