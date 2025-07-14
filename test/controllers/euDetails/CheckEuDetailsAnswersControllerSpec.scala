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
import models.{Country, InternationalAddressWithTradingName, NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.euDetails.*
import pages.{JourneyRecoveryPage, Waypoint, Waypoints}
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

  private val euVatNumber: String = arbitraryEuVatNumber.sample.value
  private val countryCode: String = euVatNumber.substring(0, 2)
  private val country: Country = Country.euCountries.find(_.code == countryCode).head
  private val feAddress: InternationalAddressWithTradingName = arbitraryInternationalAddressWithTradingName.arbitrary.sample.value

  private lazy val checkEuDetailsAnswersRoute = routes.CheckEuDetailsAnswersController.onPageLoad(waypoints, countryIndex(0)).url

  private def checkEuDetailsAnswersSubmitRoute(incompletePromptShown: Boolean = false) =
    routes.CheckEuDetailsAnswersController.onSubmit(waypoints, countryIndex(0), incompletePromptShown = incompletePromptShown).url

  private val checkEuDetailsAnswersPage: CheckEuDetailsAnswersPage = CheckEuDetailsAnswersPage(countryIndex(0))

  private val answers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasFixedEstablishmentPage(), true).success.value
    .set(EuCountryPage(countryIndex(0)), country).success.value
    .set(FixedEstablishmentAddressPage(countryIndex(0)), feAddress).success.value
    .set(RegistrationTypePage(countryIndex(0)), VatNumber).success.value
    .set(EuVatNumberPage(countryIndex(0)), euVatNumber).success.value

  "CheckEuDetailsAnswers Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, checkEuDetailsAnswersRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckEuDetailsAnswersView]

        val summaryList: SummaryList = SummaryListViewModel(
          rows = Seq(
            EuCountrySummary.row(waypoints, answers, countryIndex(0), checkEuDetailsAnswersPage),
            FixedEstablishmentAddressSummary.row(waypoints, answers, countryIndex(0), checkEuDetailsAnswersPage),
            RegistrationTypeSummary.row(waypoints, answers, countryIndex(0), checkEuDetailsAnswersPage),
            EuVatNumberSummary.row(waypoints, answers, countryIndex(0), checkEuDetailsAnswersPage),
            EuTaxReferenceSummary.row(waypoints, answers, countryIndex(0), checkEuDetailsAnswersPage),
          ).flatten
        )

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(waypoints, countryIndex(0), country, summaryList, incomplete = false)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when there are incomplete answers" in {

      val incompleteAnswers: UserAnswers = answers
        .remove(EuVatNumberPage(countryIndex(0))).success.value

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, checkEuDetailsAnswersRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckEuDetailsAnswersView]

        val summaryList: SummaryList = SummaryListViewModel(
          rows = Seq(
            HasFixedEstablishmentSummary.row(waypoints, incompleteAnswers, checkEuDetailsAnswersPage),
            RegistrationTypeSummary.row(waypoints, incompleteAnswers, countryIndex(0), checkEuDetailsAnswersPage),
            EuVatNumberSummary.row(waypoints, incompleteAnswers, countryIndex(0), checkEuDetailsAnswersPage),
            EuTaxReferenceSummary.row(waypoints, incompleteAnswers, countryIndex(0), checkEuDetailsAnswersPage),
            FixedEstablishmentAddressSummary.row(waypoints, incompleteAnswers, countryIndex(0), checkEuDetailsAnswersPage)
          ).flatten
        )

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(waypoints, countryIndex(0), country, summaryList, incomplete = true)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, checkEuDetailsAnswersSubmitRoute())

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` CheckEuDetailsAnswersPage(countryIndex(0))
          .navigate(waypoints, answers, answers).url
      }
    }

    "must redirect to the corresponding incomplete answer page for a POST when there are incomplete answers" in {

      val incompleteAnswers: UserAnswers = answers
        .remove(EuVatNumberPage(countryIndex(0))).success.value

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {

        val request = FakeRequest(POST, checkEuDetailsAnswersSubmitRoute(incompletePromptShown = true))

        val result = route(application, request).value

        val updatedWaypoints: Waypoints = waypoints.setNextWaypoint(Waypoint(checkEuDetailsAnswersPage, NormalMode, checkEuDetailsAnswersPage.urlFragment))

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` EuVatNumberPage(countryIndex(0)).route(updatedWaypoints).url
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
        val request = FakeRequest(POST, checkEuDetailsAnswersSubmitRoute())

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
