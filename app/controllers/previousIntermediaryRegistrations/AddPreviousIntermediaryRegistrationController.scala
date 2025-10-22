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
import models.Country
import models.previousIntermediaryRegistrations.{PreviousIntermediaryRegistrationDetails, PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber}
import models.requests.AuthenticatedDataRequest
import pages.previousIntermediaryRegistrations.AddPreviousIntermediaryRegistrationPage
import pages.{JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.previousIntermediaryRegistrations.DeriveNumberOfPreviousIntermediaryRegistrations
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.CheckWaypoints.CheckWaypointsOps
import utils.CompletionChecks
import utils.FutureSyntax.FutureOps
import utils.ItemsHelper.getDerivedItems
import utils.PreviousIntermediaryRegistrationCompletionChecks.{getAllIncompletePreviousIntermediaryRegistrations, incompletePreviousIntermediaryRegistrationRedirect}
import viewmodels.checkAnswers.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationsSummary
import views.html.previousIntermediaryRegistrations.AddPreviousIntermediaryRegistrationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddPreviousIntermediaryRegistrationController @Inject()(
                                                               override val messagesApi: MessagesApi,
                                                               cc: AuthenticatedControllerComponents,
                                                               formProvider: AddPreviousIntermediaryRegistrationFormProvider,
                                                               view: AddPreviousIntermediaryRegistrationView
                                                             )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with CompletionChecks {

  protected val controllerComponents: MessagesControllerComponents = cc

  private val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
    implicit request =>

      val previousRegistration = getPreviousRegistrationsWhenInAmend(waypoints, request)

      getDerivedItems(waypoints, DeriveNumberOfPreviousIntermediaryRegistrations) { numberOfPreviousIntermediaryRegistrations =>

        val preparedForm = request.userAnswers.get(AddPreviousIntermediaryRegistrationPage()) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        val canAddCountries: Boolean = numberOfPreviousIntermediaryRegistrations < Country.euCountries.size

        val previousIntermediaryRegistrationSummary: SummaryList = PreviousIntermediaryRegistrationsSummary
          .row(waypoints, request.userAnswers, AddPreviousIntermediaryRegistrationPage(), previousRegistration)

        withCompleteDataAsync[PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber](
          data = getAllIncompletePreviousIntermediaryRegistrations _,
          onFailure = (incomplete: Seq[PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber]) => {
            Ok(view(preparedForm, waypoints, previousIntermediaryRegistrationSummary, canAddCountries, incomplete)).toFuture
          }) {
          Ok(view(preparedForm, waypoints, previousIntermediaryRegistrationSummary, canAddCountries)).toFuture
        }
      }
  }

  private def getPreviousRegistrationsWhenInAmend(
                                                   waypoints: Waypoints,
                                                   request: AuthenticatedDataRequest[AnyContent]
                                                 ): Seq[PreviousIntermediaryRegistrationDetails] = {
    if (waypoints.inAmend) {
      request.registrationWrapper.flatMap(_.etmpDisplayRegistration.intermediaryDetails.map(_.otherIossIntermediaryRegistrations))
        .map(PreviousIntermediaryRegistrationDetails.fromOtherIossIntermediaryRegistrations)
        .getOrElse(Seq.empty)
    } else {
      Seq.empty
    }
  }

  def onSubmit(waypoints: Waypoints, incompletePromptShown: Boolean): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
    implicit request =>

      withCompleteDataAsync[PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber](
        data = getAllIncompletePreviousIntermediaryRegistrations _,
        onFailure = (incomplete: Seq[PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber]) => {
          if (incompletePromptShown) {
            incompletePreviousIntermediaryRegistrationRedirect(waypoints).map { redirectIncompletePage =>
              redirectIncompletePage.toFuture
            }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture)
          } else {
            Redirect(AddPreviousIntermediaryRegistrationPage().route(waypoints).url).toFuture
          }
        }) {

        getDerivedItems(waypoints, DeriveNumberOfPreviousIntermediaryRegistrations) { numberOfPreviousIntermediaryRegistrations =>

          val canAddCountries: Boolean = numberOfPreviousIntermediaryRegistrations < Country.euCountries.size

          val previousIntermediaryRegistrationSummary: SummaryList = PreviousIntermediaryRegistrationsSummary
            .row(waypoints, request.userAnswers, AddPreviousIntermediaryRegistrationPage(), Seq.empty)

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, previousIntermediaryRegistrationSummary, canAddCountries)).toFuture,

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(AddPreviousIntermediaryRegistrationPage(), value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(AddPreviousIntermediaryRegistrationPage()
                .navigate(
                  waypoints.calculateNextStepWaypoints(
                    value,
                    AddPreviousIntermediaryRegistrationPage(),
                    AddPreviousIntermediaryRegistrationPage.checkModeUrlFragment
                  ),
                  request.userAnswers,
                  updatedAnswers).route
              )
          )
        }
      }
  }
}
