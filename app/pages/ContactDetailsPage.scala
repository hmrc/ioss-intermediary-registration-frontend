package pages

import models.ContactDetails
import play.api.libs.json.JsPath

case object ContactDetailsPage extends QuestionPage[ContactDetails] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "contactDetails"
}
