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

package models.etmp.amend

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, Json}

class EtmpExclusionDetailsSpec extends SpecBase {

  private val etmpExclusionDetails: EtmpExclusionDetails = arbitraryEtmpExclusionDetails.arbitrary.sample.value

  "EtmpExclusionDetails" - {

    "must deserialise/serialise to and from EtmpExclusionDetails" - {

      "when all optional values are present" in {

        val json = Json.obj(
          "revertExclusion" -> etmpExclusionDetails.revertExclusion,
          "noLongerSupplyGoods" -> etmpExclusionDetails.noLongerSupplyGoods,
          "noLongerEligible" -> etmpExclusionDetails.noLongerEligible,
          "partyType" -> etmpExclusionDetails.partyType,
          "exclusionRequestDate" -> etmpExclusionDetails.exclusionRequestDate
        )

        val expectedResult = EtmpExclusionDetails(
          revertExclusion = etmpExclusionDetails.revertExclusion,
          noLongerSupplyGoods = etmpExclusionDetails.noLongerSupplyGoods,
          noLongerEligible = etmpExclusionDetails.noLongerEligible,
          partyType = etmpExclusionDetails.partyType,
          exclusionRequestDate = etmpExclusionDetails.exclusionRequestDate
        )

        Json.toJson(expectedResult) `mustBe` json
        json.validate[EtmpExclusionDetails] `mustBe` JsSuccess(expectedResult)
      }

      "when all optional values are absent" in {

        val json = Json.obj(
          "revertExclusion" -> etmpExclusionDetails.revertExclusion,
          "noLongerSupplyGoods" -> etmpExclusionDetails.noLongerSupplyGoods,
          "noLongerEligible" -> etmpExclusionDetails.noLongerEligible,
          "partyType" -> etmpExclusionDetails.partyType
        )

        val expectedResult = EtmpExclusionDetails(
          revertExclusion = etmpExclusionDetails.revertExclusion,
          noLongerSupplyGoods = etmpExclusionDetails.noLongerSupplyGoods,
          noLongerEligible = etmpExclusionDetails.noLongerEligible,
          partyType = etmpExclusionDetails.partyType,
          exclusionRequestDate = None
        )

        Json.toJson(expectedResult) `mustBe` json
        json.validate[EtmpExclusionDetails] `mustBe` JsSuccess(expectedResult)
      }
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "revertExclusion" -> etmpExclusionDetails.revertExclusion,
        "noLongerSupplyGoods" -> etmpExclusionDetails.noLongerSupplyGoods,
        "noLongerEligible" -> 123456,
        "partyType" -> etmpExclusionDetails.partyType,
        "exclusionRequestDate" -> etmpExclusionDetails.exclusionRequestDate
      )

      json.validate[EtmpExclusionDetails] `mustBe` a[JsError]
    }
  }
}

