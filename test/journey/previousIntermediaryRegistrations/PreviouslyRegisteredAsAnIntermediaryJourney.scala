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
import pages.JourneyRecoveryPage
import pages.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediaryPage, PreviousEuCountryPage}

class PreviouslyRegisteredAsAnIntermediaryJourney extends AnyFreeSpec with JourneyHelpers with ModelGenerators with SpecBase {

  private val countryIndex: Index = Index(0)
  
  "Previously Registered As An Intermediary" - {

    "users who have NOT previously registered as an intermediary for IOSS in an EU country must go to Tax Registered In Eu Page" in {

      startingFrom(HasPreviouslyRegisteredAsIntermediaryPage)
        .run(
          setUserAnswerTo(basicUserAnswersWithVatInfo),
          submitAnswer(HasPreviouslyRegisteredAsIntermediaryPage, false),
          pageMustBe(JourneyRecoveryPage) // TODO -> to TaxRegisteredInEuPage
        )
    }

    "users who have previously registered as an intermediary for IOSS in an EU country must go to Previous EU Country Page" in {

      startingFrom(HasPreviouslyRegisteredAsIntermediaryPage)
        .run(
          setUserAnswerTo(basicUserAnswersWithVatInfo),
          submitAnswer(HasPreviouslyRegisteredAsIntermediaryPage, true),
          pageMustBe(PreviousEuCountryPage(countryIndex))
        )
    }

    "users who have previously registered as an intermediary for IOSS in an EU country must select a country and  proceed to the next page" in { // TODO

      startingFrom(HasPreviouslyRegisteredAsIntermediaryPage)
        .run(
          submitAnswer(HasPreviouslyRegisteredAsIntermediaryPage, true),
          submitAnswer(PreviousEuCountryPage(countryIndex), arbitraryCountry.arbitrary.sample.value),
          pageMustBe(JourneyRecoveryPage) // TODO
        )
    }
  }
}
