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

import forms.mappings.Mappings
import forms.validation.Validation.{commonTextPattern, postcodePattern}
import models.UkAddress
import play.api.data.Form
import play.api.data.Forms.*

import javax.inject.Inject

class NiAddressFormProvider @Inject() extends Mappings {

  def apply(): Form[UkAddress] = Form(
    mapping(
      "line1" -> text("niAddress.error.line1.required")
        .verifying(maxLength(35, "niAddress.error.line1.length"))
        .verifying(regexp(commonTextPattern, "niAddress.error.line1.format")),

      "line2" -> optional(text("niAddress.error.line2.required")
        .verifying(maxLength(35, "niAddress.error.line2.length"))
        .verifying(regexp(commonTextPattern, "niAddress.error.line2.format"))),

      "townOrCity" -> text("niAddress.error.townOrCity.required")
        .verifying(maxLength(35, "niAddress.error.townOrCity.length"))
        .verifying(regexp(commonTextPattern, "niAddress.error.townOrCity.format")),

      "county" -> optional(text("niAddress.error.county.required")
        .verifying(maxLength(35, "niAddress.error.county.length"))
        .verifying(regexp(commonTextPattern, "niAddress.error.county.format"))),

      "postCode" -> text("niAddress.error.postCode.required")
        .verifying(firstError(
          maxLength(40, "niAddress.error.postCode.length"),
          regexp(postcodePattern, "niAddress.error.postCode.invalid")))
    )(UkAddress(_, _, _, _, _))(a => Some((a.line1, a.line2, a.townOrCity, a.county, a.postCode)))
  )
}
