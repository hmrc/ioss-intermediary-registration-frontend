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

class ReturnSpec extends SpecBase {

  private val arbReturn: Return = arbitraryReturn.arbitrary.sample.value

  "Return" - {

    "must deserialise/serialise from and to Return" in {

      val json = Json.obj(
        "period" -> arbReturn.period,
        "firstDay" -> arbReturn.firstDay,
        "lastDay" -> arbReturn.lastDay,
        "dueDate" -> arbReturn.dueDate,
        "submissionStatus" -> arbReturn.submissionStatus,
        "inProgress" -> arbReturn.inProgress,
        "isOldest" -> arbReturn.isOldest
      )

      val expectedResult = Return(
        period = arbReturn.period,
        firstDay = arbReturn.firstDay,
        lastDay = arbReturn.lastDay,
        dueDate = arbReturn.dueDate,
        submissionStatus = arbReturn.submissionStatus,
        inProgress = arbReturn.inProgress,
        isOldest = arbReturn.isOldest
      )

      Json.toJson(expectedResult) `mustBe` json
      json.validate[Return] `mustBe` JsSuccess[Return](expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[Return] `mustBe` a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "period" -> arbReturn.period,
        "firstDay" -> arbReturn.firstDay,
        "lastDay" -> arbReturn.lastDay,
        "dueDate" -> arbReturn.dueDate,
        "submissionStatus" -> 123456,
        "inProgress" -> arbReturn.inProgress,
        "isOldest" -> arbReturn.isOldest
      )

      json.validate[Return] `mustBe` a[JsError]
    }
  }
}
