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

import config.FrontendAppConfig
import controllers.actions.*
import forms.amend.CancelAmendRegistrationFormProvider
import pages.Waypoints
import pages.amend.ChangeRegistrationPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.amend.CancelAmendRegistrationView
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CancelAmendRegistrationController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   cc: AuthenticatedControllerComponents,
                                                   formProvider: CancelAmendRegistrationFormProvider,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   appConfig: FrontendAppConfig,
                                                   view: CancelAmendRegistrationView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(inAmend = true) {
    implicit request =>

      Ok(view(form, waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(inAmend = true).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints)).toFuture,

        value =>
          if (value) {
            for {
              _ <- cc.sessionRepository.clear(request.userId)
            } yield Redirect(appConfig.intermediaryYourAccountUrl)
          } else {
            Redirect(ChangeRegistrationPage.route(waypoints).url).toFuture
          }
      )
  }
}
