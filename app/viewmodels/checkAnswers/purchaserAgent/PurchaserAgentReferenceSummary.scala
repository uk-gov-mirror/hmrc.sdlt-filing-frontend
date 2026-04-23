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
import pages.purchaserAgent.{AddPurchaserAgentReferenceNumberPage, PurchaserAgentReferencePage}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*
import viewmodels.checkAnswers.summary.SummaryRowResult
import viewmodels.checkAnswers.summary.SummaryRowResult.{Missing, Row}

object PurchaserAgentReferenceSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryRowResult] = {
    val label = messages("purchaserAgent.reference.checkYourAnswersLabel")
    val changeRoute = controllers.purchaserAgent.routes.PurchaserAgentReferenceController.onPageLoad(CheckMode).url

    (answers.get(PurchaserAgentReferencePage), answers.get(AddPurchaserAgentReferenceNumberPage)) match {
      case (Some(paReference), _) =>

        Some(Row(
          SummaryListRowViewModel(
          key = label,
          value = ValueViewModel(HtmlContent(HtmlFormat.escape(paReference).toString)),
          actions = Seq(
            ActionItemViewModel("site.change", changeRoute)
              .withVisuallyHiddenText(messages("purchaserAgent.reference.change.hidden"))
          )
        ))
        )
      case (None, Some(true)) =>
        Some(Missing(controllers.purchaserAgent.routes.PurchaserAgentReferenceController.onPageLoad(CheckMode)))
      case _ => None
    }
  }
}
