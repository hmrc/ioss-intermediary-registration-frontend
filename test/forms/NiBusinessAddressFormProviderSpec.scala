package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class NiBusinessAddressFormProviderSpec extends StringFieldBehaviours {

  val form = new NiBusinessAddressFormProvider()()

  ".field1" - {

    val fieldName = "field1"
    val requiredKey = "niBusinessAddress.error.field1.required"
    val lengthKey = "niBusinessAddress.error.field1.length"
    val maxLength = 100

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
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".field2" - {

    val fieldName = "field2"
    val requiredKey = "niBusinessAddress.error.field2.required"
    val lengthKey = "niBusinessAddress.error.field2.length"
    val maxLength = 100

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
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
