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

package forms.euDetails

import config.Constants.fixedEstablishmentTradingNameMaxLength
import forms.behaviours.StringFieldBehaviours
import forms.validation.Validation.commonTextPattern
import models.Country
import play.api.data.{Form, FormError}

class FixedEstablishmentTradingNameFormProviderSpec extends StringFieldBehaviours {

  private val country: Country = arbitraryCountry.arbitrary.sample.value

  private val requiredKey: String = "fixedEstablishmentTradingName.error.required"
  private val lengthKey: String = "fixedEstablishmentTradingName.error.length"
  private val invalidKey: String = "fixedEstablishmentTradingName.error.invalid"
  private val maxLength: Int = fixedEstablishmentTradingNameMaxLength

  private val form: Form[String] = new FixedEstablishmentTradingNameFormProvider()(country)

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, args = Seq(country.name))
    )

    "must not bind invalid Trading Name" in {

      val invalidTradingName: String = "^Tr@ding=nam£"
      val result = form.bind(Map(fieldName -> invalidTradingName)).apply(fieldName)
      result.errors mustBe Seq(FormError(fieldName, invalidKey, Seq(commonTextPattern)))
    }
  }
}
