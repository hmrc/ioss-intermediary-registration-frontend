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

import logging.Logging
import models.core.Match
import models.requests.AuthenticatedDataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckOtherCountryRegistrationFilterImpl @Inject()(
                                                         service: CoreRegistrationValidationService
                                                       )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[AuthenticatedDataRequest] with Logging {

  private val exclusionStatusCode = 4
  
  override protected def filter[A](request: AuthenticatedDataRequest[A]): Future[Option[Result]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    
    
    service.searchUkVrn(request.vrn)(hc, request).map {
      case Some(activeMatch)
        if isAnActiveIntermediary(activeMatch) =>
        Some(Redirect(controllers.filters.routes.SchemeStillActiveController.onPageLoad(activeMatch.memberState)))

      case Some(activeMatch)
        if isAQuarantinedIntermediary(activeMatch) =>
        Some(Redirect(
          controllers.filters.routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
            activeMatch.memberState,
            activeMatch.getEffectiveDate
          )))

      case _ =>
        None
    }
  }

  private def isAQuarantinedIntermediary(activeMatch: Match) = {
   activeMatch.traderId.isAnIntermediary && (activeMatch.matchType.isQuarantinedTrader || activeMatch.exclusionStatusCode.contains(exclusionStatusCode))
  }

  private def isAnActiveIntermediary(activeMatch: Match) = {
    activeMatch.traderId.isAnIntermediary && activeMatch.matchType.isActiveTrader
  }

}

class CheckOtherCountryRegistrationFilter @Inject()(
                                                     service: CoreRegistrationValidationService
                                                   )(implicit val executionContext: ExecutionContext) {
  def apply(): CheckOtherCountryRegistrationFilterImpl = {
    new CheckOtherCountryRegistrationFilterImpl(service)
  }
}