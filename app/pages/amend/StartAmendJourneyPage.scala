package pages.amend

import pages.QuestionPage
import play.api.libs.json.JsPath

case object StartAmendJourneyPage extends QuestionPage[String] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "startAmendJourney"
}
