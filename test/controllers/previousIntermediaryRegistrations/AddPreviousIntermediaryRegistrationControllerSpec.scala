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
import forms.previousIntermediaryRegistrations.AddPreviousIntermediaryRegistrationFormProvider
import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetails
import models.{CheckMode, Country, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.previousIntermediaryRegistrations.{AddPreviousIntermediaryRegistrationPage, HasPreviouslyRegisteredAsIntermediaryPage, PreviousEuCountryPage, PreviousIntermediaryRegistrationNumberPage}
import pages.{CheckYourAnswersPage, JourneyRecoveryPage, Waypoint, Waypoints}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationsSummary
import views.html.previousIntermediaryRegistrations.AddPreviousIntermediaryRegistrationView

class AddPreviousIntermediaryRegistrationControllerSpec extends SpecBase with MockitoSugar {

  private val previousIntermediaryRegistrationDetails: PreviousIntermediaryRegistrationDetails =
    arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value

  private val intermediaryNumber: String = previousIntermediaryRegistrationDetails.previousIntermediaryNumber
  private val country: Country = previousIntermediaryRegistrationDetails.previousEuCountry

  private val formProvider = new AddPreviousIntermediaryRegistrationFormProvider()
  private val form: Form[Boolean] = formProvider()

  private lazy val addPreviousIntermediaryRegistrationRoute: String = {
    routes.AddPreviousIntermediaryRegistrationController.onPageLoad(waypoints).url
  }

  private def addPreviousIntermediaryRegistrationRoutePost(waypoints: Waypoints = waypoints, prompt: Boolean = false): String = {
    routes.AddPreviousIntermediaryRegistrationController.onSubmit(waypoints, prompt).url
  }

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasPreviouslyRegisteredAsIntermediaryPage, true).success.value
    .set(PreviousEuCountryPage(countryIndex(0)), country).success.value
    .set(PreviousIntermediaryRegistrationNumberPage(countryIndex(0)), intermediaryNumber).success.value

  private val incompleteAnswers: UserAnswers = updatedAnswers
    .remove(PreviousIntermediaryRegistrationNumberPage(countryIndex(0))).success.value

  "AddPreviousIntermediaryRegistration Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, addPreviousIntermediaryRegistrationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddPreviousIntermediaryRegistrationView]

        val previousIntermediaryRegistrationSummaryList: SummaryList = PreviousIntermediaryRegistrationsSummary
          .row(waypoints, updatedAnswers, AddPreviousIntermediaryRegistrationPage())

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, previousIntermediaryRegistrationSummaryList, canAddCountries = true)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when the maximum number of EU countries has been reached" in {

      val userAnswers: UserAnswers = (0 to Country.euCountries.size).foldLeft(updatedAnswers) { (userAnswers: UserAnswers, index: Int) =>
        userAnswers
          .set(PreviousEuCountryPage(countryIndex(index)), country).success.value
          .set(PreviousIntermediaryRegistrationNumberPage(countryIndex(index)), intermediaryNumber).success.value
      }

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, addPreviousIntermediaryRegistrationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddPreviousIntermediaryRegistrationView]

        val previousIntermediaryRegistrationSummaryList: SummaryList = PreviousIntermediaryRegistrationsSummary
          .row(waypoints, userAnswers, AddPreviousIntermediaryRegistrationPage())

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, previousIntermediaryRegistrationSummaryList, canAddCountries = false)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when the maximum number of EU countries will be reached with the next iteration" in {

      val userAnswers: UserAnswers = (0 until Country.euCountries.size - 1).foldLeft(updatedAnswers) { (userAnswers: UserAnswers, index: Int) =>
        userAnswers
          .set(PreviousEuCountryPage(countryIndex(index)), country).success.value
          .set(PreviousIntermediaryRegistrationNumberPage(countryIndex(index)), intermediaryNumber).success.value
      }

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, addPreviousIntermediaryRegistrationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddPreviousIntermediaryRegistrationView]

        val previousIntermediaryRegistrationSummaryList: SummaryList = PreviousIntermediaryRegistrationsSummary
          .row(waypoints, userAnswers, AddPreviousIntermediaryRegistrationPage())

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, previousIntermediaryRegistrationSummaryList, canAddCountries = true)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted in Normal mode" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application = applicationBuilder(userAnswers = Some(updatedAnswers))
        .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, addPreviousIntermediaryRegistrationRoutePost())
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(0))), true).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` AddPreviousIntermediaryRegistrationPage()
          .navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted in Check mode" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application = applicationBuilder(userAnswers = Some(updatedAnswers))
        .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {

        val checkModeWaypoints: Waypoints = waypoints
          .setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))

        val request =
          FakeRequest(POST, addPreviousIntermediaryRegistrationRoutePost(waypoints = checkModeWaypoints))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(0))), true).success.value

        val expectedWaypoints: Waypoints = checkModeWaypoints
          .setNextWaypoint(Waypoint(AddPreviousIntermediaryRegistrationPage(), CheckMode, AddPreviousIntermediaryRegistrationPage().checkModeUrlFragment))

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` AddPreviousIntermediaryRegistrationPage()
          .navigate(expectedWaypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must refresh the page for a POST when answers are incomplete and the prompt has not been shown" in {

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, addPreviousIntermediaryRegistrationRoutePost())
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` AddPreviousIntermediaryRegistrationPage().route(waypoints).url
      }
    }

    "must redirect to the Previous Intermediary Registration Number page for a POST when answers are incomplete and the prompt has been shown" in {

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, addPreviousIntermediaryRegistrationRoutePost(prompt = true))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` PreviousIntermediaryRegistrationNumberPage(countryIndex(0)).route(waypoints).url
      }
    }

    "must redirect to the Previous Intermediary Registration Number page for a POST when answers are incomplete and the prompt has been shown in Check mode" in {

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {

        val checkModeWaypoints: Waypoints = waypoints
          .setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))

        val request =
          FakeRequest(POST, addPreviousIntermediaryRegistrationRoutePost(waypoints = checkModeWaypoints, prompt = true))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` PreviousIntermediaryRegistrationNumberPage(countryIndex(0)).route(checkModeWaypoints).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request =
          FakeRequest(POST, addPreviousIntermediaryRegistrationRoutePost())
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[AddPreviousIntermediaryRegistrationView]

        val result = route(application, request).value

        val previousIntermediaryRegistrationSummaryList: SummaryList = PreviousIntermediaryRegistrationsSummary
          .row(waypoints, updatedAnswers, AddPreviousIntermediaryRegistrationPage())

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints, previousIntermediaryRegistrationSummaryList, canAddCountries = true)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, addPreviousIntermediaryRegistrationRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, addPreviousIntermediaryRegistrationRoutePost())
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
