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
import models.etmp.EtmpExclusionReason.Reversal
import models.requests.AuthenticatedMandatoryIntermediaryRequest
import pages.amend.ChangeRegistrationPage
import pages.checkVatDetails.NiAddressPage
import pages.rejoin.RejoinSchemePage
import pages.{EmptyWaypoints, GlobalAddressPage, Waypoint, Waypoints}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckNiBasedAddressFilterImpl(inRejoin: Boolean)(implicit val executionContext: ExecutionContext)
  extends ActionFilter[AuthenticatedMandatoryIntermediaryRequest] {

  override protected def filter[A](request: AuthenticatedMandatoryIntermediaryRequest[A]): Future[Option[Result]] = {

    val niBusinessAddressDefined = request.userAnswers.get(NiAddressPage).isDefined
    val niAddress = request.userAnswers.get(NiAddressPage).exists(_.postCode.toUpperCase.startsWith(niPostCodeAreaPrefix))

    val vatInfoPostcodeInNi = request.registrationWrapper.vatInfo.desAddress.postCode.exists(_.toUpperCase.startsWith(niPostCodeAreaPrefix))
    val isOtherAddressEmpty = request.registrationWrapper.etmpDisplayRegistration.otherAddress.isEmpty
    val isOtherAddressInNi = request.registrationWrapper.etmpDisplayRegistration.otherAddress.exists(_.postcode.exists(_.toUpperCase.startsWith(niPostCodeAreaPrefix)))

    val globalAddressDefined = request.userAnswers.get(GlobalAddressPage).isDefined
    
    val isExcluded = request.registrationWrapper.etmpDisplayRegistration.exclusions.exists(maybeExclusion => maybeExclusion.exclusionReason != Reversal)

    determineResult(
      inRejoin,
      isExcluded,
      niBusinessAddressDefined,
      niAddress,
      vatInfoPostcodeInNi,
      isOtherAddressInNi,
      isOtherAddressEmpty,
      globalAddressDefined
    )
  }

  private def determineResult(
                               inRejoin: Boolean,
                               isExcluded: Boolean,
                               niBusinessAddressDefined: Boolean,
                               niAddress: Boolean,
                               vatInfoPostcodeInNi: Boolean,
                               isOtherAddressInNi: Boolean,
                               isOtherAddressEmpty: Boolean,
                               globalAddressDefined: Boolean
                             ): Future[Option[Result]] = {

    val waypoints: Waypoints = if (inRejoin) {
      EmptyWaypoints.setNextWaypoint(Waypoint(RejoinSchemePage, CheckMode, RejoinSchemePage.urlFragment))
    } else {
      EmptyWaypoints.setNextWaypoint(Waypoint(ChangeRegistrationPage, CheckMode, ChangeRegistrationPage.urlFragment))
    }

    val redirect: Future[Some[Result]] = Some(Redirect(controllers.routes.BusinessBasedInNiController.onPageLoad(waypoints).url)).toFuture

    if (!niBusinessAddressDefined && !globalAddressDefined && !vatInfoPostcodeInNi || (inRejoin && globalAddressDefined)) {
      redirect
    } else if (isExcluded || (niBusinessAddressDefined && niAddress) || (vatInfoPostcodeInNi && (isOtherAddressInNi || isOtherAddressEmpty))) {
      None.toFuture
    } else {
      None.toFuture
    }
  }
}

class CheckNiBasedAddressFilterProvider @Inject()()(implicit ec: ExecutionContext) {

  def apply(inRejoin: Boolean = false): CheckNiBasedAddressFilterImpl = new CheckNiBasedAddressFilterImpl(inRejoin)
}
