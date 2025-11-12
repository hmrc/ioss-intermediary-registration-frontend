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

package services.core

import base.SpecBase
import models.core.{Match, TraderId}
import models.etmp.EtmpOtherIossIntermediaryRegistrations
import models.etmp.display.{EtmpDisplayEuRegistrationDetails, EtmpDisplayRegistration}
import models.requests.AuthenticatedDataRequest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global

class EuRegistrationsValidationServiceSpec extends SpecBase with BeforeAndAfterEach {

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val request = AuthenticatedDataRequest(FakeRequest("GET", "/"), testCredentials, vrn, testEnrolments, emptyUserAnswers, None, 1, None, None, None, None)
  private implicit val dataRequest: AuthenticatedDataRequest[AnyContent] = AuthenticatedDataRequest(request, testCredentials, vrn, testEnrolments, emptyUserAnswers, None, 1, None, None, None, None)

  private val mockCoreRegistrationValidationService: CoreRegistrationValidationService = mock[CoreRegistrationValidationService]
  private val euRegistrationValidationService: EuRegistrationsValidationService = new EuRegistrationsValidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

  private val etmpDisplayRegistration: EtmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockCoreRegistrationValidationService)
  }

  "EuRegistrationsValidationService" - {

    ".validateEuRegistrationDetails" - {

      "must return Right(true) when there are no ETMP Display Eu Registration Details" in {

        val result = euRegistrationValidationService.validateEuRegistrationDetails(List.empty).futureValue

        result `mustBe` Right(true)
      }

      "for a matched VRN" - {

        "must return Right(true) when there are no active or quarantined traders found" in {

          val updatedEtmpDisplayEuRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails] = etmpDisplayRegistration.schemeDetails.euRegistrationDetails

          when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture

          val result = euRegistrationValidationService.validateEuRegistrationDetails(updatedEtmpDisplayEuRegistrationDetails).futureValue

          result `mustBe` Right(true)
          verify(mockCoreRegistrationValidationService, times(updatedEtmpDisplayEuRegistrationDetails.size)).searchEuVrn(any(), any())(any(), any())
        }

        "must return Left(InvalidActiveTrader) when there is an active trader found" in {

          val vrn: String = arbitraryVrn.arbitrary.sample.value.vrn
          val countryCode: String = arbitraryCountry.arbitrary.sample.value.code

          val updatedEtmpDisplayEuRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails] = etmpDisplayRegistration.schemeDetails.euRegistrationDetails :+
            etmpDisplayRegistration.schemeDetails.euRegistrationDetails.head.copy(issuedBy = countryCode, vatNumber = Some(vrn))

          val aMatch: Match = createMatch(exclusionStatusCode = None, memberState = countryCode)

          when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture
          when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(vrn), eqTo(countryCode))(any(), any())) thenReturn Some(aMatch).toFuture

          val result = euRegistrationValidationService.validateEuRegistrationDetails(updatedEtmpDisplayEuRegistrationDetails).futureValue

          result `mustBe` Left(InvalidActiveTrader(countryCode, aMatch.memberState))
          verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(vrn), eqTo(countryCode))(any(), any())
        }

        "must return Left(InvalidQuarantinedTrader) when there is a quarantined trader found" in {

          val vrn: String = arbitraryVrn.arbitrary.sample.value.vrn
          val countryCode: String = arbitraryCountry.arbitrary.sample.value.code

          val updatedEtmpDisplayEuRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails] = etmpDisplayRegistration.schemeDetails.euRegistrationDetails :+
            etmpDisplayRegistration.schemeDetails.euRegistrationDetails.head.copy(issuedBy = countryCode, vatNumber = Some(vrn))

          val aMatch: Match = createMatch(exclusionStatusCode = Some(4), memberState = countryCode)

          when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture
          when(mockCoreRegistrationValidationService.searchEuVrn(eqTo(vrn), eqTo(countryCode))(any(), any())) thenReturn Some(aMatch).toFuture

          val result = euRegistrationValidationService.validateEuRegistrationDetails(updatedEtmpDisplayEuRegistrationDetails).futureValue

          result `mustBe` Left(InvalidQuarantinedTrader)
          verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(vrn), eqTo(countryCode))(any(), any())
        }
      }

      "for a matched Tax Reference" - {

        "must return Right(true) when there are no active or quarantined traders found" in {

          val taxReferenceNumber: String = arbitraryTaxRefTraderID.arbitrary.sample.value.taxReferenceNumber

          val updatedEtmpDisplayEuRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails] = etmpDisplayRegistration.schemeDetails.euRegistrationDetails :+
            etmpDisplayRegistration.schemeDetails.euRegistrationDetails.head.copy(vatNumber = None, taxIdentificationNumber = Some(taxReferenceNumber))

          when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture
          when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn None.toFuture

          val result = euRegistrationValidationService.validateEuRegistrationDetails(updatedEtmpDisplayEuRegistrationDetails).futureValue

          result `mustBe` Right(true)
          verify(mockCoreRegistrationValidationService, times(updatedEtmpDisplayEuRegistrationDetails.size - 1)).searchEuVrn(any(), any())(any(), any())
          verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(any(), any())(any(), any())
        }

        "must return Left(InvalidActiveTrader) when there is an active trader found" in {

          val taxReferenceNumber: String = arbitraryTaxRefTraderID.arbitrary.sample.value.taxReferenceNumber
          val countryCode: String = arbitraryCountry.arbitrary.sample.value.code

          val updatedEtmpDisplayEuRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails] = etmpDisplayRegistration.schemeDetails.euRegistrationDetails :+
            etmpDisplayRegistration.schemeDetails.euRegistrationDetails.head.copy(issuedBy = countryCode, taxIdentificationNumber = Some(taxReferenceNumber), vatNumber = None)

          val aMatch: Match = createMatch(exclusionStatusCode = None, memberState = countryCode)

          when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture
          when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn None.toFuture
          when(mockCoreRegistrationValidationService.searchEuTaxId(eqTo(taxReferenceNumber), eqTo(countryCode))(any(), any())) thenReturn Some(aMatch).toFuture

          val result = euRegistrationValidationService.validateEuRegistrationDetails(updatedEtmpDisplayEuRegistrationDetails).futureValue

          result `mustBe` Left(InvalidActiveTrader(countryCode, aMatch.memberState))
          verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(taxReferenceNumber), eqTo(countryCode))(any(), any())
        }

        "must return Left(InvalidQuarantinedTrader) when there is a quarantined trader found" in {

          val taxReferenceNumber: String = arbitraryTaxRefTraderID.arbitrary.sample.value.taxReferenceNumber
          val countryCode: String = arbitraryCountry.arbitrary.sample.value.code

          val updatedEtmpDisplayEuRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails] = etmpDisplayRegistration.schemeDetails.euRegistrationDetails :+
            etmpDisplayRegistration.schemeDetails.euRegistrationDetails.head.copy(issuedBy = countryCode, taxIdentificationNumber = Some(taxReferenceNumber), vatNumber = None)

          val aMatch: Match = createMatch(exclusionStatusCode = Some(4), memberState = countryCode)

          when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture
          when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn None.toFuture
          when(mockCoreRegistrationValidationService.searchEuTaxId(eqTo(taxReferenceNumber), eqTo(countryCode))(any(), any())) thenReturn Some(aMatch).toFuture

          val result = euRegistrationValidationService.validateEuRegistrationDetails(updatedEtmpDisplayEuRegistrationDetails).futureValue

          result `mustBe` Left(InvalidQuarantinedTrader)
          verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(taxReferenceNumber), eqTo(countryCode))(any(), any())
        }
      }
    }

    ".validateOtherIossIntermediaryRegistrationDetails" - {

      "must return Right(true) when there are no previous Intermediary Registration details" in {

        val result = euRegistrationValidationService.validateOtherIossIntermediaryRegistrationDetails(List.empty).futureValue

        result `mustBe` Right(true)
      }

      "must return Right(true) when there are no active or quarantined traders found" in {

        val updatedOtherIossIntermediaryRegistration: Seq[EtmpOtherIossIntermediaryRegistrations] = etmpDisplayRegistration.intermediaryDetails
          .map(_.otherIossIntermediaryRegistrations).getOrElse(Seq.empty)

        when(mockCoreRegistrationValidationService.searchScheme(any(), any())(any(), any())) thenReturn None.toFuture

        val result = euRegistrationValidationService.validateOtherIossIntermediaryRegistrationDetails(updatedOtherIossIntermediaryRegistration).futureValue

        result `mustBe` Right(true)
        verify(mockCoreRegistrationValidationService, times(updatedOtherIossIntermediaryRegistration.size)).searchScheme(any(), any())(any(), any())
      }

      "must return Left(InvalidActiveTrader) when there is an active trader found" in {

        val intermediaryNumber: String = arbitrary[String].sample.value
        val countryCode: String = arbitraryCountry.arbitrary.sample.value.code

        val updatedOtherIossIntermediaryRegistration: Seq[EtmpOtherIossIntermediaryRegistrations] = etmpDisplayRegistration.intermediaryDetails
          .get.otherIossIntermediaryRegistrations :+ etmpDisplayRegistration.intermediaryDetails
          .get.otherIossIntermediaryRegistrations.head.copy(issuedBy = countryCode, intermediaryNumber = intermediaryNumber)

        val aMatch: Match = createMatch(exclusionStatusCode = None, memberState = countryCode)

        when(mockCoreRegistrationValidationService.searchScheme(any(), any())(any(), any())) thenReturn None.toFuture
        when(mockCoreRegistrationValidationService.searchScheme(eqTo(intermediaryNumber), eqTo(countryCode))(any(), any())) thenReturn Some(aMatch).toFuture

        val result = euRegistrationValidationService.validateOtherIossIntermediaryRegistrationDetails(updatedOtherIossIntermediaryRegistration).futureValue

        result `mustBe` Left(InvalidActiveTrader(countryCode, aMatch.memberState))
        verify(mockCoreRegistrationValidationService, times(1)).searchScheme(eqTo(intermediaryNumber), eqTo(countryCode))(any(), any())
      }

      "must return Left(InvalidQuarantinedTrader) when there is a quarantined trader found" in {

        val intermediaryNumber: String = arbitrary[String].sample.value
        val countryCode: String = arbitraryCountry.arbitrary.sample.value.code

        val updatedOtherIossIntermediaryRegistration: Seq[EtmpOtherIossIntermediaryRegistrations] = etmpDisplayRegistration.intermediaryDetails
          .get.otherIossIntermediaryRegistrations :+ etmpDisplayRegistration.intermediaryDetails
          .get.otherIossIntermediaryRegistrations.head.copy(issuedBy = countryCode, intermediaryNumber = intermediaryNumber)

        val aMatch: Match = createMatch(exclusionStatusCode = Some(4), memberState = countryCode)

        when(mockCoreRegistrationValidationService.searchScheme(any(), any())(any(), any())) thenReturn None.toFuture
        when(mockCoreRegistrationValidationService.searchScheme(eqTo(intermediaryNumber), eqTo(countryCode))(any(), any())) thenReturn Some(aMatch).toFuture

        val result = euRegistrationValidationService.validateOtherIossIntermediaryRegistrationDetails(updatedOtherIossIntermediaryRegistration).futureValue

        result `mustBe` Left(InvalidQuarantinedTrader)
        verify(mockCoreRegistrationValidationService, times(1)).searchScheme(eqTo(intermediaryNumber), eqTo(countryCode))(any(), any())
      }
    }
  }

  private def createMatch(exclusionStatusCode: Option[Int], memberState: String): Match = {
    Match(
      traderId = TraderId("IN9001234566"),
      intermediary = None,
      memberState = memberState,
      exclusionStatusCode = exclusionStatusCode,
      exclusionDecisionDate = None,
      exclusionEffectiveDate = None,
      nonCompliantReturns = None,
      nonCompliantPayments = None
    )
  }
}
