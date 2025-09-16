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

package controllers.previousIntermediaryRegistrations

import controllers.GetCountry
import controllers.actions.*
import forms.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationNumberFormProvider
import models.core.Match
import models.previousIntermediaryRegistrations.{IntermediaryIdentificationNumberValidation, NonCompliantDetails}
import models.requests.AuthenticatedDataRequest
import models.{Index, UserAnswers}
import pages.Waypoints
import pages.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationNumberPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.previousIntermediaryRegistrations.NonCompliantDetailsQuery
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationNumberView
import utils.AmendWaypoints.AmendWaypointsOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreviousIntermediaryRegistrationNumberController @Inject()(
                                                                  override val messagesApi: MessagesApi,
                                                                  cc: AuthenticatedControllerComponents,
                                                                  formProvider: PreviousIntermediaryRegistrationNumberFormProvider,
                                                                  coreRegistrationValidationService: CoreRegistrationValidationService,
                                                                  view: PreviousIntermediaryRegistrationNumberView
                                                                )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with GetCountry {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
    implicit request =>
      getPreviousCountry(waypoints, countryIndex) { country =>

        val hintText: String = getIntermediaryHintText(country.code)

        val form: Form[String] = formProvider(country)

        val preparedForm = request.userAnswers.get(PreviousIntermediaryRegistrationNumberPage(countryIndex)) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        Ok(view(preparedForm, waypoints, countryIndex, country, hintText)).toFuture
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndGetData(waypoints.inAmend).async {
    implicit request =>
      getPreviousCountry(waypoints, countryIndex) { country =>

        val hintText: String = getIntermediaryHintText(country.code)

        val form: Form[String] = formProvider(country)

        form.bindFromRequest().fold(
          formWithErrors =>
            BadRequest(view(formWithErrors, waypoints, countryIndex, country, hintText)).toFuture,

          previousSchemeNumber =>
            coreRegistrationValidationService.searchScheme(
              traderID = previousSchemeNumber,
              countryCode = country.code
            ).flatMap {
              case Some(activeMatch) if activeMatch.traderId.isAnIntermediary && activeMatch.matchType.isActiveTrader =>
                Redirect(controllers.filters.routes.SchemeStillActiveController.onPageLoad(
                  activeMatch.memberState)
                ).toFuture

              case Some(activeMatch) if activeMatch.traderId.isAnIntermediary && activeMatch.matchType.isQuarantinedTrader =>
                Redirect(controllers.filters.routes.OtherCountryExcludedAndQuarantinedController.onPageLoad(
                  activeMatch.memberState,
                  activeMatch.getEffectiveDate)
                ).toFuture

              case Some(activeMatch) =>
                saveAndRedirect(
                  waypoints,
                  countryIndex,
                  previousSchemeNumber,
                  Some(NonCompliantDetails(nonCompliantReturns = activeMatch.nonCompliantReturns, nonCompliantPayments = activeMatch.nonCompliantPayments))
                )

              case _ =>
                saveAndRedirect(waypoints, countryIndex, previousSchemeNumber, maybeNonCompliantDetails = None)
            }
        )
      }
  }

  private def saveAndRedirect(
                               waypoints: Waypoints,
                               countryIndex: Index,
                               previousSchemeNumber: String,
                               maybeNonCompliantDetails: Option[NonCompliantDetails]
                             )(implicit request: AuthenticatedDataRequest[AnyContent]): Future[Result] = {
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(PreviousIntermediaryRegistrationNumberPage(countryIndex), previousSchemeNumber))
      updatedAnswersWithNonCompliantDetails <- setNonCompliantDetailsAnswers(countryIndex, maybeNonCompliantDetails, updatedAnswers)
      _ <- cc.sessionRepository.set(updatedAnswersWithNonCompliantDetails)
    } yield {
      Redirect(PreviousIntermediaryRegistrationNumberPage(countryIndex).navigate(waypoints, request.userAnswers, updatedAnswersWithNonCompliantDetails).route)
    }
  }

  private def setNonCompliantDetailsAnswers(
                                             countryIndex: Index,
                                             maybeNonCompliantDetails: Option[NonCompliantDetails],
                                             updatedAnswers: UserAnswers
                                           ): Future[UserAnswers] = {
    maybeNonCompliantDetails match {
      case Some(nonCompliantDetails) =>
        Future.fromTry(updatedAnswers.set(NonCompliantDetailsQuery(countryIndex), nonCompliantDetails))

      case _ =>
        updatedAnswers.toFuture
    }
  }


  private def getIntermediaryHintText(countryCode: String): String = {
    IntermediaryIdentificationNumberValidation.euCountriesWithIntermediaryValidationRules
      .find(_.country.code == countryCode).head.messageInput
  }
}
