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
import models.domain.VatCustomerInfo
import models.requests.AuthenticatedDataRequest
import models.{CheckMode, UserAnswers}
import pages.tradingNames.{HasTradingNamePage, TradingNamePage}
import pages.{CheckYourAnswersPage, EmptyWaypoints, JourneyRecoveryPage, Waypoint, Waypoints}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import testutils.CheckYourAnswersSummaries.{getCYANonNiVatDetailsSummaryList, getCYASummaryList, getCYAVatDetailsSummaryList}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency {

  private val waypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))

  private implicit val request: AuthenticatedDataRequest[AnyContent] =
    AuthenticatedDataRequest(fakeRequest, testCredentials, vrn, testEnrolments, emptyUserAnswers, None, 0, None, None)

  private lazy val routeCheckYourAnswersControllerGET: String = routes.CheckYourAnswersController.onPageLoad().url

  private def routeCheckYourAnswersControllerPOST(incompletePrompt: Boolean): String = {
    routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt).url
  }

  "Check Your Answers Controller" - {

    ".onPageLoad" - {

      "must return OK and the correct view for a GET" - {

        val niVatInfo: VatCustomerInfo = vatCustomerInfo
          .copy(desAddress = vatCustomerInfo.desAddress
            .copy(postCode = Some("BT12 3CD")))

        val completedUserAnswersWithNiVatInfo: UserAnswers = completeUserAnswersWithVatInfo.copy(vatInfo = Some(niVatInfo))

        "with completed data present" in {
          
          val application = applicationBuilder(userAnswers = Some(completedUserAnswersWithNiVatInfo)).build()

          running(application) {

            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routeCheckYourAnswersControllerGET)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CheckYourAnswersView]

            val vatDetailsList: SummaryList = SummaryListViewModel(
              rows = getCYAVatDetailsSummaryList(completedUserAnswersWithNiVatInfo)
            )

            val list: SummaryList = SummaryListViewModel(
              rows = getCYASummaryList(waypoints, completedUserAnswersWithNiVatInfo, CheckYourAnswersPage)
            )

            status(result) `mustBe` OK
            contentAsString(result) `mustBe` view(waypoints, vatDetailsList, list, isValid = true)(request, messages(application)).toString
          }
        }

        "with completed data present for non-NI VAT details" in {

          val nonNiVatInfo: VatCustomerInfo = vatCustomerInfo
            .copy(desAddress = vatCustomerInfo.desAddress
              .copy(postCode = Some("AB12 3CD")))

          val completeUserAnswersWithNonNiVatInfo: UserAnswers = completeUserAnswersWithVatInfo.copy(vatInfo = Some(nonNiVatInfo))
          
          val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithNonNiVatInfo)).build()

          running(application) {

            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routeCheckYourAnswersControllerGET)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CheckYourAnswersView]

            val vatDetailsList: SummaryList = SummaryListViewModel(
              rows = getCYANonNiVatDetailsSummaryList(completeUserAnswersWithNonNiVatInfo)
            )

            val list: SummaryList = SummaryListViewModel(
              rows = getCYASummaryList(waypoints, completeUserAnswersWithNonNiVatInfo, CheckYourAnswersPage)
            )

            status(result) `mustBe` OK
            contentAsString(result) `mustBe` view(waypoints, vatDetailsList, list, isValid = true)(request, messages(application)).toString
          }
        }

        "with incomplete data" in {

          val missingAnswers: UserAnswers = completedUserAnswersWithNiVatInfo
            .remove(TradingNamePage(countryIndex(0))).success.value

          val application = applicationBuilder(userAnswers = Some(missingAnswers)).build()

          running(application) {

            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routeCheckYourAnswersControllerGET)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CheckYourAnswersView]

            val vatDetailsList: SummaryList = SummaryListViewModel(
              rows = getCYAVatDetailsSummaryList(completedUserAnswersWithNiVatInfo)
            )

            val list: SummaryList = SummaryListViewModel(
              rows = getCYASummaryList(waypoints, missingAnswers, CheckYourAnswersPage)
            )

            status(result) `mustBe` OK
            contentAsString(result) `mustBe` view(waypoints, vatDetailsList, list, isValid = false)(request, messages(application)).toString
          }
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routeCheckYourAnswersControllerGET)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
        }
      }
    }

    ".onSubmit" - {

      "must submit completed answers" in {

        val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo)).build()

        running(application) {

          val request = FakeRequest(POST, routeCheckYourAnswersControllerPOST(incompletePrompt = false))

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` CheckYourAnswersPage.navigate(waypoints, completeUserAnswersWithVatInfo, completeUserAnswersWithVatInfo).url
        }
      }

      "must redirect to the correct page when there is incomplete data" in {

        val incompleteAnswers: UserAnswers = completeUserAnswersWithVatInfo
          .remove(TradingNamePage(countryIndex(0))).success.value

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

        running(application) {

          val request = FakeRequest(POST, routeCheckYourAnswersControllerPOST(incompletePrompt = true))

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` HasTradingNamePage.route(waypoints).url
        }
      }
    }
  }
}
