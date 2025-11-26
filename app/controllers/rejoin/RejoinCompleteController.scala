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

import config.FrontendAppConfig
import controllers.actions.AuthenticatedControllerComponents
import models.UserAnswers
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.rejoin.NewIossReferenceQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.rejoin.RejoinCompleteView
import utils.FutureSyntax.FutureOps

import javax.inject.Inject

class RejoinCompleteController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        cc: AuthenticatedControllerComponents,
                                        frontendAppConfig: FrontendAppConfig,
                                        view: RejoinCompleteView
                                        ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc
  
  def onPageLoad(): Action[AnyContent] = cc.authAndRequireIntermediaryAndCheckNiAddress(inAmend = false, inRejoin = true).async {
    implicit request =>
      val newIossReference = getNewIossReference(request.userAnswers)

      Ok(view(newIossReference, frontendAppConfig.intermediaryYourAccountUrl)).toFuture
  }

  private def getNewIossReference(answers: UserAnswers) = {
    answers.get(NewIossReferenceQuery).getOrElse(throw new RuntimeException("NewIossReference has not been set in answers"))
  }
}
