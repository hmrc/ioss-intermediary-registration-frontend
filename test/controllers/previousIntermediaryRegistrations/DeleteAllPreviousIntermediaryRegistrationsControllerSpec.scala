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
import forms.previousIntermediaryRegistrations.DeleteAllPreviousIntermediaryRegistrationsFormProvider
import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetails
import models.{Country, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.previousIntermediaryRegistrations.*
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.previousIntermediaryRegistrations.AllPreviousIntermediaryRegistrationsQuery
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.previousIntermediaryRegistrations.DeleteAllPreviousIntermediaryRegistrationsView

class DeleteAllPreviousIntermediaryRegistrationsControllerSpec extends SpecBase with MockitoSugar {

  private val previousIntermediaryRegistrationDetails1: PreviousIntermediaryRegistrationDetails =
    arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value

  private val previousIntermediaryRegistrationDetails2: PreviousIntermediaryRegistrationDetails =
    arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value

  private val intermediaryNumber1: String = previousIntermediaryRegistrationDetails1.previousIntermediaryNumber
  private val country1: Country = previousIntermediaryRegistrationDetails1.previousEuCountry

  private val intermediaryNumber2: String = previousIntermediaryRegistrationDetails2.previousIntermediaryNumber
  private val country2: Country = previousIntermediaryRegistrationDetails2.previousEuCountry

  private val formProvider = new DeleteAllPreviousIntermediaryRegistrationsFormProvider()
  private val form: Form[Boolean] = formProvider()

  private lazy val deleteAllPreviousIntermediaryRegistrationsRoute: String = {
    routes.DeleteAllPreviousIntermediaryRegistrationsController.onPageLoad(waypoints).url
  }

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasPreviouslyRegisteredAsIntermediaryPage, true).success.value
    .set(PreviousEuCountryPage(countryIndex(0)), country1).success.value
    .set(PreviousIntermediaryRegistrationNumberPage(countryIndex(0)), intermediaryNumber1).success.value
    .set(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(0))), true).success.value
    .set(PreviousEuCountryPage(countryIndex(1)), country2).success.value
    .set(PreviousIntermediaryRegistrationNumberPage(countryIndex(1)), intermediaryNumber2).success.value

  "DeleteAllPreviousIntermediaryRegistrations Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, deleteAllPreviousIntermediaryRegistrationsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeleteAllPreviousIntermediaryRegistrationsView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints)(request, messages(application)).toString
      }
    }

    "must remove all Previous Intermediary Registrations and then redirect to the next page when valid the user answers Yes" in {

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
          FakeRequest(POST, deleteAllPreviousIntermediaryRegistrationsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(DeleteAllPreviousIntermediaryRegistrationsPage, true).success.value
          .remove(AllPreviousIntermediaryRegistrationsQuery).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` DeleteAllPreviousIntermediaryRegistrationsPage
          .navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must not remove all Previous Intermediary Registrations and then redirect to the next page when valid the user answers No" in {

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
          FakeRequest(POST, deleteAllPreviousIntermediaryRegistrationsRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(DeleteAllPreviousIntermediaryRegistrationsPage, false).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` DeleteAllPreviousIntermediaryRegistrationsPage
          .navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteAllPreviousIntermediaryRegistrationsRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DeleteAllPreviousIntermediaryRegistrationsView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, deleteAllPreviousIntermediaryRegistrationsRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteAllPreviousIntermediaryRegistrationsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
