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

package controllers.tradingNames

import config.Constants.maxTradingNames
import controllers.actions.*
import forms.tradingNames.AddTradingNameFormProvider
import pages.Waypoints
import pages.tradingNames.AddTradingNamePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.tradingNames.DeriveNumberOfTradingNames
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps
import utils.ItemsHelper.getDerivedItems
import viewmodels.checkAnswers.tradingNames.TradingNameSummary
import views.html.tradingNames.AddTradingNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddTradingNameController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          cc: AuthenticatedControllerComponents,
                                          formProvider: AddTradingNameFormProvider,
                                          view: AddTradingNameView
                                        )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc
  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] =
    cc.authAndGetData(waypoints.inAmend, waypoints.inRejoin, restrictExcludedAmend = true).async {
      implicit request =>

        // TODO -> Take into consideration number of TN in user answers and additional trading names from OSS and IOSS x-schema if they exist.
        // TODO -> So total 10 allowed but calculated from getDerivedItems, may need to gather number of IOSS/OSS account trading names and add to total number
        // TODO -> otherwise number < maxTradingNames will be incorrect
        // TODO -> Check with Susan regarding how many trading names can be submitted in other services with composite account implemented

        val compositeAccountTradingNamesSize: Int = request.compositeAccount.map(_.tradingNames.size).getOrElse(0)

        getDerivedItems(waypoints, DeriveNumberOfTradingNames, compositeAccountTradingNamesSize) { number =>

          val preparedForm = request.userAnswers.get(AddTradingNamePage()) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          val canAddTradingNames = number < maxTradingNames
          val tradingNamesSummary = TradingNameSummary.addToListRows(waypoints, request.userAnswers, AddTradingNamePage())
          val numberOfIossRegistrations = request.numberOfIossRegistrations // TODO -> What is this used for in this service? Remove??

          Ok(view(preparedForm, waypoints, tradingNamesSummary, canAddTradingNames, numberOfIossRegistrations, request.compositeAccount)).toFuture
        }
    }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] =
    cc.authAndGetData(waypoints.inAmend, waypoints.inRejoin, restrictExcludedAmend = true).async {
      implicit request =>

        val compositeAccountTradingNamesSize: Int = request.compositeAccount.map(_.tradingNames.size).getOrElse(0)

        getDerivedItems(waypoints, DeriveNumberOfTradingNames, compositeAccountTradingNamesSize) { number =>

          val canAddTradingNames = number < maxTradingNames
          val tradingNamesSummary = TradingNameSummary.addToListRows(waypoints, request.userAnswers, AddTradingNamePage())
          val numberOfIossRegistrations = request.numberOfIossRegistrations

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(
                formWithErrors,
                waypoints,
                tradingNamesSummary,
                canAddTradingNames,
                numberOfIossRegistrations,
                request.compositeAccount
              )).toFuture,

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(AddTradingNamePage(), value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(AddTradingNamePage().navigate(waypoints, request.userAnswers, updatedAnswers).route)
          )
        }
    }
}
