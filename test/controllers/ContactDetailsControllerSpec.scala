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
import config.FrontendAppConfig
import forms.ContactDetailsFormProvider
import models.emailVerification.EmailVerificationResponse
import models.emailVerification.PasscodeAttemptsStatus.{LockedPasscodeForSingleEmail, LockedTooManyLockedEmails, NotVerified, Verified}
import models.{BankDetails, CheckMode, ContactDetails, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoMoreInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ChangeRegistrationPage
import pages.{BankDetailsPage, ContactDetailsPage, EmptyWaypoints, Waypoint, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import services.{EmailVerificationService, SaveForLaterService}
import utils.FutureSyntax.FutureOps
import views.html.ContactDetailsView

class ContactDetailsControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val formProvider: ContactDetailsFormProvider = new ContactDetailsFormProvider()
  private val form: Form[ContactDetails] = formProvider()

  private lazy val contactDetailsRoute: String = routes.ContactDetailsController.onPageLoad(waypoints).url
  private lazy val amendBusinessContactDetailsRoute: String = routes.ContactDetailsController.onPageLoad(amendWaypoints).url

  private val amendWaypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
  private val contactDetails: ContactDetails = ContactDetails("name", "0111 2223334", "email@example.com")
  private val userAnswers: UserAnswers = basicUserAnswersWithVatInfo.set(ContactDetailsPage, contactDetails).success.value
  private val emptyWaypoints: Waypoints = EmptyWaypoints

  private val mockEmailVerificationService: EmailVerificationService = mock[EmailVerificationService]
  private val mockSaveForLaterService: SaveForLaterService = mock[SaveForLaterService]

  private def createEmailVerificationResponse(waypoints: Waypoints): EmailVerificationResponse = EmailVerificationResponse(
    redirectUri = routes.BankDetailsController.onPageLoad(waypoints).url
  )

  private val amendEmailVerificationResponse: EmailVerificationResponse = EmailVerificationResponse(
    redirectUri = controllers.amend.routes.ChangeRegistrationController.onPageLoad().url
  )


  private val bankDetails = BankDetails(
    accountName = "Account name",
    bic = Some(bic),
    iban = iban
  )

  override def beforeEach(): Unit = {
    Mockito.reset(mockEmailVerificationService)
    Mockito.reset(mockSaveForLaterService)
  }

  "ContactDetails Controller" - {

    "GET" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo)).build()

        running(application) {
          val request = FakeRequest(GET, contactDetailsRoute)

          val view = application.injector.instanceOf[ContactDetailsView]

          val result = route(application, request).value

          status(result) `mustBe` OK
          contentAsString(result) `mustBe` view(form, waypoints, None, 0, None)(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, contactDetailsRoute)

          val view = application.injector.instanceOf[ContactDetailsView]

          val result = route(application, request).value

          status(result) `mustBe` OK
          contentAsString(result) `mustBe` view(form.fill(contactDetails), waypoints, None, 0, None)(request, messages(application)).toString
        }
      }
    }

    "onSubmit" - {

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, contactDetailsRoute)
              .withFormUrlEncodedBody(("value", "invalid value"))

          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[ContactDetailsView]

          val result = route(application, request).value

          status(result) `mustBe` BAD_REQUEST
          contentAsString(result) `mustBe` view(boundForm, waypoints, None, 0, None)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, contactDetailsRoute)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, contactDetailsRoute)
              .withFormUrlEncodedBody(("fullName", "value 1"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@email.com"))

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "when email verification enabled" - {

        "must redirect to the next page if email is already verified and valid data is submitted" in {

          val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

          when(mockSessionRepository.set(any())) thenReturn true.toFuture
          when(mockEmailVerificationService.isEmailVerified(
            eqTo(emailVerificationRequest.email.get.address),
            eqTo(emailVerificationRequest.credId))(any())) thenReturn Verified.toFuture

          val application = applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
            .configure("features.email-verification-enabled" -> "true")
            .configure("features.enrolments-enabled" -> "false")
            .overrides(
              bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
              bind[EmailVerificationService].toInstance(mockEmailVerificationService)
            )
            .build()

          running(application) {
            val request =
              FakeRequest(POST, contactDetailsRoute)
                .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@example.com"))

            val result = route(application, request).value
            val expectedAnswers = basicUserAnswersWithVatInfo.set(ContactDetailsPage, contactDetails).success.value

            status(result) `mustBe` SEE_OTHER
            redirectLocation(result).value `mustBe` routes.BankDetailsController.onPageLoad(emptyWaypoints).url

            verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))

            verify(mockEmailVerificationService, times(1))
              .isEmailVerified(eqTo(emailVerificationRequest.email.get.address), eqTo(emailVerificationRequest.credId))(any())

            verify(mockEmailVerificationService, times(0))
              .createEmailVerificationRequest(
                eqTo(emptyWaypoints),
                eqTo(emailVerificationRequest.credId),
                eqTo(emailVerificationRequest.email.get.address),
                eqTo(emailVerificationRequest.pageTitle),
                eqTo(emailVerificationRequest.continueUrl))(any())
          }
        }

        "must redirect to the Contact Details page if email is not verified and valid data is submitted" in {

          val emailVerificationResponse = createEmailVerificationResponse(emptyWaypoints)

          val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

          when(mockSessionRepository.set(any())) thenReturn true.toFuture

          when(mockEmailVerificationService.isEmailVerified(
            emailAddress = any(),
            credId = any())(any())) thenReturn NotVerified.toFuture

          when(mockEmailVerificationService.createEmailVerificationRequest(
            waypoints = any(),
            credId = any(),
            emailAddress = any(),
            pageTitle = any(),
            continueUrl = any())(any())) thenReturn Right(emailVerificationResponse).toFuture

          val application =
            applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
              .configure("features.email-verification-enabled" -> "true")
              .configure("features.enrolments-enabled" -> "false")
              .overrides(
                bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
                bind[EmailVerificationService].toInstance(mockEmailVerificationService)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, contactDetailsRoute)
                .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@example.com"))

            val config = application.injector.instanceOf[FrontendAppConfig]
            val result = route(application, request).value
            val expectedAnswers = basicUserAnswersWithVatInfo.set(ContactDetailsPage, contactDetails).success.value

            val anEmailVerificationRequest = emailVerificationRequest.copy(
              pageTitle = Some("Register to manage your clients’ Import One Stop Shop VAT"),
              continueUrl = s"${config.loginContinueUrl}${emailVerificationRequest.continueUrl}"
            )

            status(result) `mustBe` SEE_OTHER

            redirectLocation(result).value `mustBe` config.emailVerificationUrl + emailVerificationResponse.redirectUri

            verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))

            verify(mockEmailVerificationService, times(1))
              .isEmailVerified(eqTo(anEmailVerificationRequest.email.value.address), eqTo(anEmailVerificationRequest.credId))(any())

            verify(mockEmailVerificationService, times(1))
              .createEmailVerificationRequest(
                waypoints = eqTo(emptyWaypoints),
                credId = eqTo(anEmailVerificationRequest.credId),
                emailAddress = eqTo(anEmailVerificationRequest.email.value.address),
                pageTitle = eqTo(anEmailVerificationRequest.pageTitle),
                continueUrl = eqTo(anEmailVerificationRequest.continueUrl))(any())
          }
        }

        "must redirect to the Bank Details page if if bank details are not completed" in {

          val emailVerificationResponse = EmailVerificationResponse(
            redirectUri = routes.BankDetailsController.onPageLoad().url
          )

          val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

          when(mockSessionRepository.set(any())) thenReturn true.toFuture

          when(mockEmailVerificationService.isEmailVerified(
            emailAddress = any(),
            credId = any())(any())) thenReturn NotVerified.toFuture

          when(mockEmailVerificationService.createEmailVerificationRequest(
            waypoints = any(),
            credId = any(),
            emailAddress = any(),
            pageTitle = any(),
            continueUrl = any())(any())) thenReturn Right(emailVerificationResponse).toFuture

          val application =
            applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
              .configure("features.email-verification-enabled" -> "true")
              .configure("features.enrolments-enabled" -> "false")
              .overrides(
                bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
                bind[EmailVerificationService].toInstance(mockEmailVerificationService)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, contactDetailsRoute)
                .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@example.com"))

            val config = application.injector.instanceOf[FrontendAppConfig]
            val result = route(application, request).value
            val expectedAnswers = basicUserAnswersWithVatInfo.set(ContactDetailsPage, contactDetails).success.value

            val anEmailVerificationRequest = emailVerificationRequest.copy(
              pageTitle = Some("Register to manage your clients’ Import One Stop Shop VAT"),
              continueUrl = s"${config.loginContinueUrl}${emailVerificationRequest.continueUrl}"
            )

            status(result) `mustBe` SEE_OTHER

            redirectLocation(result).value `mustBe` config.emailVerificationUrl + emailVerificationResponse.redirectUri

            verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))

            verify(mockEmailVerificationService, times(1))
              .isEmailVerified(eqTo(emailVerificationRequest.email.value.address), eqTo(emailVerificationRequest.credId))(any())

            verify(mockEmailVerificationService, times(1))
              .createEmailVerificationRequest(
                waypoints = eqTo(emptyWaypoints),
                credId = eqTo(anEmailVerificationRequest.credId),
                emailAddress = eqTo(anEmailVerificationRequest.email.value.address),
                pageTitle = eqTo(anEmailVerificationRequest.pageTitle),
                continueUrl = eqTo(anEmailVerificationRequest.continueUrl))(any())
          }
        }

        "must redirect to the CheckYourAnswersPage if bank details are completed" in {

          val emailVerificationResponse = EmailVerificationResponse(
            redirectUri = routes.CheckYourAnswersController.onPageLoad().url
          )

          val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

          when(mockSessionRepository.set(any())) thenReturn true.toFuture

          when(mockEmailVerificationService.isEmailVerified(
            emailAddress = any(),
            credId = any())(any())) thenReturn NotVerified.toFuture

          when(mockEmailVerificationService.createEmailVerificationRequest(
            waypoints = any(),
            credId = any(),
            emailAddress = any(),
            pageTitle = any(),
            continueUrl = any())(any())) thenReturn Right(emailVerificationResponse).toFuture

          val application =
            applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo))
              .configure("features.email-verification-enabled" -> "true")
              .configure("features.enrolments-enabled" -> "false")
              .overrides(
                bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
                bind[EmailVerificationService].toInstance(mockEmailVerificationService)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, contactDetailsRoute)
                .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@example.com"))

            val config = application.injector.instanceOf[FrontendAppConfig]
            val result = route(application, request).value
            val expectedAnswers = completeUserAnswersWithVatInfo
              .set(ContactDetailsPage, contactDetails).success.value
              .set(BankDetailsPage, bankDetails).success.value

            val anEmailVerificationRequest = emailVerificationRequest.copy(
              pageTitle = Some("Register to manage your clients’ Import One Stop Shop VAT"),
              continueUrl = s"${config.loginContinueUrl}/pay-clients-vat-on-eu-sales/register-import-one-stop-shop-intermediary/check-your-answers"
            )

            status(result) `mustBe` SEE_OTHER

            redirectLocation(result).value `mustBe` config.emailVerificationUrl + emailVerificationResponse.redirectUri

            verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))

            verify(mockEmailVerificationService, times(1))
              .isEmailVerified(eqTo(emailVerificationRequest.email.value.address), eqTo(emailVerificationRequest.credId))(any())

            verify(mockEmailVerificationService, times(1))
              .createEmailVerificationRequest(
                waypoints = eqTo(emptyWaypoints),
                credId = eqTo(anEmailVerificationRequest.credId),
                emailAddress = eqTo(anEmailVerificationRequest.email.value.address),
                pageTitle = eqTo(anEmailVerificationRequest.pageTitle),
                continueUrl = eqTo(anEmailVerificationRequest.continueUrl))(any())
          }
        }

        "must redirect to the Email Verification Codes Exceeded page if valid data is submitted but" +
          " verification attempts on a single email are exceeded" in {

          val expectedRedirect: Call = routes.EmailVerificationCodesExceededController.onPageLoad()

          when(mockEmailVerificationService.isEmailVerified(
            eqTo(emailVerificationRequest.email.get.address),
            eqTo(emailVerificationRequest.credId))(any())) thenReturn LockedPasscodeForSingleEmail.toFuture

          when(mockSaveForLaterService
            .saveUserAnswers(any(), any(), any())(any(), any(), any())
          ) thenReturn Redirect(expectedRedirect).toFuture

          val application =
            applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
              .configure("features.email-verification-enabled" -> "true")
              .configure("features.enrolments-enabled" -> "false")
              .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
              .overrides(bind[SaveForLaterService].toInstance(mockSaveForLaterService))
              .build()

          running(application) {

            val request =
              FakeRequest(POST, contactDetailsRoute)
                .withFormUrlEncodedBody(
                  ("fullName", "name"),
                  ("telephoneNumber", "0111 2223334"),
                  ("emailAddress", "email@example.com"))

            val result = route(application, request).value

            status(result) `mustBe` SEE_OTHER

            val actual: String = redirectLocation(result).value

            actual `mustBe` expectedRedirect.url

            verify(mockEmailVerificationService, times(1))
              .isEmailVerified(eqTo(emailVerificationRequest.email.get.address), eqTo(emailVerificationRequest.credId))(any())

            verify(mockSaveForLaterService, times(1))
              .saveUserAnswers(
                eqTo(waypoints),
                eqTo(routes.ContactDetailsController.onPageLoad(waypoints)),
                eqTo(routes.EmailVerificationCodesExceededController.onPageLoad())
              )(any(), any(), any())

            verifyNoMoreInteractions(mockEmailVerificationService)
          }
        }

        "must redirect to the Email Verification Codes and Emails Exceeded page if valid data is submitted but" +
          " verification attempts on maximum emails are exceeded" in {

          val expectedRedirect: Call = routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad()

          when(mockEmailVerificationService.isEmailVerified(
            eqTo(emailVerificationRequest.email.get.address),
            eqTo(emailVerificationRequest.credId))(any())) thenReturn LockedTooManyLockedEmails.toFuture

          when(mockSaveForLaterService
            .saveUserAnswers(any(), any(), any())(any(), any(), any())
          ) thenReturn Redirect(expectedRedirect).toFuture

          val application =
            applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
              .configure("features.email-verification-enabled" -> "true")
              .configure("features.enrolments-enabled" -> "false")
              .overrides(
                bind[EmailVerificationService].toInstance(mockEmailVerificationService),
                bind[SaveForLaterService].toInstance(mockSaveForLaterService)
              ).build()

          running(application) {

            val request =
              FakeRequest(POST, contactDetailsRoute)
                .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@example.com"))

            val result = route(application, request).value

            status(result) `mustBe` SEE_OTHER
            redirectLocation(result).value `mustBe` expectedRedirect.url

            verify(mockEmailVerificationService, times(1))
              .isEmailVerified(eqTo(emailVerificationRequest.email.get.address), eqTo(emailVerificationRequest.credId))(any())

            verify(mockSaveForLaterService, times(1))
              .saveUserAnswers(
                eqTo(waypoints),
                eqTo(routes.ContactDetailsController.onPageLoad(waypoints)),
                eqTo(routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad())
              )(any(), any(), any())

            verifyNoMoreInteractions(mockEmailVerificationService)
          }
        }
      }
    }

    "inAmend" - {

      "must save the answer and redirect to the next page if email is already verified and valid data is submitted" in {

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

        when(mockSessionRepository.set(any())) thenReturn true.toFuture
        when(mockEmailVerificationService.isEmailVerified(
          eqTo(emailVerificationRequest.email.get.address),
          eqTo(emailVerificationRequest.credId))(any())) thenReturn Verified.toFuture

        val application =
          applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
            .configure("features.email-verification-enabled" -> "true")
            .configure("features.enrolments-enabled" -> "false")
            .overrides(
              bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
              bind[EmailVerificationService].toInstance(mockEmailVerificationService),
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, amendBusinessContactDetailsRoute)
              .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", "email@example.com"))

          val result = route(application, request).value
          val expectedAnswers = basicUserAnswersWithVatInfo.set(ContactDetailsPage, contactDetails).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.amend.routes.ChangeRegistrationController.onPageLoad().url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))

          verify(mockEmailVerificationService, times(1))
            .isEmailVerified(eqTo(emailVerificationRequest.email.get.address), eqTo(emailVerificationRequest.credId))(any())

          verify(mockEmailVerificationService, times(0))
            .createEmailVerificationRequest(
              eqTo(emptyWaypoints),
              eqTo(emailVerificationRequest.credId),
              eqTo(emailVerificationRequest.email.get.address),
              eqTo(emailVerificationRequest.pageTitle),
              eqTo(emailVerificationRequest.continueUrl))(any())
        }

      }

      "must save the answer and redirect to the Business Contact Details page if email is not verified and valid data is submitted" in {

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

        val newEmailAddress = "email@example.co.uk"

        val amendEmailVerificationRequest = emailVerificationRequest.copy(
          email = emailVerificationRequest.email.map(_.copy(address = newEmailAddress)),
          continueUrl = controllers.amend.routes.ChangeRegistrationController.onPageLoad().url
        )

        when(mockSessionRepository.set(any())) thenReturn true.toFuture
        when(mockEmailVerificationService.isEmailVerified(
          any(),
          any())(any())) thenReturn NotVerified.toFuture
        when(mockEmailVerificationService.createEmailVerificationRequest(
          any(),
          any(),
          any(),
          any(),
          any())(any())) thenReturn Right(amendEmailVerificationResponse).toFuture

        val application =
          applicationBuilder(userAnswers = Some(basicUserAnswersWithVatInfo))
            .configure("features.email-verification-enabled" -> "true")
            .configure("features.enrolments-enabled" -> "false")
            .overrides(
              bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository),
              bind[EmailVerificationService].toInstance(mockEmailVerificationService)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, amendBusinessContactDetailsRoute)
              .withFormUrlEncodedBody(("fullName", "name"), ("telephoneNumber", "0111 2223334"), ("emailAddress", newEmailAddress))

          val config = application.injector.instanceOf[FrontendAppConfig]
          val result = route(application, request).value
          val expectedAnswers = basicUserAnswersWithVatInfo.set(ContactDetailsPage, contactDetails.copy(emailAddress = newEmailAddress)).success.value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual config.emailVerificationUrl + amendEmailVerificationResponse.redirectUri
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))

          verify(mockEmailVerificationService, times(1))
            .isEmailVerified(eqTo(newEmailAddress), eqTo(amendEmailVerificationRequest.credId))(any())

          verify(mockEmailVerificationService, times(0))
            .createEmailVerificationRequest(
              eqTo(emptyWaypoints),
              eqTo(emailVerificationRequest.credId),
              eqTo(emailVerificationRequest.email.get.address),
              eqTo(emailVerificationRequest.pageTitle),
              eqTo(emailVerificationRequest.continueUrl))(any())
        }
      }
    }
  }
}
