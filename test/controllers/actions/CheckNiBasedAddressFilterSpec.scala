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

package controllers.actions

import base.SpecBase
import controllers.Execution.trampoline
import models.{CheckMode, DesAddress, UkAddress, UserAnswers}
import models.requests.{AuthenticatedDataRequest, AuthenticatedMandatoryIntermediaryRequest}
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ChangeRegistrationPage
import pages.{EmptyWaypoints, NiBusinessAddressPage, Waypoint}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running

import scala.concurrent.Future

class CheckNiBasedAddressFilterSpec extends SpecBase with MockitoSugar {

  class Harness extends CheckNiBasedAddressFilterImpl {
    def callFilter(request: AuthenticatedMandatoryIntermediaryRequest[_]): Future[Option[Result]] = filter(request)
  }
  
  private val niBasedAddress = UkAddress(
    line1 = "1 The Street",
    line2 = None,
    townOrCity = "Some town",
    county = None,
    postCode = "BT11 1AA"
  )

  private val nonNiAddressDoesNotMatchVatPostcode = UkAddress(
    line1 = "1 The Street",
    line2 = None,
    townOrCity = "Some town",
    county = None,
    postCode = "YY11 1YY"
  )

  private val nonNiAddressMatchesVatPostcode = UkAddress(
    line1 = "1 The Street",
    line2 = None,
    townOrCity = "Some town",
    county = None,
    postCode = "AA11 1AA"
  )

  private val nonNiVatInfo = vatCustomerInfo.copy(
    desAddress = DesAddress(
      line1 = "1 The Street",
      line2 = None,
      line3 = None,
      line4 = None,
      line5 = None,
      postCode = Some("AA11 1AA"),
      countryCode = "GB"
    )
  )

  ".filter" - {

    "must return None" - {

      "when the address information is submitted and the postcode area matches 'BT'" in {

        val userAnswersWithNiBasedAddress: UserAnswers = completeUserAnswersWithVatInfo.set(NiBusinessAddressPage, niBasedAddress).get

        val registrationWrapperWithNonNiAddress = registrationWrapper.copy(
          vatInfo = nonNiVatInfo
        )

        val application = applicationBuilder(userAnswers = Some(userAnswersWithNiBasedAddress)).build()

        running(application) {

          val authDataRequest = AuthenticatedDataRequest(
            FakeRequest(),
            testCredentials,
            vrn,
            testEnrolments,
            userAnswersWithNiBasedAddress,
            Some(iossNumber),
            1,
            None,
            None,
            Some(intermediaryNumber),
            Some(registrationWrapperWithNonNiAddress)
          )

          val request = AuthenticatedMandatoryIntermediaryRequest(
            authDataRequest,
            testCredentials,
            vrn,
            testEnrolments,
            userAnswersWithNiBasedAddress,
            1,
            None,
            None,
            intermediaryNumber,
            registrationWrapperWithNonNiAddress
          )

          val controller = new Harness()

          val result = controller.callFilter(request).futureValue

          result mustBe None
        }
      }

      "when the submitted postcode does not match with the existing postcode in the database" in {

        val userAnswersWithoutVatPostcodeMatch: UserAnswers = completeUserAnswersWithVatInfo.set(NiBusinessAddressPage, nonNiAddressDoesNotMatchVatPostcode).get

        val registrationWrapperWithNonNiAddress = registrationWrapper.copy(
          vatInfo = nonNiVatInfo
        )

        val application = applicationBuilder(userAnswers = Some(userAnswersWithoutVatPostcodeMatch)).build()

        running(application) {

          val authDataRequest = AuthenticatedDataRequest(
            FakeRequest(),
            testCredentials,
            vrn,
            testEnrolments,
            userAnswersWithoutVatPostcodeMatch,
            Some(iossNumber),
            1,
            None,
            None,
            Some(intermediaryNumber),
            Some(registrationWrapperWithNonNiAddress)
          )

          val request = AuthenticatedMandatoryIntermediaryRequest(
            authDataRequest,
            testCredentials,
            vrn,
            testEnrolments,
            userAnswersWithoutVatPostcodeMatch,
            1,
            None,
            None,
            intermediaryNumber,
            registrationWrapperWithNonNiAddress
          )

          val controller = new Harness()

          val result = controller.callFilter(request).futureValue

          result mustBe None
        }
      }
    }

    "must redirect to BusinessBasedNiPage" - {

      "when the form is submitted but the user hasn't provided an NI address" in {

        val userAnswersWithNonNiAddress: UserAnswers = emptyUserAnswers.set(NiBusinessAddressPage, nonNiAddressMatchesVatPostcode).get

        val registrationWrapperWithNonNiAddress = registrationWrapper.copy(
          vatInfo = nonNiVatInfo
        )

        val application = applicationBuilder(userAnswers = Some(userAnswersWithNonNiAddress)).build()

        running(application) {

          val authDataRequest = AuthenticatedDataRequest(
            FakeRequest(),
            testCredentials,
            vrn,
            testEnrolments,
            userAnswersWithNonNiAddress,
            Some(iossNumber),
            1,
            None,
            None,
            Some(intermediaryNumber),
            Some(registrationWrapperWithNonNiAddress)
          )

          val request = AuthenticatedMandatoryIntermediaryRequest(
            authDataRequest,
            testCredentials,
            vrn,
            testEnrolments,
            userAnswersWithNonNiAddress,
            1,
            None,
            None,
            intermediaryNumber,
            registrationWrapperWithNonNiAddress
          )

          val controller = new Harness()


          val result = controller.callFilter(request).futureValue

          val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
          result.value mustBe Redirect(controllers.routes.BusinessBasedInNiController.onPageLoad(waypoints).url)
        }

      }

      "when the submitted postcode matches the existing postcode in the database" in {

        val userAnswersWithVatPostcodeMatch: UserAnswers = emptyUserAnswers.set(NiBusinessAddressPage, nonNiAddressMatchesVatPostcode).get

        val registrationWrapperWithNonNiAddress = registrationWrapper.copy(
          vatInfo = nonNiVatInfo
        )

        val application = applicationBuilder(userAnswers = Some(userAnswersWithVatPostcodeMatch)).build()

        running(application) {

          val authDataRequest = AuthenticatedDataRequest(
            FakeRequest(),
            testCredentials,
            vrn,
            testEnrolments,
            userAnswersWithVatPostcodeMatch,
            Some(iossNumber),
            1,
            None,
            None,
            Some(intermediaryNumber),
            Some(registrationWrapperWithNonNiAddress)
          )

          val request = AuthenticatedMandatoryIntermediaryRequest(
            authDataRequest,
            testCredentials,
            vrn,
            testEnrolments,
            userAnswersWithVatPostcodeMatch,
            1,
            None,
            None,
            intermediaryNumber,
            registrationWrapperWithNonNiAddress
          )

          val controller = new Harness()

          val result = controller.callFilter(request).futureValue

          val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
          result.value mustBe Redirect(controllers.routes.BusinessBasedInNiController.onPageLoad(waypoints).url)
        }
      }

      "when the otherAddress field retrieved from the database is empty" in {

        val userAnswersWithVatPostcodeMatch: UserAnswers = emptyUserAnswers.set(NiBusinessAddressPage, nonNiAddressMatchesVatPostcode).get

        val registrationWrapperWithEmptyOtherAddress = registrationWrapper.copy(
          vatInfo = nonNiVatInfo,
          etmpDisplayRegistration = arbitraryEtmpDisplayRegistration.arbitrary.sample.value.copy(
            otherAddress = None
          )
        )

        val application = applicationBuilder(userAnswers = Some(userAnswersWithVatPostcodeMatch)).build()

        running(application) {

          val authDataRequest = AuthenticatedDataRequest(
            FakeRequest(),
            testCredentials,
            vrn,
            testEnrolments,
            userAnswersWithVatPostcodeMatch,
            Some(iossNumber),
            1,
            None,
            None,
            Some(intermediaryNumber),
            Some(registrationWrapperWithEmptyOtherAddress)
          )

          val request = AuthenticatedMandatoryIntermediaryRequest(
            authDataRequest,
            testCredentials,
            vrn,
            testEnrolments,
            userAnswersWithVatPostcodeMatch,
            1,
            None,
            None,
            intermediaryNumber,
            registrationWrapperWithEmptyOtherAddress
          )

          val controller = new Harness()

          val result = controller.callFilter(request).futureValue

          val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
          result.value mustBe Redirect(controllers.routes.BusinessBasedInNiController.onPageLoad(waypoints).url)
        }
      }
    }
  }
}
