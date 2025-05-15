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

import controllers.AnswerExtractor
import controllers.actions.*
import forms.previousIntermediaryRegistrations.DeletePreviousIntermediaryRegistrationFormProvider
import models.Index
import pages.Waypoints
import pages.previousIntermediaryRegistrations.DeletePreviousIntermediaryRegistrationPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.previousIntermediaryRegistrations.{AllPreviousIntermediaryRegistrationsRawQuery, DeriveNumberOfPreviousIntermediaryRegistrations, PreviousIntermediaryRegistrationQuery}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.previousIntermediaryRegistrations.DeletePreviousIntermediaryRegistrationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeletePreviousIntermediaryRegistrationController @Inject()(
                                                                  override val messagesApi: MessagesApi,
                                                                  cc: AuthenticatedControllerComponents,
                                                                  formProvider: DeletePreviousIntermediaryRegistrationFormProvider,
                                                                  view: DeletePreviousIntermediaryRegistrationView
                                                                )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with AnswerExtractor {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData() {
    implicit request =>

      getAnswer(waypoints, PreviousIntermediaryRegistrationQuery(countryIndex)) { previousIntermediaryRegistration =>

        val form: Form[Boolean] = formProvider(previousIntermediaryRegistration.previousEuCountry)

        Ok(view(form, waypoints, countryIndex, previousIntermediaryRegistration.previousEuCountry))
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>

      getAnswerAsync(waypoints, PreviousIntermediaryRegistrationQuery(countryIndex)) { previousIntermediaryRegistration =>

        val form: Form[Boolean] = formProvider(previousIntermediaryRegistration.previousEuCountry)

        form.bindFromRequest().fold(
          formWithErrors =>
            BadRequest(view(formWithErrors, waypoints, countryIndex, previousIntermediaryRegistration.previousEuCountry)).toFuture,

          value =>
            if (value) {
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.remove(PreviousIntermediaryRegistrationQuery(countryIndex)))
                finalAnswers <- Future.fromTry(
                  cleanupEmptyAnswers(updatedAnswers, DeriveNumberOfPreviousIntermediaryRegistrations, AllPreviousIntermediaryRegistrationsRawQuery)
                )
                _ <- cc.sessionRepository.set(finalAnswers)
              } yield Redirect(DeletePreviousIntermediaryRegistrationPage(countryIndex).navigate(waypoints, request.userAnswers, finalAnswers).route)
            } else {
              Redirect(DeletePreviousIntermediaryRegistrationPage(countryIndex).navigate(waypoints, request.userAnswers, request.userAnswers).route).toFuture
            }
        )
      }
  }
}
