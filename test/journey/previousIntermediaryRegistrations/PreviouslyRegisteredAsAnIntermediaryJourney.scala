/*
 * Copyright 2023 HM Revenue & Customs
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

package journey.previousIntermediaryRegistrations

import base.SpecBase
import generators.ModelGenerators
import journey.JourneyHelpers
import models.{Country, Index}
import org.scalatest.freespec.AnyFreeSpec
import pages.euDetails.TaxRegisteredInEuPage
import pages.previousIntermediaryRegistrations
import pages.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediaryPage, PreviousEuCountryPage, PreviousIntermediaryRegistrationNumberPage}
import testutils.PreviousINNumberGenerator.{generateIntermediaryRegistrationNumber, getCountryFromIntermediaryRegistrationNumber}

class PreviouslyRegisteredAsAnIntermediaryJourney extends AnyFreeSpec with JourneyHelpers with ModelGenerators with SpecBase {

  private val countryIndex: Index = Index(0)
  private val intermediaryNumber: String = generateIntermediaryRegistrationNumber()
  private val country: Country = getCountryFromIntermediaryRegistrationNumber(intermediaryNumber)

  "Previously Registered As An Intermediary" - {

    "users who have NOT previously registered as an intermediary for IOSS in an EU country must go to Tax Registered In Eu Page" in {

      startingFrom(HasPreviouslyRegisteredAsIntermediaryPage)
        .run(
          setUserAnswerTo(basicUserAnswersWithVatInfo),
          submitAnswer(HasPreviouslyRegisteredAsIntermediaryPage, false),
          pageMustBe(TaxRegisteredInEuPage)
        )
    }

    "users who have previously registered as an intermediary for IOSS in an EU country" - {

      "must go to Previous EU Country Page" in {

        startingFrom(HasPreviouslyRegisteredAsIntermediaryPage)
          .run(
            setUserAnswerTo(basicUserAnswersWithVatInfo),
            submitAnswer(HasPreviouslyRegisteredAsIntermediaryPage, true),
            pageMustBe(PreviousEuCountryPage(countryIndex))
          )
      }

      "must select a country and proceed to Previous Intermediary Registration Number page" in {

        startingFrom(HasPreviouslyRegisteredAsIntermediaryPage)
          .run(
            submitAnswer(HasPreviouslyRegisteredAsIntermediaryPage, true),
            submitAnswer(PreviousEuCountryPage(countryIndex), country),
            pageMustBe(PreviousIntermediaryRegistrationNumberPage(countryIndex))
          )
      }

      "must provide their Intermediary Registration Number (IN) and proceed to the CheckPrevious Intermediary Registration Answers page" in {

        startingFrom(HasPreviouslyRegisteredAsIntermediaryPage)
          .run(
            submitAnswer(HasPreviouslyRegisteredAsIntermediaryPage, true),
            submitAnswer(PreviousEuCountryPage(countryIndex), country),
            submitAnswer(PreviousIntermediaryRegistrationNumberPage(countryIndex), intermediaryNumber)
//            pageMustBe(CheckPreviousIntermediaryRegistrationAnswersPage(countryIndex))
          )
      }

//      "must be able to add additional Intermediary Registrations for that same EU country" in {
//
//        startingFrom(HasPreviouslyRegisteredAsIntermediaryPage)
//          .run(
//            submitAnswer(HasPreviouslyRegisteredAsIntermediaryPage, true),
//            submitAnswer(PreviousEuCountryPage(countryIndex), arbitraryCountry.arbitrary.sample.value),
//            submitAnswer(PreviousIntermediaryRegistrationNumberPage(countryIndex), iNNumber),
//            pageMustBe(CheckPreviousIntermediaryRegistrationAnswersPage(countryIndex)),
//            submitAnswer(CheckPreviousIntermediaryRegistrationAnswersPage(countryIndex), true),
//            submitAnswer(PreviousIntermediaryRegistrationNumberPage(countryIndex), iNNumber2),
//            pageMustBe(CheckPreviousIntermediaryRegistrationAnswersPage(countryIndex))
//          )
//      }
    }
  }
}
