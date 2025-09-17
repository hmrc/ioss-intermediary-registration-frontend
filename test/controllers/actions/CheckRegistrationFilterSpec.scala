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

package controllers.actions

import base.SpecBase
import config.FrontendAppConfig
import controllers.routes
import models.requests.AuthenticatedIdentifierRequest
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class CheckRegistrationFilterSpec extends SpecBase {

  private val intermediaryEnrolmentKey = "HMRC-IOSS-INT"
  private val enrolment: Enrolment = Enrolment(intermediaryEnrolmentKey, Seq.empty, "test", None)

  class Harness(inAmend: Boolean, config: FrontendAppConfig) extends CheckRegistrationFilterImpl(inAmend, config){
    def callFilter[A](request: AuthenticatedIdentifierRequest[A]): Future[Option[Result]] =
      filter(request)
  }

  ".filter" - {

    "must redirect to Already Registered Controller when an existing Intermediary enrolment is found" in {

      val app = applicationBuilder(None).build()

      running(app) {
        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(enrolment)), None, 1, None, None, None)
        val controller = new Harness(false, config)

        val result = controller.callFilter(request).futureValue

        result mustBe Some(Redirect(routes.AlreadyRegisteredController.onPageLoad().url))
      }

    }

    "must return None when an existing Intermediary enrolment is not found" in {

      val app = applicationBuilder(None).build()

      running(app) {
        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set.empty), None, 1, None, None, None)
        val controller = new Harness(false, config)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }
    }

    "must return None when in amend" in {

      val app = applicationBuilder(None).build()

      running(app) {
        val config = app.injector.instanceOf[FrontendAppConfig]
        val request = AuthenticatedIdentifierRequest(FakeRequest(), testCredentials, vrn, Enrolments(Set(enrolment)), None, 1, None, None, None)
        val controller = new Harness(true, config)

        val result = controller.callFilter(request).futureValue

        result mustBe None
      }
    }
  }
}
