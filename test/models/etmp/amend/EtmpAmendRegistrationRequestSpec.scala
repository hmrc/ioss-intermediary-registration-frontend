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

  private val administration = etmpAmendRegistrationRequest.administration
  private val customerIdentification = etmpAmendRegistrationRequest.customerIdentification
  private val tradingNames = etmpAmendRegistrationRequest.tradingNames
  private val intermediaryDetails = etmpAmendRegistrationRequest.intermediaryDetails
  private val otherAddress = etmpAmendRegistrationRequest.otherAddress
  private val schemeDetails = etmpAmendRegistrationRequest.schemeDetails
  private val bankDetails = etmpAmendRegistrationRequest.bankDetails
  private val changeLog = arbitrary[EtmpAmendRegistrationChangeLog].sample.value

  "EtmpAmendRegistrationRequest" - {

    "must deserialise/serialise to and from EtmpAmendRegistrationRequest" in {

      val json = Json.obj(
        "administration" -> administration,
        "changeLog" -> changeLog,
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

