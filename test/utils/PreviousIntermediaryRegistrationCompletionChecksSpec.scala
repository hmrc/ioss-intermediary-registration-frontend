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
import models.UserAnswers
import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetails
import models.requests.AuthenticatedDataRequest
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.previousIntermediaryRegistrations.*
import play.api.mvc.AnyContent
import play.api.mvc.Results.Redirect
import play.api.test.Helpers.*
import queries.previousIntermediaryRegistrations.AllPreviousIntermediaryRegistrationsWithOptionalIntermediaryNumberQuery

class PreviousIntermediaryRegistrationCompletionChecksSpec extends SpecBase with MockitoSugar {

  private val previousIntermediaryRegistrationCompletionChecksTests: PreviousIntermediaryRegistrationCompletionChecks.type =
    PreviousIntermediaryRegistrationCompletionChecks

  private val previousIntermediaryRegistrationDetails: PreviousIntermediaryRegistrationDetails =
    arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value

  private val previousIntermediaryRegistrationDetails2: PreviousIntermediaryRegistrationDetails =
    arbitraryPreviousIntermediaryRegistrationDetails.arbitrary.sample.value

  private val validAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .set(HasPreviouslyRegisteredAsIntermediaryPage, true).success.value
    .set(PreviousEuCountryPage(countryIndex(0)), previousIntermediaryRegistrationDetails.previousEuCountry).success.value
    .set(PreviousIntermediaryRegistrationNumberPage(countryIndex(0)), previousIntermediaryRegistrationDetails.previousIntermediaryNumber).success.value
  
  ".isPreviousIntermediaryRegistrationsDefined" - {

    "when the HasPreviouslyRegisteredAsIntermediaryPage question is Yes" - {

      "must return true when answers for the section are defined" in {

        val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

        running(application) {

          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
          when(request.userAnswers) thenReturn validAnswers

          val result = previousIntermediaryRegistrationCompletionChecksTests.isPreviousIntermediaryRegistrationsDefined()

          result mustBe true
        }
      }

      "must return false when answers for the section are absent" in {

        val emptySectionAnswers: UserAnswers = emptyUserAnswersWithVatInfo
          .set(HasPreviouslyRegisteredAsIntermediaryPage, true).success.value

        val application = applicationBuilder(userAnswers = Some(emptySectionAnswers)).build()

        running(application) {

          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
          when(request.userAnswers) thenReturn emptySectionAnswers

          val result = previousIntermediaryRegistrationCompletionChecksTests.isPreviousIntermediaryRegistrationsDefined()

          result mustBe false
        }
      }
    }

    "when the HasPreviouslyRegisteredAsIntermediaryPage question is No" - {

      "must return true when answers for the section are empty" in {

        val emptySectionAnswers: UserAnswers = emptyUserAnswersWithVatInfo
          .set(HasPreviouslyRegisteredAsIntermediaryPage, false).success.value

        val application = applicationBuilder(userAnswers = Some(emptySectionAnswers)).build()

        running(application) {

          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
          when(request.userAnswers) thenReturn emptySectionAnswers

          val result = previousIntermediaryRegistrationCompletionChecksTests.isPreviousIntermediaryRegistrationsDefined()

          result mustBe true
        }
      }

      "must return false when answers for the section are defined" in {

        val answers: UserAnswers = validAnswers
          .set(HasPreviouslyRegisteredAsIntermediaryPage, false).success.value
          
        val application = applicationBuilder(userAnswers = Some(answers)).build()

        running(application) {

          implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
          when(request.userAnswers) thenReturn answers

          val result = previousIntermediaryRegistrationCompletionChecksTests.isPreviousIntermediaryRegistrationsDefined()

          result mustBe false
        }
      }
    }
  }

  ".incompletePreviousIntermediaryRegistrationRedirect" - {

    "must redirect to the correct page when there is no Intermediary Number present" in {

      val invalidAnswers: UserAnswers = validAnswers
        .remove(PreviousIntermediaryRegistrationNumberPage(countryIndex(0))).success.value

      val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

      running(application) {

        implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
        when(request.userAnswers) thenReturn invalidAnswers

        val result = previousIntermediaryRegistrationCompletionChecksTests.incompletePreviousIntermediaryRegistrationRedirect(waypoints)

        result `mustBe` Some(Redirect(PreviousIntermediaryRegistrationNumberPage(countryIndex(0)).route(waypoints).url))
      }
    }

    "must return None when a valid Intermediary Number is present" in {

      val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

      running(application) {

        implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
        when(request.userAnswers) thenReturn validAnswers

        val result = previousIntermediaryRegistrationCompletionChecksTests.incompletePreviousIntermediaryRegistrationRedirect(waypoints)

        result `mustBe` None
      }
    }
  }

  ".getAllIncompletePreviousIntermediaryRegistrations" - {

    "must return a Seq of incomplete Previous Intermediary Registrations when Intermediary Numbers are missing" in {

      val invalidAnswers: UserAnswers = validAnswers
        .set(AddPreviousIntermediaryRegistrationPage(Some(countryIndex(0))), true).success.value
        .set(PreviousEuCountryPage(countryIndex(1)), previousIntermediaryRegistrationDetails2.previousEuCountry).success.value
        .set(PreviousIntermediaryRegistrationNumberPage(countryIndex(1)), previousIntermediaryRegistrationDetails2.previousIntermediaryNumber).success.value
        .remove(PreviousIntermediaryRegistrationNumberPage(countryIndex(0))).success.value
        .remove(PreviousIntermediaryRegistrationNumberPage(countryIndex(1))).success.value

      val invalidPreviousIntermediaryRegistrationAnswers = invalidAnswers
        .get(AllPreviousIntermediaryRegistrationsWithOptionalIntermediaryNumberQuery).value

      val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

      running(application) {
        implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
        when(request.userAnswers) thenReturn invalidAnswers

        val result = previousIntermediaryRegistrationCompletionChecksTests.getAllIncompletePreviousIntermediaryRegistrations()

        result `mustBe` invalidPreviousIntermediaryRegistrationAnswers
      }
    }

    "must return List.empty when there are no incomplete Previous Intermediary Registration entries present" in {

      val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

      running(application) {
        implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
        when(request.userAnswers) thenReturn validAnswers

        val result = previousIntermediaryRegistrationCompletionChecksTests.getAllIncompletePreviousIntermediaryRegistrations()

        result `mustBe` List.empty
      }
    }
  }

  ".emptyPreviousIntermediaryRegistrationsRedirect" - {

    "must redirect to the correct page when Previous Intermediary Registrations answers are expected but none are present" in {

      val invalidAnswers: UserAnswers = emptyUserAnswersWithVatInfo
        .set(HasPreviouslyRegisteredAsIntermediaryPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

      running(application) {
        implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
        when(request.userAnswers) thenReturn invalidAnswers

        val result = previousIntermediaryRegistrationCompletionChecksTests.emptyPreviousIntermediaryRegistrationsRedirect(waypoints)

        result `mustBe` Some(Redirect(HasPreviouslyRegisteredAsIntermediaryPage.route(waypoints).url))
      }
    }

    "must redirect to the correct page when Previous Intermediary Registrations answers are not expected but are present" in {

      val invalidAnswers: UserAnswers = validAnswers
        .set(HasPreviouslyRegisteredAsIntermediaryPage, false).success.value

      val application = applicationBuilder(userAnswers = Some(invalidAnswers)).build()

      running(application) {
        implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
        when(request.userAnswers) thenReturn invalidAnswers

        val result = previousIntermediaryRegistrationCompletionChecksTests.emptyPreviousIntermediaryRegistrationsRedirect(waypoints)

        result `mustBe` Some(Redirect(HasPreviouslyRegisteredAsIntermediaryPage.route(waypoints).url))
      }
    }

    "must return None when Previous Intermediary Registrations answers are expected and are present" in {

      val application = applicationBuilder(userAnswers = Some(validAnswers)).build()

      running(application) {
        implicit val request: AuthenticatedDataRequest[AnyContent] = mock[AuthenticatedDataRequest[AnyContent]]
        when(request.userAnswers) thenReturn validAnswers

        val result = previousIntermediaryRegistrationCompletionChecksTests.emptyPreviousIntermediaryRegistrationsRedirect(waypoints)

        result `mustBe` None
      }
    }
  }
}
