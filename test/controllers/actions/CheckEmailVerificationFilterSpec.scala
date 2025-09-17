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
import controllers.routes
import models.UserAnswers
import models.emailVerification.PasscodeAttemptsStatus.*
import models.requests.AuthenticatedDataRequest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.{ContactDetailsPage, JourneyRecoveryPage}
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import services.{EmailVerificationService, SaveForLaterService}
import uk.gov.hmrc.auth.core.Enrolments
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckEmailVerificationFilterSpec extends SpecBase with MockitoSugar with EitherValues with BeforeAndAfterEach {

  class Harness(
                 inAmend: Boolean,
                 frontendAppConfig: FrontendAppConfig,
                 emailVerificationService: EmailVerificationService,
                 saveForLaterService: SaveForLaterService
               )
    extends CheckEmailVerificationFilterImpl(waypoints, inAmend, frontendAppConfig, emailVerificationService, saveForLaterService) {
    def callFilter(request: AuthenticatedDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  private val mockEmailVerificationService: EmailVerificationService = mock[EmailVerificationService]
  private val mockSaveForLaterService: SaveForLaterService = mock[SaveForLaterService]

  private val validEmailAddressUserAnswers: UserAnswers = basicUserAnswersWithVatInfo
    .set(ContactDetailsPage, contactDetails).success.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockEmailVerificationService)
    Mockito.reset(mockSaveForLaterService)
  }

  ".filter" - {

    "when email verification enabled" - {

      "must return None if no email address is present" in {

        val app = applicationBuilder(None)
          .build()

        running(app) {

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), basicUserAnswersWithVatInfo, None, 1, None, None, None, None)
          val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

          val controller = new Harness(
            inAmend = false,
            frontendAppConfig = frontendAppConfig,
            emailVerificationService = mockEmailVerificationService,
            saveForLaterService = mockSaveForLaterService
          )

          val result = controller.callFilter(request).futureValue

          result must not be defined
          verifyNoInteractions(mockSaveForLaterService)
          verifyNoInteractions(mockEmailVerificationService)
        }
      }

      "must return None when an email address is verified" in {

        val app = applicationBuilder(None)
          .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
          .build()

        running(app) {

          when(mockEmailVerificationService.isEmailVerified(
            eqTo(contactDetails.emailAddress), eqTo(userAnswersId))(any())) thenReturn Verified.toFuture

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), validEmailAddressUserAnswers, None, 1, None, None, None, None)
          val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

          val controller = new Harness(
            inAmend = false,
            frontendAppConfig = frontendAppConfig,
            emailVerificationService = mockEmailVerificationService,
            saveForLaterService = mockSaveForLaterService
          )

          val result = controller.callFilter(request).futureValue

          result must not be defined
          verify(mockEmailVerificationService, times(1)).isEmailVerified(eqTo(contactDetails.emailAddress), eqTo(userAnswersId))(any())
          verifyNoInteractions(mockSaveForLaterService)
        }
      }

      "must redirect to Email Verification Codes Exceeded page when verification attempts on a single email are exceeded" in {

        val app = applicationBuilder(None)
          .configure("features.email-verification-enabled" -> "true")
          .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
          .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
          .build()

        running(app) {

          when(mockEmailVerificationService.isEmailVerified(
            eqTo(contactDetails.emailAddress), eqTo(userAnswersId))(any())) thenReturn LockedPasscodeForSingleEmail.toFuture

          when(mockSaveForLaterService.submitSavedUserAnswersAndRedirect(
            any(), any(), any())(any(), any(), any())) thenReturn Redirect(routes.EmailVerificationCodesExceededController.onPageLoad()).toFuture

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), validEmailAddressUserAnswers, None, 1, None, None, None, None)
          val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

          val controller = new Harness(
            inAmend = false,
            frontendAppConfig = frontendAppConfig,
            emailVerificationService = mockEmailVerificationService,
            saveForLaterService = mockSaveForLaterService
          )

          val result = controller.callFilter(request).futureValue

          result `mustBe` Some(Redirect(controllers.routes.EmailVerificationCodesExceededController.onPageLoad().url))
          verify(mockEmailVerificationService, times(1)).isEmailVerified(eqTo(contactDetails.emailAddress), eqTo(userAnswersId))(any())
          verify(mockSaveForLaterService, times(1)).submitSavedUserAnswersAndRedirect(any(), any(), any())(any(), any(), any())
        }
      }

      "must redirect to Email Verification Codes and Emails Exceeded page when verification attempts on maximum email addresses are exceeded" in {

        val app = applicationBuilder(None)
          .configure("features.email-verification-enabled" -> "true")
          .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
          .build()

        running(app) {

          when(mockEmailVerificationService.isEmailVerified(
            eqTo(contactDetails.emailAddress), eqTo(userAnswersId))(any())) thenReturn LockedTooManyLockedEmails.toFuture


          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), validEmailAddressUserAnswers, None, 1, None, None, None, None)
          val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

          val controller = new Harness(
            inAmend = false,
            frontendAppConfig = frontendAppConfig,
            emailVerificationService = mockEmailVerificationService,
            saveForLaterService = mockSaveForLaterService
          )

          val result = controller.callFilter(request).futureValue

          result `mustBe` Some(Redirect(controllers.routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad().url))
          verify(mockEmailVerificationService, times(1)).isEmailVerified(eqTo(contactDetails.emailAddress), eqTo(userAnswersId))(any())
          verifyNoInteractions(mockSaveForLaterService)
        }
      }

      "must redirect to Journey Recovery page when verification attempts on a single email are exceeded but Save For Later Service returns Redirect(JourneyRecovery)" in {

        val app = applicationBuilder(None)
          .configure("features.email-verification-enabled" -> "true")
          .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
          .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
          .build()

        running(app) {

          when(mockEmailVerificationService.isEmailVerified(
            eqTo(contactDetails.emailAddress), eqTo(userAnswersId))(any())) thenReturn LockedPasscodeForSingleEmail.toFuture

          when(mockSaveForLaterService.submitSavedUserAnswersAndRedirect(
            any(), any(), any())(any(), any(), any())) thenReturn Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), validEmailAddressUserAnswers, None, 1, None, None, None, None)
          val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

          val controller = new Harness(
            inAmend = false,
            frontendAppConfig = frontendAppConfig,
            emailVerificationService = mockEmailVerificationService,
            saveForLaterService = mockSaveForLaterService
          )

          val result = controller.callFilter(request).futureValue

          result `mustBe` Some(Redirect(JourneyRecoveryPage.route(waypoints).url))
          verify(mockEmailVerificationService, times(1)).isEmailVerified(eqTo(contactDetails.emailAddress), eqTo(userAnswersId))(any())
          verify(mockSaveForLaterService, times(1)).submitSavedUserAnswersAndRedirect(any(), any(), any())(any(), any(), any())
        }
      }
    }

    "when email verification disabled" - {

      "must return None if no email address is present" in {

        val app = applicationBuilder(None)
          .configure("features.email-verification-enabled" -> "false")
          .build()

        running(app) {

          val request = AuthenticatedDataRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), basicUserAnswersWithVatInfo, None, 1, None, None, None, None)
          val frontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

          val controller = new Harness(
            inAmend = false,
            frontendAppConfig = frontendAppConfig,
            emailVerificationService = mockEmailVerificationService,
            saveForLaterService = mockSaveForLaterService
          )

          val result = controller.callFilter(request).futureValue

          result must not be defined
          verifyNoInteractions(mockEmailVerificationService)
          verifyNoInteractions(mockSaveForLaterService)
        }
      }
    }
  }
}
