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

package controllers

import config.FrontendAppConfig
import controllers.actions.*
import models.requests.AuthenticatedDataRequest
import models.{CompositeAccount, TradingName, UserAnswers}
import pages.{BankDetailsPage, ContactDetailsPage, EmptyWaypoints, JourneyRecoveryPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.etmp.EtmpEnrolmentResponseQuery
import queries.tradingNames.AllTradingNamesQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.tradingNames.{HasNoOtherTradingNamesSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, ContactDetailsSummary}
import viewmodels.govuk.all.SummaryListViewModel
import views.html.ApplicationCompleteView

import javax.inject.Inject
import scala.util.{Failure, Success}

class ApplicationCompleteController @Inject()(
                                               override val messagesApi: MessagesApi,
                                               cc: AuthenticatedControllerComponents,
                                               view: ApplicationCompleteView,
                                               frontendAppConfig: FrontendAppConfig
                                             ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndGetDataWithoutRegistrationCheck() {
    implicit request =>

      (for {
        etmpEnrolmentResponse <- request.userAnswers.get(EtmpEnrolmentResponseQuery)
        organisationName <- getOrganisationName(request.userAnswers)
      } yield {

        val intermediaryNumber = etmpEnrolmentResponse.intRef

        Ok(view(
          intermediaryNumber = intermediaryNumber,
          organisationName = organisationName,
          yourAccountUrl = frontendAppConfig.intermediaryYourAccountUrl,
          feedbackUrl = frontendAppConfig.feedbackUrl,
          maybeCompositeAccount = request.compositeAccount,
          detailsList = detailsList(request.compositeAccount)
        ))
      }).getOrElse(Redirect(JourneyRecoveryPage.route(EmptyWaypoints)))
  }

  private def getOrganisationName(answers: UserAnswers): Option[String] = {
    answers.vatInfo match {
      case Some(vatInfo) if vatInfo.organisationName.isDefined => vatInfo.organisationName
      case Some(vatInfo) if vatInfo.individualName.isDefined => vatInfo.individualName
      case _ => None
    }
  }

  private def detailsList(
                           maybeCompositeAccount: Option[CompositeAccount]
                         )(implicit request: AuthenticatedDataRequest[AnyContent]): SummaryList = {
    maybeCompositeAccount match {
      case Some(compositeAccount) =>
        SummaryListViewModel(
          rows = (
            getHasNoOtherTradingNamesRows(compositeAccount) ++
              getTradingNameRows(compositeAccount) ++
              getContactDetailsRows(compositeAccount) ++
              getBankDetailsRows(compositeAccount)
            ).flatten
        )
      case None =>
        SummaryListViewModel(rows = Seq.empty)
    }
  }

  private def getHasNoOtherTradingNamesRows(compositeAccount: CompositeAccount)
                                           (implicit request: AuthenticatedDataRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = compositeAccount.tradingNames
    val amendedAnswers = request.userAnswers.get(AllTradingNamesQuery).getOrElse(List.empty)

    if (originalAnswers != amendedAnswers) {
      Seq(HasNoOtherTradingNamesSummary.amendedRow(request.userAnswers))
    } else {
      Seq.empty
    }
  }

  private def getTradingNameRows(compositeAccount: CompositeAccount)
                                (implicit request: AuthenticatedDataRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = compositeAccount.tradingNames.map(_.name)
    val amendedAnswers = request.userAnswers.get(AllTradingNamesQuery).map(_.map(_.name)).getOrElse(List.empty)

    val addedTradingName = amendedAnswers.diff(originalAnswers)
    val removedTradingNames = originalAnswers.diff(amendedAnswers)

    val changedTradingName: List[TradingName] = amendedAnswers.zip(originalAnswers).collect {
      case (amended, original) if amended != original => TradingName(amended)
    } ++ amendedAnswers.drop(originalAnswers.size).map(tradingName => TradingName(tradingName))

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

    val removedTradingNameRow = Some(TradingNameSummary.removedRow(removedTradingNames))
    
    Seq(addedTradingNameRow, removedTradingNameRow).flatten
  }

  private def getContactDetailsRows(compositeAccount: CompositeAccount)
                                   (implicit request: AuthenticatedDataRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val originalContactName = compositeAccount.contactDetails.fullName
    val originalTelephone = compositeAccount.contactDetails.telephoneNumber
    val originalEmail = compositeAccount.contactDetails.emailAddress
    val amendedUA = request.userAnswers.get(ContactDetailsPage)

    Seq(
      if (!amendedUA.map(_.fullName).contains(originalContactName)) {
        ContactDetailsSummary.amendedRowContactName(request.userAnswers)
      } else {
        None
      },

      if (!amendedUA.map(_.telephoneNumber).contains(originalTelephone)) {
        ContactDetailsSummary.amendedRowTelephoneNumber(request.userAnswers)
      } else {
        None
      },

      if (!amendedUA.map(_.emailAddress).contains(originalEmail)) {
        ContactDetailsSummary.amendedRowEmailAddress(request.userAnswers)
      } else {
        None
      }
    )
  }

  private def getBankDetailsRows(compositeAccount: CompositeAccount)
                                (implicit request: AuthenticatedDataRequest[AnyContent]): Seq[Option[SummaryListRow]] = {

    val originalAnswers = compositeAccount.bankDetails
    val amendedUA = request.userAnswers.get(BankDetailsPage)

    Seq(
      if (!amendedUA.map(_.accountName).contains(originalAnswers.accountName)) {
        BankDetailsSummary.amendedRowAccountName(request.userAnswers)
      } else {
        None
      },

      if (!amendedUA.map(_.bic).contains(originalAnswers.bic)) {
        BankDetailsSummary.amendedRowBIC(request.userAnswers)
      } else {
        None
      },

      if (!amendedUA.map(_.iban).contains(originalAnswers.iban)) {
        BankDetailsSummary.amendedRowIBAN(request.userAnswers)
      } else {
        None
      }
    )
  }
}
