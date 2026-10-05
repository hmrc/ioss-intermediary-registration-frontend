/*
 * Copyright 2026 HM Revenue & Customs
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

package services.core

import base.SpecBase
import controllers.saveAndComeBack.routes
import models.core.{Match, TraderId}
import models.domain.VatCustomerInfo
import models.euDetails.RegistrationType.{TaxId, VatNumber}
import models.euDetails.{EuDetails, RegistrationType}
import models.ossExclusions.ExclusionReason
import models.previousIntermediaryRegistrations.PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber
import models.requests.AuthenticatedDataRequest
import models.{Country, Index, InternationalAddressWithTradingName, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.scalatest.{BeforeAndAfterEach, PrivateMethodTester}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.euDetails.*
import pages.previousIntermediaryRegistrations.{AddPreviousIntermediaryRegistrationPage, HasPreviouslyRegisteredAsIntermediaryPage, PreviousEuCountryPage, PreviousIntermediaryRegistrationNumberPage}
import play.api.mvc.AnyContent
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import queries.euDetails.AllEuDetailsQuery
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CoreSavedAnswersRevalidationServiceSpec extends SpecBase with BeforeAndAfterEach with PrivateMethodTester {

  private implicit val hc: HeaderCarrier = new HeaderCarrier()

  private val mockCoreRegistrationValidationService: CoreRegistrationValidationService = mock[CoreRegistrationValidationService]

  private val intermediaryPrefix: String = arbitraryIntermediaryNumberPrefix.arbitrary.sample.value
  private val intermediaryNumber: String = intermediaryPrefix + numStringWithFixedLength(7).sample.value

  private val baseRequest = AuthenticatedDataRequest(
    request = FakeRequest("GET", "/"),
    credentials = testCredentials,
    vrn = vrn,
    enrolments = testEnrolments,
    userAnswers = emptyUserAnswersWithVatInfo,
    iossNumber = Some(iossNumber),
    numberOfIossRegistrations = 1,
    latestIossRegistration = None,
    latestOssRegistration = None,
    intermediaryNumber = Some(intermediaryNumber),
    registrationWrapper = None
  )

  implicit private val baseDataRequest: AuthenticatedDataRequest[AnyContent] =
    AuthenticatedDataRequest(
      request = baseRequest,
      credentials = testCredentials,
      vrn = vrn,
      enrolments = testEnrolments,
      userAnswers = emptyUserAnswersWithVatInfo,
      iossNumber = Some(iossNumber),
      numberOfIossRegistrations = 1,
      latestIossRegistration = None,
      latestOssRegistration = None,
      intermediaryNumber = Some(intermediaryNumber),
      registrationWrapper = None
    )

  private val requestIntermediaryNumber: String = baseRequest.intermediaryNumber.value

  private val aMatch: Match = arbitraryMatch.arbitrary.sample.value.copy(intermediary = Some(requestIntermediaryNumber))

  private val index: Index = Index(0)

  private val previousEuCountry1: Country = arbitraryCountry.arbitrary.sample.value
  private val previousEuCountry2: Country = arbitraryCountry.arbitrary.retryUntil(_.code != previousEuCountry1.code).sample.value

  private val previousIntermediaryRegistration1: PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber =
    PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber(
      previousEuCountry = previousEuCountry1,
      previousIntermediaryNumber = Some(intermediaryNumber)
    )

  private val previousIntermediaryRegistration2: PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber =
    previousIntermediaryRegistration1
      .copy(previousEuCountry = previousEuCountry2)

  private val allPreviousIntermediaryRegistrations: List[PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber] = List(previousIntermediaryRegistration1, previousIntermediaryRegistration2)

  private val internationalAddressWithTradingName: InternationalAddressWithTradingName =
    arbitraryInternationalAddressWithTradingName.arbitrary.sample.value

  override def beforeEach(): Unit = {
    Mockito.reset(
      mockCoreRegistrationValidationService
    )
  }

  "CoreSavedAnswersRevalidationService" - {

    ".checkAndValidateSavedUserAnswers" - {

      "must return None if there are no active matches" in {

        val request = baseRequest.copy(
          intermediaryNumber = Some(requestIntermediaryNumber)
        )

        implicit val dataRequest: AuthenticatedDataRequest[AnyContent] =
          baseDataRequest.copy(
            request = request,
            intermediaryNumber = Some(requestIntermediaryNumber)
          )

        when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val result = service.checkAndValidateSavedUserAnswers().futureValue

        result `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(dataRequest.vrn))(any(), any())
      }

      "when checking UK VRN" - {

        "must revalidate UK VRN when an expired deregistration date exists" in {

          val today: LocalDate = LocalDate.now(stubClockAtArbitraryDate)
          val vatCustomerInfoWithDeregistration: VatCustomerInfo = vatCustomerInfo.copy(
            deregistrationDecisionDate = Some(today)
          )

          val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
            .copy(vatInfo = Some(vatCustomerInfoWithDeregistration))

          val request = baseRequest.copy(
            userAnswers = updatedUserAnswers,
            intermediaryNumber = Some(requestIntermediaryNumber)
          )

          implicit val dataRequest: AuthenticatedDataRequest[AnyContent] =
            baseDataRequest.copy(
              request = request,
              userAnswers = updatedUserAnswers,
              intermediaryNumber = Some(requestIntermediaryNumber)
            )

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val result = service.checkAndValidateSavedUserAnswers().futureValue

          result `mustBe` Some(Redirect(routes.SavedProgressExpiredVrnDateController.onPageLoad().url))
          verifyNoInteractions(mockCoreRegistrationValidationService)
        }

        "must revalidate UK VRN if one exists and an active match is found" in {

          val request = baseRequest.copy(
            intermediaryNumber = Some(requestIntermediaryNumber)
          )

          implicit val dataRequest: AuthenticatedDataRequest[AnyContent] =
            baseDataRequest.copy(
              request = request,
              intermediaryNumber = Some(requestIntermediaryNumber)
            )

          val activeMatch: Match = aMatch.copy(
            traderId = TraderId(traderId = s"IN${vrn.vrn}"),
            exclusionStatusCode = None,
            exclusionEffectiveDate = None
          )

          when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn Some(activeMatch).toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val result = service.checkAndValidateSavedUserAnswers().futureValue

          result `mustBe` Some(Redirect(routes.SavedProgressClientAlreadyRegisteredController.onPageLoad().url))
          verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(dataRequest.vrn))(any(), eqTo(dataRequest))
        }

        "must revalidate UK VRN if one exists and no active match is found" in {

          val request = baseRequest.copy(
            intermediaryNumber = Some(requestIntermediaryNumber)
          )

          implicit val dataRequest: AuthenticatedDataRequest[AnyContent] =
            baseDataRequest.copy(
              request = request,
              intermediaryNumber = Some(requestIntermediaryNumber)
            )

          when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn None.toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val result = service.checkAndValidateSavedUserAnswers().futureValue

          result `mustBe` None
          verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(dataRequest.vrn))(any(), eqTo(dataRequest))
        }
      }

      "when checking AllEuDetails" - {

        "must iterate through all existing EU Details and revalidate any EU VAT Numbers and EU Tax References present within the user answers" - {

          "and then return None when no active matches are found" in {

            val euVrn: String = s"IN${arbitraryEuVatNumber.sample.value}"
            val country1: Country = Country.euCountries.find(_.code == euVrn.substring(2, 4)).head

            val euTaxReference: String = arbitraryEuTaxReference.sample.value
            val country2: Country = arbitraryCountry.arbitrary.sample.value

            val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
              .set(HasFixedEstablishmentPage, true).success.value
              .set(EuCountryPage(index), country1).success.value
              .set(RegistrationTypePage(index), VatNumber).success.value
              .set(EuVatNumberPage(index), euVrn).success.value
              .set(FixedEstablishmentAddressPage(index), internationalAddressWithTradingName).success.value
              .set(AddEuDetailsPage(), true).success.value
              .set(EuCountryPage(index + 1), country2).success.value
              .set(RegistrationTypePage(index + 1), TaxId).success.value
              .set(EuTaxReferencePage(index + 1), euTaxReference).success.value
              .set(FixedEstablishmentAddressPage(index + 1), internationalAddressWithTradingName).success.value
              .set(AddEuDetailsPage(), false).success.value

            val request = baseRequest.copy(
              userAnswers = updatedUserAnswers,
              intermediaryNumber = Some(requestIntermediaryNumber)
            )

            implicit val dataRequest: AuthenticatedDataRequest[AnyContent] =
              baseDataRequest.copy(
                request = request,
                userAnswers = updatedUserAnswers,
                intermediaryNumber = Some(requestIntermediaryNumber)
              )

            when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn None.toFuture
            when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture
            when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn None.toFuture

            val service: CoreSavedAnswersRevalidationService =
              new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

            val result = service.checkAndValidateSavedUserAnswers().futureValue

            result `mustBe` None
            verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(dataRequest.vrn))(any(), any())
            verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVrn), eqTo(country1.code))(any(), any())
            verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(euTaxReference), eqTo(country2.code))(any(), any())
            verifyNoMoreInteractions(mockCoreRegistrationValidationService)
          }

          "and then return the corresponding URL when an active match is found" in {

            val euVrn: String = arbitraryEuVatNumber.sample.value
            val country1: Country = Country.euCountries.find(_.code == euVrn.substring(0, 2)).head

            val euTaxReference: String = arbitraryEuTaxReference.sample.value
            val country2: Country = arbitraryCountry.arbitrary.sample.value

            val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
              .set(HasFixedEstablishmentPage, true).success.value
              .set(EuCountryPage(index), country1).success.value
              .set(RegistrationTypePage(index), VatNumber).success.value
              .set(EuVatNumberPage(index), euVrn).success.value
              .set(FixedEstablishmentAddressPage(index), internationalAddressWithTradingName).success.value
              .set(AddEuDetailsPage(), true).success.value
              .set(EuCountryPage(index + 1), country2).success.value
              .set(RegistrationTypePage(index + 1), TaxId).success.value
              .set(EuTaxReferencePage(index + 1), euTaxReference).success.value
              .set(FixedEstablishmentAddressPage(index + 1), internationalAddressWithTradingName).success.value
              .set(AddEuDetailsPage(), false).success.value

            val request = baseRequest.copy(
              userAnswers = updatedUserAnswers,
              intermediaryNumber = Some(requestIntermediaryNumber)
            )

            implicit val dataRequest: AuthenticatedDataRequest[AnyContent] =
              baseDataRequest.copy(
                request = request,
                userAnswers = updatedUserAnswers,
                intermediaryNumber = Some(requestIntermediaryNumber)
              )

            val activeMatch: Match = aMatch.copy(
              traderId = TraderId(traderId = s"IN$euTaxReference"),
              memberState = country2.code,
              exclusionStatusCode = None,
              exclusionEffectiveDate = None
            )

            when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn None.toFuture
            when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture
            when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn Some(activeMatch).toFuture

            val service: CoreSavedAnswersRevalidationService =
              new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

            val result = service.checkAndValidateSavedUserAnswers().futureValue

            result `mustBe` Some(Redirect(routes.SavedProgressClientAlreadyRegisteredController.onPageLoad().url))
            verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(dataRequest.vrn))(any(), any())
            verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVrn), eqTo(country1.code))(any(), any())
            verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(euTaxReference), eqTo(country2.code))(any(), any())
            verifyNoMoreInteractions(mockCoreRegistrationValidationService)
          }

          "and then return the corresponding URL when an quarantined match is found" in {

            val euVrn: String = arbitraryEuVatNumber.sample.value
            val country1: Country = Country.euCountries.find(_.code == euVrn.substring(0, 2)).head

            val euTaxReference: String = arbitraryEuTaxReference.sample.value
            val country2: Country = arbitraryCountry.arbitrary.sample.value

            val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
              .set(HasFixedEstablishmentPage, true).success.value
              .set(EuCountryPage(index), country1).success.value
              .set(RegistrationTypePage(index), VatNumber).success.value
              .set(EuVatNumberPage(index), euVrn).success.value
              .set(FixedEstablishmentAddressPage(index), internationalAddressWithTradingName).success.value
              .set(AddEuDetailsPage(), true).success.value
              .set(EuCountryPage(index + 1), country2).success.value
              .set(RegistrationTypePage(index + 1), TaxId).success.value
              .set(EuTaxReferencePage(index + 1), euTaxReference).success.value
              .set(FixedEstablishmentAddressPage(index + 1), internationalAddressWithTradingName).success.value
              .set(AddEuDetailsPage(), false).success.value

            val request = baseRequest.copy(
              userAnswers = updatedUserAnswers,
              intermediaryNumber = Some(requestIntermediaryNumber)
            )

            implicit val dataRequest: AuthenticatedDataRequest[AnyContent] =
              baseDataRequest.copy(
                request = request,
                userAnswers = updatedUserAnswers,
                intermediaryNumber = Some(requestIntermediaryNumber)
              )

            val quarantinedMatch: Match = aMatch.copy(
              traderId = TraderId(traderId = s"IN$euTaxReference"),
              memberState = country2.code,
              exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
              exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
            )

            when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn None.toFuture
            when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture
            when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn Some(quarantinedMatch).toFuture

            val service: CoreSavedAnswersRevalidationService =
              new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

            val result = service.checkAndValidateSavedUserAnswers().futureValue

            result `mustBe` Some(Redirect(routes.SavedProgressQuarantinedController.onPageLoad(
              quarantinedEffectiveDate = quarantinedMatch.getEffectiveDate
            ).url))
            verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(dataRequest.vrn))(any(), any())
            verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVrn), eqTo(country1.code))(any(), any())
            verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(euTaxReference), eqTo(country2.code))(any(), any())
            verifyNoMoreInteractions(mockCoreRegistrationValidationService)
          }
        }
      }

      "when checking allPreviousIntermediaryRegistrations" - {

        val traderId: String = allPreviousIntermediaryRegistrations.tail.head.previousIntermediaryNumber.value
        val memberState: String = allPreviousIntermediaryRegistrations.tail.head.previousEuCountry.code

        val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
          .set(HasPreviouslyRegisteredAsIntermediaryPage, true).success.value
          .set(PreviousEuCountryPage(index), allPreviousIntermediaryRegistrations.head.previousEuCountry).success.value
          .set(PreviousIntermediaryRegistrationNumberPage(index), intermediaryNumber).success.value
          .set(AddPreviousIntermediaryRegistrationPage(), true).success.value
          .set(PreviousEuCountryPage(index + 1), allPreviousIntermediaryRegistrations.tail.head.previousEuCountry).success.value
          .set(PreviousIntermediaryRegistrationNumberPage(index + 1), traderId).success.value
          .set(AddPreviousIntermediaryRegistrationPage(), false).success.value

        "must iterate through all existing Previous Intermediary Registrations and revalidate any previous intermediary numbers present" - {

          "and then return None when no active matches are found" in {

            val request = baseRequest.copy(
              userAnswers = updatedUserAnswers,
              intermediaryNumber = Some(requestIntermediaryNumber)
            )

            implicit val dataRequest: AuthenticatedDataRequest[AnyContent] =
              baseDataRequest.copy(
                request = request,
                userAnswers = updatedUserAnswers,
                intermediaryNumber = Some(requestIntermediaryNumber)
              )

            when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn None.toFuture
            when(mockCoreRegistrationValidationService.searchScheme(any(), any())(any(), any())) thenReturn None.toFuture

            val service: CoreSavedAnswersRevalidationService =
              new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

            val result = service.checkAndValidateSavedUserAnswers().futureValue

            result `mustBe` None
            verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(any())(any(), any())
            verify(mockCoreRegistrationValidationService, times(2)).searchScheme(any(), any())(any(), any())
          }

          "and then return the corresponding URL when an active match is found" in {

            val traderId: String = allPreviousIntermediaryRegistrations.tail.head.previousIntermediaryNumber.value
            val memberState: String = allPreviousIntermediaryRegistrations.tail.head.previousEuCountry.code

            val request = baseRequest.copy(
              userAnswers = updatedUserAnswers,
              intermediaryNumber = Some(requestIntermediaryNumber)
            )

            implicit val dataRequest: AuthenticatedDataRequest[AnyContent] =
              baseDataRequest.copy(
                request = request,
                userAnswers = updatedUserAnswers,
                intermediaryNumber = Some(requestIntermediaryNumber)
              )

            val activeMatch: Match = aMatch.copy(
              traderId = TraderId(traderId = traderId),
              memberState = memberState,
              exclusionStatusCode = None,
              exclusionEffectiveDate = None
            )

            when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn None.toFuture
            when(mockCoreRegistrationValidationService.searchScheme(any(), any())(any(), any())) thenReturn None.toFuture
            when(mockCoreRegistrationValidationService.searchScheme(eqTo(traderId), eqTo(memberState))(any(), any())) thenReturn Some(activeMatch).toFuture

            val service: CoreSavedAnswersRevalidationService =
              new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

            val result = service.checkAndValidateSavedUserAnswers().futureValue

            result `mustBe` Some(Redirect(routes.SavedProgressClientAlreadyRegisteredController.onPageLoad()))
            verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(any())(any(), any())
            verify(mockCoreRegistrationValidationService, times(2)).searchScheme(any(), any())(any(), any())
          }

          "and then return the corresponding URL when a quarantined match is found" in {

            val request = baseRequest.copy(
              userAnswers = updatedUserAnswers,
              intermediaryNumber = Some(requestIntermediaryNumber)
            )

            implicit val dataRequest: AuthenticatedDataRequest[AnyContent] =
              baseDataRequest.copy(
                request = request,
                userAnswers = updatedUserAnswers,
                intermediaryNumber = Some(requestIntermediaryNumber)
              )

            val quarantinedMatch: Match = aMatch.copy(
              traderId = TraderId(traderId = traderId),
              memberState = memberState,
              exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
              exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
            )

            when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn None.toFuture
            when(mockCoreRegistrationValidationService.searchScheme(any(), any())(any(), any())) thenReturn None.toFuture
            when(mockCoreRegistrationValidationService.searchScheme(eqTo(traderId), eqTo(memberState))(any(), any())) thenReturn Some(quarantinedMatch).toFuture

            val service: CoreSavedAnswersRevalidationService =
              new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

            val result = service.checkAndValidateSavedUserAnswers().futureValue

            result `mustBe` Some(Redirect(routes.SavedProgressQuarantinedController.onPageLoad(
              quarantinedEffectiveDate = quarantinedMatch.getEffectiveDate
            )))
            verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(any())(any(), any())
            verify(mockCoreRegistrationValidationService, times(2)).searchScheme(any(), any())(any(), any())
          }
        }
      }
    }

    ".revalidateUKVrn" - {

      "must return None if no active match is found" in {

        when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateUKVrn"))

        val result = service invokePrivate privateMethodCall(vrn, hc, baseDataRequest)

        result.futureValue `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(baseDataRequest.vrn))(any(), any())
      }

      "must return the URL for Expired Vrn Date page when the deregistration date is present and is on or before today" in {

        val today: LocalDate = LocalDate.now(stubClockAtArbitraryDate)
        val vatCustomerInfoWithDeregistration: VatCustomerInfo = vatCustomerInfo.copy(
          deregistrationDecisionDate = Some(today)
        )

        val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo.copy(vatInfo = Some(vatCustomerInfoWithDeregistration))

        val request = baseRequest.copy(
          userAnswers = updatedUserAnswers,
          intermediaryNumber = Some(requestIntermediaryNumber)
        )

        implicit val dataRequest: AuthenticatedDataRequest[AnyContent] =
          baseDataRequest.copy(
            request = request,
            userAnswers = updatedUserAnswers,
            intermediaryNumber = Some(requestIntermediaryNumber)
          )

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateUKVrn"))

        val result = service invokePrivate privateMethodCall(vrn, hc, dataRequest)

        result.futureValue `mustBe` Some(Redirect(routes.SavedProgressExpiredVrnDateController.onPageLoad().url))
        verifyNoInteractions(mockCoreRegistrationValidationService)
      }

      "must return None and then check for an active match when the deregistration date is present and is after today" in {

        val tomorrow: LocalDate = LocalDate.now(stubClockAtArbitraryDate).plusDays(1)
        val vatCustomerInfoWithDeregistration: VatCustomerInfo = vatCustomerInfo.copy(
          deregistrationDecisionDate = Some(tomorrow)
        )

        val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo.copy(vatInfo = Some(vatCustomerInfoWithDeregistration))

        val request = baseRequest.copy(
          userAnswers = updatedUserAnswers,
          intermediaryNumber = Some(requestIntermediaryNumber)
        )

        implicit val dataRequest: AuthenticatedDataRequest[AnyContent] =
          baseDataRequest.copy(
            request = request,
            userAnswers = updatedUserAnswers,
            intermediaryNumber = Some(requestIntermediaryNumber)
          )

        when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateUKVrn"))

        val result = service invokePrivate privateMethodCall(vrn, hc, dataRequest)

        result.futureValue `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(vrn))(any(), any())
      }

      "must return the corresponding URL when an active match is found" in {

        val activeVrn: Vrn = arbitraryVrn.arbitrary.sample.value
        val activeMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IN${activeVrn.vrn}"),
          exclusionStatusCode = None,
          exclusionEffectiveDate = None
        )

        when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn Some(activeMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateUKVrn"))

        val result = service invokePrivate privateMethodCall(activeVrn, hc, baseDataRequest)

        result.futureValue `mustBe` Some(Redirect(routes.SavedProgressClientAlreadyRegisteredController.onPageLoad().url))
        verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(activeVrn))(any(), any())
      }

      "must return the corresponding URL when a quarantined match is found" in {

        val quarantinedVrn: Vrn = arbitraryVrn.arbitrary.sample.value
        val quarantinedMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IN${quarantinedVrn.vrn}"),
          exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
          exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
        )

        when(mockCoreRegistrationValidationService.searchUkVrn(any())(any(), any())) thenReturn Some(quarantinedMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateUKVrn"))

        val result = service invokePrivate privateMethodCall(quarantinedVrn, hc, baseDataRequest)

        result.futureValue `mustBe` Some(Redirect(routes.SavedProgressQuarantinedController.onPageLoad(
          quarantinedEffectiveDate = quarantinedMatch.getEffectiveDate
        ).url))
        verify(mockCoreRegistrationValidationService, times(1)).searchUkVrn(eqTo(quarantinedVrn))(any(), any())
      }
    }

    ".revalidateEuTaxId" - {

      "must return None if no active match is found" in {

        val euTaxReference: String = arbitraryEuTaxReference.sample.value
        val countryCode: String = arbitraryCountry.arbitrary.sample.value.code

        when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuTaxId"))

        val result = service invokePrivate privateMethodCall(euTaxReference, countryCode, hc, baseDataRequest)

        result.futureValue `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(euTaxReference), eqTo(countryCode))(any(), any())
      }

      "must return the corresponding URL when an active match is found" in {

        val euTaxReference: String = arbitraryEuTaxReference.sample.value
        val countryCode: String = arbitraryCountry.arbitrary.sample.value.code

        val activeMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IN$euTaxReference"),
          memberState = countryCode,
          exclusionStatusCode = None,
          exclusionEffectiveDate = None
        )

        when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn Some(activeMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuTaxId"))

        val result = service invokePrivate privateMethodCall(euTaxReference, countryCode, hc, baseDataRequest)

        result.futureValue `mustBe` Some(Redirect(routes.SavedProgressClientAlreadyRegisteredController.onPageLoad().url))
        verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(euTaxReference), eqTo(countryCode))(any(), any())
      }

      "must return the corresponding URL when a quarantined match is found" in {

        val euTaxReference: String = arbitraryEuTaxReference.sample.value
        val countryCode: String = arbitraryCountry.arbitrary.sample.value.code

        val quarantinedMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IN$euTaxReference"),
          intermediary = Some(requestIntermediaryNumber),
          memberState = countryCode,
          exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
          exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
        )

        when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn Some(quarantinedMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuTaxId"))

        val result = service invokePrivate privateMethodCall(euTaxReference, countryCode, hc, baseDataRequest)

        result.futureValue `mustBe` Some(Redirect(routes.SavedProgressQuarantinedController.onPageLoad(
          quarantinedEffectiveDate = quarantinedMatch.getEffectiveDate
        ).url))
        verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(euTaxReference), eqTo(countryCode))(any(), any())
      }
    }

    ".revalidateEuVrn" - {

      "must return None if no active match is found" in {

        val euVrn: String = arbitraryEuVatNumber.sample.value
        val countryCode: String = euVrn.substring(0, 2)

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuVrn"))

        val result = service invokePrivate privateMethodCall(euVrn, countryCode, hc, baseDataRequest)

        result.futureValue `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVrn), eqTo(countryCode))(any(), any())
      }

      "must return the corresponding URL when an active match is found" in {

        val euVrn: String = arbitraryEuVatNumber.sample.value
        val countryCode: String = euVrn.substring(0, 2)

        val activeMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IN$euVrn"),
          memberState = countryCode,
          exclusionStatusCode = None,
          exclusionEffectiveDate = None
        )

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn Some(activeMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuVrn"))

        val result = service invokePrivate privateMethodCall(euVrn, countryCode, hc, baseDataRequest)

        result.futureValue `mustBe` Some(Redirect(routes.SavedProgressClientAlreadyRegisteredController.onPageLoad().url))
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVrn), eqTo(countryCode))(any(), any())
      }

      "must return the corresponding URL when a quarantined match is found" in {

        val euVrn: String = arbitraryEuVatNumber.sample.value
        val countryCode: String = euVrn.substring(0, 2)

        val quarantinedMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IN$euVrn"),
          memberState = countryCode,
          exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
          exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
        )

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn Some(quarantinedMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuVrn"))

        val result = service invokePrivate privateMethodCall(euVrn, countryCode, hc, baseDataRequest)

        result.futureValue `mustBe` Some(Redirect(routes.SavedProgressQuarantinedController.onPageLoad(
          quarantinedEffectiveDate = quarantinedMatch.getEffectiveDate
        ).url))
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVrn), eqTo(countryCode))(any(), any())
      }
    }

    ".checkAllEuDetails" - {

      "must return None if there are no active matches found for all countries present in user answers" in {

        val euVrn: String = arbitraryEuVatNumber.sample.value
        val country1: Country = Country.euCountries.find(_.code == euVrn.substring(0, 2)).head

        val euTaxReference: String = arbitraryEuTaxReference.sample.value
        val country2: Country = arbitraryCountry.arbitrary.sample.value

        val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
          .set(HasFixedEstablishmentPage, true).success.value
          .set(EuCountryPage(index), country1).success.value
          .set(RegistrationTypePage(index), VatNumber).success.value
          .set(EuVatNumberPage(index), euVrn).success.value
          .set(FixedEstablishmentAddressPage(index), internationalAddressWithTradingName).success.value
          .set(AddEuDetailsPage(), true).success.value
          .set(EuCountryPage(index + 1), country2).success.value
          .set(RegistrationTypePage(index + 1), TaxId).success.value
          .set(EuTaxReferencePage(index + 1), euTaxReference).success.value
          .set(FixedEstablishmentAddressPage(index + 1), internationalAddressWithTradingName).success.value
          .set(AddEuDetailsPage(), false).success.value

        val request = baseRequest.copy(
          userAnswers = updatedUserAnswers,
          intermediaryNumber = Some(requestIntermediaryNumber)
        )

        implicit val dataRequest: AuthenticatedDataRequest[AnyContent] = {
          baseDataRequest.copy(
            request = request,
            userAnswers = updatedUserAnswers,
            intermediaryNumber = Some(requestIntermediaryNumber)
          )
        }

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture
        when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val list: List[EuDetails] = request.userAnswers.get(AllEuDetailsQuery).getOrElse(List.empty)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("checkAllEuDetails"))

        val result = service invokePrivate privateMethodCall(list, hc, dataRequest)

        result.futureValue `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVrn), eqTo(country1.code))(any(), eqTo(dataRequest))
        verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(euTaxReference), eqTo(country2.code))(any(), eqTo(dataRequest))
      }

      "must return the corresponding URL when the first country with an active match is found in the user answers" in {

        val euVrn: String = arbitraryEuVatNumber.sample.value
        val country1: Country = Country.euCountries.find(_.code == euVrn.substring(0, 2)).head

        val euTaxReference: String = arbitraryEuTaxReference.sample.value
        val country2: Country = arbitraryCountry.arbitrary.sample.value

        val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
          .set(HasFixedEstablishmentPage, true).success.value
          .set(EuCountryPage(index), country1).success.value
          .set(RegistrationTypePage(index), VatNumber).success.value
          .set(EuVatNumberPage(index), euVrn).success.value
          .set(FixedEstablishmentAddressPage(index), internationalAddressWithTradingName).success.value
          .set(AddEuDetailsPage(), true).success.value
          .set(EuCountryPage(index + 1), country2).success.value
          .set(RegistrationTypePage(index + 1), TaxId).success.value
          .set(EuTaxReferencePage(index + 1), euTaxReference).success.value
          .set(FixedEstablishmentAddressPage(index + 1), internationalAddressWithTradingName).success.value
          .set(AddEuDetailsPage(), false).success.value

        val request = baseRequest.copy(
          userAnswers = updatedUserAnswers,
          intermediaryNumber = Some(requestIntermediaryNumber)
        )

        implicit val dataRequest: AuthenticatedDataRequest[AnyContent] = {
          baseDataRequest.copy(
            request = request,
            userAnswers = updatedUserAnswers,
            intermediaryNumber = Some(requestIntermediaryNumber)
          )
        }

        val activeMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IN$euTaxReference"),
          memberState = country2.code,
          exclusionStatusCode = None,
          exclusionEffectiveDate = None
        )

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture
        when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn Some(activeMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val list: List[EuDetails] = request.userAnswers.get(AllEuDetailsQuery).getOrElse(List.empty)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("checkAllEuDetails"))

        val result = service invokePrivate privateMethodCall(list, hc, dataRequest)

        result.futureValue `mustBe` Some(Redirect(routes.SavedProgressClientAlreadyRegisteredController.onPageLoad().url))
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVrn), eqTo(country1.code))(any(), eqTo(dataRequest))
        verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(euTaxReference), eqTo(country2.code))(any(), eqTo(dataRequest))
      }

      "must return the corresponding URL when the first country with a quarantined match is found in the user answers" in {

        val euVrn: String = arbitraryEuVatNumber.sample.value
        val country1: Country = Country.euCountries.find(_.code == euVrn.substring(0, 2)).head

        val euTaxReference: String = arbitraryEuTaxReference.sample.value
        val country2: Country = arbitraryCountry.arbitrary.sample.value

        val updatedUserAnswers: UserAnswers = emptyUserAnswersWithVatInfo
          .set(HasFixedEstablishmentPage, true).success.value
          .set(EuCountryPage(index), country1).success.value
          .set(RegistrationTypePage(index), VatNumber).success.value
          .set(EuVatNumberPage(index), euVrn).success.value
          .set(FixedEstablishmentAddressPage(index), internationalAddressWithTradingName).success.value
          .set(AddEuDetailsPage(), true).success.value
          .set(EuCountryPage(index + 1), country2).success.value
          .set(RegistrationTypePage(index + 1), TaxId).success.value
          .set(EuTaxReferencePage(index + 1), euTaxReference).success.value
          .set(FixedEstablishmentAddressPage(index + 1), internationalAddressWithTradingName).success.value
          .set(AddEuDetailsPage(), false).success.value

        val request = baseRequest.copy(
          userAnswers = updatedUserAnswers,
          intermediaryNumber = Some(requestIntermediaryNumber)
        )

        implicit val dataRequest: AuthenticatedDataRequest[AnyContent] = {
          baseDataRequest.copy(
            request = request,
            userAnswers = updatedUserAnswers,
            intermediaryNumber = Some(requestIntermediaryNumber)
          )
        }

        val quarantinedMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IN$euVrn"),
          memberState = country1.code,
          exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
          exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
        )

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn Some(quarantinedMatch).toFuture
        when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val list: List[EuDetails] = request.userAnswers.get(AllEuDetailsQuery).getOrElse(List.empty)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("checkAllEuDetails"))

        val result = service invokePrivate privateMethodCall(list, hc, dataRequest)

        result.futureValue `mustBe` Some(Redirect(routes.SavedProgressQuarantinedController.onPageLoad(
          quarantinedEffectiveDate = quarantinedMatch.getEffectiveDate
        ).url))
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVrn), eqTo(country1.code))(any(), eqTo(dataRequest))
        verifyNoMoreInteractions(mockCoreRegistrationValidationService)
      }
    }

    ".revalidateEuDetails" - {

      "must return None when no active matches are found" in {

        val euVatNumber: String = arbitraryEuVatNumber.sample.value
        val euCountry: Country = Country.euCountries.find(_.code == euVatNumber.substring(0, 2)).head

        val euDetails: EuDetails = arbitraryEuDetails.arbitrary.sample.value.copy(
          euVatNumber = Some(euVatNumber),
          euCountry = euCountry
        )

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn None.toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuDetails"))

        val result = service invokePrivate privateMethodCall(euDetails, Some(euVatNumber), hc, baseDataRequest)

        result.futureValue `mustBe` None
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVatNumber), eqTo(euCountry.code))(any(), any())
        verifyNoMoreInteractions(mockCoreRegistrationValidationService)
      }

      "must return the corresponding URL when an active match is found for an EU VAT number" in {

        val euVatNumber: String = arbitraryEuVatNumber.sample.value
        val euCountry: Country = Country.euCountries.find(_.code == euVatNumber.substring(0, 2)).head

        val euDetails: EuDetails = arbitraryEuDetails.arbitrary.sample.value.copy(
          euVatNumber = Some(euVatNumber),
          euCountry = euCountry
        )

        val activeMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IN$euVatNumber"),
          memberState = euCountry.code,
          exclusionStatusCode = None,
          exclusionEffectiveDate = None
        )

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn Some(activeMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuDetails"))

        val result = service invokePrivate privateMethodCall(euDetails, Some(euVatNumber), hc, baseDataRequest)

        result.futureValue `mustBe` Some(Redirect(routes.SavedProgressClientAlreadyRegisteredController.onPageLoad().url))
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVatNumber), eqTo(euCountry.code))(any(), any())
        verifyNoMoreInteractions(mockCoreRegistrationValidationService)
      }

      "must return the corresponding URL when a quarantined match is found for an EU VAT number" in {

        val euVatNumber: String = arbitraryEuVatNumber.sample.value
        val euCountry: Country = Country.euCountries.find(_.code == euVatNumber.substring(0, 2)).head

        val euDetails: EuDetails = arbitraryEuDetails.arbitrary.sample.value.copy(
          euVatNumber = Some(euVatNumber),
          euCountry = euCountry
        )

        val quarantinedMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IN$euVatNumber"),
          memberState = euCountry.code,
          exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
          exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
        )

        when(mockCoreRegistrationValidationService.searchEuVrn(any(), any())(any(), any())) thenReturn Some(quarantinedMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuDetails"))

        val result = service invokePrivate privateMethodCall(euDetails, Some(euVatNumber), hc, baseDataRequest)

        result.futureValue `mustBe` Some(Redirect(routes.SavedProgressQuarantinedController.onPageLoad(
          quarantinedEffectiveDate = quarantinedMatch.getEffectiveDate
        ).url))
        verify(mockCoreRegistrationValidationService, times(1)).searchEuVrn(eqTo(euVatNumber), eqTo(euCountry.code))(any(), any())
        verifyNoMoreInteractions(mockCoreRegistrationValidationService)
      }

      "must return the corresponding URL when an active match is found for an EU Tax Reference number" in {

        val euTaxReference: String = arbitraryEuTaxReference.sample.value
        val euCountry: Country = arbitraryCountry.arbitrary.sample.value

        val euDetails: EuDetails = arbitraryEuDetails.arbitrary.sample.value.copy(
          euVatNumber = None,
          euTaxReference = Some(euTaxReference),
          euCountry = euCountry
        )

        val activeMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IN$euTaxReference"),
          memberState = euCountry.code,
          exclusionStatusCode = None,
          exclusionEffectiveDate = None
        )

        when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn Some(activeMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuDetails"))

        val result = service invokePrivate privateMethodCall(euDetails, None, hc, baseDataRequest)

        result.futureValue `mustBe` Some(Redirect(routes.SavedProgressClientAlreadyRegisteredController.onPageLoad().url))
        verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(euTaxReference), eqTo(euCountry.code))(any(), any())
        verifyNoMoreInteractions(mockCoreRegistrationValidationService)
      }

      "must return the corresponding URL when a quarantined match is found for an EU Tax Reference number" in {

        val euTaxReference: String = arbitraryEuTaxReference.sample.value
        val euCountry: Country = arbitraryCountry.arbitrary.sample.value

        val euDetails: EuDetails = arbitraryEuDetails.arbitrary.sample.value.copy(
          euVatNumber = None,
          euTaxReference = Some(euTaxReference),
          euCountry = euCountry
        )

        val quarantinedMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IN$euTaxReference"),
          memberState = euCountry.code,
          exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
          exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
        )

        when(mockCoreRegistrationValidationService.searchEuTaxId(any(), any())(any(), any())) thenReturn Some(quarantinedMatch).toFuture

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuDetails"))

        val result = service invokePrivate privateMethodCall(euDetails, None, hc, baseDataRequest)

        result.futureValue `mustBe` Some(Redirect(routes.SavedProgressQuarantinedController.onPageLoad(
          quarantinedEffectiveDate = quarantinedMatch.getEffectiveDate
        ).url))
        verify(mockCoreRegistrationValidationService, times(1)).searchEuTaxId(eqTo(euTaxReference), eqTo(euCountry.code))(any(), any())
        verifyNoMoreInteractions(mockCoreRegistrationValidationService)
      }

      "must return None when Eu Details exist with neither a Eu Vat Number or EU Tax Reference present" in {

        val euDetails: EuDetails = arbitraryEuDetails.arbitrary.sample.value.copy(
          euVatNumber = None,
          euTaxReference = None
        )

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidateEuDetails"))

        val result = service invokePrivate privateMethodCall(euDetails, None, hc, baseDataRequest)

        result.futureValue `mustBe` None
        verifyNoInteractions(mockCoreRegistrationValidationService)
      }
    }

    ".revalidatePreviousIntermediaryRegistrations" - {

      "must iterate through all existing previous intermediary registrations" - {

        "and return None when no active matches are found" in {

          when(mockCoreRegistrationValidationService.searchScheme(any(), any())(any(), any())) thenReturn None.toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidatePreviousIntermediaryRegistrations"))

          val result = service invokePrivate privateMethodCall(allPreviousIntermediaryRegistrations, hc, baseDataRequest)

          result.futureValue `mustBe` None
          verify(mockCoreRegistrationValidationService, times(2)).searchScheme(any(), any())(any(), any())
        }

        "and continue to iterate through the list when optional intermediary numbers are missing and return None when no active matches are found" in {

          val previousIntermediaryRegistrationWithoutOptionalIntermediaryNumber: PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber = previousIntermediaryRegistration1
            .copy(previousIntermediaryNumber = None)

          val updatedAllPreviousIntermediaryRegistrations: List[PreviousIntermediaryRegistrationDetailsWithOptionalIntermediaryNumber] = List(previousIntermediaryRegistrationWithoutOptionalIntermediaryNumber, previousIntermediaryRegistration1)

          when(mockCoreRegistrationValidationService.searchScheme(any(), any())(any(), any())) thenReturn None.toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidatePreviousIntermediaryRegistrations"))

          val result = service invokePrivate privateMethodCall(updatedAllPreviousIntermediaryRegistrations, hc, baseDataRequest)

          result.futureValue `mustBe` None
          verify(mockCoreRegistrationValidationService, times(1)).searchScheme(
            eqTo(updatedAllPreviousIntermediaryRegistrations.tail.head.previousIntermediaryNumber.value),
            eqTo(updatedAllPreviousIntermediaryRegistrations.tail.head.previousEuCountry.code),
          )(any(), any())
        }

        "and return the corresponding URL when an active match is found" in {

          val previousIntermediaryNumber: String = allPreviousIntermediaryRegistrations.tail.head.previousIntermediaryNumber.value

          val activeMatch: Match = aMatch.copy(
            traderId = TraderId(traderId = previousIntermediaryNumber),
            memberState = previousIntermediaryRegistration2.previousEuCountry.code,
            exclusionStatusCode = None,
            exclusionEffectiveDate = None
          )

          when(mockCoreRegistrationValidationService.searchScheme(any(), any())(any(), any())) thenReturn None.toFuture
          when(mockCoreRegistrationValidationService.searchScheme(
              eqTo(previousIntermediaryNumber),
              eqTo(previousIntermediaryRegistration2.previousEuCountry.code))
            (any(), any())) thenReturn Some(activeMatch).toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidatePreviousIntermediaryRegistrations"))

          val result = service invokePrivate privateMethodCall(allPreviousIntermediaryRegistrations, hc, baseDataRequest)

          result.futureValue `mustBe` Some(Redirect(routes.SavedProgressClientAlreadyRegisteredController.onPageLoad()))
          verify(mockCoreRegistrationValidationService, times(2)).searchScheme(any(), any())(any(), any())
        }

        "and return the corresponding URL when a quarantined match is found" in {

          val previousIntermediaryNumber: String = allPreviousIntermediaryRegistrations.tail.head.previousIntermediaryNumber.value

          val quarantinedMatch: Match = aMatch.copy(
            traderId = TraderId(traderId = previousIntermediaryNumber),
            memberState = previousIntermediaryRegistration2.previousEuCountry.code,
            exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
            exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
          )

          when(mockCoreRegistrationValidationService.searchScheme(any(), any())(any(), any())) thenReturn None.toFuture
          when(mockCoreRegistrationValidationService.searchScheme(
              eqTo(previousIntermediaryNumber),
              eqTo(previousIntermediaryRegistration2.previousEuCountry.code))
            (any(), any())) thenReturn Some(quarantinedMatch).toFuture

          val service: CoreSavedAnswersRevalidationService =
            new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

          val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("revalidatePreviousIntermediaryRegistrations"))

          val result = service invokePrivate privateMethodCall(allPreviousIntermediaryRegistrations, hc, baseDataRequest)

          result.futureValue `mustBe` Some(Redirect(routes.SavedProgressQuarantinedController.onPageLoad(
            quarantinedEffectiveDate = quarantinedMatch.getEffectiveDate
          )))
          verify(mockCoreRegistrationValidationService, times(2)).searchScheme(any(), any())(any(), any())
        }
      }
    }

    ".activeMatchRedirectUrl" - {

      "must return None when no active match is found" in {

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("activeMatchRedirectUrl"))

        val result = service invokePrivate privateMethodCall(None)

        result.futureValue `mustBe` None
      }

      "must return the URL for Client Already Registered page when an active match exists and the trader is already registered" in {

        val activeMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IN$intermediaryNumber"),
          exclusionStatusCode = None,
          exclusionEffectiveDate = None
        )

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("activeMatchRedirectUrl"))

        val result = service invokePrivate privateMethodCall(Some(activeMatch))
        result.futureValue `mustBe` Some(Redirect(routes.SavedProgressClientAlreadyRegisteredController.onPageLoad().url))
      }

      "must return the URL for Other Country Excluded And Quarantined page when a quarantined match exists" in {

        val quarantinedMatch: Match = aMatch.copy(
          traderId = TraderId(traderId = s"IN$intermediaryNumber"),
          exclusionStatusCode = Some(ExclusionReason.FailsToComply.numberValue),
          exclusionEffectiveDate = Some(LocalDate.now(stubClockAtArbitraryDate).minusYears(2).plusDays(1).toString)
        )

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Future[Option[String]]](Symbol("activeMatchRedirectUrl"))

        val result = service invokePrivate privateMethodCall(Some(quarantinedMatch))

        result.futureValue `mustBe` Some(Redirect(routes.SavedProgressQuarantinedController.onPageLoad(
          quarantinedEffectiveDate = quarantinedMatch.getEffectiveDate
        ).url))
      }
    }

    ".checkVrnExpired" - {

      "must return false if there is no Vat Customer Info present" in {

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Boolean](Symbol("checkVrnExpired"))

        val result = service invokePrivate privateMethodCall(None)

        result `mustBe` false
      }

      "must return false if deregistration date is absent" in {

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Boolean](Symbol("checkVrnExpired"))

        val result = service invokePrivate privateMethodCall(Some(vatCustomerInfo))

        result `mustBe` false
      }

      "must return false when deregistration date is present and is after today " in {

        val tomorrow: LocalDate = LocalDate.now(stubClockAtArbitraryDate).plusDays(1)
        val vatCustomerInfoWithDeregistration: VatCustomerInfo = vatCustomerInfo.copy(
          deregistrationDecisionDate = Some(tomorrow)
        )

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Boolean](Symbol("checkVrnExpired"))

        val result = service invokePrivate privateMethodCall(Some(vatCustomerInfoWithDeregistration))

        result `mustBe` false
      }

      "must return true when deregistration date is present and is today " in {

        val today: LocalDate = LocalDate.now(stubClockAtArbitraryDate)
        val vatCustomerInfoWithDeregistration: VatCustomerInfo = vatCustomerInfo.copy(
          deregistrationDecisionDate = Some(today)
        )

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Boolean](Symbol("checkVrnExpired"))

        val result = service invokePrivate privateMethodCall(Some(vatCustomerInfoWithDeregistration))

        result `mustBe` true
      }

      "must return true when deregistration date is present and is before today " in {

        val yesterday: LocalDate = LocalDate.now(stubClockAtArbitraryDate).minusDays(1)
        val vatCustomerInfoWithDeregistration: VatCustomerInfo = vatCustomerInfo.copy(
          deregistrationDecisionDate = Some(yesterday)
        )

        val service: CoreSavedAnswersRevalidationService =
          new CoreSavedAnswersRevalidationService(mockCoreRegistrationValidationService, stubClockAtArbitraryDate)

        val privateMethodCall = PrivateMethod[Boolean](Symbol("checkVrnExpired"))

        val result = service invokePrivate privateMethodCall(Some(vatCustomerInfoWithDeregistration))

        result `mustBe` true
      }
    }
  }
}
