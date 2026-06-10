package forms

import forms.behaviours.StringFieldBehaviours
import models.Country
import org.scalacheck.Arbitrary.arbitrary
import play.api.data.FormError

import scala.language.postfixOps

class NonNiBasedCountryFormProviderSpec extends StringFieldBehaviours {

  private val requiredKey = "nonNiBasedCountry.error.required"

  private val form = new NonNiBasedCountryFormProvider()()

  private val internationalCountries: Seq[Country] = Country.internationalCountries

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      internationalCountries.head.code
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must not bind any values other than valid country codes" in {
      
      val invalidAnswers = arbitrary[String] suchThat(x => !internationalCountries.map(_.code).contains(x))

      forAll(invalidAnswers) { answer =>
      val result = form.bind(Map("value" -> answer)).apply(fieldName)
      result.errors must contain only FormError(fieldName, requiredKey)
      }
    }
  }
}
