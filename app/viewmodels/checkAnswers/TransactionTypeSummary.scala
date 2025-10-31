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

import controllers.routes
import models.{CheckMode, NormalMode, UserAnswers}
import pages.TransactionTypePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object TransactionTypeSummary  {

  def row(answers: Option[UserAnswers])(implicit messages: Messages): SummaryListRow =
    answers.flatMap(_.get(TransactionTypePage)).map {
      answer =>

        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages(s"transactionType.$answer"))
          )
        )

        SummaryListRowViewModel(
          key     = "transactionType.checkYourAnswersLabel",
          value   = value,
          actions = Seq(
            ActionItemViewModel("site.change", routes.TransactionTypeController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("transactionType.change.hidden"))
          )
        )
    }.getOrElse{

      val value = ValueViewModel(
        HtmlContent(
          s"""<a href="${routes.TransactionTypeController.onPageLoad(CheckMode).url}" class="govuk-link">${messages("transactionType.link.message")}</a>""")
      )

      SummaryListRowViewModel(
        key = "transactionType.checkYourAnswersLabel",
        value = value
      )
    }
}
