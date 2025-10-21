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