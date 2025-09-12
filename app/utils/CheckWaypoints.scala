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

package utils

import models.CheckMode
import pages.{CheckAnswersPage, CheckYourAnswersPage, NonEmptyWaypoints, Waypoint, WaypointPage, Waypoints}

object CheckWaypoints {

  implicit class CheckWaypointsOps(waypoints: Waypoints) {

    private def isInMode(pages: CheckAnswersPage*) = {
      waypoints match {
        case nonEmptyWaypoints: NonEmptyWaypoints =>
          pages.exists(page => nonEmptyWaypoints.waypoints.toList.map(_.urlFragment).contains(page.urlFragment))

        case _ =>
          false
      }
    }

    def inCheck: Boolean = {
      isInMode(CheckYourAnswersPage)
    }

    def getNextCheckYourAnswersPageFromWaypoints: Option[CheckAnswersPage] = {
      waypoints match {
        case nonEmptyWaypoints: NonEmptyWaypoints =>
          List(CheckYourAnswersPage).find { page =>
            nonEmptyWaypoints.waypoints.toList.map(_.urlFragment).contains(CheckYourAnswersPage.urlFragment)
          }
        case _ =>
          None
      }
    }

    def calculateNextStepWaypoints(
                                    value: Boolean,
                                    page: WaypointPage,
                                    urlFragment: String
                                  ): Waypoints = {
      if (value && waypoints.inCheck) {
        waypoints.setNextWaypoint(Waypoint(
          page = page,
          mode = CheckMode,
          urlFragment = urlFragment)
        )
      } else {
        waypoints
      }
    }
  }
}
