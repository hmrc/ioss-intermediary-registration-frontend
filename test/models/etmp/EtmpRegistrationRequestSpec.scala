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

import models.{BankDetails, ContactDetails, Country}
import pages.{BankDetailsPage, ContactDetailsPage}
import pages.checkVatDetails.NiAddressPage
import pages.euDetails.HasFixedEstablishmentPage
import pages.previousIntermediaryRegistrations.HasPreviouslyRegisteredAsIntermediaryPage
import pages.tradingNames.HasTradingNamePage
import play.api.libs.json.{JsError, JsSuccess, Json}

import java.time.LocalDate

class EtmpRegistrationRequestSpec extends SpecBase {

  private val etmpRegistrationRequest: EtmpRegistrationRequest = arbitraryEtmpRegistrationRequest.arbitrary.sample.value
  
  "EtmpRegistrationRequest" - {

    "must deserialise/serialise to and from EtmpRegistrationRequest" in {

      val json = Json.obj(
        "administration" -> etmpRegistrationRequest.administration,
        "customerIdentification" -> etmpRegistrationRequest.customerIdentification,
        "tradingNames" -> etmpRegistrationRequest.tradingNames,
        "intermediaryDetails" -> etmpRegistrationRequest.intermediaryDetails,
        "otherAddress" -> etmpRegistrationRequest.otherAddress,
        "schemeDetails" -> etmpRegistrationRequest.schemeDetails,
        "bankDetails" -> etmpRegistrationRequest.bankDetails
      )

      val expectedResult = EtmpRegistrationRequest(
        administration = etmpRegistrationRequest.administration,
        customerIdentification = etmpRegistrationRequest.customerIdentification,
        tradingNames = etmpRegistrationRequest.tradingNames,
        intermediaryDetails = etmpRegistrationRequest.intermediaryDetails,
        otherAddress = etmpRegistrationRequest.otherAddress,
        schemeDetails = etmpRegistrationRequest.schemeDetails,
        bankDetails = etmpRegistrationRequest.bankDetails,
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpRegistrationRequest] mustBe JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpRegistrationRequest] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "administration" -> 12345,
        "customerIdentification" -> etmpRegistrationRequest.customerIdentification,
        "tradingNames" -> etmpRegistrationRequest.tradingNames,
        "schemeDetails" -> etmpRegistrationRequest.schemeDetails,
        "bankDetails" -> etmpRegistrationRequest.bankDetails
      )
      json.validate[EtmpRegistrationRequest] mustBe a[JsError]
    }

    "buildEtmpRegistrationRequest" - {

      "must set otherAddress issuedBy to Northern Ireland when feature flag is enabled" in {
        val answers = emptyUserAnswers
          .copy(vatInfo = Some(vatCustomerInfo))
          .set(HasTradingNamePage, false).success.value
          .set(HasPreviouslyRegisteredAsIntermediaryPage, false).success.value
          .set(HasFixedEstablishmentPage, false).success.value
          .set(ContactDetailsPage, contactDetails).success.value
          .set(BankDetailsPage, BankDetails("Account name", Some(bic), iban)).success.value
          .set(NiAddressPage, arbitraryUkAddress.arbitrary.sample.value).success.value

        val result = EtmpRegistrationRequest.buildEtmpRegistrationRequest(
          answers,
          vrn,
          LocalDate.now(),
          otherAddressNorthernIrelandCountryCode = true
        )

        result.otherAddress.value.issuedBy mustBe Country.northernIreland.code
      }

      "must set otherAddress issuedBy to United Kingdom when feature flag is disabled" in {
        val answers = emptyUserAnswers
          .copy(vatInfo = Some(vatCustomerInfo))
          .set(HasTradingNamePage, false).success.value
          .set(HasPreviouslyRegisteredAsIntermediaryPage, false).success.value
          .set(HasFixedEstablishmentPage, false).success.value
          .set(ContactDetailsPage, contactDetails).success.value
          .set(BankDetailsPage, BankDetails("Account name", Some(bic), iban)).success.value
          .set(NiAddressPage, arbitraryUkAddress.arbitrary.sample.value).success.value

        val result = EtmpRegistrationRequest.buildEtmpRegistrationRequest(
          answers,
          vrn,
          LocalDate.now(),
          otherAddressNorthernIrelandCountryCode = false
        )

        result.otherAddress.value.issuedBy mustBe Country.unitedKingdomCountry.code
      }
    }
  }
}

