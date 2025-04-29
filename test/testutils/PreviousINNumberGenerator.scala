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

import models.previousIntermediaryRegistrations.IntermediaryIdentificationNumberValidation
import org.scalacheck.Gen

object PreviousINNumberGenerator {

  private val min7Digit: Int = 1000000
  private val max7Digit: Int = 9999999

  def genInNumber(countryCode: String): String = {

    val regex = IntermediaryIdentificationNumberValidation.euCountriesWithIntermediaryValidationRules
      .find(_.country.code == countryCode).map(_.vrnRegex).head.substring(1, 6)

    val exception: Exception = new Exception("Couldn't generate a random 7 digit number.")
    val random7Digit = Gen.choose(min7Digit, max7Digit).sample.getOrElse(throw exception)
    s"$regex$random7Digit"
  }
}
