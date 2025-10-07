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
import forms.euDetails.EuVatNumberFormProvider
import models.{CountryWithValidationDetails, Index}
import models.requests.AuthenticatedDataRequest
import pages.Waypoints
import pages.euDetails.EuVatNumberPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps
import views.html.euDetails.EuVatNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import services.core.CoreRegistrationValidationService

import java.time.Clock

class EuVatNumberController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: EuVatNumberFormProvider,
                                       view: EuVatNumberView,
                                       coreRegistrationValidationService: CoreRegistrationValidationService,
                                       clock: Clock
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetCountry {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
    implicit request =>

      getCountry(waypoints, countryIndex) { country =>

        val form: Form[String] = formProvider(country)

        val preparedForm = request.userAnswers.get(EuVatNumberPage(countryIndex)) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        CountryWithValidationDetails.euCountriesWithVRNValidationRules.filter(_.country.code == country.code).head match {
          case countryWithValidationDetails: CountryWithValidationDetails =>

            Ok(view(preparedForm, waypoints, countryIndex, countryWithValidationDetails)).toFuture
        }
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
    implicit request =>

      getCountry(waypoints, countryIndex) { country =>

        CountryWithValidationDetails.euCountriesWithVRNValidationRules.filter(_.country.code == country.code).head match {
          case countryWithValidationDetails: CountryWithValidationDetails =>

            val form: Form[String] = formProvider(country)

            form.bindFromRequest().fold(
              formWithErrors =>
                BadRequest(view(formWithErrors, waypoints, countryIndex, countryWithValidationDetails)).toFuture,

              euVrn =>
                coreRegistrationValidationService.searchEuVrn(euVrn, country.code).flatMap {

                  case _ if waypoints.inAmend =>
                    saveAndRedirect(waypoints, countryIndex, euVrn)

                  case Some(activeMatch) if activeMatch.isActiveTrader =>
                    Future.successful(
                      Redirect(
                        controllers.filters.routes.SchemeStillActiveController.onPageLoad(activeMatch.memberState)
                      )
                    )

                  case Some(activeMatch) if activeMatch.isQuarantinedTrader(clock) =>
                    Future.successful(Redirect(controllers.filters.routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
                      activeMatch.memberState,
                      activeMatch.getEffectiveDate
                    )))

                  case _ =>
                    saveAndRedirect(waypoints, countryIndex, euVrn)
                }
            )
        }
      }
  }

  private def saveAndRedirect(waypoints: Waypoints, countryIndex: Index, euVrn: String)(implicit request: AuthenticatedDataRequest[_]) = {
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(EuVatNumberPage(countryIndex), euVrn))
      _ <- cc.sessionRepository.set(updatedAnswers)
    } yield Redirect(EuVatNumberPage(countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
  }
}
