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

package models

import models.domain.ModelHelpers.normaliseSpaces
import models.etmp.display.EtmpDisplaySchemeDetails
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*

case class ContactDetails(fullName: String,
                          telephoneNumber: String,
                          emailAddress: String) {

  def resetToOriginal(schemeDetails: EtmpDisplaySchemeDetails): ContactDetails = {
    this.copy(
      fullName = if (this.fullName != schemeDetails.contactName) schemeDetails.contactName else this.fullName,
      telephoneNumber = if (this.telephoneNumber != schemeDetails.businessTelephoneNumber) schemeDetails.businessTelephoneNumber else this.telephoneNumber,
      emailAddress = if (this.emailAddress != schemeDetails.businessEmailId) schemeDetails.businessEmailId else this.emailAddress
    )
  }

  def differsFromOriginal(schemeDetails: EtmpDisplaySchemeDetails): Boolean = {
      this.fullName != schemeDetails.contactName ||
      this.telephoneNumber != schemeDetails.businessTelephoneNumber ||
      this.emailAddress != schemeDetails.businessEmailId
  }
}

object ContactDetails {
  implicit val reads: Reads[ContactDetails] = (

    (__ \ "fullName").read[String].map(normaliseSpaces) and
      (__ \ "telephoneNumber").read[String] and
      (__ \ "emailAddress").read[String]
    )(ContactDetails.apply _)

  implicit val writes: Writes[ContactDetails] = Json.writes[ContactDetails]

  def apply(fullName: String, telephoneNumber: String, emailAddress: String): ContactDetails =
    new ContactDetails(normaliseSpaces(fullName), telephoneNumber, emailAddress)

}
