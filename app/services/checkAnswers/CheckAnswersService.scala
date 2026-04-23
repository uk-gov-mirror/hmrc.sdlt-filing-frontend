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

package services.checkAnswers

import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.checkAnswers.summary.SummaryRowResult
import viewmodels.checkAnswers.summary.SummaryRowResult.{Missing, Row}
import viewmodels.govuk.all.SummaryListViewModel

class CheckAnswersService {

  def redirectOrRender(rowResults: Seq[SummaryRowResult]): Either[Call, SummaryList] = {
    rowResults.collectFirst {
      case Missing(call) => Left(call)
    }.getOrElse {
      val rows = rowResults.collect { case Row(r) => r }
      Right(SummaryListViewModel(rows))
    }
  }
}
