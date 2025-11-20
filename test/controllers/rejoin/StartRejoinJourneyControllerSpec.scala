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

package controllers.rejoin

import base.SpecBase
import connectors.RegistrationConnector
import controllers.filters.routes as filterRoutes
import controllers.rejoin.routes as rejoinRoutes
import controllers.rejoin.validation.RejoinRegistrationValidation
import models.etmp.EtmpExclusionReason.{NoLongerSupplies, Reversal}
import models.etmp.display.{EtmpDisplayRegistration, RegistrationWrapper}
import models.etmp.{EtmpExclusion, EtmpExclusionReason, EtmpIntermediaryDetails, EtmpOtherIossIntermediaryRegistrations}
import models.responses.NotFound
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.rejoin.{CannotRejoinPage, RejoinSchemePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps

import java.time.{Clock, LocalDate}

class StartRejoinJourneyControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockRejoinRegistrationValidation: RejoinRegistrationValidation = mock[RejoinRegistrationValidation]

  private val registrationWrapper: RegistrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value

  private def createRegistrationWrapperWithExclusion(effectiveDate: LocalDate, exclusionReason: EtmpExclusionReason): RegistrationWrapper = {
    val exclusion = EtmpExclusion(
      exclusionReason = exclusionReason,
      effectiveDate = effectiveDate,
      decisionDate = LocalDate.now(),
      quarantine = false
    )

    val etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration
    registrationWrapper.copy(etmpDisplayRegistration = etmpDisplayRegistration.copy(exclusions = List(exclusion)))
  }

  private lazy val rejoinRoute: String = rejoinRoutes.StartRejoinJourneyController.onPageLoad(waypoints).url

  override def beforeEach(): Unit =
    reset(
      mockRegistrationConnector,
      mockRejoinRegistrationValidation
    )

  "StartRejoinJourney Controller" - {

    "must save the answers and redirect to the RejoinScheme page when the registration request is successful" in {

      val mockSessionRepository: AuthenticatedUserAnswersRepository = mock[AuthenticatedUserAnswersRepository]
      val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now(), NoLongerSupplies)

      when(mockSessionRepository.set(any())) thenReturn true.toFuture
      when(mockRegistrationConnector.displayRegistration(any())(any())) thenReturn Right(registrationWrapperWithExclusionOnBoundary).toFuture
      when(mockRejoinRegistrationValidation.validateEuRegistrations(any(), any())(any(), any(), any())) thenReturn Right(true).toFuture

      val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo), clock = Some(Clock.systemUTC()))
        .overrides(
          bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
          bind[RegistrationConnector].toInstance(mockRegistrationConnector),
          bind[RejoinRegistrationValidation].toInstance(mockRejoinRegistrationValidation)
        )
        .build()

      running(application) {

        val request = FakeRequest(GET, rejoinRoute)
          .withSession("intermediaryNumber" -> intermediaryNumber)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe RejoinSchemePage.route(waypoints).url
        verify(mockRegistrationConnector, times(1)).displayRegistration(any())(any())
        verify(mockRejoinRegistrationValidation, times(1)).validateEuRegistrations(any(), any())(any(), any(), any())
      }
    }

    "must redirect when a validation error is returned from EuRegistrations and Other IOSS Intermediary registrations" in {

      val countryCode: String = arbitraryCountry.arbitrary.sample.value.code
      val redirect: Call = filterRoutes.SchemeStillActiveController.onPageLoad(countryCode)

      val registration: RegistrationWrapper = createRegistrationWrapperWithExclusion(LocalDate.now(stubClockAtArbitraryDate), NoLongerSupplies)
      val updatedOtherIossIntermediaryRegistration: Seq[EtmpOtherIossIntermediaryRegistrations] = registration.etmpDisplayRegistration
        .intermediaryDetails
        .get.otherIossIntermediaryRegistrations :+ registration.etmpDisplayRegistration.intermediaryDetails
        .get.otherIossIntermediaryRegistrations.head.copy(issuedBy = countryCode, intermediaryNumber = intermediaryNumber)

      val registrationWithFailedValidation: RegistrationWrapper = registration
        .copy(etmpDisplayRegistration = registration.etmpDisplayRegistration
          .copy(intermediaryDetails = Some(EtmpIntermediaryDetails(
            otherIossIntermediaryRegistrations = updatedOtherIossIntermediaryRegistration
          )))
        )

      when(mockRegistrationConnector.displayRegistration(any())(any())) thenReturn Right(registrationWithFailedValidation).toFuture
      when(mockRejoinRegistrationValidation.validateEuRegistrations(any(), any())(any(), any(), any())) thenReturn Left(redirect).toFuture

      val application = applicationBuilder(
        userAnswers = Some(completeUserAnswersWithVatInfo),
        clock = Some(stubClockAtArbitraryDate)
      )
        .overrides(
          bind[RegistrationConnector].toInstance(mockRegistrationConnector),
          bind[RejoinRegistrationValidation].toInstance(mockRejoinRegistrationValidation)
        ).build()

      running(application) {
        val request = FakeRequest(GET, rejoinRoute)
          .withSession("intermediaryNumber" -> intermediaryNumber)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` redirect.url
        verify(mockRegistrationConnector, times(1)).displayRegistration(eqTo(intermediaryNumber))(any())
        verify(mockRejoinRegistrationValidation, times(1))
          .validateEuRegistrations(any(), eqTo(registrationWithFailedValidation.etmpDisplayRegistration)
          )(any(), any(), any())
      }
    }

    "must redirect to Cannot Rejoin page when a treader cannot rejoin the scheme" in {

      val mockSessionRepository: AuthenticatedUserAnswersRepository = mock[AuthenticatedUserAnswersRepository]
      val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now(stubClockAtArbitraryDate), Reversal)

      when(mockRegistrationConnector.displayRegistration(any())(any())) thenReturn Right(registrationWrapperWithExclusionOnBoundary).toFuture

      val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo), clock = Some(stubClockAtArbitraryDate))
        .overrides(
          bind[RegistrationConnector].toInstance(mockRegistrationConnector)
        )
        .build()

      running(application) {

        val request = FakeRequest(GET, rejoinRoute)
          .withSession("intermediaryNumber" -> intermediaryNumber)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe CannotRejoinPage.route(waypoints).url
        verify(mockRegistrationConnector, times(1)).displayRegistration(any())(any())
        verifyNoInteractions(mockSessionRepository)
        verifyNoInteractions(mockRejoinRegistrationValidation)
      }
    }

    "must throw an Exception when the connector fails to retrieve registration details" in {

      when(mockRegistrationConnector.displayRegistration(any())(any())).thenReturn(Left(NotFound).toFuture)

      val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo))
        .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
        .build()

      running(application) {

        val request = FakeRequest(GET, rejoinRoute)

        val result = route(application, request).value

        whenReady(result.failed) { exp =>
          exp mustBe a[Exception]
          exp.getMessage mustBe NotFound.body
        }

        verify(mockRegistrationConnector, times(1)).displayRegistration(any())(any())
        verifyNoInteractions(mockRejoinRegistrationValidation)
      }
    }
  }
}
