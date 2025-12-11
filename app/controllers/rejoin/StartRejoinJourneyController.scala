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

package controllers.rejoin

import connectors.{RegistrationConnector, ReturnStatusConnector}
import controllers.CheckOutstandingReturns.existsOutstandingReturns
import controllers.actions.*
import controllers.rejoin.validation.RejoinRegistrationValidation
import logging.Logging
import models.responses.ErrorResponse
import pages.Waypoints
import pages.rejoin.{CannotRejoinPage, RejoinSchemePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.AuthenticatedUserAnswersRepository
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StartRejoinJourneyController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              cc: AuthenticatedControllerComponents,
                                              registrationConnector: RegistrationConnector,
                                              returnStatusConnector: ReturnStatusConnector,
                                              val controllerComponents: MessagesControllerComponents,
                                              registrationService: RegistrationService,
                                              rejoinRegistrationValidation: RejoinRegistrationValidation,
                                              authenticatedUserAnswersRepository: AuthenticatedUserAnswersRepository,
                                              clock: Clock
                                            )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireIntermediaryAndVerifyEmail(waypoints, inAmend = false, inRejoin = true).async {
    implicit request =>

      (for {
        registrationWrapperResponse <- registrationConnector.displayRegistration(request.intermediaryNumber)
        currentReturnsResponse <- returnStatusConnector.getCurrentReturns(request.intermediaryNumber)
      } yield {
        val currentReturns = getResponseValue(currentReturnsResponse)
        val registrationWrapper = getResponseValue(registrationWrapperResponse)

        val currentDate: LocalDate = LocalDate.now(clock)
        val canRejoin = registrationWrapper.etmpDisplayRegistration.canRejoinScheme(currentDate)

        if (canRejoin && !existsOutstandingReturns(currentReturns)) {
          rejoinRegistrationValidation.validateEuRegistrations(
            waypoints, registrationWrapper.etmpDisplayRegistration
          )(hc(request), ec, request.request).flatMap {
            case Left(redirect) =>
              logger.info(s"Failed when validating EuRegistrations, redirecting to ${redirect.url}")
              Redirect(redirect).toFuture

            case _ =>
              for {
                userAnswers <- registrationService.toUserAnswers(request.userId, registrationWrapper)
                _ <- authenticatedUserAnswersRepository.set(userAnswers)
              } yield {
                Redirect(RejoinSchemePage.route(waypoints).url)
              }
          }
        } else {
          logger.warn("Cannot rejoin registration")
          Redirect(CannotRejoinPage.route(waypoints).url).toFuture
        }

      }).flatten
  }

  private def getResponseValue[A](response: Either[ErrorResponse, A]): A = {
    response match {
      case Right(value) => value
      case Left(error) =>
        val exception = new Exception(error.body)
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }
}

