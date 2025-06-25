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

package utils

import base.SpecBase
import models.domain.VatCustomerInfo

class CheckNiBasedSpec extends SpecBase {

  private val checkNiBase: CheckNiBased.type = CheckNiBased
  private val vatInfo: VatCustomerInfo = arbitraryVatCustomerInfo.arbitrary.sample.value

  "CheckNiBased" - {

    ".isNiBasedIntermediary" - {

      "must return false when the given postcode area does not match 'BT'" in {
        val nonNiVatInfo: VatCustomerInfo = vatInfo
          .copy(desAddress = vatInfo.desAddress
            .copy(postCode = Some("AA12 1BB")))

        val result = checkNiBase.isNiBasedIntermediary(nonNiVatInfo)

        result `mustBe` false
      }

      "must return true when the given postcode area does match 'BT'" in {
        val nonNiVatInfo: VatCustomerInfo = vatInfo
          .copy(desAddress = vatInfo.desAddress
            .copy(postCode = Some("BT12 1BB")))

        val result = checkNiBase.isNiBasedIntermediary(nonNiVatInfo)

        result `mustBe` true
      }
    }
  }
}
