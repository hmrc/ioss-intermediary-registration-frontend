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

package base

import controllers.actions.*
import generators.Generators
import models.domain.VatCustomerInfo
import models.emailVerification.{EmailVerificationRequest, VerifyEmail}
import models.etmp.display.RegistrationWrapper
import models.iossRegistration.IossEtmpDisplayRegistration
import models.ossRegistration.*
import models.{BankDetails, Bic, ContactDetails, DesAddress, Iban, Index, TradingName, UserAnswers}
import org.scalatest
import org.scalatest.EitherValues.*
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.euDetails.HasFixedEstablishmentPage
import pages.filters.RegisteredForIossIntermediaryInEuPage
import pages.previousIntermediaryRegistrations.HasPreviouslyRegisteredAsIntermediaryPage
import pages.tradingNames.{HasTradingNamePage, TradingNamePage}
import pages.{BankDetailsPage, ContactDetailsPage, EmptyWaypoints, Waypoints}
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Writes
import play.api.mvc.AnyContentAsEmpty
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.FakeRequest
import testutils.RegistrationData.etmpDisplayRegistration
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.domain.Vrn

import java.time.{Clock, Instant, LocalDate, ZoneId}

trait SpecBase
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with Generators {

  def countryIndex(index: Int): Index = Index(index)

  val userAnswersId: String = "12345-credId"
  val vrn: Vrn = Vrn("123456789")

  lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("", "/endpoint").withCSRFToken.asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]

  def testCredentials: Credentials = Credentials(userAnswersId, "GGW")

  def testEnrolments: Enrolments = Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", vrn.vrn)), "Activated")))

  def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId, lastUpdated = arbitraryInstant)

  def emptyUserAnswersWithVatInfo: UserAnswers = emptyUserAnswers.copy(vatInfo = Some(vatCustomerInfo))

  def basicUserAnswersWithVatInfo: UserAnswers = emptyUserAnswersWithVatInfo
    .set(RegisteredForIossIntermediaryInEuPage, false).success.value

  val iban: Iban = Iban("GB33BUKB20201555555555").value
  val bic: Bic = Bic("ABCDGB2A").get

  def completeUserAnswersWithVatInfo: UserAnswers =
    basicUserAnswersWithVatInfo
      .set(HasTradingNamePage, true).success.value
      .set(TradingNamePage(countryIndex(0)), TradingName("Test trading name")).success.value
      .set(HasPreviouslyRegisteredAsIntermediaryPage, false).success.value
      .set(HasFixedEstablishmentPage, false).success.value
      .set(ContactDetailsPage, ContactDetails("fullName", "0123456789", "testEmail@example.com")).success.value
      .set(BankDetailsPage, BankDetails("Account name", Some(bic), iban)).success.value

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val arbitraryInstant: Instant = arbitraryDate.arbitrary.sample.value.atStartOfDay(ZoneId.systemDefault()).toInstant
  val stubClockAtArbitraryDate: Clock = Clock.fixed(arbitraryInstant, ZoneId.systemDefault())

  val iossNumber: String = "IM9001234567"

  val intermediaryNumber: String = "IN9001234567"

  val waypoints: Waypoints = EmptyWaypoints

  val vatCustomerInfo: VatCustomerInfo =
    VatCustomerInfo(
      registrationDate = LocalDate.now(stubClockAtArbitraryDate),
      desAddress = arbitraryDesAddress.arbitrary.sample.value.copy(postCode = Some("BT11BT")),
      organisationName = Some("Company name"),
      individualName = None,
      singleMarketIndicator = true,
      deregistrationDecisionDate = None
    )

  val registrationWrapper: RegistrationWrapper = RegistrationWrapper(vatCustomerInfo, etmpDisplayRegistration)

  protected def applicationBuilder(
                                    userAnswers: Option[UserAnswers] = None,
                                    clock: Option[Clock] = None,
                                    iossNumber: Option[String] = None,
                                    enrolments: Option[Enrolments] = None,
                                    numberOfIossRegistrations: Int = 0,
                                    iossEtmpDisplayRegistration: Option[IossEtmpDisplayRegistration] = None,
                                    ossRegistration: Option[OssRegistration] = None,
                                    intermediaryNumber: Option[String] = None,
                                    registrationWrapper: Option[RegistrationWrapper] = None
                                  ): GuiceApplicationBuilder = {

    val clockToBind = clock.getOrElse(stubClockAtArbitraryDate)

    new GuiceApplicationBuilder()
      .overrides(
        bind[AuthenticatedIdentifierAction].toInstance(new FakeAuthenticatedIdentifierAction(iossNumber, numberOfIossRegistrations, iossEtmpDisplayRegistration, ossRegistration, intermediaryNumber)),
        bind[AuthenticatedDataRetrievalAction].toInstance(new FakeAuthenticatedDataRetrievalAction(userAnswers, vrn)),
        bind[AuthenticatedDataRequiredAction].toInstance(new FakeAuthenticatedDataRequiredActionProvider(userAnswers, registrationWrapper)),
        bind[UnauthenticatedDataRetrievalAction].toInstance(new FakeUnauthenticatedDataRetrievalAction(userAnswers)),
        bind[CheckRegistrationFilterProvider].toInstance(new FakeCheckRegistrationFilterProvider()),
        bind[CheckEmailVerificationFilterProvider].toInstance(new FakeCheckEmailVerificationFilter()),
        bind[CheckOtherCountryRegistrationFilter].toInstance(new FakeCheckOtherCountryRegistrationFilter(stubClockAtArbitraryDate)),
        bind[SaveForLaterRetrievalAction].toInstance(new FakeSaveForLaterRetrievalAction(userAnswers, vrn)),
        bind[IntermediaryRequiredAction].toInstance(new FakeIntermediaryRequiredAction(userAnswers, enrolments, iossEtmpDisplayRegistration, ossRegistration, numberOfIossRegistrations, registrationWrapper.getOrElse(this.registrationWrapper))),
        bind[Clock].toInstance(clockToBind)
      )
  }


  private val ossBankDetails = BankDetails(
    accountName = "OSS Account Name",
    bic = Bic("OSSBIC123"),
    iban = Iban("GB33BUKB20201555555555").value
  )

  private val ossContactDetail = OssContactDetails(
    fullName = "Rory Beans",
    telephoneNumber = "01234567890",
    emailAddress = "roryBeans@beans.com"
  )

  val ossRegistration: Option[OssRegistration] = Some(OssRegistration(
    vrn = vrn,
    registeredCompanyName = "Test Company",
    tradingNames = Seq("Trade1", "Trade2"),
    vatDetails = mock[OssVatDetails],
    euRegistrations = Seq(mock[OssEuTaxRegistration]),
    contactDetails = ossContactDetail,
    websites = Seq("https://example.com"),
    commencementDate = LocalDate.now(),
    previousRegistrations = Seq(mock[OssPreviousRegistration]),
    bankDetails = ossBankDetails,
    isOnlineMarketplace = false,
    niPresence = None,
    dateOfFirstSale = Some(LocalDate.now()),
    submissionReceived = Some(Instant.now()),
    lastUpdated = Some(Instant.now()),
    excludedTrader = None,
    transferringMsidEffectiveFromDate = None,
    nonCompliantReturns = None,
    nonCompliantPayments = None,
    adminUse = mock[OssAdminUse]
  ))

  val contactDetails: ContactDetails = ContactDetails(
    fullName = "name",
    telephoneNumber = "0111 2223334",
    emailAddress = "email@example.com"
  )

  val verifyEmail: VerifyEmail = VerifyEmail(
    address = contactDetails.emailAddress,
    enterUrl = "/pay-vat-on-goods-sold-to-eu/northern-ireland-register/business-contact-details"
  )

  def emailVerificationRequest: EmailVerificationRequest = {
    val serviceUrl: String = "/pay-clients-vat-on-eu-sales/register-import-one-stop-shop-intermediary"

    EmailVerificationRequest(
      credId = userAnswersId,
      continueUrl = s"$serviceUrl/bank-account-details",
      origin = "IOSS-Intermediary",
      deskproServiceName = Some("ioss-intermediary-registration-frontend"),
      accessibilityStatementUrl = "/register-import-one-stop-shop-intermediary",
      pageTitle = Some("Register to manage your clients’ Import One Stop Shop VAT"),
      backUrl = Some(s"$serviceUrl/business-contact-details"),
      email = Some(verifyEmail)
    )
  }
}
