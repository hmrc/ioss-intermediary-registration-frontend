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

package models

import base.SpecBase
import models.CompositeAccount.fromIossEtmpBankDetails
import models.iossRegistration.{IossEtmpBankDetails, IossEtmpDisplayRegistration}
import models.ossRegistration.OssRegistration
import play.api.libs.json.{JsError, JsSuccess, Json}

class CompositeAccountSpec extends SpecBase {

  private val compositeAccount: CompositeAccount = arbitraryCompositeAccount.arbitrary.sample.value
  private val iossEtmpBankDetails: IossEtmpBankDetails = arbitraryIossEtmpBankDetails.arbitrary.sample.value

  private val ossRegistration: OssRegistration = arbitraryOssRegistration.arbitrary.sample.value
  private val iossRegistration: IossEtmpDisplayRegistration = arbitraryIossEtmpDisplayRegistration.arbitrary.sample.value

  "CompositeAccount" - {

    "must serialise/deserialise to and from CompositeAccount" - {

      val json = Json.obj(
        "tradingNames" -> compositeAccount.tradingNames,
        "contactDetails" -> compositeAccount.contactDetails,
        "bankDetails" -> compositeAccount.bankDetails
      )

      val expectedResult = CompositeAccount(
        tradingNames = compositeAccount.tradingNames,
        contactDetails = compositeAccount.contactDetails,
        bankDetails = compositeAccount.bankDetails
      )

      Json.toJson(expectedResult) `mustBe` json
      json.validate[CompositeAccount] `mustBe` JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val expectedJson = Json.obj()

      expectedJson.validate[CompositeAccount] `mustBe` a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val expectedJson = Json.obj(
        "tradingNames" -> compositeAccount.tradingNames,
        "contactDetails" -> compositeAccount.contactDetails.emailAddress,
        "bankDetails" -> compositeAccount.bankDetails
      )

      expectedJson.validate[CompositeAccount] `mustBe` a[JsError]
    }

    ".fromIossEtmpBankDetails" - {

      "must return a correctly populated BankDetails model when given IossEtmpBankDetails data" in {

        val expectedResult: BankDetails = BankDetails(
          accountName = iossEtmpBankDetails.accountName,
          bic = iossEtmpBankDetails.bic,
          iban = iossEtmpBankDetails.iban,
        )

        CompositeAccount.fromIossEtmpBankDetails(iossEtmpBankDetails) `mustBe` expectedResult
      }
    }

    ".createCompositeAccount" - {

      "must return a correctly populated CompositeAccount model when given details from an OSS registration" in {

        val contactDetails: ContactDetails = ContactDetails(
          fullName = ossRegistration.contactDetails.fullName,
          telephoneNumber = ossRegistration.contactDetails.telephoneNumber,
          emailAddress = ossRegistration.contactDetails.emailAddress
        )

        val expectedResult: CompositeAccount = CompositeAccount(
          tradingNames = ossRegistration.tradingNames.map(TradingName(_)),
          contactDetails = contactDetails,
          bankDetails = ossRegistration.bankDetails,
        )

        CompositeAccount.createCompositeAccount(
          tradingNames = ossRegistration.tradingNames,
          fullName = ossRegistration.contactDetails.fullName,
          telephoneNumber = ossRegistration.contactDetails.telephoneNumber,
          emailAddress = ossRegistration.contactDetails.emailAddress,
          bankDetails = ossRegistration.bankDetails
        ) `mustBe` expectedResult
      }
      
      "must return a correctly populated CompositeAccount model when given details from an IOSS registration" in {

        val contactDetails: ContactDetails = ContactDetails(
          fullName = iossRegistration.schemeDetails.contactName,
          telephoneNumber = iossRegistration.schemeDetails.businessTelephoneNumber,
          emailAddress = iossRegistration.schemeDetails.businessEmailId
        )

        val expectedResult: CompositeAccount = CompositeAccount(
          tradingNames = iossRegistration.tradingNames.map(tn => TradingName(tn.tradingName)),
          contactDetails = contactDetails,
          bankDetails = fromIossEtmpBankDetails(iossRegistration.bankDetails),
        )

        CompositeAccount.createCompositeAccount(
          tradingNames = iossRegistration.tradingNames.map(_.tradingName),
          fullName = iossRegistration.schemeDetails.contactName,
          telephoneNumber = iossRegistration.schemeDetails.businessTelephoneNumber,
          emailAddress = iossRegistration.schemeDetails.businessEmailId,
          bankDetails = fromIossEtmpBankDetails(iossRegistration.bankDetails)
        ) `mustBe` expectedResult
      }
    }
  }
}
