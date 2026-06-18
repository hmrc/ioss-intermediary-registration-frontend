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

package controllers.actions

import base.SpecBase
import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.routes
import models.domain.VatCustomerInfo
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.{CeasedTrade, FailsToComply, NoLongerMeetsConditions, NoLongerSupplies, Reversal, TransferringMSID, VoluntarilyLeaves}
import models.etmp.display.{EtmpDisplayRegistration, RegistrationWrapper}
import models.requests.AuthenticatedIdentifierRequest
import models.responses.{ErrorResponse, InternalServerError}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import utils.FutureSyntax.FutureOps

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class CheckRegistrationFilterSpec extends SpecBase with BeforeAndAfterEach {

  private val intermediaryEnrolmentKey = "HMRC-IOSS-INT"
  private val enrolment: Enrolment = Enrolment(intermediaryEnrolmentKey, Seq.empty, "test", None)

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  class Harness(inAmend: Boolean, inRejoin: Boolean, restrictExcludedAmend: Boolean, restrictNiVatBusinessAddress: Boolean, config: FrontendAppConfig, registrationConnector: RegistrationConnector)
    extends CheckRegistrationFilterImpl(inAmend, inRejoin, restrictExcludedAmend, restrictNiVatBusinessAddress, config, registrationConnector) {
    def callFilter[A](request: AuthenticatedIdentifierRequest[A]): Future[Option[Result]] =
      filter(request)
  }

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationConnector)
  }

  ".filter" - {

    "must redirect to Already Registered Controller when an existing Intermediary enrolment is found" in {

      val app = applicationBuilder(None).build()

      running(app) {
        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(enrolment)), None, 1, None, None, None)
        val controller = new Harness(false, false, false, false, config, mockRegistrationConnector)

        val result = controller.callFilter(request).futureValue

        result mustBe Some(Redirect(routes.AlreadyRegisteredController.onPageLoad().url))
      }

    }

    "must return None when an existing Intermediary enrolment is not found" in {

      val app = applicationBuilder(None).build()

      running(app) {
        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None, 1, None, None, None)
        val controller = new Harness(false, false, false, false, config, mockRegistrationConnector)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }
    }

    "when in amend" - {

      "must redirect to Not Registered page when an existing Intermediary enrolment is not found" in {

        val app = applicationBuilder(None).build()

        running(app) {
          val config = app.injector.instanceOf[FrontendAppConfig]
          val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None, 1, None, None, None)
          val controller = new Harness(true, false, false, false, config, mockRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result mustBe Some(Redirect(routes.NotRegisteredController.onPageLoad().url))
          verifyNoInteractions(mockRegistrationConnector)
        }
      }

      "must redirect to your intermediary account page when no exclusions are present and" +
        " restrictExcludedAmend is true and restrictNiVatBusinessAddress is true" in {

        val nonExcludedRegistrationWrapper: RegistrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value.copy(
          etmpDisplayRegistration = arbitraryRegistrationWrapper.arbitrary.sample.value.etmpDisplayRegistration.copy(
            exclusions = Seq.empty
          )
        )

        when(mockRegistrationConnector.displayRegistration(any())(any())) thenReturn Right(nonExcludedRegistrationWrapper).toFuture

        val app = applicationBuilder(None).build()

        running(app) {
          val config = app.injector.instanceOf[FrontendAppConfig]
          val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(enrolment)), None, 1, None, None, Some(intermediaryNumber))
          val controller = new Harness(true, false, true, true, config, mockRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result mustBe Some(Redirect(config.intermediaryYourAccountUrl))
          verify(mockRegistrationConnector, times(1)).displayRegistration(eqTo(intermediaryNumber))(any())
        }
      }

      "must return None when exclusions are present and" +
        " restrictExcludedAmend is false and restrictNiVatBusinessAddress is false" in {

        val nonExcludedRegistrationWrapper: RegistrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value.copy(
          etmpDisplayRegistration = arbitraryRegistrationWrapper.arbitrary.sample.value.etmpDisplayRegistration.copy(
            exclusions = Seq.empty
          )
        )

        when(mockRegistrationConnector.displayRegistration(any())(any())) thenReturn Right(nonExcludedRegistrationWrapper).toFuture

        val app = applicationBuilder(None).build()

        running(app) {
          val config = app.injector.instanceOf[FrontendAppConfig]
          val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(enrolment)), None, 1, None, None, Some(intermediaryNumber))
          val controller = new Harness(true, false, false, false, config, mockRegistrationConnector)

          val result = controller.callFilter(request).futureValue

          result mustBe None
          verify(mockRegistrationConnector, times(1)).displayRegistration(eqTo(intermediaryNumber))(any())
        }
      }

      "and an exclusion is present" - {

        "must return None when exclusion is Reversal" in {

          val excludedRegistration: EtmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value
            .copy(exclusions = Seq(EtmpExclusion(
              exclusionReason = Reversal,
              effectiveDate = LocalDate.now(stubClockAtArbitraryDate),
              decisionDate = LocalDate.now(stubClockAtArbitraryDate),
              quarantine = false
            )))

          val excludedRegistrationWrapper: RegistrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value
            .copy(etmpDisplayRegistration = excludedRegistration)

          when(mockRegistrationConnector.displayRegistration(any())(any())) thenReturn Right(excludedRegistrationWrapper).toFuture

          val app = applicationBuilder(None)
            .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
            .build()

          running(app) {
            val config = app.injector.instanceOf[FrontendAppConfig]
            val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(enrolment)), None, 1, None, None, Some(intermediaryNumber))
            val controller = new Harness(true, false, false, false, config, mockRegistrationConnector)

            val result = controller.callFilter(request).futureValue

            result mustBe None
            verify(mockRegistrationConnector, times(1)).displayRegistration(eqTo(intermediaryNumber))(any())
          }
        }

        Seq(
          NoLongerSupplies,
          CeasedTrade,
          NoLongerMeetsConditions,
          FailsToComply,
          VoluntarilyLeaves,
          TransferringMSID
        ).foreach { exclusionReason =>
          s"must redirect to their intermediary account page when exclusion reason is $exclusionReason and" +
            s" restrictExcludedAmend flag is true and restrictNiVatBusinessAddress is true and has an Ni vat address" in {

            val excludedRegistration: EtmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value
              .copy(exclusions = Seq(EtmpExclusion(
                exclusionReason = exclusionReason,
                effectiveDate = LocalDate.now(stubClockAtArbitraryDate),
                decisionDate = LocalDate.now(stubClockAtArbitraryDate),
                quarantine = false
              )))

            val niVatInfo: VatCustomerInfo = arbitraryVatCustomerInfo.arbitrary.sample.value.copy(
              desAddress = arbitraryVatCustomerInfo.arbitrary.sample.value.desAddress.copy(
                postCode = Some("BT12AA")
              )
            )

            val excludedRegistrationWrapper: RegistrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value
              .copy(
                vatInfo = niVatInfo,
                etmpDisplayRegistration = excludedRegistration
              )

            when(mockRegistrationConnector.displayRegistration(any())(any())) thenReturn Right(excludedRegistrationWrapper).toFuture

            val app = applicationBuilder(None)
              .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
              .build()

            running(app) {
              val config = app.injector.instanceOf[FrontendAppConfig]
              val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(enrolment)), None, 1, None, None, Some(intermediaryNumber))
              val controller = new Harness(true, false, true, true, config, mockRegistrationConnector)

              val result = controller.callFilter(request).futureValue

              result mustBe Some(Redirect(config.intermediaryYourAccountUrl))
              verify(mockRegistrationConnector, times(1)).displayRegistration(eqTo(intermediaryNumber))(any())
            }
          }

          s"must return None when exclusion reason is $exclusionReason and" +
            s" restrictExcludedAmend flag is true and restrictNiVatBusinessAddress is true and has a nonNi vat address" in {

            val excludedRegistration: EtmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value
              .copy(exclusions = Seq(EtmpExclusion(
                exclusionReason = exclusionReason,
                effectiveDate = LocalDate.now(stubClockAtArbitraryDate),
                decisionDate = LocalDate.now(stubClockAtArbitraryDate),
                quarantine = false
              )))

            val niVatInfo: VatCustomerInfo = arbitraryVatCustomerInfo.arbitrary.sample.value.copy(
              desAddress = arbitraryVatCustomerInfo.arbitrary.sample.value.desAddress.copy(
                postCode = Some("ET12AA")
              )
            )

            val excludedRegistrationWrapper: RegistrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value
              .copy(
                vatInfo = niVatInfo,
                etmpDisplayRegistration = excludedRegistration
              )

            when(mockRegistrationConnector.displayRegistration(any())(any())) thenReturn Right(excludedRegistrationWrapper).toFuture

            val app = applicationBuilder(None)
              .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
              .build()

            running(app) {
              val config = app.injector.instanceOf[FrontendAppConfig]
              val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(enrolment)), None, 1, None, None, Some(intermediaryNumber))
              val controller = new Harness(true, false, true, true, config, mockRegistrationConnector)

              val result = controller.callFilter(request).futureValue

              result mustBe None
              verify(mockRegistrationConnector, times(1)).displayRegistration(eqTo(intermediaryNumber))(any())
            }
          }
        }
      }

      "must throw an Exception when there is an error retrieving a registration" in {

        val error: ErrorResponse = InternalServerError
        val errorMessage: String = s"There was an error retrieving Registration with error response: ${error.body}."

        when(mockRegistrationConnector.displayRegistration(any())(any())) thenReturn Left(error).toFuture

        val app = applicationBuilder(None)
          .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
          .build()

        running(app) {
          val config = app.injector.instanceOf[FrontendAppConfig]
          val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(enrolment)), None, 1, None, None, Some(intermediaryNumber))
          val controller = new Harness(true, false, true, false, config, mockRegistrationConnector)

          val result = controller.callFilter(request).failed

          whenReady(result) { exp =>
            exp mustBe a[Exception]
            exp.getMessage mustBe errorMessage
          }
          verify(mockRegistrationConnector, times(1)).displayRegistration(eqTo(intermediaryNumber))(any())
        }
      }

      "must throw an Illegal State Exception when Intermediary number is missing" in {

        val errorMessage: String = "No Intermediary number found, must have an Intermediary number."

        val app = applicationBuilder(None).build()

        running(app) {
          val config = app.injector.instanceOf[FrontendAppConfig]
          val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(enrolment)), None, 1, None, None, None)
          val controller = new Harness(true, false, false, false, config, mockRegistrationConnector)

          intercept[IllegalStateException] {
            controller.callFilter(request).failed
          }.getMessage mustBe errorMessage

          verifyNoInteractions(mockRegistrationConnector)
        }
      }
    }
  }
}
