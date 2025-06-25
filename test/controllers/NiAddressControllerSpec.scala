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
import forms.NiAddressFormProvider
import models.{UkAddress, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{CannotRegisterNotNiBasedBusinessPage, JourneyRecoveryPage, NiAddressPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.NiAddressView

class NiAddressControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new NiAddressFormProvider()
  private val form: Form[UkAddress] = formProvider()

  private lazy val niAddressRoute: String = routes.NiAddressController.onPageLoad(waypoints).url

  private val ukAddress: UkAddress = UkAddress(
    line1 = "line-1",
    line2 = None,
    townOrCity = "town-or-city",
    county = None,
    postCode = "BT1 2CD",
  )

  "NiAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, niAddressRoute)

        val view = application.injector.instanceOf[NiAddressView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
        .set(NiAddressPage, ukAddress).success.value

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, niAddressRoute)

        val view = application.injector.instanceOf[NiAddressView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill(ukAddress), waypoints)(request, messages(application)).toString
      }
    }

    "must save the answers and redirect to the next page when valid data is submitted and the postcode area matches 'BT'" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, niAddressRoute)
            .withFormUrlEncodedBody(
              ("line1", ukAddress.line1),
              ("townOrCity", ukAddress.townOrCity),
              ("postCode", ukAddress.postCode)
            )

        val result = route(application, request).value

        val expectedAnswers = emptyUserAnswersWithVatInfo
          .set(NiAddressPage, ukAddress).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` NiAddressPage.navigate(waypoints, emptyUserAnswersWithVatInfo, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must remove the answers and redirect to the next page when valid data is submitted and the postcode area does not match 'BT'" in {

      val nonNiAddress: UkAddress = ukAddress.copy(postCode = "AB12 3CD")

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, niAddressRoute)
            .withFormUrlEncodedBody(
              ("line1", nonNiAddress.line1),
              ("townOrCity", nonNiAddress.townOrCity),
              ("postCode", nonNiAddress.postCode)
            )

        val result = route(application, request).value

        val expectedAnswers = emptyUserAnswersWithVatInfo

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` CannotRegisterNotNiBasedBusinessPage.route(waypoints).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo)).build()

      running(application) {
        val request =
          FakeRequest(POST, niAddressRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[NiAddressView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, niAddressRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, niAddressRoute)
            .withFormUrlEncodedBody(("line1", "value 1"), ("line2", "value 2"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
