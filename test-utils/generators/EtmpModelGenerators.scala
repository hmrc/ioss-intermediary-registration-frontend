package generators

import models.etmp.*
import org.scalacheck.{Arbitrary, Gen}

trait EtmpModelGenerators {
  self: ModelGenerators =>

  implicit lazy val arbitraryEtmpAdministration: Arbitrary[EtmpAdministration] =
    Arbitrary {
      for {
        messageType <- Gen.oneOf(EtmpMessageType.values)
      } yield EtmpAdministration(messageType, "IOSS")
    }

  implicit lazy val arbitraryEtmpCustomerIdentification: Arbitrary[EtmpCustomerIdentification] =
    Arbitrary {
      for {
        etmpIdType <- Gen.oneOf(EtmpIdType.values)
        vrn <- Gen.alphaStr
      } yield EtmpCustomerIdentification(etmpIdType, vrn)
    }

  implicit lazy val arbitraryVatNumberTraderId: Arbitrary[VatNumberTraderId] =
    Arbitrary {
      for {
        vatNumber <- Gen.alphaNumStr
      } yield VatNumberTraderId(vatNumber)
    }

  implicit lazy val arbitraryTaxRefTraderID: Arbitrary[TaxRefTraderID] =
    Arbitrary {
      for {
        taxReferenceNumber <- Gen.alphaNumStr
      } yield TaxRefTraderID(taxReferenceNumber)
    }

  implicit lazy val arbitraryEtmpTradingName: Arbitrary[EtmpTradingName] =
    Arbitrary {
      for {
        tradingName <- Gen.alphaStr
      } yield EtmpTradingName(tradingName)
    }

  implicit lazy val arbitraryEtmpOtherIossIntermediaryRegistrations: Arbitrary[EtmpOtherIossIntermediaryRegistrations] =
    Arbitrary {
      for {
        countryCode <- Gen.listOfN(2, Gen.alphaChar).map(_.mkString)
        intermediaryNumber <- Gen.listOfN(12, Gen.alphaChar).map(_.mkString)
      } yield EtmpOtherIossIntermediaryRegistrations(countryCode, intermediaryNumber)
    }

  implicit lazy val arbitraryEtmpIntermediaryDetails: Arbitrary[EtmpIntermediaryDetails] =
    Arbitrary {
      for {
        amountOfOtherRegistrations <- Gen.chooseNum(1, 5)
        otherRegistrationDetails <- Gen.listOfN(amountOfOtherRegistrations, arbitraryEtmpOtherIossIntermediaryRegistrations.arbitrary)
      } yield EtmpIntermediaryDetails(otherRegistrationDetails)
    }

  implicit lazy val arbitraryEtmpOtherAddress: Arbitrary[EtmpOtherAddress] =
    Arbitrary {
      for {
        issuedBy <- Gen.listOfN(2, Gen.alphaChar).map(_.mkString)
        tradingName <- Gen.listOfN(20, Gen.alphaChar).map(_.mkString)
        addressLine1 <- Gen.listOfN(35, Gen.alphaChar).map(_.mkString)
        addressLine2 <- Gen.listOfN(35, Gen.alphaChar).map(_.mkString)
        townOrCity <- Gen.listOfN(35, Gen.alphaChar).map(_.mkString)
        regionOrState <- Gen.listOfN(35, Gen.alphaChar).map(_.mkString)
        postcode <- Gen.listOfN(35, Gen.alphaChar).map(_.mkString)
      } yield EtmpOtherAddress(
        issuedBy,
        tradingName,
        addressLine1,
        Some(addressLine2),
        townOrCity,
        Some(regionOrState),
        postcode
      )
    }

}
