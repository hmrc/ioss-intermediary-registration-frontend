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

import base.SpecBase
import config.FrontendAppConfig
import models.etmp.display.RegistrationWrapper
import models.{CompositeAccount, TradingName, UserAnswers}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.OriginalRegistrationQuery
import queries.rejoin.NewIossReferenceQuery
import queries.tradingNames.AllTradingNamesQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.checkAnswers.tradingNames.{HasNoOtherTradingNamesSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, ContactDetailsSummary}
import viewmodels.govuk.all.SummaryListViewModel
import views.html.rejoin.RejoinCompleteView

class RejoinCompleteControllerSpec extends SpecBase {

  private val originalRegistration = basicUserAnswersWithVatInfo
    .set(NewIossReferenceQuery, iossNumber).success.value
    .set(OriginalRegistrationQuery(intermediaryNumber), registrationWrapper.etmpDisplayRegistration).success.value

  private val compositeAccount: CompositeAccount = arbitraryCompositeAccount.arbitrary.sample.value

  private lazy val rejoinCompleteRoute: String = routes.RejoinCompleteController.onPageLoad().url

  "RejoinCompleteController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(originalRegistration))
        .build()

      running(application) {

        implicit val msgs: Messages = messages(application)
        val request = FakeRequest(GET, rejoinCompleteRoute)
        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value
        val view = application.injector.instanceOf[RejoinCompleteView]

        val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(originalRegistration, registrationWrapper))

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          iossNumber,
          config.intermediaryYourAccountUrl,
          maybeCompositeAccount = None,
          numberOfIossRegistrations = 0,
          detailsList = summaryList
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when a composite account is present" in {

      val application = applicationBuilder(
        userAnswers = Some(originalRegistration),
        compositeAccount = Some(compositeAccount)
      ).build()

      running(application) {

        implicit val msgs: Messages = messages(application)
        val request = FakeRequest(GET, rejoinCompleteRoute)
        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value
        val view = application.injector.instanceOf[RejoinCompleteView]

        val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(originalRegistration, registrationWrapper))

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          iossNumber,
          config.intermediaryYourAccountUrl,
          maybeCompositeAccount = Some(compositeAccount),
          numberOfIossRegistrations = 0,
          detailsList = summaryList
        )(request, messages(application)).toString
      }
    }

    "when answers have changed" - {

      val tradingNames: List[TradingName] =
        originalRegistration.get(AllTradingNamesQuery).getOrElse(List.empty) :+ TradingName("Additional Trading Name")

      val updatedAnswers: UserAnswers = originalRegistration
        .set(AllTradingNamesQuery, tradingNames).success.value

      "must return OK and the correct view for a GET when a composite account is present and no IOSS Registrations exists" in {

        val application = applicationBuilder(
          userAnswers = Some(updatedAnswers),
          registrationWrapper = Some(registrationWrapper),
          compositeAccount = Some(compositeAccount)
        ).build()

        running(application) {

          implicit val msgs: Messages = messages(application)
          val request = FakeRequest(GET, rejoinCompleteRoute)
          val config = application.injector.instanceOf[FrontendAppConfig]

          val result = route(application, request).value
          val view = application.injector.instanceOf[RejoinCompleteView]

          val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(updatedAnswers, registrationWrapper))

          status(result) `mustBe` OK
          contentAsString(result) `mustBe` view(
            iossNumber,
            config.intermediaryYourAccountUrl,
            maybeCompositeAccount = Some(compositeAccount),
            numberOfIossRegistrations = 0,
            detailsList = summaryList
          )(request, messages(application)).toString
        }
      }

      "must return OK and the correct view for a GET when a composite account is not present and at least one IOSS Registration exists" in {

        val application = applicationBuilder(
          userAnswers = Some(updatedAnswers),
          registrationWrapper = Some(registrationWrapper),
          numberOfIossRegistrations = 1
        ).build()

        running(application) {

          implicit val msgs: Messages = messages(application)
          val request = FakeRequest(GET, rejoinCompleteRoute)
          val config = application.injector.instanceOf[FrontendAppConfig]

          val result = route(application, request).value
          val view = application.injector.instanceOf[RejoinCompleteView]

          val summaryList = SummaryListViewModel(rows = getAmendedRegistrationSummaryList(updatedAnswers, registrationWrapper))

          status(result) `mustBe` OK
          contentAsString(result) `mustBe` view(
            iossNumber,
            config.intermediaryYourAccountUrl,
            maybeCompositeAccount = None,
            numberOfIossRegistrations = 1,
            detailsList = summaryList
          )(request, messages(application)).toString
        }
      }
    }

    "must throw a RuntimeException when newIossReference is missing" in {

      val errorMessage: String = "NewIossReference has not been set in answers"

      val application = applicationBuilder(
        userAnswers = Some(basicUserAnswersWithVatInfo),
        numberOfIossRegistrations = 1
      ).build()

      running(application) {

        val request = FakeRequest(GET, rejoinCompleteRoute)

        val result = route(application, request).value.failed

        whenReady(result) { exp =>
          exp `mustBe` a[RuntimeException]
          exp.getMessage `mustBe` errorMessage
        }
      }
    }
  }

  private def getAmendedRegistrationSummaryList(
                                                 answers: UserAnswers,
                                                 registrationWrapper: RegistrationWrapper
                                               )(implicit msgs: Messages): Seq[SummaryListRow] = {
    val hasNoOtherTradingNamesSummaryRow = HasNoOtherTradingNamesSummary.amendedRow(answers)
    val tradingNameSummaryRow = TradingNameSummary.amendedRow(answers)
    val removedTradingNameRow = TradingNameSummary.removedRow(getRemovedTradingNames(answers, registrationWrapper))
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

  private def getRemovedTradingNames(answers: UserAnswers, registrationWrapper: RegistrationWrapper): Seq[String] = {

    val amendedAnswers = answers.get(AllTradingNamesQuery).getOrElse(List.empty)
    val originalAnswers = registrationWrapper.etmpDisplayRegistration.tradingNames.map(tn => TradingName(tn.tradingName)).toList
    
    originalAnswers.diff(amendedAnswers).map(_.name)
  }
}
