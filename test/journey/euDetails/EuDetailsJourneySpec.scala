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
import models.{Country, Index}
import org.scalatest.freespec.AnyFreeSpec
import pages.JourneyRecoveryPage
import pages.euDetails.*
import queries.euDetails.EuDetailsQuery

class EuDetailsJourneySpec extends AnyFreeSpec with JourneyHelpers with Generators {

  private val euVatNumber: String = arbitraryEuVatNumber.sample.value
  private val countryCode: String = euVatNumber.substring(0, 2)
  private val country: Country = Country(countryCode, Country.euCountries.find(_.code == countryCode).head.name)
  private val taxId: String = arbitraryEuTaxReference.sample.value

  private val countryIndex: Index = Index(0)

  "EU Details" - {

    "users who do not have their business registered for tax in EU countries" - {

      "must go directly to the Contact Details Page" in {

        startingFrom(TaxRegisteredInEuPage)
          .run(
            submitAnswer(TaxRegisteredInEuPage, false),
            pageMustBe(JourneyRecoveryPage) // TODO -> to BusinessContactDetailsPage when implemented
          )
      }
    }

    // TODO -> Replace with full journey permutations when created
    "users who do have their business registered for tax in EU countries" - {

      "must proceed to the EU Country page" in {

        startingFrom(TaxRegisteredInEuPage)
          .run(
            submitAnswer(TaxRegisteredInEuPage, true),
            pageMustBe(EuCountryPage(countryIndex))
          )
      }

      "must select an EU country and proceed to the Has Fixed Establishment page" in {

        startingFrom(TaxRegisteredInEuPage)
          .run(
            submitAnswer(TaxRegisteredInEuPage, true),
            submitAnswer(EuCountryPage(countryIndex), country),
            pageMustBe(HasFixedEstablishmentPage(countryIndex))
          )
      }

      "the user can't register a country as they don't have a fixed establishment in that country" - {

        "must proceed to the Cannot Register No Fixed Establishment page" in {

          startingFrom(TaxRegisteredInEuPage)
            .run(
              submitAnswer(TaxRegisteredInEuPage, true),
              submitAnswer(EuCountryPage(countryIndex), country),
              submitAnswer(HasFixedEstablishmentPage(countryIndex), false),
              pageMustBe(CannotRegisterNoFixedEstablishmentPage(countryIndex))
            )
        }

        "must remove the EU Details answers and go to the Tax Registered in EU page when the user has only entered one country" in {

          startingFrom(TaxRegisteredInEuPage)
            .run(
              submitAnswer(TaxRegisteredInEuPage, true),
              submitAnswer(EuCountryPage(countryIndex), country),
              submitAnswer(HasFixedEstablishmentPage(countryIndex), false),
              pageMustBe(CannotRegisterNoFixedEstablishmentPage(countryIndex)),
              removeAddToListItem(EuDetailsQuery((countryIndex))),
              pageMustBe(TaxRegisteredInEuPage),
              answersMustNotContain(EuCountryPage(countryIndex))
            )
        }

        "must remove the EU Details answers and go to the Tax Registered in EU page when the user has only entered multiple countries" in {

          // TODO -> Awaiting rest of journey implementation
          startingFrom(TaxRegisteredInEuPage)
            .run(
              submitAnswer(TaxRegisteredInEuPage, true),
              submitAnswer(EuCountryPage(countryIndex), country),
              submitAnswer(HasFixedEstablishmentPage(countryIndex), false),
              pageMustBe(CannotRegisterNoFixedEstablishmentPage(countryIndex)),
              removeAddToListItem(EuDetailsQuery((countryIndex))),
              pageMustBe(TaxRegisteredInEuPage),
              answersMustNotContain(EuCountryPage(countryIndex))
            )
        }
      }

      "the user has a fixed establishment in their chosen country" - {

        "must proceed to the EU Registration Type page" in {

          startingFrom(TaxRegisteredInEuPage)
            .run(
              submitAnswer(TaxRegisteredInEuPage, true),
              submitAnswer(EuCountryPage(countryIndex), country),
              submitAnswer(HasFixedEstablishmentPage(countryIndex), true),
              pageMustBe(RegistrationTypePage(countryIndex))
            )
        }

        "when the user selects VAT number" - {

          "must proceed to the EU VAT Number page" in {

            startingFrom(TaxRegisteredInEuPage)
              .run(
                submitAnswer(TaxRegisteredInEuPage, true),
                submitAnswer(EuCountryPage(countryIndex), country),
                submitAnswer(HasFixedEstablishmentPage(countryIndex), true),
                submitAnswer(RegistrationTypePage(countryIndex), VatNumber),
                pageMustBe(EuVatNumberPage(countryIndex))
              )
          }

          "must proceed to the EU VAT Registered Trading Name page when the user enters a valid VAT number" in {

            startingFrom(TaxRegisteredInEuPage)
              .run(
                submitAnswer(TaxRegisteredInEuPage, true),
                submitAnswer(EuCountryPage(countryIndex), country),
                submitAnswer(HasFixedEstablishmentPage(countryIndex), true),
                submitAnswer(RegistrationTypePage(countryIndex), VatNumber),
                submitAnswer(EuVatNumberPage(countryIndex), euVatNumber),
                pageMustBe(FixedEstablishmentTradingNamePage(countryIndex))
              )
          }
        }

        "when the user selects TAX Id" - {

          "must proceed to Eu Tax Reference page when the user selects TaxId registration type" in {

            startingFrom(TaxRegisteredInEuPage)
              .run(
                submitAnswer(TaxRegisteredInEuPage, true),
                submitAnswer(EuCountryPage(countryIndex), country),
                submitAnswer(HasFixedEstablishmentPage(countryIndex), true),
                submitAnswer(RegistrationTypePage(countryIndex), TaxId),
                pageMustBe(EuTaxReferencePage(countryIndex))
              )
          }

          "must proceed to the EU VAT Registered Trading Name page when the user enters a valid TAX Id" in {

            startingFrom(TaxRegisteredInEuPage)
              .run(
                submitAnswer(TaxRegisteredInEuPage, true),
                submitAnswer(EuCountryPage(countryIndex), country),
                submitAnswer(HasFixedEstablishmentPage(countryIndex), true),
                submitAnswer(RegistrationTypePage(countryIndex), VatNumber),
                submitAnswer(EuVatNumberPage(countryIndex), taxId),
                pageMustBe(FixedEstablishmentTradingNamePage(countryIndex))
              )
          }
        }

        "the user proceeds to submit a Fixed Establishment Trading Name" in {

          startingFrom(TaxRegisteredInEuPage)
            .run(
              submitAnswer(TaxRegisteredInEuPage, true),
              submitAnswer(EuCountryPage(countryIndex), country),
              submitAnswer(HasFixedEstablishmentPage(countryIndex), true),
              submitAnswer(RegistrationTypePage(countryIndex), VatNumber),
              submitAnswer(EuVatNumberPage(countryIndex), euVatNumber),
              pageMustBe(FixedEstablishmentTradingNamePage(countryIndex))
            )
        }
      }
    }
  }
}
