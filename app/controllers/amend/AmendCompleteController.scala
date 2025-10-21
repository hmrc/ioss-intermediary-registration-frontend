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

import config.FrontendAppConfig
import controllers.actions.*
import logging.Logging
import models.etmp.display.{EtmpDisplayEuRegistrationDetails, EtmpDisplayRegistration, EtmpDisplaySchemeDetails}
import models.etmp.{EtmpBankDetails, EtmpIntermediaryDetails, EtmpOtherAddress, EtmpTradingName}
import models.euDetails.EuDetails
import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetails
import models.requests.AuthenticatedMandatoryIntermediaryRequest
import models.{BankDetails, ContactDetails, Country, TradingName, UkAddress, UserAnswers}
import pages.checkVatDetails.NiAddressPage
import pages.{BankDetailsPage, ContactDetailsPage, JourneyRecoveryPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.OriginalRegistrationQuery
import queries.euDetails.AllEuDetailsQuery
import queries.previousIntermediaryRegistrations.AllPreviousIntermediaryRegistrationsQuery
import queries.tradingNames.AllTradingNamesQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.checkAnswers.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediarySummary, PreviousIntermediaryRegistrationsSummary}
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, ContactDetailsSummary, NiAddressSummary}
import viewmodels.govuk.all.SummaryListViewModel
import views.html.amend.AmendCompleteView

import javax.inject.Inject
import scala.util.{Failure, Success}

class AmendCompleteController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         frontendAppConfig: FrontendAppConfig,
                                         view: AmendCompleteView
                                       ) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireIntermediary(waypoints, inAmend = true) {
    implicit request =>

      request.userAnswers.get(OriginalRegistrationQuery(request.intermediaryNumber)).map { originalRegistrationAnswers =>

        val amendedDetailsList: SummaryList = amendList(originalRegistrationAnswers)

        Ok(view(frontendAppConfig.feedbackUrl, frontendAppConfig.intermediaryYourAccountUrl, amendedDetailsList))
      }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints).url))
  }

  private def amendList(originalRegistrationAnswers: EtmpDisplayRegistration)
                       (implicit request: AuthenticatedMandatoryIntermediaryRequest[_]): SummaryList = {
    SummaryListViewModel(
      rows = (
        getHasTradingNameRows(originalRegistrationAnswers.tradingNames) ++
          getTradingNameRows(originalRegistrationAnswers.tradingNames) ++
          getHasPreviousIntermediaryRegistrationRows(originalRegistrationAnswers.intermediaryDetails) ++
          getPreviousIntermediaryRegistrationRows(originalRegistrationAnswers.intermediaryDetails) ++
          getHasFixedEstablishmentInEuDetails(originalRegistrationAnswers.schemeDetails) ++
          getFixedEstablishmentInEuRows(originalRegistrationAnswers.schemeDetails) ++
          getAmendedFixedEstablishmentInEuRows(originalRegistrationAnswers.schemeDetails) ++
          getBusinessContactDetailsRows(originalRegistrationAnswers.schemeDetails) ++
          getBankDetailsRows(originalRegistrationAnswers.bankDetails) ++
          getNiAddressRows(originalRegistrationAnswers.otherAddress)
        ).flatten
    )
  }

  private def getHasTradingNameRows(originalAnswers: Seq[EtmpTradingName])
                                   (implicit request: AuthenticatedMandatoryIntermediaryRequest[_]): Seq[Option[SummaryListRow]] = {

    val userAnswers: Seq[TradingName] = request.userAnswers.get(AllTradingNamesQuery).getOrElse(Seq.empty)

    val hasChangedToNo: Boolean = userAnswers.diff(originalAnswers).nonEmpty
    val hasChangedToYes: Boolean = originalAnswers.diff(userAnswers).nonEmpty
    val notAmended: Boolean = userAnswers.nonEmpty && originalAnswers.nonEmpty || userAnswers.isEmpty && originalAnswers.isEmpty

    if (notAmended) {
      Seq.empty
    } else if (hasChangedToNo || hasChangedToYes) {
      Seq(HasTradingNameSummary.amendedRow(request.userAnswers))
    } else {
      Seq.empty
    }
  }

  private def getTradingNameRows(originalTradingNames: Seq[EtmpTradingName])
                                (implicit request: AuthenticatedMandatoryIntermediaryRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalAnswers: Seq[String] = originalTradingNames.map(_.tradingName)
    val userAnswers: Seq[String] = request.userAnswers.get(AllTradingNamesQuery).map(_.map(_.name)).getOrElse(Seq.empty)
    val addedTradingName: Seq[String] = userAnswers.diff(originalAnswers)
    val removedTradingNames: Seq[String] = originalAnswers.diff(userAnswers)

    val changedTradingName: Seq[TradingName] = userAnswers.zip(originalAnswers).collect {
      case (amended, original) if amended != original => TradingName(amended)
    } ++ userAnswers.drop(originalAnswers.size).map(tradingName => TradingName(tradingName))

    val addedTradingNameRow: Option[Option[SummaryListRow]] = if (addedTradingName.nonEmpty) {
      request.userAnswers.set(AllTradingNamesQuery, changedTradingName.toList) match {
        case Success(amendedUserAnswer) =>
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

  private def getHasPreviousIntermediaryRegistrationRows(originalAnswers: Option[EtmpIntermediaryDetails])
                                                        (implicit request: AuthenticatedMandatoryIntermediaryRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalCountries: Seq[String] = originalAnswers.map(_.otherIossIntermediaryRegistrations.map(_.issuedBy)).getOrElse(Seq.empty)
    val amendedCountries: Seq[String] = request.userAnswers.get(AllPreviousIntermediaryRegistrationsQuery)
      .map(_.map(_.previousEuCountry.code))
      .getOrElse(Seq.empty)

    val hasChangedToNo: Boolean = amendedCountries.diff(originalCountries).nonEmpty
    val hasChangedToYes: Boolean = originalCountries.diff(amendedCountries).nonEmpty
    val notAmended: Boolean = originalCountries.nonEmpty && amendedCountries.nonEmpty || originalCountries.isEmpty && amendedCountries.isEmpty

    if (notAmended) {
      Seq.empty
    } else if (hasChangedToYes || hasChangedToNo) {
      Seq(HasPreviouslyRegisteredAsIntermediarySummary.addedRow(request.userAnswers))
    } else {
      Seq.empty
    }
  }

  private def getPreviousIntermediaryRegistrationRows(originalAnswers: Option[EtmpIntermediaryDetails])
                                                     (implicit request: AuthenticatedMandatoryIntermediaryRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalCountries: Seq[String] = originalAnswers.map(_.otherIossIntermediaryRegistrations).map(_.map(_.issuedBy)).getOrElse(Seq.empty)
    val amendedCountries: Seq[String] = request.userAnswers.get(AllPreviousIntermediaryRegistrationsQuery)
      .map(_.map(_.previousEuCountry.code))
      .getOrElse(Seq.empty)

    val addedPreviousIntermediaryRegistrations: Seq[String] = amendedCountries.diff(originalCountries)

    val newPreviousIntermediaryRegistrations: Seq[String] = amendedCountries.filterNot { amendedCountryCode =>
      originalCountries.contains(amendedCountryCode)
    }

    val addedPreviousIntermediaryRegistrationsRow = if (addedPreviousIntermediaryRegistrations.nonEmpty) {
      val amendedPreviousIntermediaryRegistrations = request.userAnswers.get(AllPreviousIntermediaryRegistrationsQuery)
        .getOrElse(Seq.empty)
        .filter(previousIntermediaryRegistrationDetails => newPreviousIntermediaryRegistrations
          .contains(previousIntermediaryRegistrationDetails.previousEuCountry.code)).toList

      request.userAnswers.set(AllPreviousIntermediaryRegistrationsQuery, amendedPreviousIntermediaryRegistrations) match {
        case Success(amendedAnswers) =>
          Some(PreviousIntermediaryRegistrationsSummary.addedRow(amendedAnswers))

        case Failure(_) => None
      }
    } else {
      None
    }

    Seq(addedPreviousIntermediaryRegistrationsRow).flatten
  }

  private def getHasFixedEstablishmentInEuDetails(originalAnswers: EtmpDisplaySchemeDetails)
                                                 (implicit request: AuthenticatedMandatoryIntermediaryRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalCountries: Seq[String] = originalAnswers.euRegistrationDetails.map(_.issuedBy)
    val amendedCountries: Seq[String] = request.userAnswers.get(AllEuDetailsQuery).map(_.map(_.euCountry.code)).getOrElse(Seq.empty)
    val hasChangedToNo: Boolean = amendedCountries.diff(originalCountries).nonEmpty
    val hasChangedToYes: Boolean = originalCountries.diff(amendedCountries).nonEmpty
    val notAmended: Boolean = originalCountries.nonEmpty && amendedCountries.nonEmpty || originalCountries.isEmpty && amendedCountries.isEmpty

    if (notAmended) {
      Seq.empty
    } else if (hasChangedToYes || hasChangedToNo) {
      Seq(HasFixedEstablishmentSummary.amendedRow(request.userAnswers))
    } else {
      Seq.empty
    }
  }

  private def getFixedEstablishmentInEuRows(originalAnswers: EtmpDisplaySchemeDetails)
                                           (implicit request: AuthenticatedMandatoryIntermediaryRequest[_]): Seq[Option[SummaryListRow]] = {

    val originalCountries: Seq[String] = originalAnswers.euRegistrationDetails.map(_.issuedBy)
    val amendedCountries: Seq[String] = request.userAnswers.get(AllEuDetailsQuery)
      .map(_.map(_.euCountry.code))
      .getOrElse(Seq.empty)

    val addedFixedEstablishmentDetails: Seq[String] = amendedCountries.diff(originalCountries)
    val removedFixedEstablishmentDetails: Seq[String] = originalCountries.diff(amendedCountries)

    val newFixedEstablishmentDetails: Seq[String] = amendedCountries.filterNot { amendedCountryCode =>
      originalCountries.contains(amendedCountryCode)
    }

    val addedFixedEstablishmentRow = if (addedFixedEstablishmentDetails.nonEmpty) {
      val amendedFixedEstablishmentDetails = request.userAnswers.get(AllEuDetailsQuery).getOrElse(Seq.empty)
        .filter(fixedEstablishmentDetails => newFixedEstablishmentDetails
          .contains(fixedEstablishmentDetails.euCountry.code)
        ).toList

      request.userAnswers.set(AllEuDetailsQuery, amendedFixedEstablishmentDetails) match {
        case Success(amendedAnswers) =>
          Some(EuDetailsSummary.addedRow(amendedAnswers))

        case Failure(_) => None
      }
    } else {
      None
    }

    val removedFixedEstablishmentCountries: Seq[Country] = removedFixedEstablishmentDetails.flatMap(Country.fromCountryCode)

    val removedFixedEstablishmentDetailsRow = Some(EuDetailsSummary.removedRow(removedFixedEstablishmentCountries))

    Seq(addedFixedEstablishmentRow, removedFixedEstablishmentDetailsRow).flatten
  }

  private def getAmendedFixedEstablishmentInEuRows(originalRegistration: EtmpDisplaySchemeDetails)
                                                  (implicit request: AuthenticatedMandatoryIntermediaryRequest[_]): Seq[Option[SummaryListRow]] = {

    val allFixedEstablishmentDetails = request.userAnswers.get(AllEuDetailsQuery).getOrElse(List.empty)

    val changedFixedEstablishmentCountries: Seq[Country] = allFixedEstablishmentDetails.flatMap { fixedEstablishmentDetails =>
      originalRegistration.euRegistrationDetails.find(_.issuedBy == fixedEstablishmentDetails.euCountry.code) match {
        case Some(originalFixedEstablishmentDetails)
          if hasFixedEstablishmentDetailsChanged(fixedEstablishmentDetails, originalFixedEstablishmentDetails) =>
          Some(fixedEstablishmentDetails.euCountry)

        case _ =>
          None
      }
    }

    if (changedFixedEstablishmentCountries.nonEmpty) {
      Seq(EuDetailsSummary.amendedRow(changedFixedEstablishmentCountries))
    } else {
      Seq.empty
    }
  }

  private def hasFixedEstablishmentDetailsChanged(amendedDetails: EuDetails, originalDetails: EtmpDisplayEuRegistrationDetails): Boolean = {

    val vatNumberWithoutCountryCode: Option[String] = amendedDetails.euVatNumber.map(_.stripPrefix(amendedDetails.euCountry.code))
    val originalRegistrationVatNumber: Option[String] = originalDetails.vatNumber

    amendedDetails.fixedEstablishmentAddress.map(_.tradingName).exists(_ != originalDetails.fixedEstablishmentTradingName) ||
      amendedDetails.fixedEstablishmentAddress.exists(address =>
        !originalDetails.fixedEstablishmentAddressLine1.equals(address.line1) ||
          !originalDetails.fixedEstablishmentAddressLine2.equals(address.line2) ||
          !originalDetails.townOrCity.equals(address.townOrCity) ||
          !originalDetails.regionOrState.equals(address.stateOrRegion) ||
          !originalDetails.postcode.equals(address.postCode)
      ) ||
      !vatNumberWithoutCountryCode.equals(originalRegistrationVatNumber) ||
      !amendedDetails.euTaxReference.equals(originalDetails.taxIdentificationNumber)
  }

  private def getBusinessContactDetailsRows(originalAnswers: EtmpDisplaySchemeDetails)
                                           (implicit request: AuthenticatedMandatoryIntermediaryRequest[_]): Seq[Option[SummaryListRow]] = {

    val userAnswers: Option[ContactDetails] = request.userAnswers.get(ContactDetailsPage)

    Seq(
      if (!userAnswers.map(_.fullName).contains(originalAnswers.contactName)) {
        ContactDetailsSummary.amendedRowContactName(request.userAnswers)
      } else {
        None
      },

      if (!userAnswers.map(_.telephoneNumber).contains(originalAnswers.businessTelephoneNumber)) {
        ContactDetailsSummary.amendedRowTelephoneNumber(request.userAnswers)
      } else {
        None
      },

      if (!userAnswers.map(_.emailAddress).contains(originalAnswers.businessEmailId)) {
        ContactDetailsSummary.amendedRowEmailAddress(request.userAnswers)
      } else {
        None
      }
    )
  }

  private def getBankDetailsRows(originalAnswers: EtmpBankDetails)
                                (implicit request: AuthenticatedMandatoryIntermediaryRequest[_]): Seq[Option[SummaryListRow]] = {

    val userAnswers: Option[BankDetails] = request.userAnswers.get(BankDetailsPage)

    Seq(
      if (!userAnswers.map(_.accountName).contains(originalAnswers.accountName)) {
        BankDetailsSummary.amendedRowAccountName(request.userAnswers)
      } else {
        None
      },

      if (!userAnswers.map(_.bic).contains(originalAnswers.bic)) {
        BankDetailsSummary.amendedRowBIC(request.userAnswers)
      } else {
        None
      },

      if (!userAnswers.map(_.iban).contains(originalAnswers.iban)) {
        BankDetailsSummary.amendedRowIBAN(request.userAnswers)
      } else {
        None
      }
    )
  }

  private def getNiAddressRows(maybeOriginalAnswers: Option[EtmpOtherAddress])
                              (implicit request: AuthenticatedMandatoryIntermediaryRequest[_]): Seq[Option[SummaryListRow]] = {


    val userAnswers: Option[UkAddress] = request.userAnswers.get(NiAddressPage)
    val otherAddressDetailsChanged: Boolean = maybeOriginalAnswers.exists { answers =>

      userAnswers.exists { ukAddress =>
        ukAddress.line1 != answers.addressLine1 ||
          ukAddress.line2 != answers.addressLine2 ||
          ukAddress.townOrCity != answers.townOrCity ||
          ukAddress.county != answers.regionOrState ||
          ukAddress.postCode != answers.postcode
      }
    }
    if (otherAddressDetailsChanged) {
      Seq(NiAddressSummary.amendedRow(request.userAnswers))
    } else {
      Seq.empty
    }
  }
}
