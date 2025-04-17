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

import models.*
import models.enrolments.{EACDEnrolment, EACDEnrolments, EACDIdentifiers}
import models.domain.ModelHelpers.normaliseSpaces
import models.iossRegistration._
import models.ossRegistration._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen.{choose, listOfN}
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.domain.Vrn

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}

trait ModelGenerators {

  private val maxFieldLength: Int = 35

  def datesBetween(min: LocalDate, max: LocalDate): Gen[LocalDate] = {

    def toMillis(date: LocalDate): Long =
      date.atStartOfDay.atZone(ZoneOffset.UTC).toInstant.toEpochMilli

    Gen.choose(toMillis(min), toMillis(max)).map {
      millis =>
        Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate
    }
  }

  implicit lazy val arbitraryDate: Arbitrary[LocalDate] =
    Arbitrary {
      datesBetween(LocalDate.of(2021, 7, 1), LocalDate.of(2023, 12, 31))
    }

  implicit lazy val arbitraryTradingName: Arbitrary[TradingName] =
    Arbitrary {
      for {
        name <- commonFieldString(maxFieldLength)
      } yield {
        TradingName(name)
      }
    }

  implicit lazy val arbitraryIossEtmpDisplaySchemeDetails: Arbitrary[IossEtmpDisplaySchemeDetails] = {
    Arbitrary {
      for {
        contactName <- Gen.alphaStr
        businessTelephoneNumber <- Gen.alphaStr
        businessEmailId <- Gen.alphaStr
      } yield IossEtmpDisplaySchemeDetails(
        contactName = contactName,
        businessTelephoneNumber = businessTelephoneNumber,
        businessEmailId = businessEmailId
      )
    }
  }

  implicit lazy val arbitraryBic: Arbitrary[Bic] = {
    val asciiCodeForA = 65
    val asciiCodeForN = 78
    val asciiCodeForP = 80
    val asciiCodeForZ = 90

    Arbitrary {
      for {
        firstChars <- Gen.listOfN(6, Gen.alphaUpperChar).map(_.mkString)
        char7 <- Gen.oneOf(Gen.alphaUpperChar, Gen.choose(2, 9).map(_.toString.head))
        char8 <- Gen.oneOf(
          Gen.choose(asciiCodeForA, asciiCodeForN).map(_.toChar),
          Gen.choose(asciiCodeForP, asciiCodeForZ).map(_.toChar),
          Gen.choose(0, 9).map(_.toString.head)
        )
        lastChars <- Gen.option(Gen.listOfN(3, Gen.oneOf(Gen.alphaUpperChar, Gen.numChar)).map(_.mkString))
      } yield Bic(s"$firstChars$char7$char8${lastChars.getOrElse("")}").get
    }
  }
  implicit lazy val arbitraryIban: Arbitrary[Iban] =
    Arbitrary {
      Gen.oneOf(
        "GB94BARC10201530093459",
        "GB33BUKB20201555555555",
        "DE29100100100987654321",
        "GB24BKEN10000031510604",
        "GB27BOFI90212729823529",
        "GB17BOFS80055100813796",
        "GB92BARC20005275849855",
        "GB66CITI18500812098709",
        "GB15CLYD82663220400952",
        "GB26MIDL40051512345674",
        "GB76LOYD30949301273801",
        "GB25NWBK60080600724890",
        "GB60NAIA07011610909132",
        "GB29RBOS83040210126939",
        "GB79ABBY09012603367219",
        "GB21SCBL60910417068859",
        "GB42CPBK08005470328725"
      ).map(v => Iban(v).toOption.get)
    }

  implicit val arbitraryIossEtmpExclusion: Arbitrary[IossEtmpExclusion] = {
    Arbitrary {
      for {
        exclusionReason <- Gen.oneOf(IossEtmpExclusionReason.values)
        effectiveDate <- datesBetween(LocalDate.of(2021, 7, 1), LocalDate.of(2022, 12, 31))
        decisionDate <- datesBetween(LocalDate.of(2021, 7, 1), LocalDate.of(2022, 12, 31))
        quarantine <- arbitrary[Boolean]
      } yield IossEtmpExclusion(
        exclusionReason = exclusionReason,
        effectiveDate = effectiveDate,
        decisionDate = decisionDate,
        quarantine = quarantine
      )
    }
  }
  
  implicit lazy val arbitraryIossEtmpTradingName: Arbitrary[IossEtmpTradingName] = {
    Arbitrary {
      for {
        tradingName <- Gen.alphaStr
      } yield IossEtmpTradingName(tradingName)
    }
  }
  
  implicit lazy val arbitraryIossEtmpBankDetails: Arbitrary[IossEtmpBankDetails] = {
    Arbitrary {
      for {
        accountName <- Gen.alphaStr
        bic <- Gen.option(arbitrary[Bic])
        iban <- arbitrary[Iban]
      } yield IossEtmpBankDetails(accountName, bic, iban)
    }
  }
  
  implicit lazy val arbitraryIossEtmpDisplayRegistration: Arbitrary[IossEtmpDisplayRegistration] = {
    Arbitrary {
      for {
        tradingNames <- Gen.listOfN(3, arbitraryIossEtmpTradingName.arbitrary)
        schemeDetails <- arbitraryIossEtmpDisplaySchemeDetails.arbitrary
        bankDetails <- arbitraryIossEtmpBankDetails.arbitrary
        exclusions <- arbitraryIossEtmpExclusion.arbitrary
      } yield IossEtmpDisplayRegistration(
        tradingNames = tradingNames,
        schemeDetails = schemeDetails,
        bankDetails = bankDetails,
        exclusions = Seq(exclusions)
      )
    }
  }

  private def commonFieldString(maxLength: Int): Gen[String] = (for {
    length <- choose(1, maxLength)
    chars <- listOfN(length, commonFieldSafeInputs)
  } yield chars.mkString).retryUntil(_.trim.nonEmpty)

  private def commonFieldSafeInputs: Gen[Char] = Gen.oneOf(
    Gen.alphaNumChar,
    Gen.oneOf('À' to 'ÿ'),
    Gen.const('.'),
    Gen.const(','),
    Gen.const('/'),
    Gen.const('’'),
    Gen.const('\''),
    Gen.const('"'),
    Gen.const('_'),
    Gen.const('&'),
    Gen.const(' '),
    Gen.const('\'')
  )

  implicit lazy val arbitraryEACDIdentifiers: Arbitrary[EACDIdentifiers] = {
    Arbitrary {
      for {
        key <- Gen.alphaStr
        value <- Gen.alphaStr
      } yield EACDIdentifiers(
        key = key,
        value = value
      )
    }
  }

  implicit lazy val arbitraryEACDEnrolment: Arbitrary[EACDEnrolment] = {
    Arbitrary {
      for {
        service <- Gen.alphaStr
        state <- Gen.alphaStr
        identifiers <- Gen.listOfN(3, arbitraryEACDIdentifiers.arbitrary)
      } yield EACDEnrolment(
        service = service,
        state = state,
        activationDate = Some(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)),
        identifiers = identifiers
      )
    }
  }

  implicit lazy val arbitraryEACDEnrolments: Arbitrary[EACDEnrolments] = {
    Arbitrary {
      for {
        enrolments <- Gen.listOfN(3, arbitraryEACDEnrolment.arbitrary)
      } yield EACDEnrolments(
        enrolments = enrolments
      )
    }
  }

  implicit lazy val arbitraryOssVatDetails: Arbitrary[OssVatDetails] =
    Arbitrary {
      for {
        registrationDate <- arbitrary[Int].map(n => LocalDate.ofEpochDay(n))
        address <- arbitrary[Address]
        partOfVatGroup <- arbitrary[Boolean]
        source <- arbitrary[OssVatDetailSource]
      } yield OssVatDetails(registrationDate, address, partOfVatGroup, source)
    }

  implicit val arbitraryOssVatDetailSource: Arbitrary[OssVatDetailSource] =
    Arbitrary(
      Gen.oneOf(OssVatDetailSource.values)
    )

  implicit lazy val arbitraryCountry: Arbitrary[Country] =
    Arbitrary {
      Gen.oneOf(Country.euCountries)
    }

  implicit lazy val arbitraryOssBusinessContactDetails: Arbitrary[OssContactDetails] =
    Arbitrary {
      for {
        fullName <- arbitrary[String]
        telephoneNumber <- arbitrary[String]
        emailAddress <- arbitrary[String]
      } yield OssContactDetails(fullName, telephoneNumber, emailAddress)
    }

  implicit lazy val arbitraryBankDetails: Arbitrary[BankDetails] =
    Arbitrary {
      for {
        accountName <- arbitrary[String]
        bic <- Gen.option(arbitrary[Bic])
        iban <- arbitrary[Iban]
      } yield BankDetails(accountName, bic, iban)
    }

  implicit lazy val arbitraryInternationalAddress: Arbitrary[InternationalAddress] =
    Arbitrary {
      for {
        line1 <- commonFieldString(maxFieldLength)
        line2 <- Gen.option(commonFieldString(maxFieldLength))
        townOrCity <- commonFieldString(maxFieldLength)
        stateOrRegion <- Gen.option(commonFieldString(maxFieldLength))
        postCode <- Gen.option(arbitrary[String])
        country <- Gen.oneOf(Country.internationalCountries)
      } yield InternationalAddress(
        normaliseSpaces(line1),
        normaliseSpaces(line2),
        normaliseSpaces(townOrCity),
        normaliseSpaces(stateOrRegion),
        normaliseSpaces(postCode),
        country
      )
    }

  implicit lazy val arbitraryDesAddress: Arbitrary[DesAddress] =
    Arbitrary {
      for {
        line1 <- commonFieldString(maxFieldLength)
        line2 <- Gen.option(commonFieldString(maxFieldLength))
        line3 <- Gen.option(commonFieldString(maxFieldLength))
        line4 <- Gen.option(commonFieldString(maxFieldLength))
        line5 <- Gen.option(commonFieldString(maxFieldLength))
        postCode <- Gen.option(arbitrary[String])
        country <- Gen.oneOf(Country.internationalCountries.map(_.code))
      } yield DesAddress(
        normaliseSpaces(line1),
        normaliseSpaces(line2),
        normaliseSpaces(line3),
        normaliseSpaces(line4),
        normaliseSpaces(line5),
        normaliseSpaces(postCode),
        country
      )
    }

  implicit val arbitraryAddress: Arbitrary[Address] =
    Arbitrary {
      Gen.oneOf(
        arbitrary[InternationalAddress],
        arbitrary[DesAddress]
      )
    }

  implicit lazy val arbitraryAdminUse: Arbitrary[OssAdminUse] =
    Arbitrary {
      for {
        changeDate <- arbitrary[LocalDateTime]
      } yield OssAdminUse(Some(changeDate))
    }

  implicit lazy val arbitraryVrn: Arbitrary[Vrn] =
    Arbitrary {
      for {
        chars <- Gen.listOfN(9, Gen.numChar)
      } yield Vrn(chars.mkString(""))
    }

  implicit val arbitraryOssRegistration: Arbitrary[OssRegistration] = {
    Arbitrary {
      for {
        vrn <- arbitraryVrn.arbitrary
        name <- arbitrary[String]
        vatDetails <- arbitrary[OssVatDetails]
        contactDetails <- arbitrary[OssContactDetails]
        bankDetails <- arbitrary[BankDetails]
        commencementDate <- datesBetween(LocalDate.of(2021, 7, 1), LocalDate.now)
        isOnlineMarketplace <- arbitrary[Boolean]
        adminUse <- arbitrary[OssAdminUse]
      } yield OssRegistration(vrn, name, Nil, vatDetails, Nil, contactDetails, Nil, commencementDate, Nil, bankDetails, isOnlineMarketplace, None, None, None, None, None, None, None, None, adminUse)
    }
  }

  implicit lazy val arbitrarySalesChannels: Arbitrary[SalesChannels] =
    Arbitrary {
      Gen.oneOf(SalesChannels.values)
    }

  implicit lazy val arbitraryFixedEstablishment: Arbitrary[OssTradeDetails] =
    Arbitrary {
      for {
        tradingName <- arbitrary[String]
        address <- arbitrary[InternationalAddress]
      } yield OssTradeDetails(tradingName, address)
    }

  implicit val arbitraryEuTaxIdentifierType: Arbitrary[OssEuTaxIdentifierType] =
    Arbitrary {
      Gen.oneOf(OssEuTaxIdentifierType.values)
    }

  implicit val arbitraryEuTaxIdentifier: Arbitrary[OssEuTaxIdentifier] =
    Arbitrary {
      for {
        identifierType <- arbitrary[OssEuTaxIdentifierType]
        value <- arbitrary[Int].map(_.toString)
      } yield OssEuTaxIdentifier(identifierType, value)
    }
}
