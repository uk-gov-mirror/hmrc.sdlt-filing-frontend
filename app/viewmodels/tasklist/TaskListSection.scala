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

package viewmodels.tasklist

import config.FrontendAppConfig
import models.{FullReturn, UserAnswers}
import play.api.i18n.Messages
import play.api.mvc.Request
import services.crossflow.fields.CrossFlowValidationService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

case class TaskListSection(heading: String, rows: Seq[TaskListSectionRow]) {

  def isComplete: Boolean = rows.forall(_.status == TLCompleted)

}

object TaskListSections {


  def sections(fullReturn: FullReturn)(implicit messagesApi: Messages,
                                       appConfig: FrontendAppConfig,
                                       hc: HeaderCarrier,
                                       ec: ExecutionContext,
                                       request: Request[_],
                                       crossFlowValidationService: CrossFlowValidationService,
                                       userAnswers: UserAnswers) = List(
    Some(VendorTaskList.build(fullReturn)),
    Some(VendorAgentTaskList.build(fullReturn)),
    Some(PurchaserTaskList.build(fullReturn)),
    Some(PurchaserAgentTaskList.build(fullReturn)),
    Some(LandTaskList.build(fullReturn)),
    Some(UkResidencyTaskList.build(fullReturn)),
    Some(TransactionTaskList.build(fullReturn)),
    Some(LeaseTaskList.build(fullReturn)),
    Some(TaxCalculationTaskList.build(fullReturn)),
    Some(SubmissionTaskList.build(fullReturn))
  ).flatten
  def allComplete(fullReturn: FullReturn)
                 (implicit messagesApi: Messages, appConfig: FrontendAppConfig, hc: HeaderCarrier, ec: ExecutionContext, request: Request[_],
                  crossFlowValidationService: CrossFlowValidationService, userAnswers: UserAnswers): Boolean =
    sections(fullReturn).forall{x => x.isComplete}
}

case class TaskListSectionRow(messageKey: String,
                              url: String,
                              tagId: String,
                              status: TaskListState,
                              canEdit: Boolean = false,
                              hint: Option[String] = None)

sealed trait TaskListState

case object TLCannotStart extends TaskListState

case object TLNotStarted extends TaskListState

case object TLInProgress extends TaskListState

case object TLCompleted extends TaskListState

case object TLFailed extends TaskListState

case object TLInvalid extends TaskListState

case object TLOptional extends TaskListState