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
import services.crossflow.fields.CrossFlowValidationService
import utils.{LeaseHelper, PropertyTypeHelper}
import viewmodels.tasklist.LandTaskList.isLandComplete
import viewmodels.tasklist.LeaseTaskList.isLeaseComplete
import viewmodels.tasklist.VendorTaskList.isVendorComplete
import viewmodels.tasklist.PurchaserTaskList.isPurchaserComplete
import viewmodels.tasklist.TaxCalculationTaskList.isTaxCalculationComplete
import viewmodels.tasklist.TransactionTaskList.isTransactionComplete
import viewmodels.tasklist.VendorAgentTaskList.*
import viewmodels.tasklist.PurchaserAgentTaskList.*
import viewmodels.tasklist.UkResidencyTaskList.isResidencyComplete

import javax.inject.Singleton

@Singleton
object SubmissionTaskList {

  def build(fullReturn: FullReturn)
           (implicit messages: Messages,
            appConfig: FrontendAppConfig,
            crossFlowValidationService: CrossFlowValidationService,
            userAnswers: UserAnswers): TaskListSection =
    TaskListSection(
      heading = messages("tasklist.submissionQuestion.heading"),
      rows = Seq(
        buildSubmissionRow(fullReturn)
      )
    )
  
  def buildSubmissionRow(fullReturn: FullReturn)
                        (implicit messages: Messages,
                         appConfig: FrontendAppConfig,
                         crossFlowValidationService: CrossFlowValidationService,
                         userAnswers: UserAnswers): TaskListSectionRow = {
    val url = fullReturn.submission match {
      case Some(submission) if submission.submissionID.isDefined =>
        controllers.submission.routes.SubmissionCompleteController.onPageLoad().url
      case _ =>
        controllers.submission.routes.SubmissionBeforeYouStartController.onPageLoad().url
    }

    TaskListRowBuilder(
      canEdit = {
        case TLCompleted => true
        case _           => true
      },
      messageKey = _ => "tasklist.submissionQuestion.details",
      hint = fullReturn => {
        if (!canStartSubmission(fullReturn))
          Some("tasklist.submissionQuestion.hint")
        else
          None
      },
      url = _ => _ => {
        url
      },
      tagId  = "submissionQuestionDetailRow",
      checks = _ => Seq(
        fullReturn.submission.exists(_.submissionID.isDefined),
        fullReturn.submission.exists(_.submissionStatus.exists(_ != "STARTED"))
      ),
      prerequisites = _ => {
        val mandatory = Seq(
          VendorTaskList.vendorRowBuilder(fullReturn),
          VendorAgentTaskList.vendorAgentRowBuilder(fullReturn),
          PurchaserTaskList.purchaserRowBuilder(fullReturn),
          PurchaserAgentTaskList.purchaserAgentRowBuilder(fullReturn),
          LandTaskList.landRowBuilder(fullReturn, viewmodels.tasklist.LandTaskList.noFailures),
          TransactionTaskList.transactionRowBuilder(fullReturn, viewmodels.tasklist.TransactionTaskList.noFailures),
          TaxCalculationTaskList.taxCalculationRowBuilder(fullReturn)
        )

        val conditional = Seq(
          Option.when(isResidencyRequired(fullReturn))(UkResidencyTaskList.ukResidencyRowBuilder(fullReturn)),
          Option.when(isLeaseRequired(fullReturn))(
            LeaseTaskList.leaseRowBuilder(fullReturn, viewmodels.tasklist.LeaseTaskList.noFailures)
          )
        ).flatten
        mandatory ++ conditional
      }
    ).build(fullReturn)
  }

  def canStartSubmission(fullReturn: FullReturn)(implicit crossFlowValidationService: CrossFlowValidationService, userAnswers: UserAnswers): Boolean = {
    isVendorComplete(fullReturn) &&
    isPurchaserComplete(fullReturn) &&
    isLandComplete(fullReturn) &&
    isTransactionComplete(fullReturn) &&
    isTaxCalculationComplete(fullReturn) &&
    isVendorAgentComplete(fullReturn) &&
    isPurchaserAgentComplete(fullReturn) &&
    (!isLeaseRequired(fullReturn) || isLeaseComplete(fullReturn)) &&
    (!isResidencyRequired(fullReturn) || isResidencyComplete(fullReturn)) &&
      crossFlowValidationService.failureCount(userAnswers) > 0
  }

  private def isLeaseRequired(fullReturn: FullReturn): Boolean = {
    LeaseHelper.isLeaseType(fullReturn)
  }

  private def isResidencyRequired(fullReturn: FullReturn): Boolean = {
    PropertyTypeHelper.isResidentialProperty(fullReturn)
  }
}