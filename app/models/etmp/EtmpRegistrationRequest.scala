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

package models.etmp

import formats.Format.eisDateFormatter
import logging.Logging
import models.{ContactDetails, UserAnswers}
import models.etmp.*
import pages.*
import pages.checkVatDetails.NiAddressPage
import pages.tradingNames.HasTradingNamePage
import play.api.libs.json.{Json, OFormat}
import queries.tradingNames.AllTradingNamesQuery
import services.etmp.{EtmpEuRegistrations, EtmpPreviousIntermediaryRegistrations}
import uk.gov.hmrc.domain.Vrn

import java.time.LocalDate

final case class EtmpRegistrationRequest(
                                          administration: EtmpAdministration,
                                          customerIdentification: EtmpCustomerIdentification,
                                          tradingNames: Seq[EtmpTradingName],
                                          intermediaryDetails: Option[EtmpIntermediaryDetails],
                                          otherAddress: Option[EtmpOtherAddress],
                                          schemeDetails: EtmpSchemeDetails,
                                          bankDetails: EtmpBankDetails
                                        )

object EtmpRegistrationRequest extends EtmpEuRegistrations with EtmpPreviousIntermediaryRegistrations with Logging {

  implicit val format: OFormat[EtmpRegistrationRequest] = Json.format[EtmpRegistrationRequest]

  def buildEtmpRegistrationRequest(answers: UserAnswers, vrn: Vrn, commencementDate: LocalDate): EtmpRegistrationRequest =
    EtmpRegistrationRequest(
      administration = EtmpAdministration(messageType = EtmpMessageType.IOSSIntCreate),
      customerIdentification = EtmpCustomerIdentification(EtmpIdType.VRN, vrn.vrn),
      tradingNames = getTradingNames(answers),
      intermediaryDetails = getIntermediaryDetails(answers),
      otherAddress = getOtherAddress(answers),
      schemeDetails = getSchemeDetails(answers, commencementDate),
      bankDetails = getBankDetails(answers)
    )

  private def getIntermediaryDetails(answers: UserAnswers): Option[EtmpIntermediaryDetails] = {
    // TODO check optionality of this based on yes/no. IE if the wrapper is provided, does the object have to be?
    Some(EtmpIntermediaryDetails(getPreviousRegistrationDetails(answers)))
  }

  private def getOtherAddress(answers: UserAnswers): Option[EtmpOtherAddress] = {
    answers.get(NiAddressPage).map{ niAddress =>
      EtmpOtherAddress(
        issuedBy = "XI", // TODO check
        "", // TODO trading name???
        AddressLine1 = niAddress.line1,
        AddressLine2 = niAddress.line2,
        townOrCity = niAddress.townOrCity,
        regionOrState = niAddress.county,
        postcode = niAddress.postCode
      )
    }
  }

  private def getSchemeDetails(answers: UserAnswers, commencementDate: LocalDate): EtmpSchemeDetails = {

    val businessContactDetails = getBusinessContactDetails(answers)

    EtmpSchemeDetails(
      commencementDate = commencementDate.format(eisDateFormatter),
      euRegistrationDetails = getEuTaxRegistrations(answers),
      previousEURegistrationDetails = Seq.empty, // TODO empty ??
      websites = Seq.empty,
      contactName = businessContactDetails.fullName,
      businessTelephoneNumber = businessContactDetails.telephoneNumber,
      businessEmailId = businessContactDetails.emailAddress,
      nonCompliantReturns = None, // TODO VEI-294
      nonCompliantPayments = None // TODO VEI-294
    )
  }

  private def getTradingNames(answers: UserAnswers): List[EtmpTradingName] = {
    answers.get(HasTradingNamePage) match {
      case Some(true) =>
        answers.get(AllTradingNamesQuery) match {
          case Some(tradingNames) =>
            for {
              tradingName <- tradingNames
            } yield EtmpTradingName(tradingName = tradingName.name)
          case Some(Nil) | None =>
            val exception = new IllegalStateException("Must have at least one trading name")
            logger.error(exception.getMessage, exception)
            throw exception
        }

      case Some(false) =>
        List.empty

      case None =>
        val exception = new IllegalStateException("Must select Yes if trading name is different")
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }

  private def getBusinessContactDetails(answers: UserAnswers): ContactDetails = {
    answers.get(ContactDetailsPage) match {
      case Some(contactDetails) => contactDetails
      case _ =>
        val exception = new IllegalStateException("User must provide contact details")
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }

  private def getBankDetails(answers: UserAnswers): EtmpBankDetails =
    answers.get(BankDetailsPage) match {
      case Some(bankDetails) =>
        EtmpBankDetails(bankDetails.accountName, bankDetails.bic, bankDetails.iban)
      case _ =>
        val exception = new IllegalStateException("User must provide bank details")
        logger.error(exception.getMessage, exception)
        throw exception
    }

}
