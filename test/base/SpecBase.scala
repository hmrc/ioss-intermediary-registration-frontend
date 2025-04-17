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
import models.iossRegistration.IossEtmpDisplayRegistration
import models.ossRegistration.OssRegistration
import models.{DesAddress, UserAnswers}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages.filters.RegisteredForIossIntermediaryInEuPage
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.Credentials
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
  
  val arbitraryInstant: Instant = arbitraryDate.arbitrary.sample.value.atStartOfDay(ZoneId.systemDefault()).toInstant
  val stubClockAtArbitraryDate: Clock = Clock.fixed(arbitraryInstant, ZoneId.systemDefault())

  def testCredentials: Credentials = Credentials(userAnswersId, "GGW")
  def emptyUserAnswers : UserAnswers = UserAnswers(userAnswersId, lastUpdated = arbitraryInstant)

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val vatCustomerInfo: VatCustomerInfo =
    VatCustomerInfo(
      registrationDate = LocalDate.now(stubClockAtArbitraryDate),
      desAddress = DesAddress("Line1", None, None, None, None, Some("AA11 1AA"), "GB"),
      partOfVatGroup = false,
      organisationName = Some("Company name"),
      individualName = None,
      singleMarketIndicator = true,
      deregistrationDecisionDate = None,
      overseasIndicator = false
    )

  val userAnswersId: String = "12345-credId"
  val vrn: Vrn = Vrn("123456789")
  val iossNumber: String = "IM9001234567"

  def emptyUserAnswersWithVatInfo: UserAnswers = emptyUserAnswers.copy(vatInfo = Some(vatCustomerInfo))
  def basicUserAnswersWithVatInfo: UserAnswers = emptyUserAnswers.set(RegisteredForIossIntermediaryInEuPage, false).success.value.copy(vatInfo = Some(vatCustomerInfo))

  protected def applicationBuilder(
                                    userAnswers: Option[UserAnswers] = None,
                                    iossNumber: Option[String] = None,
                                    numberOfIossRegistrations: Int = 0,
                                    iossEtmpDisplayRegistration: Option[IossEtmpDisplayRegistration] = None,
                                    ossRegistration: Option[OssRegistration] = None
                                  ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[AuthenticatedIdentifierAction].toInstance(new FakeAuthenticatedIdentifierAction(iossNumber, numberOfIossRegistrations, iossEtmpDisplayRegistration, ossRegistration)),
        bind[AuthenticatedDataRetrievalAction].toInstance(new FakeAuthenticatedDataRetrievalAction(userAnswers, vrn)),
        bind[UnauthenticatedDataRetrievalAction].toInstance(new FakeUnauthenticatedDataRetrievalAction(userAnswers)),
        bind[AuthenticatedDataRequiredActionImpl].toInstance(FakeAuthenticatedDataRequiredAction(userAnswers)),
      )

  lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("", "").withCSRFToken.asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]
}
