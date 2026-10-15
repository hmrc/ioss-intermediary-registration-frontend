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

package models.etmp

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, Json}

class EtmpAdminUseSpec extends SpecBase {

  private val etmpAdminUse: EtmpAdminUse = arbitraryEtmpAdminUse.arbitrary.sample.value

  "EtmpAdminUse" - {

    "must deserialise/serialise from and to EtmpAdminUse" - {

      "when the optional value is present" in {

        val json = Json.obj(
          "changeDate" -> etmpAdminUse.changeDate
        )

        val expectedResult: EtmpAdminUse = EtmpAdminUse(
          changeDate = etmpAdminUse.changeDate
        )

        Json.toJson(expectedResult) `mustBe` json
        json.validate[EtmpAdminUse] `mustBe` JsSuccess(expectedResult)
      }

      "when the optional value is absent" in {

        val json = Json.obj()

        val expectedResult: EtmpAdminUse = EtmpAdminUse(
          changeDate = None
        )

        Json.toJson(expectedResult) `mustBe` json
        json.validate[EtmpAdminUse] `mustBe` JsSuccess(expectedResult)
      }
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "changeDate" -> "123456"
      )

      json.validate[EtmpAdminUse] `mustBe` a[JsError]
    }
  }
}
