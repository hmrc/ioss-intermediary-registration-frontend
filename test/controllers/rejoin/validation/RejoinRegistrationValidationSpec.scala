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

package controllers.rejoin.validation

import base.SpecBase
import controllers.filters.routes as filterRoutes
import models.CheckMode
import models.etmp.display.{EtmpDisplayEuRegistrationDetails, EtmpDisplayRegistration, EtmpDisplaySchemeDetails}
import models.etmp.{EtmpIntermediaryDetails, EtmpOtherIossIntermediaryRegistrations}
import models.requests.AuthenticatedDataRequest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.rejoin.RejoinSchemePage
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoint}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import services.core.{EuRegistrationsValidationService, InvalidActiveTrader, InvalidQuarantinedTrader}
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global

class RejoinRegistrationValidationSpec extends SpecBase with BeforeAndAfterEach {

  private val mockEuRegistrationsValidationService: EuRegistrationsValidationService = mock[EuRegistrationsValidationService]
  private val rejoinRegistrationValidation: RejoinRegistrationValidation = new RejoinRegistrationValidation(mockEuRegistrationsValidationService)

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val request = AuthenticatedDataRequest(FakeRequest("GET", "/"), testCredentials, vrn, testEnrolments, emptyUserAnswers, None, 1, None, None, None, None)
  implicit val dataRequest: AuthenticatedDataRequest[AnyContent] = AuthenticatedDataRequest(request, testCredentials, vrn, testEnrolments, emptyUserAnswers, None, 1, None, None, None, None)

  private val etmpDisplayRegistration: EtmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value
  private val rejoinWaypoints = EmptyWaypoints.setNextWaypoint(Waypoint(RejoinSchemePage, CheckMode, ""))

  override def beforeEach(): Unit = {
    Mockito.reset(mockEuRegistrationsValidationService)
  }

  "RejoinRegistrationValidation" - {

    "validateEuRegistrations" - {

      "must return Right(true) when there are no Intermediary details present" in {

        val updatedEtmpDisplayRegistration: EtmpDisplayRegistration = etmpDisplayRegistration.copy(intermediaryDetails = None)

        val euRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails] =
          updatedEtmpDisplayRegistration.schemeDetails.euRegistrationDetails

        val otherIossIntermediaryRegistrations: Seq[EtmpOtherIossIntermediaryRegistrations] =
          updatedEtmpDisplayRegistration.intermediaryDetails.map(_.otherIossIntermediaryRegistrations).getOrElse(Seq.empty)

        when(mockEuRegistrationsValidationService.validateEuRegistrationDetails(
          eqTo(euRegistrationDetails))(any(), any(), any())
        ) thenReturn Right(true).toFuture

        when(mockEuRegistrationsValidationService.validateOtherIossIntermediaryRegistrationDetails(
          eqTo(otherIossIntermediaryRegistrations))(any(), any(), any())
        ) thenReturn Right(true).toFuture

        val result = rejoinRegistrationValidation.validateEuRegistrations(
          rejoinWaypoints,
          updatedEtmpDisplayRegistration
        ).futureValue

        result `mustBe` Right(true)
        verify(mockEuRegistrationsValidationService, times(1)).validateEuRegistrationDetails(
          eqTo(euRegistrationDetails)
        )(any(), any(), any())

        verify(mockEuRegistrationsValidationService, times(0)).validateOtherIossIntermediaryRegistrationDetails(
          eqTo(otherIossIntermediaryRegistrations)
        )(any(), any(), any())
      }

      "must return Right(true) when there are Intermediary details but with no Other Ioss Intermediary Registrations present" in {

        val updatedEtmpDisplayRegistration: EtmpDisplayRegistration = etmpDisplayRegistration
          .copy(intermediaryDetails = Some(EtmpIntermediaryDetails(Seq.empty)))

        val euRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails] =
          updatedEtmpDisplayRegistration.schemeDetails.euRegistrationDetails

        val otherIossIntermediaryRegistrations: Seq[EtmpOtherIossIntermediaryRegistrations] =
          updatedEtmpDisplayRegistration.intermediaryDetails.map(_.otherIossIntermediaryRegistrations).getOrElse(Seq.empty)

        when(mockEuRegistrationsValidationService.validateEuRegistrationDetails(
          eqTo(euRegistrationDetails))(any(), any(), any())
        ) thenReturn Right(true).toFuture

        when(mockEuRegistrationsValidationService.validateOtherIossIntermediaryRegistrationDetails(
          eqTo(otherIossIntermediaryRegistrations))(any(), any(), any())
        ) thenReturn Right(true).toFuture

        val result = rejoinRegistrationValidation.validateEuRegistrations(
          rejoinWaypoints,
          updatedEtmpDisplayRegistration
        ).futureValue

        result `mustBe` Right(true)
        verify(mockEuRegistrationsValidationService, times(1)).validateEuRegistrationDetails(
          eqTo(euRegistrationDetails)
        )(any(), any(), any())

        verify(mockEuRegistrationsValidationService, times(1)).validateOtherIossIntermediaryRegistrationDetails(
          eqTo(otherIossIntermediaryRegistrations)
        )(any(), any(), any())
      }

      "must return Right(true) when there are eu registrations with no active or quarantined traders" in {

        when(mockEuRegistrationsValidationService.validateEuRegistrationDetails(
          eqTo(etmpDisplayRegistration.schemeDetails.euRegistrationDetails)
        )(any(), any(), any())) thenReturn Right(true).toFuture

        when(mockEuRegistrationsValidationService.validateOtherIossIntermediaryRegistrationDetails(
          eqTo(etmpDisplayRegistration.intermediaryDetails.map(_.otherIossIntermediaryRegistrations).getOrElse(Seq.empty))
        )(any(), any(), any())) thenReturn Right(true).toFuture

        val result = rejoinRegistrationValidation.validateEuRegistrations(
          rejoinWaypoints,
          etmpDisplayRegistration
        ).futureValue

        result `mustBe` Right(true)
      }

      // TODO -> Fix redirects
      "must redirect to ??? when EU Registrations has an active trader" in {

        val traderId: String = arbitraryVatNumberTraderId.arbitrary.sample.value.vatNumber
        val countryCode: String = arbitraryCountry.arbitrary.sample.value.code
        val memberState: String = countryCode

        val matchedEuRegistration: EtmpDisplayEuRegistrationDetails = etmpDisplayRegistration
          .schemeDetails.euRegistrationDetails.head
          .copy(issuedBy = countryCode, vatNumber = Some(traderId))

        val updatedEtmpDisplayRegistration: EtmpDisplayRegistration = etmpDisplayRegistration
          .copy(schemeDetails = etmpDisplayRegistration.schemeDetails
            .copy(euRegistrationDetails = etmpDisplayRegistration.schemeDetails.euRegistrationDetails :+
              matchedEuRegistration
            )
          )

        val euRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails] =
          updatedEtmpDisplayRegistration.schemeDetails.euRegistrationDetails

        when(mockEuRegistrationsValidationService.validateEuRegistrationDetails(
          eqTo(euRegistrationDetails))(any(), any(), any())
        ) thenReturn Left(InvalidActiveTrader(countryCode = countryCode, memberState = memberState)).toFuture

        val result = rejoinRegistrationValidation.validateEuRegistrations(
          rejoinWaypoints,
          updatedEtmpDisplayRegistration
        ).futureValue

        result `mustBe` Left(JourneyRecoveryPage.route(waypoints)) // TODO -> Update redirect
        verify(mockEuRegistrationsValidationService, times(1)).validateEuRegistrationDetails(
          eqTo(euRegistrationDetails)
        )(any(), any(), any())

        verify(mockEuRegistrationsValidationService, times(0)).validateOtherIossIntermediaryRegistrationDetails(
          any()
        )(any(), any(), any())
      }

      "must redirect to ??? when EU Registrations has a quarantined trader" in {

        val traderId: String = arbitraryTaxRefTraderID.arbitrary.sample.value.taxReferenceNumber
        val countryCode: String = arbitraryCountry.arbitrary.sample.value.code

        val matchedEuRegistration: EtmpDisplayEuRegistrationDetails = etmpDisplayRegistration
          .schemeDetails.euRegistrationDetails.head
          .copy(issuedBy = countryCode, vatNumber = None, taxIdentificationNumber = Some(traderId))

        val updatedEtmpDisplayRegistration: EtmpDisplayRegistration = etmpDisplayRegistration
          .copy(schemeDetails = etmpDisplayRegistration.schemeDetails
            .copy(euRegistrationDetails = etmpDisplayRegistration.schemeDetails.euRegistrationDetails :+
              matchedEuRegistration
            )
          )

        val euRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails] =
          updatedEtmpDisplayRegistration.schemeDetails.euRegistrationDetails

        when(mockEuRegistrationsValidationService.validateEuRegistrationDetails(
          eqTo(euRegistrationDetails))(any(), any(), any())
        ) thenReturn Left(InvalidQuarantinedTrader).toFuture

        val result = rejoinRegistrationValidation.validateEuRegistrations(
          rejoinWaypoints,
          updatedEtmpDisplayRegistration
        ).futureValue

        result `mustBe` Left(JourneyRecoveryPage.route(waypoints)) // TODO -> Update redirect
        verify(mockEuRegistrationsValidationService, times(1)).validateEuRegistrationDetails(
          eqTo(euRegistrationDetails)
        )(any(), any(), any())

        verify(mockEuRegistrationsValidationService, times(0)).validateOtherIossIntermediaryRegistrationDetails(
          any()
        )(any(), any(), any())
      }

      "must redirect to ??? when Intermediary details has an active trader" in {

        val intermediaryNumber: String = arbitrary[String].sample.value
        val countryCode: String = arbitraryCountry.arbitrary.sample.value.code
        val memberState: String = countryCode

        val updatedOtherIossIntermediaryRegistration: Seq[EtmpOtherIossIntermediaryRegistrations] = etmpDisplayRegistration
          .intermediaryDetails
          .get.otherIossIntermediaryRegistrations :+ etmpDisplayRegistration.intermediaryDetails
          .get.otherIossIntermediaryRegistrations.head.copy(issuedBy = countryCode, intermediaryNumber = intermediaryNumber)

        val updatedEtmpDisplayRegistration = etmpDisplayRegistration
          .copy(intermediaryDetails = Some(etmpDisplayRegistration.intermediaryDetails
            .get
            .copy(otherIossIntermediaryRegistrations = updatedOtherIossIntermediaryRegistration)
          ))

        val euRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails] =
          etmpDisplayRegistration.schemeDetails.euRegistrationDetails

        when(mockEuRegistrationsValidationService.validateEuRegistrationDetails(
          eqTo(euRegistrationDetails))(any(), any(), any())
        ) thenReturn Right(true).toFuture

        when(mockEuRegistrationsValidationService.validateOtherIossIntermediaryRegistrationDetails(
          eqTo(updatedOtherIossIntermediaryRegistration))(any(), any(), any())
        ) thenReturn Left(InvalidActiveTrader(countryCode = countryCode, memberState = memberState)).toFuture

        val result = rejoinRegistrationValidation.validateEuRegistrations(
          rejoinWaypoints,
          updatedEtmpDisplayRegistration
        ).futureValue

        // TODO -> Correct redirect???
        result `mustBe` Left(filterRoutes.SchemeStillActiveController.onPageLoad(countryCode))
        verify(mockEuRegistrationsValidationService, times(1)).validateEuRegistrationDetails(
          eqTo(euRegistrationDetails)
        )(any(), any(), any())

        verify(mockEuRegistrationsValidationService, times(1)).validateOtherIossIntermediaryRegistrationDetails(
          eqTo(updatedOtherIossIntermediaryRegistration)
        )(any(), any(), any())
      }

      "must redirect to ??? when Intermediary details has a quarantined trader" in {

        val intermediaryNumber: String = arbitrary[String].sample.value
        val countryCode: String = arbitraryCountry.arbitrary.sample.value.code

        val updatedOtherIossIntermediaryRegistration: Seq[EtmpOtherIossIntermediaryRegistrations] = etmpDisplayRegistration
          .intermediaryDetails
          .get.otherIossIntermediaryRegistrations :+ etmpDisplayRegistration.intermediaryDetails
          .get.otherIossIntermediaryRegistrations.head.copy(issuedBy = countryCode, intermediaryNumber = intermediaryNumber)

        val updatedEtmpDisplayRegistration = etmpDisplayRegistration
          .copy(intermediaryDetails = Some(etmpDisplayRegistration.intermediaryDetails
            .get
            .copy(otherIossIntermediaryRegistrations = updatedOtherIossIntermediaryRegistration)
          ))

        val euRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails] =
          etmpDisplayRegistration.schemeDetails.euRegistrationDetails

        when(mockEuRegistrationsValidationService.validateEuRegistrationDetails(
          eqTo(euRegistrationDetails))(any(), any(), any())
        ) thenReturn Right(true).toFuture

        when(mockEuRegistrationsValidationService.validateOtherIossIntermediaryRegistrationDetails(
          eqTo(updatedOtherIossIntermediaryRegistration))(any(), any(), any())
        ) thenReturn Left(InvalidQuarantinedTrader).toFuture

        val result = rejoinRegistrationValidation.validateEuRegistrations(
          rejoinWaypoints,
          updatedEtmpDisplayRegistration
        ).futureValue

        // TODO -> Correct redirect???
        result `mustBe` Left(JourneyRecoveryPage.route(waypoints))
        verify(mockEuRegistrationsValidationService, times(1)).validateEuRegistrationDetails(
          eqTo(euRegistrationDetails)
        )(any(), any(), any())

        verify(mockEuRegistrationsValidationService, times(1)).validateOtherIossIntermediaryRegistrationDetails(
          eqTo(updatedOtherIossIntermediaryRegistration)
        )(any(), any(), any())
      }
    }
  }
}
