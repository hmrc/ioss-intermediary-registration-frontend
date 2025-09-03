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
import models.audit.{IntermediaryRegistrationAuditModel, RegistrationAuditType, SubmissionResult}
import models.domain.VatCustomerInfo
import models.requests.AuthenticatedDataRequest
import models.responses.InternalServerError as ServerError
import models.responses.etmp.EtmpEnrolmentResponse
import models.{CheckMode, Index, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.euDetails.{EuCountryPage, HasFixedEstablishmentPage}
import pages.tradingNames.TradingNamePage
import pages.{ApplicationCompletePage, CheckYourAnswersPage, EmptyWaypoints, ErrorSubmittingRegistrationPage, JourneyRecoveryPage, Waypoint, Waypoints}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.etmp.EtmpEnrolmentResponseQuery
import queries.euDetails.EuDetailsQuery
import repositories.AuthenticatedUserAnswersRepository
import services.{AuditService, RegistrationService, SaveForLaterService}
import testutils.CheckYourAnswersSummaries.{getCYANonNiVatDetailsSummaryList, getCYASummaryList, getCYAVatDetailsSummaryList}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.FutureSyntax.FutureOps
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

class CheckYourAnswersControllerSpec extends SpecBase with SummaryListFluency with BeforeAndAfterEach {

  private val mockRegistrationService: RegistrationService = mock[RegistrationService]
  private val mockSaveForLaterService: SaveForLaterService = mock[SaveForLaterService]
  private val mockAuditService: AuditService = mock[AuditService]

  private val waypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, CheckMode, CheckYourAnswersPage.urlFragment))
  private val country = arbitraryCountry.arbitrary.sample.value

  private implicit val request: AuthenticatedDataRequest[AnyContent] =
    AuthenticatedDataRequest(fakeRequest, testCredentials, vrn, testEnrolments, emptyUserAnswers, None, 0, None, None)

  private lazy val routeCheckYourAnswersControllerGET: String = routes.CheckYourAnswersController.onPageLoad().url

  private def routeCheckYourAnswersControllerPOST(incompletePrompt: Boolean): String = {
    routes.CheckYourAnswersController.onSubmit(waypoints, incompletePrompt).url
  }

  override def beforeEach(): Unit = {
    reset(mockRegistrationService)
    reset(mockSaveForLaterService)
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
            contentAsString(result) `mustBe` view(waypoints, vatDetailsList, list, isValid = false)(request, messages(application)).toString
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

        "must throw an exception when VAT information missing" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {

            val request = FakeRequest(GET, routeCheckYourAnswersControllerGET)

            val result = route(application, request).value

            whenReady(result.failed) { exp =>
              exp `mustBe` a[IllegalStateException]
              exp.getMessage `mustBe` "VAT information missing"
            }
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

      "must save the answer, audit event then redirect to the correct page when a successful registration request returns a valid response body" in {

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

        val etmpEnrolmentResponse: EtmpEnrolmentResponse = EtmpEnrolmentResponse(intermediary = "123456789")

        when(mockSessionRepository.set(any())) thenReturn true.toFuture
        when(mockRegistrationService.createRegistration(any(), any())(any())) thenReturn Right(etmpEnrolmentResponse).toFuture
        doNothing().when(mockAuditService).audit(any())(any(), any())

        val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .overrides(bind[AuditService].toInstance(mockAuditService))
          .build()

        running(application) {
          val request = FakeRequest(POST, routeCheckYourAnswersControllerPOST(incompletePrompt = false))

          val expectedAuditEvent = IntermediaryRegistrationAuditModel.build(
            RegistrationAuditType.CreateIntermediaryRegistration,
            completeUserAnswersWithVatInfo,
            Some(etmpEnrolmentResponse),
            SubmissionResult.Success
          )

          val result = route(application, request).value

          val expectedAnswers = completeUserAnswersWithVatInfo
            .set(EtmpEnrolmentResponseQuery, etmpEnrolmentResponse).success.value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` ApplicationCompletePage.route(waypoints).url
          verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
        }
      }

      "must save the answers, audit event and redirect the Error Submitting Registration page when back end returns any other Error Response" in {
        
        when(mockRegistrationService.createRegistration(any(), any())(any())) thenReturn Left(ServerError).toFuture
          Redirect(ErrorSubmittingRegistrationPage.route(waypoints).url).toFuture

        when(mockSaveForLaterService.saveUserAnswers(any(), any(), any())(any(), any(), any())) thenReturn
          Redirect(ErrorSubmittingRegistrationPage.route(waypoints).url).toFuture

        doNothing().when(mockAuditService).audit(any())(any(), any())

        val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo))
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
          .overrides(bind[AuditService].toInstance(mockAuditService))
          .build()

        running(application) {
          val request = FakeRequest(POST, routeCheckYourAnswersControllerPOST(incompletePrompt = false))

          val expectedAuditEvent = IntermediaryRegistrationAuditModel.build(
            RegistrationAuditType.CreateIntermediaryRegistration,
            completeUserAnswersWithVatInfo,
            None,
            SubmissionResult.Failure
          )

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` ErrorSubmittingRegistrationPage.route(waypoints).url
          verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
          verify(mockSaveForLaterService, times(1)).saveUserAnswers(any(), any(), any())(any(), any(), any())
        }
      }

      "when the user has not answered all necessary data" - {

        "the user is redirected when the incomplete prompt is shown" - {

          "to Tax Registered In EU when it has a 'yes' answer but all countries were removed" in {
            val answers = completeUserAnswersWithVatInfo
              .set(HasFixedEstablishmentPage, true).success.value
              .set(EuCountryPage(Index(0)), country).success.value
              .remove(EuDetailsQuery(Index(0))).success.value

            val application = applicationBuilder(userAnswers = Some(answers)).build()

            running(application) {
              val request = FakeRequest(POST, routeCheckYourAnswersControllerPOST(incompletePrompt = true))
              val result = route(application, request).value

              status(result) `mustBe` SEE_OTHER
              redirectLocation(result).value `mustBe` controllers.euDetails.routes.HasFixedEstablishmentController.onPageLoad(waypoints).url
            }
          }
        }
      }
    }
  }
}
