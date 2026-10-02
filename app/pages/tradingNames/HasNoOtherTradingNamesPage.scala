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

package pages.tradingNames

import controllers.tradingNames.routes
import models.{Index, UserAnswers}
import pages.amend.{ChangePreviousRegistrationPage, ChangeRegistrationPage}
import pages.previousIntermediaryRegistrations.HasPreviouslyRegisteredAsIntermediaryPage
import pages.rejoin.RejoinSchemePage
import pages.{CheckYourAnswersPage, JourneyRecoveryPage, NonEmptyWaypoints, Page, QuestionPage, RecoveryOps, Waypoints}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.tradingNames.AllTradingNamesQuery
import utils.AmendWaypoints.AmendWaypointsOps
import utils.CheckWaypoints.CheckWaypointsOps

case object HasNoOtherTradingNamesPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "hasTradingName"

  override def route(waypoints: Waypoints): Call = {
    routes.HasNoOtherTradingNamesController.onPageLoad(waypoints)
  }

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    answers.get(this).map {
      case true => HasPreviouslyRegisteredAsIntermediaryPage
      case false => TradingNamePage(Index(0))
    }.orRecover

  override protected def nextPageCheckMode(waypoints: NonEmptyWaypoints, answers: UserAnswers): Page = {
    (answers.get(this), answers.get(AllTradingNamesQuery)) match {
      case (Some(false), Some(tradingNames)) if tradingNames.nonEmpty => AddTradingNamePage()
      case (Some(false), _) => TradingNamePage(Index(0))
      case (Some(true), Some(tradingNames)) if tradingNames.nonEmpty => DeleteAllTradingNamesPage
      case (Some(true), _) if waypoints.inRejoin => RejoinSchemePage
      case (Some(true), _) if waypoints.inAmend => ChangeRegistrationPage
      case (Some(true), _) if waypoints.inCheck => CheckYourAnswersPage
      case (Some(true), _) if waypoints.inPreviousRegistrationAmend => ChangePreviousRegistrationPage
      case (Some(true), _) => HasPreviouslyRegisteredAsIntermediaryPage
      case _ => JourneyRecoveryPage
    }
  }
}
