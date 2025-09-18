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

package testutils

import base.SpecBase
import config.Constants.maxTradingNames
import formats.Format.eisDateFormatter
import models.etmp.*
import models.etmp.amend.{AmendRegistrationResponse, EtmpAmendCustomerIdentification, EtmpAmendRegistrationChangeLog, EtmpAmendRegistrationRequest}
import models.etmp.display.*
import models.{Bic, Country, Iban}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

import java.time.{LocalDate, LocalDateTime}

object RegistrationData extends SpecBase {

  val etmpEuRegistrationDetails: EtmpEuRegistrationDetails = EtmpEuRegistrationDetails(
    countryOfRegistration = arbitrary[Country].sample.value.code,
    traderId = arbitraryVatNumberTraderId.arbitrary.sample.value,
    tradingName = arbitraryEtmpTradingName.arbitrary.sample.value.tradingName,
    fixedEstablishmentAddressLine1 = arbitrary[String].sample.value,
    fixedEstablishmentAddressLine2 = Some(arbitrary[String].sample.value),
    townOrCity = arbitrary[String].sample.value,
    regionOrState = Some(arbitrary[String].sample.value),
    postcode = Some(arbitrary[String].sample.value)
  )

  val etmpDisplayEuRegistrationDetails: EtmpDisplayEuRegistrationDetails = EtmpDisplayEuRegistrationDetails(
    issuedBy = arbitrary[Country].sample.value.code,
    vatNumber = Some(Gen.alphaNumStr.sample.value),
    taxIdentificationNumber = None,
    fixedEstablishmentTradingName = arbitraryEtmpTradingName.arbitrary.sample.value.tradingName,
    fixedEstablishmentAddressLine1 = arbitrary[String].sample.value,
    fixedEstablishmentAddressLine2 = Some(arbitrary[String].sample.value),
    townOrCity = arbitrary[String].sample.value,
    regionOrState = Some(arbitrary[String].sample.value),
    postcode = Some(arbitrary[String].sample.value)
  )

  val etmpSchemeDetails: EtmpSchemeDetails = EtmpSchemeDetails(
    commencementDate = LocalDate.now.format(eisDateFormatter),
    euRegistrationDetails = Seq(etmpEuRegistrationDetails),
    previousEURegistrationDetails = Seq.empty,
    websites = None,
    contactName = arbitrary[String].sample.value,
    businessTelephoneNumber = arbitrary[String].sample.value,
    businessEmailId = arbitrary[String].sample.value,
    nonCompliantReturns = Some(arbitrary[Int].map(_.toString).sample.value),
    nonCompliantPayments = Some(arbitrary[Int].map(_.toString).sample.value)
  )

  val etmpDisplaySchemeDetails: EtmpDisplaySchemeDetails = EtmpDisplaySchemeDetails(
    commencementDate = etmpSchemeDetails.commencementDate,
    euRegistrationDetails = Seq(etmpDisplayEuRegistrationDetails),
    contactName = etmpSchemeDetails.contactName,
    businessTelephoneNumber = etmpSchemeDetails.businessTelephoneNumber,
    businessEmailId = etmpSchemeDetails.businessEmailId,
    unusableStatus = false,
    nonCompliantReturns = etmpSchemeDetails.nonCompliantReturns,
    nonCompliantPayments = etmpSchemeDetails.nonCompliantPayments
  )

  val genBankDetails: EtmpBankDetails = EtmpBankDetails(
    accountName = arbitrary[String].sample.value,
    bic = Some(arbitrary[Bic].sample.value),
    iban = arbitrary[Iban].sample.value
  )

  val etmpAdminUse: EtmpAdminUse = EtmpAdminUse(
    changeDate = Some(LocalDateTime.now())
  )

  val etmpRegistrationRequest: EtmpRegistrationRequest = EtmpRegistrationRequest(
    administration = arbitrary[EtmpAdministration].sample.value,
    customerIdentification = arbitrary[EtmpCustomerIdentification].sample.value,
    tradingNames = Seq(arbitrary[EtmpTradingName].sample.value),
    intermediaryDetails = Some(arbitrary[EtmpIntermediaryDetails].sample.value),
    otherAddress = Some(arbitrary[EtmpOtherAddress].sample.value),
    schemeDetails = etmpSchemeDetails,
    bankDetails = genBankDetails
  )

  val etmpDisplayRegistration: EtmpDisplayRegistration = EtmpDisplayRegistration(
    customerIdentification = etmpRegistrationRequest.customerIdentification,
    tradingNames = Gen.listOfN(maxTradingNames, arbitraryEtmpTradingName.arbitrary).sample.value,
    clientDetails = Seq(arbitrary[EtmpClientDetails].sample.value),
    intermediaryDetails = etmpRegistrationRequest.intermediaryDetails,
    otherAddress = etmpRegistrationRequest.otherAddress,
    schemeDetails = etmpDisplaySchemeDetails,
    exclusions = Gen.listOfN(3, arbitrary[EtmpExclusion]).sample.value,
    bankDetails = genBankDetails,
    adminUse = etmpAdminUse
  )

  val etmpAmendRegistrationRequest: EtmpAmendRegistrationRequest = EtmpAmendRegistrationRequest(
    administration = etmpRegistrationRequest.administration.copy(messageType = EtmpMessageType.IOSSIntAmend),
    changeLog = EtmpAmendRegistrationChangeLog(
      tradingNames = true,
      fixedEstablishments = true,
      contactDetails = true,
      bankDetails = true,
      reRegistration = false,
      otherAddress = true
    ),
    customerIdentification = EtmpAmendCustomerIdentification(iossNumber),
    tradingNames = etmpRegistrationRequest.tradingNames,
    intermediaryDetails = etmpRegistrationRequest.intermediaryDetails,
    otherAddress = etmpRegistrationRequest.otherAddress,
    schemeDetails = etmpRegistrationRequest.schemeDetails,
    bankDetails = etmpRegistrationRequest.bankDetails
  )

  val amendRegistrationResponse: AmendRegistrationResponse =
    AmendRegistrationResponse(
      processingDateTime = LocalDateTime.now(),
      formBundleNumber = "12345",
      vrn = "123456789",
      intermediary = "IN900100000001",
      businessPartner = "businessPartner"
    )

}
