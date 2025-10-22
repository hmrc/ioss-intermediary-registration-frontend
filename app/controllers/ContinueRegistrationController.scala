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

import connectors.SaveForLaterConnector
import controllers.actions.*
import forms.ContinueRegistrationFormProvider
import models.ContinueRegistration
import pages.{IndexPage, JourneyRecoveryPage, SavedProgressContinuePage, SavedProgressPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.http.HttpVerbs.GET
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.ContinueRegistrationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContinueRegistrationController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                cc: AuthenticatedControllerComponents,
                                                formProvider: ContinueRegistrationFormProvider,
                                                saveForLaterConnector: SaveForLaterConnector,
                                                view: ContinueRegistrationView
                                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[ContinueRegistration] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData() {
    implicit request =>

      val preparedForm = request.userAnswers.get(SavedProgressContinuePage) match {
        case Some(value) => form.fill(value)
        case _ => form
      }

      request.userAnswers.get(SavedProgressPage).map { _ =>
        Ok(view(preparedForm, waypoints))
      }.getOrElse {
        Redirect(controllers.routes.IndexController.onPageLoad())
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints)).toFuture,

        value =>
          (value, request.userAnswers.get(SavedProgressPage)) match {
            case (ContinueRegistration.Continue, Some(url)) =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(SavedProgressContinuePage, value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(Call(GET, url))

            case (ContinueRegistration.Delete, _) =>
              for {
                _ <- cc.sessionRepository.clear(request.userId)
                _ <- saveForLaterConnector.delete()
              } yield Redirect(IndexPage.route(waypoints).url)

            case _ =>
              Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
          }
      )
  }
}
