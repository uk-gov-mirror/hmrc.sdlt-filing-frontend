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

package services.lease

import models.UserAnswers
import models.prelimQuestions.TransactionType
import models.prelimQuestions.TransactionType.{ConveyanceTransferLease, GrantOfLease}
import pages.transaction.TransactionEffectiveDatePage
import play.api.mvc.Call

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LeaseService {

  def transactionType(userAnswers: UserAnswers): Option[TransactionType] =
    TransactionType.parse(
      userAnswers.fullReturn.flatMap(_.transaction).flatMap(_.transactionDescription)
    )

  def leaseFlowValidationCheck(userAnswers: UserAnswers): Option[Call] = {
    transactionType(userAnswers) match {
      case Some(GrantOfLease | ConveyanceTransferLease) => None
      case _ if userAnswers.returnId.isDefined =>
        Some(controllers.routes.ReturnTaskListController.onPageLoad())
      case _ =>
        Some(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def parseDate(value: String): Option[LocalDate] = {
    val formatters = Seq(
      DateTimeFormatter.ISO_LOCAL_DATE,
      DateTimeFormatter.ofPattern("d/M/yyyy")
    )
    formatters.view
      .flatMap(formatter =>
        scala.util.Try(LocalDate.parse(value, formatter)).toOption
      )
      .headOption
  }

  def isOnOrAfterAnnualRentCutOff(userAnswers: UserAnswers): Boolean = {
    val cutoff = LocalDate.of(2016, 2, 16)
    val effectiveDate = userAnswers.get(TransactionEffectiveDatePage)
      .orElse(
        userAnswers.fullReturn
        .flatMap(_.transaction.map(_.effectiveDate))
        .flatten
        .flatMap(parseDate)
      )
    effectiveDate.exists(date => !date.isBefore(cutoff))
  }

  def isPreviousEffectiveDateOnOrAfterAnnualRentCutOff(userAnswers: UserAnswers): Option[Boolean] = {
    val cutoff = LocalDate.of(2016, 2, 16)

    userAnswers.fullReturn
      .flatMap(_.transaction.map(_.effectiveDate))
      .flatten
      .flatMap(parseDate)
      .map(date => !date.isBefore(cutoff))
  }
}