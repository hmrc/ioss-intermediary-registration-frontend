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
import models.CheckMode
import pages.{EmptyWaypoints, Waypoint}
import pages.amend.ChangeRegistrationPage
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.EmailVerificationCodesAndEmailsExceededView

class EmailVerificationCodesAndEmailsExceededControllerSpec extends SpecBase {

  "EmailVerificationCodesAndEmailsExceeded Controller" - {

    "must return OK and the correct view for a GET during a standard journey" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad(EmptyWaypoints).url)

        val result = route(application, request).value

        val regDetailsUrl = ChangeRegistrationPage.route(EmptyWaypoints).url

        val view = application.injector.instanceOf[EmailVerificationCodesAndEmailsExceededView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(inAmend = false, registrationDetailsUrl = regDetailsUrl)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET during the Amend journey" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      val waypointsInAmend = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))

      running(application) {
        val request = FakeRequest(GET, routes.EmailVerificationCodesAndEmailsExceededController.onPageLoad(waypointsInAmend).url)

        val result = route(application, request).value

        val regDetailsUrl = ChangeRegistrationPage.route(waypointsInAmend).url

        val view = application.injector.instanceOf[EmailVerificationCodesAndEmailsExceededView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(inAmend = true, registrationDetailsUrl = regDetailsUrl)(request, messages(application)).toString
      }
    }
  }
}
