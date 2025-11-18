package pages

import models.NiBusinessAddress
import play.api.libs.json.JsPath

case object NiBusinessAddressPage extends QuestionPage[NiBusinessAddress] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "niBusinessAddress"
}
