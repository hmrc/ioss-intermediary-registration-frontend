package connectors

import logging.Logging
import models.SavedUserAnswers
import models.responses.{ConflictFound, ErrorResponse, InvalidJson, UnexpectedResponseStatus}
import play.api.http.Status.{CONFLICT, CREATED, NOT_FOUND, OK}
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object SaveForLaterHttpParser extends Logging {

  type SaveForLaterHResponse = Either[ErrorResponse, Option[SavedUserAnswers]]

  implicit object SaveForLaterHttpReads extends HttpReads[SaveForLaterHResponse] {
    override def read(method: String, url: String, response: HttpResponse): SaveForLaterHResponse = {
      response.status match {
        case OK | CREATED =>
          response.json.validate[SavedUserAnswers] match {
            case JsSuccess(answers, _) => Right(Some(answers))
            case JsError(errors) =>
              logger.error(s"Failed trying to parse JSON with error: $errors. JSON was ${response.json}.", errors)
              Left(InvalidJson)
          }

        case NOT_FOUND =>
          logger.warn(s"Received Not Found for saved user answers.")
          Right(None)

        case CONFLICT =>
          logger.warn(s"Received Conflict Found from server.")
          Left(ConflictFound)

        case status =>
          logger.error(s"Received unexpected error from saved user answers server with status: $status")
          Left(UnexpectedResponseStatus(status, s"Unexpected response received with status: $status"))
      }
    }
  }
}
