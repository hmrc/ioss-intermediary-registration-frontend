/*
 * Copyright 2023 HM Revenue & Customs
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

package pages.rejoin

import controllers.rejoin.{routes => rejoinRoutes}
import models.UserAnswers
import pages.{CheckAnswersPage, Page, Waypoints}
import play.api.mvc.Call

object CannotRejoinPage extends CheckAnswersPage {

  override def isTheSamePage(other: Page): Boolean = other match {
    case RejoinSchemePage => true
    case _ => false
  }

  override val urlFragment: String = "rejoin-check-your-details"

  override def route(waypoints: Waypoints): Call =
    rejoinRoutes.CannotRejoinController.onPageLoad()

}
