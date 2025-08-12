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

package utils

import logging.Logging
import models.Index
import models.domain.VatCustomerInfo
import models.requests.AuthenticatedDataRequest
import pages.Waypoints
import pages.checkVatDetails.NiAddressPage
import pages.tradingNames.HasTradingNamePage
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import queries.tradingNames.AllTradingNamesQuery
import utils.CheckNiBased.isNiBasedIntermediary
import utils.EuDetailsCompletionChecks.*
import utils.PreviousIntermediaryRegistrationCompletionChecks.*

import scala.concurrent.Future

trait CompletionChecks extends Logging {

  protected def withCompleteDataModel[A](index: Index, data: Index => Option[A], onFailure: Option[A] => Result)
                                        (onSuccess: => Result): Result = {
    val incomplete = data(index)
    if (incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  protected def withCompleteDataAsync[A](data: () => Seq[A], onFailure: Seq[A] => Future[Result])
                                        (onSuccess: => Future[Result]): Future[Result] = {
    val incomplete = data()
    if (incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  def validate(vatCustomerInfo: VatCustomerInfo)(implicit request: AuthenticatedDataRequest[AnyContent]): Boolean = {
    isTradingNamesValid() &&
      isPreviousIntermediaryRegistrationsDefined() &&
      getAllIncompletePreviousIntermediaryRegistrations().isEmpty &&
      isEuDetailsDefined() &&
      getAllIncompleteEuDetails().isEmpty &&
      isVatNiAddressDetailsValid(vatCustomerInfo)
  }

  def getFirstValidationErrorRedirect(
                                       waypoints: Waypoints,
                                       vatCustomerInfo: VatCustomerInfo
                                     )(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = {
    (incompleteTradingNameRedirect(waypoints) ++
      emptyPreviousIntermediaryRegistrationsRedirect(waypoints) ++
      incompletePreviousIntermediaryRegistrationRedirect(waypoints) ++
      emptyEuDetailsDRedirect(waypoints) ++
      incompleteEuDetailsRedirect(waypoints) ++
      incompleteVatNiAddressDetailsRedirect(waypoints, vatCustomerInfo)
      ).headOption
  }

  private def isTradingNamesValid()(implicit request: AuthenticatedDataRequest[AnyContent]): Boolean = {
    request.userAnswers.get(HasTradingNamePage).exists {
      case true => request.userAnswers.get(AllTradingNamesQuery).getOrElse(List.empty).nonEmpty
      case false => request.userAnswers.get(AllTradingNamesQuery).getOrElse(List.empty).isEmpty
    }
  }

  private def incompleteTradingNameRedirect(waypoints: Waypoints)(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = {
    if (!isTradingNamesValid()) {
      Some(Redirect(HasTradingNamePage.route(waypoints).url))
    } else {
      None
    }
  }

  private def incompleteVatNiAddressDetailsRedirect(
                                                     waypoints: Waypoints,
                                                     vatCustomerInfo: VatCustomerInfo
                                                   )(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = {
    if (!isVatNiAddressDetailsValid(vatCustomerInfo)) {
      Some(Redirect(NiAddressPage.route(waypoints).url))
    } else {
      None
    }
  }

  private def isVatNiAddressDetailsValid(
                                          vatCustomerInfo: VatCustomerInfo
                                        )(implicit request: AuthenticatedDataRequest[AnyContent]): Boolean = {
    if (!isNiBasedIntermediary(vatCustomerInfo)) {
      request.userAnswers.get(NiAddressPage).nonEmpty
    } else {
      true
    }
  }
}
