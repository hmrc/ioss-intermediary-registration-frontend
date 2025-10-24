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
import formats.Format.dateFormatter
import models.etmp.amend.AmendRegistrationResponse
import models.responses.InternalServerError
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.RegistrationService
import utils.FutureSyntax.FutureOps
import views.html.amend.RemovedFromIossSchemeView

import java.time.LocalDate

class RemovedFromIossSchemeControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val mockRegistrationService: RegistrationService = mock[RegistrationService]

  private val reinstateDeadlineDateFormatted: String = LocalDate.now(stubClockAtArbitraryDate)
    .plusMonths(1)
    .withDayOfMonth(1)
    .format(dateFormatter)

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationService)
  }

  "RemovedFromIossScheme Controller" - {

    "must return OK and the correct view for a GET when amend registration invocation is successful" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .build()

      val amendRegistrationResponse: AmendRegistrationResponse = arbitraryAmendRegistrationResponse.arbitrary.sample.value

      when(mockRegistrationService.amendRegistration(any(), any(), any(), any(), any(), any())(any())) thenReturn Right(amendRegistrationResponse).toFuture

      running(application) {
        val request = FakeRequest(GET, routes.RemovedFromIossSchemeController.onPageLoad().url)
        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemovedFromIossSchemeView]

        val yourAccountUrl: String = config.intermediaryYourAccountUrl

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(yourAccountUrl, reinstateDeadlineDateFormatted)(request, messages(application)).toString
        verify(mockRegistrationService, times(1)).amendRegistration(any(), any(), any(), any(), any(), eqTo(true))(any())
      }
    }

    "must throw an exception when amend registration invocation returns an error" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .build()

      when(mockRegistrationService.amendRegistration(any(), any(), any(), any(), any(), any())(any())) thenReturn Left(InternalServerError).toFuture

      running(application) {
        val request = FakeRequest(GET, routes.RemovedFromIossSchemeController.onPageLoad().url)

        val result = route(application, request).value

        whenReady(result.failed) { exp =>
          exp `mustBe` a[Exception]
          exp.getMessage `mustBe` InternalServerError.body
        }
        verify(mockRegistrationService, times(1)).amendRegistration(any(), any(), any(), any(), any(), eqTo(true))(any())
      }
    }
  }
}
