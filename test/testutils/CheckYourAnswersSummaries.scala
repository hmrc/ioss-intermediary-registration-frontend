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

package testutils

import models.UserAnswers
import models.requests.AuthenticatedDataRequest
import pages.{CheckAnswersPage, Waypoints}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.checkAnswers.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediarySummary, PreviousIntermediaryRegistrationsSummary}
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, ContactDetailsSummary, NiAddressSummary, VatRegistrationDetailsSummary}
import viewmodels.govuk.SummaryListFluency

object CheckYourAnswersSummaries extends SummaryListFluency {

  def getCYAVatDetailsSummaryList(answers: UserAnswers)
                                 (implicit msgs: Messages, request: AuthenticatedDataRequest[AnyContent]): Seq[SummaryListRow] = {

    Seq(
      VatRegistrationDetailsSummary.rowBasedInUk(answers),
      VatRegistrationDetailsSummary.rowBusinessName(answers),
      VatRegistrationDetailsSummary.rowVatNumber(answers),
      VatRegistrationDetailsSummary.rowBusinessAddress(answers)
    ).flatten
  }

  def getCYANonNiVatDetailsSummaryList(answers: UserAnswers)
                                      (implicit msgs: Messages, request: AuthenticatedDataRequest[AnyContent]): Seq[SummaryListRow] = {

    Seq(
      VatRegistrationDetailsSummary.rowBasedInUk(answers),
      VatRegistrationDetailsSummary.rowBusinessName(answers),
      VatRegistrationDetailsSummary.rowVatNumber(answers)
    ).flatten
  }


  def getCYASummaryList(waypoints: Waypoints, answers: UserAnswers, sourcePage: CheckAnswersPage)
                       (implicit msgs: Messages): Seq[SummaryListRow] = {

    val niAddressSummaryRow: Option[SummaryListRow] = NiAddressSummary.row(waypoints, answers, sourcePage)
    val hasTradingNameSummaryRow: Option[SummaryListRow] = HasTradingNameSummary.row(waypoints, answers, sourcePage)
    val tradingNameSummaryRow: Option[SummaryListRow] = TradingNameSummary.checkAnswersRow(waypoints, answers, sourcePage)
    val hasPreviouslyRegisteredAsIntermediarySummaryRow: Option[SummaryListRow] =
      HasPreviouslyRegisteredAsIntermediarySummary.checkAnswersRow(waypoints, answers, sourcePage)
    val previousIntermediaryRegistrationSummaryRow: Option[SummaryListRow] =
      PreviousIntermediaryRegistrationsSummary.checkAnswersRow(waypoints, answers, sourcePage)
    val hasFixedEstablishmentSummaryRow = HasFixedEstablishmentSummary.row(waypoints, answers, sourcePage)
    val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(waypoints, answers, sourcePage)
    val contactDetailsContactNameSummaryRow = ContactDetailsSummary.rowContactName(waypoints, answers, sourcePage)
    val contactDetailsTelephoneSummaryRow = ContactDetailsSummary.rowTelephoneNumber(waypoints, answers, sourcePage)
    val contactDetailsEmailSummaryRow = ContactDetailsSummary.rowEmailAddress(waypoints, answers, sourcePage)
    val bankDetailsAccountNameSummaryRow = BankDetailsSummary.rowAccountName(waypoints, answers, sourcePage)
    val bankDetailsBicSummaryRow = BankDetailsSummary.rowBIC(waypoints, answers, sourcePage)
    val bankDetailsIbanSummaryRow = BankDetailsSummary.rowIBAN(waypoints, answers, sourcePage)

    Seq(
      niAddressSummaryRow,
      hasTradingNameSummaryRow.map { sr =>
        if (tradingNameSummaryRow.isDefined) {
          sr.withCssClass("govuk-summary-list__row--no-border")
        } else {
          sr
        }
      },
      tradingNameSummaryRow,
      hasPreviouslyRegisteredAsIntermediarySummaryRow.map { sr =>
        if (previousIntermediaryRegistrationSummaryRow.isDefined) {
          sr.withCssClass("govuk-summary-list__row--no-border")
        } else {
          sr
        }
      },
      previousIntermediaryRegistrationSummaryRow,
      hasFixedEstablishmentSummaryRow.map { sr =>
        if (euDetailsSummaryRow.isDefined) {
          sr.withCssClass("govuk-summary-list__row--no-border")
        } else {
          sr
        }
      },
      euDetailsSummaryRow,
      contactDetailsContactNameSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
      contactDetailsTelephoneSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
      contactDetailsEmailSummaryRow,
      bankDetailsAccountNameSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
      bankDetailsBicSummaryRow.map(_.withCssClass("govuk-summary-list__row--no-border")),
      bankDetailsIbanSummaryRow
    ).flatten
  }
}
