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

package controllers

import com.google.inject.Inject
import controllers.actions.*
import logging.Logging
import models.CheckMode
import models.domain.VatCustomerInfo
import models.requests.AuthenticatedDataRequest
import pages.{CheckYourAnswersPage, EmptyWaypoints, Waypoint, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CheckNiBased.isNiBasedIntermediary
import utils.CompletionChecks
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, TaxRegisteredInEuSummary}
import viewmodels.checkAnswers.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediarySummary, PreviousIntermediaryRegistrationsSummary}
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, ContactDetailsSummary, NiAddressSummary, VatRegistrationDetailsSummary}
import viewmodels.govuk.summarylist.*
import views.html.CheckYourAnswersView

import scala.concurrent.ExecutionContext

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            cc: AuthenticatedControllerComponents,
                                            view: CheckYourAnswersView
                                          )(implicit executionContext: ExecutionContext)
  extends FrontendBaseController with I18nSupport with CompletionChecks with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  private val thisPage = CheckYourAnswersPage

  def onPageLoad(): Action[AnyContent] = cc.authAndGetDataAndCheckVerifyEmail() {
    implicit request =>

      request.userAnswers.vatInfo match {
        case Some(vatCustomerInfo: VatCustomerInfo) =>

          val vatRegistrationDetailsList: SummaryList =
            SummaryListViewModel(
              rows = determineVatRegistrationDetailsList(vatCustomerInfo)
            )

          val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, CheckYourAnswersPage.urlFragment))

          val niAddressSummaryRow = NiAddressSummary.row(waypoints, request.userAnswers, thisPage)
          val maybeHasTradingNameSummaryRow = HasTradingNameSummary.row(waypoints, request.userAnswers, thisPage)
          val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(waypoints, request.userAnswers, thisPage)
          val maybeHasPreviouslyRegisteredAsIntermediaryRow = HasPreviouslyRegisteredAsIntermediarySummary
            .checkAnswersRow(waypoints, request.userAnswers, thisPage)
          val previouslyRegisteredAsIntermediaryRow = PreviousIntermediaryRegistrationsSummary.checkAnswersRow(waypoints, request.userAnswers, thisPage)
          val maybeTaxRegisteredInEuSummaryRow = TaxRegisteredInEuSummary.checkAnswersRow(waypoints, request.userAnswers, thisPage)
          val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(waypoints, request.userAnswers, thisPage)
          val contactDetailsFullNameRow = ContactDetailsSummary.rowContactName(waypoints, request.userAnswers, thisPage)
          val contactDetailsTelephoneNumberRow = ContactDetailsSummary.rowTelephoneNumber(waypoints, request.userAnswers, thisPage)
          val contactDetailsEmailAddressRow = ContactDetailsSummary.rowEmailAddress(waypoints, request.userAnswers, thisPage)
          val bankDetailsAccountNameRow = BankDetailsSummary.rowAccountName(waypoints, request.userAnswers, thisPage)
          val bankDetailsBicRow = BankDetailsSummary.rowBIC(waypoints, request.userAnswers, thisPage)
          val bankDetailsIbanRow = BankDetailsSummary.rowIBAN(waypoints, request.userAnswers, thisPage)

          val list = SummaryListViewModel(
            rows = Seq(
              niAddressSummaryRow,
              maybeHasTradingNameSummaryRow.map { hasTradingNameSummaryRow =>
                if (tradingNameSummaryRow.nonEmpty) {
                  hasTradingNameSummaryRow.withCssClass("govuk-summary-list__row--no-border")
                } else {
                  hasTradingNameSummaryRow
                }
              },
              tradingNameSummaryRow,
              maybeHasPreviouslyRegisteredAsIntermediaryRow.map { hasPreviouslyRegisteredAsIntermediaryRow =>
                if (previouslyRegisteredAsIntermediaryRow.nonEmpty) {
                  hasPreviouslyRegisteredAsIntermediaryRow.withCssClass("govuk-summary-list__row--no-border")
                } else {
                  hasPreviouslyRegisteredAsIntermediaryRow
                }
              },
              previouslyRegisteredAsIntermediaryRow,
              maybeTaxRegisteredInEuSummaryRow.map { taxRegisteredInEuSummaryRow =>
                if (euDetailsSummaryRow.nonEmpty) {
                  taxRegisteredInEuSummaryRow.withCssClass("govuk-summary-list__row--no-border")
                } else {
                  taxRegisteredInEuSummaryRow
                }
              },
              euDetailsSummaryRow,
              contactDetailsFullNameRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
              contactDetailsTelephoneNumberRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
              contactDetailsEmailAddressRow,
              bankDetailsAccountNameRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
              bankDetailsBicRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
              bankDetailsIbanRow
            ).flatten
          )

          val isValid: Boolean = validate()

          Ok(view(waypoints, vatRegistrationDetailsList, list, isValid))

        case _ =>
          val message: String = "VAT information missing"
          logger.error(message)
          val exception = IllegalStateException(message)
          throw exception
      }
  }

  def onSubmit(waypoints: Waypoints, incompletePrompt: Boolean): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>

      getFirstValidationErrorRedirect(waypoints) match {
        case Some(errorRedirect) => if (incompletePrompt) {
          errorRedirect.toFuture
        } else {
          Redirect(CheckYourAnswersPage.route(waypoints).url).toFuture
        }

        case None =>

          for {
            _ <- cc.sessionRepository.set(request.userAnswers)
          } yield Redirect(CheckYourAnswersPage.navigate(waypoints, request.userAnswers, request.userAnswers).route)
      }
  }

  private def determineVatRegistrationDetailsList(
                                                   vatCustomerInfo: VatCustomerInfo
                                                 )(implicit request: AuthenticatedDataRequest[AnyContent]): Seq[SummaryListRow] = {
    
    val rows = Seq(
      VatRegistrationDetailsSummary.rowBasedInUk(request.userAnswers),
      VatRegistrationDetailsSummary.rowBusinessName(request.userAnswers),
      VatRegistrationDetailsSummary.rowVatNumber(request.userAnswers)
    ).flatten

    if (!isNiBasedIntermediary(vatCustomerInfo)) {
      rows
    } else {
      rows ++ VatRegistrationDetailsSummary.rowBusinessAddress(request.userAnswers)
    }
  }
}
