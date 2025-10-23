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
import models.etmp.*
import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{JsError, JsSuccess, Json}
import testutils.RegistrationData.etmpAmendRegistrationRequest

class EtmpAmendRegistrationRequestSpec extends SpecBase {

  private val administration: EtmpAdministration = etmpAmendRegistrationRequest.administration
  private val customerIdentification: EtmpAmendCustomerIdentification = etmpAmendRegistrationRequest.customerIdentification
  private val tradingNames: Seq[EtmpTradingName] = etmpAmendRegistrationRequest.tradingNames
  private val intermediaryDetails: Option[EtmpIntermediaryDetails] = etmpAmendRegistrationRequest.intermediaryDetails
  private val otherAddress: Option[EtmpOtherAddress] = etmpAmendRegistrationRequest.otherAddress
  private val schemeDetails: EtmpSchemeDetails = etmpAmendRegistrationRequest.schemeDetails
  private val bankDetails: EtmpBankDetails = etmpAmendRegistrationRequest.bankDetails
  private val changeLog: EtmpAmendRegistrationChangeLog = arbitrary[EtmpAmendRegistrationChangeLog].sample.value
  private val exclusionDetails: EtmpExclusionDetails = arbitraryEtmpExclusionDetails.arbitrary.sample.value

  "EtmpAmendRegistrationRequest" - {

    "must deserialise/serialise to and from EtmpAmendRegistrationRequest" - {

      "when all optional values are present" in {

        val json = Json.obj(
          "administration" -> administration,
          "changeLog" -> changeLog,
          "exclusionDetails" -> Some(exclusionDetails),
          "customerIdentification" -> customerIdentification,
          "tradingNames" -> tradingNames,
          "intermediaryDetails" -> intermediaryDetails,
          "otherAddress" -> otherAddress,
          "schemeDetails" -> schemeDetails,
          "bankDetails" -> bankDetails
        )

        val expectedResult = EtmpAmendRegistrationRequest(
          administration = administration,
          changeLog = changeLog,
          exclusionDetails = Some(exclusionDetails),
          customerIdentification = customerIdentification,
          tradingNames = tradingNames,
          intermediaryDetails = intermediaryDetails,
          otherAddress = otherAddress,
          schemeDetails = schemeDetails,
          bankDetails = bankDetails,
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpAmendRegistrationRequest] mustBe JsSuccess(expectedResult)
      }

      "when all optional values are absent" in {

        val json = Json.obj(
          "administration" -> administration,
          "changeLog" -> changeLog,
          "customerIdentification" -> customerIdentification,
          "tradingNames" -> Json.arr(),
          "schemeDetails" -> schemeDetails,
          "bankDetails" -> bankDetails
        )

        val expectedResult = EtmpAmendRegistrationRequest(
          administration = administration,
          changeLog = changeLog,
          exclusionDetails = None,
          customerIdentification = customerIdentification,
          tradingNames = Seq.empty,
          intermediaryDetails = None,
          otherAddress = None,
          schemeDetails = schemeDetails,
          bankDetails = bankDetails,
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpAmendRegistrationRequest] mustBe JsSuccess(expectedResult)
      }
    }

    "must handle missing fields during deserialization" in {
      val json = Json.obj()

      json.validate[EtmpAmendRegistrationRequest] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "administration" -> administration,
        "changeLog" -> changeLog,
        "customerIdentification" -> customerIdentification,
        "tradingNames" -> 12345,
        "intermediaryDetails" -> intermediaryDetails,
        "otherAddress" -> otherAddress,
        "schemeDetails" -> schemeDetails,
        "bankDetails" -> bankDetails
      )

      json.validate[EtmpAmendRegistrationRequest] mustBe a[JsError]
    }
  }
}

