package models.requests

import models.domain.VatCustomerInfo
import play.api.libs.json.{JsValue, Json, OFormat}
import uk.gov.hmrc.domain.Vrn

// TODO -> Test
case class SaveForLaterRequest(
                                vrn: Vrn,
                                data: JsValue,
                                vatInfo: Option[VatCustomerInfo]
                              )

object SaveForLaterRequest {
  
  implicit val format: OFormat[SaveForLaterRequest] = Json.format[SaveForLaterRequest]
}
