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

package controllers.auth

import base.SpecBase
import config.FrontendAppConfig
import connectors.{RegistrationConnector, SaveForLaterConnector}
import controllers.auth.routes as authRoutes
import models.checkVatDetails.VatApiCallResult
import models.domain.VatCustomerInfo
import models.responses.NotFound
import models.{DesAddress, UserAnswers, responses}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.checkVatDetails.{CheckVatDetailsPage, VatApiDownPage}
import pages.filters.BusinessBasedInNiOrEuPage
import pages.{ContinueRegistrationPage, NoRegistrationInProgressPage, SavedProgressPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.VatApiCallResultQuery
import repositories.AuthenticatedUserAnswersRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}
import utils.FutureSyntax.FutureOps
import views.html.auth.{InsufficientEnrolmentsView, UnsupportedAffinityGroupView, UnsupportedAuthProviderView, UnsupportedCredentialRoleView}

import java.net.URLEncoder
import java.time.{Instant, LocalDate}

class AuthControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockAuthenticatedUserAnswersRepository: AuthenticatedUserAnswersRepository = mock[AuthenticatedUserAnswersRepository]
  private val mockSaveForLaterConnector: SaveForLaterConnector = mock[SaveForLaterConnector]

  private val continueUrl = "http://localhost/foo"

  override def beforeEach(): Unit = {
    reset(mockAuthenticatedUserAnswersRepository)
    reset(mockRegistrationConnector)
    reset(mockSaveForLaterConnector)
  }

  "AuthController" - {

    ".onSignIn" - {

      "when we already have some user answers" - {

        "and there are saved user answers" - {

          "must redirect to the Continue Registration page" in {

            val continueUrl: RedirectUrl = RedirectUrl("/continueUrl")

            val updatedAnswers: UserAnswers = emptyUserAnswers
              .set(SavedProgressPage, continueUrl.get(OnlyRelative).url).success.value

            val application = applicationBuilder(Some(updatedAnswers))
              .overrides(
                bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository)
              ).build()

            when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

            running(application) {

              val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
              val result = route(application, request).value

              status(result) `mustBe` SEE_OTHER
              redirectLocation(result).value `mustBe` ContinueRegistrationPage.route(waypoints).url
              verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
              verifyNoInteractions(mockSaveForLaterConnector)
            }
          }

          "must use the request user answers and then redirect to the Confirm Vat Details page when the " +
            "Saved Users Answers retrieval fails" in {

            val application = applicationBuilder(Some(emptyUserAnswersWithVatInfo))
              .overrides(
                bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository),
                bind[RegistrationConnector].toInstance(mockRegistrationConnector),
                bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector)
              ).build()

            when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture
            when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(vatCustomerInfo).toFuture
            when(mockSaveForLaterConnector.get()(any())) thenReturn Left(NotFound).toFuture

            running(application) {

              val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
              val result = route(application, request).value

              val expectedAnswers: UserAnswers = emptyUserAnswersWithVatInfo
                .set(VatApiCallResultQuery, VatApiCallResult.Success).success.value

              status(result) `mustBe` SEE_OTHER
              redirectLocation(result).value `mustBe` CheckVatDetailsPage.route(waypoints).url
              verify(mockSaveForLaterConnector, times(1)).get()(any())
              verify(mockRegistrationConnector, times(1)).getVatCustomerInfo()(any())
              verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
            }
          }
        }

        "and we have made a call to get VAT info" - {

          "must redirect to the next page without making calls to get data or updating the users answers" in {

            val answers = emptyUserAnswers.set(VatApiCallResultQuery, VatApiCallResult.Success).success.value

            val application = applicationBuilder(Some(answers))
              .overrides(
                bind[RegistrationConnector].toInstance(mockRegistrationConnector),
                bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
                bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository)
              ).build()

            when(mockSaveForLaterConnector.get()(any())) thenReturn Right(None).toFuture

            running(application) {
              val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
              val result = route(application, request).value

              status(result) `mustBe` SEE_OTHER
              redirectLocation(result).value `mustBe` CheckVatDetailsPage.route(waypoints).url
              verify(mockSaveForLaterConnector, times(1)).get()(any())
              verifyNoInteractions(mockRegistrationConnector)
              verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
            }
          }
        }

        "and we have not yet made a call to get VAT info" - {

          "and we can find their VAT details" - {

            val niDesAddress: DesAddress = DesAddress(
              "1 The Street",
              Some("Some Town"),
              None,
              None,
              None,
              Some("BT11 1AA"),
              "GB"
            )

            val niVatInfo: VatCustomerInfo =
              VatCustomerInfo(
                registrationDate = LocalDate.now(stubClockAtArbitraryDate),
                desAddress = niDesAddress,
                organisationName = Some("Company name"),
                individualName = None,
                singleMarketIndicator = true,
                deregistrationDecisionDate = None
              )

            val userAnswersWithNiVatInfo: UserAnswers = emptyUserAnswers.copy(vatInfo = Some(niVatInfo))

            "must create user answers with their VAT details, then redirect to the next page" in {

              val application = applicationBuilder(Some(emptyUserAnswers))
                .overrides(
                  bind[RegistrationConnector].toInstance(mockRegistrationConnector),
                  bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
                  bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository)
                ).build()

              when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(niVatInfo).toFuture
              when(mockSaveForLaterConnector.get()(any())) thenReturn Right(None).toFuture
              when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

              running(application) {

                val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
                val result = route(application, request).value

                val expectedAnswers = userAnswersWithNiVatInfo.set(VatApiCallResultQuery, VatApiCallResult.Success).success.value

                status(result) `mustBe` SEE_OTHER
                redirectLocation(result).value `mustBe` CheckVatDetailsPage.route(waypoints).url
                verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
                verify(mockSaveForLaterConnector, times(1)).get()(any())
              }
            }

            "and the de-registration date is today or before" - {

              "must redirect to Expired Vrn Date page" in {

                val application = applicationBuilder(Some(emptyUserAnswers))
                  .overrides(
                    bind[RegistrationConnector].toInstance(mockRegistrationConnector),
                    bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
                    bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository)
                  )
                  .build()
                val expiredVrnVatInfo = vatCustomerInfo.copy(deregistrationDecisionDate = Some(LocalDate.now(stubClockAtArbitraryDate)))

                when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(expiredVrnVatInfo).toFuture
                when(mockSaveForLaterConnector.get()(any())) thenReturn Right(None).toFuture
                when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn false.toFuture

                running(application) {

                  val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
                  val result = route(application, request).value

                  status(result) mustBe SEE_OTHER

                  redirectLocation(result).value `mustBe` controllers.routes.ExpiredVrnDateController.onPageLoad(waypoints).url
                  verify(mockSaveForLaterConnector, times(1)).get()(any())
                  verifyNoInteractions(mockAuthenticatedUserAnswersRepository)
                }
              }
            }

            "and the de-registration date is later than today" - {

              "must create user answers with their VAT details, then redirect to the next page" in {

                val answers = userAnswersWithNiVatInfo.set(BusinessBasedInNiOrEuPage, true).success.value
                val application = applicationBuilder(Some(answers))
                  .overrides(
                    bind[RegistrationConnector].toInstance(mockRegistrationConnector),
                    bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
                    bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository)
                  )
                  .build()

                val nonExpiredVrnVatInfo = niVatInfo.copy(
                  singleMarketIndicator = true,
                  deregistrationDecisionDate = Some(LocalDate.now(stubClockAtArbitraryDate).plusDays(1))
                )

                when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Right(nonExpiredVrnVatInfo).toFuture
                when(mockSaveForLaterConnector.get()(any())) thenReturn Right(None).toFuture
                when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

                running(application) {

                  val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
                  val result = route(application, request).value

                  val expectedAnswers = emptyUserAnswersWithVatInfo.copy(vatInfo = Some(nonExpiredVrnVatInfo))
                    .set(BusinessBasedInNiOrEuPage, true).success.value
                    .set(VatApiCallResultQuery, VatApiCallResult.Success).success.value

                  status(result) mustBe SEE_OTHER
                  redirectLocation(result).value mustBe CheckVatDetailsPage.route(waypoints).url
                  verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
                  verify(mockSaveForLaterConnector, times(1)).get()(any())
                }
              }
            }
          }

          "and we cannot find their VAT details" - {

            "must redirect to VAT API down page" in {

              val application = applicationBuilder(Some(emptyUserAnswers))
                .overrides(
                  bind[RegistrationConnector].toInstance(mockRegistrationConnector),
                  bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
                  bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository)
                ).build()

              when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Left(NotFound).toFuture
              when(mockSaveForLaterConnector.get()(any())) thenReturn Right(None).toFuture
              when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

              running(application) {

                val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
                val result = route(application, request).value

                val expectedAnswers = emptyUserAnswers.set(VatApiCallResultQuery, VatApiCallResult.Error).success.value

                status(result) `mustBe` SEE_OTHER
                redirectLocation(result).value `mustBe` VatApiDownPage.route(waypoints).url
                verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
                verify(mockSaveForLaterConnector, times(1)).get()(any())
              }
            }
          }

          "and the call to get their VAT details fails" - {

            val failureResponse = responses.UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "foo")

            "must return an internal server error" in {

              val application = applicationBuilder(None)
                .overrides(
                  bind[RegistrationConnector].toInstance(mockRegistrationConnector),
                  bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
                  bind[AuthenticatedUserAnswersRepository].toInstance(mockAuthenticatedUserAnswersRepository)
                ).build()

              when(mockRegistrationConnector.getVatCustomerInfo()(any())) thenReturn Left(failureResponse).toFuture
              when(mockSaveForLaterConnector.get()(any())) thenReturn Right(None).toFuture
              when(mockAuthenticatedUserAnswersRepository.set(any())) thenReturn true.toFuture

              running(application) {

                val request = FakeRequest(GET, authRoutes.AuthController.onSignIn().url)
                val result = route(application, request).value

                val expectedAnswers = emptyUserAnswers.set(VatApiCallResultQuery, VatApiCallResult.Error).success.value

                status(result) `mustBe` SEE_OTHER
                redirectLocation(result).value `mustBe` VatApiDownPage.route(waypoints).url
                verify(mockAuthenticatedUserAnswersRepository, times(1)).set(eqTo(expectedAnswers))
                verify(mockSaveForLaterConnector, times(1)).get()(any())
              }
            }
          }
        }
      }
    }

    ".continueOnSignIn" - {

      "must redirect to the Continue Registration page when the saved progress url was retrieved from saved user answers" in {

        val continueUrl: RedirectUrl = RedirectUrl("/continueUrl")

        val answers: UserAnswers = emptyUserAnswers
          .set(VatApiCallResultQuery, VatApiCallResult.Success).success.value
          .set(SavedProgressPage, continueUrl.get(OnlyRelative).url).success.value

        val application = applicationBuilder(Some(answers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.AuthController.continueOnSignIn().url)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` ContinueRegistrationPage.route(waypoints).url
        }
      }

      "must redirect to No Registration In Progress when there are no saved answers" in {

        val application = applicationBuilder(None)
          .overrides(bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector))
          .build()

        when(mockSaveForLaterConnector.get()(any())) thenReturn Right(None).toFuture

        running(application) {
          val request = FakeRequest(GET, routes.AuthController.continueOnSignIn().url)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` NoRegistrationInProgressPage.route(waypoints).url
          verify(mockSaveForLaterConnector, times(1)).get()(any())
        }
      }
    }

    ".redirectToRegister" - {

      "must redirect the user to bas-gateway to register" in {

        val application = applicationBuilder(Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.AuthController.redirectToRegister(RedirectUrl("http://localhost/foo")).url)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER

          redirectLocation(result).value `mustBe` "http://localhost:9553/bas-gateway/register?origin=IOSS-Intermediary&continueUrl=http%3A%2F%2Flocalhost%2Ffoo&accountType=Organisation"
        }
      }
    }

    ".redirectToLogin" - {

      "must redirect the user to bas-gateway to log in" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.AuthController.redirectToLogin(RedirectUrl("http://localhost/foo")).url)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER

          redirectLocation(result).value `mustBe` "http://localhost:9553/bas-gateway/sign-in?origin=IOSS-Intermediary&continue=http%3A%2F%2Flocalhost%2Ffoo"
        }
      }
    }

    ".signOut" - {

      "must redirect to sign out, specifying the exit survey as the continue URL" in {

        val application = applicationBuilder(None).build()

        running(application) {

          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val request = FakeRequest(GET, routes.AuthController.signOut().url)

          val result = route(application, request).value

          val encodedContinueUrl = URLEncoder.encode(appConfig.exitSurveyUrl, "UTF-8")
          val expectedRedirectUrl = s"${appConfig.signOutUrl}?continue=$encodedContinueUrl"

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` expectedRedirectUrl
        }
      }
    }

    ".signOutNoSurvey" - {

      "must redirect to sign out, specifying SignedOut as the continue URL" in {

        val application = applicationBuilder(None).build()

        running(application) {

          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val request = FakeRequest(GET, routes.AuthController.signOutNoSurvey().url)

          val result = route(application, request).value

          val encodedContinueUrl = URLEncoder.encode(routes.SignedOutController.onPageLoad().url, "UTF-8")
          val expectedRedirectUrl = s"${appConfig.signOutUrl}?continue=$encodedContinueUrl"

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` expectedRedirectUrl
        }
      }
    }

    ".unsupportedAffinityGroup" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {

          val request = FakeRequest(GET, routes.AuthController.unsupportedAffinityGroup().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[UnsupportedAffinityGroupView]

          status(result) `mustBe` OK

          contentAsString(result) `mustBe` view()(request, messages(application)).toString()
        }
      }

    }

    ".unsupportedAuthProvider" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {

          val request = FakeRequest(GET, routes.AuthController.unsupportedAuthProvider(RedirectUrl("http://localhost/foo")).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[UnsupportedAuthProviderView]

          status(result) `mustBe` OK

          contentAsString(result) `mustBe` view(RedirectUrl(continueUrl))(request, messages(application)).toString
        }
      }
    }

    ".insufficientEnrolments" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {

          val request = FakeRequest(GET, routes.AuthController.insufficientEnrolments().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[InsufficientEnrolmentsView]

          status(result) `mustBe` OK

          contentAsString(result) `mustBe` view()(request, messages(application)).toString()
        }
      }
    }

    ".unsupportedCredentialRole" - {

      "must return OK and the correct view" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {

          val request = FakeRequest(GET, routes.AuthController.unsupportedCredentialRole().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[UnsupportedCredentialRoleView]

          status(result) `mustBe` OK

          contentAsString(result) `mustBe` view()(request, messages(application)).toString()
        }
      }
    }
  }
}
