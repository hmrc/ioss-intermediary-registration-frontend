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
import connectors.RegistrationConnector
import models.Country.euCountries
import models.etmp.*
import models.etmp.display.{EtmpDisplayEuRegistrationDetails, EtmpDisplayRegistration, EtmpDisplaySchemeDetails, RegistrationWrapper}
import models.euDetails.{EuDetails, RegistrationType}
import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetails
import models.responses.etmp.EtmpEnrolmentResponse
import models.{BankDetails, ContactDetails, Country, InternationalAddressWithTradingName, TradingName, UkAddress, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.checkVatDetails.NiAddressPage
import pages.euDetails.HasFixedEstablishmentPage
import pages.filters.BusinessBasedInNiOrEuPage
import pages.previousIntermediaryRegistrations.HasPreviouslyRegisteredAsIntermediaryPage
import pages.tradingNames.HasTradingNamePage
import pages.{BankDetailsPage, ContactDetailsPage}
import play.api.test.Helpers.running
import queries.euDetails.AllEuDetailsQuery
import queries.previousIntermediaryRegistrations.AllPreviousIntermediaryRegistrationsQuery
import queries.tradingNames.AllTradingNamesQuery
import testutils.RegistrationData.{amendRegistrationResponse, etmpDisplayRegistration}
import testutils.WireMockHelper
import uk.gov.hmrc.http.HeaderCarrier
import utils.CheckNiBased.isNiBasedIntermediary
import utils.FutureSyntax.FutureOps

class RegistrationServiceSpec extends SpecBase with WireMockHelper with BeforeAndAfterEach {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val registrationService = new RegistrationService(stubClockAtArbitraryDate, mockRegistrationConnector)

  private val registrationWrapper: RegistrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value

  override def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
  }

  ".createRegistration" - {

    "must create a registration request from user answers provided and return a successful ETMP enrolment response" in {

      val etmpEnrolmentResponse: EtmpEnrolmentResponse =
        EtmpEnrolmentResponse(intRef = arbitrary[TaxRefTraderID].sample.value.taxReferenceNumber)

      when(mockRegistrationConnector.createRegistration(any())(any())) thenReturn Right(etmpEnrolmentResponse).toFuture

      val app = applicationBuilder(Some(completeUserAnswersWithVatInfo), Some(stubClockAtArbitraryDate))
        .build()

      running(app) {

        registrationService.createRegistration(completeUserAnswersWithVatInfo, vrn).futureValue mustBe Right(etmpEnrolmentResponse)
        verify(mockRegistrationConnector, times(1)).createRegistration(any())(any())
      }
    }
  }

  ".amendRegistration" - {

    "must create a registration request from user answers provided and return a successful response" in {
      
      when(mockRegistrationConnector.amendRegistration(any())(any())) thenReturn Right(amendRegistrationResponse).toFuture

      val app = applicationBuilder(Some(completeUserAnswersWithVatInfo), Some(stubClockAtArbitraryDate))
        .build()

      running(app) {
        registrationService.amendRegistration(
          answers = completeUserAnswersWithVatInfo,
          registration = etmpDisplayRegistration,
          vrn = vrn,
          iossNumber = intermediaryNumber,
          rejoin = false
        ).futureValue mustBe Right(amendRegistrationResponse)
        verify(mockRegistrationConnector, times(1)).amendRegistration(any())(any())
      }
    }
  }

  ".toUserAnswers" - {

    "must covert from RegistrationWrapper to UserAnswers" - {

      "when user is an NIBased Intermediary" in {

        val niPostCode: String = "BT11BT"

        val niRegistrationWrapper: RegistrationWrapper = registrationWrapper
          .copy(vatInfo = registrationWrapper.vatInfo.
            copy(desAddress = registrationWrapper.vatInfo.desAddress
              .copy(postCode = Some(niPostCode))
            )
          )

        val service = new RegistrationService(stubClockAtArbitraryDate, mockRegistrationConnector)

        val result = service.toUserAnswers(userAnswersId, niRegistrationWrapper).futureValue

        result `mustBe` convertedUserAnswers(niRegistrationWrapper).copy(lastUpdated = result.lastUpdated)
      }

      "when user is not an NIBased Intermediary" in {

        val nonNiPostCode: String = "LT11BT"

        val nonNiRegistrationWrapper: RegistrationWrapper = registrationWrapper
          .copy(vatInfo = registrationWrapper.vatInfo.
            copy(desAddress = registrationWrapper.vatInfo.desAddress
              .copy(postCode = Some(nonNiPostCode))
            )
          )

        val service = new RegistrationService(stubClockAtArbitraryDate, mockRegistrationConnector)

        val result = service.toUserAnswers(userAnswersId, nonNiRegistrationWrapper).futureValue

        result `mustBe` convertedUserAnswers(nonNiRegistrationWrapper).copy(lastUpdated = result.lastUpdated)
      }
    }

    "must throw an Illegal State Exception when NI based Intermediary is true but no UK Address is supplied" in {

      val nonNiPostCode: String = "LT11BT"

      val nonNiRegistrationWrapper: RegistrationWrapper = registrationWrapper
        .copy(vatInfo = registrationWrapper.vatInfo.
          copy(desAddress = registrationWrapper.vatInfo.desAddress
            .copy(postCode = Some(nonNiPostCode))
          )
        ).copy(etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration.copy(otherAddress = None))

      val service = new RegistrationService(stubClockAtArbitraryDate, mockRegistrationConnector)

      val result = service.toUserAnswers(userAnswersId, nonNiRegistrationWrapper).failed

      whenReady(result) { exp =>
        exp mustBe a[IllegalStateException]
        exp.getMessage mustBe "Must have A UK Address when Ni based Intermediary."
      }
    }

    "must throw a Run Time Exception when previous Intermediary country doesn't exist" in {

      val nonNiPostCode: String = "BT11BT"
      val invalidCountryCode: String = "WW"

      val invalidPreviousIntermediaryCountryRegistrationWrapper: RegistrationWrapper = registrationWrapper
        .copy(vatInfo = registrationWrapper.vatInfo.
          copy(desAddress = registrationWrapper.vatInfo.desAddress
            .copy(postCode = Some(nonNiPostCode))
          )
        ).copy(etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration.copy(
          intermediaryDetails = Some(EtmpIntermediaryDetails(
            otherIossIntermediaryRegistrations = Seq(
              EtmpOtherIossIntermediaryRegistrations(
                issuedBy = invalidCountryCode,
                intermediaryNumber = intermediaryNumber
              )
            )
          ))
        ))

      val service = new RegistrationService(stubClockAtArbitraryDate, mockRegistrationConnector)

      val result = service.toUserAnswers(userAnswersId, invalidPreviousIntermediaryCountryRegistrationWrapper).failed

      whenReady(result) { exp =>
        exp mustBe a[RuntimeException]
        exp.getMessage mustBe s"Country code $invalidCountryCode not found"
      }
    }

    "must throw an Illegal State Exception when EU country doesn't exist" in {

      val nonNiPostCode: String = "BT11BT"
      val invalidCountryCode: String = "WW"

      val invalidPreviousIntermediaryCountryRegistrationWrapper: RegistrationWrapper = registrationWrapper
        .copy(vatInfo = registrationWrapper.vatInfo.
          copy(desAddress = registrationWrapper.vatInfo.desAddress
            .copy(postCode = Some(nonNiPostCode))
          )
        ).copy(etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration
          .copy(schemeDetails = registrationWrapper.etmpDisplayRegistration.schemeDetails
            .copy(euRegistrationDetails = Seq(registrationWrapper.etmpDisplayRegistration.schemeDetails.euRegistrationDetails.head
              .copy(issuedBy = invalidCountryCode)
            ))
          ))

      val service = new RegistrationService(stubClockAtArbitraryDate, mockRegistrationConnector)

      val result = service.toUserAnswers(userAnswersId, invalidPreviousIntermediaryCountryRegistrationWrapper).failed

      whenReady(result) { exp =>
        exp mustBe a[IllegalStateException]
        exp.getMessage mustBe s"Unable to find country $invalidCountryCode"
      }
    }
  }

  private def convertedUserAnswers(registrationWrapper: RegistrationWrapper): UserAnswers = {
    val displayRegistration: EtmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration
    val convertedTradingNamesUA: Seq[TradingName] = convertTradingNames(displayRegistration.tradingNames)
    val convertedPreviousEuRegistrationDetails: Seq[PreviousIntermediaryRegistrationDetails] =
      convertPreviousIntermediaryRegistrationDetails(displayRegistration.intermediaryDetails)
    val convertedEuFixedEstablishmentDetails: Seq[EuDetails] =
      convertEuFixedEstablishmentDetails(displayRegistration.schemeDetails.euRegistrationDetails)
    val contactDetails: ContactDetails = getContactDetails(displayRegistration.schemeDetails)
    val convertedBankDetails: BankDetails = convertBankDetails(displayRegistration.bankDetails)

    val userAnswers = emptyUserAnswersWithVatInfo
      .copy(vatInfo = Some(registrationWrapper.vatInfo))
      .set(BusinessBasedInNiOrEuPage, isNiBasedIntermediary(registrationWrapper.vatInfo)).success.value
      .set(NiAddressPage, convertNonNiAddress(displayRegistration.otherAddress)).success.value
      .set(HasTradingNamePage, convertedTradingNamesUA.nonEmpty).success.value
      .set(AllTradingNamesQuery, convertedTradingNamesUA.toList).success.value
      .set(HasPreviouslyRegisteredAsIntermediaryPage, convertedPreviousEuRegistrationDetails.nonEmpty).success.value
      .set(AllPreviousIntermediaryRegistrationsQuery, convertedPreviousEuRegistrationDetails.toList).success.value
      .set(HasFixedEstablishmentPage, convertedEuFixedEstablishmentDetails.nonEmpty).success.value
      .set(AllEuDetailsQuery, convertedEuFixedEstablishmentDetails.toList).success.value
      .set(ContactDetailsPage, contactDetails).success.value
      .set(BankDetailsPage, convertedBankDetails).success.value

    if (isNiBasedIntermediary(registrationWrapper.vatInfo)) {
      userAnswers.remove(NiAddressPage).success.value
    } else {
      userAnswers
    }
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
      throw exception
    }
  }

  private def convertTradingNames(etmpTradingNames: Seq[EtmpTradingName]): Seq[TradingName] = {
    for {
      etmpTradingName <- etmpTradingNames
    } yield TradingName(name = etmpTradingName.tradingName)
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