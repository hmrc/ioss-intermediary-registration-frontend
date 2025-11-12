/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.amend

import base.SpecBase
import connectors.RegistrationConnector
import forms.amend.ViewOrChangePreviousRegistrationsMultipleFormProvider
import models.UserAnswers
import models.amend.PreviousRegistration
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ViewOrChangePreviousRegistrationsMultiplePage
import pages.{EmptyWaypoints, JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.amend.PreviousRegistrationIntermediaryNumberQuery
import repositories.AuthenticatedUserAnswersRepository
import services.ioss.AccountService
import utils.FutureSyntax.FutureOps
import views.html.amend.ViewOrChangePreviousRegistrationsMultipleView

class ViewOrChangePreviousRegistrationsMultipleControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val waypoints: Waypoints = EmptyWaypoints

  private val previousRegistrations: Seq[PreviousRegistration] = Gen.listOfN(4, arbitraryPreviousRegistration.arbitrary).sample.value

  private lazy val viewOrChangePreviousRegistrationsMultipleRoute = routes.ViewOrChangePreviousRegistrationsMultipleController.onPageLoad(waypoints).url

  private val formProvider = new ViewOrChangePreviousRegistrationsMultipleFormProvider()
  private val form: Form[String] = formProvider(previousRegistrations)

  private val validAnswer: String = previousRegistrations.map(_.intermediaryNumber).head
  private val invalidAnswer: String = arbitraryPreviousRegistration.arbitrary
    .suchThat(_.intermediaryNumber.toSeq != previousRegistrations.map(_.intermediaryNumber)).sample.value.intermediaryNumber

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockAccountService: AccountService = mock[AccountService]


  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationConnector)
    Mockito.reset(mockAccountService)
  }

  "ViewOrChangePreviousRegistrationsMultiple Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockAccountService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
        .overrides(bind[AccountService].toInstance(mockAccountService))
        .build()

      running(application) {
        val request = FakeRequest(GET, viewOrChangePreviousRegistrationsMultipleRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ViewOrChangePreviousRegistrationsMultipleView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, previousRegistrations)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswersWithVatInfo.set(ViewOrChangePreviousRegistrationsMultiplePage, "").success.value

      when(mockAccountService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[AccountService].toInstance(mockAccountService))
        .build()

      running(application) {
        val request = FakeRequest(GET, viewOrChangePreviousRegistrationsMultipleRoute)

        val view = application.injector.instanceOf[ViewOrChangePreviousRegistrationsMultipleView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill(""), waypoints, previousRegistrations)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture
      when(mockAccountService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
        .overrides(bind[AccountService].toInstance(mockAccountService))
        .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, viewOrChangePreviousRegistrationsMultipleRoute)
            .withFormUrlEncodedBody(("value", validAnswer))

        val result = route(application, request).value
        val expectedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
          .set(PreviousRegistrationIntermediaryNumberQuery, validAnswer).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          ViewOrChangePreviousRegistrationsMultiplePage.navigate(waypoints, emptyUserAnswersWithVatInfo, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockAccountService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
        .overrides(bind[AccountService].toInstance(mockAccountService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, viewOrChangePreviousRegistrationsMultipleRoute)
            .withFormUrlEncodedBody(("value", invalidAnswer))

        val boundForm = form.bind(Map("value" -> invalidAnswer))

        val view = application.injector.instanceOf[ViewOrChangePreviousRegistrationsMultipleView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, previousRegistrations)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, viewOrChangePreviousRegistrationsMultipleRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, viewOrChangePreviousRegistrationsMultipleRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
