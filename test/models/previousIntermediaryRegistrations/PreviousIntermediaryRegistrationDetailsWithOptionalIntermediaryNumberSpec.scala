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

package models.previousIntermediaryRegistrations

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, Json}

class PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumberSpec extends SpecBase {

  private val previousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber: PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber =
    arbitraryPreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber.arbitrary.sample.value

  "PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber" - {

    "must serialise/deserialise from and to PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber" - {

      "with all optional fields present" in {

        val json = Json.obj(
          "previousEuCountry" -> previousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber.previousEuCountry,
          "previousIntermediaryNumber" -> previousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber.previousIntermediaryNumber
        )

        val expectedResult: PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber =
          PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber(
            previousEuCountry = previousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber.previousEuCountry,
            previousIntermediaryNumber = previousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber.previousIntermediaryNumber
          )

        json.validate[PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber] mustBe JsSuccess(expectedResult)
        Json.toJson(expectedResult) mustBe json
      }

      "with all optional fields absent" in {

        val json = Json.obj(
          "previousEuCountry" -> previousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber.previousEuCountry
        )

        val expectedResult: PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber =
          PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber(
            previousEuCountry = previousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber.previousEuCountry,
            previousIntermediaryNumber = None
          )

        json.validate[PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber] mustBe JsSuccess(expectedResult)
        Json.toJson(expectedResult) mustBe json
      }
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "previousEuCountry" -> "Country",
        "previousIntermediaryNumber" -> 1234566789
      )

      json.validate[PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber] mustBe a[JsError]
    }
  }
}
