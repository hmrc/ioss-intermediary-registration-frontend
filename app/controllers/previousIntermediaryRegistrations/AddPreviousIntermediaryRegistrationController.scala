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
import forms.previousIntermediaryRegistrations.AddPreviousIntermediaryRegistrationFormProvider
import pages.Waypoints
import pages.previousIntermediaryRegistrations.AddPreviousIntermediaryRegistrationPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationsSummary
import views.html.previousIntermediaryRegistrations.AddPreviousIntermediaryRegistrationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddPreviousIntermediaryRegistrationController @Inject()(
                                                               override val messagesApi: MessagesApi,
                                                               cc: AuthenticatedControllerComponents,
                                                               formProvider: AddPreviousIntermediaryRegistrationFormProvider,
                                                               view: AddPreviousIntermediaryRegistrationView
                                                             )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  private val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData() {
    implicit request =>

      val previousIntermediaryRegistrationSummary: SummaryList = PreviousIntermediaryRegistrationsSummary
        .row(waypoints, request.userAnswers, AddPreviousIntermediaryRegistrationPage())

      Ok(view(form, waypoints, previousIntermediaryRegistrationSummary))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>

      val previousIntermediaryRegistrationSummary: SummaryList = PreviousIntermediaryRegistrationsSummary
        .row(waypoints, request.userAnswers, AddPreviousIntermediaryRegistrationPage())

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints, previousIntermediaryRegistrationSummary)).toFuture,

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(AddPreviousIntermediaryRegistrationPage(), value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(AddPreviousIntermediaryRegistrationPage().navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }
}
