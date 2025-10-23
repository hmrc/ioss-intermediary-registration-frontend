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
import models.core.{Match, TraderId}
import models.requests.AuthenticatedDataRequest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.prop.TableDrivenPropertyChecks.*
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.domain.Vrn

import java.time.{Clock, LocalDate}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckOtherCountryRegistrationFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {


  def createMatchResponse(
                           traderId: TraderId = TraderId("IN333333333"),
                           exclusionEffectiveDate: Option[String] = None,
                           exclusionStatusCode: Option[Int] = None
                         ): Match = Match(
    traderId = traderId,
    intermediary = None,
    memberState = "DE",
    exclusionStatusCode = exclusionStatusCode,
    exclusionDecisionDate = None,
    exclusionEffectiveDate = exclusionEffectiveDate,
    nonCompliantReturns = None,
    nonCompliantPayments = None
  )

  override def beforeEach(): Unit = {
    reset(mockCoreRegistrationValidationService)
  }

  class Harness(service: CoreRegistrationValidationService, clock: Clock, inAmend: Boolean) extends CheckOtherCountryRegistrationFilterImpl(service, clock, inAmend) {
    def callFilter(request: AuthenticatedDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  private val mockCoreRegistrationValidationService = mock[CoreRegistrationValidationService]

  ".filter" - {

    "When the Match Trader ID is an intermediary (starts IN)" - {

      "And Match Type is an active trader must redirect to SchemeStillActive page" in {

        val vrn = Vrn("333333331")
        val app = applicationBuilder()
          .overrides(
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          ).build()

        val testConditions = Table(
          ("ReasonCode"),
          (None),
          (Some(-1)),
        )

        forAll(testConditions) { (reasonCode) =>
          running(app) {

            val activeIntermediaryMatch = createMatchResponse(
              exclusionStatusCode = reasonCode
            )

            when(mockCoreRegistrationValidationService.searchUkVrn(eqTo(vrn))(any(), any())) thenReturn
              Future.successful(Option(activeIntermediaryMatch))

            val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), emptyUserAnswers, None, 1, None, None, None, None)

            val controller = new Harness(mockCoreRegistrationValidationService, stubClockAtArbitraryDate, inAmend = false)

            val result = controller.callFilter(request).futureValue

            result `mustBe` Some(Redirect(controllers.filters.routes.SchemeStillActiveController.onPageLoad(activeIntermediaryMatch.memberState).url))
          }
        }
      }

      "And Match Type is a quarantined trader must redirect to OtherCountryExcludedAndQuarantined page" in {

        val vrn = Vrn("333333331")
        val app = applicationBuilder()
          .overrides(
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          ).build()

        running(app) {

          val quarantinedIntermediaryMatch = createMatchResponse(
            exclusionStatusCode = Some(4),
            exclusionEffectiveDate = Some("2022-10-10")
          )

          when(mockCoreRegistrationValidationService.searchUkVrn(eqTo(vrn))(any(), any())) thenReturn
            Future.successful(Option(quarantinedIntermediaryMatch))

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), emptyUserAnswers, None, 1, None, None, None, None)

          val controller = new Harness(mockCoreRegistrationValidationService, stubClockAtArbitraryDate, inAmend = false)

          val result = controller.callFilter(request).futureValue

          result `mustBe` Some(Redirect(controllers.filters.routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(quarantinedIntermediaryMatch.memberState, quarantinedIntermediaryMatch.getEffectiveDate).url))
        }
      }

      "And an Exclusion code of 4 is given for a quarantined trader must redirect to OtherCountryExcludedAndQuarantined page" in {

        val vrn = Vrn("333333331")
        val app = applicationBuilder()
          .overrides(
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          ).build()

        running(app) {

          val quarantinedIntermediaryMatch = createMatchResponse(
            exclusionStatusCode = Some(4),
            exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(1).toString)
          )

          when(mockCoreRegistrationValidationService.searchUkVrn(eqTo(vrn))(any(), any())) thenReturn
            Future.successful(Option(quarantinedIntermediaryMatch))

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), emptyUserAnswers, None, 1, None, None, None, None)

          val controller = new Harness(mockCoreRegistrationValidationService, stubClockAtArbitraryDate, inAmend = false)

          val result = controller.callFilter(request).futureValue

          result `mustBe` Some(Redirect(controllers.filters.routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(quarantinedIntermediaryMatch.memberState, quarantinedIntermediaryMatch.getEffectiveDate).url))
        }
      }

      "And no exclusion effective date is given for a quarantined trader, throw an illegal state exception" in {
        val timeout = 30

        val vrn = Vrn("333333331")
        val app = applicationBuilder(None)
          .configure(
            "features.other-country-reg-validation-enabled" -> true
          )
          .overrides(
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          ).build()

        running(app) {

          val quarantinedIntermediaryMatch = createMatchResponse(
            exclusionEffectiveDate = None,
            exclusionStatusCode = Some(4)
          )

          when(mockCoreRegistrationValidationService.searchUkVrn(eqTo(vrn))(any(), any())) thenReturn Future.successful(Option(quarantinedIntermediaryMatch))

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), emptyUserAnswers, None, 1, None, None, None, None)

          val controller = new Harness(mockCoreRegistrationValidationService, stubClockAtArbitraryDate, inAmend = false)

          val result = controller.callFilter(request).failed

          whenReady(result, Timeout(Span(timeout, Seconds))) { exp =>
            exp mustBe a[IllegalStateException]
            exp.getMessage must include(s"Exclusion with status ${quarantinedIntermediaryMatch.exclusionStatusCode} didn't include an expected exclusion effective date")
          }
        }
      }

      "And Match Type is neither active trader, quarantined or an exclusion code of 4 must return None NOT preventing the user journey" in {

        val vrn = Vrn("333333331")
        val app = applicationBuilder()
          .overrides(
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          ).build()

        val testConditions = Table(
          ("Reason code"),
          1,
          2,
          3,
          5,
          6
        )

        forAll(testConditions) { (reasonCode) =>
          running(app) {

            val quarantinedIntermediaryMatch = createMatchResponse(
              exclusionStatusCode = Some(reasonCode)
            )

            when(mockCoreRegistrationValidationService.searchUkVrn(eqTo(vrn))(any(), any())) thenReturn
              Future.successful(Option(quarantinedIntermediaryMatch))

            val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), emptyUserAnswers, None, 1, None, None, None, None)

            val controller = new Harness(mockCoreRegistrationValidationService, stubClockAtArbitraryDate, inAmend = false)

            val result = controller.callFilter(request).futureValue

            result `mustBe` None
          }
        }
      }

    }

    "When the return data is not a Intermediaries Trader ID (starting IN)" - {

      "And Match Type is any match type or with exclusion code 4 must return None NOT preventing the user journey" in {

        val vrn = Vrn("333333331")
        val app = applicationBuilder()
          .overrides(
            bind[CoreRegistrationValidationService].toInstance(mockCoreRegistrationValidationService)
          ).build()

        running(app) {

          val anyNonIntermediaryMatch = createMatchResponse(
            traderId = TraderId("12345"),
            exclusionStatusCode = Some(4)
          )

          when(mockCoreRegistrationValidationService.searchUkVrn(eqTo(vrn))(any(), any())) thenReturn
            Future.successful(Option(anyNonIntermediaryMatch))

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), emptyUserAnswers, None, 1, None, None, None, None)

          val controller = new Harness(mockCoreRegistrationValidationService, stubClockAtArbitraryDate, inAmend = false)

          val result = controller.callFilter(request).futureValue

          result `mustBe` None
        }
      }

    }

  }

}
