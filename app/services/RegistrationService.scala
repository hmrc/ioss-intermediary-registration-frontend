package services

import connectors.RegistrationConnector
import connectors.RegistrationHttpParser.RegistrationResultResponse
import logging.Logging
import models.UserAnswers
import models.etmp.EtmpRegistrationRequest.buildEtmpRegistrationRequest
import services.etmp.EtmpEuRegistrations
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.Future

class RegistrationService @Inject()(
                                     clock: Clock,
                                     registrationConnector: RegistrationConnector
                                   ) extends EtmpEuRegistrations with Logging {

  def createRegistration(answers: UserAnswers, vrn: Vrn)(implicit hc: HeaderCarrier): Future[RegistrationResultResponse] = {
    val commencementDate = LocalDate.now(clock)
    registrationConnector.createRegistration(buildEtmpRegistrationRequest(answers, vrn, commencementDate))
  }

}
