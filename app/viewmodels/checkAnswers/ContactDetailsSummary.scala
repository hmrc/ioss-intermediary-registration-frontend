package viewmodels.checkAnswers

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.ContactDetailsPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object ContactDetailsSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(ContactDetailsPage).map {
      answer =>

      val value = HtmlFormat.escape(answer.Contact Name).toString + "<br/>" + HtmlFormat.escape(answer.Telephone Number).toString

        SummaryListRowViewModel(
          key     = "contactDetails.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlContent(value)),
          actions = Seq(
            ActionItemViewModel("site.change", routes.ContactDetailsController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("contactDetails.change.hidden"))
          )
        )
    }
}
