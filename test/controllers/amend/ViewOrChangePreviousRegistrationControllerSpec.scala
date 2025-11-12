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

package controllers.amend

import base.SpecBase
import connectors.RegistrationConnector
import forms.amend.ViewOrChangePreviousRegistrationFormProvider
import models.amend.PreviousRegistration
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.{ViewOrChangePreviousRegistrationPage, ViewOrChangePreviousRegistrationsMultiplePage}
import pages.{EmptyWaypoints, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.amend.PreviousRegistrationIntermediaryNumberQuery
import repositories.AuthenticatedUserAnswersRepository
import services.ioss.AccountService
import utils.FutureSyntax.FutureOps
import views.html.amend.ViewOrChangePreviousRegistrationView

import java.time.LocalDate

class ViewOrChangePreviousRegistrationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val previousRegistration = PreviousRegistration(intermediaryNumber, LocalDate.now(), LocalDate.now().plusMonths(6))

  private val formProvider = new ViewOrChangePreviousRegistrationFormProvider()
  private val form: Form[Boolean] = formProvider(intermediaryNumber)

  private val waypoints: Waypoints = EmptyWaypoints

  private lazy val viewOrChangePreviousRegistrationRoute: String = routes.ViewOrChangePreviousRegistrationController.onPageLoad(waypoints).url
  private lazy val viewOrChangePreviousRegistrationSubmitRoute: String = routes.ViewOrChangePreviousRegistrationController.onSubmit(waypoints).url

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockAccountService: AccountService = mock[AccountService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationConnector)
    Mockito.reset(mockAccountService)
  }

  "must return OK and the correct view for a GET when a single previous registration exists" in {

    when(mockAccountService.getPreviousRegistrations()(any())).thenReturn(Seq(previousRegistration).toFuture)

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
      .overrides(bind[AccountService].toInstance(mockAccountService))
      .build()

    running(application) {
      val request = FakeRequest(GET, viewOrChangePreviousRegistrationRoute)

      val result = route(application, request).value

      val view = application.injector.instanceOf[ViewOrChangePreviousRegistrationView]

      status(result) mustBe OK
      contentAsString(result) mustBe view(form, waypoints, intermediaryNumber)(request, messages(application)).toString
    }
  }
  "must redirect to the next page on a GET when multiple previous registrations exist" in {

    when(mockAccountService.getPreviousRegistrations()(any())).thenReturn(Seq(previousRegistration, previousRegistration).toFuture)

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
      .overrides(bind[AccountService].toInstance(mockAccountService))
      .build()

    running(application) {
      val request = FakeRequest(GET, viewOrChangePreviousRegistrationRoute)

      val result = route(application, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe ViewOrChangePreviousRegistrationsMultiplePage.route(waypoints).url
    }
  }



    "must throw an exception when no previousRegistrations returned" in {

    when(mockAccountService.getPreviousRegistrations()(any())).thenReturn(Seq.empty.toFuture)

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
    .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
    .overrides(bind[AccountService].toInstance(mockAccountService))
    .build()

    running(application) {
    val request = FakeRequest(GET, viewOrChangePreviousRegistrationRoute)

      val result = route(application, request).value.failed

      whenReady(result) { exp =>
        exp mustBe a[Exception]
      }
    }
    }


  "must update answers and redirect to the next page for a POST with valid data" in {

    val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

    when(mockAccountService.getPreviousRegistrations()(any())).thenReturn(Seq(previousRegistration).toFuture)
    when(mockSessionRepository.set(any())) thenReturn true.toFuture

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
      .overrides(bind[AccountService].toInstance(mockAccountService))
      .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
      .build()

    running(application) {
      val request = FakeRequest(POST, viewOrChangePreviousRegistrationSubmitRoute)
        .withFormUrlEncodedBody(("value", "true"))


      val result = route(application, request).value

      val expectedAnswers = emptyUserAnswersWithVatInfo
        .set(ViewOrChangePreviousRegistrationPage, true).success.value
        .set(PreviousRegistrationIntermediaryNumberQuery, previousRegistration.intermediaryNumber).success.value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe ViewOrChangePreviousRegistrationPage.navigate(waypoints, emptyUserAnswersWithVatInfo, expectedAnswers).url
      verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
    }
  }

  "must return a Bad Request and errors when invalid data is submitted" in {
    when(mockAccountService.getPreviousRegistrations()(any())).thenReturn(Seq(previousRegistration).toFuture)

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
      .overrides(bind[AccountService].toInstance(mockAccountService))
      .build()


    running(application) {
      val request =
        FakeRequest(POST, viewOrChangePreviousRegistrationSubmitRoute)
          .withFormUrlEncodedBody(("value", ""))

      val boundForm = form.bind(Map("value" -> ""))

      val view = application.injector.instanceOf[ViewOrChangePreviousRegistrationView]

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST
      contentAsString(result) mustBe view(boundForm, waypoints, intermediaryNumber)(request, messages(application)).toString
    }
  }
}
