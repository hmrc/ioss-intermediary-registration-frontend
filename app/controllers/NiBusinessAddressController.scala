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

import config.Constants.niPostCodeAreaPrefix
import controllers.actions.*
import forms.NiBusinessAddressFormProvider
import pages.{BusinessBasedInNiPage, CannotRegisterNotNiBasedBusinessPage, NiBusinessAddressPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.NiBusinessAddressView
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NiBusinessAddressController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      cc: AuthenticatedControllerComponents,
                                      formProvider: NiBusinessAddressFormProvider,
                                      val controllerComponents: MessagesControllerComponents,
                                      view: NiBusinessAddressView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireIntermediaryAndVerifyEmail(waypoints, inAmend = waypoints.inAmend, waypoints.inRejoin).async{
    implicit request =>

      val preparedForm = request.userAnswers.get(NiBusinessAddressPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints)).toFuture
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireIntermediaryAndVerifyEmail(waypoints, inAmend = waypoints.inAmend, waypoints.inRejoin).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints)).toFuture,

        value =>
          if (value.postCode.toUpperCase.startsWith(niPostCodeAreaPrefix)) {
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(NiBusinessAddressPage, value))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield Redirect(NiBusinessAddressPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
          } else if (!value.postCode.toUpperCase.startsWith(niPostCodeAreaPrefix) && waypoints.inAmend) {
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.remove(NiBusinessAddressPage))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield Redirect(BusinessBasedInNiPage.route(waypoints).url)
          } else {
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.remove(NiBusinessAddressPage))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield Redirect(CannotRegisterNotNiBasedBusinessPage.route(waypoints).url)
          }
      )
  }
}
