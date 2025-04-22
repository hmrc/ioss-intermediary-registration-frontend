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

package controllers.auth

import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.actions.AuthenticatedControllerComponents
import models.UserAnswers
import models.checkVatDetails.VatApiCallResult
import pages.EmptyWaypoints
import pages.checkVatDetails.{CheckVatDetailsPage, VatApiDownPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.VatApiCallResultQuery
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, OnlyRelative, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.auth.{InsufficientEnrolmentsView, UnsupportedAffinityGroupView, UnsupportedAuthProviderView, UnsupportedCredentialRoleView}

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class AuthController @Inject()(
                                cc: AuthenticatedControllerComponents,
                                frontendAppConfig: FrontendAppConfig,
                                registrationConnector: RegistrationConnector,
                                insufficientEnrolmentsView: InsufficientEnrolmentsView,
                                unsupportedAffinityGroupView: UnsupportedAffinityGroupView,
                                unsupportedAuthProviderView: UnsupportedAuthProviderView,
                                unsupportedCredentialRoleView: UnsupportedCredentialRoleView,
                                clock: Clock
                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  private val redirectPolicy = OnlyRelative | AbsoluteWithHostnameFromAllowlist(frontendAppConfig.allowedRedirectUrls: _*)

  def redirectToRegister(continueUrl: RedirectUrl): Action[AnyContent] = Action {
    Redirect(
      frontendAppConfig.registerUrl,
      Map(
        "origin" -> Seq(frontendAppConfig.origin),
        "continueUrl" -> Seq(continueUrl.get(redirectPolicy).url),
        "accountType" -> Seq("Organisation"))
    )
  }

  def redirectToLogin(continueUrl: RedirectUrl): Action[AnyContent] = Action {
    Redirect(frontendAppConfig.loginUrl,
      Map(
        "origin" -> Seq(frontendAppConfig.origin),
        "continue" -> Seq(continueUrl.get(redirectPolicy).url)
      )
    )
  }

  def onSignIn(): Action[AnyContent] = cc.authAndGetOptionalData().async {
    implicit request =>
      val answers: UserAnswers = request.userAnswers.getOrElse(UserAnswers(request.userId, lastUpdated = Instant.now(clock)))
      answers.get(VatApiCallResultQuery) match {
        case Some(vatApiCallResult) if vatApiCallResult == VatApiCallResult.Success =>
          Redirect(CheckVatDetailsPage.route(EmptyWaypoints).url).toFuture

        case _ =>
          registrationConnector.getVatCustomerInfo().flatMap {
            case Right(vatInfo) =>
              for {
                updatedAnswers <- Future.fromTry(answers.copy(vatInfo = Some(vatInfo)).set(VatApiCallResultQuery, VatApiCallResult.Success))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(CheckVatDetailsPage.route(EmptyWaypoints).url)

            case _ =>
              for {
                updatedAnswers <- Future.fromTry(answers.set(VatApiCallResultQuery, VatApiCallResult.Error))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(VatApiDownPage.route(EmptyWaypoints).url)
          }
      }
  }

  def signOut(): Action[AnyContent] = Action {
    _ =>
      Redirect(frontendAppConfig.signOutUrl, Map("continue" -> Seq(frontendAppConfig.exitSurveyUrl)))
  }

  def signOutNoSurvey(): Action[AnyContent] = Action {
    _ =>
      Redirect(frontendAppConfig.signOutUrl, Map("continue" -> Seq(routes.SignedOutController.onPageLoad().url)))
  }

  def unsupportedAffinityGroup(): Action[AnyContent] = Action {
    implicit request =>
      Ok(unsupportedAffinityGroupView())
  }

  def unsupportedAuthProvider(continueUrl: RedirectUrl): Action[AnyContent] = Action {
    implicit request =>
      Ok(unsupportedAuthProviderView(continueUrl))
  }

  def insufficientEnrolments(): Action[AnyContent] = Action {
    implicit request =>
      Ok(insufficientEnrolmentsView())
  }

  def unsupportedCredentialRole(): Action[AnyContent] = Action {
    implicit request =>
      Ok(unsupportedCredentialRoleView())
  }
}
