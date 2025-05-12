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
import models.{Country, Index, InternationalAddress, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.euDetails.*
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.EuDetailsSummary
import views.html.euDetails.AddEuDetailsView

class AddEuDetailsControllerSpec extends SpecBase with MockitoSugar {

  private val countryIndex: Index = Index(0)
  private val euVatNumber: String = arbitraryEuVatNumber.sample.value
  private val countryCode: String = euVatNumber.substring(0, 2)
  private val country: Country = Country.euCountries.find(_.code == countryCode).head
  private val feTradingName: String = arbitraryTradingName.arbitrary.sample.value.name
  private val feAddress: InternationalAddress = arbitraryInternationalAddress.arbitrary.sample.value

  private val formProvider = new AddEuDetailsFormProvider()
  private val form = formProvider()

  private lazy val addEuDetailsRoute = routes.AddEuDetailsController.onPageLoad(waypoints).url

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(TaxRegisteredInEuPage, true).success.value
    .set(EuCountryPage(countryIndex), country).success.value
    .set(HasFixedEstablishmentPage(countryIndex), true).success.value
    .set(RegistrationTypePage(countryIndex), VatNumber).success.value
    .set(EuVatNumberPage(countryIndex), euVatNumber).success.value
    .set(FixedEstablishmentTradingNamePage(countryIndex), feTradingName).success.value
    .set(FixedEstablishmentAddressPage(countryIndex), feAddress).success.value

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

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = updatedAnswers.set(AddEuDetailsPage(), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, addEuDetailsRoute)

        val view = application.injector.instanceOf[AddEuDetailsView]

        val result = route(application, request).value

        val euDetailsSummaryList: SummaryList = EuDetailsSummary.row(waypoints, updatedAnswers, AddEuDetailsPage())

        status(result) `mustBe` OK
        contentAsString(result) must not be view(form.fill(true), waypoints, euDetailsSummaryList, canAddEuDetails = true)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when the maximum number of EU countries has been reached" in {

      val userAnswers = (0 to Country.euCountries.size).foldLeft(updatedAnswers) { (userAnswers: UserAnswers, index: Int) =>
        userAnswers.set(EuCountryPage(Index(index)), country).success.value
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

    "must save the answer and redirect to the next page when valid data is submitted" in {

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
          FakeRequest(POST, addEuDetailsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(AddEuDetailsPage(), true).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` AddEuDetailsPage().navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return OK and the correct view for a GET when the maximum number of EU countries will be reached with the next iteration" in {

      val userAnswers = (0 until Country.euCountries.size - 1).foldLeft(updatedAnswers) { (userAnswers: UserAnswers, index: Int) =>
        userAnswers.set(EuCountryPage(Index(index)), country).success.value
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

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request =
          FakeRequest(POST, addEuDetailsRoute)
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
          FakeRequest(POST, addEuDetailsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
