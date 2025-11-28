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
import models.{CheckMode, UserAnswers}
import pages.amend.ChangeRegistrationPage
import pages.{ContactDetailsPage, EmptyWaypoints, Waypoint}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.EmailVerificationCodesExceededView

class EmailVerificationCodesExceededControllerSpec extends SpecBase {

  val userAnswersWithContactDetails: UserAnswers = emptyUserAnswers.set(ContactDetailsPage, contactDetails).success.value

  "EmailVerificationCodesExceededController Controller" - {

    "must return OK and the correct view for a GET during a standard journey" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithContactDetails), registrationWrapper = Some(registrationWrapper)).build()

      running(application) {
        val request = FakeRequest(GET, routes.EmailVerificationCodesExceededController.onPageLoad(EmptyWaypoints).url)

        val result = route(application, request).value

        val regDetailsUrl = ChangeRegistrationPage.route(EmptyWaypoints).url

        val view = application.injector.instanceOf[EmailVerificationCodesExceededView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(inAmend = false, registrationDetailsUrl = regDetailsUrl)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET during the Amend journey" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithContactDetails), registrationWrapper = Some(registrationWrapper))
        .build()

      val waypointsInAmend = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
      

      running(application) {
        val request = FakeRequest(GET, routes.EmailVerificationCodesExceededController.onPageLoad(waypointsInAmend).url)

        val result = route(application, request).value

        val regDetailsUrl = ChangeRegistrationPage.route(waypointsInAmend).url

        val view = application.injector.instanceOf[EmailVerificationCodesExceededView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(inAmend = true, registrationDetailsUrl = regDetailsUrl)(request, messages(application)).toString
      }
    }
  }
}
