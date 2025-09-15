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

class EtmpRegistrationRequestSpec extends SpecBase {

  private val etmpRegistrationRequest: EtmpRegistrationRequest = arbitraryEtmpRegistrationRequest.arbitrary.sample.value
  
  "EtmpRegistrationRequest" - {

    "must deserialise/serialise to and from EtmpRegistrationRequest" in {

      val json = Json.obj(
        "administration" -> etmpRegistrationRequest.administration,
        "customerIdentification" -> etmpRegistrationRequest.customerIdentification,
        "tradingNames" -> etmpRegistrationRequest.tradingNames,
        "intermediaryDetails" -> etmpRegistrationRequest.intermediaryDetails,
        "otherAddress" -> etmpRegistrationRequest.otherAddress,
        "schemeDetails" -> etmpRegistrationRequest.schemeDetails,
        "bankDetails" -> etmpRegistrationRequest.bankDetails
      )

      val expectedResult = EtmpRegistrationRequest(
        administration = etmpRegistrationRequest.administration,
        customerIdentification = etmpRegistrationRequest.customerIdentification,
        tradingNames = etmpRegistrationRequest.tradingNames,
        intermediaryDetails = etmpRegistrationRequest.intermediaryDetails,
        otherAddress = etmpRegistrationRequest.otherAddress,
        schemeDetails = etmpRegistrationRequest.schemeDetails,
        bankDetails = etmpRegistrationRequest.bankDetails,
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpRegistrationRequest] mustBe JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpRegistrationRequest] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "administration" -> 12345,
        "customerIdentification" -> etmpRegistrationRequest.customerIdentification,
        "tradingNames" -> etmpRegistrationRequest.tradingNames,
        "schemeDetails" -> etmpRegistrationRequest.schemeDetails,
        "bankDetails" -> etmpRegistrationRequest.bankDetails
      )
      json.validate[EtmpRegistrationRequest] mustBe a[JsError]
    }
  }
}

