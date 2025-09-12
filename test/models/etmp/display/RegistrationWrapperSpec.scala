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

package models.etmp.display

import base.SpecBase
import models.domain.VatCustomerInfo
import play.api.libs.functional.syntax.*
import play.api.libs.json.*

class RegistrationWrapperSpec extends SpecBase {

  private val vatCustomerInfo: VatCustomerInfo = arbitraryVatCustomerInfo.arbitrary.sample.value
  private val etmpDisplayRegistration: EtmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value

  private val registrationWrapper: RegistrationWrapper = RegistrationWrapper(
    vatInfo = vatCustomerInfo,
    etmpDisplayRegistration = etmpDisplayRegistration
  )

  "RegistrationWrapper" - {

    "must deserialise from RegistrationWrapper" in {

      val json: JsValue = Json.toJson(registrationWrapper)

      val expectedResult = RegistrationWrapper(
        vatInfo = vatCustomerInfo,
        etmpDisplayRegistration = etmpDisplayRegistration
      )

      Json.toJson(expectedResult) `mustBe` json
    }

    "must serialise to RegistrationWrapper" in {

      val registrationWrapperWrites: Writes[RegistrationWrapper] = {
        (
          (__ \ "vatInfo").write[VatCustomerInfo] and
            (__ \ "etmpDisplayRegistration").write[EtmpDisplayRegistration]
          )(registrationWrapper => Tuple.fromProductTyped(registrationWrapper))
      }

      val json: JsValue = Json.toJson(registrationWrapper)(registrationWrapperWrites)

      val expectedResult = RegistrationWrapper(
        vatInfo = vatCustomerInfo,
        etmpDisplayRegistration = etmpDisplayRegistration
      )

      json.validate[RegistrationWrapper] `mustBe` JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[RegistrationWrapper] `mustBe` a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "vatInfo" -> 123456
      )

      json.validate[RegistrationWrapper] `mustBe` a[JsError]
    }
  }
}
