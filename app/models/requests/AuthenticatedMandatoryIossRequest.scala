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

package models.requests

import models.UserAnswers
import models.etmp.display.RegistrationWrapper
import models.iossRegistration.IossEtmpDisplayRegistration
import models.ossRegistration.OssRegistration
import play.api.mvc.WrappedRequest
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.Vrn


case class AuthenticatedMandatoryIossRequest[A](
                                                         request: AuthenticatedDataRequest[A],
                                                         credentials: Credentials,
                                                         vrn: Vrn,
                                                         enrolments: Enrolments,
                                                         userAnswers: UserAnswers,
                                                         iossNumber: String,
                                                         numberOfIossRegistrations: Int,
                                                         latestIossRegistration: Option[IossEtmpDisplayRegistration],
                                                         latestOssRegistration: Option[OssRegistration],
                                                         registrationWrapper: RegistrationWrapper,
                                                       ) extends WrappedRequest[A](request) {

  val userId: String = credentials.providerId

  lazy val hasMultipleIossEnrolments: Boolean = {
    enrolments.enrolments
      .filter(_.key == "HMRC-IOSS-ORG")
      .toSeq
      .flatMap(_.identifiers
        .filter(_.key == "IOSSNumber")
        .map(_.value)
      ).size > 1
  }
  
}