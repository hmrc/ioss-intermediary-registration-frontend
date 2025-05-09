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

package viewmodels.checkAnswers.euDetails

import models.{Index, UserAnswers}
import pages.euDetails.CheckEuDetailsAnswersPage
import pages.{AddItemPage, Waypoints}
import play.api.i18n.Messages
import queries.euDetails.AllEuDetailsQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object EuDetailsSummary {

  def row(
           waypoints: Waypoints,
           answers: UserAnswers,
           sourcePage: AddItemPage
         )(implicit messages: Messages): SummaryList = {
    SummaryList(
      answers.get(AllEuDetailsQuery).getOrElse(List.empty).zipWithIndex.map {
        case (euDetails, countryIndex) =>

          val value = euDetails.euVatNumber.getOrElse("") + euDetails.euTaxReference.getOrElse("")

          SummaryListRowViewModel(
            key = euDetails.euCountry.name,
            value = ValueViewModel(HtmlContent(value)),
            actions = Seq(
              ActionItemViewModel("site.change", CheckEuDetailsAnswersPage(Index(countryIndex)).changeLink(waypoints, sourcePage).url)
                .withVisuallyHiddenText(messages("change.euDetails.hidden", euDetails.euCountry.name)),

              // TODO -> Replace with Delete Page when created and message key
              ActionItemViewModel("site.remove", CheckEuDetailsAnswersPage(Index(countryIndex)).changeLink(waypoints, sourcePage).url)
                .withVisuallyHiddenText(messages("change.euDetails.hidden", euDetails.euCountry.name))
            ),
            actionClasses = "govuk-!-width-one-third"
          )
      }
    )
  }
}
