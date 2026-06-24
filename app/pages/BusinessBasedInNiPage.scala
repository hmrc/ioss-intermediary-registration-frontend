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

package pages

import models.UserAnswers
import pages.amend.RemoveBusinessFromIossPage
import pages.checkVatDetails.NiAddressPage
import play.api.libs.json.JsPath
import play.api.mvc.Call
import utils.AmendWaypoints.AmendWaypointsOps

import scala.util.Try

case object BusinessBasedInNiPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "businessBasedInNi"

  override def route(waypoints: Waypoints): Call =
    controllers.routes.BusinessBasedInNiController.onPageLoad(waypoints)

  override protected def nextPageCheckMode(waypoints: NonEmptyWaypoints, answers: UserAnswers): Page = {
    answers.get(this).map {
      case true => NiAddressPage
      case _ if waypoints.inRejoin => CannotRegisterNotNiBasedBusinessPage
      case _ => RemoveBusinessFromIossPage
    }.orRecover
  }

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] = {
    value match {
      case Some(true) =>
        for {
          answers1 <- userAnswers.remove(NonNiBasedCountryPage)
          answers2 <- answers1.remove(GlobalAddressPage)
        } yield answers2

      case _ =>
        Try(userAnswers)
    }
  }
}
