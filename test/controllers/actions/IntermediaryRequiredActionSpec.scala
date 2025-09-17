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
import models.etmp.display.RegistrationWrapper
import models.requests.{AuthenticatedDataRequest, AuthenticatedMandatoryIntermediaryRequest}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import testutils.RegistrationData.etmpDisplayRegistration
import uk.gov.hmrc.auth.core.Enrolments

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IntermediaryRequiredActionSpec extends SpecBase with MockitoSugar {

  class Harness() extends IntermediaryRequiredActionImpl() {

    def callRefine[A](request: AuthenticatedDataRequest[A]):
    Future[Either[Result, AuthenticatedMandatoryIntermediaryRequest[A]]] = refine(request)
  }

  "IntermediaryRequiredAction" - {

    "when the user has logged in as an Organisation Admin with strong credentials but ioss enrolment" - {

      "must return Unauthorized" in {

        val action = new Harness()
        val request = FakeRequest(GET, "/test/url?k=session-id")
        val result = action.callRefine(AuthenticatedDataRequest(
          request = request,
          credentials = testCredentials,
          vrn = vrn,
          enrolments = Enrolments(Set.empty),
          userAnswers = emptyUserAnswersWithVatInfo,
          iossNumber = Some(iossNumber),
          numberOfIossRegistrations = 1,
          latestIossRegistration = None,
          latestOssRegistration = None,
          intermediaryNumber = None,
          registrationWrapper = None
        )).futureValue

        result mustBe Left(Unauthorized)
      }

      "must return Right" in {

        val registrationWrapper: RegistrationWrapper = RegistrationWrapper(vatCustomerInfo, etmpDisplayRegistration)

        val action = new Harness()
        val request = AuthenticatedDataRequest(
          request = FakeRequest(GET, "/test/url?k=session-id"),
          credentials = testCredentials,
          vrn = vrn,
          enrolments = Enrolments(Set.empty),
          iossNumber = Some(iossNumber),
          userAnswers = emptyUserAnswersWithVatInfo,
          numberOfIossRegistrations = 1,
          latestIossRegistration = None,
          latestOssRegistration = None,
          intermediaryNumber = Some(intermediaryNumber),
          registrationWrapper = Some(registrationWrapper)
        )

        val result = action.callRefine(request).futureValue

        val expectResult = AuthenticatedMandatoryIntermediaryRequest(
          request = request,
          credentials = testCredentials,
          vrn = vrn,
          enrolments = Enrolments(Set.empty),
          userAnswers = emptyUserAnswersWithVatInfo,
          numberOfIossRegistrations = 1,
          latestIossRegistration = None,
          latestOssRegistration = None,
          intermediaryNumber = intermediaryNumber,
          registrationWrapper = registrationWrapper
        )

        result mustBe Right(expectResult)
      }
    }
  }
}
