package models.audit

import models.core.{CoreRegistrationRequest, CoreRegistrationValidationResult}
import models.requests.AuthenticatedDataRequest
import play.api.libs.json.{JsValue, Json}

case class IntermediaryRegistrationAuditModel(
                                       credId: String,
                                       userAgent: String,
                                       vrn: String,
                                       coreRegistrationRequest: CoreRegistrationRequest,
                                       coreRegistrationValidationResult: CoreRegistrationValidationResult
                                     ) extends JsonAuditModel {

  override val auditType: String = "CoreRegistrationValidation"

  override val transactionName: String = "core-registration-validation"


  override val detail: JsValue = Json.obj(
    "credId" -> credId,
    "browserUserAgent" -> userAgent,
    "requestersVrn" -> vrn,
    "coreRegistrationRequest" -> Json.toJson(coreRegistrationRequest),
    "coreRegistrationValidationResponse" -> Json.toJson(coreRegistrationValidationResult)
  )
}

object IntermediaryRegistrationAuditModel {

  def build(
             coreRegistrationRequest: CoreRegistrationRequest,
             coreRegistrationValidationResult: CoreRegistrationValidationResult
           )(implicit request: AuthenticatedDataRequest[_]): IntermediaryRegistrationAuditModel =
    IntermediaryRegistrationAuditModel(
      credId = request.credentials.providerId,
      userAgent = request.headers.get("user-agent").getOrElse(""),
      request.vrn.vrn,
      coreRegistrationRequest: CoreRegistrationRequest,
      coreRegistrationValidationResult: CoreRegistrationValidationResult
    )
}