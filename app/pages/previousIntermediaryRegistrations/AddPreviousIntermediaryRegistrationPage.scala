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
import models.{Country, Index, UserAnswers}
import pages.euDetails.TaxRegisteredInEuPage
import pages.{AddItemPage, Page, QuestionPage, RecoveryOps, Waypoints}
import play.api.libs.json.{JsObject, JsPath}
import play.api.mvc.Call
import queries.Derivable
import queries.previousIntermediaryRegistrations.DeriveNumberOfPreviousIntermediaryRegistrations

final case class AddPreviousIntermediaryRegistrationPage(override val index: Option[Index] = None) extends AddItemPage(index) with QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "addPreviousIntermediaryRegistration"

  override def route(waypoints: Waypoints): Call = {
    routes.AddPreviousIntermediaryRegistrationController.onPageLoad(waypoints)
  }

  override val normalModeUrlFragment: String = AddPreviousIntermediaryRegistrationPage.normalModeUrlFragment
  override val checkModeUrlFragment: String = AddPreviousIntermediaryRegistrationPage.checkModeUrlFragment

  override def isTheSamePage(other: Page): Boolean = other match {
    case _: AddPreviousIntermediaryRegistrationPage => true
    case _ => false
  }

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page = {
    answers.get(this).map {
      case true =>
        index.map { i =>
          if (i.position + 1 < Country.euCountries.size) {
            PreviousEuCountryPage(Index(i.position + 1))
          } else {
            TaxRegisteredInEuPage
          }
        }.getOrElse {
          answers
            .get(deriveNumberOfItems).map { n =>
              PreviousEuCountryPage(Index(n))
            }.orRecover
        }

      case _ => TaxRegisteredInEuPage
    }.orRecover
  }

  override def deriveNumberOfItems: Derivable[Seq[JsObject], Int] = DeriveNumberOfPreviousIntermediaryRegistrations
}

object AddPreviousIntermediaryRegistrationPage {

  val normalModeUrlFragment: String = "add-previous-intermediary-registration"
  val checkModeUrlFragment: String = "change-add-previous-intermediary-registration"
}
