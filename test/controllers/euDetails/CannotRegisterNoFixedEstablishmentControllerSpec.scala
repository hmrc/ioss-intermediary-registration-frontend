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
import models.{Country, Index, InternationalAddress, UserAnswers}
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

  private val countryIndex1: Index = Index(0)
  private val country1: Country = arbitraryCountry.arbitrary.sample.value

  private val countryIndex2: Index = Index(1)
  private val country2: Country = arbitraryCountry.arbitrary.sample.value

  private val countryIndex3: Index = Index(2)
  private val country3: Country = arbitraryCountry.arbitrary.sample.value

  private val euTaxId1: String = arbitraryEuTaxReference.sample.value
  private val euTaxId2: String = arbitraryEuTaxReference.sample.value

  private val feTradingName1: String = arbitraryTradingName.arbitrary.sample.value.name
  private val feAddress1: InternationalAddress = arbitraryInternationalAddress.arbitrary.sample.value
  private val feTradingName2: String = arbitraryTradingName.arbitrary.sample.value.name
  private val feAddress2: InternationalAddress = arbitraryInternationalAddress.arbitrary.sample.value

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(TaxRegisteredInEuPage, true).success.value
    .set(EuCountryPage(countryIndex1), country1).success.value

  private lazy val noFixedEstablishmentRoute: String = routes.CannotRegisterNoFixedEstablishmentController.onPageLoad(waypoints, countryIndex1).url

  "CannotRegisterNoFixedEstablishment Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, noFixedEstablishmentRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CannotRegisterNoFixedEstablishmentView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(waypoints, countryIndex1)(request, messages(application)).toString
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
          .set(HasFixedEstablishmentPage(countryIndex1), false).success.value
          .remove(EuDetailsQuery(countryIndex1)).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` CannotRegisterNoFixedEstablishmentPage(countryIndex1).navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must delete the country and redirect to the correct page when there are multiple countries present" in {

      lazy val noFixedEstablishmentRoute: String = routes.CannotRegisterNoFixedEstablishmentController.onPageLoad(waypoints, countryIndex3).url

      val answers: UserAnswers = updatedAnswers
        .set(HasFixedEstablishmentPage(countryIndex1), true).success.value
        .set(RegistrationTypePage(countryIndex1), TaxId).success.value
        .set(EuTaxReferencePage(countryIndex1), euTaxId1).success.value
        .set(FixedEstablishmentTradingNamePage(countryIndex1), feTradingName1).success.value
        .set(FixedEstablishmentAddressPage(countryIndex1), feAddress1).success.value
        .set(AddEuDetailsPage(Some(countryIndex1)), true).success.value
        .set(EuCountryPage(countryIndex2), country2).success.value
        .set(HasFixedEstablishmentPage(countryIndex2), true).success.value
        .set(RegistrationTypePage(countryIndex2), TaxId).success.value
        .set(EuTaxReferencePage(countryIndex2), euTaxId2).success.value
        .set(FixedEstablishmentTradingNamePage(countryIndex2), feTradingName2).success.value
        .set(FixedEstablishmentAddressPage(countryIndex2), feAddress2).success.value
        .set(AddEuDetailsPage(Some(countryIndex2)), true).success.value
        .set(EuCountryPage(countryIndex3), country3).success.value

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(POST, noFixedEstablishmentRoute)

        val result = route(application, request).value

        val expectedAnswers: UserAnswers = answers
          .set(HasFixedEstablishmentPage(countryIndex3), false).success.value
          .remove(EuDetailsQuery(countryIndex3)).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` CannotRegisterNoFixedEstablishmentPage(countryIndex3).navigate(waypoints, answers, expectedAnswers).url
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
