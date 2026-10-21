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

package models.returns

import base.SpecBase
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, Json}

class SubmissionStatusSpec extends SpecBase with ScalaCheckPropertyChecks {

  "SubmissionStatus" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(SubmissionStatus.values)

      forAll(gen) {
        submissionStatus =>

          JsString(submissionStatus.toString)
            .validate[SubmissionStatus].asOpt.value `mustBe` submissionStatus
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String].suchThat(!SubmissionStatus.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValues =>

          JsString(invalidValues).validate[SubmissionStatus] `mustBe` JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(SubmissionStatus.values)

      forAll(gen) {
        submissionStatus =>

          Json.toJson(submissionStatus) `mustBe` JsString(submissionStatus.toString)
      }
    }
  }
}
