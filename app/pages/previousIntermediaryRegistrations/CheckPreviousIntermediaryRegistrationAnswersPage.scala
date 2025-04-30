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
import models.{CheckMode, Index, NormalMode, UserAnswers}
import pages.{AddItemPage, Page, QuestionPage, RecoveryOps, Waypoint, Waypoints}
import play.api.libs.json.{JsObject, JsPath}
import play.api.mvc.Call
import queries.Derivable
import queries.previousIntermediaryRegistrations.DeriveNumberOfPreviousIntermediaryRegistrationsForCountry

case class CheckPreviousIntermediaryRegistrationAnswersPage(
                                                             countryIndex: Index,
                                                             registrationIndex: Option[Index] = None
                                                           ) extends AddItemPage(registrationIndex) with QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ "previousIntermediaryRegistrations" \ countryIndex.position \ toString

  override def toString: String = "checkPreviousIntermediaryRegistrationAnswers"

  override def route(waypoints: Waypoints): Call = {
    routes.CheckPreviousIntermediaryRegistrationAnswersController.onPageLoad(waypoints, countryIndex)
  }

  override val normalModeUrlFragment: String = s"previous-intermediary-registration-answers-${countryIndex.display}"
  override val checkModeUrlFragment: String = s"change-previous-intermediary-registration-answers-${countryIndex.display}"

  override def deriveNumberOfItems: Derivable[Seq[JsObject], Int] =
    DeriveNumberOfPreviousIntermediaryRegistrationsForCountry(countryIndex)

  override def isTheSamePage(other: Page): Boolean = other match {
    case p: CheckPreviousIntermediaryRegistrationAnswersPage => p.countryIndex == this.countryIndex
    case _ => false
  }

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page = {
    answers.get(this).map {
      case true =>
        registrationIndex
          .map(i => PreviousIntermediaryRegistrationNumberPage(countryIndex, Index(i.position + 1)))
          .getOrElse {
            answers
              .get(deriveNumberOfItems)
              .map(n => PreviousIntermediaryRegistrationNumberPage(countryIndex, Index(n)))
              .orRecover
          }

      case false =>
        ??? // TODO -> to Add another country page
    }.orRecover
  }
}

object CheckPreviousIntermediaryRegistrationAnswersPage {

  def waypointFromString(s: String): Option[Waypoint] = {

    val normalModePattern = """previous-intermediary-registration-answers-(\d{1,3})""".r.anchored
    val checkModePattern = """change-previous-intermediary-registration-answers-(\d{1,3})""".r.anchored

    s match {
      case normalModePattern(indexDisplay) =>
        Some(CheckPreviousIntermediaryRegistrationAnswersPage(Index(indexDisplay.toInt - 1), None).waypoint(NormalMode))

      case checkModePattern(indexDisplay) =>
        Some(CheckPreviousIntermediaryRegistrationAnswersPage(Index(indexDisplay.toInt - 1), None).waypoint(CheckMode))

      case _ => None
    }
  }
}
