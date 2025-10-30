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

import play.api.libs.json.{Json, Reads}

case class SessionUserData(
                            purchaserIsIndividual: String,
                            purchaserSurnameOrCompanyName: String,
                            purchaserAddress: SessionAddress,
                            transactionType: String
                          )
object SessionUserData {
  implicit val reader: Reads[SessionUserData] = Json.reads[SessionUserData]

}

case class SessionAddress(
                           houseNumber: Option[Int],
                           line1: String,
                           line2: Option[String],
                           line3: Option[String],
                           line4: Option[String],
                           line5: Option[String],
                           postcode: Option[String],
                           country: Option[SessionCountry],
                           addressValidated: Boolean,
                         )

object SessionAddress {
  implicit val reader: Reads[SessionAddress] = Json.reads[SessionAddress]
}


case class SessionCountry(
                           code: Option[String],
                           name: Option[String]
                         )
object SessionCountry {
  implicit val reader: Reads[SessionCountry] = Json.reads[SessionCountry]
}

