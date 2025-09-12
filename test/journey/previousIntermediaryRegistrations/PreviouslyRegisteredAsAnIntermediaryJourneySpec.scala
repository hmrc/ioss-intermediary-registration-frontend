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

package journey.previousIntermediaryRegistrations

import base.SpecBase
import generators.ModelGenerators
import journey.JourneyHelpers
import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetails
import models.{Country, Index}
import org.scalatest.freespec.AnyFreeSpec
import pages.euDetails.HasFixedEstablishmentPage
import pages.previousIntermediaryRegistrations.*
import pages.{CheckYourAnswersPage, previousIntermediaryRegistrations}
import queries.previousIntermediaryRegistrations.{AllPreviousIntermediaryRegistrationsQuery, PreviousIntermediaryRegistrationQuery}

class PreviouslyRegisteredAsAnIntermediaryJourneySpec extends AnyFreeSpec with JourneyHelpers with ModelGenerators with SpecBase {

  private val maxCountries: Int = Country.euCountries.size

  private val previousIntermediaryRegistrationDetails1: PreviousIntermediaryRegistrationDetails =
    arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value

  private val previousIntermediaryRegistrationDetails2: PreviousIntermediaryRegistrationDetails =
    arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value

  private val intermediaryNumber1: String = previousIntermediaryRegistrationDetails1.previousIntermediaryNumber
  private val intermediaryNumber2: String = previousIntermediaryRegistrationDetails2.previousIntermediaryNumber

  private val country1: Country = previousIntermediaryRegistrationDetails1.previousEuCountry
  private val country2: Country = previousIntermediaryRegistrationDetails2.previousEuCountry

  private val initialise = journeyOf(
    setUserAnswerTo(HasPreviouslyRegisteredAsIntermediaryPage, true),
    setUserAnswerTo(PreviousEuCountryPage(countryIndex(0)), country1),
    setUserAnswerTo(PreviousIntermediaryRegistrationNumberPage(countryIndex(0)), intermediaryNumber1),
    setUserAnswerTo(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(0))), true),
    setUserAnswerTo(PreviousEuCountryPage(countryIndex(1)), country2),
    setUserAnswerTo(PreviousIntermediaryRegistrationNumberPage(countryIndex(1)), intermediaryNumber2),
    setUserAnswerTo(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(1))), false),
    goTo(CheckYourAnswersPage)
  )

  "Previously Registered As An Intermediary" - {

    "users who have NOT previously registered as an intermediary for IOSS in an EU country must go to Tax Registered In Eu Page" in {

      startingFrom(HasPreviouslyRegisteredAsIntermediaryPage)
        .run(
          setUserAnswerTo(basicUserAnswersWithVatInfo),
          submitAnswer(HasPreviouslyRegisteredAsIntermediaryPage, false),
          pageMustBe(HasFixedEstablishmentPage)
        )
    }

    s"must be asked for as many as necessary upto the maximum of $maxCountries EU countries" in {

      def generatePreviousIntermediaryRegistrations: Seq[JourneyStep[Unit]] = {
        (0 until maxCountries).foldLeft(Seq.empty[JourneyStep[Unit]]) {
          case (journeySteps: Seq[JourneyStep[Unit]], countryIndex: Int) =>
            journeySteps :+
              submitAnswer(PreviousEuCountryPage(Index(countryIndex)), country1) :+
              submitAnswer(PreviousIntermediaryRegistrationNumberPage(Index(countryIndex)), intermediaryNumber1) :+
              pageMustBe(AddPreviousIntermediaryRegistrationPage(Some(Index(countryIndex)))) :+
              submitAnswer(AddPreviousIntermediaryRegistrationPage(Some(Index(countryIndex))), true)
        }
      }

      startingFrom(HasPreviouslyRegisteredAsIntermediaryPage)
        .run(
          submitAnswer(HasPreviouslyRegisteredAsIntermediaryPage, true) +:
            generatePreviousIntermediaryRegistrations :+
            pageMustBe(HasFixedEstablishmentPage): _*
        )
    }

    "users who have previously registered as an intermediary for IOSS in an EU country" - {

      "must be able to register a country" in {

        startingFrom(HasPreviouslyRegisteredAsIntermediaryPage)
          .run(
            submitAnswer(HasPreviouslyRegisteredAsIntermediaryPage, true),
            submitAnswer(PreviousEuCountryPage(countryIndex(0)), country1),
            submitAnswer(PreviousIntermediaryRegistrationNumberPage(countryIndex(0)), intermediaryNumber1),
            pageMustBe(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(0))))
          )
      }

      "must be able to register multiple countries" in {

        startingFrom(HasPreviouslyRegisteredAsIntermediaryPage)
          .run(
            submitAnswer(HasPreviouslyRegisteredAsIntermediaryPage, true),
            submitAnswer(PreviousEuCountryPage(countryIndex(0)), country1),
            submitAnswer(PreviousIntermediaryRegistrationNumberPage(countryIndex(0)), intermediaryNumber1),
            pageMustBe(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(0)))),
            submitAnswer(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(0))), true),
            submitAnswer(PreviousEuCountryPage(countryIndex(1)), country2),
            submitAnswer(PreviousIntermediaryRegistrationNumberPage(countryIndex(1)), intermediaryNumber2),
            pageMustBe(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(1))))
          )
      }

      "must be able to remove them" - {

        "when there is a single country" in {

          startingFrom(HasPreviouslyRegisteredAsIntermediaryPage)
            .run(
              submitAnswer(HasPreviouslyRegisteredAsIntermediaryPage, true),
              submitAnswer(PreviousEuCountryPage(countryIndex(0)), country1),
              submitAnswer(PreviousIntermediaryRegistrationNumberPage(countryIndex(0)), intermediaryNumber1),
              pageMustBe(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(0)))),
              goTo(DeletePreviousIntermediaryRegistrationPage(countryIndex(0))),
              removeAddToListItem(PreviousIntermediaryRegistrationQuery(countryIndex(0))),
              pageMustBe(HasPreviouslyRegisteredAsIntermediaryPage),
              answersMustNotContain(PreviousIntermediaryRegistrationQuery(countryIndex(0)))
            )
        }

        "when there are multiple countries" in {

          startingFrom(HasPreviouslyRegisteredAsIntermediaryPage)
            .run(
              submitAnswer(HasPreviouslyRegisteredAsIntermediaryPage, true),
              submitAnswer(PreviousEuCountryPage(countryIndex(0)), country1),
              submitAnswer(PreviousIntermediaryRegistrationNumberPage(countryIndex(0)), intermediaryNumber1),
              pageMustBe(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(0)))),
              submitAnswer(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(0))), true),
              submitAnswer(PreviousEuCountryPage(countryIndex(1)), country2),
              submitAnswer(PreviousIntermediaryRegistrationNumberPage(countryIndex(1)), intermediaryNumber2),
              pageMustBe(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(1)))),
              goTo(DeletePreviousIntermediaryRegistrationPage(countryIndex(1))),
              removeAddToListItem(PreviousIntermediaryRegistrationQuery(countryIndex(1))),
              pageMustBe(AddPreviousIntermediaryRegistrationPage()),
              answersMustNotContain(PreviousIntermediaryRegistrationQuery(countryIndex(1)))
            )
        }
      }

      "must be able to change the users original Intermediary Number answer for that country" in {

        val updatedIntermediaryNumber: String = s"${intermediaryNumber1.substring(0, 6)}7654321"

        startingFrom(HasPreviouslyRegisteredAsIntermediaryPage)
          .run(
            submitAnswer(HasPreviouslyRegisteredAsIntermediaryPage, true),
            submitAnswer(PreviousEuCountryPage(countryIndex(0)), country1),
            submitAnswer(PreviousIntermediaryRegistrationNumberPage(countryIndex(0)), intermediaryNumber1),
            pageMustBe(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(0)))),
            submitAnswer(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(0))), true),
            submitAnswer(PreviousEuCountryPage(countryIndex(1)), country2),
            submitAnswer(PreviousIntermediaryRegistrationNumberPage(countryIndex(1)), intermediaryNumber2),
            pageMustBe(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(1)))),
            goToChangeAnswer(PreviousIntermediaryRegistrationNumberPage(countryIndex(0))),
            pageMustBe(PreviousIntermediaryRegistrationNumberPage(countryIndex(0))),
            submitAnswer(PreviousIntermediaryRegistrationNumberPage(countryIndex(0)), updatedIntermediaryNumber),
            pageMustBe(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(1)))),
            answerMustEqual(PreviousIntermediaryRegistrationNumberPage(countryIndex(0)), updatedIntermediaryNumber)
          )
      }

      "must be able to remove all original Previous Intermediary Registrations" - {

        "when the user is on the Check Your Answers page and they change their original answer of having previous Intermediary Registrations " +
          "in other EU countries to No" in {

          startingFrom(CheckYourAnswersPage)
            .run(
              initialise,
              goToChangeAnswer(HasPreviouslyRegisteredAsIntermediaryPage),
              submitAnswer(HasPreviouslyRegisteredAsIntermediaryPage, false),
              pageMustBe(DeleteAllPreviousIntermediaryRegistrationsPage),
              submitAnswer(DeleteAllPreviousIntermediaryRegistrationsPage, true),
              removeAddToListItem(AllPreviousIntermediaryRegistrationsQuery),
              answersMustNotContain(PreviousEuCountryPage(countryIndex(0))),
              answersMustNotContain(PreviousEuCountryPage(countryIndex(1)))
            )
        }
      }

      "must be able to retain all original Previous Intermediary Registrations" - {

        "when the user is on the Check Your Answers page and they change their original answer of having previous Intermediary Registrations " +
          "in other EU countries to No" - {

          "but answer no when asked if they want to remove all Previous Intermediary Registrations" in {

            startingFrom(CheckYourAnswersPage)
              .run(
                initialise,
                goToChangeAnswer(HasPreviouslyRegisteredAsIntermediaryPage),
                submitAnswer(HasPreviouslyRegisteredAsIntermediaryPage, false),
                pageMustBe(DeleteAllPreviousIntermediaryRegistrationsPage),
                submitAnswer(DeleteAllPreviousIntermediaryRegistrationsPage, false),
                pageMustBe(CheckYourAnswersPage),
                answersMustContain(PreviousEuCountryPage(countryIndex(0))),
                answersMustContain(PreviousEuCountryPage(countryIndex(1)))
              )
          }
        }
      }
    }
  }
}
