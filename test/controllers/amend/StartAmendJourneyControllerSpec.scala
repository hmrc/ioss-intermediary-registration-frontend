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

package controllers.amend

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import pages.{EmptyWaypoints, Waypoints}
import pages.amend.ChangeRegistrationPage
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class StartAmendJourneyControllerSpec extends SpecBase with MockitoSugar {

  private val waypoints: Waypoints = EmptyWaypoints

  "StartAmendJourney Controller" - {
    "must redirect to Change Registration and render the stubbed user answers" in {

      val application = applicationBuilder(
        userAnswers = Some(completeUserAnswersWithVatInfo),
      ).build()
      
      running(application) {
        val request = FakeRequest(GET, controllers.amend.routes.StartAmendJourneyController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe ChangeRegistrationPage.route(waypoints).url
      }
    }
  }
}
