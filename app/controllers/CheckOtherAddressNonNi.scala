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

import config.Constants.niPostCodeAreaPrefix
import models.requests.AuthenticatedMandatoryIntermediaryRequest
import pages.checkVatDetails.NiAddressPage

object CheckOtherAddressNonNi {

  def checkOtherAddressNi(request: AuthenticatedMandatoryIntermediaryRequest[_]) = {
    request.userAnswers.get(NiAddressPage) match {
      case Some(niAddress) if niAddress.postCode.toUpperCase.startsWith(niPostCodeAreaPrefix) => true
      case _ => false
    }
  }
}
