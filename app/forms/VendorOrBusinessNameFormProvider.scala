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

import javax.inject.Inject
import forms.mappings.Mappings
import models.VendorName
import play.api.data.Form
import play.api.data.Forms.*

class VendorOrBusinessNameFormProvider @Inject() extends Mappings {

  private val formRegex = "[A-Za-z0-9 ~!@%&'()*+,\\-./:=?\\[\\]^_{}\\;]*"

  def apply(): Form[VendorName] =
    Form(
      mapping(
        "forename1" -> optionalText()
          .verifying(optionalMaxLength(14, "vendor.individual.error.length.firstName"))
          .verifying(optionalRegexp(formRegex, "vendor.name.form.regex.error")),
        "forename2" -> optionalText()
          .verifying(optionalMaxLength(14, "vendor.individual.error.length.middleName"))
          .verifying(optionalRegexp(formRegex, "vendor.name.form.regex.error")),
        "name" -> text("vendor.name.error.required")
          .verifying(maxLength(56, "vendor.name.error.length"))
          .verifying(regexp(formRegex, "vendor.name.form.regex.error"))
      )(VendorName.apply)(o => Some(Tuple.fromProductTyped(o)))
    )
}
