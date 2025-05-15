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

package testutils

import base.SpecBase
import models.Country
import models.previousIntermediaryRegistrations.IntermediaryIdentificationNumberValidation

object PreviousINNumberGenerator extends SpecBase {

  private val numberLength: Int = 7

  def generateIntermediaryRegistrationNumber(): String = {

    val prefix = arbitraryIntermediaryNumberPrefix.arbitrary.sample.value
    val number = numStringWithFixedLength(numberLength).sample.value

    s"$prefix$number"
  }

  def getCountryFromIntermediaryRegistrationNumber(intermediaryNumber: String): Country = {
    val prefix: String = intermediaryNumber.substring(0, 5)

    IntermediaryIdentificationNumberValidation.euCountriesWithIntermediaryValidationRules
      .find(_.vrnRegex.contains(prefix)).map(_.country).head
  }
}
