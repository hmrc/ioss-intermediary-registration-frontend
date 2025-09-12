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

class EtmpExclusionSpec extends SpecBase {

  private val etmpExclusion: EtmpExclusion = arbitraryEtmpExclusion.arbitrary.sample.value

  "EtmpExclusion" - {

    "must deserialise/serialise from and to EtmpExclusion" in {

      val json = Json.obj(
        "exclusionReason" -> etmpExclusion.exclusionReason,
        "effectiveDate" -> etmpExclusion.effectiveDate,
        "decisionDate" -> etmpExclusion.decisionDate,
        "quarantine" -> etmpExclusion.quarantine
      )

      val expectedResult: EtmpExclusion = EtmpExclusion(
        exclusionReason = etmpExclusion.exclusionReason,
        effectiveDate = etmpExclusion.effectiveDate,
        decisionDate = etmpExclusion.decisionDate,
        quarantine = etmpExclusion.quarantine
      )

      Json.toJson(expectedResult) `mustBe` json
      json.validate[EtmpExclusion] `mustBe` JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpExclusion] `mustBe` a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "exclusionReason" -> 123456
      )

      json.validate[EtmpExclusion] `mustBe` a[JsError]
    }
  }
}
