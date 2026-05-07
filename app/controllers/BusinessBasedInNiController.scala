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
import forms.BusinessBasedInNiFormProvider
import pages.{BusinessBasedInNiPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps
import views.html.BusinessBasedInNiView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessBasedInNiController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             cc: AuthenticatedControllerComponents,
                                             formProvider: BusinessBasedInNiFormProvider,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: BusinessBasedInNiView
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] =
    cc.authAndRequireIntermediaryAndVerifyEmail(inAmend = waypoints.inAmend, waypoints.inRejoin).async {
    implicit request =>

      val preparedForm = request.userAnswers.get(BusinessBasedInNiPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints)).toFuture
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] =
    cc.authAndRequireIntermediaryAndVerifyEmail(inAmend = waypoints.inAmend, waypoints.inRejoin).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, waypoints))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(BusinessBasedInNiPage, value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(BusinessBasedInNiPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }
}
