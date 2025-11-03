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

package controllers.amend

import controllers.actions.*
import forms.amend.HasBusinessAddressInNiFormProvider
import models.amend.BusinessAddressInNi
import pages.Waypoints
import pages.amend.HasBusinessAddressInNiPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps
import views.html.amend.HasBusinessAddressInNiView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HasBusinessAddressInNiController @Inject()(
                                                  override val messagesApi: MessagesApi,
                                                  cc: AuthenticatedControllerComponents,
                                                  formProvider: HasBusinessAddressInNiFormProvider,
                                                  view: HasBusinessAddressInNiView
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[BusinessAddressInNi] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireIntermediary(waypoints, inAmend = waypoints.inAmend, waypoints.inRejoin) {
    implicit request =>

      val preparedForm = request.userAnswers.get(HasBusinessAddressInNiPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireIntermediary(waypoints, inAmend = waypoints.inAmend, waypoints.inRejoin).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints)).toFuture,

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(HasBusinessAddressInNiPage, value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(HasBusinessAddressInNiPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }
}
