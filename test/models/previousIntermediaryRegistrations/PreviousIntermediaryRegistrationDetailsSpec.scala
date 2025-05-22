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

class PreviousIntermediaryRegistrationDetailsSpec extends SpecBase {

  private val previousIntermediaryRegistrationDetails: PreviousIntermediaryRegistrationDetails =
    arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value

  "PreviousIntermediaryRegistrationDetails" - {

    "must serialise/deserialise from and to PreviousIntermediaryRegistrationDetails" in {

      val json = Json.obj(
        "previousEuCountry" -> previousIntermediaryRegistrationDetails.previousEuCountry,
        "previousIntermediaryNumber" -> previousIntermediaryRegistrationDetails.previousIntermediaryNumber
      )

      val expectedResult: PreviousIntermediaryRegistrationDetails = PreviousIntermediaryRegistrationDetails(
        previousEuCountry = previousIntermediaryRegistrationDetails.previousEuCountry,
        previousIntermediaryNumber = previousIntermediaryRegistrationDetails.previousIntermediaryNumber
      )

      json.validate[PreviousIntermediaryRegistrationDetails] mustBe JsSuccess(expectedResult)
      Json.toJson(expectedResult) mustBe json
    }
  }

  "must handle missing fields during deserialization" in {

    val json = Json.obj()

    json.validate[PreviousIntermediaryRegistrationDetails] mustBe a[JsError]
  }

  "must handle invalid data during deserialization" in {

    val json = Json.obj(
      "previousEuCountry" -> "Country",
      "previousIntermediaryNumber" -> 1234566789
    )

    json.validate[PreviousIntermediaryRegistrationDetails] mustBe a[JsError]
  }
}
