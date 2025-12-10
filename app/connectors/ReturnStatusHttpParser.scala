/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import models.responses.{ErrorResponse, InvalidJson, UnexpectedResponseStatus}
import models.returns.CurrentReturns
import play.api.Logging
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object ReturnStatusHttpParser extends Logging {

  type ReturnStatusResponse = Either[ErrorResponse, Seq[CurrentReturns]]

  implicit object ReturnStatusResponseReads extends HttpReads[ReturnStatusResponse] {
    override def read(method: String, url: String, response: HttpResponse): ReturnStatusResponse = {
      response.status match {
        case OK => response.json.validate[Seq[CurrentReturns]] match {
          case JsSuccess(allCurrentReturns, _) => Right(allCurrentReturns)
          case JsError(errors) =>
            logger.error(s"Failed when trying to parse Seq[CurrentReturns] with error: $errors")
            Left(InvalidJson)
        }

        case status =>
          logger.error(s"There was an error retrieving current returns with status: $status and response body: ${response.body}.")
          Left(UnexpectedResponseStatus(
            response.status,
            s"An error occurred retrieving current returns with status: $status and response body: ${response.body}.")
          )
      }
    }
  }
}
