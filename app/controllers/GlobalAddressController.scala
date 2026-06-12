/*
 * Copyright 2026 HM Revenue & Customs
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
import forms.GlobalAddressFormProvider
import models.{Country, InternationalAddress}
import pages.{GlobalAddressPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import views.html.GlobalAddressView
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GlobalAddressController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: GlobalAddressFormProvider,
                                        view: GlobalAddressView
                                    )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with GetCountry {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend, waypoints.inRejoin).async {
    implicit request =>
      
      getInternationalCountry(waypoints) { country =>
        val form: Form[InternationalAddress] = formProvider(country)
        val preparedForm = request.userAnswers.get(GlobalAddressPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        Ok(view(preparedForm, waypoints, country.name)).toFuture

      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend, waypoints.inRejoin).async {
    implicit request =>
      
      getInternationalCountry(waypoints) { country =>
        val form: Form[InternationalAddress] = formProvider(country)
        form.bindFromRequest().fold(
          formWithErrors =>
            BadRequest(view(formWithErrors, waypoints, country.name)).toFuture,

          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(GlobalAddressPage, value))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield Redirect(GlobalAddressPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
        )
      }
  }
}
