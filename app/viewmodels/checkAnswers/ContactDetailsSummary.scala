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
import pages.{CheckAnswersPage, ContactDetailsPage, Waypoints}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object ContactDetailsSummary {

  def rowContactName(waypoints: Waypoints, answers: UserAnswers, sourcePage: CheckAnswersPage)(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(ContactDetailsPage).map { answer =>

      val value = HtmlFormat.escape(answer.fullName).toString

      SummaryListRowViewModel(
        key = "contactDetails.fullName",
        value = ValueViewModel(HtmlContent(value)),
        actions = Seq(
          ActionItemViewModel("site.change", ContactDetailsPage.changeLink(waypoints, sourcePage).url)
            .withVisuallyHiddenText(messages("contactDetails.change.fullName.hidden"))
        )
      )
    }
  }

  def rowTelephoneNumber(waypoints: Waypoints, answers: UserAnswers, sourcePage: CheckAnswersPage)(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(ContactDetailsPage).map { answer =>

      val value = HtmlFormat.escape(answer.telephoneNumber).toString

      SummaryListRowViewModel(
        key = "contactDetails.telephoneNumber",
        value = ValueViewModel(HtmlContent(value)),
        actions = Seq(
          ActionItemViewModel("site.change", ContactDetailsPage.changeLink(waypoints, sourcePage).url)
            .withVisuallyHiddenText(messages("contactDetails.change.telephoneNumber.hidden"))
        )
      )
    }
  }

  def rowEmailAddress(waypoints: Waypoints, answers: UserAnswers, sourcePage: CheckAnswersPage)(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(ContactDetailsPage).map { answer =>

      val value = HtmlFormat.escape(answer.emailAddress).toString

      SummaryListRowViewModel(
        key = "contactDetails.emailAddress",
        value = ValueViewModel(HtmlContent(value)),
        actions = Seq(
          ActionItemViewModel("site.change", ContactDetailsPage.changeLink(waypoints, sourcePage).url)
            .withVisuallyHiddenText(messages("contactDetails.change.emailAddress.hidden"))
        )
      )
    }
  }

  def amendedRowContactName(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(ContactDetailsPage).map { answer =>

      val value = HtmlFormat.escape(answer.fullName).toString

      SummaryListRowViewModel(
        key = KeyViewModel("contactDetails.fullName").withCssClass("govuk-!-width-one-half"),
        value = ValueViewModel(HtmlContent(value))
      )
    }
  }

  def amendedRowTelephoneNumber(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(ContactDetailsPage).map { answer =>

      val value = HtmlFormat.escape(answer.telephoneNumber).toString

      SummaryListRowViewModel(
        key = KeyViewModel("contactDetails.telephoneNumber").withCssClass("govuk-!-width-one-half"),
        value = ValueViewModel(HtmlContent(value))
      )
    }
  }

  def amendedRowEmailAddress(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(ContactDetailsPage).map { answer =>

      val value = HtmlFormat.escape(answer.emailAddress).toString

      SummaryListRowViewModel(
        key = KeyViewModel("contactDetails.emailAddress").withCssClass("govuk-!-width-one-half"),
        value = ValueViewModel(HtmlContent(value))
      )
    }
  }
}
