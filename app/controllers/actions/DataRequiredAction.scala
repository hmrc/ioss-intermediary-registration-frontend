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
import controllers.filters.routes as filterRoutes
import controllers.routes
import models.etmp.display.RegistrationWrapper
import models.requests.{AuthenticatedDataRequest, AuthenticatedOptionalDataRequest, UnauthenticatedDataRequest, UnauthenticatedOptionalDataRequest}
import models.responses.ErrorResponse
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedDataRequiredActionImpl @Inject()(
                                                     registrationConnector: RegistrationConnector,
                                                     isInAmendMode: Boolean
                                                   )(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[AuthenticatedOptionalDataRequest, AuthenticatedDataRequest] {

  override protected def refine[A](request: AuthenticatedOptionalDataRequest[A]): Future[Either[Result, AuthenticatedDataRequest[A]]] = {

    request.userAnswers match {
      case None =>
        Left(Redirect(routes.JourneyRecoveryController.onPageLoad())).toFuture
      case Some(data) =>
        val eventualMaybeRegistrationWrapper = {
          if (isInAmendMode) {
            val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request.request, request.session)
            val intermediaryNumber = request.intermediaryNumber.getOrElse(throw new Exception("No Intermediary Number"))
            registrationConnector.displayRegistration(intermediaryNumber)(hc).flatMap {
              case Left(error: ErrorResponse) =>
                Future.failed(new RuntimeException(s"Failed to retrieve registration whilst in amend mode: ${error.body}"))

              case Right(registrationWrapper: RegistrationWrapper) =>
                Some(registrationWrapper).toFuture
            }
          } else {
            None.toFuture
          }
        }

        eventualMaybeRegistrationWrapper.map { maybeRegistrationWrapper =>
          Right(
            AuthenticatedDataRequest(
              request = request,
              credentials = request.credentials,
              vrn = request.vrn,
              enrolments = request.enrolments,
              userAnswers = data,
              iossNumber = request.iossNumber,
              numberOfIossRegistrations = request.numberOfIossRegistrations,
              latestIossRegistration = request.latestIossRegistration,
              latestOssRegistration = request.latestOssRegistration,
              intermediaryNumber = request.intermediaryNumber,
              registrationWrapper = maybeRegistrationWrapper
            )
          )
        }
    }
  }
}

class AuthenticatedDataRequiredAction @Inject()(registrationConnector: RegistrationConnector)(implicit executionContext: ExecutionContext) {

  def apply(isInAmendMode: Boolean): AuthenticatedDataRequiredActionImpl = {
    new AuthenticatedDataRequiredActionImpl(registrationConnector, isInAmendMode)
  }
}

class UnauthenticatedDataRequiredAction @Inject()(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[UnauthenticatedOptionalDataRequest, UnauthenticatedDataRequest] {

  override protected def refine[A](request: UnauthenticatedOptionalDataRequest[A]): Future[Either[Result, UnauthenticatedDataRequest[A]]] = {

    request.userAnswers match {
      case None =>
        Left(Redirect(filterRoutes.RegisteredForIossIntermediaryInEuController.onPageLoad())).toFuture
      case Some(data) =>
        Right(UnauthenticatedDataRequest(request.request, request.userId, data)).toFuture
    }
  }
}