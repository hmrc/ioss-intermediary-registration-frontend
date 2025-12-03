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
import models.{CheckMode, ContactDetails}
import models.emailVerification.PasscodeAttemptsStatus.{LockedPasscodeForSingleEmail, LockedTooManyLockedEmails, NotVerified, Verified}
import models.requests.{AuthenticatedDataRequest, AuthenticatedMandatoryIntermediaryRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ChangeRegistrationPage
import pages.{ContactDetailsPage, EmptyWaypoints, Waypoint}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers.*
import services.EmailVerificationService
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckBouncedEmailFilterSpec extends SpecBase with MockitoSugar {

  class Harness(emailVerificationService: EmailVerificationService) extends CheckBouncedEmailFilterImpl(emailVerificationService) {
    def callFilter(request: AuthenticatedMandatoryIntermediaryRequest[_]): Future[Option[Result]] =
      filter(request)
  }

  private val mockEmailVerificationService = mock[EmailVerificationService]

  private val changeRegWaypoint = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))

  ".filter" - {

    "when unusableStatus is False and the email provided by the user does not match the one in the database" - {

      "must return None" in {

        val contactDetails: ContactDetails = ContactDetails(
          fullName = "Rocky Balboa",
          telephoneNumber = "028 123 4567",
          emailAddress = "rocky.balboa@chartoffwinkler.co.uk"
        )

        val updatedUserAnswers = emptyUserAnswersWithVatInfo.set(ContactDetailsPage, contactDetails).success.value

        val updatedRegistrationWrapper = registrationWrapper.copy(
          etmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value.copy(
            schemeDetails = arbitraryEtmpDisplaySchemeDetails.arbitrary.sample.value.copy(
              unusableStatus = false,
              businessEmailId = "not-matching@email.com"
            )
          )
        )

        val application = applicationBuilder(userAnswers = Some(updatedUserAnswers)).build()

        running(application) {

          val authDataRequest = AuthenticatedDataRequest(
            FakeRequest(),
            testCredentials,
            vrn,
            testEnrolments,
            updatedUserAnswers,
            Some(iossNumber),
            1,
            None,
            None,
            Some(intermediaryNumber),
            Some(updatedRegistrationWrapper)
          )

          val request = AuthenticatedMandatoryIntermediaryRequest(
            authDataRequest,
            testCredentials,
            vrn,
            testEnrolments,
            updatedUserAnswers,
            1,
            None,
            None,
            intermediaryNumber,
            updatedRegistrationWrapper
          )

          val controller = new Harness(mockEmailVerificationService)

          val result = controller.callFilter(request).futureValue

          result mustBe None
        }
      }
    }

    "when unusableStatus is True and the email provided by the user matches the one in the database" - {

      "must redirect to EmailVerificationCodesAndEmailsExceeded page if the email verification returns LockedTooManyLockedEmails" in {

        val contactDetails: ContactDetails = ContactDetails(
          fullName = "Rocky Balboa",
          telephoneNumber = "028 123 4567",
          emailAddress = "rocky.balboa@chartoffwinkler.co.uk"
        )

        val userAnswersWithMatchingEmail = emptyUserAnswersWithVatInfo.set(ContactDetailsPage, contactDetails).success.value

        val updatedRegistrationWrapper = registrationWrapper.copy(
          etmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value.copy(
            schemeDetails = arbitraryEtmpDisplaySchemeDetails.arbitrary.sample.value.copy(
              unusableStatus = true,
              businessEmailId = "rocky.balboa@chartoffwinkler.co.uk"
            )
          )
        )

        when(mockEmailVerificationService.isEmailVerified(any(), any())(any())) thenReturn LockedTooManyLockedEmails.toFuture

        val application = applicationBuilder(userAnswers = Some(userAnswersWithMatchingEmail)).build()

        running(application) {

          val authDataRequest = AuthenticatedDataRequest(
            FakeRequest(),
            testCredentials,
            vrn,
            testEnrolments,
            userAnswersWithMatchingEmail,
            Some(iossNumber),
            1,
            None,
            None,
            Some(intermediaryNumber),
            Some(updatedRegistrationWrapper)
          )

          val request = AuthenticatedMandatoryIntermediaryRequest(
            authDataRequest,
            testCredentials,
            vrn,
            testEnrolments,
            userAnswersWithMatchingEmail,
            1,
            None,
            None,
            intermediaryNumber,
            updatedRegistrationWrapper
          )

          val controller = new Harness(mockEmailVerificationService)

          val result = controller.callFilter(request).futureValue

          result.value mustBe Redirect(controllers.routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad().url)
        }
      }

      "must redirect to EmailVerificationCodesExceeded page if the email verification returns LockedPasscodeForSingleEmail" in {

        val contactDetails: ContactDetails = ContactDetails(
          fullName = "Rocky Balboa",
          telephoneNumber = "028 123 4567",
          emailAddress = "rocky.balboa@chartoffwinkler.co.uk"
        )

        val userAnswersWithMatchingEmail = emptyUserAnswersWithVatInfo.set(ContactDetailsPage, contactDetails).success.value

        val updatedRegistrationWrapper = registrationWrapper.copy(
          etmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value.copy(
            schemeDetails = arbitraryEtmpDisplaySchemeDetails.arbitrary.sample.value.copy(
              unusableStatus = true,
              businessEmailId = "rocky.balboa@chartoffwinkler.co.uk"
            )
          )
        )

        when(mockEmailVerificationService.isEmailVerified(any(), any())(any())) thenReturn LockedPasscodeForSingleEmail.toFuture

        val application = applicationBuilder(userAnswers = Some(userAnswersWithMatchingEmail)).build()

        running(application) {

          val authDataRequest = AuthenticatedDataRequest(
            FakeRequest(),
            testCredentials,
            vrn,
            testEnrolments,
            userAnswersWithMatchingEmail,
            Some(iossNumber),
            1,
            None,
            None,
            Some(intermediaryNumber),
            Some(updatedRegistrationWrapper)
          )

          val request = AuthenticatedMandatoryIntermediaryRequest(
            authDataRequest,
            testCredentials,
            vrn,
            testEnrolments,
            userAnswersWithMatchingEmail,
            1,
            None,
            None,
            intermediaryNumber,
            updatedRegistrationWrapper
          )

          val controller = new Harness(mockEmailVerificationService)

          val result = controller.callFilter(request).futureValue

          result.value mustBe Redirect(controllers.routes.EmailVerificationCodesExceededController.onPageLoad().url)
        }
      }

      "must redirect to ContactDetails page if the email is not verified" in {

        val contactDetails: ContactDetails = ContactDetails(
          fullName = "Rocky Balboa",
          telephoneNumber = "028 123 4567",
          emailAddress = "rocky.balboa@chartoffwinkler.co.uk"
        )

        val userAnswersWithMatchingEmail = emptyUserAnswersWithVatInfo.set(ContactDetailsPage, contactDetails).success.value

        val updatedRegistrationWrapper = registrationWrapper.copy(
          etmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value.copy(
            schemeDetails = arbitraryEtmpDisplaySchemeDetails.arbitrary.sample.value.copy(
              unusableStatus = true,
              businessEmailId = "rocky.balboa@chartoffwinkler.co.uk"
            )
          )
        )

        when(mockEmailVerificationService.isEmailVerified(any(), any())(any())) thenReturn NotVerified.toFuture

        val application = applicationBuilder(userAnswers = Some(userAnswersWithMatchingEmail)).build()

        running(application) {

          val authDataRequest = AuthenticatedDataRequest(
            FakeRequest(),
            testCredentials,
            vrn,
            testEnrolments,
            userAnswersWithMatchingEmail,
            Some(iossNumber),
            1,
            None,
            None,
            Some(intermediaryNumber),
            Some(updatedRegistrationWrapper)
          )

          val request = AuthenticatedMandatoryIntermediaryRequest(
            authDataRequest,
            testCredentials,
            vrn,
            testEnrolments,
            userAnswersWithMatchingEmail,
            1,
            None,
            None,
            intermediaryNumber,
            updatedRegistrationWrapper
          )

          val controller = new Harness(mockEmailVerificationService)

          val result = controller.callFilter(request).futureValue

          result.value mustBe Redirect(controllers.routes.ContactDetailsController.onPageLoad(changeRegWaypoint))
        }
      }

      "must return None" - {

        "if the email is verified" in {

          val contactDetails: ContactDetails = ContactDetails(
            fullName = "Rocky Balboa",
            telephoneNumber = "028 123 4567",
            emailAddress = "rocky.balboa@chartoffwinkler.co.uk"
          )

          val userAnswersWithMatchingEmail = emptyUserAnswersWithVatInfo.set(ContactDetailsPage, contactDetails).success.value

          val updatedRegistrationWrapper = registrationWrapper.copy(
            etmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value.copy(
              schemeDetails = arbitraryEtmpDisplaySchemeDetails.arbitrary.sample.value.copy(
                unusableStatus = true,
                businessEmailId = "rocky.balboa@chartoffwinkler.co.uk"
              )
            )
          )

          when(mockEmailVerificationService.isEmailVerified(any(), any())(any())) thenReturn Verified.toFuture

          val application = applicationBuilder(userAnswers = Some(userAnswersWithMatchingEmail)).build()

          running(application) {

            val authDataRequest = AuthenticatedDataRequest(
              FakeRequest(),
              testCredentials,
              vrn,
              testEnrolments,
              userAnswersWithMatchingEmail,
              Some(iossNumber),
              1,
              None,
              None,
              Some(intermediaryNumber),
              Some(updatedRegistrationWrapper)
            )

            val request = AuthenticatedMandatoryIntermediaryRequest(
              authDataRequest,
              testCredentials,
              vrn,
              testEnrolments,
              userAnswersWithMatchingEmail,
              1,
              None,
              None,
              intermediaryNumber,
              updatedRegistrationWrapper
            )

            val controller = new Harness(mockEmailVerificationService)

            val result = controller.callFilter(request).futureValue

            result mustBe None
          }
        }

        "if the email has been updated" in {

          val contactDetails: ContactDetails = ContactDetails(
            fullName = "Rocky Balboa",
            telephoneNumber = "028 123 4567",
            emailAddress = "rocky.balboa@chartoffwinkler.co.uk"
          )

          val updatedUserAnswers = completeUserAnswersWithVatInfo.set(ContactDetailsPage, contactDetails).success.value

          val updatedRegistrationWrapper = registrationWrapper.copy(
            etmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value.copy(
              schemeDetails = arbitraryEtmpDisplaySchemeDetails.arbitrary.sample.value.copy(
                unusableStatus = true,
                businessEmailId = "rocky.balboa@chartoffwinkler.co.uk"
              )
            )
          )

          when(mockEmailVerificationService.isEmailVerified(any(), any())(any())) thenReturn Verified.toFuture

          val application = applicationBuilder(userAnswers = Some(updatedUserAnswers)).build()

          running(application) {

            val authDataRequest = AuthenticatedDataRequest(
              FakeRequest(),
              testCredentials,
              vrn,
              testEnrolments,
              updatedUserAnswers,
              Some(iossNumber),
              1,
              None,
              None,
              Some(intermediaryNumber),
              Some(updatedRegistrationWrapper)
            )

            val request = AuthenticatedMandatoryIntermediaryRequest(
              authDataRequest,
              testCredentials,
              vrn,
              testEnrolments,
              updatedUserAnswers,
              1,
              None,
              None,
              intermediaryNumber,
              updatedRegistrationWrapper
            )

            val controller = new Harness(mockEmailVerificationService)

            val result = controller.callFilter(request).futureValue

            result mustBe None
          }
        }
      }
    }
  }
}
