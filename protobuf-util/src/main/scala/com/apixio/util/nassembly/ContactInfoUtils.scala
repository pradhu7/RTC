package com.apixio.util.nassembly

import com.apixio.datacatalog.AddressOuterClass.Address
import com.apixio.datacatalog.ContactInfoOuterClass.{ContactInfo, TelephoneNumber}

import scala.collection.JavaConverters._


object ContactInfoUtils {

  def mergeContactDetails(contactDetails: List[ContactInfo]): ContactInfo ={
    contactDetails.filter(_ != null) match {
      case null => null
      case Nil => null
      case h :: t =>

        val primaryAddressOpt = (h +: t).find(hasPrimaryAddress).map(_.getAddress)
        val otherAddress = dedupAddresses(t.map(_.getAddress) ++ t.flatMap(_.getAlternateAddressesList.asScala) ++ h.getAlternateAddressesList.asScala)
          .filterNot(_.getSerializedSize == 0)

        val primaryEmailOpt = (h +: t).find(hasPrimaryEmail).map(_.getPrimaryEmail)
        val otherEmails: List[String] = (t.map(_.getPrimaryEmail) ++ t.flatMap(_.getAlternateEmailsList.asScala) ++ h.getAlternateEmailsList.asScala).distinct
          .filterNot(_.isEmpty)

        val primaryTelephoneOpt = (h +: t).find(hasPrimaryPhone).map(_.getPrimaryPhone)
        val otherTelephones = dedupTelephone(t.map(_.getPrimaryPhone) ++ t.flatMap(_.getAlternatePhonesList.asScala) ++ h.getAlternatePhonesList.asScala)
          .filterNot(_.getSerializedSize == 0)

        val builder = h.toBuilder
          .clearAlternateAddresses()
          .clearAlternatePhones()
          .clearAlternateEmails()

        primaryEmailOpt match {
          case Some(email) =>
            builder.setPrimaryEmail(email)
            builder.addAllAlternateEmails(otherEmails.asJava)
          case None =>
            if (otherEmails.nonEmpty) {
              builder.setPrimaryEmail(otherEmails.head)
              builder.addAllAlternateEmails(otherEmails.drop(1).asJava)
            }
        }

        primaryTelephoneOpt match {
          case Some(telephone) =>
            builder.setPrimaryPhone(telephone)
            builder.addAllAlternatePhones(otherTelephones.asJava)
          case None =>
            if (otherTelephones.nonEmpty) {
              builder.addAllAlternatePhones(otherTelephones.drop(1).asJava)
            }
        }

        primaryAddressOpt match {
          case Some(address) =>
            builder.setAddress(address)
            builder.addAlternateAddresses(address)
            builder.addAllAlternateAddresses(otherAddress.asJava)
          case None =>
            if (otherAddress.nonEmpty) {
              builder.setAddress(otherAddress.head)
              builder.addAllAlternateAddresses(otherAddress.drop(1).asJava)
            }
        }

        builder.build()
    }
  }

  def hasPrimaryPhone(contactInfo: ContactInfo): Boolean = {
    contactInfo.hasPrimaryPhone && contactInfo.getPrimaryPhone.getSerializedSize > 0
  }

  def hasPrimaryAddress(contactInfo: ContactInfo): Boolean = {
    contactInfo.hasAddress && contactInfo.getAddress.getSerializedSize > 0
  }

  def hasPrimaryEmail(contactInfo: ContactInfo): Boolean = {
    contactInfo.getPrimaryEmail.nonEmpty
  }


  def dedupAddresses(addresses: List[Address]): Iterable[Address] = {
    addresses.groupBy(IdentityFunctions.getIdentity).map {
      case (identity, duplicates) => duplicates.head
    }
  }

  def dedupTelephone(telephoneNumbers: List[TelephoneNumber]): Iterable[TelephoneNumber] = {
    telephoneNumbers.groupBy(IdentityFunctions.getIdentity).map {
      case (_, duplicates) => duplicates.head
    }
  }
}
