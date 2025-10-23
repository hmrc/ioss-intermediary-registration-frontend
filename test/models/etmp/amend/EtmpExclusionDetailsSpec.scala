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

