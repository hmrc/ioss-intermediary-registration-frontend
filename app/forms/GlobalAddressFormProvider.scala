/*
 * Copyright 2026 HM Revenue & Customs
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
import models.Country.unitedKingdomCountry
import models.{Country, InternationalAddress}
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}

import javax.inject.Inject

class GlobalAddressFormProvider @Inject() extends Mappings {

  def apply(country: Country): Form[InternationalAddress] =
    Form(
      mapping(
        "line1" -> text("globalAddress.error.line1.required")
          .verifying(maxLength(35, "globalAddress.error.line1.length"))
          .verifying(regexp(commonTextPattern, "globalAddress.error.line1.format")),

        "line2" -> optional(text("globalAddress.error.line2.required")
          .verifying(maxLength(35, "globalAddress.error.line2.length"))
          .verifying(regexp(commonTextPattern, "globalAddress.error.line2.format"))),

        "townOrCity" -> text("globalAddress.error.townOrCity.required")
          .verifying(maxLength(35, "globalAddress.error.townOrCity.length"))
          .verifying(regexp(commonTextPattern, "globalAddress.error.townOrCity.format")),

        "stateOrRegion" -> optional(text("globalAddress.error.stateOrRegion.required")
          .verifying(maxLength(35, "globalAddress.error.stateOrRegion.length"))
          .verifying(regexp(commonTextPattern, "globalAddress.error.stateOrRegion.format"))),

        "postCode" -> {
          if (country.code == unitedKingdomCountry.code) {
            text("globalAddress.error.postCode.required")
              .verifying(firstError(
                maxLength(40, "globalAddress.error.postCode.length"),
                regexp(postcodePattern, "globalAddress.error.postCode.invalid")))
              .transform[Option[String]](x => Some(x), {
                case Some(postcode) => postcode
                case _ => ""
              })
          } else {
            optional(text("globalAddress.error.postCode.required")
              .verifying(firstError(
                maxLength(40, "globalAddress.error.postCode.length"),
                regexp(postcodePattern, "globalAddress.error.postCode.invalid"))))
          }
        }
      )(InternationalAddress(_, _, _, _, _, country))(a => Some((a.line1, a.line2, a.townOrCity, a.stateOrRegion, a.postCode)))
    )
    
}
