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
import models.etmp.EtmpExclusionReason.{Reversal, TransferringMSID}
import models.etmp.EtmpOtherAddress
import models.requests.{AuthenticatedDataRequest, AuthenticatedMandatoryIntermediaryRequest}
import models.{CheckMode, DesAddress, InternationalAddress, UkAddress, UserAnswers}
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ChangeRegistrationPage
import pages.checkVatDetails.NiAddressPage
import pages.rejoin.RejoinSchemePage
import pages.{BusinessStillBasedInNIPage, EmptyWaypoints, GlobalAddressPage, NonNiBasedCountryPage, Waypoint, Waypoints}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running

import scala.concurrent.Future

class CheckNiBasedAddressFilterSpec extends SpecBase with MockitoSugar {

  class Harness extends CheckNiBasedAddressFilterImpl(false) {
    def callFilter(request: AuthenticatedMandatoryIntermediaryRequest[_]): Future[Option[Result]] = filter(request)
  }

  class RejoinHarness extends CheckNiBasedAddressFilterImpl(true) {
    def callFilter(request: AuthenticatedMandatoryIntermediaryRequest[_]): Future[Option[Result]] = filter(request)
  }

  override val waypoints: Waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))

  private val niBasedAddress = UkAddress(
    line1 = "1 The Street",
    line2 = None,
    townOrCity = "Some town",
    county = None,
    postCode = "BT11 1AA"
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

  private val globalAddress: InternationalAddress = arbitraryInternationalAddress.arbitrary.sample.value

  ".filter" - {

    "must return None" - {

      "when the address information is submitted and the postcode area matches 'BT'" in {

        val userAnswersWithNiBasedAddress: UserAnswers = completeUserAnswersWithVatInfo
          .set(NiAddressPage, niBasedAddress).success.value

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

      "when an exclusion exists and is not a reversal" in {

        val userAnswersWithNiBasedAddress: UserAnswers = completeUserAnswersWithVatInfo
          .set(NiAddressPage, niBasedAddress).success.value

        val registrationWrapperWithNonNiAddressWithExclusion = registrationWrapper.copy(
          vatInfo = nonNiVatInfo,
          etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration.copy(
            exclusions = Seq(arbitraryEtmpExclusion.arbitrary.sample.value.copy(
              exclusionReason = TransferringMSID
            ))
          )
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
            Some(registrationWrapperWithNonNiAddressWithExclusion)
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
            registrationWrapperWithNonNiAddressWithExclusion
          )

          val controller = new Harness()

          val result = controller.callFilter(request).futureValue

          result mustBe None
        }
      }

      "when vat info is NI and other address is empty" in {

        val registrationWrapperWithNonNiAddressWithExclusion = registrationWrapper.copy(
          etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration.copy(
            exclusions = Seq(arbitraryEtmpExclusion.arbitrary.sample.value.copy(
              exclusionReason = TransferringMSID
            )),
            otherAddress = None
          )
        )

        val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo)).build()

        running(application) {

          val authDataRequest = AuthenticatedDataRequest(
            FakeRequest(),
            testCredentials,
            vrn,
            testEnrolments,
            completeUserAnswersWithVatInfo,
            Some(iossNumber),
            1,
            None,
            None,
            Some(intermediaryNumber),
            Some(registrationWrapperWithNonNiAddressWithExclusion)
          )

          val request = AuthenticatedMandatoryIntermediaryRequest(
            authDataRequest,
            testCredentials,
            vrn,
            testEnrolments,
            completeUserAnswersWithVatInfo,
            1,
            None,
            None,
            intermediaryNumber,
            registrationWrapperWithNonNiAddressWithExclusion
          )

          val controller = new Harness()

          val result = controller.callFilter(request).futureValue

          result mustBe None
        }
      }

      "when vat info is NI and other address is defined and is Ni" in {

        val registrationWrapperWithNonNiAddressWithExclusion = registrationWrapper.copy(
          etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration.copy(
            exclusions = Seq(arbitraryEtmpExclusion.arbitrary.sample.value.copy(
              exclusionReason = TransferringMSID
            )),
            otherAddress = Some(
              EtmpOtherAddress(
                issuedBy = "GB",
                tradingName = Some("Test trading name"),
                addressLine1 = niBasedAddress.line1,
                addressLine2 = niBasedAddress.line2,
                townOrCity = niBasedAddress.townOrCity,
                regionOrState = niBasedAddress.county,
                postcode = Some(niBasedAddress.postCode),
              )
            )
          )
        )

        val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo)).build()

        running(application) {

          val authDataRequest = AuthenticatedDataRequest(
            FakeRequest(),
            testCredentials,
            vrn,
            testEnrolments,
            completeUserAnswersWithVatInfo,
            Some(iossNumber),
            1,
            None,
            None,
            Some(intermediaryNumber),
            Some(registrationWrapperWithNonNiAddressWithExclusion)
          )

          val request = AuthenticatedMandatoryIntermediaryRequest(
            authDataRequest,
            testCredentials,
            vrn,
            testEnrolments,
            completeUserAnswersWithVatInfo,
            1,
            None,
            None,
            intermediaryNumber,
            registrationWrapperWithNonNiAddressWithExclusion
          )

          val controller = new Harness()

          val result = controller.callFilter(request).futureValue

          result mustBe None
        }
      }

      "when a reversal exclusion exists" - {

        val registrationWrapperWithReversalExclusion = registrationWrapper.copy(
          etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration.copy(
            exclusions = Seq(arbitraryEtmpExclusion.arbitrary.sample.value.copy(
              exclusionReason = Reversal
            ))
          )
        )

        val application = applicationBuilder(userAnswers = Some(completeUserAnswersWithVatInfo)).build()

        running(application) {

          val authDataRequest = AuthenticatedDataRequest(
            FakeRequest(),
            testCredentials,
            vrn,
            testEnrolments,
            completeUserAnswersWithVatInfo,
            Some(iossNumber),
            1,
            None,
            None,
            Some(intermediaryNumber),
            Some(registrationWrapperWithReversalExclusion)
          )

          val request = AuthenticatedMandatoryIntermediaryRequest(
            authDataRequest,
            testCredentials,
            vrn,
            testEnrolments,
            completeUserAnswersWithVatInfo,
            1,
            None,
            None,
            intermediaryNumber,
            registrationWrapperWithReversalExclusion
          )

          val controller = new Harness()

          val result = controller.callFilter(request).futureValue

          result mustBe None
        }
      }
    }

    "must redirect to BusinessBasedInNiPage" - {

      "when no exclusions exist" - {

        val registrationWrapperWithoutExclusion = registrationWrapper.copy(
          etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration.copy(
            exclusions = Seq.empty
          )
        )

        "and the user doesn't have a NI VAT address and hasn't provided an NI address or a global address" in {

          val userAnswersWithNonNiAddress: UserAnswers = completeUserAnswersWithVatInfo

          val registrationWrapperWithNonNiAddress = registrationWrapperWithoutExclusion.copy(
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

            result.value mustBe Redirect(controllers.routes.BusinessBasedInNiController.onPageLoad(waypoints).url)
          }
        }

        "when the otherAddress field retrieved from the database is empty" in {

          val userAnswersWithVatPostcodeMatch: UserAnswers = emptyUserAnswers

          val registrationWrapperWithEmptyOtherAddress = registrationWrapperWithoutExclusion.copy(
            vatInfo = nonNiVatInfo,
            etmpDisplayRegistration = registrationWrapperWithoutExclusion.etmpDisplayRegistration.copy(
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

            result.value mustBe Redirect(controllers.routes.BusinessBasedInNiController.onPageLoad(waypoints).url)
          }
        }

        "when inRejoin is true and global address is defined" in {

          val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(RejoinSchemePage, CheckMode, RejoinSchemePage.urlFragment))

          val userAnswersWithNonNiAddress: UserAnswers = emptyUserAnswers
            .set(BusinessStillBasedInNIPage, false).success.value
            .set(NonNiBasedCountryPage, globalAddress.country).success.value
            .set(GlobalAddressPage, globalAddress).success.value

          val registrationWrapperWithNonNiAddress = registrationWrapperWithoutExclusion.copy(
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

            val controller = new RejoinHarness()

            val result = controller.callFilter(request).futureValue

            result.value mustBe Redirect(controllers.routes.BusinessBasedInNiController.onPageLoad(waypoints).url)
          }
        }
      }
    }
  }
}
