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
import pages.{CheckYourAnswersPage, EmptyWaypoints, Waypoint}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.govuk.summarylist.*
import views.html.CheckYourAnswersView

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            cc: UnauthenticatedControllerComponents,
                                            view: CheckYourAnswersView
                                          ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(): Action[AnyContent] = cc.identifyAndGetData {
    implicit request =>

      val thisPage = CheckYourAnswersPage
      val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, CheckYourAnswersPage.urlFragment))
      val maybeHasTradingNameSummaryRow = HasTradingNameSummary.row(request.userAnswers, waypoints, thisPage)
      val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(request.userAnswers, waypoints, thisPage)
      
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
        ).flatten
      )

      Ok(view(waypoints, list))
  }
}
