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

package models.euDetails

import base.SpecBase
import play.api.libs.json.*

class EuDetailsSpec extends SpecBase {
  
  private val euDetails: EuDetails = arbitraryEuDetails.arbitrary.sample.value

  // TODO -> Complete
  "EuDetails" - {
    
    "must serialise/deserialise to and from EuDetails" in {
      
      val json = Json.obj(
        "euCountry" -> euDetails.euCountry
      )
      
      val expectedResult = EuDetails(
        euCountry = euDetails.euCountry
      )
      
      Json.toJson(expectedResult) mustBe json
      json.validate[EuDetails] mustBe JsSuccess(expectedResult)
    }
  }
}
