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
import forms.euDetails.FixedEstablishmentAddressFormProvider
import models.euDetails.RegistrationType.TaxId
import models.{Country, Index, InternationalAddress, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.euDetails.*
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.euDetails.FixedEstablishmentAddressView

class FixedEstablishmentAddressControllerSpec extends SpecBase with MockitoSugar {

  private val countryIndex: Index = Index(0)
  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val euTaxReference: String = genEuTaxReference.sample.value
  private val tradingName: String = genFixedEstablishmentTradingName.sample.value
  private val feAddress: InternationalAddress = InternationalAddress(
    line1 = "line-1",
    line2 = None,
    townOrCity = "town-or-city",
    stateOrRegion = None,
    postCode = None,
    country = country
  )

  private val formProvider = new FixedEstablishmentAddressFormProvider()
  private val form: Form[InternationalAddress] = formProvider(country)

  private lazy val fixedEstablishmentAddressRoute: String =
    routes.FixedEstablishmentAddressController.onPageLoad(waypoints, countryIndex).url

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(TaxRegisteredInEuPage, true).success.value
    .set(EuCountryPage(countryIndex), country).success.value
    .set(HasFixedEstablishmentPage(countryIndex), true).success.value
    .set(RegistrationTypePage(countryIndex), TaxId).success.value
    .set(EuTaxReferencePage(countryIndex), euTaxReference).success.value
    .set(FixedEstablishmentTradingNamePage(countryIndex), tradingName).success.value

  "FixedEstablishmentAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, fixedEstablishmentAddressRoute)

        val view = application.injector.instanceOf[FixedEstablishmentAddressView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, countryIndex, country)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = updatedAnswers.set(FixedEstablishmentAddressPage(countryIndex), feAddress).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, fixedEstablishmentAddressRoute)

        val view = application.injector.instanceOf[FixedEstablishmentAddressView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill(feAddress), waypoints, countryIndex, country)(request, messages(application)).toString
      }
    }

    "must save the answers and redirect to the next page when valid data is submitted" in {

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
          FakeRequest(POST, fixedEstablishmentAddressRoute)
            .withFormUrlEncodedBody(
              ("line1", feAddress.line1), ("townOrCity", feAddress.townOrCity), ("country", feAddress.country.name)
            )

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(FixedEstablishmentAddressPage(countryIndex), feAddress).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` FixedEstablishmentAddressPage(countryIndex)
          .navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, fixedEstablishmentAddressRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[FixedEstablishmentAddressView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints, countryIndex, country)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, fixedEstablishmentAddressRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, fixedEstablishmentAddressRoute)
            .withFormUrlEncodedBody(("line1", "value 1"), ("line2", "value 2"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
