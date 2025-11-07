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

package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class VendorOrBusinessNameFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "vendor.name.error.required"
  val nameLengthKey = "vendor.name.error.length"
  val firstNameLengthKey = "vendor.individual.error.length.firstName"
  val middleNameLengthKey = "vendor.individual.error.length.middleName"
  val invalidKey = "vendor.name.form.regex.error"
  val maxLength = 56
  val firstNameMaxLength = 14
  val middleNameMaxLength = 14

  val form = new VendorOrBusinessNameFormProvider()()

  ".vendorOrBusinessName" - {

    val mandatoryFieldName = "name"
    val optionalFirstName = "forename1"
    val optionalMiddleName = "forename2"

    ".name" - {
      "must bind valid form data" in {
        val validNames = Seq(
          "Mr test",
          "Business test name",
          "Business are us",
          "Business@business.com",
          "(555) 123-4567"
        )

        validNames.foreach { validName =>
          val result = form.bind(Map(mandatoryFieldName -> validName))
          result.errors must be(empty)
        }
      }

      "must not bind strings longer than 56 characters" in {
        val longName = "a" * 57
        val result = form.bind(Map(mandatoryFieldName -> longName))
        result.errors must contain(FormError(mandatoryFieldName, nameLengthKey, Seq(maxLength)))
      }

      behave like mandatoryField(
        form,
        mandatoryFieldName,
        requiredError = FormError(mandatoryFieldName, requiredKey)
      )

      "must reject invalid name formats" in {
        val invalidNames = Seq(
          "Hello #world",
          "Price: $50",
          "A < B",
          "File \\ path",
          "José",
          "\"Line1\\nLine2\""
        )

        invalidNames.foreach { invalidName =>
          val result = form.bind(Map(mandatoryFieldName -> invalidName))
          result.errors must contain(
            FormError(mandatoryFieldName, invalidKey, Seq("[A-Za-z0-9 ~!@%&'()*+,\\-./:=?\\[\\]^_{}\\;]*"))
          )
        }
      }

    }

    ".forename1" - {
      "must bind valid form data" in {
        val validNames = Seq(
          "Mr test",
          "Business",
          "Pokemon",
          "Middle Name",
        )

        validNames.foreach { validName =>
          val result = form.bind(Map(optionalFirstName -> validName, mandatoryFieldName -> "name"))
          result.errors.isEmpty must be(true)
        }
      }

      "must not bind strings longer than 14 characters" in {
        val longName = "a" * 15

        val result = form.bind(Map(optionalFirstName -> longName, mandatoryFieldName -> "name"))
        result.errors must contain(FormError(optionalFirstName, firstNameLengthKey, Seq(firstNameMaxLength)))
      }

      behave like optionalField(
        form,
        optionalFirstName
    )

      "must reject invalid name formats" in {
        val invalidNames = Seq(
          "Hello #world",
          "Price: $50",
          "A < B",
          "File \\ path",
          "José",
          "\"Line1\\nLine2\""
        )

        invalidNames.foreach { invalidName =>
          val result = form.bind(Map(optionalFirstName -> invalidName, mandatoryFieldName -> "name"))
          result.errors must contain(
            FormError(optionalFirstName, invalidKey, Seq("[A-Za-z0-9 ~!@%&'()*+,\\-./:=?\\[\\]^_{}\\;]*"))
          )
        }
      }
    }

    ".forename2" - {
      "must bind valid form data" in {
        val validNames = Seq(
          "Mr test",
          "Business",
          "Pokemon",
          "Middle Name",
        )

        validNames.foreach { validName =>
          val result = form.bind(Map(mandatoryFieldName -> "name", optionalMiddleName -> validName))
          result.errors.isEmpty must be(true)
        }
      }

      "must not bind strings longer than 14 characters" in {
        val longName = "a" * 15

        val result = form.bind(Map(mandatoryFieldName -> "name", optionalMiddleName -> longName))
        result.errors must contain(FormError(optionalMiddleName, middleNameLengthKey, Seq(firstNameMaxLength)))
      }

      behave like optionalField(
        form,
        optionalMiddleName
      )

      "must reject invalid name formats" in {
        val invalidNames = Seq(
          "Hello #world",
          "Price: $50",
          "A < B",
          "File \\ path",
          "José",
          "\"Line1\\nLine2\""
        )

        invalidNames.foreach { invalidName =>
          val result = form.bind(Map(mandatoryFieldName -> "name", optionalMiddleName -> invalidName))
          result.errors must contain(
            FormError(optionalMiddleName, invalidKey, Seq("[A-Za-z0-9 ~!@%&'()*+,\\-./:=?\\[\\]^_{}\\;]*"))
          )
        }
      }
    }
  }
}

