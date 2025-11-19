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

package controllers.rejoin

import controllers.actions.*
import models.Country
import models.etmp.display.EtmpDisplayEuRegistrationDetails
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.rejoin.CannotRejoinVatNumberAlreadyRegisteredView

import javax.inject.Inject

class CannotRejoinVatNumberAlreadyRegisteredController @Inject()(
                                                                  override val messagesApi: MessagesApi,
                                                                  cc: AuthenticatedControllerComponents,
                                                                  view: CannotRejoinVatNumberAlreadyRegisteredView
                                                                ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(countryCode: String): Action[AnyContent] = cc.authAndRequireIntermediaryCoreValidationInfraction(inRejoin = true) {
    implicit request =>

      val countryName: String = Country.fromCountryCodeUnsafe(countryCode).name

      val etmpDisplayEuRegistrationDetails: Seq[EtmpDisplayEuRegistrationDetails] = request.registrationWrapper
        .etmpDisplayRegistration.schemeDetails.euRegistrationDetails

      val isVatNumber: Boolean = etmpDisplayEuRegistrationDetails
        .find(_.issuedBy == countryCode).exists(_.vatNumber.isDefined)

      val isTaxId: Boolean = etmpDisplayEuRegistrationDetails
        .find(_.issuedBy == countryCode).exists(_.taxIdentificationNumber.isDefined)

      Ok(view(countryName, isVatNumber, isTaxId))
  }
}
