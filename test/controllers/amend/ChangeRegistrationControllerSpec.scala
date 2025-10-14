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

package controllers.amend

import base.SpecBase
import models.audit.{IntermediaryAmendRegistrationAuditModel, RegistrationAuditType, SubmissionResult}
import models.domain.VatCustomerInfo
import models.requests.{AuthenticatedDataRequest, AuthenticatedMandatoryIntermediaryRequest}
import models.responses.InternalServerError
import models.{BankDetails, Bic, CheckMode, ContactDetails, DesAddress, Iban, Index, TradingName, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{doNothing, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{BankDetailsPage, ContactDetailsPage, EmptyWaypoints, Waypoint, Waypoints}
import pages.amend.ChangeRegistrationPage
import pages.euDetails.HasFixedEstablishmentPage
import pages.filters.RegisteredForIossIntermediaryInEuPage
import pages.previousIntermediaryRegistrations.HasPreviouslyRegisteredAsIntermediaryPage
import pages.tradingNames.{HasTradingNamePage, TradingNamePage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import services.{AuditService, RegistrationService}
import testutils.RegistrationData.amendRegistrationResponse
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.checkAnswers.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediarySummary, PreviousIntermediaryRegistrationsSummary}
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, ContactDetailsSummary, NiAddressSummary, VatRegistrationDetailsSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.ChangeRegistrationView

import java.time.{Instant, LocalDate}

class ChangeRegistrationControllerSpec extends SpecBase with SummaryListFluency {

  private val waypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
  private val amendYourAnswersPage = ChangeRegistrationPage
  private val previousIntermediaryRegistration = arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value
  private val mockAuditService: AuditService = mock[AuditService]
  private val mockRegistrationService: RegistrationService = mock[RegistrationService]

  override val iban: Iban = Iban("GB33BUKB202015555555555").toOption.get
  override val bic: Bic = Bic("BARCGB22456").get

  override val vatCustomerInfo: VatCustomerInfo =
    VatCustomerInfo(
      registrationDate = LocalDate.now(),
      desAddress = DesAddress(
        line1 = "1818 East Tusculum Street",
        line2 = Some("Phil Tow"),
        line3 = None, line4 = None, line5 = None,
        postCode = Some("BT4 2XW"),
        countryCode = "EL"),
      organisationName = Some("Company name"),
      individualName = None,
      singleMarketIndicator = true,
      deregistrationDecisionDate = None
    )
  override  def basicUserAnswersWithVatInfo: UserAnswers =
    UserAnswers(id = "12345-credId", vatInfo = Some(vatCustomerInfo), lastUpdated = Instant.now())

  override def completeUserAnswersWithVatInfo: UserAnswers =
    basicUserAnswersWithVatInfo
      .set(RegisteredForIossIntermediaryInEuPage, false).get
      .set(HasTradingNamePage, true).get
      .set(TradingNamePage(Index(0)), TradingName("Chartoff Winkler and Co. Robert Rocky Balboa Robert Balboa")).get
      .set(HasPreviouslyRegisteredAsIntermediaryPage, false).get
      .set(HasFixedEstablishmentPage, false).get
      .set(ContactDetailsPage, ContactDetails("Rocky Balboa", "028 123 4567", "rocky.balboa@chartoffwinkler.co.uk")).get
      .set(BankDetailsPage, BankDetails("Chartoff Winkler and Co.", Some(bic), iban)).get


  "ChangeRegistration Controller" - {

    ".onPageLoad" - {

      "must return OK and the correct view for a GET" - {

        "with completed data present" in {

          val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo)).build()

          running(application) {

            val request = FakeRequest(GET, controllers.amend.routes.ChangeRegistrationController.onPageLoad().url)
              .withSession("intermediaryNumber" -> "IN1234567890")
            implicit val msgs: Messages = messages(application)
            val result = route(application, request).value

            val view = application.injector.instanceOf[ChangeRegistrationView]

            val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(completeUserAnswersWithVatInfo))

            val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(completeUserAnswersWithVatInfo))

            val hasMultipleIntermediaryEnrolments: Boolean = false

            status(result) mustBe OK
            contentAsString(result) mustBe view(waypoints, vatInfoList, list, intermediaryNumber, hasMultipleIntermediaryEnrolments, isValid = true)(request, messages(application)).toString
          }
        }

        "with incomplete data" in {
          val missingAnswers: UserAnswers = completeUserAnswersWithVatInfo
            .remove(TradingNamePage(countryIndex(0))).success.value

          val application = applicationBuilder(userAnswers = Some(missingAnswers)).build()

          running(application) {

            val request = FakeRequest(GET, controllers.amend.routes.ChangeRegistrationController.onPageLoad().url)
              .withSession("intermediaryNumber" -> "IN1234567890")
            implicit val msgs: Messages = messages(application)
            val result = route(application, request).value

            val view = application.injector.instanceOf[ChangeRegistrationView]

            val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(completeUserAnswersWithVatInfo))

            val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(missingAnswers))

            val hasMultipleIntermediaryEnrolments: Boolean = false

            status(result) mustBe OK
            contentAsString(result) mustBe view(waypoints, vatInfoList, list, intermediaryNumber, hasMultipleIntermediaryEnrolments, isValid = false)(request, messages(application)).toString
          }
        }
      }
    }

    ".onSubmit" - {

      "must audit success event then redirect when registration succeeds" in {

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val userAnswers = completeUserAnswersWithVatInfo

        when(mockSessionRepository.set(any())) thenReturn true.toFuture
        when(mockRegistrationService.amendRegistration(any(), any(), any(), any(), any(), any())(any())) thenReturn Right(amendRegistrationResponse).toFuture
        doNothing().when(mockAuditService).audit(any())(any(), any())

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .overrides(bind[AuditService].toInstance(mockAuditService))
          .build()

        running(application) {

          val request = FakeRequest(POST, controllers.amend.routes.ChangeRegistrationController.onSubmit(EmptyWaypoints, incompletePrompt = false).url)

          val result = route(application, request).value

          implicit val authenticatedDataRequest: AuthenticatedDataRequest[_] =
            AuthenticatedDataRequest(
              request = request,
              credentials = testCredentials,
              vrn = vrn,
              enrolments = Enrolments(Set.empty),
              userAnswers = userAnswers,
              iossNumber = Some(iossNumber),
              numberOfIossRegistrations = 1,
              latestIossRegistration = None,
              latestOssRegistration = None,
              intermediaryNumber = Some(intermediaryNumber),
              registrationWrapper = Some(registrationWrapper)
            )

          implicit val dataRequest: AuthenticatedMandatoryIntermediaryRequest[_] = {
            AuthenticatedMandatoryIntermediaryRequest(
              request = authenticatedDataRequest,
              credentials = testCredentials,
              vrn = vrn,
              enrolments = Enrolments(Set.empty),
              userAnswers = userAnswers,
              numberOfIossRegistrations = 1,
              latestIossRegistration = None,
              latestOssRegistration = None,
              intermediaryNumber = intermediaryNumber,
              registrationWrapper = registrationWrapper
            )
          }

          val expectedAuditEvent = IntermediaryAmendRegistrationAuditModel.build(
            RegistrationAuditType.AmendRegistration,
            userAnswers,
            Some(amendRegistrationResponse),
            SubmissionResult.Success
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.amend.routes.AmendCompleteController.onPageLoad().url
          verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
        }
      }

      "must audit failure event and throw exception when registration fails" in {

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val userAnswers = completeUserAnswersWithVatInfo

        when(mockSessionRepository.set(any())) thenReturn true.toFuture
        when(mockRegistrationService.amendRegistration(any(), any(), any(), any(), any(), any())(any())) thenReturn Left(InternalServerError).toFuture
        doNothing().when(mockAuditService).audit(any())(any(), any())

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .overrides(bind[AuditService].toInstance(mockAuditService))
          .build()

        running(application) {

          val request = FakeRequest(POST, controllers.amend.routes.ChangeRegistrationController.onSubmit(EmptyWaypoints, incompletePrompt = false).url)

          val result = route(application, request).value

          implicit val authenticatedDataRequest: AuthenticatedDataRequest[_] =
            AuthenticatedDataRequest(
              request = request,
              credentials = testCredentials,
              vrn = vrn,
              enrolments = Enrolments(Set.empty),
              userAnswers = userAnswers,
              iossNumber = Some(iossNumber),
              numberOfIossRegistrations = 1,
              latestIossRegistration = None,
              latestOssRegistration = None,
              intermediaryNumber = Some(intermediaryNumber),
              registrationWrapper = Some(registrationWrapper)
            )

          implicit val dataRequest: AuthenticatedMandatoryIntermediaryRequest[_] = {
            AuthenticatedMandatoryIntermediaryRequest(
              request = authenticatedDataRequest,
              credentials = testCredentials,
              vrn = vrn,
              enrolments = Enrolments(Set.empty),
              userAnswers = userAnswers,
              numberOfIossRegistrations = 1,
              latestIossRegistration = None,
              latestOssRegistration = None,
              intermediaryNumber = intermediaryNumber,
              registrationWrapper = registrationWrapper
            )
          }

          val thrown = intercept[Exception] {
            await(result)
          }

          thrown.getMessage must include("Internal server error")

          val expectedAuditEvent = IntermediaryAmendRegistrationAuditModel.build(
            RegistrationAuditType.AmendRegistration,
            userAnswers,
            None,
            SubmissionResult.Failure
          )

          verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
        }
      }
    }
  }

  private def getChangeRegistrationVatRegistrationDetailsSummaryList(answers: UserAnswers)(implicit msgs: Messages): Seq[SummaryListRow] = {

    implicit val authRequest: AuthenticatedDataRequest[AnyContent] =
      AuthenticatedDataRequest(
        fakeRequest, testCredentials, vrn, testEnrolments, answers, None, 0, None, None, None, None
      )

    Seq(
      VatRegistrationDetailsSummary.rowBasedInUk(answers),
      VatRegistrationDetailsSummary.rowBusinessName(answers),
      VatRegistrationDetailsSummary.rowVatNumber(),
      VatRegistrationDetailsSummary.rowBusinessAddress(answers)
    ).flatten
  }

  private def getChangeRegistrationSummaryList(answers: UserAnswers)(implicit msgs: Messages): Seq[SummaryListRow] =
    val niAddressSummaryRow = NiAddressSummary.row(waypoints, answers, amendYourAnswersPage)
    val maybeHasTradingNameSummaryRow = HasTradingNameSummary.row(waypoints, answers, amendYourAnswersPage)
    val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(waypoints, answers, amendYourAnswersPage)
    val maybeHasPreviouslyRegisteredAsIntermediaryRow = HasPreviouslyRegisteredAsIntermediarySummary
      .checkAnswersRow(waypoints, answers, amendYourAnswersPage)
    val previouslyRegisteredAsIntermediaryRow = PreviousIntermediaryRegistrationsSummary.checkAnswersRow(waypoints, answers, amendYourAnswersPage, Seq(previousIntermediaryRegistration))
    val maybeHasFixedEstablishmentSummaryRow = HasFixedEstablishmentSummary.row(waypoints, answers, amendYourAnswersPage)
    val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(waypoints, answers, amendYourAnswersPage)
    val contactDetailsFullNameRow = ContactDetailsSummary.rowContactName(waypoints, answers, amendYourAnswersPage)
    val contactDetailsTelephoneNumberRow = ContactDetailsSummary.rowTelephoneNumber(waypoints, answers, amendYourAnswersPage)
    val contactDetailsEmailAddressRow = ContactDetailsSummary.rowEmailAddress(waypoints, answers, amendYourAnswersPage)
    val bankDetailsAccountNameRow = BankDetailsSummary.rowAccountName(waypoints, answers, amendYourAnswersPage)
    val bankDetailsBicRow = BankDetailsSummary.rowBIC(waypoints, answers, amendYourAnswersPage)
    val bankDetailsIbanRow = BankDetailsSummary.rowIBAN(waypoints, answers, amendYourAnswersPage)

    Seq(
        niAddressSummaryRow,
        maybeHasTradingNameSummaryRow.map { hasTradingNameSummaryRow =>
          if (tradingNameSummaryRow.nonEmpty) {
            hasTradingNameSummaryRow.withCssClass("govuk-summary-list__row--no-border")
          } else {
            hasTradingNameSummaryRow
          }
        },
        tradingNameSummaryRow,
        maybeHasPreviouslyRegisteredAsIntermediaryRow.map { hasPreviouslyRegisteredAsIntermediaryRow =>
          if (previouslyRegisteredAsIntermediaryRow.nonEmpty) {
            hasPreviouslyRegisteredAsIntermediaryRow.withCssClass("govuk-summary-list__row--no-border")
          } else {
            hasPreviouslyRegisteredAsIntermediaryRow
          }
        },
        previouslyRegisteredAsIntermediaryRow,
        maybeHasFixedEstablishmentSummaryRow.map { hasFixedEstablishmentSummaryRow =>
          if (euDetailsSummaryRow.nonEmpty) {
            hasFixedEstablishmentSummaryRow.withCssClass("govuk-summary-list__row--no-border")
          } else {
            hasFixedEstablishmentSummaryRow
          }
        },
        euDetailsSummaryRow,
        contactDetailsFullNameRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
        contactDetailsTelephoneNumberRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
        contactDetailsEmailAddressRow,
        bankDetailsAccountNameRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
        bankDetailsBicRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
        bankDetailsIbanRow
    ).flatten
}
