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

package controllers.rejoin

import base.SpecBase
import config.Constants.addQuarantineYears
import formats.Format.dateFormatter
import models.Country
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.rejoin.CannotRejoinQuarantinedByOtherCountryView

import java.time.LocalDate

class CannotRejoinQuarantinedByOtherCountryControllerSpec extends SpecBase {

  private val countryCode: String = arbitraryCountry.arbitrary.sample.value.code
  private val countryName: String = Country.fromCountryCodeUnsafe(countryCode).name

  private val effectiveDateFormatted: String = LocalDate.now(stubClockAtArbitraryDate).toString
  private val formattedRejoinDate: String = LocalDate.now(stubClockAtArbitraryDate).plusYears(addQuarantineYears).format(dateFormatter)

  "CannotRejoinQuarantinedByOtherCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CannotRejoinQuarantinedByOtherCountryController.onPageLoad(countryCode, effectiveDateFormatted).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CannotRejoinQuarantinedByOtherCountryView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(countryName, formattedRejoinDate)(request, messages(application)).toString
      }
    }

    "must throw an Exception if country doesn't exist" in {

      val invalidCountryCode: String = "ZZ"
      val errorMessage: String = s"countryCode $invalidCountryCode could not be mapped to a country"

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CannotRejoinQuarantinedByOtherCountryController.onPageLoad(invalidCountryCode, effectiveDateFormatted).url)

        val result = route(application, request).value

        whenReady(result.failed) { exp =>
          exp `mustBe` a[RuntimeException]
          exp.getMessage `mustBe` errorMessage
        }
      }
    }
  }
}
