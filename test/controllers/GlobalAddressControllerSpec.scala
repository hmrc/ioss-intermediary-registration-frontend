package controllers

import base.SpecBase
import forms.GlobalAddressFormProvider
import models.{Country, InternationalAddress}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{GlobalAddressPage, NonNiBasedCountryPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AuthenticatedUserAnswersRepository
import views.html.GlobalAddressView

import scala.concurrent.Future

class GlobalAddressControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new GlobalAddressFormProvider()

  private val internationalAddress = InternationalAddress(
    line1 = "line1 test",
    line2 = Some("line2 test"),
    townOrCity = "town or city test",
    stateOrRegion = Some("state or region test"),
    postCode = Some("post code test"),
    country = arbitraryCountry.arbitrary.sample.value
  )
  private val form = formProvider(internationalAddress.country)
  private lazy val globalAddressRoute = routes.GlobalAddressController.onPageLoad(waypoints).url
  private val userAnswersWithCountry = basicUserAnswersWithVatInfo.set(NonNiBasedCountryPage, internationalAddress.country).success.value


  "GlobalAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountry)).build()

      running(application) {
        val request = FakeRequest(GET, globalAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[GlobalAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithCountry.set(GlobalAddressPage, internationalAddress).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, globalAddressRoute)

        val view = application.injector.instanceOf[GlobalAddressView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(internationalAddress), waypoints)(request, messages(application)).toString
      }
    }

    "must save answers and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[AuthenticatedUserAnswersRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCountry))
          .overrides(
            bind[AuthenticatedUserAnswersRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, globalAddressRoute)
            .withFormUrlEncodedBody(
              ("line1", internationalAddress.line1),
              ("line2", internationalAddress.line2.value),
              ("townOrCity", internationalAddress.townOrCity),
              ("stateOrRegion", internationalAddress.stateOrRegion.value),
              ("postCode", internationalAddress.postCode.value),
              ("country", internationalAddress.country.name)
            )

        val result = route(application, request).value
        val expectedAnswers = userAnswersWithCountry.set(GlobalAddressPage, internationalAddress).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual GlobalAddressPage.navigate(waypoints, userAnswersWithCountry, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountry)).build()

      running(application) {
        val request =
          FakeRequest(POST, globalAddressRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[GlobalAddressView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, globalAddressRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, globalAddressRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
