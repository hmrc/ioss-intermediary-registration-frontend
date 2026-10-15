/*
 * Copyright 2026 HM Revenue & Customs
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

package models.returns

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, Json}

class CurrentReturnsSpec extends SpecBase {

  private val currentReturns: CurrentReturns = arbitraryCurrentReturns.arbitrary.sample.value

  "CurrentReturns" - {

    "must deserialise/serialise from and to CurrentReturns" in {

      val json = Json.obj(
        "iossNumber" -> currentReturns.iossNumber,
        "incompleteReturns" -> currentReturns.incompleteReturns,
        "completedReturns" -> currentReturns.completedReturns,
        "finalReturnsCompleted" -> currentReturns.finalReturnsCompleted
      )

      val expectedResult = CurrentReturns(
        iossNumber = currentReturns.iossNumber,
        incompleteReturns = currentReturns.incompleteReturns,
        completedReturns = currentReturns.completedReturns,
        finalReturnsCompleted = currentReturns.finalReturnsCompleted
      )

      Json.toJson(expectedResult) `mustBe` json
      json.validate[CurrentReturns] `mustBe` JsSuccess[CurrentReturns](expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[CurrentReturns] `mustBe` a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "iossNumber" -> currentReturns.iossNumber,
        "incompleteReturns" -> 123456,
        "completedReturns" -> currentReturns.completedReturns
      )

      json.validate[CurrentReturns] `mustBe` a[JsError]
    }
  }
}
