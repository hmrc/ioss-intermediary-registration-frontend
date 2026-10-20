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

package services

import base.SpecBase
import config.FrontendAppConfig
import connectors.RegistrationConnector
import models.iossRegistration.IossEtmpDisplayRegistration
import models.ossRegistration.OssRegistration
import models.responses.InternalServerError
import models.{BankDetails, CompositeAccount, ContactDetails, TradingName}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.{BeforeAndAfterEach, PrivateMethodTester}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CompositeAccountServiceSpec extends SpecBase with MockitoSugar with PrivateMethodTester with BeforeAndAfterEach {

  private val ossEnrolmentKey: String = "HMRC-OSS-ORG"
  private val ossEnrolmentIdentifier: String = "VRN"

  private val iossEnrolmentKey: String = "HMRC-IOSS-ORG"
  private val iossEnrolmentIdentifier: String = "IOSSNumber"

  private val iossNumber: String = "IM9001234567"

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  when(mockFrontendAppConfig.ossEnrolment) thenReturn ossEnrolmentKey
  when(mockFrontendAppConfig.iossEnrolment) thenReturn iossEnrolmentKey


  private val enrolments: Enrolments = Enrolments(
    enrolments = Set(
      Enrolment(
        key = ossEnrolmentKey,
        identifiers = Seq(EnrolmentIdentifier(ossEnrolmentIdentifier, vrn.vrn)),
        state = "state"
      ),
      Enrolment(
        key = iossEnrolmentKey,
        identifiers = Seq(EnrolmentIdentifier(iossEnrolmentIdentifier, iossNumber)),
        state = "state"
      )
    )
  )

  private val iossEtmpDisplayRegistration: IossEtmpDisplayRegistration = arbitraryIossEtmpDisplayRegistration.arbitrary.sample.value
  private val ossRegistration: OssRegistration = arbitraryOssRegistration.arbitrary.sample.value

  private implicit val hc: HeaderCarrier = new HeaderCarrier()

  override def beforeEach(): Unit = {
    Mockito.reset(
      mockRegistrationConnector
    )
  }

  "CompositeAccountService" - {

    ".getCompositeAccount" - {

      "must return None when there are no additional accounts retrieved from enrolments" in {

        val noEnrolments: Enrolments = enrolments.copy(enrolments = Set.empty)

        val service = CompositeAccountService(mockRegistrationConnector, mockFrontendAppConfig)

        val result = service.getCompositeAccount(noEnrolments, vrn, None).futureValue

        result `mustBe` None
        verifyNoInteractions(mockRegistrationConnector)
      }

      "when checking for an OSS Registration" - {

        "must throw an Exception when the server responds with an error" in {

          val errorMessage: String = s"There was a problem retrieving OSS Registration, error: $InternalServerError."

          when(mockRegistrationConnector.getOssRegistration(any())(any())) thenReturn Left(InternalServerError).toFuture

          val service = CompositeAccountService(mockRegistrationConnector, mockFrontendAppConfig)

          val result = service.getCompositeAccount(enrolments, vrn, None).failed

          whenReady(result) { exp =>
            exp `mustBe` a[Exception]
            exp.getMessage `mustBe` errorMessage
          }
          verify(mockRegistrationConnector, times(1)).getOssRegistration(eqTo(vrn))(any())
        }

        "must create a composite account with an OSS Registration when the server responds with Right()" in {

          when(mockRegistrationConnector.getOssRegistration(any())(any())) thenReturn Right(ossRegistration).toFuture

          val service = CompositeAccountService(mockRegistrationConnector, mockFrontendAppConfig)

          val result = service.getCompositeAccount(enrolments, vrn, None).futureValue

          val expectedResult: CompositeAccount = CompositeAccount(
            tradingNames = ossRegistration.tradingNames.map(TradingName(_)),
            contactDetails = ContactDetails(
              fullName = ossRegistration.contactDetails.fullName,
              telephoneNumber = ossRegistration.contactDetails.telephoneNumber,
              emailAddress = ossRegistration.contactDetails.emailAddress,
            ),
            bankDetails = ossRegistration.bankDetails
          )

          result `mustBe` Some(expectedResult)
          verify(mockRegistrationConnector, times(1)).getOssRegistration(eqTo(vrn))(any())
        }
      }

      "when checking for an IOSS Registration" - {

        val iossEnrolment: Enrolments = enrolments.copy(enrolments = enrolments.enrolments.tail)

        "must throw an Exception when the server responds with an error" in {

          val errorMessage: String = s"There was a problem retrieving IOSS ETMP Display Registration, error: $InternalServerError."

          when(mockRegistrationConnector.getIossRegistration(any())(any())) thenReturn Left(InternalServerError).toFuture

          val service = CompositeAccountService(mockRegistrationConnector, mockFrontendAppConfig)

          val result = service.getCompositeAccount(iossEnrolment, vrn, Some(iossNumber)).failed

          whenReady(result) { exp =>
            exp `mustBe` a[Exception]
            exp.getMessage `mustBe` errorMessage
          }
          verify(mockRegistrationConnector, times(1)).getIossRegistration(eqTo(iossNumber))(any())
        }

        "must create a composite account with an IOSS ETMP Display Registration when the server responds with a Right(successful response)" in {

          when(mockRegistrationConnector.getIossRegistration(any())(any())) thenReturn Right(iossEtmpDisplayRegistration).toFuture

          val service = CompositeAccountService(mockRegistrationConnector, mockFrontendAppConfig)

          val result = service.getCompositeAccount(iossEnrolment, vrn, Some(iossNumber)).futureValue

          val expectedResult: CompositeAccount = CompositeAccount(
            tradingNames = iossEtmpDisplayRegistration.tradingNames.map(x => TradingName(x.tradingName)),
            contactDetails = ContactDetails(
              fullName = iossEtmpDisplayRegistration.schemeDetails.contactName,
              telephoneNumber = iossEtmpDisplayRegistration.schemeDetails.businessTelephoneNumber,
              emailAddress = iossEtmpDisplayRegistration.schemeDetails.businessEmailId
            ),
            bankDetails = BankDetails(
              accountName = iossEtmpDisplayRegistration.bankDetails.accountName,
              bic = iossEtmpDisplayRegistration.bankDetails.bic,
              iban = iossEtmpDisplayRegistration.bankDetails.iban,
            )
          )

          result `mustBe` Some(expectedResult)
          verify(mockRegistrationConnector, times(1)).getIossRegistration(eqTo(iossNumber))(any())
        }
      }
    }

    ".getIossEtmpDisplayRegistration" - {

      "must return an IOSS ETMP Display Registration when an IOSS enrolment exists and the server responds with Right()" in {

        when(mockRegistrationConnector.getIossRegistration(any())(any())) thenReturn Right(iossEtmpDisplayRegistration).toFuture

        val service = CompositeAccountService(mockRegistrationConnector, mockFrontendAppConfig)
        val privateMethod = PrivateMethod[Future[IossEtmpDisplayRegistration]](Symbol("getIossEtmpDisplayRegistration"))

        val result = service invokePrivate privateMethod(iossNumber, hc)

        result.futureValue `mustBe` iossEtmpDisplayRegistration
        verify(mockRegistrationConnector, times(1)).getIossRegistration(eqTo(iossNumber))(any())
      }

      "must throw an Exception when an IOSS enrolment exists and the server responds with an error" in {

        val errorMessage: String = s"There was a problem retrieving IOSS ETMP Display Registration, error: $InternalServerError."

        when(mockRegistrationConnector.getIossRegistration(any())(any())) thenReturn Left(InternalServerError).toFuture

        val service = CompositeAccountService(mockRegistrationConnector, mockFrontendAppConfig)
        val privateMethod = PrivateMethod[Future[IossEtmpDisplayRegistration]](Symbol("getIossEtmpDisplayRegistration"))

        val result = service invokePrivate privateMethod(iossNumber, hc)

        whenReady(result.failed) { exp =>
          exp `mustBe` a[Exception]
          exp.getMessage `mustBe` errorMessage
        }
        verify(mockRegistrationConnector, times(1)).getIossRegistration(eqTo(iossNumber))(any())
      }
    }
  }
}
