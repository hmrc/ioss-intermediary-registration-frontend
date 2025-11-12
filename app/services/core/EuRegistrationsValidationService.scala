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

package services.core

import logging.Logging
import models.core.Match
import models.etmp.EtmpOtherIossIntermediaryRegistrations
import models.etmp.display.EtmpDisplayEuRegistrationDetails
import models.requests.AuthenticatedDataRequest
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

sealed trait PreviousValidationInvalidResult

case class InvalidActiveTrader(countryCode: String, memberState: String) extends PreviousValidationInvalidResult

case class InvalidQuarantinedTrader(countryCode: String, effectiveDate: String) extends PreviousValidationInvalidResult

class EuRegistrationsValidationService @Inject(
                                                coreRegistrationValidationService: CoreRegistrationValidationService,
                                                clock: Clock
                                              ) extends Logging {

  def validateEuRegistrationDetails(
                                     euRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails]
                                   )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: AuthenticatedDataRequest[_]): Future[Either[PreviousValidationInvalidResult, Boolean]] = {
    euRegistrationDetails.toList match {
      case ::(currentDetails, remaining) =>
        checkCurrentEtmpDisplayEuRegistrationDetails(currentDetails, currentDetails.vatNumber).flatMap { (maybeMatch: Option[Match]) =>
          maybeMatch match {
            case Some(foundMatch) =>
              remapMatchToError(currentDetails.issuedBy, foundMatch) match {
                case Some(previousValidationInvalidResult) =>
                  Left(previousValidationInvalidResult).toFuture

                case _ => validateEuRegistrationDetails(remaining)
              }

            case _ =>
              validateEuRegistrationDetails(remaining)
          }
        }

      case Nil =>
        Right(true).toFuture
    }
  }

  private def checkCurrentEtmpDisplayEuRegistrationDetails(
                                                            etmpDisplayEuRegistrationDetails: EtmpDisplayEuRegistrationDetails,
                                                            vatNumber: Option[String]
                                                          )(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Match]] = {
    vatNumber match {
      case Some(euVrn) =>
        coreRegistrationValidationService.searchEuVrn(euVrn, etmpDisplayEuRegistrationDetails.issuedBy)

      case _ => etmpDisplayEuRegistrationDetails.taxIdentificationNumber match {
        case Some(taxId) =>
          coreRegistrationValidationService.searchEuTaxId(taxId, etmpDisplayEuRegistrationDetails.issuedBy)

        case _ => Future.failed(
          new RuntimeException(s"$etmpDisplayEuRegistrationDetails has neither a VRN or Tax Identification Number")
        )
      }
    }
  }

  def validateOtherIossIntermediaryRegistrationDetails(
                                                        otherIossIntermediaryRegistrations: Seq[EtmpOtherIossIntermediaryRegistrations]
                                                      )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: AuthenticatedDataRequest[_]): Future[Either[PreviousValidationInvalidResult, Boolean]] = {
    otherIossIntermediaryRegistrations.toList match {
      case ::(mostRecentDetails, remaining) =>
        validateOtherIossIntermediaryRegistration(mostRecentDetails, remaining)

      case Nil =>
        Right(true).toFuture
    }
  }

  private def validateOtherIossIntermediaryRegistration(
                                                         mostRecentOtherIossIntermediaryRegistration: EtmpOtherIossIntermediaryRegistrations,
                                                         remaining: Seq[EtmpOtherIossIntermediaryRegistrations]
                                                       )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: AuthenticatedDataRequest[_]): Future[Either[PreviousValidationInvalidResult, Boolean]] = {
    coreRegistrationValidationService.searchScheme(
      mostRecentOtherIossIntermediaryRegistration.intermediaryNumber,
      mostRecentOtherIossIntermediaryRegistration.issuedBy
    ).flatMap {
      case Some(foundMatch) =>
        remapMatchToError(mostRecentOtherIossIntermediaryRegistration.issuedBy, foundMatch) match {
          case Some(previousValidationInvalidResult) =>
            Left(previousValidationInvalidResult).toFuture

          case _ =>
            validateOtherIossIntermediaryRegistrationDetails(remaining)
        }
      case _ =>
        validateOtherIossIntermediaryRegistrationDetails(remaining)
    }
  }

  private def remapMatchToError(countryCode: String, foundMatch: Match): Option[PreviousValidationInvalidResult] = {
    if (foundMatch.isActiveTrader) {
      Some(InvalidActiveTrader(countryCode = countryCode, memberState = foundMatch.memberState))
    }
    else if (foundMatch.isQuarantinedTrader(clock)) {
      Some(InvalidQuarantinedTrader(countryCode = countryCode, effectiveDate = foundMatch.getEffectiveDate))
    } else {
      None
    }
  }
}
