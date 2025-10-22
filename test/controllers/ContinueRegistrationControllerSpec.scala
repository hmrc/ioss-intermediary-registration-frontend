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

package controllers

import base.SpecBase
import connectors.SaveForLaterConnector
import forms.ContinueRegistrationFormProvider
import models.{ContinueRegistration, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{IndexPage, JourneyRecoveryPage, SavedProgressContinuePage, SavedProgressPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}
import utils.FutureSyntax.FutureOps
import views.html.ContinueRegistrationView

class ContinueRegistrationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockSaveForLaterConnector: SaveForLaterConnector = mock[SaveForLaterConnector]

  private lazy val continueRegistrationRoute = routes.ContinueRegistrationController.onPageLoad(waypoints).url

  private val continueUrl: RedirectUrl = RedirectUrl("/continueUrl")

  private val formProvider: ContinueRegistrationFormProvider = new ContinueRegistrationFormProvider()
  private val form: Form[ContinueRegistration] = formProvider()

  override def beforeEach(): Unit = {
    Mockito.reset(mockSaveForLaterConnector)
  }

  "ContinueRegistration Controller" - {

    "must return OK and the correct view for a GET when saved user answers are present" in {

      val savedUserAnswers: UserAnswers = emptyUserAnswers
        .set(SavedProgressPage, continueUrl.get(OnlyRelative).url).success.value

      val application = applicationBuilder(userAnswers = Some(savedUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, continueRegistrationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ContinueRegistrationView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints)(request, messages(application)).toString
      }
    }


    "must return OK and the correct view for a GET when saved user answers are present and continue answer is prefilled" in {

      val savedUserAnswers: UserAnswers = emptyUserAnswers
        .set(SavedProgressPage, continueUrl.get(OnlyRelative).url).success.value
        .set(SavedProgressContinuePage, ContinueRegistration.Continue).success.value

      val application = applicationBuilder(userAnswers = Some(savedUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, continueRegistrationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ContinueRegistrationView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill(ContinueRegistration.Continue), waypoints)(request, messages(application)).toString
      }
    }

    "must redirect to the Index Page for a GET when saved user answers are not present" in {

      val savedUserAnswers: UserAnswers = emptyUserAnswers

      val application = applicationBuilder(userAnswers = Some(savedUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, continueRegistrationRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` IndexPage.route(waypoints).url
      }
    }

    "must redirect to the saved url page when the user submits the Continue option" in {

      val updatedAnswers: UserAnswers = emptyUserAnswers
        .set(SavedProgressPage, continueUrl.get(OnlyRelative).url).success.value

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(updatedAnswers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request = {
          FakeRequest(POST, continueRegistrationRoute)
            .withFormUrlEncodedBody(("value", ContinueRegistration.values.head.toString))
        }

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` continueUrl.get(OnlyRelative).url
        verify(mockSessionRepository, times(1)).set(any())
      }
    }

    "must delete the answers and redirect to the Index page when the user submits the Delete option" in {

      val updatedAnswers: UserAnswers = emptyUserAnswers
        .set(SavedProgressPage, continueUrl.get(OnlyRelative).url).success.value

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.clear(any())) thenReturn true.toFuture
      when(mockSaveForLaterConnector.delete()(any())) thenReturn Right(true).toFuture

      val application =
        applicationBuilder(userAnswers = Some(updatedAnswers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
            bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector)
          )
          .build()

      running(application) {
        val request = {
          FakeRequest(POST, continueRegistrationRoute)
            .withFormUrlEncodedBody(("value", ContinueRegistration.values.tail.head.toString))
        }

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` IndexPage.route(waypoints).url
        verify(mockSessionRepository, times(1)).clear(any())
        verify(mockSaveForLaterConnector, times(1)).delete()(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, continueRegistrationRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[ContinueRegistrationView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints)(request, messages(application)).toString
      }
    }

    "must redirect to the index page if SavedProgressPage is missing" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, continueRegistrationRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` IndexPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery if the form value is valid but SavedProgressPage is missing" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
            bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, continueRegistrationRoute)
            .withFormUrlEncodedBody(("value", ContinueRegistration.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryPage.route(waypoints).url
        verifyNoInteractions(mockSessionRepository)
        verifyNoInteractions(mockSaveForLaterConnector)
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, continueRegistrationRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, continueRegistrationRoute)
            .withFormUrlEncodedBody(("value", ContinueRegistration.values.head.toString))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER

        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
