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

package models.emailVerfication

import base.SpecBase
import models.emailVerification.{EmailVerificationRequest, VerifyEmail}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsSuccess, Json}


class EmailVerificationRequestSpec extends AnyFreeSpec with Matchers with SpecBase {

  "EmailVerificationRequest" - {

    "must serialise and deserialise to and from a EmailVerificationRequest" - {

      "with all optional fields present" in {

        val emailVerificationRequest: EmailVerificationRequest =
          EmailVerificationRequest(
            credId = "12345-credId",
            continueUrl = "/pay-clients-vat-on-eu-sales/register-import-one-stop-shop-intermediary/bank-account-details",
            origin = "IOSS-Intermediary",
            deskproServiceName = Some("ioss-intermediary-registration-frontend"),
            accessibilityStatementUrl = "/register-import-one-stop-shop-intermediary",
            pageTitle = Some("Register to manage your clients’ Import One Stop Shop VAT"),
            backUrl = Some("/pay-clients-vat-on-eu-sales/register-import-one-stop-shop-intermediary/contact-details"),
            email = Some(VerifyEmail(
              "email@example.com",
              "/pay-clients-vat-on-eu-sales/register-import-one-stop-shop-intermediary/contact-details"
            ))
          )

        val expectedJson = Json.obj(
          "credId" -> "12345-credId",
          "continueUrl" -> "/pay-clients-vat-on-eu-sales/register-import-one-stop-shop-intermediary/bank-account-details",
          "origin" -> "IOSS-Intermediary",
          "deskproServiceName" -> "ioss-intermediary-registration-frontend",
          "accessibilityStatementUrl" -> "/register-import-one-stop-shop-intermediary",
          "pageTitle" -> "Register to manage your clients’ Import One Stop Shop VAT",
          "backUrl" -> "/pay-clients-vat-on-eu-sales/register-import-one-stop-shop-intermediary/contact-details",
          "email" -> Json.obj(
            "address" -> "email@example.com",
            "enterUrl" -> "/pay-clients-vat-on-eu-sales/register-import-one-stop-shop-intermediary/contact-details"
          ),
          "lang" -> "en"
        )

        Json.toJson(emailVerificationRequest) mustEqual expectedJson
        expectedJson.validate[EmailVerificationRequest] mustEqual JsSuccess(emailVerificationRequest)
      }

      "with all optional fields missing" in {
        val emailVerificationRequest: EmailVerificationRequest =
          EmailVerificationRequest(
            credId = "12345-credId",
            continueUrl = "/pay-clients-vat-on-eu-sales/register-import-one-stop-shop-intermediary/bank-account-details",
            origin = "IOSS-Intermediary",
            None,
            accessibilityStatementUrl = "/register-import-one-stop-shop-intermediary",
            None,
            None,
            None
          )

        val expectedJson = Json.obj(
          "credId" -> "12345-credId",
          "continueUrl" -> "/pay-clients-vat-on-eu-sales/register-import-one-stop-shop-intermediary/bank-account-details",
          "origin" -> "IOSS-Intermediary",
          "accessibilityStatementUrl" -> "/register-import-one-stop-shop-intermediary",
          "lang" -> "en"
        )

        Json.toJson(emailVerificationRequest) mustEqual expectedJson
        expectedJson.validate[EmailVerificationRequest] mustEqual JsSuccess(emailVerificationRequest)
      }


      "must handle missing fields during deserialization" in {

        val expectedJson = Json.obj()

        expectedJson.validate[EmailVerificationRequest] mustBe a[JsError]
      }

      "must handle invalid data during deserialization" in {

        val expectedJson = Json.obj(
          "credId" -> 12345,
          "continueUrl" -> "/pay-clients-vat-on-eu-sales/register-import-one-stop-shop-intermediary/bank-account-details",
          "origin" -> "IOSS-Intermediary",
          "accessibilityStatementUrl" -> "/register-import-one-stop-shop-intermediary",
          "lang" -> "en"
        )

        expectedJson.validate[EmailVerificationRequest] mustBe a[JsError]
      }
    }
  }

  "verifyEmail" - {

    "must serialize to JSON correctly" in {
      val verifyEmail = VerifyEmail(
        "email@example.com",
        "/pay-clients-vat-on-eu-sales/register-import-one-stop-shop-intermediary/contact-details"
      )

      val expectedJson = Json.obj(
        "address" -> "email@example.com",
        "enterUrl" -> "/pay-clients-vat-on-eu-sales/register-import-one-stop-shop-intermediary/contact-details"
      )

      Json.toJson(verifyEmail) mustBe expectedJson
    }

    "must deserialize from JSON correctly" in {

      val expectedJson = Json.obj(
        "address" -> "email@example.com",
        "enterUrl" -> "/pay-clients-vat-on-eu-sales/register-import-one-stop-shop-intermediary/contact-details"
      )

      val verifyEmail = VerifyEmail(
        "email@example.com",
        "/pay-clients-vat-on-eu-sales/register-import-one-stop-shop-intermediary/contact-details"
      )

      expectedJson.validate[VerifyEmail] mustBe JsSuccess(verifyEmail)
    }

    "must handle missing fields during deserialization" in {

      val expectedJson = Json.obj()

      expectedJson.validate[VerifyEmail] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val expectedJson = Json.obj(
        "address" -> 12345,
        "enterUrl" -> "/pay-clients-vat-on-eu-sales/register-import-one-stop-shop-intermediary/contact-details"
      )

      expectedJson.validate[VerifyEmail] mustBe a[JsError]
    }
  }

}
