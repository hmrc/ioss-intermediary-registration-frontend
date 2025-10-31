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

package services.ioss

import base.SpecBase
import config.Constants.intermediaryEnrolmentKey
import connectors.RegistrationConnector
import models.amend.PreviousRegistration
import models.enrolments.{EACDEnrolment, EACDEnrolments, EACDIdentifiers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global

class AccountServiceSpec extends SpecBase {

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]

  private implicit val hc: HeaderCarrier = new HeaderCarrier()

  private val eACDEnrolment1: EACDEnrolment = arbitraryEACDEnrolment.arbitrary.sample.value.copy(
    identifiers = Seq(arbitraryEACDIdentifiers.arbitrary.sample.value.copy(key = intermediaryEnrolmentKey))
  )

  private val eACDEnrolment2: EACDEnrolment = arbitraryEACDEnrolment.arbitrary.sample.value.copy(
    identifiers = Seq(arbitraryEACDIdentifiers.arbitrary.sample.value.copy(key = intermediaryEnrolmentKey))
  )

  private val eACDEnrolments: EACDEnrolments = arbitraryEACDEnrolments.arbitrary.sample.value
    .copy(enrolments = Seq(eACDEnrolment1, eACDEnrolment2))

  "AccountService" - {

    "must retrieve the latest intermediary account if one exists" in {

      when(mockRegistrationConnector.getAccounts()(any())) thenReturn eACDEnrolments.toFuture

      val service = new AccountService(mockRegistrationConnector)

      val result = service.getLatestAccount().futureValue

      result mustBe Some(eACDEnrolments.enrolments.maxBy(_.activationDate).identifiers.head.value)
    }

    "must return None when no intermediary accounts are retrieved" in {

      when(mockRegistrationConnector.getAccounts()(any())) thenReturn arbitraryEACDEnrolments.arbitrary.sample.value.toFuture

      val service = new AccountService(mockRegistrationConnector)

      val result = service.getLatestAccount().futureValue

      result mustBe None
    }
  }
  "getPreviousRegistrations" - {

    "must return all previous registrations without current registration" in {

      val startPeriod: LocalDateTime = LocalDateTime.of(2023, 6, 1, 0, 0)
      val nextActivation: LocalDateTime = LocalDateTime.of(2023, 9, 1, 0, 0)

      val enrolment1 = EACDEnrolment(
        service = "HMRC-IOSS-ORG",
        state = "Activated",
        activationDate = Some(startPeriod),
        identifiers = Seq(EACDIdentifiers("IntNumber", intermediaryNumber))
      )

      val enrolment2 = EACDEnrolment(
        service = "HMRC-IOSS-ORG",
        state = "Activated",
        activationDate = Some(nextActivation),
        identifiers = Seq(EACDIdentifiers("IntNumber", "IN9001234568"))
      )

      val eacdEnrolments = EACDEnrolments(Seq(enrolment1, enrolment2))

      when(mockRegistrationConnector.getAccounts()(any())) thenReturn eacdEnrolments.toFuture

      val service = new AccountService(mockRegistrationConnector)

      val result = service.getPreviousRegistrations().futureValue

      result mustBe Seq(
        PreviousRegistration(
          intermediaryNumber = intermediaryNumber,
          startPeriod = startPeriod.toLocalDate,
          endPeriod = nextActivation.toLocalDate.minusMonths(1)
        )
      )
    }
  }

}
