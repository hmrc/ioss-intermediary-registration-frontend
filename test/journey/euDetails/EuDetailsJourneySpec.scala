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

package journey.euDetails

import base.SpecBase
import generators.Generators
import journey.JourneyHelpers
import models.euDetails.RegistrationType.{TaxId, VatNumber}
import models.{Country, Index, InternationalAddress}
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import pages.euDetails.*
import pages.{CheckYourAnswersPage, ContactDetailsPage}
import queries.euDetails.{AllEuDetailsRawQuery, EuDetailsQuery}

class EuDetailsJourneySpec extends SpecBase with JourneyHelpers with Generators {

  private val euVatNumber: String = arbitraryEuVatNumber.sample.value
  private val countryCode: String = euVatNumber.substring(0, 2)
  private val country: Country = Country(countryCode, Country.euCountries.find(_.code == countryCode).head.name)
  private val euTaxId: String = genEuTaxReference.sample.value

  private val maxCountries: Int = Country.euCountries.size

  private val feTradingName1 = genFixedEstablishmentTradingName.sample.value
  private val feTradingName2 = genFixedEstablishmentTradingName.sample.value
  private val feTradingName3 = genFixedEstablishmentTradingName.sample.value
  private val feTradingNames: Seq[String] = Seq(feTradingName1, feTradingName2, feTradingName3)
  private val feAddress: InternationalAddress = arbitraryInternationalAddress.arbitrary.sample.value

  private val initialise = journeyOf(
    setUserAnswerTo(TaxRegisteredInEuPage, true),
    setUserAnswerTo(EuCountryPage(countryIndex(0)), country),
    setUserAnswerTo(HasFixedEstablishmentPage(countryIndex(0)), true),
    setUserAnswerTo(RegistrationTypePage(countryIndex(0)), VatNumber),
    setUserAnswerTo(EuVatNumberPage(countryIndex(0)), euVatNumber),
    setUserAnswerTo(FixedEstablishmentTradingNamePage(countryIndex(0)), feTradingName1),
    setUserAnswerTo(FixedEstablishmentAddressPage(countryIndex(0)), feAddress),
    setUserAnswerTo(AddEuDetailsPage(Some(countryIndex(0))), true),
    setUserAnswerTo(EuCountryPage(countryIndex(1)), country),
    setUserAnswerTo(HasFixedEstablishmentPage(countryIndex(1)), true),
    setUserAnswerTo(RegistrationTypePage(countryIndex(1)), TaxId),
    setUserAnswerTo(EuTaxReferencePage(countryIndex(1)), euTaxId),
    setUserAnswerTo(FixedEstablishmentTradingNamePage(countryIndex(1)), feTradingName1),
    setUserAnswerTo(FixedEstablishmentAddressPage(countryIndex(1)), feAddress),
    setUserAnswerTo(AddEuDetailsPage(Some(countryIndex(1))), false),
    goTo(CheckYourAnswersPage)
  )

  "must go directly to add Business Contact Details page if not registered for VAT in any EU countries" in {
    startingFrom(TaxRegisteredInEuPage)
      .run(
        submitAnswer(TaxRegisteredInEuPage, false),
        pageMustBe(ContactDetailsPage)
      )
  }

  s"must be asked for as many as necessary upto the maximum of $maxCountries EU countries" in {

    def generateEuDetails: Seq[JourneyStep[Unit]] = {
      (0 until maxCountries).foldLeft(Seq.empty[JourneyStep[Unit]]) {
        case (journeySteps: Seq[JourneyStep[Unit]], countryIndex: Int) =>
          journeySteps :+
            submitAnswer(EuCountryPage(Index(countryIndex)), country) :+
            submitAnswer(HasFixedEstablishmentPage(Index(countryIndex)), true) :+
            submitAnswer(RegistrationTypePage(Index(countryIndex)), VatNumber) :+
            submitAnswer(EuVatNumberPage(Index(countryIndex)), euVatNumber) :+
            submitAnswer(FixedEstablishmentTradingNamePage(Index(countryIndex)), Gen.oneOf(feTradingNames).sample.value) :+
            submitAnswer(FixedEstablishmentAddressPage(Index(countryIndex)), feAddress) :+
            pageMustBe(CheckEuDetailsAnswersPage(Index(countryIndex))) :+
            goTo(AddEuDetailsPage(Some(Index(countryIndex)))) :+
            submitAnswer(AddEuDetailsPage(Some(Index(countryIndex))), true)
      }
    }

    startingFrom(TaxRegisteredInEuPage)
      .run(
        submitAnswer(TaxRegisteredInEuPage, true) +:
          generateEuDetails :+
          pageMustBe(ContactDetailsPage): _*
      )
  }

  "users with one or more EU registrations" - {

    "the user can't register a country as they don't have a fixed establishment in that country" - {

      "when the user has only entered one country" in {

        startingFrom(TaxRegisteredInEuPage)
          .run(
            submitAnswer(TaxRegisteredInEuPage, true),
            submitAnswer(EuCountryPage(countryIndex(0)), country),
            submitAnswer(HasFixedEstablishmentPage(countryIndex(0)), false),
            pageMustBe(CannotRegisterNoFixedEstablishmentPage(countryIndex(0))),
            removeAddToListItem(EuDetailsQuery(countryIndex(0))),
            pageMustBe(TaxRegisteredInEuPage),
            answersMustNotContain(EuCountryPage(countryIndex(0)))
          )
      }

      "when the user has entered multiple countries" in {

        val initialise = journeyOf(
          setUserAnswerTo(TaxRegisteredInEuPage, true),
          setUserAnswerTo(EuCountryPage(countryIndex(0)), country),
          setUserAnswerTo(HasFixedEstablishmentPage(countryIndex(0)), true),
          setUserAnswerTo(RegistrationTypePage(countryIndex(0)), VatNumber),
          setUserAnswerTo(EuVatNumberPage(countryIndex(0)), euVatNumber),
          setUserAnswerTo(FixedEstablishmentTradingNamePage(countryIndex(0)), feTradingName1),
          setUserAnswerTo(FixedEstablishmentAddressPage(countryIndex(0)), feAddress),
          setUserAnswerTo(AddEuDetailsPage(Some(countryIndex(0))), true),
          setUserAnswerTo(EuCountryPage(countryIndex(1)), country),
          setUserAnswerTo(HasFixedEstablishmentPage(countryIndex(1)), true),
          setUserAnswerTo(RegistrationTypePage(countryIndex(1)), TaxId),
          setUserAnswerTo(EuTaxReferencePage(countryIndex(1)), euTaxId),
          setUserAnswerTo(FixedEstablishmentTradingNamePage(countryIndex(1)), feTradingName1),
          setUserAnswerTo(FixedEstablishmentAddressPage(countryIndex(1)), feAddress),
          setUserAnswerTo(AddEuDetailsPage(Some(countryIndex(1))), true)
        )

        startingFrom(EuCountryPage(countryIndex(2)))
          .run(
            initialise,
            submitAnswer(EuCountryPage(countryIndex(2)), country),
            submitAnswer(HasFixedEstablishmentPage(countryIndex(2)), false),
            pageMustBe(CannotRegisterNoFixedEstablishmentPage(countryIndex(2))),
            removeAddToListItem(EuDetailsQuery(countryIndex(2))),
            answersMustNotContain(EuDetailsQuery(countryIndex(2))),
            answersMustContain(EuDetailsQuery(countryIndex(0))),
            answersMustContain(EuDetailsQuery(countryIndex(1)))
          )
      }
    }

    "the user registers a country with a VAT number" in {

      startingFrom(TaxRegisteredInEuPage)
        .run(
          submitAnswer(TaxRegisteredInEuPage, true),
          submitAnswer(EuCountryPage(countryIndex(0)), country),
          submitAnswer(HasFixedEstablishmentPage(countryIndex(0)), true),
          submitAnswer(RegistrationTypePage(countryIndex(0)), VatNumber),
          submitAnswer(EuVatNumberPage(countryIndex(0)), euVatNumber),
          submitAnswer(FixedEstablishmentTradingNamePage(countryIndex(0)), feTradingName1),
          submitAnswer(FixedEstablishmentAddressPage(countryIndex(0)), feAddress),
          pageMustBe(CheckEuDetailsAnswersPage(countryIndex(0)))
        )
    }

    "the user registers a country with an EU Tax Reference" in {

      startingFrom(TaxRegisteredInEuPage)
        .run(
          submitAnswer(TaxRegisteredInEuPage, true),
          submitAnswer(EuCountryPage(countryIndex(0)), country),
          submitAnswer(HasFixedEstablishmentPage(countryIndex(0)), true),
          submitAnswer(RegistrationTypePage(countryIndex(0)), TaxId),
          submitAnswer(EuTaxReferencePage(countryIndex(0)), euTaxId),
          submitAnswer(FixedEstablishmentTradingNamePage(countryIndex(0)), feTradingName1),
          submitAnswer(FixedEstablishmentAddressPage(countryIndex(0)), feAddress),
          pageMustBe(CheckEuDetailsAnswersPage(countryIndex(0)))
        )
    }

    "must be able to remove them" - {

      "when there is only one" in {

        startingFrom(TaxRegisteredInEuPage)
          .run(
            submitAnswer(TaxRegisteredInEuPage, true),
            submitAnswer(EuCountryPage(countryIndex(0)), country),
            submitAnswer(HasFixedEstablishmentPage(countryIndex(0)), true),
            submitAnswer(RegistrationTypePage(countryIndex(0)), TaxId),
            submitAnswer(EuTaxReferencePage(countryIndex(0)), euTaxId),
            submitAnswer(FixedEstablishmentTradingNamePage(countryIndex(0)), feTradingName1),
            submitAnswer(FixedEstablishmentAddressPage(countryIndex(0)), feAddress),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex(0))),
            goTo(DeleteEuDetailsPage(Index(0))),
            removeAddToListItem(EuDetailsQuery(Index(0))),
            pageMustBe(TaxRegisteredInEuPage),
            answersMustNotContain(EuDetailsQuery(Index(0)))
          )
      }

      "when there are multiple" in {

        startingFrom(TaxRegisteredInEuPage)
          .run(
            submitAnswer(TaxRegisteredInEuPage, true),
            submitAnswer(EuCountryPage(countryIndex(0)), country),
            submitAnswer(HasFixedEstablishmentPage(countryIndex(0)), true),
            submitAnswer(RegistrationTypePage(countryIndex(0)), VatNumber),
            submitAnswer(EuVatNumberPage(countryIndex(0)), euVatNumber),
            submitAnswer(FixedEstablishmentTradingNamePage(countryIndex(0)), feTradingName1),
            submitAnswer(FixedEstablishmentAddressPage(countryIndex(0)), feAddress),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex(0))),
            goTo(AddEuDetailsPage(Some(countryIndex(0)))),
            submitAnswer(AddEuDetailsPage(Some(countryIndex(0))), true),
            submitAnswer(EuCountryPage(countryIndex(1)), country),
            submitAnswer(HasFixedEstablishmentPage(countryIndex(1)), true),
            submitAnswer(RegistrationTypePage(countryIndex(1)), TaxId),
            submitAnswer(EuTaxReferencePage(countryIndex(1)), euTaxId),
            submitAnswer(FixedEstablishmentTradingNamePage(countryIndex(1)), feTradingName1),
            submitAnswer(FixedEstablishmentAddressPage(countryIndex(1)), feAddress),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex(1))),
            goTo(DeleteEuDetailsPage(countryIndex(0))),
            removeAddToListItem(EuDetailsQuery(countryIndex(0))),
            pageMustBe(AddEuDetailsPage()),
            answersMustNotContain(EuDetailsQuery(countryIndex(1)))
          )
      }
    }

    "must be able to change the users original answer" - {

      "when there is only one" in {

        val initialise = journeyOf(
          submitAnswer(TaxRegisteredInEuPage, true),
          submitAnswer(EuCountryPage(countryIndex(0)), country),
          submitAnswer(HasFixedEstablishmentPage(countryIndex(0)), true),
          submitAnswer(RegistrationTypePage(countryIndex(0)), TaxId),
          submitAnswer(EuTaxReferencePage(countryIndex(0)), euTaxId),
          submitAnswer(FixedEstablishmentTradingNamePage(countryIndex(0)), feTradingName1),
          submitAnswer(FixedEstablishmentAddressPage(countryIndex(0)), feAddress),
          pageMustBe(CheckEuDetailsAnswersPage(countryIndex(0))),
          goTo(AddEuDetailsPage(Some(countryIndex(0)))),
          submitAnswer(AddEuDetailsPage(Some(countryIndex(0))), false),
          goTo(AddEuDetailsPage())
        )

        startingFrom(TaxRegisteredInEuPage)
          .run(
            initialise,
            goTo(CheckEuDetailsAnswersPage(countryIndex(0))),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex(0))),
            goToChangeAnswer(RegistrationTypePage(countryIndex(0))),
            pageMustBe(RegistrationTypePage(countryIndex(0))),
            submitAnswer(RegistrationTypePage(countryIndex(0)), VatNumber),
            pageMustBe(EuVatNumberPage(countryIndex(0))),
            submitAnswer(EuVatNumberPage(countryIndex(0)), euVatNumber),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex(0))),
            goTo(AddEuDetailsPage(Some(countryIndex(0)))),
            answerMustEqual(RegistrationTypePage(countryIndex(0)), VatNumber),
            answerMustEqual(EuVatNumberPage(countryIndex(0)), euVatNumber),
            answersMustNotContain(EuTaxReferencePage(countryIndex(0)))
          )
      }

      "when there are multiple changes required" in {

        val initialise = journeyOf(
          submitAnswer(TaxRegisteredInEuPage, true),
          submitAnswer(EuCountryPage(countryIndex(0)), country),
          submitAnswer(HasFixedEstablishmentPage(countryIndex(0)), true),
          submitAnswer(RegistrationTypePage(countryIndex(0)), VatNumber),
          submitAnswer(EuVatNumberPage(countryIndex(0)), euVatNumber),
          submitAnswer(FixedEstablishmentTradingNamePage(countryIndex(0)), feTradingName1),
          submitAnswer(FixedEstablishmentAddressPage(countryIndex(0)), feAddress),
          pageMustBe(CheckEuDetailsAnswersPage(countryIndex(0))),
          goTo(AddEuDetailsPage(Some(countryIndex(0)))),
          submitAnswer(AddEuDetailsPage(Some(countryIndex(0))), true),
          submitAnswer(EuCountryPage(countryIndex(1)), country),
          submitAnswer(HasFixedEstablishmentPage(countryIndex(1)), true),
          submitAnswer(RegistrationTypePage(countryIndex(1)), TaxId),
          submitAnswer(EuTaxReferencePage(countryIndex(1)), euTaxId),
          submitAnswer(FixedEstablishmentTradingNamePage(countryIndex(1)), feTradingName1),
          submitAnswer(FixedEstablishmentAddressPage(countryIndex(1)), feAddress),
          pageMustBe(CheckEuDetailsAnswersPage(countryIndex(1))),
          goTo(AddEuDetailsPage(Some(countryIndex(1)))),
          submitAnswer(AddEuDetailsPage(Some(countryIndex(1))), false),
          goTo(AddEuDetailsPage())
        )

        startingFrom(TaxRegisteredInEuPage)
          .run(
            initialise,
            goTo(CheckEuDetailsAnswersPage(countryIndex(1))),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex(1))),
            goToChangeAnswer(FixedEstablishmentTradingNamePage(countryIndex(1))),
            pageMustBe(FixedEstablishmentTradingNamePage(countryIndex(1))),
            submitAnswer(FixedEstablishmentTradingNamePage(countryIndex(1)), feTradingName2),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex(1))),
            goTo(AddEuDetailsPage(Some(countryIndex(1)))),
            answerMustEqual(FixedEstablishmentTradingNamePage(countryIndex(1)), feTradingName2)
          )
      }
    }

    "must be able to remove all original EU registrations answers" - {

      "when the user is on the Check Your Answers page and they change their original answer of being registered for tax in other EU countries to No" in {

        startingFrom(CheckYourAnswersPage)
          .run(
            initialise,
            goToChangeAnswer(TaxRegisteredInEuPage),
            submitAnswer(TaxRegisteredInEuPage, false),
            pageMustBe(DeleteAllEuDetailsPage),
            submitAnswer(DeleteAllEuDetailsPage, true),
            removeAddToListItem(AllEuDetailsRawQuery),
            answersMustNotContain(EuCountryPage(countryIndex(0))),
            answersMustNotContain(EuCountryPage(countryIndex(1)))
          )
      }
    }

    "must be able to retain all original EU registrations answers" - {

      "when the user is on the Check Your Answers page and they change their original answer of being registered for tax in other EU countries to No" - {

        "but answer no when asked if they want to remove all EU registrations from the scheme" in {

          startingFrom(CheckYourAnswersPage)
            .run(
              initialise,
              goToChangeAnswer(TaxRegisteredInEuPage),
              submitAnswer(TaxRegisteredInEuPage, false),
              pageMustBe(DeleteAllEuDetailsPage),
              submitAnswer(DeleteAllEuDetailsPage, false),
              pageMustBe(CheckYourAnswersPage),
              answersMustContain(EuCountryPage(countryIndex(0))),
              answersMustContain(EuCountryPage(countryIndex(1)))
            )
        }
      }
    }
  }
}
