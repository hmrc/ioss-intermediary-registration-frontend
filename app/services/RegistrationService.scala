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

import connectors.RegistrationConnector
import connectors.RegistrationHttpParser.{AmendRegistrationResultResponse, RegistrationResultResponse}
import logging.Logging
import models.Country.euCountries
import models.etmp.*
import models.etmp.EtmpRegistrationRequest.buildEtmpRegistrationRequest
import models.etmp.amend.EtmpAmendRegistrationRequest.buildEtmpAmendRegistrationRequest
import models.etmp.display.{EtmpDisplayEuRegistrationDetails, EtmpDisplayRegistration, EtmpDisplaySchemeDetails, RegistrationWrapper}
import models.euDetails.{EuDetails, RegistrationType}
import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetails
import models.{BankDetails, ContactDetails, Country, InternationalAddressWithTradingName, TradingName, UkAddress, UserAnswers}
import pages.checkVatDetails.NiAddressPage
import pages.euDetails.HasFixedEstablishmentPage
import pages.filters.BusinessBasedInNiOrEuPage
import pages.previousIntermediaryRegistrations.HasPreviouslyRegisteredAsIntermediaryPage
import pages.tradingNames.HasTradingNamePage
import pages.{BankDetailsPage, ContactDetailsPage}
import queries.euDetails.AllEuDetailsQuery
import queries.previousIntermediaryRegistrations.AllPreviousIntermediaryRegistrationsQuery
import queries.tradingNames.AllTradingNamesQuery
import services.etmp.EtmpEuRegistrations
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import utils.CheckNiBased.isNiBasedIntermediary

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.Future
import scala.util.Try

class RegistrationService @Inject()(
                                     clock: Clock,
                                     registrationConnector: RegistrationConnector
                                   ) extends EtmpEuRegistrations with Logging {

  def createRegistration(answers: UserAnswers, vrn: Vrn)(implicit hc: HeaderCarrier): Future[RegistrationResultResponse] = {
    val commencementDate = LocalDate.now(clock)
    registrationConnector.createRegistration(buildEtmpRegistrationRequest(answers, vrn, commencementDate))
  }

  def amendRegistration(
                       answers: UserAnswers,
                       registration: EtmpDisplayRegistration,
                       vrn: Vrn,
                       iossNumber: String,
                       rejoin: Boolean
                       )(implicit hc: HeaderCarrier): Future[AmendRegistrationResultResponse] = {
    
    val commencementDate = LocalDate.parse(registration.schemeDetails.commencementDate)

    registrationConnector.amendRegistration(
      buildEtmpAmendRegistrationRequest(
        answers = answers,
        registration = registration,
        vrn = vrn,
        commencementDate = commencementDate,
        iossNumber = iossNumber,
        rejoin = rejoin
      )
    )
  }

  def toUserAnswers(userId: String, registrationWrapper: RegistrationWrapper): Future[UserAnswers] = {

    val etmpTradingNames: Seq[EtmpTradingName] = registrationWrapper.etmpDisplayRegistration.tradingNames
    val maybeIntermediaryDetails: Option[EtmpIntermediaryDetails] = registrationWrapper.etmpDisplayRegistration.intermediaryDetails
    val maybeOtherAddress: Option[EtmpOtherAddress] = registrationWrapper.etmpDisplayRegistration.otherAddress
    val schemeDetails: EtmpDisplaySchemeDetails = registrationWrapper.etmpDisplayRegistration.schemeDetails
    val etmpBankDetails: EtmpBankDetails = registrationWrapper.etmpDisplayRegistration.bankDetails

    val hasNiBasedAddress: Boolean = isNiBasedIntermediary(registrationWrapper.vatInfo)

    val userAnswers = for {
      businessBasedInNi <- UserAnswers(
        id = userId,
        vatInfo = Some(registrationWrapper.vatInfo)
      ).set(BusinessBasedInNiOrEuPage, hasNiBasedAddress)
      hasNiAddress <- if (!hasNiBasedAddress) {
        businessBasedInNi.set(NiAddressPage, convertNonNiAddress(maybeOtherAddress))
      } else {
        Try(businessBasedInNi)
      }
      hasTradingNamesUA <- hasNiAddress.set(HasTradingNamePage, etmpTradingNames.nonEmpty)
      tradingNamesUA <- if (etmpTradingNames.nonEmpty) {
        hasTradingNamesUA.set(AllTradingNamesQuery, convertTradingNames(etmpTradingNames).toList)
      } else {
        Try(hasTradingNamesUA)
      }

      hasPreviousRegistrationsUA <- tradingNamesUA.set(HasPreviouslyRegisteredAsIntermediaryPage, maybeIntermediaryDetails.exists(_.otherIossIntermediaryRegistrations.nonEmpty))
      previousRegistrationsUA <- if (maybeIntermediaryDetails.exists(_.otherIossIntermediaryRegistrations.nonEmpty)) {
        hasPreviousRegistrationsUA.set(AllPreviousIntermediaryRegistrationsQuery, convertPreviousIntermediaryRegistrationDetails(maybeIntermediaryDetails).toList)
      } else {
        Try(hasPreviousRegistrationsUA)
      }

      hasEuFixedEstablishment <- previousRegistrationsUA.set(HasFixedEstablishmentPage, schemeDetails.euRegistrationDetails.nonEmpty)
      euFixedEstablishmentUA <- if (schemeDetails.euRegistrationDetails.nonEmpty) {
        hasEuFixedEstablishment.set(AllEuDetailsQuery, convertEuFixedEstablishmentDetails(schemeDetails.euRegistrationDetails).toList)
      } else {
        Try(hasEuFixedEstablishment)
      }

      contactDetailsUA <- euFixedEstablishmentUA.set(ContactDetailsPage, getContactDetails(schemeDetails))
      bankDetailsUA <- contactDetailsUA.set(BankDetailsPage, convertBankDetails(etmpBankDetails))
    } yield bankDetailsUA

    Future.fromTry(userAnswers)
  }

  private def convertNonNiAddress(maybeOtherAddress: Option[EtmpOtherAddress]): UkAddress = {
    maybeOtherAddress.map { otherAddress =>
      UkAddress(
        line1 = otherAddress.addressLine1,
        line2 = otherAddress.addressLine2,
        townOrCity = otherAddress.townOrCity,
        county = otherAddress.regionOrState,
        postCode = otherAddress.postcode
      )
    }.getOrElse {
      val exception = new IllegalStateException(s"Must have A UK Address when Ni based Intermediary.")
      logger.error(exception.getMessage, exception)
      throw exception
    }
  }


  private def convertTradingNames(etmpTradingNames: Seq[EtmpTradingName]): Seq[TradingName] = {
    for {
      tradingName <- etmpTradingNames.map(_.tradingName)
    } yield TradingName(name = tradingName)
  }

  private def convertPreviousIntermediaryRegistrationDetails(
                                                              maybeEtmpIntermediaryDetails: Option[EtmpIntermediaryDetails]
                                                            ): Seq[PreviousIntermediaryRegistrationDetails] = {
    maybeEtmpIntermediaryDetails match {
      case Some(etmpIntermediaryDetails) =>

        for {
          issuedBy <- etmpIntermediaryDetails.otherIossIntermediaryRegistrations.map(_.issuedBy).distinct
          otherIossIntermediaryRegistrations <- etmpIntermediaryDetails.otherIossIntermediaryRegistrations
        } yield {

          val country = euCountries.find(_.code == issuedBy)
            .getOrElse(throw new RuntimeException(s"Country code $issuedBy not found"))

          PreviousIntermediaryRegistrationDetails(
            previousEuCountry = country,
            previousIntermediaryNumber = otherIossIntermediaryRegistrations.intermediaryNumber,
            nonCompliantDetails = None
          )
        }

      case _ => List.empty
    }
  }


  private def convertEuFixedEstablishmentDetails(etmpEuRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails]): Seq[EuDetails] = {
    for {
      etmpDisplayEuyRegistrationDetails <- etmpEuRegistrationDetails
    } yield {
      EuDetails(
        euCountry = getCountry(etmpDisplayEuyRegistrationDetails.issuedBy),
        hasFixedEstablishment = Some(true),
        registrationType = determineRegistrationType(
          etmpDisplayEuyRegistrationDetails.vatNumber,
          etmpDisplayEuyRegistrationDetails.taxIdentificationNumber
        ),
        euVatNumber = convertEuVatNumber(etmpDisplayEuyRegistrationDetails.issuedBy, etmpDisplayEuyRegistrationDetails.vatNumber),
        euTaxReference = etmpDisplayEuyRegistrationDetails.taxIdentificationNumber,
        fixedEstablishmentAddress = Some(InternationalAddressWithTradingName(
          line1 = etmpDisplayEuyRegistrationDetails.fixedEstablishmentAddressLine1,
          line2 = etmpDisplayEuyRegistrationDetails.fixedEstablishmentAddressLine2,
          townOrCity = etmpDisplayEuyRegistrationDetails.townOrCity,
          stateOrRegion = etmpDisplayEuyRegistrationDetails.regionOrState,
          postCode = etmpDisplayEuyRegistrationDetails.postcode,
          country = getCountry(etmpDisplayEuyRegistrationDetails.issuedBy),
          tradingName = etmpDisplayEuyRegistrationDetails.fixedEstablishmentTradingName
        ))
      )
    }
  }

  private def convertEuVatNumber(countryCode: String, maybeVatNumber: Option[String]): Option[String] = {
    maybeVatNumber.map { vatNumber =>
      s"$countryCode$vatNumber"
    }
  }

  private def determineRegistrationType(vatNumber: Option[String], taxIdentificationNumber: Option[String]): Option[RegistrationType] = {
    (vatNumber, taxIdentificationNumber) match {
      case (Some(_), _) => Some(RegistrationType.VatNumber)
      case _ => Some(RegistrationType.TaxId)
    }
  }

  private def getCountry(countryCode: String): Country = {
    Country.fromCountryCode(countryCode) match {
      case Some(country) => country
      case _ =>
        val exception = new IllegalStateException(s"Unable to find country $countryCode")
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }

  private def getContactDetails(schemeDetails: EtmpDisplaySchemeDetails): ContactDetails = {
    ContactDetails(
      fullName = schemeDetails.contactName,
      telephoneNumber = schemeDetails.businessTelephoneNumber,
      emailAddress = schemeDetails.businessEmailId
    )
  }

  private def convertBankDetails(etmpBankDetails: EtmpBankDetails): BankDetails = {
    BankDetails(
      accountName = etmpBankDetails.accountName,
      bic = etmpBankDetails.bic,
      iban = etmpBankDetails.iban
    )
  }
}
