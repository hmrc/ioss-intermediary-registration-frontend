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

import com.google.inject.Inject
import controllers.actions.*
import models.CheckMode
import pages.{CheckYourAnswersPage, EmptyWaypoints, Waypoint, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, TaxRegisteredInEuSummary}
import viewmodels.checkAnswers.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediarySummary, PreviousIntermediaryRegistrationsSummary}
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.govuk.summarylist.*
import views.html.CheckYourAnswersView

import scala.concurrent.ExecutionContext

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            cc: AuthenticatedControllerComponents,
                                            view: CheckYourAnswersView
                                          )(implicit executionContext: ExecutionContext)
  extends FrontendBaseController with I18nSupport with CompletionChecks {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(): Action[AnyContent] = cc.authAndGetDataAndCheckVerifyEmail() {
    implicit request =>

      val thisPage = CheckYourAnswersPage
      val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, CheckYourAnswersPage.urlFragment))
      val maybeHasTradingNameSummaryRow = HasTradingNameSummary.row(request.userAnswers, waypoints, thisPage)
      val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(request.userAnswers, waypoints, thisPage)
      val maybeHasPreviouslyRegisteredAsIntermediaryRow = HasPreviouslyRegisteredAsIntermediarySummary
        .checkAnswersRow(waypoints, request.userAnswers, thisPage)
      val previouslyRegisteredAsIntermediaryRow = PreviousIntermediaryRegistrationsSummary.checkAnswersRow(waypoints, request.userAnswers, thisPage)
      val maybeTaxRegisteredInEuSummaryRow = TaxRegisteredInEuSummary.checkAnswersRow(waypoints, request.userAnswers, thisPage)
      val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(waypoints, request.userAnswers, thisPage)

      val list = SummaryListViewModel(
        rows = Seq(
          maybeHasTradingNameSummaryRow.map { hasTradingNameSummaryRow =>
            if (tradingNameSummaryRow.nonEmpty) {
              hasTradingNameSummaryRow.withCssClass("govuk-summary-list__row--no-border")
            } else {
              hasTradingNameSummaryRow
            }
          },
          tradingNameSummaryRow,
          maybeHasPreviouslyRegisteredAsIntermediaryRow.map { hasPreviouslyRegisteredAsIntermediaryRow =>
            if (previouslyRegisteredAsIntermediaryRow.nonEmpty) {
              hasPreviouslyRegisteredAsIntermediaryRow.withCssClass("govuk-summary-list__row--no-border")
            } else {
              hasPreviouslyRegisteredAsIntermediaryRow
            }
          },
          previouslyRegisteredAsIntermediaryRow,
          maybeTaxRegisteredInEuSummaryRow.map { taxRegisteredInEuSummaryRow =>
            if (euDetailsSummaryRow.nonEmpty) {
              taxRegisteredInEuSummaryRow.withCssClass("govuk-summary-list__row--no-border")
            } else {
              taxRegisteredInEuSummaryRow
            }
          },
          euDetailsSummaryRow
        ).flatten
      )

      val isValid: Boolean = validate()

      Ok(view(waypoints, list, isValid))
  }

  def onSubmit(waypoints: Waypoints, incompletePrompt: Boolean): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>

      getFirstValidationErrorRedirect(waypoints) match {
        case Some(errorRedirect) => if (incompletePrompt) {
          errorRedirect.toFuture
        } else {
          Redirect(CheckYourAnswersPage.route(waypoints).url).toFuture
        }

        case None =>

          for {
            _ <- cc.sessionRepository.set(request.userAnswers)
          } yield Redirect(CheckYourAnswersPage.navigate(waypoints, request.userAnswers, request.userAnswers).route)
      }
  }

  def onSubmit(): Action[AnyContent] = cc.authAndGetData() {
    implicit request =>
      //todo and incomplete prompt etc as part of CYA ticket
      val thisPage = CheckYourAnswersPage
      val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, thisPage.urlFragment))

      Redirect(CheckYourAnswersPage.navigate(waypoints, request.userAnswers, request.userAnswers).route)
  }
}
