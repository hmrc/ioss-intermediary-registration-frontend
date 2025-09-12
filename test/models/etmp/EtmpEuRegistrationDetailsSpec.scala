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

class EtmpEuRegistrationDetailsSpec extends SpecBase {

  private val etmpEuRegistrationDetails: EtmpEuRegistrationDetails =
    arbitraryEtmpEuRegistrationDetails.arbitrary.sample.value

  "EtmpEuRegistrationDetails" - {

    "must deserialise/serialise from and to EtmpEuRegistrationDetails" - {

      "when all optional values are present" in {

        val json = Json.obj(
          "countryOfRegistration" -> etmpEuRegistrationDetails.countryOfRegistration,
          "traderId" -> etmpEuRegistrationDetails.traderId,
          "tradingName" -> etmpEuRegistrationDetails.tradingName,
          "fixedEstablishmentAddressLine1" -> etmpEuRegistrationDetails.fixedEstablishmentAddressLine1,
          "fixedEstablishmentAddressLine2" -> etmpEuRegistrationDetails.fixedEstablishmentAddressLine2,
          "townOrCity" -> etmpEuRegistrationDetails.townOrCity,
          "regionOrState" -> etmpEuRegistrationDetails.regionOrState,
          "postcode" -> etmpEuRegistrationDetails.postcode
        )

        val expectedResult = EtmpEuRegistrationDetails(
          countryOfRegistration = etmpEuRegistrationDetails.countryOfRegistration,
          traderId = etmpEuRegistrationDetails.traderId,
          tradingName = etmpEuRegistrationDetails.tradingName,
          fixedEstablishmentAddressLine1 = etmpEuRegistrationDetails.fixedEstablishmentAddressLine1,
          fixedEstablishmentAddressLine2 = etmpEuRegistrationDetails.fixedEstablishmentAddressLine2,
          townOrCity = etmpEuRegistrationDetails.townOrCity,
          regionOrState = etmpEuRegistrationDetails.regionOrState,
          postcode = etmpEuRegistrationDetails.postcode
        )

        Json.toJson(expectedResult) `mustBe` json
        json.validate[EtmpEuRegistrationDetails] `mustBe` JsSuccess(expectedResult)
      }

      "when all optional values are absent" in {

        val json = Json.obj(
          "countryOfRegistration" -> etmpEuRegistrationDetails.countryOfRegistration,
          "traderId" -> etmpEuRegistrationDetails.traderId,
          "tradingName" -> etmpEuRegistrationDetails.tradingName,
          "fixedEstablishmentAddressLine1" -> etmpEuRegistrationDetails.fixedEstablishmentAddressLine1,
          "townOrCity" -> etmpEuRegistrationDetails.townOrCity
        )

        val expectedResult = EtmpEuRegistrationDetails(
          countryOfRegistration = etmpEuRegistrationDetails.countryOfRegistration,
          traderId = etmpEuRegistrationDetails.traderId,
          tradingName = etmpEuRegistrationDetails.tradingName,
          fixedEstablishmentAddressLine1 = etmpEuRegistrationDetails.fixedEstablishmentAddressLine1,
          fixedEstablishmentAddressLine2 = None,
          townOrCity = etmpEuRegistrationDetails.townOrCity,
          regionOrState = None,
          postcode = None
        )

        Json.toJson(expectedResult) `mustBe` json
        json.validate[EtmpEuRegistrationDetails] `mustBe` JsSuccess(expectedResult)
      }

      "must handle missing fields during deserialization" in {

        val json = Json.obj()

        json.validate[EtmpEuRegistrationDetails] `mustBe` a[JsError]
      }

      "must handle invalid fields during deserialization" in {

        val json = Json.obj(
          "countryOfRegistration" -> 123456
        )

        json.validate[EtmpEuRegistrationDetails] `mustBe` a[JsError]
      }
    }
  }
}

