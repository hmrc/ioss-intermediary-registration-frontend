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

package controllers.actions

import base.SpecBase
import connectors.SaveForLaterConnector
import models.ossRegistration.OssRegistration
import models.requests.AuthenticatedOptionalDataRequest
import models.{SavedUserAnswers, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar
import pages.SavedProgressPage
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.auth.core.Enrolments
import utils.FutureSyntax.FutureOps

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SaveForLaterRetrievalActionSpec extends SpecBase with MockitoSugar with EitherValues with BeforeAndAfterEach {

  private val mockAuthenticatedUserAnswersRepository = mock[AuthenticatedUserAnswersRepository]
  private val mockSaveForLaterConnector = mock[SaveForLaterConnector]

  class Harness(
                 authenticatedUserAnswersRepository: AuthenticatedUserAnswersRepository,
                 saveForLaterConnector: SaveForLaterConnector
               ) extends SaveForLaterRetrievalAction(authenticatedUserAnswersRepository, saveForLaterConnector) {

    def callRefine[A](request: AuthenticatedOptionalDataRequest[A]): Future[Either[Result, AuthenticatedOptionalDataRequest[A]]] = refine(request)
  }

  override def beforeEach(): Unit = {
    Mockito.reset(mockAuthenticatedUserAnswersRepository)
    Mockito.reset(mockSaveForLaterConnector)
  }

  "SaveForLaterRetrievalAction" - {

    "must apply the user answers saved in the session when user answers in session are present with the continueUrl set" in {

      val answers: UserAnswers = UserAnswers(userAnswersId).set(SavedProgressPage, "/url").success.value
      val action = new Harness(mockAuthenticatedUserAnswersRepository, mockSaveForLaterConnector)
      val request = FakeRequest(GET, "/test/url?k=session-id")

      val result = action.callRefine(
        AuthenticatedOptionalDataRequest(
          request = request,
          credentials = testCredentials,
          vrn = vrn,
          enrolments = Enrolments(Set.empty),
          userAnswers = Some(answers),
          iossNumber = None,
          numberOfIossRegistrations = 1,
          latestIossRegistration = None,
          latestOssRegistration = None
        )
      ).futureValue

      result.value.userAnswers `mustBe` Some(answers)
      verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
      verifyNoInteractions(mockSaveForLaterConnector)
    }

    "when there are no answers present in the session with the continueUrl set" - {

      "must retrieve saved answers when present" in {

        val instant = Instant.now
        val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
        val answers: UserAnswers = UserAnswers(userAnswersId, lastUpdated = Instant.now(stubClock)).set(SavedProgressPage, "/url").success.value

        when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture
        when(mockSaveForLaterConnector.get()(any())) thenReturn Right(Some(SavedUserAnswers(vrn, answers.data, None, instant))).toFuture

        val action = new Harness(mockAuthenticatedUserAnswersRepository, mockSaveForLaterConnector)
        val request = FakeRequest(GET, "/test/url?k=session-id")

        val result = action.callRefine(
          AuthenticatedOptionalDataRequest(
            request = request,
            credentials = testCredentials,
            vrn = vrn,
            enrolments = Enrolments(Set.empty),
            userAnswers = Some(UserAnswers(userAnswersId)),
            iossNumber = None,
            numberOfIossRegistrations = 1,
            latestIossRegistration = None,
            latestOssRegistration = None
          )
        ).futureValue

        result.value.userAnswers `mustBe` Some(answers)
        verify(mockSaveForLaterConnector, times(1)).get()(any())
        verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(answers))
      }

      "must apply user answers in request when no saved answers present" in {

        when(mockSaveForLaterConnector.get()(any())) thenReturn Right(None).toFuture

        val action = new Harness(mockAuthenticatedUserAnswersRepository, mockSaveForLaterConnector)
        val request = FakeRequest(GET, "/test/url?k=session-id")

        val result = action.callRefine(
          AuthenticatedOptionalDataRequest(request,
            credentials = testCredentials,
            vrn = vrn,
            enrolments = Enrolments(Set.empty),
            userAnswers = Some(emptyUserAnswers),
            iossNumber = None,
            numberOfIossRegistrations = 1,
            latestIossRegistration = None,
            latestOssRegistration = None
          )
        ).futureValue

        result.value.userAnswers `mustBe` Some(emptyUserAnswers)
        verify(mockSaveForLaterConnector, times(1)).get()(any())
        verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
      }
    }

    "must include the latestOssRegistration in the transformed request when latestOssRegistration is provided" in {

      val latestOssRegistration: Option[OssRegistration] = ossRegistration
      val answers = UserAnswers(userAnswersId).set(SavedProgressPage, "/url").success.value

      val action = new Harness(mockAuthenticatedUserAnswersRepository, mockSaveForLaterConnector)
      val request = FakeRequest(GET, "/test/url?k=session-id")

      val result = action.callRefine(
        AuthenticatedOptionalDataRequest(
          request = request,
          credentials = testCredentials,
          vrn = vrn,
          enrolments = Enrolments(Set.empty),
          userAnswers = Some(answers),
          iossNumber = None,
          numberOfIossRegistrations = 1,
          latestIossRegistration = None,
          latestOssRegistration = latestOssRegistration
        )
      ).futureValue

      result.value.userAnswers `mustBe` Some(answers)
      result.value.latestOssRegistration `mustBe` latestOssRegistration
      verifyNoInteractions(mockSaveForLaterConnector)
      verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
    }

    "must set latestOssRegistration as None in the transformed request when latestOssRegistration is not provided" in {

      val answers = UserAnswers(userAnswersId).set(SavedProgressPage, "/url").success.value
      val action = new Harness(mockAuthenticatedUserAnswersRepository, mockSaveForLaterConnector)
      val request = FakeRequest(GET, "/test/url?k=session-id")

      val result = action.callRefine(
        AuthenticatedOptionalDataRequest(
          request = request,
          credentials = testCredentials,
          vrn = vrn,
          enrolments = Enrolments(Set.empty),
          userAnswers = Some(answers),
          iossNumber = None,
          numberOfIossRegistrations = 1,
          latestIossRegistration = None,
          latestOssRegistration = None
        )
      ).futureValue

      result.value.userAnswers `mustBe` Some(answers)
      result.value.latestOssRegistration `mustBe` None
      verifyNoInteractions(mockSaveForLaterConnector)
      verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
    }
  }
}
