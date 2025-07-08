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

package pages.previousIntermediaryRegistrations

import controllers.previousIntermediaryRegistrations.routes
import models.{Index, UserAnswers}
import pages.euDetails.HasFixedEstablishmentPage
import pages.{JourneyRecoveryPage, NonEmptyWaypoints, Page, QuestionPage, RecoveryOps, Waypoints}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.previousIntermediaryRegistrations.AllPreviousIntermediaryRegistrationsQuery

case object HasPreviouslyRegisteredAsIntermediaryPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "hasPreviouslyRegisteredAsIntermediary"

  override def route(waypoints: Waypoints): Call = {
    routes.HasPreviouslyRegisteredAsIntermediaryController.onPageLoad(waypoints)
  }

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page = {
    answers.get(this).map {
      case true => PreviousEuCountryPage(Index(0))
      case false => HasFixedEstablishmentPage()
    }.orRecover
  }

  override protected def nextPageCheckMode(waypoints: NonEmptyWaypoints, answers: UserAnswers): Page = {
    (answers.get(this), answers.get(AllPreviousIntermediaryRegistrationsQuery)) match {
      case (Some(true), Some(previousIntermediaryRegistrations)) if previousIntermediaryRegistrations.nonEmpty =>
        AddPreviousIntermediaryRegistrationPage()

      case (Some(true), _) => PreviousEuCountryPage(Index(0))
      case (Some(false), Some(previousIntermediaryRegistrations)) if previousIntermediaryRegistrations.nonEmpty =>
        DeleteAllPreviousIntermediaryRegistrationsPage

      case (Some(false), _) => HasFixedEstablishmentPage()
      case _ => JourneyRecoveryPage
    }
  }
}
