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

package services

import base.SpecBase
import connectors.SaveForLaterConnector
import models.SavedUserAnswers
import models.requests.AuthenticatedDataRequest
import models.responses.InternalServerError
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{BankDetailsPage, ContactDetailsPage, JourneyRecoveryPage}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Call}
import play.api.test.FakeRequest
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global

class SaveForLaterServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private implicit lazy val hc: HeaderCarrier = new HeaderCarrier()

  private val request: AuthenticatedDataRequest[AnyContent] = {
    AuthenticatedDataRequest(FakeRequest("GET", "/"), testCredentials, vrn, Enrolments(Set.empty), emptyUserAnswers, None, 1, None, None, None)
  }

  private implicit val dataRequest: AuthenticatedDataRequest[AnyContent] = {
    AuthenticatedDataRequest[AnyContent](request, testCredentials, vrn, Enrolments(Set.empty), emptyUserAnswers, None, 1, None, None, None)
  }

  private val mockAuthenticatedUserAnswersRepository: AuthenticatedUserAnswersRepository = mock[AuthenticatedUserAnswersRepository]
  private val mockSaveForLaterConnector: SaveForLaterConnector = mock[SaveForLaterConnector]

  private val savedUserAnswers: SavedUserAnswers = arbitrarySavedUserAnswers.arbitrary.sample.value

  private val originLocation: Call = ContactDetailsPage.route(waypoints)
  private val redirectLocation: Call = BankDetailsPage.route(waypoints)
  private val errorRedirectLocation: Call = JourneyRecoveryPage.route(waypoints)

  override def beforeEach(): Unit = {
    Mockito.reset(mockAuthenticatedUserAnswersRepository)
    Mockito.reset(mockSaveForLaterConnector)
  }

  "SaveForLaterService" - {

    ".savedUserAnswers" - {

      "must redirect to the correct location when saved user answers are successfully submitted" in {

        when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture
        when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Right(Some(savedUserAnswers)).toFuture

        val service = new SaveForLaterService(mockAuthenticatedUserAnswersRepository, mockSaveForLaterConnector)

        val result = service.saveUserAnswers(waypoints, originLocation, redirectLocation).futureValue

        result `mustBe` Redirect(redirectLocation)
        verify(mockSaveForLaterConnector, times(1)).submit(any())(any())
        verify(mockAuthenticatedUserAnswersRepository, times(1)).set(any())
      }

      "must redirect to Journey Recovery when Right(None) is returned" in {

        when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn false.toFuture
        when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Right(None).toFuture

        val service = new SaveForLaterService(mockAuthenticatedUserAnswersRepository, mockSaveForLaterConnector)

        val result = service.saveUserAnswers(waypoints, originLocation, redirectLocation).futureValue

        result `mustBe` Redirect(errorRedirectLocation)
        verify(mockSaveForLaterConnector, times(1)).submit(any())(any())
        verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
      }

      "must redirect to Journey Recovery when Left(error) is returned" in {

        when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn false.toFuture
        when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Left(InternalServerError).toFuture

        val service = new SaveForLaterService(mockAuthenticatedUserAnswersRepository, mockSaveForLaterConnector)

        val result = service.saveUserAnswers(waypoints, originLocation, redirectLocation).futureValue

        result `mustBe` Redirect(errorRedirectLocation)
        verify(mockSaveForLaterConnector, times(1)).submit(any())(any())
        verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
      }
    }
  }
}
