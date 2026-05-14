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

import models.etmp.*
import models.etmp.display.{EtmpDisplayEuRegistrationDetails, EtmpDisplayRegistration}
import models.euDetails.EuDetails
import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetails
import models.{BankDetails, ContactDetails, TradingName, UkAddress, UserAnswers}
import pages.checkVatDetails.NiAddressPage
import pages.{BankDetailsPage, ContactDetailsPage}
import queries.euDetails.AllEuDetailsQuery
import queries.previousIntermediaryRegistrations.AllPreviousIntermediaryRegistrationsQuery
import queries.tradingNames.AllTradingNamesQuery

import javax.inject.Inject

class AmendAnswersComparisonService @Inject()() {

  def answersHaveChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    tradingNamesChanged(originalAnswers, userAnswers) ||
      previousRegistrationsChanged(originalAnswers, userAnswers) ||
      fixedEstablishmentsChanged(originalAnswers, userAnswers) ||
      contactDetailsChanged(originalAnswers, userAnswers) ||
      bankDetailsChanged(originalAnswers, userAnswers) ||
      niAddressChanged(originalAnswers, userAnswers)
  }

  private def tradingNamesChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(AllTradingNamesQuery).map(_.map(_.name)).getOrElse(Seq.empty) !=
      originalAnswers.tradingNames.map(_.tradingName)
  }

  private def previousRegistrationsChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(AllPreviousIntermediaryRegistrationsQuery).map(_.map(_.previousEuCountry.code)).getOrElse(Seq.empty) !=
      originalAnswers.intermediaryDetails
        .map(_.otherIossIntermediaryRegistrations.map(_.issuedBy))
        .getOrElse(Seq.empty)
  }

  private def fixedEstablishmentsChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    val amendedFixedEstablishments = userAnswers.get(AllEuDetailsQuery).getOrElse(Seq.empty)

    val originalFixedEstablishments = originalAnswers.schemeDetails.euRegistrationDetails

    val countriesChanged =
      amendedFixedEstablishments.map(_.euCountry.code).toSet !=
        originalFixedEstablishments.map(_.issuedBy).toSet

    val detailsChanged =
      amendedFixedEstablishments.exists { amended =>
        originalFixedEstablishments
          .find(_.issuedBy == amended.euCountry.code)
          .exists(original => fixedEstablishmentDetailsChanged(amended, original))
      }

    countriesChanged || detailsChanged
  }

  private def fixedEstablishmentDetailsChanged(amendedDetails: EuDetails, originalDetails: EtmpDisplayEuRegistrationDetails): Boolean = {
    val vatNumberWithoutCountryCode =
      amendedDetails.euVatNumber.map(_.stripPrefix(amendedDetails.euCountry.code))

    amendedDetails.fixedEstablishmentAddress.map(_.tradingName).exists(_ != originalDetails.fixedEstablishmentTradingName) ||
      amendedDetails.fixedEstablishmentAddress.exists { address =>
        address.line1 != originalDetails.fixedEstablishmentAddressLine1 ||
          address.line2 != originalDetails.fixedEstablishmentAddressLine2 ||
          address.townOrCity != originalDetails.townOrCity ||
          address.stateOrRegion != originalDetails.regionOrState ||
          address.postCode != originalDetails.postcode
      } ||
      vatNumberWithoutCountryCode != originalDetails.vatNumber ||
      amendedDetails.euTaxReference != originalDetails.taxIdentificationNumber
  }

  private def contactDetailsChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(ContactDetailsPage).exists { contactDetails =>
      contactDetails.fullName != originalAnswers.schemeDetails.contactName ||
        contactDetails.telephoneNumber != originalAnswers.schemeDetails.businessTelephoneNumber ||
        contactDetails.emailAddress != originalAnswers.schemeDetails.businessEmailId
    }
  }

  private def bankDetailsChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(BankDetailsPage).exists { bankDetails =>
      bankDetails.accountName != originalAnswers.bankDetails.accountName ||
        bankDetails.bic != originalAnswers.bankDetails.bic ||
        bankDetails.iban != originalAnswers.bankDetails.iban
    }
  }

  private def niAddressChanged(originalAnswers: EtmpDisplayRegistration, userAnswers: UserAnswers): Boolean = {
    userAnswers.get(NiAddressPage) match {
      case Some(address) =>
        originalAnswers.otherAddress match {
          case Some(originalAddress) =>
            address.line1 != originalAddress.addressLine1 ||
              address.line2 != originalAddress.addressLine2 ||
              address.townOrCity != originalAddress.townOrCity ||
              address.county != originalAddress.regionOrState ||
              address.postCode != originalAddress.postcode

          case None =>
            true
        }

      case None =>
        originalAnswers.otherAddress.isDefined
    }
  }

}
