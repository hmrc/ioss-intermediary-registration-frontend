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

import connectors.SaveForLaterConnector
import logging.Logging
import models.SavedUserAnswers
import models.requests.{AuthenticatedDataRequest, SaveForLaterRequest}
import pages.{JourneyRecoveryPage, SavedProgressPage, Waypoints}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SaveForLaterService @Inject()(
                                     authenticatedUserAnswersRepository: AuthenticatedUserAnswersRepository,
                                     saveForLaterConnector: SaveForLaterConnector
                                   ) extends Logging {

  def saveUserAnswers(waypoints: Waypoints,
                      originLocation: Call,
                      redirectLocation: Call
                      )(implicit request: AuthenticatedDataRequest[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    submitSavedUserAnswersAndRedirect(waypoints, originLocation.url, redirectLocation.url)
  }

  def submitSavedUserAnswersAndRedirect(
                                         waypoints: Waypoints,
                                         originLocation: String,
                                         redirectLocation: String,
                                       )(implicit request: AuthenticatedDataRequest[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    Future.fromTry(request.userAnswers.set(SavedProgressPage, originLocation)).flatMap { savedProgressAnswers =>

      val saveForLaterRequest: SaveForLaterRequest = SaveForLaterRequest(request.vrn, savedProgressAnswers.data, None)

      saveForLaterConnector.submit(saveForLaterRequest).flatMap {
        case Right(Some(_: SavedUserAnswers)) =>
          logger.info("Saving user answers")
          for {
            _ <- authenticatedUserAnswersRepository.set(savedProgressAnswers)
          } yield {
            Redirect(redirectLocation)
          }
        case Right(None) =>
          logger.warn(s"An unexpected result occurred when submitting saved user answers.")
          Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
        case Left(error) =>
          logger.error(s"There was an error submitting saved user answers: ${error.body}.")
          Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
      }
    }
  }
}
