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

import logging.Logging
import models.{CheckMode, ContactDetails}
import models.emailVerification.PasscodeAttemptsStatus.*
import models.requests.AuthenticatedMandatoryIntermediaryRequest
import pages.amend.ChangeRegistrationPage
import pages.{ContactDetailsPage, EmptyWaypoints, Waypoint}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import services.EmailVerificationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckBouncedEmailFilterImpl(
                                 emailVerificationService: EmailVerificationService
                                 )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[AuthenticatedMandatoryIntermediaryRequest] with Logging {

  override protected def filter[A](request: AuthenticatedMandatoryIntermediaryRequest[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val emailAddress = request.registrationWrapper.etmpDisplayRegistration.schemeDetails.businessEmailId
    val emailMatched = request.userAnswers.get(ContactDetailsPage).exists(_.emailAddress == emailAddress)
    
    if (request.registrationWrapper.etmpDisplayRegistration.schemeDetails.unusableStatus && emailMatched) {
      checkVerificationStatusAndRedirect(request.request.userId, emailAddress)
    } else {
      None.toFuture
    }
  }

  private def checkVerificationStatusAndRedirect(
                                                userId: String,
                                                emailAddress: String
                                                )(implicit hc: HeaderCarrier): Future[Option[Result]] = {

    emailVerificationService.isEmailVerified(emailAddress, userId).flatMap {
      case Verified =>
        logger.info("CheckBouncedEmailFilter - Verified")
        None.toFuture

      case LockedTooManyLockedEmails =>
        logger.info("CheckBouncedEmailFilter - LockedTooManyLockedEmails")
        Some(Redirect(controllers.routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad().url)).toFuture

      case LockedPasscodeForSingleEmail =>
        logger.info("CheckBouncedEmailFilter - LockedPasscodeForSingleEmail")
        Some(Redirect(controllers.routes.EmailVerificationCodesExceededController.onPageLoad().url)).toFuture

      case NotVerified =>
        logger.info("CheckBouncedEmailFilter - NotVerified")
        val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
        Some(Redirect(controllers.routes.ContactDetailsController.onPageLoad(waypoints).url)).toFuture
    }
  }
}


class CheckBouncedEmailFilterProvider @Inject()(
                                               emailVerificationService: EmailVerificationService
                                               )(implicit ec: ExecutionContext) {

  def apply(): CheckBouncedEmailFilterImpl = new CheckBouncedEmailFilterImpl(emailVerificationService)
}
