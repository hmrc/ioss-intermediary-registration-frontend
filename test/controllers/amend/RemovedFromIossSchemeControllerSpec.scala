package controllers.amend

import base.SpecBase
import controllers.routes
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.RemovedFromIossSchemeView

class RemovedFromIossSchemeControllerSpec extends SpecBase {

  "RemovedFromIossScheme Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.RemovedFromIossSchemeController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemovedFromIossSchemeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }
  }
}
