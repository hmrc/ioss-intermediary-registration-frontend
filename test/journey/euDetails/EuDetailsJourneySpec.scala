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

import generators.Generators
import journey.JourneyHelpers
import models.euDetails.RegistrationType.{TaxId, VatNumber}
import models.{Country, Index, InternationalAddress}
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import pages.euDetails.*
import pages.{CheckYourAnswersPage, ContactDetailsPage}
import queries.euDetails.{AllEuDetailsRawQuery, EuDetailsQuery}

class EuDetailsJourneySpec extends AnyFreeSpec with JourneyHelpers with Generators {

  private val euVatNumber: String = arbitraryEuVatNumber.sample.value
  private val countryCode: String = euVatNumber.substring(0, 2)
  private val country: Country = Country(countryCode, Country.euCountries.find(_.code == countryCode).head.name)
  private val euTaxId: String = arbitraryEuTaxReference.sample.value

  private val maxCountries: Int = Country.euCountries.size
  private val countryIndex1: Index = Index(0)
  private val countryIndex2: Index = Index(1)
  private val countryIndex3: Index = Index(2)

  private val feTradingName1 = arbitraryFixedEstablishmentTradingName.sample.value
  private val feTradingName2 = arbitraryFixedEstablishmentTradingName.sample.value
  private val feTradingName3 = arbitraryFixedEstablishmentTradingName.sample.value
  private val feTradingNames: Seq[String] = Seq(feTradingName1, feTradingName2, feTradingName3)
  private val feAddress: InternationalAddress = arbitraryInternationalAddress.arbitrary.sample.value

  private val initialise = journeyOf(
    setUserAnswerTo(TaxRegisteredInEuPage, true),
    setUserAnswerTo(EuCountryPage(countryIndex1), country),
    setUserAnswerTo(HasFixedEstablishmentPage(countryIndex1), true),
    setUserAnswerTo(RegistrationTypePage(countryIndex1), VatNumber),
    setUserAnswerTo(EuVatNumberPage(countryIndex1), euVatNumber),
    setUserAnswerTo(FixedEstablishmentTradingNamePage(countryIndex1), feTradingName1),
    setUserAnswerTo(FixedEstablishmentAddressPage(countryIndex1), feAddress),
    setUserAnswerTo(AddEuDetailsPage(Some(countryIndex1)), true),
    setUserAnswerTo(EuCountryPage(countryIndex2), country),
    setUserAnswerTo(HasFixedEstablishmentPage(countryIndex2), true),
    setUserAnswerTo(RegistrationTypePage(countryIndex2), TaxId),
    setUserAnswerTo(EuTaxReferencePage(countryIndex2), euTaxId),
    setUserAnswerTo(FixedEstablishmentTradingNamePage(countryIndex2), feTradingName1),
    setUserAnswerTo(FixedEstablishmentAddressPage(countryIndex2), feAddress),
    setUserAnswerTo(AddEuDetailsPage(Some(countryIndex2)), false),
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
        case (journeySteps: Seq[JourneyStep[Unit]], index: Int) =>
          journeySteps :+
            submitAnswer(EuCountryPage(Index(index)), country) :+
            submitAnswer(HasFixedEstablishmentPage(Index(index)), true) :+
            submitAnswer(RegistrationTypePage(Index(index)), VatNumber) :+
            submitAnswer(EuVatNumberPage(Index(index)), euVatNumber) :+
            submitAnswer(FixedEstablishmentTradingNamePage(Index(index)), Gen.oneOf(feTradingNames).sample.value) :+
            submitAnswer(FixedEstablishmentAddressPage(Index(index)), feAddress) :+
            pageMustBe(CheckEuDetailsAnswersPage(Index(index))) :+
            goTo(AddEuDetailsPage(Some(Index(index)))) :+
            submitAnswer(AddEuDetailsPage(Some(Index(index))), true)
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
            submitAnswer(EuCountryPage(countryIndex1), country),
            submitAnswer(HasFixedEstablishmentPage(countryIndex1), false),
            pageMustBe(CannotRegisterNoFixedEstablishmentPage(countryIndex1)),
            removeAddToListItem(EuDetailsQuery(countryIndex1)),
            pageMustBe(TaxRegisteredInEuPage),
            answersMustNotContain(EuCountryPage(countryIndex1))
          )
      }

      "when the user has entered multiple countries" in {

        val initialise = journeyOf(
          setUserAnswerTo(TaxRegisteredInEuPage, true),
          setUserAnswerTo(EuCountryPage(countryIndex1), country),
          setUserAnswerTo(HasFixedEstablishmentPage(countryIndex1), true),
          setUserAnswerTo(RegistrationTypePage(countryIndex1), VatNumber),
          setUserAnswerTo(EuVatNumberPage(countryIndex1), euVatNumber),
          setUserAnswerTo(FixedEstablishmentTradingNamePage(countryIndex1), feTradingName1),
          setUserAnswerTo(FixedEstablishmentAddressPage(countryIndex1), feAddress),
          setUserAnswerTo(AddEuDetailsPage(Some(countryIndex1)), true),
          setUserAnswerTo(EuCountryPage(countryIndex2), country),
          setUserAnswerTo(HasFixedEstablishmentPage(countryIndex2), true),
          setUserAnswerTo(RegistrationTypePage(countryIndex2), TaxId),
          setUserAnswerTo(EuTaxReferencePage(countryIndex2), euTaxId),
          setUserAnswerTo(FixedEstablishmentTradingNamePage(countryIndex2), feTradingName1),
          setUserAnswerTo(FixedEstablishmentAddressPage(countryIndex2), feAddress),
          setUserAnswerTo(AddEuDetailsPage(Some(countryIndex2)), true)
        )

        startingFrom(EuCountryPage(countryIndex3))
          .run(
            initialise,
            submitAnswer(EuCountryPage(countryIndex3), country),
            submitAnswer(HasFixedEstablishmentPage(countryIndex3), false),
            pageMustBe(CannotRegisterNoFixedEstablishmentPage(countryIndex3)),
            removeAddToListItem(EuDetailsQuery(countryIndex3)),
            answersMustNotContain(EuDetailsQuery(countryIndex3)),
            answersMustContain(EuDetailsQuery(countryIndex1)),
            answersMustContain(EuDetailsQuery(countryIndex2))
          )
      }
    }

    "the user registers a country with a VAT number" in {

      startingFrom(TaxRegisteredInEuPage)
        .run(
          submitAnswer(TaxRegisteredInEuPage, true),
          submitAnswer(EuCountryPage(countryIndex1), country),
          submitAnswer(HasFixedEstablishmentPage(countryIndex1), true),
          submitAnswer(RegistrationTypePage(countryIndex1), VatNumber),
          submitAnswer(EuVatNumberPage(countryIndex1), euVatNumber),
          submitAnswer(FixedEstablishmentTradingNamePage(countryIndex1), feTradingName1),
          submitAnswer(FixedEstablishmentAddressPage(countryIndex1), feAddress),
          pageMustBe(CheckEuDetailsAnswersPage(countryIndex1))
        )
    }

    "the user registers a country with an EU Tax Reference" in {

      startingFrom(TaxRegisteredInEuPage)
        .run(
          submitAnswer(TaxRegisteredInEuPage, true),
          submitAnswer(EuCountryPage(countryIndex1), country),
          submitAnswer(HasFixedEstablishmentPage(countryIndex1), true),
          submitAnswer(RegistrationTypePage(countryIndex1), TaxId),
          submitAnswer(EuTaxReferencePage(countryIndex1), euTaxId),
          submitAnswer(FixedEstablishmentTradingNamePage(countryIndex1), feTradingName1),
          submitAnswer(FixedEstablishmentAddressPage(countryIndex1), feAddress),
          pageMustBe(CheckEuDetailsAnswersPage(countryIndex1))
        )
    }

    "must be able to remove them" - {

      "when there is only one" in {

        startingFrom(TaxRegisteredInEuPage)
          .run(
            submitAnswer(TaxRegisteredInEuPage, true),
            submitAnswer(EuCountryPage(countryIndex1), country),
            submitAnswer(HasFixedEstablishmentPage(countryIndex1), true),
            submitAnswer(RegistrationTypePage(countryIndex1), TaxId),
            submitAnswer(EuTaxReferencePage(countryIndex1), euTaxId),
            submitAnswer(FixedEstablishmentTradingNamePage(countryIndex1), feTradingName1),
            submitAnswer(FixedEstablishmentAddressPage(countryIndex1), feAddress),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex1)),
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
            submitAnswer(EuCountryPage(countryIndex1), country),
            submitAnswer(HasFixedEstablishmentPage(countryIndex1), true),
            submitAnswer(RegistrationTypePage(countryIndex1), VatNumber),
            submitAnswer(EuVatNumberPage(countryIndex1), euVatNumber),
            submitAnswer(FixedEstablishmentTradingNamePage(countryIndex1), feTradingName1),
            submitAnswer(FixedEstablishmentAddressPage(countryIndex1), feAddress),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex1)),
            goTo(AddEuDetailsPage(Some(countryIndex1))),
            submitAnswer(AddEuDetailsPage(Some(countryIndex1)), true),
            submitAnswer(EuCountryPage(countryIndex2), country),
            submitAnswer(HasFixedEstablishmentPage(countryIndex2), true),
            submitAnswer(RegistrationTypePage(countryIndex2), TaxId),
            submitAnswer(EuTaxReferencePage(countryIndex2), euTaxId),
            submitAnswer(FixedEstablishmentTradingNamePage(countryIndex2), feTradingName1),
            submitAnswer(FixedEstablishmentAddressPage(countryIndex2), feAddress),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex2)),
            goTo(DeleteEuDetailsPage(countryIndex1)),
            removeAddToListItem(EuDetailsQuery(countryIndex1)),
            pageMustBe(AddEuDetailsPage()),
            answersMustNotContain(EuDetailsQuery(countryIndex2))
          )
      }
    }

    "must be able to change the users original answer" - {

      "when there is only one" in {

        val initialise = journeyOf(
          submitAnswer(TaxRegisteredInEuPage, true),
          submitAnswer(EuCountryPage(countryIndex1), country),
          submitAnswer(HasFixedEstablishmentPage(countryIndex1), true),
          submitAnswer(RegistrationTypePage(countryIndex1), TaxId),
          submitAnswer(EuTaxReferencePage(countryIndex1), euTaxId),
          submitAnswer(FixedEstablishmentTradingNamePage(countryIndex1), feTradingName1),
          submitAnswer(FixedEstablishmentAddressPage(countryIndex1), feAddress),
          pageMustBe(CheckEuDetailsAnswersPage(countryIndex1)),
          goTo(AddEuDetailsPage(Some(countryIndex1))),
          submitAnswer(AddEuDetailsPage(Some(countryIndex1)), false),
          goTo(AddEuDetailsPage())
        )

        startingFrom(TaxRegisteredInEuPage)
          .run(
            initialise,
            goTo(CheckEuDetailsAnswersPage(countryIndex1)),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex1)),
            goToChangeAnswer(RegistrationTypePage(countryIndex1)),
            pageMustBe(RegistrationTypePage(countryIndex1)),
            submitAnswer(RegistrationTypePage(countryIndex1), VatNumber),
            pageMustBe(EuVatNumberPage(countryIndex1)),
            submitAnswer(EuVatNumberPage(countryIndex1), euVatNumber),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex1)),
            goTo(AddEuDetailsPage(Some(countryIndex1))),
            answerMustEqual(RegistrationTypePage(countryIndex1), VatNumber),
            answerMustEqual(EuVatNumberPage(countryIndex1), euVatNumber),
            answersMustNotContain(EuTaxReferencePage(countryIndex1))
          )
      }

      "when there are multiple changes required" in {

        val initialise = journeyOf(
          submitAnswer(TaxRegisteredInEuPage, true),
          submitAnswer(EuCountryPage(countryIndex1), country),
          submitAnswer(HasFixedEstablishmentPage(countryIndex1), true),
          submitAnswer(RegistrationTypePage(countryIndex1), VatNumber),
          submitAnswer(EuVatNumberPage(countryIndex1), euVatNumber),
          submitAnswer(FixedEstablishmentTradingNamePage(countryIndex1), feTradingName1),
          submitAnswer(FixedEstablishmentAddressPage(countryIndex1), feAddress),
          pageMustBe(CheckEuDetailsAnswersPage(countryIndex1)),
          goTo(AddEuDetailsPage(Some(countryIndex1))),
          submitAnswer(AddEuDetailsPage(Some(countryIndex1)), true),
          submitAnswer(EuCountryPage(countryIndex2), country),
          submitAnswer(HasFixedEstablishmentPage(countryIndex2), true),
          submitAnswer(RegistrationTypePage(countryIndex2), TaxId),
          submitAnswer(EuTaxReferencePage(countryIndex2), euTaxId),
          submitAnswer(FixedEstablishmentTradingNamePage(countryIndex2), feTradingName1),
          submitAnswer(FixedEstablishmentAddressPage(countryIndex2), feAddress),
          pageMustBe(CheckEuDetailsAnswersPage(countryIndex2)),
          goTo(AddEuDetailsPage(Some(countryIndex2))),
          submitAnswer(AddEuDetailsPage(Some(countryIndex2)), false),
          goTo(AddEuDetailsPage())
        )

        startingFrom(TaxRegisteredInEuPage)
          .run(
            initialise,
            goTo(CheckEuDetailsAnswersPage(countryIndex2)),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex2)),
            goToChangeAnswer(FixedEstablishmentTradingNamePage(countryIndex2)),
            pageMustBe(FixedEstablishmentTradingNamePage(countryIndex2)),
            submitAnswer(FixedEstablishmentTradingNamePage(countryIndex2), feTradingName2),
            pageMustBe(CheckEuDetailsAnswersPage(countryIndex2)),
            goTo(AddEuDetailsPage(Some(countryIndex2))),
            answerMustEqual(FixedEstablishmentTradingNamePage(countryIndex2), feTradingName2)
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
            answersMustNotContain(EuCountryPage(countryIndex1)),
            answersMustNotContain(EuCountryPage(countryIndex2))
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
              answersMustContain(EuCountryPage(countryIndex1)),
              answersMustContain(EuCountryPage(countryIndex2))
            )
        }
      }
    }
  }
}
