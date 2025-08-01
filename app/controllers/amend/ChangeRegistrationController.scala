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

package controllers.amend

import controllers.actions.*
import models.domain.VatCustomerInfo
import models.{BankDetails, Bic, CheckMode, ContactDetails, DesAddress, Iban, Index, TradingName, UserAnswers}
import pages.{BankDetailsPage, ContactDetailsPage, EmptyWaypoints, Waypoint, Waypoints}
import pages.amend.{AmendCompletePage, ChangeRegistrationPage}
import pages.euDetails.HasFixedEstablishmentPage
import pages.filters.RegisteredForIossIntermediaryInEuPage
import pages.previousIntermediaryRegistrations.HasPreviouslyRegisteredAsIntermediaryPage
import pages.tradingNames.{HasTradingNamePage, TradingNamePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ChangeRegistrationView
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.checkAnswers.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediarySummary, PreviousIntermediaryRegistrationsSummary}
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, ContactDetailsSummary, NiAddressSummary, VatRegistrationDetailsSummary}
import viewmodels.govuk.summarylist.*

import java.time.{Instant, LocalDate}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ChangeRegistrationController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        cc: AuthenticatedControllerComponents,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: ChangeRegistrationView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = cc.authAndGetData(inAmend = true).async {

      implicit request =>

        val thisPage = ChangeRegistrationPage

        val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, ChangeRegistrationPage.urlFragment))

        val iban: Iban = Iban("GB33BUKB202015555555555").toOption.get
        val bic: Bic = Bic("BARCGB22456").get

        val vatCustomerInfo: VatCustomerInfo =
          VatCustomerInfo(
            registrationDate = LocalDate.now(),
            desAddress = DesAddress(
              line1 = "1818 East Tusculum Street",
              line2 = Some("Phil Tow"),
              line3 = None, line4 = None, line5 = None,
              postCode = Some("BT4 2XW"),
              countryCode = "EL"),
            organisationName = Some("Company name"),
            individualName = None,
            singleMarketIndicator = true,
            deregistrationDecisionDate = None
          )

        def basicUserAnswersWithVatInfo: UserAnswers =
          UserAnswers(id = "12345-credId", vatInfo = Some(vatCustomerInfo), lastUpdated = Instant.now())

        def completeUserAnswersWithVatInfo: UserAnswers =
          basicUserAnswersWithVatInfo
            .set(RegisteredForIossIntermediaryInEuPage, false).get
            .set(HasTradingNamePage, true).get
            .set(TradingNamePage(Index(0)), TradingName("Chartoff Winkler and Co. Robert Rocky Balboa Robert Balboa")).get
            .set(HasPreviouslyRegisteredAsIntermediaryPage, false).get
            .set(HasFixedEstablishmentPage, false).get
            .set(ContactDetailsPage, ContactDetails("Rocky Balboa", "028 123 4567", "rocky.balboa@chartoffwinkler.co.uk")).get
            .set(BankDetailsPage, BankDetails("Chartoff Winkler and Co.", Some(bic), iban)).get

        val vatRegistrationDetailsList = SummaryListViewModel(
          rows = Seq(
            VatRegistrationDetailsSummary.rowBusinessAddress(completeUserAnswersWithVatInfo)
          ).flatten
        )

        val niAddressSummaryRow = NiAddressSummary.row(waypoints, completeUserAnswersWithVatInfo, thisPage)
        val maybeHasTradingNameSummaryRow = HasTradingNameSummary.row(waypoints, completeUserAnswersWithVatInfo, thisPage)
        val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(waypoints, completeUserAnswersWithVatInfo, thisPage)
        val maybeHasPreviouslyRegisteredAsIntermediaryRow = HasPreviouslyRegisteredAsIntermediarySummary
          .checkAnswersRow(waypoints, completeUserAnswersWithVatInfo, thisPage)
        val previouslyRegisteredAsIntermediaryRow = PreviousIntermediaryRegistrationsSummary.checkAnswersRow(waypoints, completeUserAnswersWithVatInfo, thisPage)
        val maybeHasFixedEstablishmentSummaryRow = HasFixedEstablishmentSummary.row(waypoints,completeUserAnswersWithVatInfo, thisPage)
        val euDetailsSummaryRow = EuDetailsSummary.checkAnswersRow(waypoints, completeUserAnswersWithVatInfo, thisPage)
        val contactDetailsFullNameRow = ContactDetailsSummary.rowContactName(waypoints, completeUserAnswersWithVatInfo, thisPage)
        val contactDetailsTelephoneNumberRow = ContactDetailsSummary.rowTelephoneNumber(waypoints, completeUserAnswersWithVatInfo, thisPage)
        val contactDetailsEmailAddressRow = ContactDetailsSummary.rowEmailAddress(waypoints, completeUserAnswersWithVatInfo, thisPage)
        val bankDetailsAccountNameRow = BankDetailsSummary.rowAccountName(waypoints, completeUserAnswersWithVatInfo, thisPage)
        val bankDetailsBicRow = BankDetailsSummary.rowBIC(waypoints, completeUserAnswersWithVatInfo, thisPage)
        val bankDetailsIbanRow = BankDetailsSummary.rowIBAN(waypoints, completeUserAnswersWithVatInfo, thisPage)

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
            maybeHasFixedEstablishmentSummaryRow.map { hasFixedEstablishmentSummaryRow =>
              if (euDetailsSummaryRow.nonEmpty) {
                hasFixedEstablishmentSummaryRow.withCssClass("govuk-summary-list__row--no-border")
              } else {
                hasFixedEstablishmentSummaryRow
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

        Ok(view(waypoints, vatRegistrationDetailsList, list)).toFuture
  }


  def onSubmit(waypoints: Waypoints): Action[AnyContent] = Action {
    Redirect(AmendCompletePage.route(waypoints).url)
  }

}
