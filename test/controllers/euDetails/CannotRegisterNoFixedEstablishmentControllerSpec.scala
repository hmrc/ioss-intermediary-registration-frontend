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
import models.{Country, Index, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.JourneyRecoveryPage
import pages.euDetails.{CannotRegisterNoFixedEstablishmentPage, EuCountryPage, HasFixedEstablishmentPage, TaxRegisteredInEuPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.euDetails.EuDetailsQuery
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.euDetails.CannotRegisterNoFixedEstablishmentView

class CannotRegisterNoFixedEstablishmentControllerSpec extends SpecBase {
  
  private val countryIndex: Index = Index(0)
  private val country: Country = arbitraryCountry.arbitrary.sample.value

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(TaxRegisteredInEuPage, true).success.value
    .set(EuCountryPage(countryIndex), country).success.value

  private lazy val noFixedEstablishmentRoute: String = routes.CannotRegisterNoFixedEstablishmentController.onPageLoad(waypoints, countryIndex).url

  "CannotRegisterNoFixedEstablishment Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, noFixedEstablishmentRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CannotRegisterNoFixedEstablishmentView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(waypoints, countryIndex)(request, messages(application)).toString
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
          .set(HasFixedEstablishmentPage(countryIndex), false).success.value
          .remove(EuDetailsQuery(countryIndex)).success.value
        
        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` CannotRegisterNoFixedEstablishmentPage(countryIndex).navigate(waypoints, updatedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    // TODO -> Delete multiple countries

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
