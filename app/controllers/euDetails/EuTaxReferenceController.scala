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
import forms.euDetails.EuTaxReferenceFormProvider
import models.Index
import models.requests.AuthenticatedDataRequest
import pages.Waypoints
import pages.euDetails.EuTaxReferencePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.AmendWaypoints.AmendWaypointsOps
import utils.FutureSyntax.FutureOps
import views.html.euDetails.EuTaxReferenceView
import services.core.CoreRegistrationValidationService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EuTaxReferenceController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          cc: AuthenticatedControllerComponents,
                                          formProvider: EuTaxReferenceFormProvider,
                                          view: EuTaxReferenceView,
                                          coreRegistrationValidationService: CoreRegistrationValidationService
                                        )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with GetCountry {

  protected val controllerComponents: MessagesControllerComponents = cc


  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
    implicit request =>

      getCountry(waypoints, countryIndex) { country =>

        val form = formProvider(country)

        val preparedForm = request.userAnswers.get(EuTaxReferencePage(countryIndex)) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        Ok(view(preparedForm, waypoints, countryIndex, country)).toFuture
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
    implicit request =>

      getCountry(waypoints, countryIndex) { country =>

        val form = formProvider(country)

        form.bindFromRequest().fold(
          formWithErrors =>
            BadRequest(view(formWithErrors, waypoints, countryIndex, country)).toFuture,

          euTaxRef =>
            coreRegistrationValidationService.searchEuTaxId(euTaxRef, country.code).flatMap {
              case _ if waypoints.inAmend =>
                updateAnswersAndRedirect(waypoints, countryIndex, request, euTaxRef)

              case Some(activeMatch) if activeMatch.traderId.isAnIntermediary && activeMatch.isActiveTrader =>
                Future.successful(
                  Redirect(
                    controllers.filters.routes.SchemeStillActiveController.onPageLoad(
                      activeMatch.memberState
                    )
                  )
                )

              case Some(activeMatch) if activeMatch.traderId.isAnIntermediary && activeMatch.isQuarantinedTrader =>
                Future.successful(
                  Redirect(
                    controllers.filters.routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
                      activeMatch.memberState,
                      activeMatch.getEffectiveDate
                    )
                  )
                )

              case _ =>
                updateAnswersAndRedirect(waypoints, countryIndex, request, euTaxRef)
            }
        )
      }
  }

  private def updateAnswersAndRedirect(waypoints: Waypoints, countryIndex: Index, request: AuthenticatedDataRequest[AnyContent], euTaxRef: String) = {
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(EuTaxReferencePage(countryIndex), euTaxRef))
      _ <- cc.sessionRepository.set(updatedAnswers)
    } yield Redirect(EuTaxReferencePage(countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
  }
}
