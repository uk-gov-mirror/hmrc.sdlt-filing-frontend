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

import org.scalatest.{EitherValues, OptionValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.libs.json.*

import java.time.Instant

class PrelimReturnSpec extends AnyFreeSpec with Matchers with EitherValues with OptionValues {

  private val validPrelimReturnJsonComplete = Json.obj(
    "stornId" -> "12345",
    "purchaserIsCompany" -> "YES",
    "surNameOrCompanyName" -> "Test Company",
    "houseNumber" -> 23,
    "addressLine1" -> "Test Street",
    "addressLine2" -> "Apartment 5",
    "addressLine3" -> "Building A",
    "addressLine4" -> "District B",
    "postcode" -> "TE23 5TT",
    "transactionType" -> "O"
  )

  private val validPrelimReturnJsonMinimal = Json.obj(
    "stornId" -> "12345",
    "purchaserIsCompany" -> "YES",
    "surNameOrCompanyName" -> "Test Company",
    "addressLine1" -> "Test Street",
    "transactionType" -> "O"
  )

  private val completePrelimReturn = PrelimReturn(
    stornId = "12345",
    purchaserIsCompany = "YES",
    surNameOrCompanyName = "Test Company",
    houseNumber = Some(23),
    addressLine1 = "Test Street",
    addressLine2 = Some("Apartment 5"),
    addressLine3 = Some("Building A"),
    addressLine4 = Some("District B"),
    postcode = Some("TE23 5TT"),
    transactionType = "O"
  )

  private val minimalPrelimReturn = PrelimReturn(
    stornId = "12345",
    purchaserIsCompany = "YES",
    surNameOrCompanyName = "Test Company",
    houseNumber = None,
    addressLine1 = "Test Street",
    addressLine2 = None,
    addressLine3 = None,
    addressLine4 = None,
    postcode = None,
    transactionType = "O"
  )

  "PrelimReturn" - {

    ".reads" - {

      "must be found implicitly" in {
        implicitly[Reads[PrelimReturn]]
      }

      "must deserialize valid JSON with all fields" in {
        val result = Json.fromJson[PrelimReturn](validPrelimReturnJsonComplete).asEither.value

        result.stornId mustBe "12345"
        result.purchaserIsCompany mustBe "YES"
        result.surNameOrCompanyName mustBe "Test Company"
        result.houseNumber mustBe Some(23)
        result.addressLine1 mustBe "Test Street"
        result.addressLine2 mustBe Some("Apartment 5")
        result.addressLine3 mustBe Some("Building A")
        result.addressLine4 mustBe Some("District B")
        result.postcode mustBe Some("TE23 5TT")
        result.transactionType mustBe "O"
      }

      "must deserialize valid JSON with only required fields" in {
        val result = Json.fromJson[PrelimReturn](validPrelimReturnJsonMinimal).asEither.value

        result.stornId mustBe "12345"
        result.purchaserIsCompany mustBe "YES"
        result.surNameOrCompanyName mustBe "Test Company"
        result.houseNumber must not be defined
        result.addressLine1 mustBe "Test Street"
        result.addressLine2 must not be defined
        result.addressLine3 must not be defined
        result.addressLine4 must not be defined
        result.postcode must not be defined
        result.transactionType mustBe "O"
      }

      "must deserialize JSON with null optional fields" in {
        val json = Json.obj(
          "stornId" -> "12345",
          "purchaserIsCompany" -> "YES",
          "surNameOrCompanyName" -> "Test Company",
          "houseNumber" -> JsNull,
          "addressLine1" -> "Test Street",
          "addressLine2" -> JsNull,
          "addressLine3" -> JsNull,
          "addressLine4" -> JsNull,
          "postcode" -> JsNull,
          "transactionType" -> "O"
        )

        val result = Json.fromJson[PrelimReturn](json).asEither.value

        result.houseNumber must not be defined
        result.addressLine2 must not be defined
        result.postcode must not be defined
      }

      "must fail to deserialize when stornId is missing" in {
        val json = validPrelimReturnJsonComplete - "stornId"

        val result = Json.fromJson[PrelimReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when purchaserIsCompany is missing" in {
        val json = validPrelimReturnJsonComplete - "purchaserIsCompany"

        val result = Json.fromJson[PrelimReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when surNameOrCompanyName is missing" in {
        val json = validPrelimReturnJsonComplete - "surNameOrCompanyName"

        val result = Json.fromJson[PrelimReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when addressLine1 is missing" in {
        val json = validPrelimReturnJsonComplete - "addressLine1"

        val result = Json.fromJson[PrelimReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when transactionType is missing" in {
        val json = validPrelimReturnJsonComplete - "transactionType"

        val result = Json.fromJson[PrelimReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when houseNumber has invalid type" in {
        val json = validPrelimReturnJsonComplete ++ Json.obj("houseNumber" -> "invalid")

        val result = Json.fromJson[PrelimReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize when required field has invalid type" in {
        val json = validPrelimReturnJsonComplete ++ Json.obj("stornId" -> 123)

        val result = Json.fromJson[PrelimReturn](json).asEither

        result.isLeft mustBe true
      }

      "must fail to deserialize completely invalid JSON structure" in {
        val json = Json.obj("invalidField" -> "value")

        val result = Json.fromJson[PrelimReturn](json).asEither

        result.isLeft mustBe true
      }
    }

    ".writes" - {

      "must be found implicitly" in {
        implicitly[Writes[PrelimReturn]]
      }

      "must serialize PrelimReturn with all fields" in {
        val json = Json.toJson(completePrelimReturn)

        (json \ "stornId").as[String] mustBe "12345"
        (json \ "purchaserIsCompany").as[String] mustBe "YES"
        (json \ "surNameOrCompanyName").as[String] mustBe "Test Company"
        (json \ "houseNumber").asOpt[Int] mustBe Some(23)
        (json \ "addressLine1").as[String] mustBe "Test Street"
        (json \ "addressLine2").asOpt[String] mustBe Some("Apartment 5")
        (json \ "addressLine3").asOpt[String] mustBe Some("Building A")
        (json \ "addressLine4").asOpt[String] mustBe Some("District B")
        (json \ "postcode").asOpt[String] mustBe Some("TE23 5TT")
        (json \ "transactionType").as[String] mustBe "O"
      }

      "must serialize PrelimReturn with only required fields" in {
        val json = Json.toJson(minimalPrelimReturn)

        (json \ "stornId").as[String] mustBe "12345"
        (json \ "purchaserIsCompany").as[String] mustBe "YES"
        (json \ "surNameOrCompanyName").as[String] mustBe "Test Company"
        (json \ "addressLine1").as[String] mustBe "Test Street"
        (json \ "transactionType").as[String] mustBe "O"
      }

      "must serialize None optional fields correctly" in {
        val json = Json.toJson(minimalPrelimReturn)

        // When deserialized back, None values should work correctly
        val deserialized = Json.fromJson[PrelimReturn](json).asEither.value
        deserialized.houseNumber must not be defined
        deserialized.addressLine2 must not be defined
        deserialized.postcode must not be defined
      }

      "must produce valid JSON structure" in {
        val json = Json.toJson(completePrelimReturn)

        json mustBe a[JsObject]
        json.as[JsObject].keys must contain allOf("stornId", "purchaserIsCompany", "surNameOrCompanyName", "addressLine1", "transactionType")
      }
    }

    ".formats" - {

      "must be found implicitly" in {
        implicitly[Format[PrelimReturn]]
      }

      "must round-trip serialize and deserialize with all fields" in {
        val json = Json.toJson(completePrelimReturn)
        val result = Json.fromJson[PrelimReturn](json).asEither.value

        result mustEqual completePrelimReturn
      }

      "must round-trip serialize and deserialize with only required fields" in {
        val json = Json.toJson(minimalPrelimReturn)
        val result = Json.fromJson[PrelimReturn](json).asEither.value

        result mustEqual minimalPrelimReturn
      }

      "must round-trip with mixed optional fields" in {
        val mixedPrelimReturn = PrelimReturn(
          stornId = "ABC123",
          purchaserIsCompany = "NO",
          surNameOrCompanyName = "John Doe",
          houseNumber = Some(42),
          addressLine1 = "Main Street",
          addressLine2 = Some("Apt 10"),
          addressLine3 = None,
          addressLine4 = Some("West Wing"),
          postcode = None,
          transactionType = "R"
        )

        val json = Json.toJson(mixedPrelimReturn)
        val result = Json.fromJson[PrelimReturn](json).asEither.value

        result mustEqual mixedPrelimReturn
      }
    }

    "case class" - {

      "must create instance with all fields" in {
        completePrelimReturn.stornId mustBe "12345"
        completePrelimReturn.houseNumber mustBe Some(23)
        completePrelimReturn.addressLine2 mustBe Some("Apartment 5")
      }

      "must create instance with only required fields" in {
        minimalPrelimReturn.stornId mustBe "12345"
        minimalPrelimReturn.houseNumber must not be defined
        minimalPrelimReturn.addressLine2 must not be defined
      }

      "must support equality" in {
        val prelimReturn1 = minimalPrelimReturn
        val prelimReturn2 = minimalPrelimReturn.copy()

        prelimReturn1 mustEqual prelimReturn2
      }

      "must support equality with all fields" in {
        val prelimReturn1 = completePrelimReturn
        val prelimReturn2 = completePrelimReturn.copy()

        prelimReturn1 mustEqual prelimReturn2
      }

      "must support copy with modifications" in {
        val modified = minimalPrelimReturn.copy(
          houseNumber = Some(99),
          postcode = Some("AB12 3CD")
        )

        modified.houseNumber mustBe Some(99)
        modified.postcode mustBe Some("AB12 3CD")
        modified.stornId mustBe minimalPrelimReturn.stornId
      }

      "must not be equal when required fields differ" in {
        val prelimReturn1 = minimalPrelimReturn
        val prelimReturn2 = minimalPrelimReturn.copy(stornId = "DIFFERENT")

        prelimReturn1 must not equal prelimReturn2
      }

      "must not be equal when optional fields differ" in {
        val prelimReturn1 = minimalPrelimReturn
        val prelimReturn2 = minimalPrelimReturn.copy(houseNumber = Some(100))

        prelimReturn1 must not equal prelimReturn2
      }
    }

    ".from" - {
      "must convert into PrelimReturn when all data present" in {
        val userAnswers = UserAnswers(
          id = "12345",
          returnId = None,
          data = Json.obj(
            "purchaserIsIndividual" -> "YES",
            "purchaserSurnameOrCompanyName" -> "Test Company",
            "purchaserAddress" -> Json.obj(
              "houseNumber" -> 23,
              "line1" -> "Test Street",
              "line2" -> "Apartment 5",
              "line3" -> "Building A",
              "line4" -> "District B",
              "line5" -> "District C",
              "postcode" -> "TE23 5TT",
              "country" -> Json.obj(
                "code" -> "GB",
                "name" -> "UK"
              ),
              "addressValidated" -> false
            ),
            "transactionType" -> "O"
          ),
          lastUpdated = Instant.now
        )

        val prelimReturn = PrelimReturn.from(Some(userAnswers))

        prelimReturn shouldBe completePrelimReturn
      }

      "must convert into PrelimReturn when only required data is present" in {
        val userAnswers = UserAnswers(
          id = "12345",
          returnId = None,
          data = Json.obj(
            "purchaserIsIndividual" -> "YES",
            "purchaserSurnameOrCompanyName" -> "Test Company",
            "purchaserAddress" -> Json.obj(
              "houseNumber" -> JsNull,
              "line1" -> "Test Street",
              "line2" -> JsNull,
              "line3" -> JsNull,
              "line4" -> JsNull,
              "line5" -> JsNull ,
              "postcode" ->  JsNull,
              "country" -> JsNull,
              "addressValidated" -> false
            ),
            "transactionType" -> "O"
          ),
          lastUpdated = Instant.now
        )

        val prelimReturn = PrelimReturn.from(Some(userAnswers))

        prelimReturn shouldBe minimalPrelimReturn
      }
      }
    }
  }