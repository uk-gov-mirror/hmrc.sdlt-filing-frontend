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

package viewmodels.checkAnswers

import controllers.preliminary.routes
import models.UserAnswers
import models.address.{Address, Country}
import pages.PurchaserAddressPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object PrelimAddressSummary {
  def row(answers: Option[UserAnswers])(implicit messages: Messages): SummaryListRow =
    answers.flatMap(_.get(PurchaserAddressPage)).map { answer =>
      
      val listOfAddressDetails = List(
        answer.line1,
        answer.line2,
        answer.line3,
        answer.line4,
        answer.line5,
        answer.postcode,
        answer.country
      )

      val list = listOfAddressDetails.collect {
        case Some(Country(Some(code), Some(name))) => name
        case Some(detail) => detail
        case detail => detail
      }.filter(x => x != None)

      val prelimAddressString = list.mkString(", ")

      val value = ValueViewModel(
        HtmlContent(HtmlFormat.escape(prelimAddressString))
      )

      SummaryListRowViewModel(
        key = "purchaserAddress.checkYourAnswersLabel",
        value = value,
        actions = Seq(
          ActionItemViewModel("site.change", routes.PrelimAddressController.redirectToAddressLookup(Some("change")).url)
            .withVisuallyHiddenText(messages("purchaserAddress.change.hidden"))
        )
      )
    }.getOrElse{

      val value = ValueViewModel(
        HtmlContent(
          s"""<a href="${routes.PrelimAddressController.redirectToAddressLookup().url}" class="govuk-link">${messages("purchaserAddress.link.message")}</a>""")
      )
      
      SummaryListRowViewModel(
        key = "purchaserAddress.checkYourAnswersLabel",
        value = value
      )
    }
}
