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

import connectors.RegistrationConnector
import controllers.actions.FakeAuthenticatedDataRequiredAction.mockRegistrationConnector
import controllers.routes
import models.UserAnswers
import models.etmp.display.RegistrationWrapper
import models.requests.{AuthenticatedDataRequest, AuthenticatedOptionalDataRequest}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import utils.FutureSyntax.FutureOps

import scala.concurrent.{ExecutionContext, Future}

case class FakeAuthenticatedDataRequiredAction(
                                                isInAmendMode: Boolean,
                                                dataToReturn: Option[UserAnswers],
                                                registrationWrapper: Option[RegistrationWrapper]
                                              )
  extends AuthenticatedDataRequiredActionImpl(mockRegistrationConnector, isInAmendMode = isInAmendMode)(ExecutionContext.Implicits.global) {

  override protected def refine[A](request: AuthenticatedOptionalDataRequest[A]): Future[Either[Result, AuthenticatedDataRequest[A]]] = {

    dataToReturn match {
      case Some(data) =>
        Right(AuthenticatedDataRequest(
          request,
          request.credentials,
          request.vrn,
          request.enrolments,
          data,
          request.iossNumber,
          request.numberOfIossRegistrations,
          request.latestIossRegistration,
          request.latestOssRegistration,
          request.intermediaryNumber,
          registrationWrapper
        )).toFuture

      case None =>
        Left(Redirect(routes.JourneyRecoveryController.onPageLoad())).toFuture
    }
  }
}

class FakeAuthenticatedDataRequiredActionProvider(
                                                   dataToReturn: Option[UserAnswers],
                                                   registrationWrapper: Option[RegistrationWrapper]
                                                 )
  extends AuthenticatedDataRequiredAction(mockRegistrationConnector)(ExecutionContext.Implicits.global) {

  override def apply(isInAmendMode: Boolean): FakeAuthenticatedDataRequiredAction = {
    new FakeAuthenticatedDataRequiredAction(isInAmendMode, dataToReturn, registrationWrapper)
  }
}

object FakeAuthenticatedDataRequiredAction {

  val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
}
