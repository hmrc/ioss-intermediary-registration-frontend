/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.actions

import models.UserAnswers
import models.iossRegistration.IossEtmpDisplayRegistration
import models.ossRegistration.OssRegistration
import models.requests.{AuthenticatedDataRequest, AuthenticatedMandatoryIntermediaryRequest}
import play.api.mvc.Result
import uk.gov.hmrc.auth.core.Enrolments
import utils.FutureSyntax.FutureOps

import java.time.{LocalDate, ZoneId}
import scala.concurrent.{ExecutionContext, Future}

case class FakeIntermediaryRequiredActionImpl(
                                               dataToReturn: Option[UserAnswers],
                                               maybeEnrolments: Option[Enrolments],
                                               iossRegistration: Option[IossEtmpDisplayRegistration],
                                               ossRegistration: Option[OssRegistration],
                                               numberOfIossRegistrations: Int
                                             )
  extends IntermediaryRequiredActionImpl()(ExecutionContext.Implicits.global) {

  private val intermediaryNumber: String = "IN9001234567"

  private val emptyUserAnswers: UserAnswers = UserAnswers("12345-credId", lastUpdated = LocalDate.now.atStartOfDay(ZoneId.systemDefault()).toInstant)

  private val data: UserAnswers = dataToReturn match {
    case Some(data) => data
    case _ => emptyUserAnswers
  }

  override protected def refine[A](request: AuthenticatedDataRequest[A]): Future[Either[Result, AuthenticatedMandatoryIntermediaryRequest[A]]] = {
    Right(AuthenticatedMandatoryIntermediaryRequest(
      request = request,
      credentials = request.credentials,
      vrn = request.vrn,
      enrolments = maybeEnrolments.getOrElse(request.enrolments),
      userAnswers = data,
      numberOfIossRegistrations = numberOfIossRegistrations,
      latestIossRegistration = iossRegistration,
      latestOssRegistration = ossRegistration,
      intermediaryNumber = request.intermediaryNumber.getOrElse(intermediaryNumber)
    )).toFuture
  }
}

class FakeIntermediaryRequiredAction(
                                      dataToReturn: Option[UserAnswers],
                                      enrolments: Option[Enrolments] = None,
                                      iossRegistration: Option[IossEtmpDisplayRegistration],
                                      ossRegistration: Option[OssRegistration],
                                      numberOfIossRegistrations: Int
                                    )
  extends IntermediaryRequiredAction()(ExecutionContext.Implicits.global) {
  override def apply(): IntermediaryRequiredActionImpl =
    FakeIntermediaryRequiredActionImpl(dataToReturn, enrolments, iossRegistration, ossRegistration, numberOfIossRegistrations)
}

