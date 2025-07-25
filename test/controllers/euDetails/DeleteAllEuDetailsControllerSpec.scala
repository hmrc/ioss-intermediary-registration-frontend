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
import forms.euDetails.DeleteAllEuDetailsFormProvider
import models.euDetails.RegistrationType.{TaxId, VatNumber}
import models.{Country, InternationalAddressWithTradingName, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.euDetails.*
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.euDetails.AllEuDetailsQuery
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.euDetails.DeleteAllEuDetailsView

class DeleteAllEuDetailsControllerSpec extends SpecBase with MockitoSugar {

  private val euTaxId: String = genEuTaxReference.sample.value
  private val euVatNumber: String = arbitraryEuVatNumber.sample.value
  private val countryCode: String = euVatNumber.substring(0, 2)
  
  private val feAddress1: InternationalAddressWithTradingName = arbitraryInternationalAddressWithTradingName.arbitrary.sample.value
  private val feAddress2: InternationalAddressWithTradingName = arbitraryInternationalAddressWithTradingName.arbitrary.sample.value

  private val country1: Country = Country.euCountries.find(_.code == countryCode).head
  private val country2: Country = arbitraryCountry.arbitrary.sample.value

  private val formProvider = new DeleteAllEuDetailsFormProvider()
  private val form = formProvider()

  private lazy val deleteAllEuDetailsRoute = routes.DeleteAllEuDetailsController.onPageLoad(waypoints).url

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasFixedEstablishmentPage, true).success.value
    .set(EuCountryPage(countryIndex(0)), country1).success.value
    .set(FixedEstablishmentAddressPage(countryIndex(0)), feAddress1).success.value
    .set(RegistrationTypePage(countryIndex(0)), VatNumber).success.value
    .set(EuVatNumberPage(countryIndex(0)), euVatNumber).success.value
    .set(AddEuDetailsPage(Some(countryIndex(0))), true).success.value
    .set(EuCountryPage(countryIndex(1)), country2).success.value
    .set(HasFixedEstablishmentPage, true).success.value
    .set(RegistrationTypePage(countryIndex(1)), TaxId).success.value
    .set(EuTaxReferencePage(countryIndex(1)), euTaxId).success.value
    .set(FixedEstablishmentAddressPage(countryIndex(1)), feAddress2).success.value

  "DeleteAllEuDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, deleteAllEuDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeleteAllEuDetailsView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints)(request, messages(application)).toString
      }
    }

    "must remove all EU Details and then redirect to the next page when the user answers Yes" in {

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
          FakeRequest(POST, deleteAllEuDetailsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(DeleteAllEuDetailsPage, true).success.value
          .remove(AllEuDetailsQuery).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` DeleteAllEuDetailsPage.navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must not remove all EU Details and then redirect to the next page when the user answers No" in {

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
          FakeRequest(POST, deleteAllEuDetailsRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(DeleteAllEuDetailsPage, false).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` DeleteAllEuDetailsPage.navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteAllEuDetailsRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DeleteAllEuDetailsView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, deleteAllEuDetailsRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteAllEuDetailsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
