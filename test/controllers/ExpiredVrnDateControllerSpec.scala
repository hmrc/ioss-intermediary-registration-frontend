package controllers

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.ExpiredVrnDateView

class ExpiredVrnDateControllerSpec extends SpecBase {

  "ExpiredVrnDate Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.ExpiredVrnDateController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ExpiredVrnDateView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }
  }
}
