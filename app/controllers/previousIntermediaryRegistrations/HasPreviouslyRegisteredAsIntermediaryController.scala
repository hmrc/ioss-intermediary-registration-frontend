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

package controllers.previousIntermediaryRegistrations

import controllers.actions.*
import forms.previousIntermediaryRegistrations.HasPreviouslyRegisteredAsIntermediaryFormProvider
import pages.Waypoints
import pages.previousIntermediaryRegistrations.HasPreviouslyRegisteredAsIntermediaryPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps
import views.html.previousIntermediaryRegistrations.HasPreviouslyRegisteredAsIntermediaryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HasPreviouslyRegisteredAsIntermediaryController @Inject()(
                                                                 override val messagesApi: MessagesApi,
                                                                 cc: AuthenticatedControllerComponents,
                                                                 formProvider: HasPreviouslyRegisteredAsIntermediaryFormProvider,
                                                                 view: HasPreviouslyRegisteredAsIntermediaryView
                                                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend) {
    implicit request =>

      val preparedForm = request.userAnswers.get(HasPreviouslyRegisteredAsIntermediaryPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints: Waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints: Waypoints)).toFuture,

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(HasPreviouslyRegisteredAsIntermediaryPage, value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(HasPreviouslyRegisteredAsIntermediaryPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }
}
