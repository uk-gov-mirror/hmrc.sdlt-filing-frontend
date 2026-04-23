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

package viewmodels.checkAnswers.preliminary

import models.{CheckMode, UserAnswers}
import pages.preliminary.PurchaserIsIndividualPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*
import viewmodels.checkAnswers.summary.SummaryRowResult
import viewmodels.checkAnswers.summary.SummaryRowResult.{Missing, Row}

object PurchaserIsIndividualSummary {

  def row(answers: Option[UserAnswers])(implicit messages: Messages): SummaryRowResult =
    answers.flatMap(_.get(PurchaserIsIndividualPage)).map { answer =>

      val answerText = answer.toString match {
        case "Individual" => messages("prelim.purchaserIsIndividual.individual.value")
        case _  => messages("prelim.purchaserIsIndividual.company.value")
      }

      val value = ValueViewModel(
        HtmlContent(answerText)
      )

      Row(
        SummaryListRowViewModel(
          key = "prelim.purchaserIsIndividual.checkYourAnswersLabel",
          value = value,
          actions = Seq(
            ActionItemViewModel("site.change", controllers.preliminary.routes.PurchaserIsIndividualController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("prelim.purchaserIsIndividual.change.hidden"))
          )
        )
      )
    }.getOrElse {
      Missing(controllers.preliminary.routes.PurchaserIsIndividualController.onPageLoad(CheckMode))
    }
}
