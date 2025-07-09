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
import models.euDetails.EuDetails
import models.euDetails.RegistrationType.VatNumber
import models.requests.AuthenticatedDataRequest
import models.{Country, InternationalAddressWithTradingName, UserAnswers, euDetails}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.euDetails.*
import play.api.mvc.AnyContent
import play.api.mvc.Results.Redirect
import play.api.test.Helpers.*

class EuDetailsCompletionChecksSpec extends SpecBase with MockitoSugar {

  private val euDetailsCompletionChecksTests: EuDetailsCompletionChecks.type = EuDetailsCompletionChecks

  private val euVatNumber: String = arbitraryEuVatNumber.sample.value
  private val countryCode: String = euVatNumber.substring(0, 2)
  private val country: Country = Country.euCountries.find(_.code == countryCode).head

  private val euVatNumber2: String = arbitraryEuVatNumber.sample.value
  private val countryCode2: String = euVatNumber2.substring(0, 2)
  private val country2: Country = Country.euCountries.find(_.code == countryCode2).head

  private val feAddress: InternationalAddressWithTradingName = arbitraryInternationalAddressWithTradingName.arbitrary.sample.value

  private val validAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasFixedEstablishmentPage(), true).success.value
    .set(EuCountryPage(countryIndex(0)), country).success.value
    .set(FixedEstablishmentAddressPage(countryIndex(0)), feAddress).success.value
    .set(RegistrationTypePage(countryIndex(0)), VatNumber).success.value
    .set(EuVatNumberPage(countryIndex(0)), euVatNumber).success.value
    .set(AddEuDetailsPage(Some(countryIndex(0))), true).success.value
    .set(EuCountryPage(countryIndex(1)), country2).success.value
    .set(FixedEstablishmentAddressPage(countryIndex(1)), feAddress).success.value
    .set(RegistrationTypePage(countryIndex(1)), VatNumber).success.value
    .set(EuVatNumberPage(countryIndex(1)), euVatNumber2).success.value

  "EuDetailsCompletionChecks" - {

    ".isEuDetailsDefined" - {

      "when the HasFixedEstablishmentPage question is Yes" - {

        "must return true when answers for the section are defined" in {

          val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

          running(application) {

            implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
            when(request.userAnswers) thenReturn validAnswers

            val result = euDetailsCompletionChecksTests.isEuDetailsDefined()

            result mustBe true
          }
        }

        "must return false when answers for the section are absent" in {

          val emptySectionAnswers: UserAnswers = emptyUserAnswersWithVatInfo
            .set(HasFixedEstablishmentPage(), true).success.value

          val application = applicationBuilder(userAnswers = Some(emptySectionAnswers)).build()

          running(application) {

            implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
            when(request.userAnswers) thenReturn emptySectionAnswers

            val result = euDetailsCompletionChecksTests.isEuDetailsDefined()

            result mustBe false
          }
        }
      }

      "when the HasFixedEstablishmentPage question is No" - {

        "must return true when answers for the section are empty" in {

          val emptySectionAnswers: UserAnswers = emptyUserAnswersWithVatInfo
            .set(HasFixedEstablishmentPage(), false).success.value

          val application = applicationBuilder(userAnswers = Some(emptySectionAnswers)).build()

          running(application) {

            implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
            when(request.userAnswers) thenReturn emptySectionAnswers

            val result = euDetailsCompletionChecksTests.isEuDetailsDefined()

            result mustBe true
          }
        }

        "must return false when answers for the section are defined" in {

          val answers: UserAnswers = validAnswers
            .set(HasFixedEstablishmentPage(), false).success.value

          val application = applicationBuilder(userAnswers = Some(answers)).build()

          running(application) {

            implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
            when(request.userAnswers) thenReturn answers

            val result = euDetailsCompletionChecksTests.isEuDetailsDefined()

            result mustBe false
          }
        }
      }
    }

    ".emptyEuDetailsDRedirect" - {

      "must redirect to the correct page when answers are expected but none are present" in {

        val invalidAnswers: UserAnswers = emptyUserAnswers
          .set(HasFixedEstablishmentPage(), true).success.value

        val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

        running(application) {
          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
          when(request.userAnswers) thenReturn invalidAnswers

          val result = euDetailsCompletionChecksTests.emptyEuDetailsDRedirect(waypoints)

          result `mustBe` Some(Redirect(HasFixedEstablishmentPage().route(waypoints).url))
        }
      }

      "must redirect to the correct page when answers are not expected and are present" in {

        val invalidAnswers: UserAnswers = validAnswers
          .set(HasFixedEstablishmentPage(), false).success.value

        val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

        running(application) {
          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
          when(request.userAnswers) thenReturn invalidAnswers

          val result = euDetailsCompletionChecksTests.emptyEuDetailsDRedirect(waypoints)

          result `mustBe` Some(Redirect(HasFixedEstablishmentPage().route(waypoints).url))
        }
      }

      "must return None when EuDetails answers are expected and are present" in {

        val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

        running(application) {
          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
          when(request.userAnswers) thenReturn validAnswers

          val result = euDetailsCompletionChecksTests.emptyEuDetailsDRedirect(waypoints)

          result `mustBe` None
        }
      }
    }

    ".incompleteEuDetailsRedirect" - {

      "must redirect to the correct page when the corresponding details are incomplete" in {

        val incompleteAnswers: UserAnswers = validAnswers
          .remove(EuVatNumberPage(countryIndex(0))).success.value

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

        running(application) {
          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
          when(request.userAnswers) thenReturn incompleteAnswers

          val result = euDetailsCompletionChecksTests.incompleteEuDetailsRedirect(waypoints)

          result `mustBe` Some(Redirect(EuVatNumberPage(countryIndex(0)).route(waypoints).url))
        }
      }

      "must redirect to the EuVatNumber page when the EuVatNumber is absent or invalid" in {

        val incompleteAnswers: UserAnswers = validAnswers
          .remove(EuVatNumberPage(countryIndex(1))).success.value

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

        running(application) {
          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
          when(request.userAnswers) thenReturn incompleteAnswers

          val result = euDetailsCompletionChecksTests.incompleteEuDetailsRedirect(waypoints)

          result `mustBe` Some(Redirect(EuVatNumberPage(countryIndex(1)).route(waypoints).url))
        }
      }

      "must return None when there are no incomplete EuDetails user answers present" in {

        val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

        running(application) {
          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
          when(request.userAnswers) thenReturn validAnswers

          val result = euDetailsCompletionChecksTests.incompleteEuDetailsRedirect(waypoints)

          result `mustBe` None
        }
      }
    }

    ".getIncompleteEuDetails" - {

      "must return Some(EuDetails) when there are incomplete details present" in {

        val incompleteAnswers: UserAnswers = validAnswers
          .remove(EuVatNumberPage(countryIndex(0))).success.value

        val euDetails: EuDetails = EuDetails(
          euCountry = incompleteAnswers.get(EuCountryPage(countryIndex(0))).value,
          hasFixedEstablishment = None,
          registrationType = incompleteAnswers.get(RegistrationTypePage(countryIndex(0))),
          euVatNumber = None,
          euTaxReference = None,
          fixedEstablishmentAddress = incompleteAnswers.get(FixedEstablishmentAddressPage(countryIndex(0)))
        )

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

        running(application) {
          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
          when(request.userAnswers) thenReturn incompleteAnswers

          val result = euDetailsCompletionChecksTests.getIncompleteEuDetails(countryIndex(0))

          result `mustBe` Some(euDetails)
        }
      }

      "must return None when there are no incomplete EuDetails present" in {

        val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

        running(application) {
          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
          when(request.userAnswers) thenReturn validAnswers

          val result = euDetailsCompletionChecksTests.getIncompleteEuDetails(countryIndex(0))

          result `mustBe` None
        }
      }
    }

    ".getAllIncompleteEuDetails" - {

      "must return a Seq[EuDetails] when there are incomplete details present" in {

        val incompleteEuDetails: EuDetails = EuDetails(
          euCountry = validAnswers.get(EuCountryPage(countryIndex(1))).value,
          hasFixedEstablishment = None,
          registrationType = validAnswers.get(RegistrationTypePage(countryIndex(1))),
          euVatNumber = None,
          euTaxReference = None,
          fixedEstablishmentAddress = validAnswers.get(FixedEstablishmentAddressPage(countryIndex(1)))
        )

        val incompleteAnswers: UserAnswers = validAnswers
          .remove(EuVatNumberPage(countryIndex(1))).success.value

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

        running(application) {

          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
          when(request.userAnswers) thenReturn incompleteAnswers

          val result = euDetailsCompletionChecksTests.getAllIncompleteEuDetails()

          result `mustBe` Seq(incompleteEuDetails)
        }
      }

      "must return a Seq[EuDetails] when there are incomplete details present for multiple countries" in {

        val invalidEuVatNumber: String = arbitraryEuVatNumber.sample.value.substring(2, 5)

        val incompleteAnswers: UserAnswers = validAnswers
          .remove(EuVatNumberPage(countryIndex(1))).success.value
          .set(EuVatNumberPage(countryIndex(0)), invalidEuVatNumber).success.value

        val incompleteEuDetails: Seq[EuDetails] = Seq(
          EuDetails(
            euCountry = incompleteAnswers.get(EuCountryPage(countryIndex(0))).value,
            hasFixedEstablishment = None,
            registrationType = incompleteAnswers.get(RegistrationTypePage(countryIndex(0))),
            euVatNumber = incompleteAnswers.get(EuVatNumberPage(countryIndex(0))),
            euTaxReference = None,
            fixedEstablishmentAddress = incompleteAnswers.get(FixedEstablishmentAddressPage(countryIndex(0)))
          ),
          EuDetails(
            euCountry = incompleteAnswers.get(EuCountryPage(countryIndex(1))).value,
            hasFixedEstablishment = None,
            registrationType = incompleteAnswers.get(RegistrationTypePage(countryIndex(1))),
            euVatNumber = None,
            euTaxReference = None,
            fixedEstablishmentAddress = incompleteAnswers.get(FixedEstablishmentAddressPage(countryIndex(1)))
          )
        )

        val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

        running(application) {

          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
          when(request.userAnswers) thenReturn incompleteAnswers

          val result = euDetailsCompletionChecksTests.getAllIncompleteEuDetails()

          result `mustBe` incompleteEuDetails
        }
      }

      "must return an empty List when there are no incomplete EuDetails present" in {

        val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

        running(application) {
          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
          when(request.userAnswers) thenReturn validAnswers

          val result = euDetailsCompletionChecksTests.getAllIncompleteEuDetails()

          result `mustBe` List.empty
        }
      }
    }
  }
}
