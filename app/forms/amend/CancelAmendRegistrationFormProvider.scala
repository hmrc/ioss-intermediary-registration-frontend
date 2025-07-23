package forms.amend

import forms.mappings.Mappings
import play.api.data.Form

import javax.inject.Inject

class CancelAmendRegistrationFormProvider @Inject() extends Mappings {

  def apply(): Form[Boolean] =
    Form(
      "value" -> boolean("cancelAmendRegistration.error.required")
    )
}
