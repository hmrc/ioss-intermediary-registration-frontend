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

package services

import base.SpecBase
import models.etmp.*
import models.etmp.display.{EtmpDisplayEuRegistrationDetails, EtmpDisplayRegistration}
import models.euDetails.EuDetails
import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetails
import models.{BankDetails, ContactDetails, Country, InternationalAddressWithTradingName, TradingName, UkAddress, UserAnswers}
import pages.checkVatDetails.NiAddressPage
import pages.{BankDetailsPage, ContactDetailsPage}
import queries.euDetails.AllEuDetailsQuery
import queries.previousIntermediaryRegistrations.AllPreviousIntermediaryRegistrationsQuery
import queries.tradingNames.AllTradingNamesQuery
import testutils.RegistrationData.etmpDisplayRegistration
import testutils.WireMockHelper

class AmendAnswersComparisonServiceSpec extends SpecBase with WireMockHelper {

  private val service = new AmendAnswersComparisonService()
  private val originalAnswers: EtmpDisplayRegistration = etmpDisplayRegistration

  ".answersHaveChanged" - {

    "return false when nothing has changed" in {
      val userAnswers = matchingUserAnswers(originalAnswers)

      service.answersHaveChanged(originalAnswers, userAnswers) mustBe false
    }

    "return true when trading names have changed" in {
      val userAnswers = matchingUserAnswers(originalAnswers)
        .set(
          AllTradingNamesQuery,
          List(TradingName("Changed trading name"))
        ).success.value

      service.answersHaveChanged(originalAnswers, userAnswers) mustBe true
    }

    "return true when previous registrations have changed" in {
      val userAnswers = matchingUserAnswers(originalAnswers)
        .set(
          AllPreviousIntermediaryRegistrationsQuery,
          List(
            PreviousIntermediaryRegistrationDetails(
              previousEuCountry = Country.fromCountryCodeUnsafe("FR"),
              previousIntermediaryNumber = "INFR1234567890",
              nonCompliantDetails = None
            )
          )
        ).success.value

      service.answersHaveChanged(originalAnswers, userAnswers) mustBe true
    }

    "return true when fixed establishment countries have changed" in {
      val userAnswers = matchingUserAnswers(originalAnswers)
        .set(
          AllEuDetailsQuery,
          List(
            EuDetails(
              euCountry = Country.fromCountryCodeUnsafe("FR"),
              hasFixedEstablishment = Some(true),
              fixedEstablishmentAddress = None,
              euVatNumber = None,
              euTaxReference = None,
              registrationType = None
            )
          )
        ).success.value

      service.answersHaveChanged(originalAnswers, userAnswers) mustBe true
    }

    "return true when fixed establishment details have changed" in {
      val existingEuDetails = originalAnswers.schemeDetails.euRegistrationDetails.head
      val country = Country.fromCountryCodeUnsafe(existingEuDetails.issuedBy)

      val changedEuDetails = EuDetails(
        euCountry = country,
        hasFixedEstablishment = Some(true),
        fixedEstablishmentAddress = Some(
          InternationalAddressWithTradingName(
            tradingName = "Changed Trading Name",
            line1 = "Changed address line 1",
            line2 = existingEuDetails.fixedEstablishmentAddressLine2,
            townOrCity = existingEuDetails.townOrCity,
            stateOrRegion = existingEuDetails.regionOrState,
            postCode = existingEuDetails.postcode,
            country = country
          )
        ),
        euVatNumber = existingEuDetails.vatNumber.map(vat => s"${existingEuDetails.issuedBy}$vat"),
        euTaxReference = existingEuDetails.taxIdentificationNumber,
        registrationType = None
      )

      val userAnswers = matchingUserAnswers(originalAnswers)
        .set(AllEuDetailsQuery, List(changedEuDetails)).success.value

      service.answersHaveChanged(originalAnswers, userAnswers) mustBe true
    }

    "return true when contact details have changed" in {
      val userAnswers = matchingUserAnswers(originalAnswers)
        .set(
          ContactDetailsPage,
          ContactDetails(
            fullName = "Changed Name",
            telephoneNumber = originalAnswers.schemeDetails.businessTelephoneNumber,
            emailAddress = originalAnswers.schemeDetails.businessEmailId
          )
        ).success.value

      service.answersHaveChanged(originalAnswers, userAnswers) mustBe true
    }

    "return true when bank details have changed" in {
      val userAnswers = matchingUserAnswers(originalAnswers)
        .set(
          BankDetailsPage,
          BankDetails(
            accountName = "Changed Account Name",
            bic = originalAnswers.bankDetails.bic,
            iban = originalAnswers.bankDetails.iban
          )
        ).success.value

      service.answersHaveChanged(originalAnswers, userAnswers) mustBe true
    }

    "return true when NI address has changed" in {
      val userAnswers = matchingUserAnswers(originalAnswers)
        .set(
          NiAddressPage,
          UkAddress(
            line1 = "Changed address line 1",
            line2 = None,
            townOrCity = "Belfast",
            county = None,
            postCode = "BT1 1AA"
          )
        ).success.value

      service.answersHaveChanged(originalAnswers, userAnswers) mustBe true
    }
  }

  private def matchingUserAnswers(original: EtmpDisplayRegistration): UserAnswers = {

    val baseAnswers =
      emptyUserAnswers
        .set(
          AllTradingNamesQuery,
          original.tradingNames.map(x => TradingName(x.tradingName)).toList
        ).success.value
        .set(
          AllPreviousIntermediaryRegistrationsQuery,
          original.intermediaryDetails
            .map(_.otherIossIntermediaryRegistrations.map { x =>
              PreviousIntermediaryRegistrationDetails(
                previousEuCountry = Country.fromCountryCodeUnsafe(x.issuedBy),
                previousIntermediaryNumber = x.intermediaryNumber,
                nonCompliantDetails = None
              )
            }.toList)
            .getOrElse(List.empty)
        ).success.value
        .set(
          AllEuDetailsQuery,
          original.schemeDetails.euRegistrationDetails.map(toEuDetails).toList
        ).success.value
        .set(
          ContactDetailsPage,
          ContactDetails(
            fullName = original.schemeDetails.contactName,
            telephoneNumber = original.schemeDetails.businessTelephoneNumber,
            emailAddress = original.schemeDetails.businessEmailId
          )
        ).success.value
        .set(
          BankDetailsPage,
          BankDetails(
            accountName = original.bankDetails.accountName,
            bic = original.bankDetails.bic,
            iban = original.bankDetails.iban
          )
        ).success.value

    addNiAddress(baseAnswers, original)
  }

  private def toEuDetails(original: EtmpDisplayEuRegistrationDetails): EuDetails = {

    val country = Country.fromCountryCodeUnsafe(original.issuedBy)

    EuDetails(
      euCountry = country,
      hasFixedEstablishment = Some(true),
      fixedEstablishmentAddress = Some(
        InternationalAddressWithTradingName(
          tradingName = original.fixedEstablishmentTradingName,
          line1 = original.fixedEstablishmentAddressLine1,
          line2 = original.fixedEstablishmentAddressLine2,
          townOrCity = original.townOrCity,
          stateOrRegion = original.regionOrState,
          postCode = original.postcode,
          country = country
        )
      ),
      euVatNumber = original.vatNumber.map { vat =>
        if (vat.startsWith(original.issuedBy)) vat else s"${original.issuedBy}$vat"
      },
      euTaxReference = original.taxIdentificationNumber,
      registrationType = None
    )
  }

  private def addNiAddress(userAnswers: UserAnswers, original: EtmpDisplayRegistration): UserAnswers = {

    original.otherAddress match {

      case Some(address) =>
        userAnswers.set(
          NiAddressPage,
          UkAddress(
            line1 = address.addressLine1,
            line2 = address.addressLine2,
            townOrCity = address.townOrCity,
            county = address.regionOrState,
            postCode = address.postcode
          )
        ).success.value

      case None =>
        userAnswers
    }
  }
}
