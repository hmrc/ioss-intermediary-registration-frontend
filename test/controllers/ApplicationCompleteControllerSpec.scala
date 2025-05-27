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
import models.UserAnswers
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.ApplicationCompleteView

class ApplicationCompleteControllerSpec extends SpecBase {

  private val userAnswers: UserAnswers = basicUserAnswersWithVatInfo
  private val intermediaryNumber = "IN9001234567"

  "ApplicationComplete Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value

        val view = application.injector.instanceOf[ApplicationCompleteView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          intermediaryNumber = intermediaryNumber,
          organisationName = vatCustomerInfo.organisationName.get,
          yourAccountUrl = config.intermediaryYourAccountUrl,
          feedbackUrl = config.feedbackUrl(request)
        )(request, messages(application)).toString
      }
    }
  }
}
