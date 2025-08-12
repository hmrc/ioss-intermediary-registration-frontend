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

package controllers.amend

import controllers.actions.*
import models.domain.VatCustomerInfo
import models.{BankDetails, Bic, ContactDetails, DesAddress, Iban, UserAnswers}
import pages.amend.ChangeRegistrationPage
import pages.euDetails.HasFixedEstablishmentPage
import pages.filters.RegisteredForIossIntermediaryInEuPage
import pages.previousIntermediaryRegistrations.HasPreviouslyRegisteredAsIntermediaryPage
import pages.tradingNames.HasTradingNamePage
import pages.{BankDetailsPage, ContactDetailsPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.{Instant, LocalDate}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StartAmendJourneyController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             cc: AuthenticatedControllerComponents,
                                             val controllerComponents: MessagesControllerComponents
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetData(inAmend = true).async {

    implicit request =>
      val iban: Iban = Iban("GB33BUKB202015555555555").toOption.get
      val bic: Bic = Bic("BARCGB22456").get

      val vatCustomerInfo: VatCustomerInfo =
        VatCustomerInfo(
          registrationDate = LocalDate.now(),
          desAddress = DesAddress(
            line1 = "1818 East Tusculum Street",
            line2 = Some("Phil Tow"),
            line3 = None, line4 = None, line5 = None,
            postCode = Some("BT4 2XW"),
            countryCode = "EL"),
          organisationName = Some("Company name"),
          individualName = None,
          singleMarketIndicator = true,
          deregistrationDecisionDate = None
        )

      def basicUserAnswersWithVatInfo: UserAnswers =
        UserAnswers(id = request.userId, vatInfo = Some(vatCustomerInfo), lastUpdated = Instant.now())

      def completeUserAnswersWithVatInfo: UserAnswers = {
        basicUserAnswersWithVatInfo
          .set(RegisteredForIossIntermediaryInEuPage, false).get
          .set(HasTradingNamePage, false).get
          .set(HasPreviouslyRegisteredAsIntermediaryPage, false).get
          .set(HasFixedEstablishmentPage, false).get
          .set(ContactDetailsPage, ContactDetails("Rocky Balboa", "028 123 4567", "rocky.balboa@chartoffwinkler.co.uk")).get
          .set(BankDetailsPage, BankDetails("Chartoff Winkler and Co.", Some(bic), iban)).get
      }

      for {
        _           <- cc.sessionRepository.set(completeUserAnswersWithVatInfo)
      } yield Redirect(ChangeRegistrationPage.route(waypoints).url)
  }
}
