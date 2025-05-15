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

package controllers.previousIntermediaryRegistrations

import base.SpecBase
import forms.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationNumberFormProvider
import models.core.MatchType.{FixedEstablishmentActiveNETP, FixedEstablishmentQuarantinedNETP, OtherMSNETPActiveNETP, OtherMSNETPQuarantinedNETP, TraderIdActiveNETP, TraderIdQuarantinedNETP}
import models.core.{Match, MatchType, TraderId}
import models.previousIntermediaryRegistrations.{IntermediaryIdentificationNumberValidation, PreviousIntermediaryRegistrationDetails}
import models.{Country, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.prop.TableDrivenPropertyChecks.*
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediaryPage, PreviousEuCountryPage, PreviousIntermediaryRegistrationNumberPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import services.core.CoreRegistrationValidationService
import utils.FutureSyntax.FutureOps
import views.html.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationNumberView

import scala.concurrent.Future

class PreviousIntermediaryRegistrationNumberControllerSpec extends SpecBase with MockitoSugar {

  private val previousIntermediaryRegistrationDetails: PreviousIntermediaryRegistrationDetails =
    arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value

  private val intermediaryNumber: String = previousIntermediaryRegistrationDetails.previousIntermediaryNumber
  private val country: Country = previousIntermediaryRegistrationDetails.previousEuCountry

  private val intermediaryRegistrationNumberPrefix: String = intermediaryNumber.substring(0, 5)

  private val hintText: String = IntermediaryIdentificationNumberValidation.euCountriesWithIntermediaryValidationRules
    .find(_.vrnRegex.contains(intermediaryRegistrationNumberPrefix))
    .map(_.messageInput).head

  private val formProvider = new PreviousIntermediaryRegistrationNumberFormProvider()
  private val form = formProvider(country)

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasPreviouslyRegisteredAsIntermediaryPage, true).success.value
    .set(PreviousEuCountryPage(countryIndex(0)), country).success.value

  private lazy val previousIntermediaryRegistrationNumberRoute: String =
    routes.PreviousIntermediaryRegistrationNumberController.onPageLoad(waypoints, countryIndex(0)).url

  private val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

  def createMatchResponse(
                           matchType: MatchType = MatchType.TransferringMSID,
                           traderId: TraderId = TraderId("IN333333333"),
                           exclusionStatusCode: Option[Int] = None
                         ): Match = Match(
    matchType,
    traderId = traderId,
    intermediary = None,
    memberState = "DE",
    exclusionStatusCode = exclusionStatusCode,
    exclusionDecisionDate = None,
    exclusionEffectiveDate = Some("2022-10-10"),
    nonCompliantReturns = None,
    nonCompliantPayments = None
  )

  "PreviousIntermediaryRegistrationNumber Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, previousIntermediaryRegistrationNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PreviousIntermediaryRegistrationNumberView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, countryIndex(0), country, hintText)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = updatedAnswers.set(PreviousIntermediaryRegistrationNumberPage(countryIndex(0)), "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, previousIntermediaryRegistrationNumberRoute)

        val view = application.injector.instanceOf[PreviousIntermediaryRegistrationNumberView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill("answer"), waypoints, countryIndex(0), country, hintText)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(updatedAnswers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          )
          .build()

      running(application) {
        when(mockCoreRegistrationValidationService.searchScheme(any(), any())(any(), any())) thenReturn
          Future.successful(None)

        val request =
          FakeRequest(POST, previousIntermediaryRegistrationNumberRoute)
            .withFormUrlEncodedBody(("value", intermediaryNumber))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(PreviousIntermediaryRegistrationNumberPage(countryIndex(0)), intermediaryNumber).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` PreviousIntermediaryRegistrationNumberPage(countryIndex(0))
          .navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, previousIntermediaryRegistrationNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[PreviousIntermediaryRegistrationNumberView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints, countryIndex(0), country, hintText)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, previousIntermediaryRegistrationNumberRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, previousIntermediaryRegistrationNumberRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to SchemeStillActive for a POST if an active intermediary trader is found" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val testConditions = Table(
        ("MatchType"),
        (TraderIdActiveNETP),
        (OtherMSNETPActiveNETP),
        (FixedEstablishmentActiveNETP)
      )

      forAll(testConditions) { (matchType) =>

        val application =
          applicationBuilder(userAnswers = Some(updatedAnswers))
            .overrides(
              bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
              bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
            )
            .build()

        running(application) {

          val activeIntermediaryMatch = createMatchResponse(
            matchType = matchType, traderId = TraderId("IN333333333")
          )

          when(mockCoreRegistrationValidationService.searchScheme(any(), any())(any(), any())) thenReturn
            Future.successful(Some(activeIntermediaryMatch))

          val request =
            FakeRequest(POST, previousIntermediaryRegistrationNumberRoute)
              .withFormUrlEncodedBody(("value", intermediaryNumber))

          val result = route(application, request).value

          redirectLocation(result).value mustEqual controllers.filters.routes.SchemeStillActiveController.onPageLoad(activeIntermediaryMatch.memberState).url
        }
      }
    }

    "must redirect to OtherCountryExcludedAndQuarantined for a POST if a quarantined intermediary trader is found" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val testConditions = Table(
        ("MatchType", "exclusionStatusCode"),
        (TraderIdQuarantinedNETP, None),
        (OtherMSNETPQuarantinedNETP, None),
        (FixedEstablishmentQuarantinedNETP, None)
      )

      forAll(testConditions) { (matchType, exclusionStatusCode) =>

        val application =
          applicationBuilder(userAnswers = Some(updatedAnswers))
            .overrides(
              bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
              bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
            )
            .build()

        running(application) {

          val quarantinedIntermediaryMatch = createMatchResponse(
            matchType = matchType, traderId = TraderId("IN333333333"), exclusionStatusCode = exclusionStatusCode
          )

          when(mockCoreRegistrationValidationService.searchScheme(any(), any())(any(), any())) thenReturn
            Future.successful(Some(quarantinedIntermediaryMatch))

          val request =
            FakeRequest(POST, previousIntermediaryRegistrationNumberRoute)
              .withFormUrlEncodedBody(("value", intermediaryNumber))

          val result = route(application, request).value

          redirectLocation(result).value mustEqual controllers.filters.routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(quarantinedIntermediaryMatch.memberState, quarantinedIntermediaryMatch.getEffectiveDate).url
        }
      }
    }
  }
}
