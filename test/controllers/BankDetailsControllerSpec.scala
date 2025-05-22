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

package controllers

import base.SpecBase
import forms.BankDetailsFormProvider
import models.ossRegistration.OssRegistration
import models.{BankDetails, Bic, Iban}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest
import org.scalatest.EitherValues.*
import org.scalatestplus.mockito.MockitoSugar
import pages.BankDetailsPage
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.BankDetailsView

class BankDetailsControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new BankDetailsFormProvider()
  val form = formProvider()

  lazy val bankDetailsRoute: String = routes.BankDetailsController.onPageLoad().url

  private val genBic = arbitrary[Bic].sample.value
  private val genIban = arbitrary[Iban].sample.value
  private val bankDetails = BankDetails("account name", Some(genBic), genIban)
  private val userAnswers = basicUserAnswersWithVatInfo.set(BankDetailsPage, bankDetails).success.value


  "BankDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val ossRegistration: Option[OssRegistration] = None

        val numberOfIossRegistrations: Int = 0

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, ossRegistration, numberOfIossRegistrations)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers), numberOfIossRegistrations = 1).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill(BankDetails("account name", Some(genBic), genIban)), waypoints, None, 1)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when Oss Registration is present" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo), ossRegistration = ossRegistration).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val expectedBankDetails = BankDetails(
          accountName = "OSS Account Name",
          bic = Bic("OSSBIC123"),
          iban = Iban("GB33BUKB20201555555555").value
        )

        val result = route(application, request).value
        
        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill(expectedBankDetails), waypoints, ossRegistration, 0)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when Oss Registration and Ioss registrations are present" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo), ossRegistration = ossRegistration, numberOfIossRegistrations = 1).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val expectedBankDetails = BankDetails(
          accountName = "OSS Account Name",
          bic = Bic("OSSBIC123"),
          iban = Iban("GB33BUKB20201555555555").value
        )

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form.fill(expectedBankDetails), waypoints, ossRegistration, 1)(request, messages(application)).toString

      }
    }

    "must return OK and the correct view for a GET when 1 previous Ioss registration is present" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo), ossRegistration = None, numberOfIossRegistrations = 1).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, None, 1)(request, messages(application)).toString

      }
    }

    "must return OK and the correct view for a GET when more than 1 Ioss registrations are present" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo), ossRegistration = None, numberOfIossRegistrations = 2).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val view = application.injector.instanceOf[BankDetailsView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(form, waypoints, None, 2)(request, messages(application)).toString
      }
    }

    //        "must redirect to the next page when valid data is submitted" in {
    //
    //      val mockSessionRepository = mock[SessionRepository]
    //
    //      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
    //
    //      val application =
    //        applicationBuilder(userAnswers = Some(emptyUserAnswers))
    //          .overrides(
    //            bind[SessionRepository].toInstance(mockSessionRepository)
    //          )
    //          .build()
    //
    //      running(application) {
    //        val request =
    //          FakeRequest(POST, bankDetailsRoute)
    //            .withFormUrlEncodedBody(("field1", "value 1"), ("field2", "value 2"))
    //
    //        val result = route(application, request).value
    //
    //        status(result) `mustBe` SEE_OTHER
    //        redirectLocation(result).value `mustBe` onwardRoute.url
    //      }
    //    }

    //    "must return a Bad Request and errors when invalid data is submitted" in {
    //
    //      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
    //
    //      running(application) {
    //        val request =
    //          FakeRequest(POST, bankDetailsRoute)
    //            .withFormUrlEncodedBody(("value", "invalid value"))
    //
    //        val boundForm = form.bind(Map("value" -> "invalid value"))
    //
    //        val view = application.injector.instanceOf[BankDetailsView]
    //
    //        val result = route(application, request).value
    //
    //        status(result) `mustBe` BAD_REQUEST
    //        contentAsString(result) `mustBe` view(boundForm, waypoints)(request, messages(application)).toString
    //      }
    //    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, bankDetailsRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, bankDetailsRoute)
            .withFormUrlEncodedBody(("field1", "value 1"), ("field2", "value 2"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
