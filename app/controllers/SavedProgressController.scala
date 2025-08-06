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
import formats.Format.saveForLaterDateFormatter
import models.UserAnswers
import pages.{SavedProgressPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.SavedProgressView

import java.time.Clock
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SavedProgressController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         clock: Clock,
                                         frontendAppConfig: FrontendAppConfig,
                                         view: SavedProgressView
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, continueUrl: RedirectUrl): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>

      val answersExpiry: String = request.userAnswers.lastUpdated.plus(frontendAppConfig.saveForLaterTtl, ChronoUnit.DAYS)
        .atZone(clock.getZone).toLocalDate.format(saveForLaterDateFormatter)

      val savedProgressAnswers: Future[UserAnswers] = Future.fromTry(request.userAnswers.set(SavedProgressPage, continueUrl.get(OnlyRelative).url))

      savedProgressAnswers.flatMap { updatedAnswers =>

        Ok(view(answersExpiry, continueUrl.get(OnlyRelative).url, frontendAppConfig.loginUrl)).toFuture
      }
  }
}
