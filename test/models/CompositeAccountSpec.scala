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
import models.iossRegistration.IossEtmpBankDetails
import play.api.libs.json.{JsError, JsSuccess, Json}

class CompositeAccountSpec extends SpecBase {

  private val compositeAccount: CompositeAccount = arbitraryCompositeAccount.arbitrary.sample.value
  private val iossEtmpBankDetails: IossEtmpBankDetails = arbitraryIossEtmpBankDetails.arbitrary.sample.value

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
        
        CompositeAccount.fromIossEtmpBankDetails(iossEtmpBankDetails) `mustBe`expectedResult
      }
    }
  }
}
