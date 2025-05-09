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
import models.euDetails.RegistrationType.VatNumber
import models.{Country, Index, InternationalAddress, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.JourneyRecoveryPage
import pages.euDetails.*
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.*
import viewmodels.govuk.SummaryListFluency
import views.html.euDetails.CheckEuDetailsAnswersView

class CheckEuDetailsAnswersControllerSpec extends SpecBase with SummaryListFluency {

  private val countryIndex: Index = Index(0)
  private val euVatNumber: String = arbitraryEuVatNumber.sample.value
  private val countryCode: String = euVatNumber.substring(0, 2)
  private val country: Country = Country.euCountries.find(_.code == countryCode).head
  private val feTradingName: String = arbitraryTradingName.arbitrary.sample.value.name
  private val feAddress: InternationalAddress = arbitraryInternationalAddress.arbitrary.sample.value

  private lazy val checkEuDetailsAnswersRoute = routes.CheckEuDetailsAnswersController.onPageLoad(waypoints, countryIndex).url
  private lazy val checkEuDetailsAnswersSubmitRoute = routes.CheckEuDetailsAnswersController.onSubmit(waypoints, countryIndex).url

  private val checkEuDetailsAnswersPage: CheckEuDetailsAnswersPage = CheckEuDetailsAnswersPage(countryIndex)

  private val updatedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(TaxRegisteredInEuPage, true).success.value
    .set(EuCountryPage(countryIndex), country).success.value
    .set(HasFixedEstablishmentPage(countryIndex), true).success.value
    .set(RegistrationTypePage(countryIndex), VatNumber).success.value
    .set(EuVatNumberPage(countryIndex), euVatNumber).success.value
    .set(FixedEstablishmentTradingNamePage(countryIndex), feTradingName).success.value
    .set(FixedEstablishmentAddressPage(countryIndex), feAddress).success.value

  "CheckEuDetailsAnswers Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, checkEuDetailsAnswersRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckEuDetailsAnswersView]

        val summaryList: SummaryList = SummaryListViewModel(
          rows = Seq(
            HasFixedEstablishmentSummary.row(waypoints, updatedAnswers, countryIndex, country, checkEuDetailsAnswersPage),
            RegistrationTypeSummary.row(waypoints, updatedAnswers, countryIndex, checkEuDetailsAnswersPage),
            EuVatNumberSummary.row(waypoints, updatedAnswers, countryIndex, checkEuDetailsAnswersPage),
            EuTaxReferenceSummary.row(waypoints, updatedAnswers, countryIndex, checkEuDetailsAnswersPage),
            FixedEstablishmentTradingNameSummary.row(waypoints, updatedAnswers, countryIndex, checkEuDetailsAnswersPage),
            FixedEstablishmentAddressSummary.row(waypoints, updatedAnswers, countryIndex, checkEuDetailsAnswersPage)
          ).flatten
        )

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(waypoints, countryIndex, country, summaryList)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(updatedAnswers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, checkEuDetailsAnswersSubmitRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` CheckEuDetailsAnswersPage(countryIndex)
          .navigate(waypoints, updatedAnswers, updatedAnswers).url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, checkEuDetailsAnswersRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, checkEuDetailsAnswersSubmitRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
