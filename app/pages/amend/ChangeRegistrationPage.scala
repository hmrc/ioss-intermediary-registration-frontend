package pages.amend

import pages.QuestionPage
import play.api.libs.json.JsPath

case object ChangeRegistrationPage extends QuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "changeRegistration"
}
