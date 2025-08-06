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

import base.SpecBase
import config.FrontendAppConfig
import formats.Format.saveForLaterDateFormatter
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.SavedProgressView

import java.time.temporal.ChronoUnit

class SavedProgressControllerSpec extends SpecBase {

  private val continueUrl: RedirectUrl = RedirectUrl("/continueUrl")
  private val s4lTtl: Int = 28

  private val answersExpiryDate: String = emptyUserAnswers.lastUpdated.plus(s4lTtl, ChronoUnit.DAYS)
    .atZone(stubClockAtArbitraryDate.getZone).toLocalDate.format(saveForLaterDateFormatter)

  "SavedProgress Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.SavedProgressController.onPageLoad(waypoints, continueUrl).url)

        val result = route(application, request).value

        val config = application.injector.instanceOf[FrontendAppConfig]

        val view = application.injector.instanceOf[SavedProgressView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(answersExpiryDate, "/continueUrl", config.loginUrl)(request, messages(application)).toString
      }
    }
  }
}
