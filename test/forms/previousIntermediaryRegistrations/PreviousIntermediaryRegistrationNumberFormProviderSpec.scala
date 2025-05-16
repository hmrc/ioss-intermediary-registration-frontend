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

package forms.previousIntermediaryRegistrations

import forms.behaviours.StringFieldBehaviours
import models.Country
import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetails
import org.scalacheck.Arbitrary.arbitrary
import play.api.data.{Form, FormError}

class PreviousIntermediaryRegistrationNumberFormProviderSpec extends StringFieldBehaviours {

  private val previousIntermediaryRegistrationDetails: PreviousIntermediaryRegistrationDetails =
    arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value

  private val intermediaryNumber: String = previousIntermediaryRegistrationDetails.previousIntermediaryNumber
  private val country: Country = previousIntermediaryRegistrationDetails.previousEuCountry

  private val requiredKey: String = "previousIntermediaryRegistrationNumber.error.required"
  private val invalidKey: String = "previousIntermediaryRegistrationNumber.error.invalid"

  private val form: Form[String] = new PreviousIntermediaryRegistrationNumberFormProvider()(country)

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      intermediaryNumber
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, args = Seq(country.name))
    )

    "must not bind any values other than valid IN numbers" in {

      val invalidAnswers = arbitrary[String] suchThat (x => !intermediaryNumber.contains(x))

      forAll(invalidAnswers) {
        answer =>
          val result = form.bind(Map("value" -> answer)).apply(fieldName)
          result.errors must contain only FormError(fieldName, invalidKey)
      }
    }
  }
}

