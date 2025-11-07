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

class VendorNameSpec extends AnyFreeSpec with Matchers with EitherValues {

  "VendorName" - {

    def vendorNameWithOptionalJson: JsObject = Json.obj("forename1" -> "Jon", "forename2" -> "Bones", "name" -> "Jones")

    def vendorNameWithNoOptionalJson: JsObject = Json.obj("name" -> "Jones")

    def inValidVendorNameJson: JsObject = Json.obj("name" -> true)

    def vendorNameWithOptional = VendorName(forename1 = Some("Jon"), forename2 = Some("Bones"), name = "Jones")

    def vendorNameWithNoOptional = VendorName(forename1 = None, forename2 = None, name = "Jones")

    ".reads" - {
      "must be found implicitly" in {
        implicitly[Reads[VendorName]]
      }

      "must deserialize valid JSON" - {
        "with optional values" in {
          val result = Json.fromJson[VendorName](vendorNameWithOptionalJson).asEither.value

          result mustBe VendorName(forename1 = Some("Jon"), forename2 = Some("Bones"), name = "Jones")
        }

        "with no optional values" in {
          val result = Json.fromJson[VendorName](vendorNameWithNoOptionalJson).asEither.value

          result mustBe VendorName(forename1 = None, forename2 = None, name = "Jones")
        }
      }

      "must fail when field has wrong type" in {
        val result = Json.fromJson[VendorName](inValidVendorNameJson).asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {
      "must be found implicitly" in {
        implicitly[Writes[VendorName]]
      }

      "must serialize VendorName" - {
        "with optional values" in {
          val json = Json.toJson(vendorNameWithOptional)

          (json \ "forename1").as[String] mustBe "Jon"
          (json \ "forename2").as[String] mustBe "Bones"
          (json \ "name").as[String] mustBe "Jones"
        }

        "with no optional values" in {
          val json = Json.toJson(vendorNameWithNoOptional)

          (json \ "name").as[String] mustBe "Jones"
        }
      }
    }

    ".formats" - {
      "must round-trip" in {
        val json = Json.toJson(vendorNameWithOptional)
        val result = Json.fromJson[VendorName](json).asEither.value

        result mustEqual vendorNameWithOptional
      }
    }
  }
}
