package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms._
import models.ContactDetails

class ContactDetailsFormProvider @Inject() extends Mappings {

   def apply(): Form[ContactDetails] = Form(
     mapping(
      "Contact Name" -> text("contactDetails.error.Contact Name.required")
        .verifying(maxLength(100, "contactDetails.error.Contact Name.length")),
      "Telephone Number" -> text("contactDetails.error.Telephone Number.required")
        .verifying(maxLength(100, "contactDetails.error.Telephone Number.length"))
    )(ContactDetails.apply)(x => Some((x.Contact Name, x.Telephone Number)))
   )
 }
