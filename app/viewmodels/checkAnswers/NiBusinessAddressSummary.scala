package viewmodels.checkAnswers

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.NiBusinessAddressPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object NiBusinessAddressSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(NiBusinessAddressPage).map {
      answer =>

      val value = HtmlFormat.escape(answer.field1).toString + "<br/>" + HtmlFormat.escape(answer.field2).toString

        SummaryListRowViewModel(
          key     = "niBusinessAddress.checkYourAnswersLabel",
          value   = ValueViewModel(HtmlContent(value)),
          actions = Seq(
            ActionItemViewModel("site.change", routes.NiBusinessAddressController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("niBusinessAddress.change.hidden"))
          )
        )
    }
}
