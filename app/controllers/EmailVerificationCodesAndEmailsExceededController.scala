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

import controllers.actions.*
import models.ContactDetails
import models.etmp.display.EtmpDisplaySchemeDetails
import pages.amend.ChangeRegistrationPage
import pages.rejoin.RejoinSchemePage
import pages.{ContactDetailsPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.OriginalRegistrationQuery
import queries.amend.PreviousRegistrationIntermediaryNumberQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import views.html.EmailVerificationCodesAndEmailsExceededView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class EmailVerificationCodesAndEmailsExceededController @Inject()(
                                                                   override val messagesApi: MessagesApi,
                                                                   cc: AuthenticatedControllerComponents,
                                                                   view: EmailVerificationCodesAndEmailsExceededView
                                                                 )(implicit val executionContext: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(inAmend = waypoints.inAmend, inRejoin = waypoints.inRejoin).async {
    implicit request =>
      
      val changeRegistrationUrl: String = ChangeRegistrationPage.route(waypoints).url
      val rejoinSchemeUrl: String = RejoinSchemePage.route(waypoints).url

      val contactDetails: ContactDetails = request.userAnswers.get(ContactDetailsPage).getOrElse{
        throw new IllegalStateException("Contact Details have not been set in answers")
      }

      val schemeDetails: EtmpDisplaySchemeDetails = {
        if (request.userAnswers.get(PreviousRegistrationIntermediaryNumberQuery).isDefined) {

          request.userAnswers.get(PreviousRegistrationIntermediaryNumberQuery).flatMap { previousIossNum =>
            request.userAnswers.get(OriginalRegistrationQuery(previousIossNum)).map { previousRegistrationWrapper =>
              previousRegistrationWrapper.schemeDetails
            }
          }.getOrElse(throw new IllegalStateException("Previous Scheme Details are not present and required for a previous registration"))

        } else {
          request.registrationWrapper.map(_.etmpDisplayRegistration.schemeDetails).getOrElse {
            throw new IllegalStateException("Scheme Details are not present in the registration wrapper")
          }
        }
      }

      if (contactDetails.differsFromOriginal(schemeDetails)) {
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(ContactDetailsPage, contactDetails.resetToOriginal(schemeDetails)))
          _ <- cc.sessionRepository.set(updatedAnswers)
        } yield {
          Ok(view(waypoints.inAmend, waypoints.inRejoin, changeRegistrationUrl, rejoinSchemeUrl))
        }
      } else {
        Future.successful(Ok(view(waypoints.inAmend, waypoints.inRejoin, changeRegistrationUrl, rejoinSchemeUrl)))
      }
  }

}
