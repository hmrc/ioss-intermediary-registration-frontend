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

package controllers.rejoin

import base.SpecBase
import connectors.RegistrationConnector
import controllers.rejoin.validation.RejoinRegistrationValidation
import models.audit.{IntermediaryAmendRegistrationAuditModel, RegistrationAuditType, SubmissionResult}
import models.etmp.{EtmpExclusion, EtmpOtherAddress}
import models.etmp.EtmpExclusionReason.NoLongerSupplies
import models.etmp.amend.AmendRegistrationResponse
import models.etmp.display.{EtmpDisplayRegistration, RegistrationWrapper}
import models.requests.{AuthenticatedDataRequest, AuthenticatedMandatoryIntermediaryRequest}
import models.responses.InternalServerError
import models.{CheckMode, UkAddress, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{doNothing, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.checkVatDetails.NiAddressPage
import pages.rejoin.{CannotRejoinPage, CannotRejoinVatNumberAlreadyRegisteredPage, RejoinSchemePage}
import pages.{EmptyWaypoints, Waypoint, Waypoints}
import play.api.i18n.Messages
import play.api.inject
import play.api.inject.bind
import play.api.mvc.{AnyContent, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.rejoin.NewIossReferenceQuery
import services.{AuditService, RegistrationService}
import testutils.CheckYourAnswersSummaries.FluentSummaryListRow
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.checkAnswers.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediarySummary, PreviousIntermediaryRegistrationsSummary}
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, ContactDetailsSummary, NiAddressSummary, VatRegistrationDetailsSummary}
import viewmodels.govuk.all.SummaryListViewModel
import views.html.rejoin.RejoinSchemeView

import java.time.{Clock, LocalDate, LocalDateTime}
import scala.concurrent.Future

class RejoinSchemeControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockRegistrationService: RegistrationService = mock[RegistrationService]
  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockRejoinRegistrationValidation: RejoinRegistrationValidation = mock[RejoinRegistrationValidation]

  private val rejoinSchemePage = RejoinSchemePage
  private val previousIntermediaryRegistration = arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value
  private val mockAuditService: AuditService = mock[AuditService]

  private val rejoinWaypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(RejoinSchemePage, CheckMode, RejoinSchemePage.urlFragment))
  private val amendRegistrationResponse: AmendRegistrationResponse = {
    AmendRegistrationResponse(
      processingDateTime = LocalDateTime.now(),
      formBundleNumber = "12345",
      vrn = "123456789",
      intReference = "IM900100000001",
      businessPartner = "businessPartner"
    )
  }

  private val registrationWrapperWithNiAddress: RegistrationWrapper = registrationWrapper.copy(
    etmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value.copy(
      otherAddress = Some(
        EtmpOtherAddress(
          issuedBy = "GB",
          tradingName = Some("Company name"),
          addressLine1 = "Other Address Line 1",
          addressLine2 = Some("Other Address Line 2"),
          townOrCity = "Other Town or City",
          regionOrState = Some("Other Region or State"),
          postcode = Some("BT111AH")
        )
      )
    )
  )

  override def beforeEach(): Unit = {
    Mockito.reset(
      mockRejoinRegistrationValidation,
      mockRegistrationConnector,
      mockRegistrationService
    )
  }

  "RejoinScheme Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockRejoinRegistrationValidation.validateEuRegistrations(any(), any())(any(), any(), any())) thenReturn Right(true).toFuture

      val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo))
        .overrides(bind[RejoinRegistrationValidation].toInstance(mockRejoinRegistrationValidation))
        .build()

      running(application) {

        val request = FakeRequest(GET, controllers.rejoin.routes.RejoinSchemeController.onPageLoad().url)

        implicit val msgs: Messages = messages(application)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RejoinSchemeView]

        val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(completeUserAnswersWithVatInfo))

        val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(completeUserAnswersWithVatInfo))

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(rejoinWaypoints, vatInfoList, list)(request, msgs).toString
        verify(mockRejoinRegistrationValidation, times(1)).validateEuRegistrations(any(), any())(any(), any(), any())
      }
    }

    "must redirect to the appropriate kick out page when registration validation fails for a GET" in {

      val countryCode: String = arbitraryCountry.arbitrary.sample.value.code
      val redirect: Call = CannotRejoinVatNumberAlreadyRegisteredPage(countryCode).route(rejoinWaypoints)

      when(mockRejoinRegistrationValidation.validateEuRegistrations(any(), any())(any(), any(), any())) thenReturn Left(redirect).toFuture

      val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo))
        .overrides(bind[RejoinRegistrationValidation].toInstance(mockRejoinRegistrationValidation))
        .build()

      running(application) {

        val request = FakeRequest(GET, controllers.rejoin.routes.RejoinSchemeController.onPageLoad().url)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` redirect.url
        verify(mockRejoinRegistrationValidation, times(1)).validateEuRegistrations(any(), any())(any(), any(), any())
      }
    }

    ".onSubmit" - {

      "must trigger amendRegistration and redirect to the next page if an intermediary can rejoin the scheme" in {

        val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())

        val fromEtmpOtherAddress: Option[EtmpOtherAddress] = registrationWrapperWithExclusionOnBoundary.etmpDisplayRegistration.otherAddress

        val niAddress: UkAddress = UkAddress(
          line1 = fromEtmpOtherAddress.value.addressLine1,
          line2 = fromEtmpOtherAddress.value.addressLine2,
          townOrCity = fromEtmpOtherAddress.value.townOrCity,
          county = fromEtmpOtherAddress.value.regionOrState,
          postCode = fromEtmpOtherAddress.value.postcode.value
        )

        val updatedAnswersWithNiAddress: UserAnswers = completeUserAnswersWithVatInfo
          .set(NiAddressPage, niAddress).success.value
        
        when(mockRejoinRegistrationValidation.validateEuRegistrations(any(), any())(any(), any(), any())) thenReturn Right(true).toFuture
        when(mockRegistrationConnector.displayRegistration(any())(any())) thenReturn Right(registrationWrapperWithExclusionOnBoundary).toFuture

        val application = applicationBuilder(
          userAnswers = Some(updatedAnswersWithNiAddress),
          clock = Some(Clock.systemUTC()),
          registrationWrapper = Some(registrationWrapperWithExclusionOnBoundary)
        )
          .overrides(
            bind[RegistrationService].toInstance(mockRegistrationService),
            bind[RejoinRegistrationValidation].toInstance(mockRejoinRegistrationValidation)
          )
          .build()

        when(mockRegistrationService.amendRegistration(any(), any(), any(), any(), any(), any(), any(), any())(any())) thenReturn
          Right(amendRegistrationResponse).toFuture

        running(application) {
          val request = FakeRequest(POST, controllers.rejoin.routes.RejoinSchemeController.onSubmit(rejoinWaypoints).url)
          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe`
            RejoinSchemePage.navigate(EmptyWaypoints, updatedAnswersWithNiAddress, updatedAnswersWithNiAddress).route.url
          verify(mockRejoinRegistrationValidation, times(1)).validateEuRegistrations(any(), any())(any(), any(), any())
        }
      }

      "must redirect to the appropriate kick out page when registration validation fails for a POST" in {

        val registrationWrapper: RegistrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value
        val updatedRegistrationWrapper: RegistrationWrapper = registrationWrapper
          .copy(
            vatInfo = registrationWrapperWithNiAddress.vatInfo,
            etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration
              .copy(exclusions = Seq(EtmpExclusion(
                exclusionReason = NoLongerSupplies,
                effectiveDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(2),
                decisionDate = LocalDate.now(),
                quarantine = false
              )),
                otherAddress = registrationWrapperWithNiAddress.etmpDisplayRegistration.otherAddress
              ))

        val countryCode: String = arbitraryCountry.arbitrary.sample.value.code
        val redirect: Call = CannotRejoinVatNumberAlreadyRegisteredPage(countryCode).route(rejoinWaypoints)

        when(mockRejoinRegistrationValidation.validateEuRegistrations(any(), any())(any(), any(), any())) thenReturn Left(redirect).toFuture

        val application = applicationBuilder(
          userAnswers = Some(completeUserAnswersWithVatInfo),
          registrationWrapper = Some(updatedRegistrationWrapper)
        )
          .overrides(bind[RejoinRegistrationValidation].toInstance(mockRejoinRegistrationValidation))
          .build()

        running(application) {

          val request = FakeRequest(POST, controllers.rejoin.routes.RejoinSchemeController.onSubmit(rejoinWaypoints).url)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` redirect.url
          verify(mockRejoinRegistrationValidation, times(1)).validateEuRegistrations(any(), any())(any(), any(), any())
        }
      }

      "must redirect to CannotRejoinPage if an intermediary cannot rejoin the scheme" in {

        val fakeDisplayRegistration = mock[EtmpDisplayRegistration]
        when(fakeDisplayRegistration.canRejoinScheme(any())) thenReturn false

        val updatedRegistrationWrapper: RegistrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value
          .copy(
            vatInfo = registrationWrapperWithNiAddress.vatInfo,
            etmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value
              .copy(
                exclusions = Seq.empty,
                otherAddress = registrationWrapperWithNiAddress.etmpDisplayRegistration.otherAddress
              ))

        val application =
          applicationBuilder(
            userAnswers = Some(emptyUserAnswers),
            registrationWrapper = Some(updatedRegistrationWrapper)
          )
            .build()

        running(application) {

          val request = FakeRequest(POST, controllers.rejoin.routes.RejoinSchemeController.onSubmit(waypoints).url)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` CannotRejoinPage.route(EmptyWaypoints).url
        }
      }

      "must audit the event and redirect to the next page and successfully" in {

        val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())

        when(mockRejoinRegistrationValidation.validateEuRegistrations(any(), any())(any(), any(), any())) thenReturn Right(true).toFuture
        when(mockRegistrationConnector.displayRegistration(any())(any())).thenReturn(Future.successful(Right(registrationWrapperWithExclusionOnBoundary)))
        doNothing().when(mockAuditService).audit(any())(any(), any())

        val application = applicationBuilder(
          userAnswers = Some(completeUserAnswersWithVatInfo),
          clock = Some(Clock.systemUTC()),
          registrationWrapper = Some(registrationWrapperWithExclusionOnBoundary)
        )
          .overrides(
            bind[RegistrationService].toInstance(mockRegistrationService),
            bind[AuditService].toInstance(mockAuditService),
            bind[RejoinRegistrationValidation].toInstance(mockRejoinRegistrationValidation)
          )
          .build()

        when(mockRegistrationService.amendRegistration(any(), any(), any(), any(), any(), any(), any(), any())(any())) thenReturn
          Right(amendRegistrationResponse).toFuture

        running(application) {
          val request = FakeRequest(POST, controllers.rejoin.routes.RejoinSchemeController.onSubmit(rejoinWaypoints).url)
          val result = route(application, request).value

          implicit val authenticatedDataRequest: AuthenticatedDataRequest[_] =
            AuthenticatedDataRequest(
              request = request,
              credentials = testCredentials,
              vrn = vrn,
              enrolments = Enrolments(Set.empty),
              userAnswers = completeUserAnswersWithVatInfo,
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
              userAnswers = completeUserAnswersWithVatInfo,
              numberOfIossRegistrations = 1,
              latestIossRegistration = None,
              latestOssRegistration = None,
              intermediaryNumber = intermediaryNumber,
              registrationWrapper = registrationWrapper
            )
          }

          val updatedUserAnswers = completeUserAnswersWithVatInfo
            .set(NewIossReferenceQuery, amendRegistrationResponse.intReference).get

          val expectedAuditEvent = IntermediaryAmendRegistrationAuditModel.build(
            RegistrationAuditType.AmendRegistration,
            updatedUserAnswers,
            Some(amendRegistrationResponse),
            SubmissionResult.Success
          )

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe
            RejoinSchemePage.navigate(EmptyWaypoints, completeUserAnswersWithVatInfo, completeUserAnswersWithVatInfo).route.url
          verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
        }
      }

      "when the submission fails because of a technical issue" in {

        val registrationWrapperWithExclusionOnBoundary = createRegistrationWrapperWithExclusion(LocalDate.now())

        when(mockRejoinRegistrationValidation.validateEuRegistrations(any(), any())(any(), any(), any())) thenReturn Right(true).toFuture
        when(mockRegistrationConnector.displayRegistration(any())(any())).thenReturn(Future.successful(Right(registrationWrapperWithExclusionOnBoundary)))
        when(mockRegistrationService.amendRegistration(any(), any(), any(), any(), any(), any(), any(), any())(any()))
          .thenReturn(Left(InternalServerError).toFuture)
        doNothing().when(mockAuditService).audit(any())(any(), any())

        val application = applicationBuilder(
          userAnswers = Some(completeUserAnswersWithVatInfo),
          clock = Some(Clock.systemUTC()),
          registrationWrapper = Some(registrationWrapperWithExclusionOnBoundary)
        )
          .overrides(
            bind[RegistrationService].toInstance(mockRegistrationService),
            bind[AuditService].toInstance(mockAuditService),
            bind[RejoinRegistrationValidation].toInstance(mockRejoinRegistrationValidation)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, controllers.rejoin.routes.RejoinSchemeController.onSubmit(rejoinWaypoints).url)

          implicit val authenticatedDataRequest: AuthenticatedDataRequest[_] =
            AuthenticatedDataRequest(
              request = request,
              credentials = testCredentials,
              vrn = vrn,
              enrolments = Enrolments(Set.empty),
              userAnswers = completeUserAnswersWithVatInfo,
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
              userAnswers = completeUserAnswersWithVatInfo,
              numberOfIossRegistrations = 1,
              latestIossRegistration = None,
              latestOssRegistration = None,
              intermediaryNumber = intermediaryNumber,
              registrationWrapper = registrationWrapper
            )
          }

          val expectedAuditEvent = IntermediaryAmendRegistrationAuditModel.build(
            RegistrationAuditType.AmendRegistration,
            completeUserAnswersWithVatInfo,
            None,
            SubmissionResult.Failure
          )
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe
            controllers.rejoin.routes.ErrorSubmittingRejoinController.onPageLoad().url

          await(result)
          verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
        }
      }
    }
  }

  def createRegistrationWrapperWithExclusion(effectiveDate: LocalDate): RegistrationWrapper = {
    val registration = registrationWrapper.etmpDisplayRegistration

    registrationWrapper.copy(
      etmpDisplayRegistration = registration.copy(
        exclusions = List(
          EtmpExclusion(
            exclusionReason = NoLongerSupplies,
            effectiveDate = effectiveDate,
            decisionDate = LocalDate.now(),
            quarantine = false
          )
        ),
        otherAddress = Some(
          EtmpOtherAddress(
            issuedBy = "GB",
            tradingName = Some("Company name"),
            addressLine1 = "Other Address Line 1",
            addressLine2 = Some("Other Address Line 2"),
            townOrCity = "Other Town or City",
            regionOrState = Some("Other Region or State"),
            postcode = Some("BT111AH")
          )
        )
      )
    )
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
    val niAddressSummaryRow = NiAddressSummary.row(waypoints, answers, isExcluded = true, rejoinSchemePage)
    val maybeHasTradingNameSummaryRow = HasTradingNameSummary.row(waypoints, answers, rejoinSchemePage)
    val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(waypoints, answers, rejoinSchemePage)
    val maybeHasPreviouslyRegisteredAsIntermediaryRow = HasPreviouslyRegisteredAsIntermediarySummary
      .checkAnswersRow(waypoints, answers, rejoinSchemePage)
    val previouslyRegisteredAsIntermediaryRow = PreviousIntermediaryRegistrationsSummary.checkAnswersRow(waypoints, answers, rejoinSchemePage, Seq(previousIntermediaryRegistration))
    val maybeHasFixedEstablishmentSummaryRow = HasFixedEstablishmentSummary.row(waypoints, answers, rejoinSchemePage)
    val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(waypoints, answers, rejoinSchemePage)
    val contactDetailsFullNameRow = ContactDetailsSummary.rowContactName(waypoints, answers, rejoinSchemePage)
    val contactDetailsTelephoneNumberRow = ContactDetailsSummary.rowTelephoneNumber(waypoints, answers, rejoinSchemePage)
    val contactDetailsEmailAddressRow = ContactDetailsSummary.rowEmailAddress(waypoints, answers, rejoinSchemePage)
    val bankDetailsAccountNameRow = BankDetailsSummary.rowAccountName(waypoints, answers, rejoinSchemePage)
    val bankDetailsBicRow = BankDetailsSummary.rowBIC(waypoints, answers, rejoinSchemePage)
    val bankDetailsIbanRow = BankDetailsSummary.rowIBAN(waypoints, answers, rejoinSchemePage)

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
