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
import models.etmp.amend.AmendRegistrationResponse
import models.requests.{AuthenticatedDataRequest, AuthenticatedMandatoryIntermediaryRequest}
import models.responses.InternalServerError
import models.{BankDetails, Bic, CheckMode, ContactDetails, DesAddress, Iban, Index, TradingName, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{doNothing, reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.amend.{ChangePreviousRegistrationPage, ChangeRegistrationPage}
import pages.euDetails.HasFixedEstablishmentPage
import pages.filters.RegisteredForIossIntermediaryInEuPage
import pages.previousIntermediaryRegistrations.HasPreviouslyRegisteredAsIntermediaryPage
import pages.tradingNames.{HasTradingNamePage, TradingNamePage}
import pages.{BankDetailsPage, CheckAnswersPage, ContactDetailsPage, EmptyWaypoints, Waypoint, Waypoints}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.amend.PreviousRegistrationIntermediaryNumberQuery
import repositories.AuthenticatedUserAnswersRepository
import services.{AuditService, RegistrationService}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.checkAnswers.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediarySummary, PreviousIntermediaryRegistrationsSummary}
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, ContactDetailsSummary, NiAddressSummary, VatRegistrationDetailsSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.ChangeRegistrationView

import java.time.{Instant, LocalDate, LocalDateTime}

class ChangeRegistrationControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar  with BeforeAndAfterEach {

  private val waypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
  private val amendYourAnswersPage = ChangeRegistrationPage
  private val previousIntermediaryRegistration = arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value
  private val mockAuditService: AuditService = mock[AuditService]
  private val mockRegistrationService: RegistrationService = mock[RegistrationService]

  override val iban: Iban = Iban("GB33BUKB202015555555555").toOption.get
  override val bic: Bic = Bic("BARCGB22456").get

  val amendRegistrationResponse = AmendRegistrationResponse(
    processingDateTime = LocalDateTime.now(),
    businessPartner = "businessPartner",
    intermediary = "IN900100000001",
    formBundleNumber = "12345",
    vrn = "123456789",
  )

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

  override def basicUserAnswersWithVatInfo: UserAnswers =
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

  override def beforeEach(): Unit = {
    reset(mockAuditService)
    reset(mockRegistrationService)
  }

  "ChangeRegistration Controller" - {

    ".onPageLoad" - {

      "must return OK and the correct view for a GET when isPreviousRegistration is false" - {

        "with completed data present" in {

          val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo)).build()

          running(application) {

            val request = FakeRequest(GET, controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = false).url)
              .withSession("intermediaryNumber" -> "IN1234567890")
            implicit val msgs: Messages = messages(application)
            val result = route(application, request).value

            val view = application.injector.instanceOf[ChangeRegistrationView]

            val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(completeUserAnswersWithVatInfo))

            val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(waypoints, completeUserAnswersWithVatInfo, amendYourAnswersPage))

            val hasMultipleIntermediaryEnrolments: Boolean = false

            val isCurrentIntermediaryAccount: Boolean = true

            status(result) mustBe OK
            contentAsString(result) mustBe view(waypoints, vatInfoList, list, intermediaryNumber, hasMultipleIntermediaryEnrolments, isCurrentIntermediaryAccount, isValid = true, moreThanOnePreviousReg = true)(request, messages(application)).toString
          }
        }

        "with incomplete data" in {
          val missingAnswers: UserAnswers = completeUserAnswersWithVatInfo
            .remove(TradingNamePage(countryIndex(0))).success.value

          val application = applicationBuilder(userAnswers = Some(missingAnswers)).build()

          running(application) {

            val request = FakeRequest(GET, controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = false).url)
              .withSession("intermediaryNumber" -> "IN1234567890")
            implicit val msgs: Messages = messages(application)
            val result = route(application, request).value

            val view = application.injector.instanceOf[ChangeRegistrationView]

            val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(completeUserAnswersWithVatInfo))

            val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(waypoints, missingAnswers, amendYourAnswersPage))

            val hasMultipleIntermediaryEnrolments: Boolean = false

            val isCurrentIntermediaryAccount: Boolean = true

            status(result) mustBe OK
            contentAsString(result) mustBe view(waypoints, vatInfoList, list, intermediaryNumber, hasMultipleIntermediaryEnrolments, isCurrentIntermediaryAccount, isValid = false, moreThanOnePreviousReg = true)(request, messages(application)).toString
          }
        }
      }

      "must return OK and the correct view for a GET when isPreviousRegistration is true" - {

        val previousRegistrationPage = ChangePreviousRegistrationPage

        val previousIntermediaryNumber: String = "IN12345678"

        val userAnswersForPreviousReg: UserAnswers = completeUserAnswersWithVatInfo
          .set(PreviousRegistrationIntermediaryNumberQuery, previousIntermediaryNumber).success.value

        val isPreviousRegWaypoint = EmptyWaypoints.setNextWaypoint(Waypoint(previousRegistrationPage, CheckMode, ChangePreviousRegistrationPage.urlFragment))

        "with completed data present" in {

          val application = applicationBuilder(userAnswers = Some(userAnswersForPreviousReg)).build()

          running(application) {

            val request = FakeRequest(GET, controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = true).url)
              .withSession("intermediaryNumber" -> "IN1234567890")
            implicit val msgs: Messages = messages(application)
            val result = route(application, request).value

            val view = application.injector.instanceOf[ChangeRegistrationView]

            val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(userAnswersForPreviousReg))

            val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(isPreviousRegWaypoint, completeUserAnswersWithVatInfo, previousRegistrationPage))

            val hasMultipleIntermediaryEnrolments: Boolean = false

            val isCurrentIntermediaryAccount: Boolean = false
            
            status(result) mustBe OK
            contentAsString(result) mustBe view(isPreviousRegWaypoint, vatInfoList, list, previousIntermediaryNumber, hasMultipleIntermediaryEnrolments, isCurrentIntermediaryAccount, isValid = true, moreThanOnePreviousReg = true)(request, messages(application)).toString
          }
        }

        "with incomplete data" in {
          val missingAnswers: UserAnswers = userAnswersForPreviousReg
            .remove(TradingNamePage(countryIndex(0))).success.value

          val application = applicationBuilder(userAnswers = Some(missingAnswers)).build()

          running(application) {

            val request = FakeRequest(GET, controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = true).url)
              .withSession("intermediaryNumber" -> "IN1234567890")
            implicit val msgs: Messages = messages(application)
            val result = route(application, request).value

            val view = application.injector.instanceOf[ChangeRegistrationView]

            val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(completeUserAnswersWithVatInfo))

            val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(isPreviousRegWaypoint, missingAnswers, previousRegistrationPage))

            val hasMultipleIntermediaryEnrolments: Boolean = false

            val isCurrentIntermediaryAccount: Boolean = false

            status(result) mustBe OK
            contentAsString(result) mustBe view(isPreviousRegWaypoint, vatInfoList, list, previousIntermediaryNumber, hasMultipleIntermediaryEnrolments, isCurrentIntermediaryAccount, isValid = false, moreThanOnePreviousReg = true)(request, messages(application)).toString
          }
        }
      }
    }

    ".onSubmit" - {

      "must send the amended registration, audit success event then redirect when registration succeeds" in {

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
          verify(mockRegistrationService, times(1)).amendRegistration(any(), any(), any(), any(), any(), any())(any())
        }
      }

      "must send the amended registration, audit failure event and throw exception when registration fails" in {

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
          verify(mockRegistrationService, times(1)).amendRegistration(any(), any(), any(), any(), any(), any())(any())
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

  private def getChangeRegistrationSummaryList(waypoints: Waypoints, answers: UserAnswers, page: CheckAnswersPage = ChangeRegistrationPage)(implicit msgs: Messages): Seq[SummaryListRow] =

    val niAddressSummaryRow = NiAddressSummary.row(waypoints, answers, page)
    val maybeHasTradingNameSummaryRow = HasTradingNameSummary.row(waypoints, answers, page)
    val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(waypoints, answers, page)
    val maybeHasPreviouslyRegisteredAsIntermediaryRow = HasPreviouslyRegisteredAsIntermediarySummary
      .checkAnswersRow(waypoints, answers, page)
    val previouslyRegisteredAsIntermediaryRow = PreviousIntermediaryRegistrationsSummary.checkAnswersRow(waypoints, answers, page, Seq(previousIntermediaryRegistration))
    val maybeHasFixedEstablishmentSummaryRow = HasFixedEstablishmentSummary.row(waypoints, answers, page)
    val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(waypoints, answers, page)
    val contactDetailsFullNameRow = ContactDetailsSummary.rowContactName(waypoints, answers, page)
    val contactDetailsTelephoneNumberRow = ContactDetailsSummary.rowTelephoneNumber(waypoints, answers, page)
    val contactDetailsEmailAddressRow = ContactDetailsSummary.rowEmailAddress(waypoints, answers, page)
    val bankDetailsAccountNameRow = BankDetailsSummary.rowAccountName(waypoints, answers, page)
    val bankDetailsBicRow = BankDetailsSummary.rowBIC(waypoints, answers, page)
    val bankDetailsIbanRow = BankDetailsSummary.rowIBAN(waypoints, answers, page)

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
