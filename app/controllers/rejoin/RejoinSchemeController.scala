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

package controllers.rejoin

import config.Constants.niPostCodeAreaPrefix
import controllers.actions.*
import controllers.rejoin.validation.RejoinRegistrationValidation
import logging.Logging
import models.audit.RegistrationAuditType.AmendRegistration
import models.audit.{IntermediaryAmendRegistrationAuditModel, SubmissionResult}
import models.etmp.display.EtmpDisplayRegistration
import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetails
import models.requests.{AuthenticatedDataRequest, AuthenticatedMandatoryIntermediaryRequest}
import models.{CheckMode, Country}
import pages.rejoin.{CannotRejoinPage, RejoinSchemePage}
import pages.{EmptyWaypoints, Waypoint, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.rejoin.NewIossReferenceQuery
import services.{AuditService, RegistrationService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.checkAnswers.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediarySummary, PreviousIntermediaryRegistrationsSummary}
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, ContactDetailsSummary, NiAddressSummary, VatRegistrationDetailsSummary}
import viewmodels.govuk.summarylist.*
import views.html.rejoin.RejoinSchemeView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class RejoinSchemeController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        cc: AuthenticatedControllerComponents,
                                        view: RejoinSchemeView,
                                        auditService: AuditService,
                                        registrationService: RegistrationService,
                                        rejoinRegistrationValidation: RejoinRegistrationValidation,
                                        clock: Clock
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(): Action[AnyContent] = cc.authAndRequireIntermediaryAndVerifyEmail(inAmend = false, inRejoin = true).async {
    implicit request => 

      val thisPage = RejoinSchemePage

      val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, RejoinSchemePage.urlFragment))

      checkExistingRegistrationsValidation(waypoints, request.registrationWrapper.etmpDisplayRegistration) {

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

        val niAddressSummaryRow = NiAddressSummary.row(waypoints, request.userAnswers, checkOtherAddressNi = false, thisPage)
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

        val iossDetailsList = SummaryListViewModel(
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

        Ok(view(waypoints, vatRegistrationDetailsList, iossDetailsList)).toFuture
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireIntermediaryAndCheckNiAddress(inAmend = false, inRejoin = true).async {
    implicit request =>

      val canRejoin = request.registrationWrapper.etmpDisplayRegistration.canRejoinScheme(LocalDate.now(clock))

      if (canRejoin) {

        checkExistingRegistrationsValidation(waypoints, request.registrationWrapper.etmpDisplayRegistration) {

          val userAnswers = request.userAnswers

          registrationService.amendRegistration(
            answers = userAnswers,
            registration = request.registrationWrapper.etmpDisplayRegistration,
            vrn = request.vrn,
            iossNumber = request.intermediaryNumber,
            rejoin = true
          ).flatMap {
            case Right(amendRegistrationResponse) =>
              userAnswers.set(NewIossReferenceQuery, amendRegistrationResponse.intReference) match {
                case Failure(throwable) =>
                  logger.error(s"Unexpected result on updating answers with new IOSS Reference: ${throwable.getMessage}", throwable)
                  Redirect(routes.ErrorSubmittingRejoinController.onPageLoad()).toFuture

                case Success(updatedUserAnswer) =>
                  cc.sessionRepository.set(updatedUserAnswer).map { _ =>
                    auditService.audit(
                      IntermediaryAmendRegistrationAuditModel.build(
                        registrationAuditType = AmendRegistration,
                        userAnswers = updatedUserAnswer,
                        amendRegistrationResponse = Some(amendRegistrationResponse),
                        submissionResult = SubmissionResult.Success
                      )
                    )
                    Redirect(RejoinSchemePage.navigate(EmptyWaypoints, userAnswers, userAnswers).route)
                  }
              }

            case Left(error) =>
              logger.error(s"Unexpected result on submit: ${error.body}")
              auditService.audit(
                IntermediaryAmendRegistrationAuditModel.build(
                  registrationAuditType = AmendRegistration,
                  userAnswers = request.userAnswers,
                  amendRegistrationResponse = None,
                  submissionResult = SubmissionResult.Failure
                )
              )
              Redirect(controllers.rejoin.routes.ErrorSubmittingRejoinController.onPageLoad().url).toFuture
          }
        }
      }
      else {
        Redirect(CannotRejoinPage.route(EmptyWaypoints).url).toFuture
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

  private def checkExistingRegistrationsValidation(
                                                    waypoints: Waypoints,
                                                    etmpDisplayRegistration: EtmpDisplayRegistration
                                                  )(successCall: => Future[Result])(
                                                    implicit hc: HeaderCarrier,
                                                    ec: ExecutionContext,
                                                    request: AuthenticatedMandatoryIntermediaryRequest[AnyContent]
                                                  ): Future[Result] = {

    implicit val authenticatedDataRequest: AuthenticatedDataRequest[AnyContent] = request.request

    rejoinRegistrationValidation.validateEuRegistrations(waypoints, etmpDisplayRegistration).flatMap {
      case Left(validationFailureRedirect) =>
        Redirect(validationFailureRedirect).toFuture

      case _ => successCall
    }
  }
}
