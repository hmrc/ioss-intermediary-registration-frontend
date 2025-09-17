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
import logging.Logging
import models.CheckMode
import pages.amend.ChangeRegistrationPage
import pages.{EmptyWaypoints, Waypoint, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.checkAnswers.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediarySummary, PreviousIntermediaryRegistrationsSummary}
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, ContactDetailsSummary, NiAddressSummary, VatRegistrationDetailsSummary}
import viewmodels.govuk.summarylist.*
import views.html.ChangeRegistrationView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ChangeRegistrationController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        cc: AuthenticatedControllerComponents,
                                        registrationService: RegistrationService,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: ChangeRegistrationView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad: Action[AnyContent] = cc.authAndRequireIntermediary(waypoints = EmptyWaypoints, inAmend = true).async {

      implicit request =>

        val thisPage = ChangeRegistrationPage

        val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, ChangeRegistrationPage.urlFragment))

        val vatRegistrationDetailsList = SummaryListViewModel(
          rows = Seq(
            VatRegistrationDetailsSummary.rowBusinessAddress(request.userAnswers)
          ).flatten
        )

        val niAddressSummaryRow = NiAddressSummary.row(waypoints, request.userAnswers, thisPage)
        val maybeHasTradingNameSummaryRow = HasTradingNameSummary.row(waypoints, request.userAnswers, thisPage)
        val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(waypoints, request.userAnswers, thisPage)
        val maybeHasPreviouslyRegisteredAsIntermediaryRow = HasPreviouslyRegisteredAsIntermediarySummary
          .checkAnswersRow(waypoints, request.userAnswers, thisPage)
        val previouslyRegisteredAsIntermediaryRow = PreviousIntermediaryRegistrationsSummary.checkAnswersRow(waypoints, request.userAnswers, thisPage)
        val maybeHasFixedEstablishmentSummaryRow = HasFixedEstablishmentSummary.row(waypoints,request.userAnswers, thisPage)
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

        Ok(view(waypoints, vatRegistrationDetailsList, list, request.intermediaryNumber)).toFuture
  }


  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireIntermediary(waypoints = EmptyWaypoints, inAmend = true).async {
    implicit request =>

      registrationService.amendRegistration(
        answers = request.userAnswers,
        registration = request.registrationWrapper.etmpDisplayRegistration,
        vrn = request.vrn,
        iossNumber = request.intermediaryNumber,
        rejoin = false
      ).map {
        case Right(_) =>
          Redirect(ChangeRegistrationPage.navigate(EmptyWaypoints, request.userAnswers, request.userAnswers).route)
        case Left(error) =>
          val exception = new Exception(error.body)
          logger.error(exception.getMessage, exception)
          throw exception
      }
  }
}

