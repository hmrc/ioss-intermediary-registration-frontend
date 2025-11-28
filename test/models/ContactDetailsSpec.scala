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

import base.SpecBase
import models.etmp.display.EtmpDisplaySchemeDetails
import play.api.libs.json.{JsError, JsSuccess, Json}

class ContactDetailsSpec extends SpecBase {


  "ContactDetails" - {

    "must serialise/deserialise to and from ContactDetails" - {

      "with all fields present" in {

        val json = Json.obj(
          "fullName" -> contactDetails.fullName,
          "telephoneNumber" -> contactDetails.telephoneNumber,
          "emailAddress" -> contactDetails.emailAddress
        )

        val expectedResult = ContactDetails(
          fullName = contactDetails.fullName,
          telephoneNumber = contactDetails.telephoneNumber,
          emailAddress = contactDetails.emailAddress
        )

        Json.toJson(expectedResult) `mustBe` json
        json.validate[ContactDetails] `mustBe` JsSuccess(expectedResult)
      }

      "must handle missing fields during deserialization" in {

        val expectedJson = Json.obj()

        expectedJson.validate[ContactDetails] `mustBe` a[JsError]
      }

      "must handle invalid data during deserialization" in {

        val expectedJson = Json.obj(
          "fullName" -> "First Second",
          "telephoneNumber" -> 1234565,
          "emailAddress" -> "email@email.com"
        )

        expectedJson.validate[ContactDetails] `mustBe` a[JsError]
      }
    }

    ".resetToOriginal" - {
      "should return Contact Details unchanged when Contact Details matches the original Scheme Details" in {
        val testSchemeDetails: EtmpDisplaySchemeDetails = arbitraryEtmpDisplaySchemeDetails.arbitrary.sample.value

        val testContactDetails: ContactDetails = ContactDetails(
          fullName = testSchemeDetails.contactName,
          telephoneNumber = testSchemeDetails.businessTelephoneNumber,
          emailAddress = testSchemeDetails.businessEmailId
        )

        val result = testContactDetails.resetToOriginal(testSchemeDetails)

        result mustBe testContactDetails
      }

      "should return Contact Details Changed to match the original Scheme Details when Contact Details have been changed" in {
        val testSchemeDetails: EtmpDisplaySchemeDetails = arbitraryEtmpDisplaySchemeDetails.arbitrary.sample.value

        val testContactDetails: ContactDetails = ContactDetails(
          fullName = "DifferentName",
          telephoneNumber = testSchemeDetails.businessTelephoneNumber,
          emailAddress = testSchemeDetails.businessEmailId
        )

        val result = testContactDetails.resetToOriginal(testSchemeDetails)

        result must not be testContactDetails
        result.fullName mustBe testSchemeDetails.contactName
      }

    }

    ".differsFromOriginal" - {

      "should return TRUE when the name differs between contactDetails and schemeDetails" in {
        val testSchemeDetails: EtmpDisplaySchemeDetails = arbitraryEtmpDisplaySchemeDetails.arbitrary.sample.value

        val testContactDetails: ContactDetails = ContactDetails(
          fullName = "varying field",
          telephoneNumber = testSchemeDetails.businessTelephoneNumber,
          emailAddress = testSchemeDetails.businessEmailId
        )

        val result: Boolean = testContactDetails.differsFromOriginal(testSchemeDetails)

        result mustBe true
      }

      "should return TRUE when the telephoneNumber differs between contactDetails and schemeDetails" in {
        val testSchemeDetails: EtmpDisplaySchemeDetails = arbitraryEtmpDisplaySchemeDetails.arbitrary.sample.value

        val testContactDetails: ContactDetails = ContactDetails(
          fullName = testSchemeDetails.contactName,
          telephoneNumber = "varying field",
          emailAddress = testSchemeDetails.businessEmailId
        )

        val result: Boolean = testContactDetails.differsFromOriginal(testSchemeDetails)

        result mustBe true

      }

      "should return TRUE when the emailAddress differs between contactDetails and schemeDetails" in {
        val testSchemeDetails: EtmpDisplaySchemeDetails = arbitraryEtmpDisplaySchemeDetails.arbitrary.sample.value

        val testContactDetails: ContactDetails = ContactDetails(
          fullName = testSchemeDetails.contactName,
          telephoneNumber = testSchemeDetails.businessTelephoneNumber,
          emailAddress = "varying field"
        )

        val result: Boolean = testContactDetails.differsFromOriginal(testSchemeDetails)

        result mustBe true

      }

      "should return TRUE when more than one field differs between contactDetails and schemeDetails" in {
        val testSchemeDetails: EtmpDisplaySchemeDetails = arbitraryEtmpDisplaySchemeDetails.arbitrary.sample.value

        val testContactDetails: ContactDetails = ContactDetails(
          fullName = "varying field",
          telephoneNumber = testSchemeDetails.businessTelephoneNumber,
          emailAddress = "varying field"
        )

        val result: Boolean = testContactDetails.differsFromOriginal(testSchemeDetails)

        result mustBe true

      }

      "should return FALSE when all fields match between contactDetails and schemeDetails" in {
        val testSchemeDetails: EtmpDisplaySchemeDetails = arbitraryEtmpDisplaySchemeDetails.arbitrary.sample.value

        val testContactDetails: ContactDetails = ContactDetails(
          fullName = testSchemeDetails.contactName,
          telephoneNumber = testSchemeDetails.businessTelephoneNumber,
          emailAddress = testSchemeDetails.businessEmailId
        )

        val result: Boolean = testContactDetails.differsFromOriginal(testSchemeDetails)

        result mustBe false

      }
    }
  }
}
