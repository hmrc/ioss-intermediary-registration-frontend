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

package forms

import javax.inject.Inject
import forms.mappings.Mappings
import forms.validation.Validation.{commonTextPattern, emailPattern, telephonePattern}
import play.api.data.Form
import play.api.data.Forms.*
import models.ContactDetails

class ContactDetailsFormProvider @Inject() extends Mappings {

  def apply(): Form[ContactDetails] = Form(
    mapping(
      "fullName" -> text("contactDetails.error.fullName.required")
        .verifying(firstError(
          maxLength(100, "contactDetails.error.fullName.length"),
          regexp(commonTextPattern, "contactDetails.error.fullName.invalid"))),
      "telephoneNumber" -> text("contactDetails.error.telephoneNumber.required")
        .verifying(firstError(
          maxLength(20, "contactDetails.error.telephoneNumber.length"),
          regexp(telephonePattern, "contactDetails.error.telephoneNumber.invalid"))),
      "emailAddress" -> text("contactDetails.error.emailAddress.required")
        .verifying(firstError(
          maxLength(50, "contactDetails.error.emailAddress.length"),
          regexp(emailPattern, "contactDetails.error.emailAddress.invalid")))
    )(ContactDetails.apply)(contactDetails => Some(Tuple.fromProductTyped(contactDetails)))
  )
 }
