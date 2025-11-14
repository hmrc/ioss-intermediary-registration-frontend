package forms

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class BusinessNotBasedInNiFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "businessNotBasedInNi.error.required"
  val invalidKey = "error.boolean"

  val form = new BusinessNotBasedInNiFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
