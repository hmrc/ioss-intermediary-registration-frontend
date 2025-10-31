package controllers.amend

import base.SpecBase
import connectors.RegistrationConnector
import forms.amend.ViewOrChangePreviousRegistrationFormProvider
import models.amend.PreviousRegistration
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.ViewOrChangePreviousRegistrationsMultiplePage
import pages.{EmptyWaypoints, Waypoints}
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.ioss.AccountService
import views.html.amend.ViewOrChangePreviousRegistrationView
import utils.FutureSyntax.FutureOps
import play.api.inject.bind

import java.time.LocalDate

class ViewOrChangePreviousRegistrationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val previousRegistration = PreviousRegistration(intermediaryNumber, LocalDate.now(), LocalDate.now().plusMonths(6))

  private val formProvider = new ViewOrChangePreviousRegistrationFormProvider()
  private val form: Form[Boolean] = formProvider(intermediaryNumber)

  private val waypoints: Waypoints = EmptyWaypoints

  private lazy val viewOrChangePreviousRegistrationRoute: String = routes.ViewOrChangePreviousRegistrationController.onPageLoad(waypoints).url

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockAccountService: AccountService = mock[AccountService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationConnector)
    Mockito.reset(mockAccountService)
  }

  "must return OK and the correct view for a GET when a single previous registration exists" in {

    when(mockRegistrationConnector.getAccounts()(any())) thenReturn Right(registrationWrapper).toFuture
    when(mockAccountService.getPreviousRegistrations()(any())).thenReturn(Seq(previousRegistration).toFuture)

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
      .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
      .overrides(bind[AccountService].toInstance(mockAccountService))
      .build()

    running(application) {
      val request = FakeRequest(GET, viewOrChangePreviousRegistrationRoute)

      val result = route(application, request).value

      val view = application.injector.instanceOf[ViewOrChangePreviousRegistrationView]

      status(result) mustBe OK
      contentAsString(result) mustBe view(form, waypoints, intermediaryNumber)(request, messages(application)).toString
    }
  }
  "must redirect to the next page on a GET when multiple previous registrations exist" in {

    when(mockRegistrationConnector.getAccounts()(any())) thenReturn Right(registrationWrapper).toFuture
    when(mockAccountService.getPreviousRegistrations()(any())).thenReturn(Seq(previousRegistration).toFuture)

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo))
      .overrides(bind[RegistrationConnector].toInstance(mockRegistrationConnector))
      .overrides(bind[AccountService].toInstance(mockAccountService))
      .build()

    running(application) {
      val request = FakeRequest(GET, viewOrChangePreviousRegistrationRoute)

      val result = route(application, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe ViewOrChangePreviousRegistrationsMultiplePage.route(waypoints).url
    }
  }


}
