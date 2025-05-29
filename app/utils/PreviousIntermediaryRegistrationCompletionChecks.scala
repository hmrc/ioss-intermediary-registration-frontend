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

package utils

import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber
import models.requests.AuthenticatedDataRequest
import models.{Country, Index}
import pages.Waypoints
import pages.previousIntermediaryRegistrations.{HasPreviouslyRegisteredAsIntermediaryPage, PreviousIntermediaryRegistrationNumberPage}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import queries.previousIntermediaryRegistrations.AllPreviousIntermediaryRegistrationsWithOptionalIntermediaryNumberQuery

object PreviousIntermediaryRegistrationCompletionChecks extends CompletionChecks {

  private val query: AllPreviousIntermediaryRegistrationsWithOptionalIntermediaryNumberQuery.type =
    AllPreviousIntermediaryRegistrationsWithOptionalIntermediaryNumberQuery

  def incompletePreviousIntermediaryRegistrationRedirect(waypoints: Waypoints)(implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = {
    firstIndexedIncompletePreviousIntermediaryRegistrationCountry(getAllIncompletePreviousIntermediaryRegistrations().map(_.previousEuCountry)) match {
      case Some(incompleteCountry) if incompleteCountry._1.previousIntermediaryNumber.isEmpty =>
        val countryIndex: Index = Index(incompleteCountry._2)
        Some(Redirect(PreviousIntermediaryRegistrationNumberPage(countryIndex).route(waypoints).url))

      case _ => None
    }
  }

  def getAllIncompletePreviousIntermediaryRegistrations()(implicit request: AuthenticatedDataRequest[AnyContent]):
  Seq[PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber] = {
    request.userAnswers.get(query).map { allPreviousIntermediaryRegistrations =>
      allPreviousIntermediaryRegistrations.filter { previousIntermediaryRegistration =>
        previousIntermediaryRegistration.previousIntermediaryNumber.isEmpty
      }
    }.getOrElse(List.empty)
  }

  def emptyPreviousIntermediaryRegistrationsRedirect(waypoints: Waypoints)
                                                    (implicit request: AuthenticatedDataRequest[AnyContent]): Option[Result] = {
    if (!isPreviousIntermediaryRegistrationsDefined()) {
      Some(Redirect(HasPreviouslyRegisteredAsIntermediaryPage.route(waypoints).url))
    } else {
      None
    }
  }

  def isPreviousIntermediaryRegistrationsDefined()(implicit request: AuthenticatedDataRequest[AnyContent]): Boolean = {
    request.userAnswers.get(HasPreviouslyRegisteredAsIntermediaryPage).exists {
      case true => request.userAnswers.get(query).isDefined
      case false => request.userAnswers.get(query).getOrElse(List.empty).isEmpty
    }
  }

  private def firstIndexedIncompletePreviousIntermediaryRegistrationCountry(incompleteCountries: Seq[Country])
                                                                           (implicit request: AuthenticatedDataRequest[AnyContent]):
  Option[(PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber, Int)] = {
    request.userAnswers.get(query)
      .getOrElse(List.empty)
      .zipWithIndex
      .find(indexedDetails => incompleteCountries.contains(indexedDetails._1.previousEuCountry))
  }
}
