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
import models.CheckMode
import models.requests.AuthenticatedMandatoryIntermediaryRequest
import pages.{EmptyWaypoints, NiBusinessAddressPage, Waypoint}
import pages.amend.ChangeRegistrationPage
import play.api.mvc.{ActionFilter, Result}
import play.api.mvc.Results.Redirect
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckNiBasedAddressFilterImpl()(implicit val executionContext: ExecutionContext)
  extends ActionFilter[AuthenticatedMandatoryIntermediaryRequest]{

  override protected def filter[A](request: AuthenticatedMandatoryIntermediaryRequest[A]): Future[Option[Result]] = {

    val niBusinessAddressAmended = request.userAnswers.get(NiBusinessAddressPage).isDefined
    val niAddress = request.userAnswers.get(NiBusinessAddressPage).exists(_.postCode.toUpperCase.startsWith(niPostCodeAreaPrefix))

    val businessPostcode = request.registrationWrapper.vatInfo.desAddress.postCode.getOrElse("")
    val postcodeMatched = request.userAnswers.get(NiBusinessAddressPage).exists(_.postCode == businessPostcode)
    
    if (niBusinessAddressAmended && niAddress && !postcodeMatched) {
      None.toFuture
    } else {
      val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
      Some(Redirect(controllers.routes.BusinessBasedInNiController.onPageLoad(waypoints).url)).toFuture
    }
  }
}

class CheckNiBasedAddressFilterProvider @Inject()()(implicit ec: ExecutionContext) {

  def apply(): CheckNiBasedAddressFilterImpl = new CheckNiBasedAddressFilterImpl()
}
