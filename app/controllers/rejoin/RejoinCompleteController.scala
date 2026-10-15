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

package controllers.rejoin

import config.FrontendAppConfig
import controllers.actions.AuthenticatedControllerComponents
import models.etmp.display.RegistrationWrapper
import models.requests.AuthenticatedMandatoryIntermediaryRequest
import models.{TradingName, UserAnswers}
import pages.{BankDetailsPage, ContactDetailsPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.rejoin.NewIossReferenceQuery
import queries.tradingNames.AllTradingNamesQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.tradingNames.{HasNoOtherTradingNamesSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, ContactDetailsSummary}
import viewmodels.govuk.all.SummaryListViewModel
import views.html.rejoin.RejoinCompleteView

import javax.inject.Inject
import scala.util.{Failure, Success}

class RejoinCompleteController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          cc: AuthenticatedControllerComponents,
                                          frontendAppConfig: FrontendAppConfig,
                                          view: RejoinCompleteView
                                        ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(): Action[AnyContent] = cc.authAndRequireIntermediaryAndCheckNiAddress(inAmend = false, inRejoin = true).async {
    implicit request =>
      val newIossReference = getNewIossReference(request.userAnswers)

      Ok(view(
        newIossReference,
        frontendAppConfig.intermediaryYourAccountUrl,
        request.compositeAccount,
        request.numberOfIossRegistrations,
        detailsList(request.registrationWrapper)
      )).toFuture
  }

  private def getNewIossReference(answers: UserAnswers) = {
    answers.get(NewIossReferenceQuery).getOrElse(throw new RuntimeException("NewIossReference has not been set in answers"))
  }

  private def detailsList(
                           originalRegistration: RegistrationWrapper
                         )(implicit request: AuthenticatedMandatoryIntermediaryRequest[AnyContent]) = {
    SummaryListViewModel(
      rows = (
        getHasNoOtherTradingNamesRows(originalRegistration) ++
          getTradingNameRows(originalRegistration) ++
          getBusinessContactDetailsRows(originalRegistration) ++
          getBankDetailsRows(originalRegistration)
        ).flatten
    )
  }

  private def getHasNoOtherTradingNamesRows(originalRegistration: RegistrationWrapper)
                                           (implicit request: AuthenticatedMandatoryIntermediaryRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = originalRegistration.etmpDisplayRegistration.tradingNames.map(tn => TradingName(tn.tradingName)).toList
    val amendedAnswers = request.userAnswers.get(AllTradingNamesQuery).getOrElse(List.empty)

    if (originalAnswers != amendedAnswers) {
      Seq(HasNoOtherTradingNamesSummary.amendedRow(request.userAnswers))
    } else {
      Seq.empty
    }
  }

  private def getTradingNameRows(originalRegistration: RegistrationWrapper)
                                (implicit request: AuthenticatedMandatoryIntermediaryRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = originalRegistration.etmpDisplayRegistration.tradingNames.map(tradingName => TradingName(tradingName.tradingName)).toList
    val amendedAnswers = request.userAnswers.get(AllTradingNamesQuery).getOrElse(List.empty)
    val addedTradingName = amendedAnswers.diff(originalAnswers)
    val removedTradingNames = originalAnswers.diff(amendedAnswers)

    val changedTradingName: List[TradingName] = amendedAnswers.zip(originalAnswers).collect {
      case (amended, original) if amended != original => amended
    } ++ amendedAnswers.drop(originalAnswers.size)


    val addedTradingNameRow = if (addedTradingName.nonEmpty) {
      request.userAnswers.set(AllTradingNamesQuery, changedTradingName) match {
        case Success(amendedUserAnswer: UserAnswers) =>
          Some(TradingNameSummary.amendedRow(amendedUserAnswer))
        case Failure(_) =>
          None
      }
    } else {
      None
    }

    val removedTradingNameRow = Some(TradingNameSummary.removedRow(removedTradingNames.map(_.name)))

    Seq(addedTradingNameRow, removedTradingNameRow).flatten
  }

  private def getBusinessContactDetailsRows(originalRegistration: RegistrationWrapper)
                                           (implicit request: AuthenticatedMandatoryIntermediaryRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val originalContactName = Some(originalRegistration.etmpDisplayRegistration.schemeDetails.contactName)
    val originalTelephone = Some(originalRegistration.etmpDisplayRegistration.schemeDetails.businessTelephoneNumber)
    val originalEmail = Some(originalRegistration.etmpDisplayRegistration.schemeDetails.businessEmailId)
    val amendedUA = request.userAnswers.get(ContactDetailsPage)
    val amendedContactName = amendedUA.map(_.fullName)
    val amendedTelephone = amendedUA.map(_.telephoneNumber)
    val amendedEmail = amendedUA.map(_.emailAddress)

    val contactNameChanged = amendedContactName.exists(_ != originalContactName.getOrElse(""))
    val telephoneChanged = amendedTelephone.exists(_ != originalTelephone.getOrElse(""))
    val emailChanged = amendedEmail.exists(_ != originalEmail.getOrElse(""))

    Seq(
      if (contactNameChanged) {
        ContactDetailsSummary.amendedRowContactName(request.userAnswers)
      } else {
        None
      },

      if (telephoneChanged) {
        ContactDetailsSummary.amendedRowTelephoneNumber(request.userAnswers)
      } else {
        None
      },

      if (emailChanged) {
        ContactDetailsSummary.amendedRowEmailAddress(request.userAnswers)
      } else {
        None
      }
    )
  }

  private def getBankDetailsRows(originalRegistration: RegistrationWrapper)
                                (implicit request: AuthenticatedMandatoryIntermediaryRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = originalRegistration.etmpDisplayRegistration.bankDetails
    val amendedUA = request.userAnswers.get(BankDetailsPage)

    val originalAccountName = Some(originalAnswers.accountName)
    val originalBic = Some(originalAnswers.bic).flatten
    val originalIban = Some(originalAnswers.iban)
    val amendedAccountName = amendedUA.map(_.accountName)
    val amendedBic = amendedUA.flatMap(_.bic)
    val amendedIban = amendedUA.map(_.iban)

    val accountNameChanged = amendedAccountName.exists(_ != originalAccountName.getOrElse(""))
    val bicChanged = amendedBic.exists(_ != originalBic.getOrElse(""))
    val ibanChanged = amendedIban.exists(_ != originalIban.getOrElse(""))

    Seq(
      if (accountNameChanged) {
        BankDetailsSummary.amendedRowAccountName(request.userAnswers)
      } else {
        None
      },

      if (bicChanged) {
        Some(BankDetailsSummary.amendedRowBIC(request.userAnswers)).flatten
      } else {
        None
      },

      if (ibanChanged) {
        BankDetailsSummary.amendedRowIBAN(request.userAnswers)
      } else {
        None
      }
    )
  }
}
