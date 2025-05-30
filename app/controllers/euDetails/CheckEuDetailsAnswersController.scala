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

package controllers.euDetails

import controllers.GetCountry
import controllers.actions.*
import models.Index
import models.euDetails.EuDetails
import pages.euDetails.{CheckEuDetailsAnswersPage, EuCountryPage}
import pages.{Waypoint, Waypoints}  
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import utils.EuDetailsCompletionChecks.{getIncompleteEuDetails, incompleteEuDetailsRedirect}
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.euDetails.*
import viewmodels.govuk.all.SummaryListViewModel
import views.html.euDetails.CheckEuDetailsAnswersView

import javax.inject.Inject

class CheckEuDetailsAnswersController @Inject()(
                                                 override val messagesApi: MessagesApi,
                                                 cc: AuthenticatedControllerComponents,
                                                 view: CheckEuDetailsAnswersView
                                               ) extends FrontendBaseController with I18nSupport with GetCountry with CompletionChecks {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>

      getCountry(waypoints, countryIndex) { country =>

        val sourcePage: CheckEuDetailsAnswersPage = CheckEuDetailsAnswersPage(countryIndex)

        val summaryList: SummaryList = SummaryListViewModel(
          rows = Seq(
            HasFixedEstablishmentSummary.row(waypoints, request.userAnswers, countryIndex, country, sourcePage),
            RegistrationTypeSummary.row(waypoints, request.userAnswers, countryIndex, sourcePage),
            EuVatNumberSummary.row(waypoints, request.userAnswers, countryIndex, sourcePage),
            EuTaxReferenceSummary.row(waypoints, request.userAnswers, countryIndex, sourcePage),
            FixedEstablishmentTradingNameSummary.row(waypoints, request.userAnswers, countryIndex, sourcePage),
            FixedEstablishmentAddressSummary.row(waypoints, request.userAnswers, countryIndex, sourcePage)
          ).flatten
        )

        withCompleteDataModel[EuDetails](
          index = countryIndex,
          data = getIncompleteEuDetails _,
          onFailure = (incomplete: Option[EuDetails]) => {
            Ok(view(waypoints, countryIndex, country, summaryList, incomplete.isDefined))
          }) {
          Ok(view(waypoints, countryIndex, country, summaryList))
        }.toFuture
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index, incompletePromptShown: Boolean): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>

      withCompleteDataModel[EuDetails](
        index = countryIndex,
        data = getIncompleteEuDetails _,
        onFailure = (_: Option[EuDetails]) => {
          if (!incompletePromptShown) {
            Redirect(CheckEuDetailsAnswersPage(countryIndex).route(waypoints).url)
          } else {
            val updatedWaypoints: Waypoints = waypoints
              .setNextWaypoint(Waypoint(CheckEuDetailsAnswersPage(countryIndex), waypoints.currentMode, CheckEuDetailsAnswersPage(countryIndex).urlFragment))
            incompleteEuDetailsRedirect(updatedWaypoints) match {
              case Some(redirectResult) => redirectResult
              case _ => Redirect(EuCountryPage(countryIndex).route(updatedWaypoints).url)
            }
          }
        }) {
        Redirect(CheckEuDetailsAnswersPage(countryIndex).navigate(waypoints, request.userAnswers, request.userAnswers).route)
      }.toFuture
  }
}
