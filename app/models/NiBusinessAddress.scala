package models

import play.api.libs.json._

case class NiBusinessAddress (field1: String, field2: String)

object NiBusinessAddress {

  implicit val format: OFormat[NiBusinessAddress] = Json.format
}
