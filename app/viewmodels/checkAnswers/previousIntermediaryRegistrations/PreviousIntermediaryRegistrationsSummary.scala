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

import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetails
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

  def row(
           waypoints: Waypoints,
           answers: UserAnswers,
           sourcePage: AddItemPage,
           existingPreviousRegistrations: Seq[PreviousIntermediaryRegistrationDetails]
         )(implicit messages: Messages): SummaryList = {

    val existingCountries = existingPreviousRegistrations.map(_.previousEuCountry)

    SummaryList(
      answers.get(AllPreviousIntermediaryRegistrationsWithOptionalIntermediaryNumberQuery).getOrElse(List.empty).zipWithIndex.map {
        case (previousIntermediaryRegistrationDetails, countryIndex) =>

          val actions = if (existingCountries.contains(previousIntermediaryRegistrationDetails.previousEuCountry)) {
            Seq.empty
          } else {
            Seq(
              ActionItemViewModel("site.change", PreviousIntermediaryRegistrationNumberPage(Index(countryIndex)).changeLink(waypoints, sourcePage).url)
                .withVisuallyHiddenText(
                  messages("change.previousIntermediaryRegistration.hidden", previousIntermediaryRegistrationDetails.previousEuCountry.name)
                ),

              ActionItemViewModel("site.remove", DeletePreviousIntermediaryRegistrationPage(Index(countryIndex)).route(waypoints).url)
                .withVisuallyHiddenText(
                  messages("remove.previousIntermediaryRegistration.hidden", previousIntermediaryRegistrationDetails.previousEuCountry.name)
                )
            )
          }
          SummaryListRowViewModel(
            key = previousIntermediaryRegistrationDetails.previousEuCountry.name,
            value = ValueViewModel(HtmlContent(previousIntermediaryRegistrationDetails.previousIntermediaryNumber.getOrElse(""))),
            actions = actions,
            actionClasses = "govuk-!-width-one-third"
          )
      }
    )
  }

  def checkAnswersRow(
                       waypoints: Waypoints,
                       answers: UserAnswers,
                       sourcePage: CheckAnswersPage,
                       existingPreviousRegistrations: Seq[PreviousIntermediaryRegistrationDetails]
                     )(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(AllPreviousIntermediaryRegistrationsQuery).map { previousIntermediaryRegistrations =>

      val currentAnswerCountries = previousIntermediaryRegistrations.map(_.previousEuCountry)
      val existingCountries = existingPreviousRegistrations.map(previousRegistration => previousRegistration.previousEuCountry)
      val sameListOfCountries: Boolean = currentAnswerCountries.sortBy(_.code) == existingCountries.sortBy(_.code)

      val value = previousIntermediaryRegistrations.map { previousIntermediaryRegistrationDetails =>
        HtmlFormat.escape(previousIntermediaryRegistrationDetails.previousEuCountry.name)
      }.mkString("<br/>")

      SummaryListRowViewModel(
        key = "previousIntermediaryRegistrations.checkYourAnswers",
        value = ValueViewModel(HtmlContent(value)),
        actions = Seq(
          if (sameListOfCountries) {
            ActionItemViewModel(
              "site.add",
              controllers.previousIntermediaryRegistrations.routes.AddPreviousIntermediaryRegistrationController.onPageLoad(waypoints).url
            ).withVisuallyHiddenText(messages("previousIntermediaryRegistrations.add.hidden"))
          } else {
            ActionItemViewModel("site.change", AddPreviousIntermediaryRegistrationPage().changeLink(waypoints, sourcePage).url)
              .withVisuallyHiddenText(messages("previousIntermediaryRegistrations.change.hidden"))
          }
        )
      )
    }
  }

  def addedRow(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(AllPreviousIntermediaryRegistrationsQuery).map { allPreviousIntermediaryRegistrations =>

      val value = allPreviousIntermediaryRegistrations.map { previousIntermediaryRegistrationDetails =>
        HtmlFormat.escape(previousIntermediaryRegistrationDetails.previousEuCountry.name)
      }.mkString("<br/>")

      SummaryListRowViewModel(
        key = KeyViewModel("previousIntermediaryRegistrations.added").withCssClass("govuk-!-width-one-half"),
        value = ValueViewModel(HtmlContent(value))
      )
    }
  }
}
