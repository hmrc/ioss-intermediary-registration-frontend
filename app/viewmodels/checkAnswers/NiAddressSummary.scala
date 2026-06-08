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

package viewmodels.checkAnswers

import models.UserAnswers
import pages.amend.ChangePreviousRegistrationPage
import pages.checkVatDetails.NiAddressPage
import pages.{BusinessStillBasedInNIPage, CheckAnswersPage, Waypoints}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object NiAddressSummary {

  def row(
           waypoints: Waypoints,
           answers: UserAnswers,
           checkOtherAddressNi: Boolean,
           sourcePage: CheckAnswersPage
         )(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(NiAddressPage).map { answer =>

      val value = Seq(
        Some(HtmlFormat.escape(answer.line1).toString),
        answer.line2.map(HtmlFormat.escape),
        Some(HtmlFormat.escape(answer.townOrCity).toString),
        answer.county.map(HtmlFormat.escape),
        Some(HtmlFormat.escape(answer.postCode).toString)
      ).flatten.mkString("<br/>")
      
      val checkYourAnswersLabel = if (checkOtherAddressNi) {
        "niAddress.checkYourAnswersLabel"
      } else {
        "niAddress.checkYourAnswersLabel.nonNi"
      }

      val changeHiddenLabel = if (checkOtherAddressNi) {
        "niAddress.change.hidden"
      } else {
        "niAddress.change.hidden.nonNi"
      }

      SummaryListRowViewModel(
        key = checkYourAnswersLabel,
        value = ValueViewModel(HtmlContent(value)),
        actions =
          if (sourcePage.isInstanceOf[ChangePreviousRegistrationPage.type]) {
            Nil
          } else {
            // Redirect to page below when yes is selected on stillBasedInNI page
            // TODO: NiAddressPage.changeLink(waypoints, sourcePage).url)
            Seq(
              ActionItemViewModel("site.change", BusinessStillBasedInNIPage.changeLink(waypoints, sourcePage).url)
                .withVisuallyHiddenText(messages(changeHiddenLabel))
            )
          }
      )
    }
  }

  def amendedRow(answers: UserAnswers, checkOtherAddressNi: Boolean)(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(NiAddressPage).map { answer =>

      val value = Seq(
        Some(HtmlFormat.escape(answer.line1).toString),
        answer.line2.map(HtmlFormat.escape),
        Some(HtmlFormat.escape(answer.townOrCity).toString),
        answer.county.map(HtmlFormat.escape),
        Some(HtmlFormat.escape(answer.postCode).toString)
      ).flatten.mkString("<br/>")

      val messageKey: String = if (checkOtherAddressNi) {
        "niAddress.changed"
      } else {
        "niAddress.changed.withoutNi"
      }
      
      SummaryListRowViewModel(
        key = KeyViewModel(messageKey),
        value = ValueViewModel(HtmlContent(value))
      )
    }
  }
}