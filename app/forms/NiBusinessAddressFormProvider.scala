package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms._
import models.NiBusinessAddress

class NiBusinessAddressFormProvider @Inject() extends Mappings {

   def apply(): Form[NiBusinessAddress] = Form(
     mapping(
      "field1" -> text("niBusinessAddress.error.field1.required")
        .verifying(maxLength(100, "niBusinessAddress.error.field1.length")),
      "field2" -> text("niBusinessAddress.error.field2.required")
        .verifying(maxLength(100, "niBusinessAddress.error.field2.length"))
    )(NiBusinessAddress.apply)(x => Some((x.field1, x.field2)))
   )
 }
