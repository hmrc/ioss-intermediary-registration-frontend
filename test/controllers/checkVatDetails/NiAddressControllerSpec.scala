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

package controllers.checkVatDetails

import base.SpecBase
import config.FrontendAppConfig
import controllers.amend.routes as amendRoutes
import forms.NiAddressFormProvider
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.TransferringMSID
import models.etmp.display.RegistrationWrapper
import models.{CheckMode, UkAddress, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ChangeRegistrationPage
import pages.checkVatDetails.NiAddressPage
import pages.rejoin.RejoinSchemePage
import pages.{CannotRegisterNotNiBasedBusinessPage, EmptyWaypoints, JourneyRecoveryPage, Waypoint, Waypoints}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import utils.FutureSyntax.FutureOps
import views.html.checkVatDetails.NiAddressView

import java.time.LocalDate

class NiAddressControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new NiAddressFormProvider()
  private val form: Form[UkAddress] = formProvider(isInAmend = false, isExcluded = false, isNiBasedAddress = false)

  private lazy val niAddressRoute: String = routes.NiAddressController.onPageLoad(waypoints).url
  private val registration: RegistrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value

  private val niAddress: UkAddress = UkAddress(
    line1 = "line-1",
    line2 = None,
    townOrCity = "town-or-city",
    county = None,
    postCode = "BT1 2CD",
  )

  private val nonNivatInfo = vatCustomerInfo.copy(desAddress = vatCustomerInfo.desAddress.copy(postCode = Some("AA11BT")))
  private val nonNiVatInfoAnswers: UserAnswers = emptyUserAnswersWithVatInfo
    .copy(vatInfo = Some(nonNivatInfo))

  "NiAddress Controller" - {

    "must throw when VAT info already contains a Northern Ireland address in normal mode" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo)).build()

      running(application) {
        val request = FakeRequest(GET, niAddressRoute)

        val exception = route(application, request).value.failed.futureValue

        val result = route(application, request).value

        exception mustBe a[IllegalStateException]
        exception.getMessage mustBe
          "Cannot access NiAddressPage when VAT info already contains a Northern Ireland address"
      }
    }

    "must return OK and the correct view for a GET when the users vat address is not based in NI and the form is not populated" in {

      val application = applicationBuilder(userAnswers = Some(nonNiVatInfoAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, niAddressRoute)

        val view = application.injector.instanceOf[NiAddressView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, showNiAddressText = true, isExcluded = false)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val updatedAnswers: UserAnswers = nonNiVatInfoAnswers
        .set(NiAddressPage, niAddress).success.value

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, niAddressRoute)

        val view = application.injector.instanceOf[NiAddressView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill(niAddress), waypoints, showNiAddressText = true, isExcluded = false)(request, messages(application)).toString
      }
    }

    "must save the answers and redirect to the next page when valid data is submitted and the postcode area matches 'BT'" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(nonNiVatInfoAnswers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, niAddressRoute)
            .withFormUrlEncodedBody(
              ("line1", niAddress.line1),
              ("townOrCity", niAddress.townOrCity),
              ("postCode", niAddress.postCode)
            )

        val result = route(application, request).value

        val expectedAnswers = nonNiVatInfoAnswers
          .set(NiAddressPage, niAddress).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` NiAddressPage.navigate(waypoints, nonNiVatInfoAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must remove the answers and redirect to the next page when valid data is submitted and the postcode area does not match 'BT'" in {

      val nonNiAddress: UkAddress = niAddress.copy(postCode = "AB12 3CD")

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(nonNiVatInfoAnswers))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, niAddressRoute)
            .withFormUrlEncodedBody(
              ("line1", nonNiAddress.line1),
              ("townOrCity", nonNiAddress.townOrCity),
              ("postCode", nonNiAddress.postCode)
            )

        val result = route(application, request).value

        val expectedAnswers = nonNiVatInfoAnswers

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` CannotRegisterNotNiBasedBusinessPage.route(waypoints).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(nonNiVatInfoAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, niAddressRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[NiAddressView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints, showNiAddressText = false, isExcluded = false)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, niAddressRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, niAddressRoute)
            .withFormUrlEncodedBody(("line1", "value 1"), ("line2", "value 2"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "inAmend" - {

      val amendWaypoints: Waypoints = EmptyWaypoints
        .setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))

      lazy val niAddressAmendRoute: String = routes.NiAddressController.onPageLoad(amendWaypoints).url

      "must return OK and the correct view for a GET when the user has a NI VAT address, is excluded and is in amend" in {

        val registrationWithExclusion: RegistrationWrapper = registration.copy(
          etmpDisplayRegistration = registration.etmpDisplayRegistration.copy(
            exclusions = Seq(registration.etmpDisplayRegistration.exclusions.head.copy(
              exclusionReason = TransferringMSID
            ))
          ))

        val application = applicationBuilder(
          userAnswers = Some(completeUserAnswersWithVatInfo),
          registrationWrapper = Some(registrationWithExclusion)
        ).build()

        running(application) {
          val request = FakeRequest(GET, niAddressAmendRoute)

          val view = application.injector.instanceOf[NiAddressView]

          val result = route(application, request).value

          status(result) `mustBe` OK
          contentAsString(result) `mustBe` view(form, amendWaypoints, showNiAddressText = false, isExcluded = true)(request, messages(application)).toString
        }
      }

      "must save the answers and redirect to the next page when valid data is submitted and the postcode area does not match 'BT'" in {

        val nonNiAddress: UkAddress = niAddress.copy(postCode = "NA11AT")

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

        when(mockSessionRepository.set(any())) thenReturn true.toFuture

        val application =
          applicationBuilder(userAnswers = Some(nonNiVatInfoAnswers))
            .overrides(
              bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, niAddressAmendRoute)
              .withFormUrlEncodedBody(
                ("line1", nonNiAddress.line1),
                ("townOrCity", nonNiAddress.townOrCity),
                ("postCode", nonNiAddress.postCode)
              )

          val result = route(application, request).value

          val expectedAnswers = nonNiVatInfoAnswers
            .set(NiAddressPage, nonNiAddress).success.value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` amendRoutes.HasBusinessAddressInNiController.onPageLoad(amendWaypoints).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
        }
      }

      "must save the answers and redirect to the next page when valid data is submitted and the postcode area does not match 'BT and the user is excluded'" in {

        val exclusion: EtmpExclusion = EtmpExclusion(
          exclusionReason = TransferringMSID,
          effectiveDate = LocalDate.now(stubClockAtArbitraryDate),
          decisionDate = LocalDate.now(stubClockAtArbitraryDate),
          quarantine = false
        )
        val excludedRegistration: RegistrationWrapper = registrationWrapper
          .copy(etmpDisplayRegistration = registrationWrapper.etmpDisplayRegistration.copy(
            exclusions = Seq(exclusion)
          ))

        val nonNiAddress: UkAddress = niAddress.copy(postCode = "NA11AT")

        val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

        when(mockSessionRepository.set(any())) thenReturn true.toFuture

        val application =
          applicationBuilder(
            userAnswers = Some(nonNiVatInfoAnswers),
            registrationWrapper = Some(excludedRegistration)
          )
            .overrides(
              bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {

          val request = FakeRequest(POST, niAddressAmendRoute)
            .withFormUrlEncodedBody(
              ("line1", nonNiAddress.line1),
              ("townOrCity", nonNiAddress.townOrCity),
              ("postCode", nonNiAddress.postCode)
            )
          
          val result = route(application, request).value

          val expectedAnswers = nonNiVatInfoAnswers
            .set(NiAddressPage, nonNiAddress).success.value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` ChangeRegistrationPage.route(waypoints).url
          verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
        }
      }

      "must redirect to Your Account when VAT info already contains a Northern Ireland address in amend mode" in {

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo)).build()

        running(application) {
          val config = application.injector.instanceOf[FrontendAppConfig]

          val request =
            FakeRequest(GET, routes.NiAddressController.onPageLoad(amendWaypoints).url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe config.intermediaryYourAccountUrl
        }
      }

      "must redirect to Your Account on submit when VAT info already contains a Northern Ireland address in amend mode" in {

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo)).build()

        running(application) {
          val config = application.injector.instanceOf[FrontendAppConfig]

          val request =
            FakeRequest(POST, routes.NiAddressController.onSubmit(amendWaypoints).url)
              .withFormUrlEncodedBody(
                "line1" -> "line-1",
                "townOrCity" -> "town-or-city",
                "postCode" -> "BT1 2CD"
              )

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe config.intermediaryYourAccountUrl
        }
      }
    }

    "inRejoin" - {

      val rejoinWaypoints: Waypoints = EmptyWaypoints
        .setNextWaypoint(Waypoint(RejoinSchemePage, CheckMode, RejoinSchemePage.urlFragment))

      "must redirect to Your Account when VAT info already contains a Northern Ireland address in amend mode" in {

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo)).build()

        running(application) {
          val config = application.injector.instanceOf[FrontendAppConfig]

          val request =
            FakeRequest(GET, routes.NiAddressController.onPageLoad(rejoinWaypoints).url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe config.intermediaryYourAccountUrl
        }
      }

      "must redirect to Your Account on submit when VAT info already contains a Northern Ireland address in amend mode" in {

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo)).build()

        running(application) {
          val config = application.injector.instanceOf[FrontendAppConfig]

          val request =
            FakeRequest(POST, routes.NiAddressController.onSubmit(rejoinWaypoints).url)
              .withFormUrlEncodedBody(
                "line1" -> "line-1",
                "townOrCity" -> "town-or-city",
                "postCode" -> "BT1 2CD"
              )

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe config.intermediaryYourAccountUrl
        }
      }
    }
  }
}
