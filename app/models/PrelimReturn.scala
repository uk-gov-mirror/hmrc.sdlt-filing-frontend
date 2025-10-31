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

package models

import play.api.libs.json.{Json, OFormat}

import scala.concurrent.Future

case class PrelimReturn(
                       stornId: String,
                       purchaserIsCompany: String,
                       surNameOrCompanyName: String,
                       houseNumber: Option[Int],
                       addressLine1: String,
                       addressLine2: Option[String],
                       addressLine3: Option[String],
                       addressLine4: Option[String],
                       postcode: Option[String],
                       transactionType: String
                       )

object PrelimReturn {
  implicit val format: OFormat[PrelimReturn] = Json.format[PrelimReturn]

  def from(userAnswers: Option[UserAnswers]): Future[PrelimReturn] = {
    val sessionUserData: SessionUserData = userAnswers.get.data.as[SessionUserData]
    Future.successful(PrelimReturn(
      stornId = userAnswers.get.id,
      purchaserIsCompany = sessionUserData.purchaserIsIndividual,
      surNameOrCompanyName = sessionUserData.purchaserSurnameOrCompanyName,
      houseNumber = sessionUserData.purchaserAddress.houseNumber,
      addressLine1 = sessionUserData.purchaserAddress.line1,
      addressLine2 = sessionUserData.purchaserAddress.line2,
      addressLine3 = sessionUserData.purchaserAddress.line3,
      addressLine4 = sessionUserData.purchaserAddress.line4,
      postcode = sessionUserData.purchaserAddress.postcode,
      transactionType = sessionUserData.transactionType
    )
    )
  }
}
