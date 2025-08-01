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

import models.checkVatDetails.CheckVatDetails
import models.euDetails.RegistrationType
import models.{Country, InternationalAddress, UkAddress}
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import pages.checkVatDetails.{CheckVatDetailsPage, NiAddressPage}
import pages.euDetails.*
import pages.previousIntermediaryRegistrations.*
import pages.tradingNames.{AddTradingNamePage, DeleteAllTradingNamesPage, TradingNamePage}
import play.api.libs.json.{JsValue, Json}

trait UserAnswersEntryGenerators extends PageGenerators with ModelGenerators {

  implicit lazy val arbitraryDeleteAllTradingNamesUserAnswersEntry: Arbitrary[(DeleteAllTradingNamesPage.type, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[DeleteAllTradingNamesPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryAddTradingNameUserAnswersEntry: Arbitrary[(AddTradingNamePage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[AddTradingNamePage]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryTradingNameUserAnswersEntry: Arbitrary[(TradingNamePage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[TradingNamePage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryCheckVatDetailsUserAnswersEntry: Arbitrary[(CheckVatDetailsPage.type, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[CheckVatDetailsPage.type]
        value <- arbitrary[CheckVatDetails].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryHasPreviouslyRegisteredAsIntermediaryUserAnswersEntry: Arbitrary[(HasPreviouslyRegisteredAsIntermediaryPage.type, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[HasPreviouslyRegisteredAsIntermediaryPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryPreviousEuCountryUserAnswersEntry: Arbitrary[(PreviousEuCountryPage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[PreviousEuCountryPage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryPreviousIntermediaryRegistrationNumberUserAnswersEntry: Arbitrary[(PreviousIntermediaryRegistrationNumberPage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[PreviousIntermediaryRegistrationNumberPage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryAddPreviousIntermediaryRegistrationUserAnswersEntry: Arbitrary[(AddPreviousIntermediaryRegistrationPage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[AddPreviousIntermediaryRegistrationPage]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryDeleteAllPreviousIntermediaryRegistrationsUserAnswersEntry: Arbitrary[(DeleteAllPreviousIntermediaryRegistrationsPage.type, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[DeleteAllPreviousIntermediaryRegistrationsPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
  }
  
  implicit lazy val arbitraryEuCountryUserAnswersEntry: Arbitrary[(EuCountryPage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[EuCountryPage]
        value <- arbitrary[Country].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryHasFixedEstablishmentUserAnswersEntry: Arbitrary[(HasFixedEstablishmentPage.type, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[HasFixedEstablishmentPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryRegistrationTypeUserAnswersEntry: Arbitrary[(RegistrationTypePage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[RegistrationTypePage]
        value <- arbitrary[RegistrationType].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryEuVatNumberUserAnswersEntry: Arbitrary[(EuVatNumberPage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[EuVatNumberPage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryEuTaxReferenceUserAnswersEntry: Arbitrary[(EuTaxReferencePage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[EuTaxReferencePage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }
  }
  
  implicit lazy val arbitraryFixedEstablishmentAddressUserAnswersEntry: Arbitrary[(FixedEstablishmentAddressPage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[FixedEstablishmentAddressPage]
        value <- arbitrary[InternationalAddress].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryAddEuDetailsUserAnswersEntry: Arbitrary[(AddEuDetailsPage, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[AddEuDetailsPage]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryDeleteAllEuDetailsUserAnswersEntry: Arbitrary[(DeleteAllEuDetailsPage.type, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[DeleteAllEuDetailsPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
  }

  implicit lazy val arbitraryNiAddressUserAnswersEntry: Arbitrary[(NiAddressPage.type, JsValue)] = {
    Arbitrary {
      for {
        page <- arbitrary[NiAddressPage.type]
        value <- arbitrary[UkAddress].map(Json.toJson(_))
      } yield (page, value)
    }
  }
}

