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

package controllers.rejoin.validation

import controllers.filters.routes as filterRoutes
import logging.Logging
import models.etmp.EtmpIntermediaryDetails
import models.etmp.display.{EtmpDisplayEuRegistrationDetails, EtmpDisplayRegistration}
import models.requests.AuthenticatedDataRequest
import pages.{JourneyRecoveryPage, Waypoints}
import play.api.mvc.Call
import services.core.{EuRegistrationsValidationService, InvalidActiveTrader, InvalidQuarantinedTrader, PreviousValidationInvalidResult}
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RejoinRegistrationValidation @Inject()(
                                              euRegistrationsValidationService: EuRegistrationsValidationService
                                            ) extends Logging {

  def validateEuRegistrations(
                               waypoints: Waypoints,
                               etmpDisplayRegistration: EtmpDisplayRegistration
                             )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: AuthenticatedDataRequest[_]): Future[Either[Call, Boolean]] = {

    euRegistrationsValidationService.validateEuRegistrationDetails(etmpDisplayRegistration.schemeDetails.euRegistrationDetails)
      .flatMap {
        case Left(previousValidationInvalidResult) =>
          determineRedirect(waypoints, etmpDisplayRegistration.schemeDetails.euRegistrationDetails, previousValidationInvalidResult)
        case Right(_) =>
          findInfractionInOtherIossIntermediaryRegistration(waypoints, etmpDisplayRegistration.intermediaryDetails)
      }
  }

  private def determineRedirect(
                                 waypoints: Waypoints,
                                 euRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails],
                                 previousValidationInvalidResult: PreviousValidationInvalidResult
                               ): Future[Left[Call, Nothing]] = {
    previousValidationInvalidResult match {
      case invalidActiveTraderResult: InvalidActiveTrader =>
        logger.info(
          s"EU Registration ${euRegistrationDetails.map(_.issuedBy == invalidActiveTraderResult.countryCode)} has been mapped to InvalidActiveTraderResult"
        )
        // TODO -> Redirect???
        Left(JourneyRecoveryPage.route(waypoints)).toFuture
      // TODO
      //            FixedEstablishmentVRNAlreadyRegisteredController.onPageLoad(
      //              waypoints,
      //              invalidActiveTraderResult.countryCode
      //            )).toFuture
      case InvalidQuarantinedTrader =>
        logger.info(
          s"EU Registration $euRegistrationDetails has been mapped to InvalidQuarantinedTraderResult"
        )
        // TODO -> Redirect???
        // TODO Left(ExcludedVRNController.onPageLoad()).toFuture
        Left(JourneyRecoveryPage.route(waypoints)).toFuture
    }
  }

  private def findInfractionInOtherIossIntermediaryRegistration(
                                                                 waypoints: Waypoints,
                                                                 maybeIntermediaryDetails: Option[EtmpIntermediaryDetails]
                                                               )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: AuthenticatedDataRequest[_]): Future[Either[Call, Boolean]] = {
    maybeIntermediaryDetails match {
      case Some(intermediaryDetails) =>
        euRegistrationsValidationService.validateOtherIossIntermediaryRegistrationDetails(intermediaryDetails.otherIossIntermediaryRegistrations)
          .flatMap {
            case Left(previousValidationInvalidResult) =>
              previousValidationInvalidResult match {
                case InvalidActiveTrader(countryCode, _) =>
                  logger.info(
                    s"Other Ioss Intermediary Registrations ${intermediaryDetails.otherIossIntermediaryRegistrations.map(_.issuedBy == countryCode)} has been mapped to InvalidActiveTraderResult"
                  )
                  // TODO -> Redirect here???
                  Left(filterRoutes.SchemeStillActiveController.onPageLoad(countryCode)).toFuture

                case InvalidQuarantinedTrader =>
                  logger.info(
                    s"Other Ioss Intermediary Registrations ${intermediaryDetails.otherIossIntermediaryRegistrations} has been mapped to InvalidActiveTraderResult"
                  )
                  // TODO -> Redirect here????
                  // TODO -> Require country code and exclusion date here
                  //                  Left(filterRoutes.OtherCountryExcludedAndQuarantinedController.onPageLoad()).toFuture
                  Left(JourneyRecoveryPage.route(waypoints)).toFuture
              }

            case Right(_) =>
              Right(true).toFuture
          }

      case None =>
        Right(true).toFuture
    }
  }
}