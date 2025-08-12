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

package utils

import base.SpecBase
import models.domain.VatCustomerInfo
import models.euDetails.EuDetails
import models.euDetails.RegistrationType.VatNumber
import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetails
import models.requests.AuthenticatedDataRequest
import models.{Index, TradingName, UkAddress, UserAnswers}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.SavedProgressPage
import pages.checkVatDetails.NiAddressPage
import pages.euDetails.*
import pages.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediaryPage, PreviousEuCountryPage, PreviousIntermediaryRegistrationNumberPage}
import pages.tradingNames.{HasTradingNamePage, TradingNamePage}
import play.api.mvc.AnyContent
import play.api.mvc.Results.Redirect
import play.api.test.Helpers.*
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}

class CompletionChecksSpec extends SpecBase with MockitoSugar {

  private object CompletionChecksTests extends CompletionChecks

  private val tradingNameIndex: Index = Index(0)
  private val tradingName: TradingName = arbitraryTradingName.arbitrary.sample.value

  private val previousIntermediaryRegistrationDetails: PreviousIntermediaryRegistrationDetails =
    arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value

  private val euDetails: EuDetails = arbitraryEuDetails.arbitrary.sample.value
    .copy(registrationType = Some(VatNumber))

  private val invalidVatInfo: VatCustomerInfo = vatCustomerInfo
    .copy(desAddress = vatCustomerInfo.desAddress
      .copy(postCode = Some("AA11AA"))
    )

  private val validAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasTradingNamePage, true).success.value
    .set(TradingNamePage(tradingNameIndex), tradingName).success.value
    .set(HasPreviouslyRegisteredAsIntermediaryPage, true).success.value
    .set(PreviousEuCountryPage(countryIndex(0)), previousIntermediaryRegistrationDetails.previousEuCountry).success.value
    .set(PreviousIntermediaryRegistrationNumberPage(countryIndex(0)), previousIntermediaryRegistrationDetails.previousIntermediaryNumber).success.value
    .set(EuCountryPage(countryIndex(0)), euDetails.euCountry).success.value
    .set(HasFixedEstablishmentPage, true).success.value
    .set(RegistrationTypePage(countryIndex(0)), VatNumber).success.value
    .set(EuVatNumberPage(countryIndex(0)), euDetails.euVatNumber.value).success.value
    .set(FixedEstablishmentAddressPage(countryIndex(0)), euDetails.fixedEstablishmentAddress.value).success.value
  
  "CompletionChecks" - {

    ".validate" - {

      "must validate and return true when valid data is present" in {

        val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

        running(application) {
          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]

          when(request.userAnswers) thenReturn validAnswers

          val result = CompletionChecksTests.validate(vatCustomerInfo)

          result `mustBe` true
        }
      }

      "must validate and return false when invalid data is present" in {

        val invalidAnswers: UserAnswers = validAnswers
          .remove(PreviousIntermediaryRegistrationNumberPage(countryIndex(0))).success.value

        val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

        running(application) {
          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]

          when(request.userAnswers) thenReturn invalidAnswers

          val result = CompletionChecksTests.validate(vatCustomerInfo)

          result `mustBe` false
        }
      }

      "must validate and return false when invalid VAT Information address data is present" in {

        val invalidAnswers: UserAnswers = validAnswers
          .copy(vatInfo = Some(invalidVatInfo))

        val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

        running(application) {
          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]

          when(request.userAnswers) thenReturn invalidAnswers

          val result = CompletionChecksTests.validate(invalidVatInfo)

          result `mustBe` false
        }
      }
    }

    ".getFirstValidationErrorRedirect" - {

      "must obtain the first validation error and redirect to the correct page" - {

        "when there is only one validation error present" in {

          val invalidAnswers: UserAnswers = validAnswers
            .remove(PreviousIntermediaryRegistrationNumberPage(countryIndex(0))).success.value

          val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

          running(application) {
            implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]

            when(request.userAnswers) thenReturn invalidAnswers

            val result = CompletionChecksTests.getFirstValidationErrorRedirect(waypoints, vatCustomerInfo)

            result `mustBe` Some(Redirect(PreviousIntermediaryRegistrationNumberPage(countryIndex(0)).route(waypoints).url))
          }
        }

        "when there are multiple validation errors present" in {

          val invalidAnswers: UserAnswers = validAnswers
            .remove(TradingNamePage(tradingNameIndex)).success.value
            .remove(PreviousIntermediaryRegistrationNumberPage(countryIndex(0))).success.value

          val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

          running(application) {
            implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]

            when(request.userAnswers) thenReturn invalidAnswers

            val result = CompletionChecksTests.getFirstValidationErrorRedirect(waypoints, vatCustomerInfo)

            result `mustBe` Some(Redirect(HasTradingNamePage.route(waypoints).url))
          }
        }

        "when there are saved answers with invalid VAT customer information present" in {

          val continueUrl: RedirectUrl = RedirectUrl("/continueUrl")

          val invalidAnswers: UserAnswers = validAnswers
            .set(SavedProgressPage, continueUrl.get(OnlyRelative).url).success.value
            .remove(TradingNamePage(tradingNameIndex)).success.value
            .remove(PreviousIntermediaryRegistrationNumberPage(countryIndex(0))).success.value

          val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

          running(application) {
            implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]

            when(request.userAnswers) thenReturn invalidAnswers

            val result = CompletionChecksTests.getFirstValidationErrorRedirect(waypoints, invalidVatInfo)

            result `mustBe` Some(Redirect(HasTradingNamePage.route(waypoints).url))
          }
        }

        "when here are saved answers with invalid VAT customer information present but new valid Ni address data is provided" in {

          val continueUrl: RedirectUrl = RedirectUrl("/continueUrl")

          val invalidAnswers: UserAnswers = validAnswers
            .set(SavedProgressPage, continueUrl.get(OnlyRelative).url).success.value

          val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

          running(application) {
            implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]

            when(request.userAnswers) thenReturn invalidAnswers

            val result = CompletionChecksTests.getFirstValidationErrorRedirect(waypoints, invalidVatInfo)

            result `mustBe` Some(Redirect(NiAddressPage.route(waypoints).url))
          }
        }
      }

      "must return None when there are no validation errors present" in {

        val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

        running(application) {

          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
          when(request.userAnswers) thenReturn validAnswers

          val result = CompletionChecksTests.getFirstValidationErrorRedirect(waypoints, vatCustomerInfo)

          result `mustBe` None
        }
      }

      "when return None when there are saved answers with invalid VAT customer information present but new valid Ni address data is provided" in {

        val niAddress: UkAddress = UkAddress(
          line1 = "Test Line 1",
          line2 = None,
          townOrCity = "Test Town",
          county = None,
          postCode = "BT11BT"
        )

        val continueUrl: RedirectUrl = RedirectUrl("/continueUrl")

        val invalidAnswers: UserAnswers = validAnswers
          .set(SavedProgressPage, continueUrl.get(OnlyRelative).url).success.value
          .set(NiAddressPage, niAddress).success.value

        val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

        running(application) {
          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]

          when(request.userAnswers) thenReturn invalidAnswers

          val result = CompletionChecksTests.getFirstValidationErrorRedirect(waypoints, invalidVatInfo)

          result `mustBe` None
        }
      }
    }
  }
}
