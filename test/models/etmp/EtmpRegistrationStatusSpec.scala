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
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, Json}

class EtmpRegistrationStatusSpec extends SpecBase with ScalaCheckPropertyChecks {

  "EtmpRegistrationStatus" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(EtmpRegistrationStatus.values)

      forAll(gen) {
        etmpRegistrationStatus =>

          JsString(etmpRegistrationStatus.toString).validate[EtmpRegistrationStatus].asOpt.value mustBe etmpRegistrationStatus
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String].suchThat(!EtmpRegistrationStatus.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValues =>

          JsString(invalidValues).validate[EtmpRegistrationStatus] mustBe JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(EtmpRegistrationStatus.values)

      forAll(gen) {
        etmpRegistrationStatus =>

          Json.toJson(etmpRegistrationStatus) mustBe JsString(etmpRegistrationStatus.toString)
      }
    }
  }
}
