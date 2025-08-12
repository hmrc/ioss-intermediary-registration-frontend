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
import models.NormalMode
import models.emailVerification.PasscodeAttemptsStatus.*
import models.requests.AuthenticatedDataRequest
import pages.{CheckYourAnswersPage, ContactDetailsPage, EmptyWaypoints, Waypoint, Waypoints}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import services.{EmailVerificationService, SaveForLaterService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckEmailVerificationFilterImpl(
                                        waypoints: Waypoints,
                                        inAmend: Boolean,
                                        frontendAppConfig: FrontendAppConfig,
                                        emailVerificationService: EmailVerificationService,
                                        saveForLaterService: SaveForLaterService
                                      )(implicit val executionContext: ExecutionContext) extends ActionFilter[AuthenticatedDataRequest] with Logging {

  override protected def filter[A](request: AuthenticatedDataRequest[A]): Future[Option[Result]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    val dataRequest: AuthenticatedDataRequest[_] = request

    if (frontendAppConfig.emailVerificationEnabled && !inAmend) {
      request.userAnswers.get(ContactDetailsPage) match {
        case Some(contactDetails) =>
          emailVerificationService.isEmailVerified(contactDetails.emailAddress, request.userId).flatMap {
            case Verified =>
              logger.info("CheckEmailVerificationFilter - Verified")
              None.toFuture

            case LockedTooManyLockedEmails =>
              logger.info("CheckEmailVerificationFilter - LockedTooManyLockedEmails")
              Some(Redirect(routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad().url)).toFuture

            case LockedPasscodeForSingleEmail =>
              logger.info("CheckEmailVerificationFilter - LockedPasscodeForSingleEmail")
              saveForLaterService.submitSavedUserAnswersAndRedirect(
                waypoints = waypoints,
                originLocation = request.uri,
                redirectLocation = routes.EmailVerificationCodesExceededController.onPageLoad().url
              )(dataRequest, hc, executionContext).map(result => Some(result))

            case _ =>
              logger.info("CheckEmailVerificationFilter - Not Verified")
              val waypoint = EmptyWaypoints.setNextWaypoint(Waypoint(CheckYourAnswersPage, NormalMode, CheckYourAnswersPage.urlFragment))
              Some(Redirect(routes.ContactDetailsController.onPageLoad(waypoint).url)).toFuture
          }
        case None => None.toFuture
      }
    } else {
      None.toFuture
    }
  }

}


class CheckEmailVerificationFilterProvider @Inject()(
                                                      frontendAppConfig: FrontendAppConfig,
                                                      emailVerificationService: EmailVerificationService,
                                                      saveForLaterService: SaveForLaterService
                                                    )(implicit executionContext: ExecutionContext) {

  def apply(waypoints: Waypoints, inAmend: Boolean): CheckEmailVerificationFilterImpl = {
    new CheckEmailVerificationFilterImpl(waypoints, inAmend, frontendAppConfig, emailVerificationService, saveForLaterService)
  }
}