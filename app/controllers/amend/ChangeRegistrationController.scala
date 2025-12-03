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

import config.Constants.niPostCodeAreaPrefix
import controllers.actions.*
import logging.Logging
import models.audit.IntermediaryAmendRegistrationAuditModel
import models.audit.RegistrationAuditType.AmendRegistration
import models.audit.SubmissionResult.{Failure, Success}
import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetails
import models.requests.{AuthenticatedDataRequest, AuthenticatedMandatoryIntermediaryRequest}
import models.{CheckMode, Country}
import pages.amend.{ChangePreviousRegistrationPage, ChangeRegistrationPage}
import pages.{CheckAnswersPage, EmptyWaypoints, NonEmptyWaypoints, Waypoint, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.amend.PreviousRegistrationIntermediaryNumberQuery
import services.{AuditService, RegistrationService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.checkAnswers.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediarySummary, PreviousIntermediaryRegistrationsSummary}
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, ContactDetailsSummary, NiAddressSummary, VatRegistrationDetailsSummary}
import viewmodels.govuk.summarylist.*
import views.html.ChangeRegistrationView
import utils.AmendWaypoints.AmendWaypointsOps

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ChangeRegistrationController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              cc: AuthenticatedControllerComponents,
                                              auditService: AuditService,
                                              registrationService: RegistrationService,
                                              val controllerComponents: MessagesControllerComponents,
                                              view: ChangeRegistrationView
                                            )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with CompletionChecks with Logging {

  def onPageLoad(isPreviousRegistration: Boolean): Action[AnyContent] = cc.authAndRequireIntermediaryAndCheckNiAddress(waypoints = EmptyWaypoints, inAmend = true, isPreviousRegistration = isPreviousRegistration).async {
    implicit request: AuthenticatedMandatoryIntermediaryRequest[AnyContent] =>

      val selectedPreviousRegistration: Option[String] = request.userAnswers.get(PreviousRegistrationIntermediaryNumberQuery)

      val intermediaryNumber: String = selectedPreviousRegistration.getOrElse(request.intermediaryNumber)

      val isCurrentIntermediaryAccount: Boolean = request.intermediaryNumber == intermediaryNumber

      val thisPage: CheckAnswersPage = if (isPreviousRegistration) {
        ChangePreviousRegistrationPage
      } else {
        ChangeRegistrationPage
      }

      val waypoints: NonEmptyWaypoints =
        if (isPreviousRegistration) {
          EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, ChangePreviousRegistrationPage.urlFragment))
        } else {
          EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, ChangeRegistrationPage.urlFragment))
        }

      val hasPreviousRegistrations: Boolean = request.hasMultipleIntermediaryEnrolments
      val moreThanOnePreviousReg: Boolean = request.hasMoreThanOnePreviousEnrolment

      val vatRegistrationDetailsList: SummaryList =
        SummaryListViewModel(
          rows = determineVatRegistrationDetailsList()(request.request)
        )

      val existingPreviousRegistrations: Seq[PreviousIntermediaryRegistrationDetails] =
        request.registrationWrapper.etmpDisplayRegistration.intermediaryDetails.map(_.otherIossIntermediaryRegistrations.map { etmp =>
          PreviousIntermediaryRegistrationDetails(
            previousEuCountry = Country.fromCountryCodeUnsafe(etmp.issuedBy),
            previousIntermediaryNumber = etmp.intermediaryNumber,
            nonCompliantDetails = None
          )
        }).getOrElse(Seq.empty)

      val niAddressSummaryRow = NiAddressSummary.row(waypoints, request.userAnswers, thisPage)
      val maybeHasTradingNameSummaryRow = HasTradingNameSummary.row(waypoints, request.userAnswers, thisPage)
      val tradingNameSummaryRow = TradingNameSummary.checkAnswersRow(waypoints, request.userAnswers, thisPage)
      val maybeHasPreviouslyRegisteredAsIntermediaryRow = HasPreviouslyRegisteredAsIntermediarySummary
        .checkAnswersRow(waypoints, request.userAnswers, thisPage)
      val previouslyRegisteredAsIntermediaryRow = PreviousIntermediaryRegistrationsSummary.checkAnswersRow(waypoints, request.userAnswers, thisPage, existingPreviousRegistrations)
      val maybeHasFixedEstablishmentSummaryRow = HasFixedEstablishmentSummary.row(waypoints, request.userAnswers, thisPage)
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

      val unusableStatus = request.registrationWrapper.etmpDisplayRegistration.schemeDetails.unusableStatus

      request.userAnswers.vatInfo match {
        case Some(vatInfo) =>
          val isValid: Boolean = validate(vatInfo)(request.request)
          Ok(view(waypoints, vatRegistrationDetailsList, list, intermediaryNumber, hasPreviousRegistrations, isCurrentIntermediaryAccount, isValid, moreThanOnePreviousReg, unusableStatus)).toFuture
        case None =>
          logger.warn("Missing VAT information, redirecting to start of amend journey")
          Redirect(routes.StartAmendJourneyController.onPageLoad()).toFuture
      }
  }


  def onSubmit(waypoints: Waypoints, incompletePrompt: Boolean, isPreviousRegistration: Boolean): Action[AnyContent] = {
    cc.authAndRequireIntermediaryAndCheckNiAddress(waypoints = EmptyWaypoints, inAmend = true, isPreviousRegistration = isPreviousRegistration).async {
      implicit request =>

        getFirstValidationErrorRedirect(waypoints, request.userAnswers.getVatInfoOrError)(request.request) match {
          case Some(errorRedirect) => if (incompletePrompt) {
            errorRedirect.toFuture
          } else {
            Redirect(routes.ChangeRegistrationController.onPageLoad(waypoints.inPreviousRegistrationAmend)).toFuture
          }

          case None =>

            registrationService.amendRegistration(
              answers = request.userAnswers,
              registration = request.registrationWrapper.etmpDisplayRegistration,
              vrn = request.vrn,
              iossNumber = request.intermediaryNumber,
              rejoin = false
            ).map {
              case Right(response) =>
                auditService.audit(
                  IntermediaryAmendRegistrationAuditModel.build(
                    registrationAuditType = AmendRegistration,
                    userAnswers = request.userAnswers,
                    amendRegistrationResponse = Some(response),
                    submissionResult = Success
                  )
                )

                Redirect(ChangeRegistrationPage.navigate(EmptyWaypoints, request.userAnswers, request.userAnswers).route)
              case Left(error) =>
                val exception = new Exception(error.body)
                logger.error(exception.getMessage, exception)
                auditService.audit(
                  IntermediaryAmendRegistrationAuditModel.build(
                    registrationAuditType = AmendRegistration,
                    userAnswers = request.userAnswers,
                    amendRegistrationResponse = None,
                    submissionResult = Failure
                  )
                )
                throw exception
            }
        }
        }
    }

  private def determineVatRegistrationDetailsList()(implicit request: AuthenticatedDataRequest[AnyContent]): Seq[SummaryListRow] = {

    val rows = Seq(
      VatRegistrationDetailsSummary.rowBasedInUk(request.userAnswers),
      VatRegistrationDetailsSummary.rowBusinessName(request.userAnswers),
      VatRegistrationDetailsSummary.rowVatNumber()
    ).flatten

    val isNiBasedIntermediary = request.userAnswers.vatInfo
      .flatMap(_.desAddress.postCode)
      .exists(_.toUpperCase.startsWith(niPostCodeAreaPrefix))
    if (!isNiBasedIntermediary) {
      rows
    } else {
      rows ++ VatRegistrationDetailsSummary.rowBusinessAddress(request.userAnswers)
    }
  }
}

