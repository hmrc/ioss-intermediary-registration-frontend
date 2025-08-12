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

import connectors.SaveForLaterConnector
import controllers.actions.FakeSaveForLaterRetrievalAction.{mockAuthenticatedUserAnswersRepository, mockSaveForLaterConnector}
import models.UserAnswers
import models.requests.AuthenticatedOptionalDataRequest
import org.scalatestplus.mockito.MockitoSugar.mock
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.domain.Vrn

import scala.concurrent.{ExecutionContext, Future}

class FakeSaveForLaterRetrievalAction(dataToReturn: Option[UserAnswers], vrn: Vrn)
  extends SaveForLaterRetrievalAction(mockAuthenticatedUserAnswersRepository, mockSaveForLaterConnector)(ExecutionContext.Implicits.global) {

  override protected def transform[A](request: AuthenticatedOptionalDataRequest[A]): Future[AuthenticatedOptionalDataRequest[A]] =
    Future.successful(
      AuthenticatedOptionalDataRequest(
        request.request,
        request.credentials,
        vrn,
        request.enrolments,
        dataToReturn,
        request.iossNumber,
        request.numberOfIossRegistrations,
        request.latestIossRegistration,
        request.latestOssRegistration
      ))
}

class FakeSaveForLaterRetrievalActionProvider(dataToReturn: Option[UserAnswers], vrn: Vrn)
  extends SaveForLaterRetrievalActionProvider(mockAuthenticatedUserAnswersRepository, mockSaveForLaterConnector)(ExecutionContext.Implicits.global) {

  override def apply(): SaveForLaterRetrievalAction = new FakeSaveForLaterRetrievalAction(dataToReturn, vrn)

}

object FakeSaveForLaterRetrievalAction {

  val mockAuthenticatedUserAnswersRepository: AuthenticatedUserAnswersRepository = mock[AuthenticatedUserAnswersRepository]
  val mockSaveForLaterConnector: SaveForLaterConnector = mock[SaveForLaterConnector]
}