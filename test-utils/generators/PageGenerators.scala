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

package generators

import models.Index
import org.scalacheck.Arbitrary
import pages.*
import pages.euDetails.*
import pages.tradingNames.{AddTradingNamePage, DeleteAllTradingNamesPage, TradingNamePage}

trait PageGenerators {

  implicit lazy val arbitraryDeleteAllTradingNamesPage: Arbitrary[DeleteAllTradingNamesPage.type] = {
    Arbitrary(DeleteAllTradingNamesPage)
  }

  implicit lazy val arbitraryAddTradingNamePage: Arbitrary[AddTradingNamePage] = {
    Arbitrary(AddTradingNamePage(Some(Index(1))))
  }

  implicit lazy val arbitraryTradingNamePage: Arbitrary[TradingNamePage] = {
    Arbitrary(TradingNamePage(Index(0)))
  }

  implicit lazy val arbitraryTaxRegisteredInEuPage: Arbitrary[TaxRegisteredInEuPage.type] = {
    Arbitrary(TaxRegisteredInEuPage)
  }

  implicit lazy val arbitraryEuCountryPage: Arbitrary[EuCountryPage] = {
    Arbitrary(EuCountryPage(Index(0)))
  }

  implicit lazy val arbitraryHasFixedEstablishmentPage: Arbitrary[HasFixedEstablishmentPage] = {
    Arbitrary(HasFixedEstablishmentPage(Index(0)))
  }

  implicit lazy val arbitraryRegistrationTypePage: Arbitrary[RegistrationTypePage] = {
    Arbitrary(RegistrationTypePage(Index(0)))
  }

  implicit lazy val arbitraryEuVatNumberPage: Arbitrary[EuVatNumberPage] = {
    Arbitrary(EuVatNumberPage(Index(0)))
  }

  implicit lazy val arbitraryEuTaxReferencePage: Arbitrary[EuTaxReferencePage] = {
    Arbitrary(EuTaxReferencePage(Index(0)))
  }

  implicit lazy val arbitraryFixedEstablishmentTradingNamePage: Arbitrary[FixedEstablishmentTradingNamePage] = {
    Arbitrary(FixedEstablishmentTradingNamePage(Index(0)))
  }

  implicit lazy val arbitraryFixedEstablishmentAddressPage: Arbitrary[FixedEstablishmentAddressPage] = {
    Arbitrary(FixedEstablishmentAddressPage(Index(0)))
  }

  implicit lazy val arbitraryAddEuDetailsPage: Arbitrary[AddEuDetailsPage] = {
    Arbitrary(AddEuDetailsPage(Some(Index(0))))
  }
  
  implicit lazy val arbitraryDeleteAllEuDetailsPage: Arbitrary[DeleteAllEuDetailsPage.type] = {
    Arbitrary(DeleteAllEuDetailsPage)
  }
}

