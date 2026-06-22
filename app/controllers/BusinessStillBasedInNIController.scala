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
import controllers.actions.*
import forms.BusinessStillBasedInNIFormProvider
import pages.checkVatDetails.NiAddressPage
import pages.{BusinessStillBasedInNIPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import views.html.BusinessStillBasedInNIView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessStillBasedInNIController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: BusinessStillBasedInNIFormProvider,
                                         view: BusinessStillBasedInNIView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {
  
  protected val controllerComponents: MessagesControllerComponents = cc

  private val form = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend, waypoints.inRejoin, restrictExcludedAmend = true, restrictNiVatBusinessAddress = true) {
    implicit request =>

      val stillBasedInNi: Boolean = request.userAnswers.get(NiAddressPage).exists(_.postCode.toUpperCase.startsWith(niPostCodeAreaPrefix))
      
      val preparedForm = request.userAnswers.get(BusinessStillBasedInNIPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints, stillBasedInNi))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend, waypoints.inRejoin, restrictExcludedAmend = true, restrictNiVatBusinessAddress = true).async {
    implicit request =>

      val stillBasedInNi: Boolean = request.userAnswers.get(NiAddressPage).exists(_.postCode.toUpperCase.startsWith(niPostCodeAreaPrefix))

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, waypoints, stillBasedInNi))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(BusinessStillBasedInNIPage, value))
            _              <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(BusinessStillBasedInNIPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }
}
