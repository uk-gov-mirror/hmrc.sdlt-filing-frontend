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
import pages.preliminary.{PurchaserIsIndividualPage, PurchaserSurnameOrCompanyNamePage}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*
import viewmodels.checkAnswers.summary.SummaryRowResult
import viewmodels.checkAnswers.summary.SummaryRowResult.{Missing, Row}

object PurchaserSurnameOrCompanyNameSummary  {

  def row(answers: Option[UserAnswers])(implicit messages: Messages): SummaryRowResult = {
    
    val typeOfPurchaser = answers.flatMap(_.get(PurchaserIsIndividualPage)) match {
      case Some(value) => if(value.toString == "Individual") "purchaser" else "company"
      case _ => "default"
    }

    answers.flatMap(_.get(PurchaserSurnameOrCompanyNamePage)).map {
      answer =>
        Row(
          SummaryListRowViewModel(
            key     = s"prelim.purchaser.name.checkYourAnswersLabel.${typeOfPurchaser}",
            value   = ValueViewModel(HtmlContent(HtmlFormat.escape(answer).toString)),
            actions = Seq(
              ActionItemViewModel("site.change", controllers.preliminary.routes.PurchaserSurnameOrCompanyNameController.onPageLoad(CheckMode).url)
                .withVisuallyHiddenText(messages("prelim.purchaser.name.change.hidden"))
            )
          )
        )
    }.getOrElse {
      Missing(controllers.preliminary.routes.PurchaserSurnameOrCompanyNameController.onPageLoad(CheckMode))
    }
  }
}