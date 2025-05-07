package models

import play.api.libs.json._

case class ContactDetails (Contact Name: String, Telephone Number: String)

object ContactDetails {

  implicit val format: OFormat[ContactDetails] = Json.format
}
