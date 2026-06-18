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

package controllers.actions

import config.FrontendAppConfig
import connectors.RegistrationConnector
import controllers.actions.FakeCheckRegistrationFilter.{mockFrontendAppConfig, mockRegistrationConnector}
import models.requests.AuthenticatedIdentifierRequest
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Result
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FakeCheckRegistrationFilter extends CheckRegistrationFilterImpl(
  inAmend = false,
  inRejoin = false,
  restrictExcludedAmend = false,
  restrictNiVatBusinessAddress = false,
  mockFrontendAppConfig,
  mockRegistrationConnector
) {

  override protected def filter[A](request: AuthenticatedIdentifierRequest[A]): Future[Option[Result]] =
    None.toFuture
}

class FakeCheckRegistrationFilterProvider extends CheckRegistrationFilterProvider(mockFrontendAppConfig, mockRegistrationConnector) {

  override def apply(inAmend: Boolean, inRejoin: Boolean, restrictExcludedAmend: Boolean, restrictNiVatBusinessAddress: Boolean): CheckRegistrationFilterImpl = new FakeCheckRegistrationFilter()
}

object FakeCheckRegistrationFilter {

  val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
}