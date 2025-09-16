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
import forms.previousIntermediaryRegistrations.PreviousEuCountryFormProvider
import models.{Country, Index}
import pages.Waypoints
import pages.previousIntermediaryRegistrations.PreviousEuCountryPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.previousIntermediaryRegistrations.AllPreviousIntermediaryRegistrationsQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps
import views.html.previousIntermediaryRegistrations.PreviousEuCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreviousEuCountryController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             cc: AuthenticatedControllerComponents,
                                             formProvider: PreviousEuCountryFormProvider,
                                             view: PreviousEuCountryView
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend) {
    implicit request =>

      val form: Form[Country] = formProvider(countryIndex, request.userAnswers.get(AllPreviousIntermediaryRegistrationsQuery)
        .getOrElse(Seq.empty).map(_.previousEuCountry))

      val preparedForm = request.userAnswers.get(PreviousEuCountryPage(countryIndex)) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints, countryIndex))
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
    implicit request =>

      val form: Form[Country] = formProvider(countryIndex, request.userAnswers.get(AllPreviousIntermediaryRegistrationsQuery)
        .getOrElse(Seq.empty).map(_.previousEuCountry))

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints, countryIndex)).toFuture,

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(PreviousEuCountryPage(countryIndex), value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(PreviousEuCountryPage(countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }
}
