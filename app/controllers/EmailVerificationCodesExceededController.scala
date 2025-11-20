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

import config.FrontendAppConfig
import controllers.actions.*
import pages.Waypoints
import pages.amend.ChangeRegistrationPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import views.html.EmailVerificationCodesExceededView

import javax.inject.Inject


class EmailVerificationCodesExceededController @Inject()(
                                                          override val messagesApi: MessagesApi,
                                                          cc: AuthenticatedControllerComponents,
                                                          config: FrontendAppConfig,
                                                          view: EmailVerificationCodesExceededView
                                                        ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(inAmend = waypoints.inAmend, inRejoin = waypoints.inRejoin) {
    implicit request =>
      val changeRegistrationUrl = ChangeRegistrationPage.route(waypoints).url
      Ok(view(waypoints.inAmend, changeRegistrationUrl))
  }
}
