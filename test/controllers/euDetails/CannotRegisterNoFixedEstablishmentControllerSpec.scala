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
import models.euDetails.RegistrationType.TaxId
import models.{Country, InternationalAddress, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.JourneyRecoveryPage
import pages.euDetails.*
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.euDetails.EuDetailsQuery
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.euDetails.CannotRegisterNoFixedEstablishmentView

class CannotRegisterNoFixedEstablishmentControllerSpec extends SpecBase {
  
  private val country1: Country = arbitraryCountry.arbitrary.sample.value
  private val country2: Country = arbitraryCountry.arbitrary.sample.value
  private val country3: Country = arbitraryCountry.arbitrary.sample.value

  private val euTaxId1: String = genEuTaxReference.sample.value
  private val euTaxId2: String = genEuTaxReference.sample.value

  private val feTradingName1: String = arbitraryTradingName.arbitrary.sample.value.name
  private val feAddress1: InternationalAddress = arbitraryInternationalAddress.arbitrary.sample.value
  private val feTradingName2: String = arbitraryTradingName.arbitrary.sample.value.name
  private val feAddress2: InternationalAddress = arbitraryInternationalAddress.arbitrary.sample.value

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(TaxRegisteredInEuPage, true).success.value
    .set(EuCountryPage(countryIndex(0)), country1).success.value

  private lazy val noFixedEstablishmentRoute: String = routes.CannotRegisterNoFixedEstablishmentController.onPageLoad(waypoints, countryIndex(0)).url

  "CannotRegisterNoFixedEstablishment Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, noFixedEstablishmentRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CannotRegisterNoFixedEstablishmentView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(waypoints, countryIndex(0))(request, messages(application)).toString
      }
    }

    "must delete the country and redirect to the correct page when only one country is present" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application = applicationBuilder(userAnswers = Some(updatedAnswers))
        .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, noFixedEstablishmentRoute)

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = updatedAnswers
          .set(HasFixedEstablishmentPage(countryIndex(0)), false).success.value
          .remove(EuDetailsQuery(countryIndex(0))).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` CannotRegisterNoFixedEstablishmentPage(countryIndex(0)).navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must delete the country and redirect to the correct page when there are multiple countries present" in {

      lazy val noFixedEstablishmentRoute: String = routes.CannotRegisterNoFixedEstablishmentController.onPageLoad(waypoints, countryIndex(2)).url

      val answers: UserAnswers = updatedAnswers
        .set(HasFixedEstablishmentPage(countryIndex(0)), true).success.value
        .set(RegistrationTypePage(countryIndex(0)), TaxId).success.value
        .set(EuTaxReferencePage(countryIndex(0)), euTaxId1).success.value
        .set(FixedEstablishmentTradingNamePage(countryIndex(0)), feTradingName1).success.value
        .set(FixedEstablishmentAddressPage(countryIndex(0)), feAddress1).success.value
        .set(AddEuDetailsPage(Some(countryIndex(0))), true).success.value
        .set(EuCountryPage(countryIndex(1)), country2).success.value
        .set(HasFixedEstablishmentPage(countryIndex(1)), true).success.value
        .set(RegistrationTypePage(countryIndex(1)), TaxId).success.value
        .set(EuTaxReferencePage(countryIndex(1)), euTaxId2).success.value
        .set(FixedEstablishmentTradingNamePage(countryIndex(1)), feTradingName2).success.value
        .set(FixedEstablishmentAddressPage(countryIndex(1)), feAddress2).success.value
        .set(AddEuDetailsPage(Some(countryIndex(1))), true).success.value
        .set(EuCountryPage(countryIndex(2)), country3).success.value

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, noFixedEstablishmentRoute)

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = answers
          .set(HasFixedEstablishmentPage(countryIndex(2)), false).success.value
          .remove(EuDetailsQuery(countryIndex(2))).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` CannotRegisterNoFixedEstablishmentPage(countryIndex(2)).navigate(waypoints, answers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, noFixedEstablishmentRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
