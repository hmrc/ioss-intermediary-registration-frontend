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

package models.euDetails

import base.SpecBase
import models.Country
import play.api.libs.json.*

class EuDetailsSpec extends SpecBase {

  private val euDetails: EuDetails = arbitraryEuDetails.arbitrary.sample.value

  "EuDetails" - {

    "must serialise/deserialise to and from EuDetails" - {

      "with all optional fields present" in {

        val json = Json.obj(
          "euCountry" -> euDetails.euCountry,
          "hasFixedEstablishment" -> euDetails.hasFixedEstablishment,
          "registrationType" -> euDetails.registrationType,
          "euVatNumber" -> euDetails.euVatNumber,
          "euTaxReference" -> euDetails.euTaxReference,
          "fixedEstablishmentTradingName" -> euDetails.fixedEstablishmentTradingName,
          "fixedEstablishmentAddress" -> euDetails.fixedEstablishmentAddress
        )

        val expectedResult = EuDetails(
          euCountry = euDetails.euCountry,
          hasFixedEstablishment = euDetails.hasFixedEstablishment,
          registrationType = euDetails.registrationType,
          euVatNumber = euDetails.euVatNumber,
          euTaxReference = euDetails.euTaxReference,
          fixedEstablishmentTradingName = euDetails.fixedEstablishmentTradingName,
          fixedEstablishmentAddress = euDetails.fixedEstablishmentAddress
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EuDetails] mustBe JsSuccess(expectedResult)
      }

      "with all optional fields absent" in {

        val json = Json.obj(
          "euCountry" -> euDetails.euCountry,
        )

        val expectedResult = EuDetails(
          euCountry = euDetails.euCountry,
          hasFixedEstablishment = None,
          registrationType = None,
          euVatNumber = None,
          euTaxReference = None,
          fixedEstablishmentTradingName = None,
          fixedEstablishmentAddress = None
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EuDetails] mustBe JsSuccess(expectedResult)
      }

      "must handle missing fields during deserialization" in {

        val expectedJson = Json.obj()

        expectedJson.validate[EuDetails] mustBe a[JsError]
      }

      "must handle invalid data during deserialization" in {

        val expectedJson = Json.obj(
          "euCountry" -> 12345
        )

        expectedJson.validate[EuDetails] mustBe a[JsError]
      }
    }
  }
}
