/*
 * Copyright 2026 HM Revenue & Customs
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

package viewmodels.checkAnswers.purchaserAgent

import models.{CheckMode, UserAnswers}
import pages.purchaserAgent.{PurchaserAgentAuthorisedPage, PurchaserAgentNamePage}
import play.api.i18n.Messages
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*
import viewmodels.checkAnswers.summary.SummaryRowResult
import viewmodels.checkAnswers.summary.SummaryRowResult.{Missing, Row}

object PurchaserAgentAuthorisedSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): SummaryRowResult = {
    val changeRoute = controllers.purchaserAgent.routes.PurchaserAgentAuthorisedController.onPageLoad(CheckMode).url
    val nameOfPurchaserAgent = answers.get(PurchaserAgentNamePage).getOrElse(messages("site.agent.theAgent"))
    val label = messages("purchaserAgent.purchaserAgentAuthorised.checkYourAnswersLabel", nameOfPurchaserAgent)

    answers.get(PurchaserAgentAuthorisedPage).map {
      answer =>

        val value = ValueViewModel(
          if (answer) "site.yes" else "site.no"
        )

        Row(
          SummaryListRowViewModel(
            key = label,
            value = value,
            actions = Seq(
              ActionItemViewModel("site.change", changeRoute)
                .withVisuallyHiddenText(messages("purchaserAgent.purchaserAgentAuthorised.change.hidden", nameOfPurchaserAgent))
            )
          )
        )
    }.getOrElse {
      Missing(controllers.purchaserAgent.routes.PurchaserAgentAuthorisedController.onPageLoad(CheckMode))
    }
  }
}
