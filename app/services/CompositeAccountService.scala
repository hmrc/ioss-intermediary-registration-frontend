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

import config.FrontendAppConfig
import connectors.RegistrationConnector
import logging.Logging
import models.CompositeAccount.fromIossEtmpBankDetails
import models.iossRegistration.IossEtmpDisplayRegistration
import models.ossRegistration.OssRegistration
import models.{BankDetails, CompositeAccount, ContactDetails, TradingName}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CompositeAccountService @Inject()(
                                         registrationConnector: RegistrationConnector,
                                         frontendAppConfig: FrontendAppConfig
                                       )(implicit ec: ExecutionContext) extends Logging {

  def getCompositeAccount(
                           enrolments: Enrolments,
                           vrn: Vrn,
                           maybeIossNumber: Option[String]
                         )(implicit hc: HeaderCarrier): Future[Option[CompositeAccount]] = {

    val hasOssEnrolment = enrolments.enrolments.exists(_.key == frontendAppConfig.ossEnrolment)

    if (hasOssEnrolment) {
      registrationConnector.getOssRegistration(vrn).map {
        case Right(ossRegistration) =>
          Some(createCompositeAccount(
            tradingNames = ossRegistration.tradingNames,
            fullName = ossRegistration.contactDetails.fullName,
            telephoneNumber = ossRegistration.contactDetails.telephoneNumber,
            emailAddress = ossRegistration.contactDetails.emailAddress,
            bankDetails = ossRegistration.bankDetails
          ))

        case Left(error) =>
          val errorMessage: String = s"There was a problem retrieving OSS Registration, error: $error."
          logger.error(errorMessage)
          val exception = new Exception(errorMessage)
          throw exception
      }
    } else {
      maybeIossNumber.map { iossNumber =>
        getIossEtmpDisplayRegistration(iossNumber).flatMap {
          case Some(iossEtmpDisplayRegistration) =>
            Some(createCompositeAccount(
              tradingNames = iossEtmpDisplayRegistration.tradingNames.map(_.tradingName),
              fullName = iossEtmpDisplayRegistration.schemeDetails.contactName,
              telephoneNumber = iossEtmpDisplayRegistration.schemeDetails.businessTelephoneNumber,
              emailAddress = iossEtmpDisplayRegistration.schemeDetails.businessEmailId,
              bankDetails = fromIossEtmpBankDetails(iossEtmpDisplayRegistration.bankDetails)
            )).toFuture

          case _ =>
            None.toFuture
        }
      }.getOrElse {
        None.toFuture
      }
    }
  }


  private def getIossEtmpDisplayRegistration(iossNumber: String)
                                            (implicit hc: HeaderCarrier): Future[Option[IossEtmpDisplayRegistration]] = {
    registrationConnector.getIossRegistration(iossNumber).flatMap {
      case Right(iossEtmpDisplayRegistration) =>
        Some(iossEtmpDisplayRegistration).toFuture

      case Left(error) =>
        val errorMessage: String = s"There was a problem retrieving IOSS ETMP Display Registration, error: $error."
        logger.error(errorMessage)
        val exception: Exception = new Exception(errorMessage)
        throw exception
    }
  }

  private def createCompositeAccount(
                                      tradingNames: Seq[String],
                                      fullName: String,
                                      telephoneNumber: String,
                                      emailAddress: String,
                                      bankDetails: BankDetails
                                    ): CompositeAccount = {
    CompositeAccount(
      tradingNames = tradingNames.map(TradingName(_)),
      contactDetails = ContactDetails(
        fullName = fullName,
        telephoneNumber = telephoneNumber,
        emailAddress = emailAddress
      ),
      bankDetails = bankDetails
    )
  }
}
