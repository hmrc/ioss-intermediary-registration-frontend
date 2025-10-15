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

package models.etmp

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, Json}

class EtmpSchemeDetailsSpec extends SpecBase {

  private val etmpSchemeDetails: EtmpSchemeDetails = arbitraryEtmpSchemeDetails.arbitrary.sample.value

  "EtmpSchemeDetails" - {

    "must deserialise/serialise to and from EtmpSchemeDetails" - {

      "when all optional values are present" in {

        val json = Json.obj(
          "commencementDate" -> etmpSchemeDetails.commencementDate,
          "euRegistrationDetails" -> etmpSchemeDetails.euRegistrationDetails,
          "previousEURegistrationDetails" -> etmpSchemeDetails.previousEURegistrationDetails,
          "contactName" -> etmpSchemeDetails.contactName,
          "businessTelephoneNumber" -> etmpSchemeDetails.businessTelephoneNumber,
          "businessEmailId" -> etmpSchemeDetails.businessEmailId,
          "nonCompliantReturns" -> etmpSchemeDetails.nonCompliantReturns,
          "nonCompliantPayments" -> etmpSchemeDetails.nonCompliantPayments
        )

        val expectedResult = EtmpSchemeDetails(
          commencementDate = etmpSchemeDetails.commencementDate,
          euRegistrationDetails = etmpSchemeDetails.euRegistrationDetails,
          previousEURegistrationDetails = etmpSchemeDetails.previousEURegistrationDetails,
          contactName = etmpSchemeDetails.contactName,
          businessTelephoneNumber = etmpSchemeDetails.businessTelephoneNumber,
          businessEmailId = etmpSchemeDetails.businessEmailId,
          nonCompliantReturns = etmpSchemeDetails.nonCompliantReturns,
          nonCompliantPayments = etmpSchemeDetails.nonCompliantPayments
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpSchemeDetails] mustBe JsSuccess(expectedResult)
      }

      "when all optional values are absent" in {

        val json = Json.obj(
          "commencementDate" -> etmpSchemeDetails.commencementDate,
          "euRegistrationDetails" -> etmpSchemeDetails.euRegistrationDetails,
          "previousEURegistrationDetails" -> etmpSchemeDetails.previousEURegistrationDetails,
          "contactName" -> etmpSchemeDetails.contactName,
          "businessTelephoneNumber" -> etmpSchemeDetails.businessTelephoneNumber,
          "businessEmailId" -> etmpSchemeDetails.businessEmailId,
        )

        val expectedResult = EtmpSchemeDetails(
          commencementDate = etmpSchemeDetails.commencementDate,
          euRegistrationDetails = etmpSchemeDetails.euRegistrationDetails,
          previousEURegistrationDetails = etmpSchemeDetails.previousEURegistrationDetails,
          contactName = etmpSchemeDetails.contactName,
          businessTelephoneNumber = etmpSchemeDetails.businessTelephoneNumber,
          businessEmailId = etmpSchemeDetails.businessEmailId,
          nonCompliantReturns = None,
          nonCompliantPayments = None
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpSchemeDetails] mustBe JsSuccess(expectedResult)
      }
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpSchemeDetails] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "commencementDate" -> 12345,
        "euRegistrationDetails" -> etmpSchemeDetails.euRegistrationDetails,
        "previousEURegistrationDetails" -> etmpSchemeDetails.previousEURegistrationDetails,
        "contactName" -> etmpSchemeDetails.contactName,
        "businessTelephoneNumber" -> etmpSchemeDetails.businessTelephoneNumber,
        "businessEmailId" -> etmpSchemeDetails.businessEmailId,
      )

      json.validate[EtmpSchemeDetails] mustBe a[JsError]
    }
  }
}

