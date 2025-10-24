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

import base.SpecBase
import config.FrontendAppConfig
import models.etmp.EtmpOtherAddress
import models.etmp.display.EtmpDisplayRegistration
import models.euDetails.EuDetails
import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetails
import models.{ContactDetails, Country, TradingName, UkAddress, UserAnswers}
import org.scalacheck.Gen
import pages.checkVatDetails.NiAddressPage
import pages.{BankDetailsPage, ContactDetailsPage, JourneyRecoveryPage}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.OriginalRegistrationQuery
import queries.euDetails.AllEuDetailsQuery
import queries.previousIntermediaryRegistrations.AllPreviousIntermediaryRegistrationsQuery
import queries.tradingNames.AllTradingNamesQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.checkAnswers.euDetails.{EuDetailsSummary, HasFixedEstablishmentSummary}
import viewmodels.checkAnswers.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediarySummary, PreviousIntermediaryRegistrationsSummary}
import viewmodels.checkAnswers.tradingNames.{HasTradingNameSummary, TradingNameSummary}
import viewmodels.checkAnswers.{BankDetailsSummary, ContactDetailsSummary, NiAddressSummary}
import viewmodels.govuk.all.SummaryListViewModel
import views.html.amend.AmendCompleteView

class AmendCompleteControllerSpec extends SpecBase {

  private val etmpDisplayRegistration: EtmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value

  private val originalRegistration: UserAnswers = emptyUserAnswersWithVatInfo
    .set(OriginalRegistrationQuery(intermediaryNumber), etmpDisplayRegistration).success.value

  private lazy val amendCompleteRoute: String = routes.AmendCompleteController.onPageLoad(waypoints).url

  "AmendComplete Controller" - {

    "must return OK and the correct view for a GET when there are no changes present" in {

      val application = applicationBuilder(userAnswers = Some(originalRegistration))
        .build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, amendCompleteRoute)
        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value
        val view = application.injector.instanceOf[AmendCompleteView]

        val emptySummaryList = SummaryListViewModel(rows = generateSummaryList(emptyUserAnswersWithVatInfo, etmpDisplayRegistration))

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          config.feedbackUrl(request),
          config.intermediaryYourAccountUrl,
          emptySummaryList
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when trading names have changed" in {

      val amendedAnswers: UserAnswers = originalRegistration
        .set(AllTradingNamesQuery, Gen.listOfN(3, arbitraryTradingName.arbitrary).sample.value).success.value

      val application = applicationBuilder(userAnswers = Some(amendedAnswers))
        .build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, amendCompleteRoute)
        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value
        val view = application.injector.instanceOf[AmendCompleteView]

        val amendedSummaryList = SummaryListViewModel(rows = generateSummaryList(amendedAnswers, etmpDisplayRegistration))

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          config.feedbackUrl(request),
          config.intermediaryYourAccountUrl,
          amendedSummaryList
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when previous intermediary registrations have changed" in {

      val amendedAnswers: UserAnswers = originalRegistration
        .set(
          AllPreviousIntermediaryRegistrationsQuery,
          Gen.listOfN(2, arbitraryPreviousIntermediaryRegistrationDetails.arbitrary).sample.value
        ).success.value

      val application = applicationBuilder(userAnswers = Some(amendedAnswers))
        .build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, amendCompleteRoute)
        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value
        val view = application.injector.instanceOf[AmendCompleteView]

        val amendedSummaryList = SummaryListViewModel(rows = generateSummaryList(amendedAnswers, etmpDisplayRegistration))

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          config.feedbackUrl(request),
          config.intermediaryYourAccountUrl,
          amendedSummaryList
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when fixed establishment in EU details have changed" in {

      val amendedAnswers: UserAnswers = originalRegistration
        .set(AllEuDetailsQuery, Gen.listOfN(2, arbitraryEuDetails.arbitrary).sample.value).success.value

      val application = applicationBuilder(userAnswers = Some(amendedAnswers))
        .build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, amendCompleteRoute)
        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value
        val view = application.injector.instanceOf[AmendCompleteView]

        val amendedSummaryList = SummaryListViewModel(rows = generateSummaryList(amendedAnswers, etmpDisplayRegistration))

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          config.feedbackUrl(request),
          config.intermediaryYourAccountUrl,
          amendedSummaryList
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when contact details have changed" in {

      val amendedAnswers: UserAnswers = originalRegistration
        .set(ContactDetailsPage, arbitraryContactDetails.arbitrary.sample.value).success.value

      val application = applicationBuilder(userAnswers = Some(amendedAnswers))
        .build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, amendCompleteRoute)
        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value
        val view = application.injector.instanceOf[AmendCompleteView]

        val amendedSummaryList = SummaryListViewModel(rows = generateSummaryList(amendedAnswers, etmpDisplayRegistration))

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          config.feedbackUrl(request),
          config.intermediaryYourAccountUrl,
          amendedSummaryList
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when bank details have changed" in {

      val amendedAnswers: UserAnswers = originalRegistration
        .set(BankDetailsPage, arbitraryBankDetails.arbitrary.sample.value).success.value

      val application = applicationBuilder(userAnswers = Some(amendedAnswers))
        .build()

      running(application) {
        implicit val msgs: Messages = messages(application)
        val request = FakeRequest(GET, amendCompleteRoute)
        val config = application.injector.instanceOf[FrontendAppConfig]
        val result = route(application, request).value
        val view = application.injector.instanceOf[AmendCompleteView]

        val amendedSummaryList = SummaryListViewModel(rows = generateSummaryList(amendedAnswers, etmpDisplayRegistration))

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          config.feedbackUrl(request),
          config.intermediaryYourAccountUrl,
          amendedSummaryList
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when other address details have changed" in {

      val otherAddress: EtmpOtherAddress = arbitraryEtmpOtherAddress.arbitrary.sample.value
      val niAddress = UkAddress(
        line1 = otherAddress.addressLine1,
        line2 = otherAddress.addressLine2,
        townOrCity = otherAddress.townOrCity,
        county = otherAddress.regionOrState,
        postCode = otherAddress.postcode
      )

      val amendedAnswers: UserAnswers = originalRegistration
        .set(NiAddressPage, niAddress).success.value

      val application = applicationBuilder(userAnswers = Some(amendedAnswers))
        .build()

      running(application) {
        implicit val msgs: Messages = messages(application)
        val request = FakeRequest(GET, amendCompleteRoute)
        val config = application.injector.instanceOf[FrontendAppConfig]
        val result = route(application, request).value
        val view = application.injector.instanceOf[AmendCompleteView]

        val amendedSummaryList = SummaryListViewModel(rows = generateSummaryList(amendedAnswers, etmpDisplayRegistration))

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          config.feedbackUrl(request),
          config.intermediaryYourAccountUrl,
          amendedSummaryList
        )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET when there are no user answers changes present" in {

      val application = applicationBuilder(userAnswers = None)
        .build()

      running(application) {
        val request = FakeRequest(GET, amendCompleteRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }

  private def generateSummaryList(
                                   amendedAnswers: UserAnswers,
                                   etmpDisplayRegistration: EtmpDisplayRegistration
                                 )(implicit msgs: Messages): Seq[SummaryListRow] = {

    val hasTradingNameSummaryRow = HasTradingNameSummary.amendedRow(amendedAnswers)
    val tradingNameSummaryRow = TradingNameSummary.amendedRow(amendedAnswers)
    val removedTradingNameRow = TradingNameSummary.removedRow(removedTradingNames(amendedAnswers, Some(etmpDisplayRegistration)))
    val hasPreviousIntermediaryRegistrationRows = HasPreviouslyRegisteredAsIntermediarySummary.addedRow(amendedAnswers)
    val previousIntermediaryRegistrationRows = PreviousIntermediaryRegistrationsSummary.addedRow(amendedAnswers)
    val hasFixedEstablishmentInEuDetails = HasFixedEstablishmentSummary.amendedRow(amendedAnswers)
    val fixedEstablishmentInEuDetailsSummaryRow = EuDetailsSummary.addedRow(amendedAnswers)
    val removeFixedEstablishmentInEuDetailsRow = EuDetailsSummary
      .removedRow(removedFixedEstablishmentInEuDetailsRow(amendedAnswers, Some(etmpDisplayRegistration)))
    val contactDetailsContactNameSummaryRow = ContactDetailsSummary.amendedRowContactName(amendedAnswers)
    val contactDetailsTelephoneSummaryRow = ContactDetailsSummary.amendedRowTelephoneNumber(amendedAnswers)
    val contactDetailsEmailSummaryRow = ContactDetailsSummary.amendedRowEmailAddress(amendedAnswers)
    val bankDetailsAccountNameSummaryRow = BankDetailsSummary.amendedRowAccountName(amendedAnswers)
    val bankDetailsBicSummaryRow = BankDetailsSummary.amendedRowBIC(amendedAnswers)
    val bankDetailsIbanSummaryRow = BankDetailsSummary.amendedRowIBAN(amendedAnswers)
    val niAddressSummaryRow = NiAddressSummary.amendedRow(amendedAnswers)

    Seq(
      hasTradingNameSummaryRow,
      tradingNameSummaryRow,
      removedTradingNameRow,
      hasPreviousIntermediaryRegistrationRows,
      previousIntermediaryRegistrationRows,
      hasFixedEstablishmentInEuDetails,
      fixedEstablishmentInEuDetailsSummaryRow,
      removeFixedEstablishmentInEuDetailsRow,
      contactDetailsContactNameSummaryRow,
      contactDetailsTelephoneSummaryRow,
      contactDetailsEmailSummaryRow,
      bankDetailsAccountNameSummaryRow,
      bankDetailsBicSummaryRow,
      bankDetailsIbanSummaryRow,
      niAddressSummaryRow
    ).flatten
  }

  private def removedTradingNames(answers: UserAnswers, etmpDisplayRegistration: Option[EtmpDisplayRegistration]): Seq[String] = {

    val amendedAnswers = answers.get(AllTradingNamesQuery).getOrElse(List.empty)
    val originalAnswers = etmpDisplayRegistration.map(_.tradingNames.map(_.tradingName)).getOrElse(Seq.empty)

    originalAnswers.diff(amendedAnswers)
  }

  private def removedFixedEstablishmentInEuDetailsRow(answers: UserAnswers, etmpDisplayRegistration: Option[EtmpDisplayRegistration]): Seq[Country] = {

    val amendedAnswers = answers.get(AllEuDetailsQuery).map(_.map(_.euCountry.code)).getOrElse(List.empty)
    val originalAnswers = etmpDisplayRegistration.map(_.schemeDetails.euRegistrationDetails.map(_.issuedBy)).getOrElse(Seq.empty)

    val removedCountries = originalAnswers.diff(amendedAnswers)

    removedCountries.flatMap(Country.fromCountryCode)
  }
}
