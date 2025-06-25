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

package controllers.checkVatDetails

import base.SpecBase
import forms.checkVatDetails.CheckVatDetailsFormProvider
import models.UserAnswers
import models.checkVatDetails.CheckVatDetails
import models.domain.VatCustomerInfo
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.checkVatDetails.CheckVatDetailsPage
import play.api.data.Form
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import viewmodels.CheckVatDetailsViewModel
import views.html.checkVatDetails.CheckVatDetailsView

class CheckVatDetailsControllerSpec extends SpecBase with MockitoSugar {

  private lazy val checkVatDetailsRoute: String = routes.CheckVatDetailsController.onPageLoad(waypoints).url

  private val formProvider = new CheckVatDetailsFormProvider()
  private val form: Form[CheckVatDetails] = formProvider()

  private val niVatInfo: VatCustomerInfo = vatCustomerInfo
    .copy(desAddress = vatCustomerInfo.desAddress
      .copy(postCode = Some("BT12 3CD")))

  private val nonNiVatInfo: VatCustomerInfo = vatCustomerInfo
    .copy(desAddress = vatCustomerInfo.desAddress
      .copy(postCode = Some("AB12 3CD")))

  private val emptyUserAnswersWithNiVatInfo: UserAnswers = emptyUserAnswers.copy(vatInfo = Some(niVatInfo))
  private val emptyUserAnswersWithNonNiVatInfo: UserAnswers = emptyUserAnswers.copy(vatInfo = Some(nonNiVatInfo))

  "CheckVatDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithNiVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, checkVatDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckVatDetailsView]
        implicit val msgs: Messages = messages(application)
        val viewModel = CheckVatDetailsViewModel(vrn, niVatInfo)

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, viewModel, showAddress = true)(request, implicitly).toString
      }
    }

    "must return OK and the correct view for a GET when their principal place of business is not NI based" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithNonNiVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, checkVatDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckVatDetailsView]
        implicit val msgs: Messages = messages(application)
        val viewModel = CheckVatDetailsViewModel(vrn, nonNiVatInfo)

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, viewModel, showAddress = false)(request, implicitly).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswersWithNiVatInfo.set(CheckVatDetailsPage, CheckVatDetails.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, checkVatDetailsRoute)

        val view = application.injector.instanceOf[CheckVatDetailsView]
        implicit val msgs: Messages = messages(application)
        val viewModel = CheckVatDetailsViewModel(vrn, niVatInfo)

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill(CheckVatDetails.values.head), waypoints, viewModel, showAddress = true)(request, implicitly).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswersWithNiVatInfo))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, checkVatDetailsRoute)
            .withFormUrlEncodedBody(("value", CheckVatDetails.Yes.toString))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswersWithNiVatInfo.set(CheckVatDetailsPage, CheckVatDetails.Yes).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` CheckVatDetailsPage.navigate(waypoints, emptyUserAnswersWithNiVatInfo, expectedAnswers).route.url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted and their principal place of business is not NI based" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswersWithNonNiVatInfo))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, checkVatDetailsRoute)
            .withFormUrlEncodedBody(("value", CheckVatDetails.Yes.toString))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswersWithNonNiVatInfo.set(CheckVatDetailsPage, CheckVatDetails.Yes).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` CheckVatDetailsPage.navigate(waypoints, emptyUserAnswersWithNonNiVatInfo, expectedAnswers).route.url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithNiVatInfo)).build()

      running(application) {
        val request =
          FakeRequest(POST, checkVatDetailsRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[CheckVatDetailsView]
        val viewModel = CheckVatDetailsViewModel(vrn, niVatInfo)

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints, viewModel, showAddress = true)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, checkVatDetailsRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a GET if no VAT data is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, checkVatDetailsRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, checkVatDetailsRoute)
            .withFormUrlEncodedBody(("value", CheckVatDetails.values.head.toString))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER

        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "redirect to Journey Recovery for a POST if no VAT data is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, checkVatDetailsRoute)
            .withFormUrlEncodedBody(("value", CheckVatDetails.values.head.toString))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER

        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
