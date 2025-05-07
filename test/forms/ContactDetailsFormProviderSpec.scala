package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class ContactDetailsFormProviderSpec extends StringFieldBehaviours {

  val form = new ContactDetailsFormProvider()()

  ".Contact Name" - {

    val fieldName = "Contact Name"
    val requiredKey = "contactDetails.error.Contact Name.required"
    val lengthKey = "contactDetails.error.Contact Name.length"
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

  ".Telephone Number" - {

    val fieldName = "Telephone Number"
    val requiredKey = "contactDetails.error.Telephone Number.required"
    val lengthKey = "contactDetails.error.Telephone Number.length"
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
