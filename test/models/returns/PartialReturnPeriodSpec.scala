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

package models.returns

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, Json}

class PartialReturnPeriodSpec extends SpecBase {

  private val partialReturnPeriod: PartialReturnPeriod =
    arbitraryPartialReturnPeriod.arbitrary.sample.value

  "PartialReturnPeriod" - {

    "must deserialise/serialise from and to PartialReturnPeriod" in {

      val json = Json.obj(
        "firstDay" -> partialReturnPeriod.firstDay,
        "lastDay" -> partialReturnPeriod.lastDay,
        "year" -> partialReturnPeriod.year,
        "month" -> s"M${partialReturnPeriod.month.getValue}"
      )

      val expectedResult = PartialReturnPeriod(
        firstDay = partialReturnPeriod.firstDay,
        lastDay = partialReturnPeriod.lastDay,
        year = partialReturnPeriod.year,
        month = partialReturnPeriod.month
      )

      Json.toJson(expectedResult) `mustBe` json
      json.validate[PartialReturnPeriod] `mustBe` JsSuccess[PartialReturnPeriod](expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[PartialReturnPeriod] `mustBe` a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "firstDay" -> partialReturnPeriod.firstDay,
        "lastDay" -> partialReturnPeriod.lastDay,
        "year" -> "2025",
        "month" -> s"M${partialReturnPeriod.month.getValue}"
      )

      json.validate[PartialReturnPeriod] `mustBe` a[JsError]
    }
  }
}
