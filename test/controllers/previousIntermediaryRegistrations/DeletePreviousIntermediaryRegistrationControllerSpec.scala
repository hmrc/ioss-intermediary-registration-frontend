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
import forms.previousIntermediaryRegistrations.DeletePreviousIntermediaryRegistrationFormProvider
import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetails
import models.{Country, Index, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.previousIntermediaryRegistrations.{DeletePreviousIntermediaryRegistrationPage, HasPreviouslyRegisteredAsIntermediaryPage, PreviousEuCountryPage, PreviousIntermediaryRegistrationNumberPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.previousIntermediaryRegistrations.{AllPreviousIntermediaryRegistrationsRawQuery, PreviousIntermediaryRegistrationQuery}
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.previousIntermediaryRegistrations.DeletePreviousIntermediaryRegistrationView

class DeletePreviousIntermediaryRegistrationControllerSpec extends SpecBase with MockitoSugar {

  private val countryIndex: Index = Index(0)

  private val previousIntermediaryRegistrationDetails: PreviousIntermediaryRegistrationDetails =
    arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value

  private val intermediaryNumber: String = previousIntermediaryRegistrationDetails.previousIntermediaryNumber
  private val country: Country = previousIntermediaryRegistrationDetails.previousEuCountry

  private val formProvider = new DeletePreviousIntermediaryRegistrationFormProvider()
  private val form: Form[Boolean] = formProvider(country)

  private lazy val deletePreviousIntermediaryRegistrationRoute: String = {
    routes.DeletePreviousIntermediaryRegistrationController.onPageLoad(waypoints, countryIndex).url
  }

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasPreviouslyRegisteredAsIntermediaryPage, true).success.value
    .set(PreviousEuCountryPage(countryIndex), country).success.value
    .set(PreviousIntermediaryRegistrationNumberPage(countryIndex), intermediaryNumber).success.value

  "DeletePreviousIntermediaryRegistration Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, deletePreviousIntermediaryRegistrationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeletePreviousIntermediaryRegistrationView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, countryIndex, country)(request, messages(application)).toString
      }
    }

    "must remove the record and redirect to the next page when the user answers Yes" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(updatedAnswers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, deletePreviousIntermediaryRegistrationRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .remove(PreviousIntermediaryRegistrationQuery(countryIndex)).success.value
          .remove(AllPreviousIntermediaryRegistrationsRawQuery).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` DeletePreviousIntermediaryRegistrationPage(countryIndex)
          .navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must remove the record and redirect to the next page when the user answers Yes and there are multiple countries" in {

      val previousIntermediaryRegistrationDetails2: PreviousIntermediaryRegistrationDetails =
        arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value

      val intermediaryNumber2: String = previousIntermediaryRegistrationDetails2.previousIntermediaryNumber
      val country2: Country = previousIntermediaryRegistrationDetails2.previousEuCountry

      val countryIndex2: Index = Index(1)

      val answers: UserAnswers = updatedAnswers
        .set(PreviousEuCountryPage(countryIndex2), country2).success.value
        .set(PreviousIntermediaryRegistrationNumberPage(countryIndex2), intermediaryNumber2).success.value

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, deletePreviousIntermediaryRegistrationRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = answers
          .remove(PreviousIntermediaryRegistrationQuery(countryIndex)).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` DeletePreviousIntermediaryRegistrationPage(countryIndex)
          .navigate(waypoints, answers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must not remove the record and redirect to the next page when the user answers No" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(updatedAnswers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, deletePreviousIntermediaryRegistrationRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` DeletePreviousIntermediaryRegistrationPage(countryIndex)
          .navigate(waypoints, updatedAnswers, updatedAnswers).url
        verifyNoInteractions(mockSessionRepository)
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, deletePreviousIntermediaryRegistrationRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DeletePreviousIntermediaryRegistrationView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints, countryIndex, country)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, deletePreviousIntermediaryRegistrationRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if the Previous Intermediary Registration is not found" in {

      val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

      running(application) {
        val request =
          FakeRequest(POST, deletePreviousIntermediaryRegistrationRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, deletePreviousIntermediaryRegistrationRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
