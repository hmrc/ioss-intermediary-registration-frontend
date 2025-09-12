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

package controllers.euDetails

import base.SpecBase
import forms.euDetails.AddEuDetailsFormProvider
import models.euDetails.RegistrationType.VatNumber
import models.requests.AuthenticatedDataRequest
import models.{CheckMode, Country, InternationalAddressWithTradingName, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.euDetails.*
import pages.{CheckYourAnswersPage, JourneyRecoveryPage, Waypoint, Waypoints}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.EuDetailsCompletionChecks.getAllIncompleteEuDetails
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.EuDetailsSummary
import views.html.euDetails.AddEuDetailsView

class AddEuDetailsControllerSpec extends SpecBase with MockitoSugar {

  private val euVatNumber: String = arbitraryEuVatNumber.sample.value
  private val countryCode: String = euVatNumber.substring(0, 2)
  private val country: Country = Country.euCountries.find(_.code == countryCode).head
  private val feAddress: InternationalAddressWithTradingName = arbitraryInternationalAddressWithTradingName.arbitrary.sample.value

  private val formProvider = new AddEuDetailsFormProvider()
  private val form: Form[Boolean] = formProvider()

  private lazy val addEuDetailsRoute: String = routes.AddEuDetailsController.onPageLoad(waypoints).url

  private def addEuDetailsRoutePost(waypoints: Waypoints = waypoints, incompletePromptShown: Boolean = false): String =
    routes.AddEuDetailsController.onSubmit(waypoints, incompletePromptShown = incompletePromptShown).url

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasFixedEstablishmentPage, true).success.value
    .set(EuCountryPage(countryIndex(0)), country).success.value
    .set(FixedEstablishmentAddressPage(countryIndex(0)), feAddress).success.value
    .set(RegistrationTypePage(countryIndex(0)), VatNumber).success.value
    .set(EuVatNumberPage(countryIndex(0)), euVatNumber).success.value

  private val incompleteAnswers: UserAnswers = updatedAnswers
    .remove(EuVatNumberPage(countryIndex(0))).success.value

  "AddEuDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, addEuDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddEuDetailsView]

        val euDetailsSummaryList: SummaryList = EuDetailsSummary.row(waypoints, updatedAnswers, AddEuDetailsPage())

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, euDetailsSummaryList, canAddEuDetails = true)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when the maximum number of EU countries has been reached" in {

      val userAnswers = (0 to Country.euCountries.size).foldLeft(updatedAnswers) { (userAnswers: UserAnswers, index: Int) =>
        userAnswers
          .set(EuCountryPage(countryIndex(index)), country).success.value
          .set(HasFixedEstablishmentPage, true).success.value
          .set(RegistrationTypePage(countryIndex(index)), VatNumber).success.value
          .set(EuVatNumberPage(countryIndex(index)), euVatNumber).success.value
          .set(FixedEstablishmentAddressPage(countryIndex(index)), feAddress).success.value
      }

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, addEuDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddEuDetailsView]

        val euDetailsSummaryList: SummaryList = EuDetailsSummary.row(waypoints, userAnswers, AddEuDetailsPage())

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, euDetailsSummaryList, canAddEuDetails = false)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when the maximum number of EU countries will be reached with the next iteration" in {

      val userAnswers = (0 until Country.euCountries.size - 1).foldLeft(updatedAnswers) { (userAnswers: UserAnswers, index: Int) =>
        userAnswers
          .set(EuCountryPage(countryIndex(index)), country).success.value
          .set(HasFixedEstablishmentPage, true).success.value
          .set(RegistrationTypePage(countryIndex(index)), VatNumber).success.value
          .set(EuVatNumberPage(countryIndex(index)), euVatNumber).success.value
          .set(FixedEstablishmentAddressPage(countryIndex(index)), feAddress).success.value
      }

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, addEuDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddEuDetailsView]

        val euDetailsSummaryList: SummaryList = EuDetailsSummary.row(waypoints, userAnswers, AddEuDetailsPage())

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, euDetailsSummaryList, canAddEuDetails = true)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when there are multiple countries with incomplete answers" in {

      val userAnswers = (0 until Country.euCountries.size - 1).foldLeft(updatedAnswers) { (userAnswers: UserAnswers, index: Int) =>
        userAnswers.set(EuCountryPage(countryIndex(index)), country).success.value
      }

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, addEuDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddEuDetailsView]

        val euDetailsSummaryList: SummaryList = EuDetailsSummary.row(waypoints, userAnswers, AddEuDetailsPage())

        val authenticatedDataRequest: AuthenticatedDataRequest[AnyContent] =
          AuthenticatedDataRequest(request, testCredentials, vrn, Enrolments(Set.empty), userAnswers, None, 1, None, None, None)

        val incomplete = getAllIncompleteEuDetails()(authenticatedDataRequest)

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, euDetailsSummaryList, canAddEuDetails = true, incomplete)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted in Normal mode" in {

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
          FakeRequest(POST, addEuDetailsRoutePost())
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(AddEuDetailsPage(), true).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` AddEuDetailsPage().navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted in Check mode" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(updatedAnswers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {

        val checkModeWaypoints: Waypoints = waypoints
          .setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))

        val request =
          FakeRequest(POST, addEuDetailsRoutePost(waypoints = checkModeWaypoints))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(AddEuDetailsPage(), true).success.value

        val expectedWaypoints: Waypoints = checkModeWaypoints
          .setNextWaypoint(Waypoint(AddEuDetailsPage(), CheckMode, AddEuDetailsPage().checkModeUrlFragment))

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` AddEuDetailsPage().navigate(expectedWaypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must refresh the page for a POST when answers are incomplete and the prompt has not been shown" - {

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, addEuDetailsRoutePost())
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` AddEuDetailsPage().route(waypoints).url
      }
    }

    "must redirect to the EuVatNumber page for a POST when answers are incomplete and the prompt has been shown" in {

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, addEuDetailsRoutePost(incompletePromptShown = true))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` EuVatNumberPage(countryIndex(0)).route(waypoints).url
      }
    }

    "must redirect to the corresponding page when answers are incomplete and the prompt has been shown in Check mode" in {

      val country: Country = arbitraryCountry.arbitrary.sample.value

      val incompleteAnswers: UserAnswers = updatedAnswers
        .set(EuCountryPage(countryIndex(1)), country).success.value
        .set(FixedEstablishmentAddressPage(countryIndex(1)), feAddress).success.value
        .set(RegistrationTypePage(countryIndex(1)), VatNumber).success.value

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {

        val checkModeWaypoints: Waypoints = waypoints
          .setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))

        val request =
          FakeRequest(POST, addEuDetailsRoutePost(waypoints = checkModeWaypoints, incompletePromptShown = true))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` EuVatNumberPage(countryIndex(1)).route(checkModeWaypoints).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request =
          FakeRequest(POST, addEuDetailsRoutePost())
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[AddEuDetailsView]

        val euDetailsSummaryList: SummaryList = EuDetailsSummary.row(waypoints, updatedAnswers, AddEuDetailsPage())

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints, euDetailsSummaryList, canAddEuDetails = true)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, addEuDetailsRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, addEuDetailsRoutePost())
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
