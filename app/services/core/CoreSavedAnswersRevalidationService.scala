/*
 * Copyright 2026 HM Revenue & Customs
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

import controllers.saveAndComeBack.routes
import logging.Logging
import models.core.Match
import models.domain.VatCustomerInfo
import models.euDetails.EuDetails
import models.previousIntermediaryRegistrations.*
import models.requests.AuthenticatedDataRequest
import pages.euDetails.HasFixedEstablishmentPage
import pages.previousIntermediaryRegistrations.HasPreviouslyRegisteredAsIntermediaryPage
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.Reads
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import queries.euDetails.AllEuDetailsQuery
import queries.previousIntermediaryRegistrations.AllPreviousIntermediaryRegistrationsWithOptionalIntermediaryNumberQuery
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CoreSavedAnswersRevalidationService @Inject()(
                                                     coreRegistrationValidationService: CoreRegistrationValidationService,
                                                     clock: Clock
                                                   )(implicit ec: ExecutionContext) extends Logging {

  def checkAndValidateSavedUserAnswers()(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    revalidateUKVrn(request.vrn).flatMap {
      case None =>
        checkEuDetails().flatMap {
          case None =>
            checkPreviousIntermediaryRegistrations()

          case redirectUrl => redirectUrl.toFuture
        }

      case redirectUrl => redirectUrl.toFuture
    }
  }

  private def checkEuDetails()(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    request.userAnswers.get(HasFixedEstablishmentPage) match {
      case Some(true) =>
        val euDetails: List[EuDetails] = request.userAnswers.get(AllEuDetailsQuery).getOrElse(List.empty)
        checkAllEuDetails(euDetails)

      case _ =>
        None.toFuture
    }
  }

  private def checkPreviousIntermediaryRegistrations()(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    request.userAnswers.get(HasPreviouslyRegisteredAsIntermediaryPage) match {
      case Some(true) =>
        val previousIntermediaryRegistrations =
          request.userAnswers.get(AllPreviousIntermediaryRegistrationsWithOptionalIntermediaryNumberQuery).getOrElse(List.empty)
        revalidatePreviousIntermediaryRegistrations(previousIntermediaryRegistrations)

      case _ =>
        None.toFuture
    }
  }

  private def revalidateUKVrn(ukVrn: Vrn)(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    if (checkVrnExpired(request.userAnswers.vatInfo)) {
      Some(Redirect(routes.SavedProgressExpiredVrnDateController.onPageLoad().url)).toFuture
    } else {
      coreRegistrationValidationService.searchUkVrn(ukVrn).flatMap { maybeActiveMatch =>
        activeMatchRedirectUrl(maybeActiveMatch)
      }
    }
  }

  private def checkAllEuDetails(allEuDetails: List[EuDetails])(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    allEuDetails match {
      case ::(currentEuDetails, remaining) =>
        revalidateEuDetails(currentEuDetails, currentEuDetails.euVatNumber).flatMap {
          case Some(urlString) => Some(urlString).toFuture
          case _ => checkAllEuDetails(remaining)
        }

      case Nil => None.toFuture
    }
  }

  private def revalidateEuDetails(
                                   euDetails: EuDetails,
                                   euVatNumber: Option[String]
                                 )(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    euVatNumber match {
      case Some(euVrn) =>
        revalidateEuVrn(euVrn, euDetails.euCountry.code)

      case _ => euDetails.euTaxReference match {
        case Some(euTaxReference) =>
          revalidateEuTaxId(euTaxReference, euDetails.euCountry.code)

        case _ =>
          None.toFuture
      }
    }
  }

  private def revalidatePreviousIntermediaryRegistrations(
                                                           allPreviousIntermediaryRegistrations: List[PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber],
                                                         )(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    allPreviousIntermediaryRegistrations match {
      case ::(PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber(
        previousEuCountry,
        Some(intermediaryNumber)
      ), remaining) =>
        coreRegistrationValidationService.searchScheme(
          traderID = intermediaryNumber,
          countryCode = previousEuCountry.code
        ).flatMap { maybeActiveMatch =>
          activeMatchRedirectUrl(maybeActiveMatch).flatMap {
            case Some(urlString) =>
              Some(urlString).toFuture

            case _ =>
              revalidatePreviousIntermediaryRegistrations(remaining)
          }
        }

      case ::(_, remaining) =>
        revalidatePreviousIntermediaryRegistrations(remaining)

      case Nil => None.toFuture
    }
  }

  private def revalidateEuTaxId(
                                 euTaxReference: String,
                                 countryCode: String
                               )(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    coreRegistrationValidationService.searchEuTaxId(euTaxReference, countryCode).flatMap { maybeActiveMatch =>
      activeMatchRedirectUrl(maybeActiveMatch)
    }
  }

  private def revalidateEuVrn(
                               euVrn: String,
                               countryCode: String
                             )(implicit hc: HeaderCarrier, request: AuthenticatedDataRequest[_]): Future[Option[Result]] = {
    coreRegistrationValidationService.searchEuVrn(euVrn, countryCode).flatMap { maybeActiveMatch =>
      activeMatchRedirectUrl(maybeActiveMatch)
    }
  }

  private def activeMatchRedirectUrl(maybeMatch: Option[Match]): Future[Option[Result]] = {
    maybeMatch match {
      case Some(activeMatch) if activeMatch.isActiveTrader =>
        Some(Redirect(routes.SavedProgressClientAlreadyRegisteredController.onPageLoad())).toFuture

      case Some(activeMatch) if activeMatch.isQuarantinedTrader(clock) =>
        Some(Redirect(routes.SavedProgressQuarantinedController.onPageLoad(activeMatch.getEffectiveDate).url)).toFuture

      case _ => None.toFuture
    }
  }

  private def checkVrnExpired(vatCustomerInfo: Option[VatCustomerInfo]): Boolean = {
    vatCustomerInfo match {
      case Some(vatInfo) =>
        vatInfo.deregistrationDecisionDate.exists(!_.isAfter(LocalDate.now(clock)))

      case _ => false
    }
  }
}
