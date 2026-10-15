/*
 * Copyright 2026 HM Revenue & Customs
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

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlEqualTo}
import models.Period.getNext
import models.responses.{InvalidJson, UnexpectedResponseStatus}
import models.returns.{CurrentReturns, Return}
import play.api.Application
import play.api.http.Status.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.running
import testutils.WireMockHelper
import uk.gov.hmrc.http.HeaderCarrier

class ReturnStatusConnectorSpec extends SpecBase with WireMockHelper {

  private implicit val hc: HeaderCarrier = new HeaderCarrier()

  private val arbReturn: Return = arbitraryReturn.arbitrary.sample.value

  private def application: Application = GuiceApplicationBuilder()
    .configure(
      "microservice.services.ioss-intermediary-dashboard.host" -> "127.0.0.1",
      "microservice.services.ioss-intermediary-dashboard.port" -> server.port
    )
    .build()

  "ReturnStatusConnector" - {

    ".getCurrentReturns" - {

      val url: String = s"/ioss-intermediary-dashboard/vat-returns/current-returns/$intermediaryNumber"

      "must return a Seq[CurrentReturns] when the server returns a valid payload" in {

        val additionalReturn: Return = arbReturn.copy(period = getNext(arbReturn.period))

        val responseBody: String =
          s"""[
             |   {
             |     "iossNumber" : "IM9001234567",
             |     "incompleteReturns" : [
             |       {
             |         "period" : ${Json.toJson(arbReturn.period)},
             |         "firstDay" : "${arbReturn.firstDay}",
             |         "lastDay" : "${arbReturn.lastDay}",
             |         "dueDate" : "${arbReturn.dueDate}",
             |         "submissionStatus" : "${arbReturn.submissionStatus}",
             |         "inProgress" : ${arbReturn.inProgress},
             |         "isOldest" : ${arbReturn.isOldest}
             |       },
             |       {
             |         "period" : ${Json.toJson(additionalReturn.period)},
             |         "firstDay" : "${additionalReturn.firstDay}",
             |         "lastDay" : "${additionalReturn.lastDay}",
             |         "dueDate" : "${additionalReturn.dueDate}",
             |         "submissionStatus" : "${additionalReturn.submissionStatus}",
             |         "inProgress" : ${additionalReturn.inProgress},
             |         "isOldest" : ${additionalReturn.isOldest}
             |       }
             |     ],
             |     "completedReturns" : [],
             |     "finalReturnsCompleted": false
             |   },
             |  {
             |     "iossNumber" : "IM9001234568",
             |     "incompleteReturns" : [],
             |     "completedReturns" : [
             |      {
             |         "period" : ${Json.toJson(arbReturn.period)},
             |         "firstDay" : "${arbReturn.firstDay}",
             |         "lastDay" : "${arbReturn.lastDay}",
             |         "dueDate" : "${arbReturn.dueDate}",
             |         "submissionStatus" : "${arbReturn.submissionStatus}",
             |         "inProgress" : ${arbReturn.inProgress},
             |         "isOldest" : ${arbReturn.isOldest}
             |       },
             |       {
             |         "period" : ${Json.toJson(additionalReturn.period)},
             |         "firstDay" : "${additionalReturn.firstDay}",
             |         "lastDay" : "${additionalReturn.lastDay}",
             |         "dueDate" : "${additionalReturn.dueDate}",
             |         "submissionStatus" : "${additionalReturn.submissionStatus}",
             |         "inProgress" : ${additionalReturn.inProgress},
             |         "isOldest" : ${additionalReturn.isOldest}
             |       }
             |     ],
             |     "finalReturnsCompleted": false
             |   }
             |]""".stripMargin

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(aResponse()
              .withStatus(OK)
              .withBody(responseBody)
            )
        )

        running(application) {

          val connector = application.injector.instanceOf[ReturnStatusConnector]

          val result = connector.getCurrentReturns(intermediaryNumber).futureValue

          val expectedResult: Seq[CurrentReturns] = Seq(
            CurrentReturns("IM9001234567", Seq(arbReturn, additionalReturn), Seq.empty, false),
            CurrentReturns("IM9001234568", Seq.empty, Seq(arbReturn, additionalReturn), false)
          )

          result `mustBe` Right(expectedResult)
        }
      }

      "must return InvalidJson when the server returns an incorrectly parsed JSON payload" in {

        val invalidResponseBody: String = {
          """[
            |   {
            |     "invalidField" : "IM9001234567",
            |     "incompleteReturns" : [],
            |     "completedReturns" : []
            |   }
            |
            |]""".stripMargin
        }

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(aResponse()
              .withStatus(OK)
              .withBody(invalidResponseBody)
            )
        )

        running(application) {

          val connector = application.injector.instanceOf[ReturnStatusConnector]

          val result = connector.getCurrentReturns(intermediaryNumber).futureValue

          result `mustBe` Left(InvalidJson)
        }
      }

      Seq(NOT_FOUND, UNPROCESSABLE_ENTITY, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { status =>
        s"must return Left(UnexpectedResponseStatus) when the server responds with $status" in {

          val responseBody: String = "ERROR"

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(status)
                .withBody(responseBody)
              )
          )

          running(application) {

            val connector = application.injector.instanceOf[ReturnStatusConnector]

            val result = connector.getCurrentReturns(intermediaryNumber).futureValue

            val expectedResult: UnexpectedResponseStatus = UnexpectedResponseStatus(
              status,
              s"An error occurred retrieving current returns with status: $status and response body: $responseBody."
            )

            result `mustBe` Left(expectedResult)
          }
        }
      }
    }
  }
}
