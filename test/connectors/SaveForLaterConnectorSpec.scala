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

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.*
import models.SavedUserAnswers
import models.requests.SaveForLaterRequest
import models.responses.{ConflictFound, InvalidJson, UnexpectedResponseStatus}
import play.api.http.Status.*
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers.running
import testutils.WireMockHelper
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant

class SaveForLaterConnectorSpec extends SpecBase with WireMockHelper {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val url: String = "/ioss-intermediary-registration/save-for-later"

  // TODO -> Make arbitrary model???
  private val saveForLaterRequest: SaveForLaterRequest = SaveForLaterRequest(
    vrn = vrn,
    data = Json.toJson("savedAnswers"),
    vatInfo = None
  )

  // TODO -> Make arbitrary model???
  private val expectedSavedUserAnswers: SavedUserAnswers = SavedUserAnswers(
    vrn = vrn,
    data = JsObject(Seq("savedAnswers" -> Json.toJson("savedAnswers"))),
    vatInfo = None,
    lastUpdated = Instant.now(stubClockAtArbitraryDate)
  )

  private def application = applicationBuilder()
    .configure("microservice.services.ioss-intermediary-registration.port" -> server.port)
    .build()

  "SaveForLaterConnector" - {

    ".submit" - {

      "must return Right(Some(SavedUserAnswers)) when the server responds with CREATED" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          val responseJson: JsValue = Json.toJson(expectedSavedUserAnswers)

          server.stubFor(
            post(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(CREATED)
                .withBody(responseJson.toString()))
          )

          val result = connector.submit(saveForLaterRequest).futureValue

          result `mustBe` Right(Some(expectedSavedUserAnswers))
        }
      }

      "must return Left(ConflictFound) with server responds with CONFLICT" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            post(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(CONFLICT))
          )

          val result = connector.submit(saveForLaterRequest).futureValue

          result `mustBe` Left(ConflictFound)
        }
      }

      "must return Left(UnexpectedResponseStatus) with server responds with any other error" in {

        val status: Int = INTERNAL_SERVER_ERROR
        val UnexpectedStatusResponseMessage: String = s"Unexpected response received with status: $status"
        val UnexpectedStatusResponse: UnexpectedResponseStatus = UnexpectedResponseStatus(status, UnexpectedStatusResponseMessage)

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            post(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(status))
          )

          val result = connector.submit(saveForLaterRequest).futureValue

          result `mustBe` Left(UnexpectedStatusResponse)
        }
      }
    }

    ".get" - {

      "must return Right(Some(SavedUserAnswers)) when the server responds with OK" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          val responseJson: JsValue = Json.toJson(expectedSavedUserAnswers)

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(OK)
                .withBody(responseJson.toString()))
          )

          val result = connector.get().futureValue

          result `mustBe` Right(Some(expectedSavedUserAnswers))
        }
      }

      "must return Left(InvalidJson) when JSON cannot be parsed correctly" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(OK)
                .withBody(Json.toJson("invalidJson").toString))
          )

          val result = connector.get().futureValue

          result `mustBe` Left(InvalidJson)
        }
      }

      "must return Right(None) when the server responds with NotFound" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(NOT_FOUND))
          )

          val result = connector.get().futureValue

          result `mustBe` Right(None)
        }
      }

      "must return Left(ConflictFound) with server responds with CONFLICT" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(CONFLICT))
          )

          val result = connector.get().futureValue

          result `mustBe` Left(ConflictFound)
        }
      }

      "must return Left(UnexpectedResponseStatus) with server responds with any other error" in {

        val status: Int = INTERNAL_SERVER_ERROR
        val UnexpectedStatusResponseMessage: String = s"Unexpected response received with status: $status"
        val UnexpectedStatusResponse: UnexpectedResponseStatus = UnexpectedResponseStatus(status, UnexpectedStatusResponseMessage)

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(url))
              .willReturn(aResponse()
                .withStatus(status))
          )

          val result = connector.get().futureValue

          result `mustBe` Left(UnexpectedStatusResponse)
        }
      }
    }
  }
}
