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

import config.Constants.niPostCodeAreaPrefix
import logging.Logging
import models.Index
import models.domain.VatCustomerInfo
import models.requests.AuthenticatedDataRequest
import pages.{BankDetailsPage, ContactDetailsPage, Waypoints}
import pages.checkVatDetails.NiAddressPage
import pages.tradingNames.{HasTradingNamePage, TradingNamePage}
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
      isVatNiAddressDetailsValid(vatCustomerInfo) &&
      isContactDetailsPopulated() &&
      isBankDetailsPopulated()
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
      incompleteVatNiAddressDetailsRedirect(waypoints, vatCustomerInfo) ++
      emptyContactDetails(waypoints) ++
      emptyBankDetails(waypoints)
      ).headOption
  }

  private def isTradingNamesValid()(implicit request: AuthenticatedDataRequest[AnyContent]): Boolean = {
    request.userAnswers.get(HasTradingNamePage).exists {
      case true => request.userAnswers.get(AllTradingNamesQuery).getOrElse(List.empty).nonEmpty
      case false => request.userAnswers.get(AllTradingNamesQuery).getOrElse(List.empty).isEmpty
    }
  }

  private def incompleteTradingNameRedirect(waypoints: Waypoints)(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = {
    request.userAnswers.get(HasTradingNamePage) match {

      case None => Some(Redirect(HasTradingNamePage.route(waypoints).url))

      case Some(false) =>
        val existingTradingNames = request.userAnswers.get(AllTradingNamesQuery).getOrElse(Seq.empty)
        if (existingTradingNames.nonEmpty) {
          Some(Redirect(HasTradingNamePage.route(waypoints).url))
        } else {
          None
        }

      case Some(true) =>

        if (request.userAnswers.get(TradingNamePage(Index(0))).isEmpty) {
          Some(Redirect(TradingNamePage(Index(0)).route(waypoints).url))
        } else {
          None
        }
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
      request.userAnswers.get(NiAddressPage) match {
        case Some(niAddress) =>
          niAddress.postCode.trim.toUpperCase.startsWith(niPostCodeAreaPrefix)
        case _ =>
          false
      }
    } else {
      true
    }
  }

  private def isContactDetailsPopulated()(implicit request: AuthenticatedDataRequest[AnyContent]): Boolean = {
    request.userAnswers.get(ContactDetailsPage) exists { details =>
      details.fullName.trim.nonEmpty &&
        details.telephoneNumber.trim.nonEmpty &&
        details.emailAddress.trim.nonEmpty
    }
  }

  private def emptyContactDetails(waypoints: Waypoints)(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = {
    if (!isContactDetailsPopulated()) {
      Some(Redirect(controllers.routes.ContactDetailsController.onPageLoad(waypoints)))
    } else {
      None
    }
  }

  private def isBankDetailsPopulated()(implicit request: AuthenticatedDataRequest[AnyContent]): Boolean = {
    request.userAnswers.get(BankDetailsPage).isDefined
  }

  private def emptyBankDetails(waypoints: Waypoints)(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = {
    if (!isBankDetailsPopulated()) {
      Some(Redirect(controllers.routes.BankDetailsController.onPageLoad(waypoints)))
    } else {
      None
    }
  }
}
