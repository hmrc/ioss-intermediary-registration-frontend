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
import config.FrontendAppConfig
import formats.Format.eisDateFormatter
import models.amend.BusinessAddressInNi.Yes
import models.audit.{IntermediaryAmendRegistrationAuditModel, RegistrationAuditType, SubmissionResult}
import models.domain.VatCustomerInfo
import models.etmp.EtmpExclusionReason.{Reversal, TransferringMSID}
import models.etmp.amend.AmendRegistrationResponse
import models.etmp.display.RegistrationWrapper
import models.etmp.{EtmpExclusion, EtmpOtherAddress, EtmpTradingName}
import models.requests.{AuthenticatedDataRequest, AuthenticatedMandatoryIntermediaryRequest}
import models.responses.InternalServerError
import models.{BankDetails, Bic, CheckMode, ContactDetails, DesAddress, Iban, Index, TradingName, UkAddress, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.amend.{ChangePreviousRegistrationPage, ChangeRegistrationPage, HasBusinessAddressInNiPage}
import pages.checkVatDetails.NiAddressPage
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
import queries.OriginalRegistrationQuery
import queries.amend.PreviousRegistrationIntermediaryNumberQuery
import queries.tradingNames.AllTradingNamesQuery
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

class ChangeRegistrationControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar with BeforeAndAfterEach {

  private val waypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
  private val amendYourAnswersPage = ChangeRegistrationPage
  private val previousIntermediaryRegistration = arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value
  private val mockAuditService: AuditService = mock[AuditService]
  private val mockRegistrationService: RegistrationService = mock[RegistrationService]

  override val iban: Iban = Iban("GB33BUKB202015555555555").toOption.get
  override val bic: Bic = Bic("BARCGB22456").get

  val amendRegistrationResponse: AmendRegistrationResponse = AmendRegistrationResponse(
    processingDateTime = LocalDateTime.now(),
    businessPartner = "businessPartner",
    intReference = "IN900100000001",
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
      .set(RegisteredForIossIntermediaryInEuPage, false).success.value
      .set(HasTradingNamePage, true).success.value
      .set(TradingNamePage(Index(0)), TradingName("Chartoff Winkler and Co. Robert Rocky Balboa Robert Balboa")).success.value
      .set(HasPreviouslyRegisteredAsIntermediaryPage, false).success.value
      .set(HasFixedEstablishmentPage, false).success.value
      .set(ContactDetailsPage, ContactDetails("Rocky Balboa", "028 123 4567", "rocky.balboa@chartoffwinkler.co.uk")).success.value
      .set(BankDetailsPage, BankDetails("Chartoff Winkler and Co.", Some(bic), iban)).success.value

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
          postcode = "BT111AH"
        )
      ),
      schemeDetails = arbitraryEtmpDisplaySchemeDetails.arbitrary.sample.value.copy(
        unusableStatus = true
      ),
      exclusions = Seq.empty
    )
  )

  override def beforeEach(): Unit = {
    reset(mockAuditService)
    reset(mockRegistrationService)
  }

  "ChangeRegistration Controller" - {

    ".onPageLoad" - {

      "must return OK and the correct view for a GET when isPreviousRegistration is false" - {

        "with completed data present" in {

          val application = applicationBuilder(
            userAnswers = Some(completeUserAnswersWithVatInfo),
            registrationWrapper = Some(registrationWrapperWithNiAddress)
          ).build()

          running(application) {

            val request = FakeRequest(GET, controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = false).url)
              .withSession("intermediaryNumber" -> "IN1234567890")

            val config = application.injector.instanceOf[FrontendAppConfig]

            implicit val msgs: Messages = messages(application)
            val result = route(application, request).value

            val view = application.injector.instanceOf[ChangeRegistrationView]

            val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(completeUserAnswersWithVatInfo))

            val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(waypoints, completeUserAnswersWithVatInfo, checkOtherAddressNi = false, amendYourAnswersPage))

            val hasMultipleIntermediaryEnrolments: Boolean = false

            val isCurrentIntermediaryAccount: Boolean = true

            status(result) mustBe OK
            contentAsString(result) mustBe view(waypoints, vatInfoList, list, intermediaryNumber, hasMultipleIntermediaryEnrolments, isCurrentIntermediaryAccount, isValid = true, moreThanOnePreviousReg = true, unusableStatus = true, noChangesMade = true, config.intermediaryYourAccountUrl)(request, messages(application)).toString
          }
        }

        "with incomplete data" in {

          val missingAnswers: UserAnswers = completeUserAnswersWithVatInfo
            .remove(TradingNamePage(countryIndex(0))).success.value

          val application = applicationBuilder(userAnswers = Some(missingAnswers), registrationWrapper = Some(registrationWrapperWithNiAddress)).build()

          running(application) {

            val request = FakeRequest(GET, controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = false).url)
              .withSession("intermediaryNumber" -> "IN1234567890")

            val config = application.injector.instanceOf[FrontendAppConfig]
            implicit val msgs: Messages = messages(application)
            val result = route(application, request).value

            val view = application.injector.instanceOf[ChangeRegistrationView]

            val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(completeUserAnswersWithVatInfo))

            val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(waypoints, missingAnswers, checkOtherAddressNi = false, amendYourAnswersPage))

            val hasMultipleIntermediaryEnrolments: Boolean = false

            val isCurrentIntermediaryAccount: Boolean = true

            status(result) mustBe OK
            contentAsString(result) mustBe view(waypoints, vatInfoList, list, intermediaryNumber, hasMultipleIntermediaryEnrolments, isCurrentIntermediaryAccount, isValid = false, moreThanOnePreviousReg = true, unusableStatus = true, noChangesMade = true, config.intermediaryYourAccountUrl)(request, messages(application)).toString
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

          val application = applicationBuilder(userAnswers = Some(userAnswersForPreviousReg), registrationWrapper = Some(registrationWrapperWithNiAddress)).build()

          running(application) {

            val request = FakeRequest(GET, controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = true).url)
              .withSession("intermediaryNumber" -> "IN1234567890")

            val config = application.injector.instanceOf[FrontendAppConfig]
            implicit val msgs: Messages = messages(application)
            val result = route(application, request).value

            val view = application.injector.instanceOf[ChangeRegistrationView]

            val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(userAnswersForPreviousReg))

            val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(isPreviousRegWaypoint, completeUserAnswersWithVatInfo, checkOtherAddressNi = false, previousRegistrationPage))

            val hasMultipleIntermediaryEnrolments: Boolean = false

            val isCurrentIntermediaryAccount: Boolean = false

            status(result) mustBe OK
            contentAsString(result) mustBe view(isPreviousRegWaypoint, vatInfoList, list, previousIntermediaryNumber, hasMultipleIntermediaryEnrolments, isCurrentIntermediaryAccount, isValid = true, moreThanOnePreviousReg = true, unusableStatus = true, noChangesMade = true, config.intermediaryYourAccountUrl)(request, messages(application)).toString
          }
        }

        "with incomplete data" in {

          val missingAnswers: UserAnswers = userAnswersForPreviousReg
            .remove(TradingNamePage(countryIndex(0))).success.value

          val application = applicationBuilder(userAnswers = Some(missingAnswers), registrationWrapper = Some(registrationWrapperWithNiAddress)).build()

          running(application) {

            val request = FakeRequest(GET, controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = true).url)
              .withSession("intermediaryNumber" -> "IN1234567890")

            val config = application.injector.instanceOf[FrontendAppConfig]
            implicit val msgs: Messages = messages(application)
            val result = route(application, request).value

            val view = application.injector.instanceOf[ChangeRegistrationView]

            val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(completeUserAnswersWithVatInfo))

            val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(isPreviousRegWaypoint, missingAnswers, checkOtherAddressNi = false, previousRegistrationPage))

            val hasMultipleIntermediaryEnrolments: Boolean = false

            val isCurrentIntermediaryAccount: Boolean = false

            status(result) mustBe OK
            contentAsString(result) mustBe view(isPreviousRegWaypoint, vatInfoList, list, previousIntermediaryNumber, hasMultipleIntermediaryEnrolments, isCurrentIntermediaryAccount, isValid = false, moreThanOnePreviousReg = true, unusableStatus = true, noChangesMade = true, config.intermediaryYourAccountUrl)(request, messages(application)).toString
          }
        }
      }

      "must return OK and the correct view for a GET when vatInfo contains a non NI address and subsequently adds a manual NI address" in {

        val vatInfoWithoutNiAddress: VatCustomerInfo =
          registrationWrapper.vatInfo
            .copy(desAddress = registrationWrapper.vatInfo.desAddress
              .copy(postCode = Some("AA123BC")))

        val excludedRegistration: RegistrationWrapper = registrationWrapper.copy(
          vatInfo = vatInfoWithoutNiAddress,
          etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration.copy(
            exclusions = Seq.empty,
            schemeDetails = registrationWrapper.etmpDisplayRegistration.schemeDetails.copy(
              commencementDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(2).format(eisDateFormatter)
            )
          )
        )

        val niAddress: UkAddress = arbitraryUkAddress.arbitrary.sample.value.copy(postCode = "BT123BC")
        val updatedUserAnswers: UserAnswers = completeUserAnswersWithVatInfo
          .set(HasBusinessAddressInNiPage, Yes).success.value
          .set(NiAddressPage, niAddress).success.value

        val application = applicationBuilder(
          userAnswers = Some(updatedUserAnswers),
          registrationWrapper = Some(excludedRegistration)
        ).build()

        running(application) {

          val request = FakeRequest(GET, controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = false).url)
            .withSession("intermediaryNumber" -> intermediaryNumber)

          val config = application.injector.instanceOf[FrontendAppConfig]
          implicit val msgs: Messages = messages(application)
          val result = route(application, request).value

          val view = application.injector.instanceOf[ChangeRegistrationView]

          val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(updatedUserAnswers))

          val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(waypoints, updatedUserAnswers, checkOtherAddressNi = true, amendYourAnswersPage))

          val hasMultipleIntermediaryEnrolments: Boolean = false

          val isCurrentIntermediaryAccount: Boolean = true

          status(result) mustBe OK
          contentAsString(result) mustBe view(waypoints, vatInfoList, list, intermediaryNumber, hasMultipleIntermediaryEnrolments, isCurrentIntermediaryAccount, isValid = true, moreThanOnePreviousReg = false, unusableStatus = false, noChangesMade = true, config.intermediaryYourAccountUrl)(request, messages(application)).toString
        }
      }

      "when exclusion exists" - {

        "must return OK and the correct view for a GET" in {

          val etmpExclusion: EtmpExclusion = EtmpExclusion(
            exclusionReason = TransferringMSID,
            effectiveDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(1),
            decisionDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(1),
            quarantine = false
          )

          val excludedRegistration: RegistrationWrapper = registrationWrapperWithNiAddress.copy(
            etmpDisplayRegistration = registrationWrapperWithNiAddress.etmpDisplayRegistration.copy(
              exclusions = Seq(etmpExclusion)
            )
          )

          val application = applicationBuilder(
            userAnswers = Some(completeUserAnswersWithVatInfo),
            registrationWrapper = Some(excludedRegistration)
          ).build()

          running(application) {

            val request = FakeRequest(GET, controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = false).url)
              .withSession("intermediaryNumber" -> "IN1234567890")

            val config = application.injector.instanceOf[FrontendAppConfig]
            implicit val msgs: Messages = messages(application)
            val result = route(application, request).value

            val view = application.injector.instanceOf[ChangeRegistrationView]

            val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(completeUserAnswersWithVatInfo))

            val list = SummaryListViewModel(rows = getChangeRegistrationSummaryListWhenExcluded(waypoints, completeUserAnswersWithVatInfo, checkOtherAddressNi = false, amendYourAnswersPage))

            val hasMultipleIntermediaryEnrolments: Boolean = false

            val isCurrentIntermediaryAccount: Boolean = true

            status(result) mustBe OK
            contentAsString(result) mustBe view(waypoints, vatInfoList, list, intermediaryNumber, hasMultipleIntermediaryEnrolments, isCurrentIntermediaryAccount, isValid = true, moreThanOnePreviousReg = false, unusableStatus = true, noChangesMade = true, config.intermediaryYourAccountUrl)(request, messages(application)).toString
          }
        }

        "must return OK and the correct view for a GET when Exclusion Reason is Reversal" in {

          val etmpExclusion: EtmpExclusion = EtmpExclusion(
            exclusionReason = Reversal,
            effectiveDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(1),
            decisionDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(1),
            quarantine = false
          )

          val excludedRegistration: RegistrationWrapper = registrationWrapperWithNiAddress.copy(
            etmpDisplayRegistration = registrationWrapperWithNiAddress.etmpDisplayRegistration.copy(
              exclusions = Seq(etmpExclusion)
            )
          )

          val application = applicationBuilder(
            userAnswers = Some(completeUserAnswersWithVatInfo),
            registrationWrapper = Some(excludedRegistration)
          ).build()

          running(application) {

            val request = FakeRequest(GET, controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = false).url)
              .withSession("intermediaryNumber" -> "IN1234567890")

            val config = application.injector.instanceOf[FrontendAppConfig]
            implicit val msgs: Messages = messages(application)
            val result = route(application, request).value

            val view = application.injector.instanceOf[ChangeRegistrationView]

            val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(completeUserAnswersWithVatInfo))

            val list = SummaryListViewModel(rows = getChangeRegistrationSummaryList(waypoints, completeUserAnswersWithVatInfo, checkOtherAddressNi = false, amendYourAnswersPage))

            val hasMultipleIntermediaryEnrolments: Boolean = false

            val isCurrentIntermediaryAccount: Boolean = true

            status(result) mustBe OK
            contentAsString(result) mustBe view(waypoints, vatInfoList, list, intermediaryNumber, hasMultipleIntermediaryEnrolments, isCurrentIntermediaryAccount, isValid = true, moreThanOnePreviousReg = false, unusableStatus = true, noChangesMade = true, config.intermediaryYourAccountUrl)(request, messages(application)).toString
          }
        }

        "must return OK and the correct view for a GET when vatInfo contains a non NI address and an existing manual NI address then changes it to a non NI address" in {

          val etmpExclusion: EtmpExclusion = EtmpExclusion(
            exclusionReason = TransferringMSID,
            effectiveDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(1),
            decisionDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(1),
            quarantine = false
          )

          val vatInfoWithoutNiAddress: VatCustomerInfo =
            registrationWrapper.vatInfo
              .copy(desAddress = registrationWrapper.vatInfo.desAddress
                .copy(postCode = Some("AA123BC")))

          val etmpOtherAddress: EtmpOtherAddress = arbitraryEtmpOtherAddress.arbitrary.sample.value.copy(postcode = "BT123BC")

          val excludedRegistration: RegistrationWrapper = registrationWrapper.copy(
            vatInfo = vatInfoWithoutNiAddress,
            etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration.copy(
              exclusions = Seq(etmpExclusion),
              schemeDetails = registrationWrapper.etmpDisplayRegistration.schemeDetails.copy(
                commencementDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(2).format(eisDateFormatter)
              ),
              otherAddress = Some(etmpOtherAddress)
            )
          )

          val nonNiAddress: UkAddress = arbitraryUkAddress.arbitrary.sample.value.copy(postCode = "AA123BD")
          val updatedUserAnswers: UserAnswers = completeUserAnswersWithVatInfo
            .set(NiAddressPage, nonNiAddress).success.value

          val application = applicationBuilder(
            userAnswers = Some(updatedUserAnswers),
            registrationWrapper = Some(excludedRegistration)
          ).build()

          running(application) {

            val request = FakeRequest(GET, controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = false).url)
              .withSession("intermediaryNumber" -> intermediaryNumber)

            val config = application.injector.instanceOf[FrontendAppConfig]
            implicit val msgs: Messages = messages(application)
            val result = route(application, request).value

            val view = application.injector.instanceOf[ChangeRegistrationView]

            val vatInfoList = SummaryListViewModel(rows = getChangeRegistrationVatRegistrationDetailsSummaryList(updatedUserAnswers))

            val list = SummaryListViewModel(rows = getChangeRegistrationSummaryListWhenExcluded(waypoints, updatedUserAnswers, checkOtherAddressNi = false, amendYourAnswersPage))

            val hasMultipleIntermediaryEnrolments: Boolean = false

            val isCurrentIntermediaryAccount: Boolean = true
            
            status(result) mustBe OK
            contentAsString(result) mustBe view(waypoints, vatInfoList, list, intermediaryNumber, hasMultipleIntermediaryEnrolments, isCurrentIntermediaryAccount, isValid = true, moreThanOnePreviousReg = false, unusableStatus = false, noChangesMade = true, config.intermediaryYourAccountUrl)(request, messages(application)).toString
          }
        }

        "has made changes to answers" in {
          val originalRegistration = registrationWrapperWithNiAddress.etmpDisplayRegistration.copy(
            tradingNames = Seq(EtmpTradingName("Original trading name"))
          )

          val changedUserAnswers = completeUserAnswersWithVatInfo
            .set(AllTradingNamesQuery, List(TradingName("Changed trading name"))).success.value
            .set(OriginalRegistrationQuery(intermediaryNumber), originalRegistration).success.value

          val registrationWrapperWithOriginal = registrationWrapperWithNiAddress.copy(
            etmpDisplayRegistration = originalRegistration
          )

          val application = applicationBuilder(
            userAnswers = Some(changedUserAnswers),
            registrationWrapper = Some(registrationWrapperWithOriginal)
          ).build()

          running(application) {

            val request = FakeRequest(GET, controllers.amend.routes.ChangeRegistrationController.onPageLoad(isPreviousRegistration = false).url)
              .withSession("intermediaryNumber" -> intermediaryNumber)

            val config = application.injector.instanceOf[FrontendAppConfig]
            implicit val msgs: Messages = messages(application)
            val result = route(application, request).value

            val view = application.injector.instanceOf[ChangeRegistrationView]

            val vatInfoList = SummaryListViewModel(
              rows = getChangeRegistrationVatRegistrationDetailsSummaryList(changedUserAnswers)
            )

            val list = SummaryListViewModel(
              rows = getChangeRegistrationSummaryList(
                waypoints,
                changedUserAnswers,
                checkOtherAddressNi = false,
                amendYourAnswersPage
              )
            )

            val hasMultipleIntermediaryEnrolments: Boolean = false
            val isCurrentIntermediaryAccount: Boolean = true

            status(result) mustBe OK

            contentAsString(result) mustBe view(
              waypoints,
              vatInfoList,
              list,
              intermediaryNumber,
              hasMultipleIntermediaryEnrolments,
              isCurrentIntermediaryAccount,
              isValid = true,
              moreThanOnePreviousReg = false,
              unusableStatus = true,
              noChangesMade = false,
              config.intermediaryYourAccountUrl
            )(request, messages(application)).toString
          }
        }
      }
    }

    ".onSubmit" - {

      "must send the amended registration, audit success event then redirect when registration succeeds" in {

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val userAnswers: UserAnswers = completeUserAnswersWithVatInfo
          .set(OriginalRegistrationQuery(intermediaryNumber), registrationWrapperWithNiAddress.etmpDisplayRegistration).success.value

        when(mockSessionRepository.set(any())) thenReturn true.toFuture
        when(mockRegistrationService.amendRegistration(any(), any(), any(), any(), any(), any())(any())) thenReturn Right(amendRegistrationResponse).toFuture
        doNothing().when(mockAuditService).audit(any())(any(), any())

        val application = applicationBuilder(userAnswers = Some(userAnswers), registrationWrapper = Some(registrationWrapperWithNiAddress))
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
        val userAnswers: UserAnswers = completeUserAnswersWithVatInfo
          .set(OriginalRegistrationQuery(intermediaryNumber), registrationWrapperWithNiAddress.etmpDisplayRegistration).success.value

        when(mockSessionRepository.set(any())) thenReturn true.toFuture
        when(mockRegistrationService.amendRegistration(any(), any(), any(), any(), any(), any())(any())) thenReturn Left(InternalServerError).toFuture
        doNothing().when(mockAuditService).audit(any())(any(), any())

        val application = applicationBuilder(userAnswers = Some(userAnswers), registrationWrapper = Some(registrationWrapperWithNiAddress))
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

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.amend.routes.ErrorSubmittingAmendController.onPageLoad().url

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

      "must send the amended registration and throw exception when original registration is not found" in {

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]
        val userAnswers: UserAnswers = completeUserAnswersWithVatInfo

        when(mockSessionRepository.set(any())) thenReturn true.toFuture
        when(mockRegistrationService.amendRegistration(any(), any(), any(), any(), any(), any())(any())) thenReturn Left(InternalServerError).toFuture
        doNothing().when(mockAuditService).audit(any())(any(), any())

        val application = applicationBuilder(userAnswers = Some(userAnswers), registrationWrapper = Some(registrationWrapperWithNiAddress))
          .overrides(bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository))
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .overrides(bind[AuditService].toInstance(mockAuditService))
          .build()

        running(application) {

          val request = FakeRequest(POST, controllers.amend.routes.ChangeRegistrationController.onSubmit(EmptyWaypoints, incompletePrompt = false).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.amend.routes.ErrorSubmittingAmendController.onPageLoad().url

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

  private def getChangeRegistrationSummaryList(
                                                waypoints: Waypoints,
                                                answers: UserAnswers,
                                                checkOtherAddressNi: Boolean,
                                                page: CheckAnswersPage = ChangeRegistrationPage
                                              )(implicit msgs: Messages): Seq[SummaryListRow] = {

    val niAddressSummaryRow = NiAddressSummary.row(waypoints, answers, checkOtherAddressNi, page)
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

  private def getChangeRegistrationSummaryListWhenExcluded(
                                                            waypoints: Waypoints,
                                                            answers: UserAnswers,
                                                            checkOtherAddressNi: Boolean,
                                                            sourcePage: CheckAnswersPage = ChangeRegistrationPage
                                                          )(implicit msgs: Messages): Seq[SummaryListRow] = {

    val niAddressSummaryRow = NiAddressSummary.row(waypoints, answers, checkOtherAddressNi, sourcePage)
    val maybeHasTradingNameSummaryRow = HasTradingNameSummary.rowWithoutActions(answers)
    val tradingNameSummaryRow = TradingNameSummary.checkAnswersRowWithoutActions(answers)
    val maybeHasPreviouslyRegisteredAsIntermediaryRow = HasPreviouslyRegisteredAsIntermediarySummary
      .checkAnswersRowWithoutActions(answers)
    val previouslyRegisteredAsIntermediaryRow = PreviousIntermediaryRegistrationsSummary.checkAnswersRowWithoutActions(answers, Seq(previousIntermediaryRegistration))
    val maybeHasFixedEstablishmentSummaryRow = HasFixedEstablishmentSummary.rowWithoutActions(answers)
    val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRowWithoutActions(answers)
    val contactDetailsFullNameRow = ContactDetailsSummary.rowContactName(waypoints, answers, sourcePage)
    val contactDetailsTelephoneNumberRow = ContactDetailsSummary.rowTelephoneNumber(waypoints, answers, sourcePage)
    val contactDetailsEmailAddressRow = ContactDetailsSummary.rowEmailAddress(waypoints, answers, sourcePage)
    val bankDetailsAccountNameRow = BankDetailsSummary.rowAccountName(waypoints, answers, sourcePage)
    val bankDetailsBicRow = BankDetailsSummary.rowBIC(waypoints, answers, sourcePage)
    val bankDetailsIbanRow = BankDetailsSummary.rowIBAN(waypoints, answers, sourcePage)

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
}
