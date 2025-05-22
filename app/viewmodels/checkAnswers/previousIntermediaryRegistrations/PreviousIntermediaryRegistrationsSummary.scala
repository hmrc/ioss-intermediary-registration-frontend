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

package viewmodels.checkAnswers.previousIntermediaryRegistrations

import models.{Index, UserAnswers}
import pages.previousIntermediaryRegistrations.{AddPreviousIntermediaryRegistrationPage, DeletePreviousIntermediaryRegistrationPage, PreviousIntermediaryRegistrationNumberPage}
import pages.{AddItemPage, CheckAnswersPage, Waypoints}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import queries.previousIntermediaryRegistrations.{AllPreviousIntermediaryRegistrationsQuery, AllPreviousIntermediaryRegistrationsWithOptionalIntermediaryNumberQuery}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object PreviousIntermediaryRegistrationsSummary {

  def row(waypoints: Waypoints, answers: UserAnswers, sourcePage: AddItemPage)(implicit messages: Messages): SummaryList = {

    SummaryList(
      answers.get(AllPreviousIntermediaryRegistrationsWithOptionalIntermediaryNumberQuery).getOrElse(List.empty).zipWithIndex.map {
        case (previousIntermediaryRegistrationDetails, countryIndex) =>

          SummaryListRowViewModel(
            key = previousIntermediaryRegistrationDetails.previousEuCountry.name,
            value = ValueViewModel(HtmlContent(previousIntermediaryRegistrationDetails.previousIntermediaryNumber.getOrElse(""))),
            actions = Seq(
              ActionItemViewModel("site.change", PreviousIntermediaryRegistrationNumberPage(Index(countryIndex)).changeLink(waypoints, sourcePage).url)
                .withVisuallyHiddenText(
                  messages("change.previousIntermediaryRegistration.hidden", previousIntermediaryRegistrationDetails.previousEuCountry.name)
                ),

              ActionItemViewModel("site.remove", DeletePreviousIntermediaryRegistrationPage(Index(countryIndex)).route(waypoints).url)
                .withVisuallyHiddenText(
                  messages("remove.previousIntermediaryRegistration.hidden", previousIntermediaryRegistrationDetails.previousEuCountry.name)
                )
            ),
            actionClasses = "govuk-!-width-one-third"
          )
      }
    )
  }

  def checkAnswersRow(
                       waypoints: Waypoints,
                       answers: UserAnswers,
                       sourcePage: CheckAnswersPage
                     )(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(AllPreviousIntermediaryRegistrationsQuery).map { previousIntermediaryRegistrations =>

      val value = previousIntermediaryRegistrations.map { previousIntermediaryRegistrationDetails =>
        HtmlFormat.escape(previousIntermediaryRegistrationDetails.previousEuCountry.name)
      }.mkString("<br/>")

      SummaryListRowViewModel(
        key = "previousIntermediaryRegistrations.checkYourAnswers",
        value = ValueViewModel(HtmlContent(value)),
        actions = Seq(
          ActionItemViewModel("site.change", AddPreviousIntermediaryRegistrationPage().changeLink(waypoints, sourcePage).url)
            .withVisuallyHiddenText(messages("previousIntermediaryRegistrations.change.hidden"))
        )
      )
    }
  }
}
