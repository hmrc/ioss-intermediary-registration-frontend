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

import config.FrontendAppConfig
import controllers.routes
import logging.Logging
import models.{ContactDetails, NormalMode}
import models.emailVerification.PasscodeAttemptsStatus.*
import models.requests.AuthenticatedDataRequest
import pages.{CheckYourAnswersPage, ContactDetailsPage, EmptyWaypoints, Waypoint, Waypoints}
import pages.amend.ChangeRegistrationPage
import pages.rejoin.RejoinSchemePage
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import queries.OriginalRegistrationQuery
import repositories.AuthenticatedUserAnswersRepository
import services.{EmailVerificationService, SaveForLaterService}
import queries.amend.PreviousRegistrationIntermediaryNumberQuery
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckEmailVerificationFilterImpl(
                                        waypoints: Waypoints,
                                        inAmend: Boolean,
                                        inRejoin: Boolean,
                                        frontendAppConfig: FrontendAppConfig,
                                        emailVerificationService: EmailVerificationService,
                                        saveForLaterService: SaveForLaterService
                                      )(implicit val executionContext: ExecutionContext) extends ActionFilter[AuthenticatedDataRequest] with Logging {

  override protected def filter[A](request: AuthenticatedDataRequest[A]): Future[Option[Result]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val dataRequest: AuthenticatedDataRequest[_] = request

    if (frontendAppConfig.emailVerificationEnabled) {
      request.userAnswers.get(ContactDetailsPage) match {
        case Some(contactDetails) if inAmend && emailHasChanged(contactDetails, request)=>
          doEmailVerificationAndRedirect(emailVerificationService, request, contactDetails, inAmend, inRejoin)
          
        case Some(contactDetails) if inRejoin && emailHasChanged(contactDetails, request) =>
          doEmailVerificationAndRedirect(emailVerificationService, request, contactDetails, inAmend, inRejoin)
          
        case Some(contactDetails) if !inAmend && !inRejoin =>
          doEmailVerificationAndRedirect(emailVerificationService, request, contactDetails, inAmend, inRejoin)
          
        case _ => None.toFuture
      }
    } else {
      None.toFuture
    }
  }

  private def emailHasChanged(contactDetails: ContactDetails, request: AuthenticatedDataRequest[_]): Boolean = {
    if (request.userAnswers.get(PreviousRegistrationIntermediaryNumberQuery).isDefined) {
      
      request.userAnswers.get(PreviousRegistrationIntermediaryNumberQuery).flatMap { previousIossNum =>
        request.userAnswers.get(OriginalRegistrationQuery(previousIossNum)).map { previousRegistrationWrapper =>
          previousRegistrationWrapper.schemeDetails.businessEmailId != contactDetails.emailAddress
        }
      }.getOrElse(throw new IllegalStateException("Previous Scheme Details are not present and required for a previous registration"))

    } else {
      request.registrationWrapper.exists(_.etmpDisplayRegistration.schemeDetails.businessEmailId != contactDetails.emailAddress)
    }
  }


  private def doEmailVerificationAndRedirect(
                                      emailVerificationService: EmailVerificationService,
                                      request: AuthenticatedDataRequest[_],
                                      contactDetails: ContactDetails,
                                      inAmend: Boolean,
                                      inRejoin: Boolean
                                    )(implicit hc: HeaderCarrier) = {
    val waypoints = if(inAmend) {
      EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, NormalMode, ChangeRegistrationPage.urlFragment))
    } else if (inRejoin) {
      EmptyWaypoints.setNextWaypoint(Waypoint(RejoinSchemePage, NormalMode, RejoinSchemePage.urlFragment))
    } else {
      EmptyWaypoints
    }

    emailVerificationService.isEmailVerified(contactDetails.emailAddress, request.userId).flatMap {
      case Verified =>
        logger.info("CheckEmailVerificationFilter - Verified")
        None.toFuture

      case LockedTooManyLockedEmails =>
        logger.info("CheckEmailVerificationFilter - LockedTooManyLockedEmails")
        Some(Redirect(routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad(waypoints).url)).toFuture

      case LockedPasscodeForSingleEmail =>
        logger.info("CheckEmailVerificationFilter - LockedPasscodeForSingleEmail")
        saveForLaterService.submitSavedUserAnswersAndRedirect(
          waypoints = waypoints,
          originLocation = request.uri,
          redirectLocation = routes.EmailVerificationCodesExceededController.onPageLoad(waypoints).url
        )(request, hc, executionContext).map(result => Some(result))

      case _ =>
        logger.info("CheckEmailVerificationFilter - Not Verified")
        val waypoint =
          if(inAmend) {
            EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, NormalMode, ChangeRegistrationPage.urlFragment))
          } else if (inRejoin) {
            EmptyWaypoints.setNextWaypoint(Waypoint(RejoinSchemePage, NormalMode, RejoinSchemePage.urlFragment))
          } else {
            EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, NormalMode, CheckYourAnswersPage.urlFragment))
          }
        Some(Redirect(routes.ContactDetailsController.onPageLoad(waypoint).url)).toFuture
    }
  }

}


class CheckEmailVerificationFilterProvider @Inject()(
                                                      frontendAppConfig: FrontendAppConfig,
                                                      emailVerificationService: EmailVerificationService,
                                                      saveForLaterService: SaveForLaterService
                                                    )(implicit executionContext: ExecutionContext) {

  def apply(waypoints: Waypoints, inAmend: Boolean, inRejoin: Boolean): CheckEmailVerificationFilterImpl = {
    new CheckEmailVerificationFilterImpl(waypoints, inAmend, inRejoin, frontendAppConfig, emailVerificationService, saveForLaterService)
  }
}