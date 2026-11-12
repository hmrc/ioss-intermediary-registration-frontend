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

package controllers

import models.requests.AuthenticatedDataRequest
import play.api.mvc.AnyContent
import queries.OriginalRegistrationQuery
import queries.amend.PreviousRegistrationIntermediaryNumberQuery

trait GetPreviousOrOriginalRegistration {

  def getPreviousOrOriginalSchemeDetails()(implicit request: AuthenticatedDataRequest[AnyContent]) = {
    if (request.userAnswers.get(PreviousRegistrationIntermediaryNumberQuery).isDefined) {

      request.userAnswers.get(PreviousRegistrationIntermediaryNumberQuery).flatMap { previousIossNum =>
        request.userAnswers.get(OriginalRegistrationQuery(previousIossNum)).map { previousRegistrationWrapper =>
          previousRegistrationWrapper.schemeDetails
        }
      }.getOrElse(throw new IllegalStateException("Previous Scheme Details are not present and required for a previous registration"))

    } else {
      request.registrationWrapper.map(_.etmpDisplayRegistration.schemeDetails).getOrElse {
        throw new IllegalStateException("Scheme Details are not present in the registration wrapper")
      }
    }
  }

}
