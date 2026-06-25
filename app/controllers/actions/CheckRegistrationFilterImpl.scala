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

package controllers.actions

import config.Constants.niPostCodeAreaPrefix
import config.FrontendAppConfig
import connectors.RegistrationConnector
import logging.Logging
import models.etmp.EtmpExclusionReason.Reversal
import models.requests.AuthenticatedIdentifierRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckRegistrationFilterImpl(
                                   inAmend: Boolean,
                                   inRejoin: Boolean,
                                   restrictExcludedAmend: Boolean,
                                   restrictNiVatBusinessAddress: Boolean,
                                   frontendAppConfig: FrontendAppConfig,
                                   registrationConnector: RegistrationConnector
                                 )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[AuthenticatedIdentifierRequest] with Logging {

  override protected def filter[A](request: AuthenticatedIdentifierRequest[A]): Future[Option[Result]] = {
    (hasIntermediaryEnrolment(request), inAmend, inRejoin) match
      case (true, false, false) =>
        Some(Redirect(controllers.routes.AlreadyRegisteredController.onPageLoad().url)).toFuture

      case (false, true, _) | (false, _, true) =>
        Some(Redirect(controllers.routes.NotRegisteredController.onPageLoad().url)).toFuture

      case (true, true, false) =>
        request.intermediaryNumber.map { intermediaryNumber =>
          checkExcludedRegistration(request, intermediaryNumber)
        }.getOrElse {
          val errorMessage: String = "No Intermediary number found, must have an Intermediary number."
          logger.error(errorMessage)
          val exception: IllegalStateException = new IllegalStateException(errorMessage)
          throw exception
        }

      case (true, _, true) if restrictExcludedAmend && restrictNiVatBusinessAddress =>
        Some(Redirect(frontendAppConfig.intermediaryYourAccountUrl)).toFuture

      case _ =>
        if (restrictExcludedAmend && restrictNiVatBusinessAddress) {
          // TODO -> Replace with new no access page
          Some(Redirect(controllers.routes.NotRegisteredController.onPageLoad().url)).toFuture
        } else {
          None.toFuture
        }
  }

  private def hasIntermediaryEnrolment(request: AuthenticatedIdentifierRequest[_]): Boolean = {
    request.enrolments.enrolments.exists(_.key == frontendAppConfig.intermediaryEnrolment)
  }

  private def checkExcludedRegistration(
                                         request: AuthenticatedIdentifierRequest[_],
                                         intermediaryNumber: String
                                       ): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    registrationConnector.displayRegistration(intermediaryNumber).map {
      case Right(registrationWrapper) =>
        registrationWrapper.etmpDisplayRegistration.exclusions.lastOption match {
          case Some(etmpExclusion) =>
            etmpExclusion.exclusionReason match {
              case Reversal =>
                None
              case _ =>
                val isNiVatBusinessAddress: Boolean = registrationWrapper.vatInfo.desAddress.postCode.exists(_.toUpperCase.startsWith(niPostCodeAreaPrefix))

                if (restrictExcludedAmend && restrictNiVatBusinessAddress && !isNiVatBusinessAddress) {
                  None
                } else if (restrictExcludedAmend) {
                  Some(Redirect(frontendAppConfig.intermediaryYourAccountUrl))
                } else {
                  None
                }
            }
          case _ =>
            if (restrictExcludedAmend && restrictNiVatBusinessAddress) {
              Some(Redirect(frontendAppConfig.intermediaryYourAccountUrl))
            } else {
              None
            }
        }

      case Left(error) =>
        val errorMessage: String = s"There was an error retrieving Registration with error response: ${error.body}."
        logger.error(errorMessage)
        val exception: Exception = new Exception(errorMessage)
        throw exception
    }
  }
}

class CheckRegistrationFilterProvider @Inject()(
                                                 frontendAppConfig: FrontendAppConfig,
                                                 registrationConnector: RegistrationConnector
                                               )(implicit executionContext: ExecutionContext) {

  def apply(inAmend: Boolean, inRejoin: Boolean, restrictExcludedAmend: Boolean, restrictNiVatBusinessAddress: Boolean): CheckRegistrationFilterImpl = {
    new CheckRegistrationFilterImpl(inAmend, inRejoin, restrictExcludedAmend, restrictNiVatBusinessAddress, frontendAppConfig, registrationConnector)
  }
}
