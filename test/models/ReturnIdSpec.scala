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

import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsObject, Json, Reads, Writes}

class ReturnIdSpec extends AnyFreeSpec with Matchers with EitherValues {

  "ReturnId" - {

    def validReturnIdJson: JsObject = Json.obj("returnId" -> "12345")

    def inValidReturnIdJson: JsObject = Json.obj("returnId" -> true)

    def validReturnId = ReturnId("12345")

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[ReturnId]]
      }

      "must deserialize valid JSON" in {
        val result = Json.fromJson[ReturnId](validReturnIdJson).asEither.value

        result mustBe ReturnId("12345")
      }

      "must fail when field has wrong type" in {
        val result = Json.fromJson[ReturnId](inValidReturnIdJson).asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {
      
      "must be found implicitly" in {
        implicitly[Writes[ReturnId]]
      }

      "must serialize Purchaser" in {
        val json = Json.toJson(validReturnId)

        (json \ "returnId").as[String] mustBe "12345"
      }
    }

    ".formats" - {

      "must round-trip" in {
        val json = Json.toJson(validReturnId)
        val result = Json.fromJson[ReturnId](json).asEither.value

        result mustEqual validReturnId
      }
    }
  }
}
