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

import base.SpecBase
import config.FrontendAppConfig
import models.responses.etmp.EtmpEnrolmentResponse
import models.{CompositeAccount, UserAnswers}
import pages.tradingNames.HasNoOtherTradingNamesPage
import pages.{BankDetailsPage, ContactDetailsPage}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.etmp.EtmpEnrolmentResponseQuery
import queries.tradingNames.AllTradingNamesQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.checkAnswers.tradingNames.{HasNoOtherTradingNamesSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, ContactDetailsSummary}
import viewmodels.govuk.all.SummaryListViewModel
import views.html.ApplicationCompleteView

class ApplicationCompleteControllerSpec extends SpecBase {

  private val intermediaryNumber = "IN9001234567"
  private val compositeAccount: CompositeAccount = arbitraryCompositeAccount.arbitrary.sample.value

  private val userAnswers: UserAnswers = basicUserAnswersWithVatInfo
    .set(EtmpEnrolmentResponseQuery, EtmpEnrolmentResponse(intermediaryNumber)).success.value

  "ApplicationComplete Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(
        userAnswers = Some(userAnswers)
      ).build()

      running(application) {

        implicit val msgs: Messages = messages(application)
        val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value

        val view = application.injector.instanceOf[ApplicationCompleteView]

        val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(userAnswers, None))

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          intermediaryNumber = intermediaryNumber,
          organisationName = vatCustomerInfo.organisationName.get,
          yourAccountUrl = config.intermediaryYourAccountUrl,
          feedbackUrl = config.feedbackUrl(request),
          maybeCompositeAccount = None,
          detailsList = summaryList
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when a composite account is present" in {

      val updatedAnswers: UserAnswers = userAnswers
        .set(HasNoOtherTradingNamesPage, compositeAccount.tradingNames.isEmpty).success.value
        .set(AllTradingNamesQuery, compositeAccount.tradingNames.toList).success.value
        .set(ContactDetailsPage, compositeAccount.contactDetails).success.value
        .set(BankDetailsPage, compositeAccount.bankDetails).success.value

      val application = applicationBuilder(
        userAnswers = Some(userAnswers),
        compositeAccount = Some(compositeAccount)
      ).build()

      running(application) {

        implicit val msgs: Messages = messages(application)
        val request = FakeRequest(GET, routes.ApplicationCompleteController.onPageLoad().url)

        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value

        val view = application.injector.instanceOf[ApplicationCompleteView]

        val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(updatedAnswers, Some(compositeAccount)))

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          intermediaryNumber = intermediaryNumber,
          organisationName = vatCustomerInfo.organisationName.get,
          yourAccountUrl = config.intermediaryYourAccountUrl,
          feedbackUrl = config.feedbackUrl(request),
          maybeCompositeAccount = Some(compositeAccount),
          detailsList = summaryList
        )(request, messages(application)).toString
      }
    }
  }

  private def getAmendedRegistrationSummaryList(
                                                 answers: UserAnswers,
                                                 compositeAccount: Option[CompositeAccount]
                                               )(implicit msgs: Messages): Seq[SummaryListRow] = {
    val hasNoOtherTradingNamesSummaryRow = HasNoOtherTradingNamesSummary.amendedRow(answers)
    val tradingNameSummaryRow = TradingNameSummary.amendedRow(answers)
    val removedTradingNameRow = TradingNameSummary.removedRow(getRemovedTradingNames(answers, compositeAccount))
    val businessContactDetailsContactNameSummaryRow = ContactDetailsSummary.amendedRowContactName(answers)
    val businessContactDetailsTelephoneSummaryRow = ContactDetailsSummary.amendedRowTelephoneNumber(answers)
    val businessContactDetailsEmailSummaryRow = ContactDetailsSummary.amendedRowEmailAddress(answers)
    val bankDetailsAccountNameSummaryRow = BankDetailsSummary.amendedRowAccountName(answers)
    val bankDetailsBicSummaryRow = BankDetailsSummary.amendedRowBIC(answers)
    val bankDetailsIbanSummaryRow = BankDetailsSummary.amendedRowIBAN(answers)

    Seq(
      hasNoOtherTradingNamesSummaryRow,
      tradingNameSummaryRow,
      removedTradingNameRow,
      businessContactDetailsContactNameSummaryRow,
      businessContactDetailsTelephoneSummaryRow,
      businessContactDetailsEmailSummaryRow,
      bankDetailsAccountNameSummaryRow,
      bankDetailsBicSummaryRow,
      bankDetailsIbanSummaryRow
    ).flatten
  }

  private def getRemovedTradingNames(answers: UserAnswers, compositeAccount: Option[CompositeAccount]): Seq[String] = {

    val amendedAnswers = answers.get(AllTradingNamesQuery).map(_.map(_.name)).getOrElse(List.empty)
    val originalAnswers = compositeAccount.map(_.tradingNames.map(_.name)).getOrElse(List.empty)

    originalAnswers.diff(amendedAnswers)
  }
}
