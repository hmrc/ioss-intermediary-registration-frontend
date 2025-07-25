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

package services.etmp

import logging.Logging
import models.UserAnswers
import models.etmp.EtmpOtherIossIntermediaryRegistrations
import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetails
import pages.previousIntermediaryRegistrations.HasPreviouslyRegisteredAsIntermediaryPage
import queries.previousIntermediaryRegistrations.AllPreviousIntermediaryRegistrationsQuery

trait EtmpPreviousIntermediaryRegistrations extends Logging {

  def getPreviousRegistrationDetails(answers: UserAnswers): Seq[EtmpOtherIossIntermediaryRegistrations] = {
    answers.get(HasPreviouslyRegisteredAsIntermediaryPage) match {
      case Some(true) =>
        answers.get(AllPreviousIntermediaryRegistrationsQuery) match {
          case Some(previousRegistrations) =>
            previousRegistrations.map { previousRegistration =>
              processPreviousIntermediaryRegistrationDetails(previousRegistration)
            }
          case None =>
            val exception = new IllegalStateException("User must provide previous Eu details when previously tax registered in the EU")
            logger.error(exception.getMessage, exception)
            throw exception
        }
      case Some(false) =>
        List.empty
      case _ =>
        val exception = new IllegalStateException("User must answer if they are previously tax registered in the EU")
        logger.error(exception.getMessage, exception)
        throw exception
    }
  }

  private def processPreviousIntermediaryRegistrationDetails(
                                                    previousRegistration: PreviousIntermediaryRegistrationDetails
                                                  ): EtmpOtherIossIntermediaryRegistrations = {
    EtmpOtherIossIntermediaryRegistrations(
      issuedBy = previousRegistration.previousEuCountry.code,
      intermediaryNumber = previousRegistration.previousIntermediaryNumber
    )
  }

}

