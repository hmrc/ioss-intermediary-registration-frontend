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

package controllers.checkVatDetails

import config.Constants.niPostCodeAreaPrefix
import controllers.actions.*
import forms.NiAddressFormProvider
import models.UkAddress
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.Reversal
import models.requests.AuthenticatedDataRequest
import pages.amend.HasBusinessAddressInNiPage
import pages.checkVatDetails.NiAddressPage
import pages.{CannotRegisterNotNiBasedBusinessPage, Page, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.CheckNiBased.isNiBasedIntermediary
import utils.FutureSyntax.FutureOps
import views.html.checkVatDetails.NiAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NiAddressController @Inject()(
                                     override val messagesApi: MessagesApi,
                                     cc: AuthenticatedControllerComponents,
                                     formProvider: NiAddressFormProvider,
                                     view: NiAddressView
                                   )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend, waypoints.inRejoin) {
    implicit request =>

      val maybeEtmpExclusion: Option[EtmpExclusion] = request.registrationWrapper.flatMap { maybeRegistrationWrapper =>
        maybeRegistrationWrapper.etmpDisplayRegistration.exclusions.lastOption.flatMap { etmpExclusion =>
          etmpExclusion.exclusionReason match {
            case Reversal => None
            case _ => Some(etmpExclusion)
          }
        }
      }

      val isExcluded: Boolean = maybeEtmpExclusion.isDefined

      val form: Form[UkAddress] = formProvider()
      val preparedForm = request.userAnswers.get(NiAddressPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }


      val isNiBasedAddress: Boolean = request.userAnswers.vatInfo.exists(isNiBasedIntermediary)
      val formIsEmpty: Boolean = preparedForm.value.isEmpty

      val showNiAddressText: Boolean = (isNiBasedAddress, formIsEmpty) match {
        case (false, true) => true
        case (_, _) => false
      }

      Ok(view(preparedForm, waypoints, showNiAddressText, isExcluded))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend, waypoints.inRejoin).async {
    implicit request =>

      val maybeEtmpExclusion: Option[EtmpExclusion] = request.registrationWrapper.flatMap { maybeRegistrationWrapper =>
        maybeRegistrationWrapper.etmpDisplayRegistration.exclusions.lastOption.flatMap { etmpExclusion =>
          etmpExclusion.exclusionReason match {
            case Reversal => None
            case _ => Some(etmpExclusion)
          }
        }
      }

      val isExcluded: Boolean = maybeEtmpExclusion.isDefined

      val form: Form[UkAddress] = formProvider()
      form.bindFromRequest().fold(
        formWithErrors =>

          BadRequest(view(formWithErrors, waypoints, showNiAddressText = false, isExcluded)).toFuture,

        value =>

          determineRedirectAndSaveAnswers(
            waypoints = waypoints,
            value = value,
            isInAmend = waypoints.inAmend,
            isExcluded = isExcluded
          )
      )
  }

  private def determineRedirectAndSaveAnswers(
                                               waypoints: Waypoints,
                                               value: UkAddress,
                                               isInAmend: Boolean,
                                               isExcluded: Boolean
                                             )(implicit request: AuthenticatedDataRequest[_]): Future[Result] = {

    val hasNiPrefix: Boolean = value.postCode.toUpperCase.startsWith(niPostCodeAreaPrefix)
    val redirectPage: Page = (hasNiPrefix, isInAmend, isExcluded) match {
      case (true, _, _) => NiAddressPage
      case (false, true, true) => NiAddressPage
      case (false, true, _) => HasBusinessAddressInNiPage
      case (_, _, _) => CannotRegisterNotNiBasedBusinessPage
    }

    for {
      updatedAnswers <- Future.fromTry {
        if (redirectPage == CannotRegisterNotNiBasedBusinessPage) {
          request.userAnswers.remove(NiAddressPage)
        } else {
          request.userAnswers.set(NiAddressPage, value)
        }
      }
      _ <- cc.sessionRepository.set(updatedAnswers)
    } yield {
      if (redirectPage != NiAddressPage) {
        Redirect(redirectPage.route(waypoints).url)
      } else {
        Redirect(redirectPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
      }
    }
  }
}
