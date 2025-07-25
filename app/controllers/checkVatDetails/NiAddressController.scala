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
import pages.{CannotRegisterNotNiBasedBusinessPage, Waypoints}
import pages.checkVatDetails.NiAddressPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
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

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData() {
    implicit request =>

      val form: Form[UkAddress] = formProvider()
      val preparedForm = request.userAnswers.get(NiAddressPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>

      val form: Form[UkAddress] = formProvider()
      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints)).toFuture,

        value =>
          if (value.postCode.toUpperCase.startsWith(niPostCodeAreaPrefix)) {
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(NiAddressPage, value))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield Redirect(NiAddressPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
          } else {
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.remove(NiAddressPage))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield Redirect(CannotRegisterNotNiBasedBusinessPage.route(waypoints).url)
          }
      )
  }
}
