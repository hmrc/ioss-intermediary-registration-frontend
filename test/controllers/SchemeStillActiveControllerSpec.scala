/*
 * Copyright 2026 HM Revenue & Customs
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
import models.Country
import models.euDetails.EuDetails
import models.euDetails.RegistrationType.{TaxId, VatNumber}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.euDetails.AllEuDetailsQuery
import views.html.SchemeStillActiveView

class SchemeStillActiveControllerSpec extends SpecBase {

  "SchemeStillActiveController Controller" - {

    ".onPageLoad" - {

      "must return OK and the correct view for a GET" in {

        val country = Country.getCountryName("EE")

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, controllers.filters.routes.SchemeStillActiveController.onPageLoad("EE").url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SchemeStillActiveView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(country, false, false)(request, messages(application)).toString
        }
      }

      "must return OK and the correct view for a Already Registered Vat Number" in {

        val country = Country.getCountryName("EE")
        val euDetails = EuDetails(
          euCountry = Country("EE", "Estonia"),
          hasFixedEstablishment = Some(false),
          registrationType = Some(VatNumber),
          euVatNumber = Some("EE123456789"),
          euTaxReference = None,
          fixedEstablishmentAddress = None
        )

        val userAnswers = emptyUserAnswers.set(AllEuDetailsQuery, List(euDetails)).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, controllers.filters.routes.SchemeStillActiveController.onPageLoad("EE").url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SchemeStillActiveView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(country, true, false)(request, messages(application)).toString
        }
      }

      "must return OK and the correct view for a Already Registered Tax" in {

        val country = Country.getCountryName("EE")
        val euDetails = EuDetails(
          euCountry = Country("EE", "Estonia"),
          hasFixedEstablishment = Some(false),
          registrationType = Some(TaxId),
          euVatNumber = None,
          euTaxReference = Some("ABC123123"),
          fixedEstablishmentAddress = None
        )

        val userAnswers = emptyUserAnswers.set(AllEuDetailsQuery, List(euDetails)).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, controllers.filters.routes.SchemeStillActiveController.onPageLoad("EE").url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SchemeStillActiveView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(country, false, true)(request, messages(application)).toString
        }
      }

    }

  }
}
