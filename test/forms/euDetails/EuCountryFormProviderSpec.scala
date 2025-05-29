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

import base.SpecBase
import forms.behaviours.StringFieldBehaviours
import models.Country
import org.scalacheck.Arbitrary.arbitrary
import play.api.data.{Form, FormError}

class EuCountryFormProviderSpec extends StringFieldBehaviours with SpecBase {

  private val requiredKey: String = "euCountry.error.required"
  private val emptyExistingAnswers: Seq[Country] = Seq.empty[Country]
  private val countries: Seq[Country] = Country.euCountries

  private val form: Form[Country] = new EuCountryFormProvider()(countryIndex(0), emptyExistingAnswers)

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      countries.head.code
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must not bind any values other than valid country codes" in {

      val invalidAnswers = arbitrary[String] suchThat (x => !countries.map(_.code).contains(x))

      forAll(invalidAnswers) { answer =>
        val result = form.bind(Map("value" -> answer)).apply(fieldName)
        result.errors must contain only FormError(fieldName, requiredKey)
      }
    }

    "must fail to bind when given a duplicate value" in {

      val existingAnswers: Seq[Country] = Seq(countries.head, countries.tail.head)
      val answer = countries.tail.head
      val form = new EuCountryFormProvider()(countryIndex(0), existingAnswers)

      val result = form.bind(Map(fieldName -> answer.code)).apply(fieldName)
      result.errors must contain only FormError(fieldName, "euCountry.error.duplicate")
    }
  }
}
