package com.apixio.nassembly.exchangeutils

import com.apixio.datacatalog.{AddressOuterClass, ContactInfoOuterClass, PatientProto}
import com.apixio.model.patient.{TelephoneNumber, _}
import com.apixio.nassembly.exchangeutils.MockApoUtil._
import com.apixio.nassembly.exchangeutils.TestVerificationUtil.verifyStrings
import com.apixio.nassembly.patient.SeparatorUtils

import scala.jdk.CollectionConverters.seqAsJavaListConverter

object ContactDetailsTestUtil {

  var srcContactDetails: ContactDetails = null

  def generateContactDetails = {
    val contactDetails = new ContactDetails
    contactDetails.setAlternateAddress(List(populateAddress).asJava)
    contactDetails.setAlternateEmails(generateSingularListOfAlphaNumericString)
    contactDetails.setAlternatePhone(List(populateTelephoneNumber).asJava)
    contactDetails.setPrimaryAddress(populateAddress)
    contactDetails.setPrimaryEmail(generateAlphaNumericString)
    contactDetails.setPrimaryPhone(populateTelephoneNumber)
    srcContactDetails = contactDetails
    contactDetails
  }

  private def populateTelephoneNumber = {
    val telephoneNumber = new TelephoneNumber
    telephoneNumber.setPhoneNumber(generateAlphaNumericString)
    telephoneNumber.setPhoneType(TelephoneType.TEMP_PHONE)
    telephoneNumber
  }

  def populateAddress = {
    val address = new Address
    address.setAddressType(AddressType.OLD)
    address.setCity("some_city")
    address.setCountry("some_country")
    address.setCounty("some_county")
    address.setState("some_state")
    address.setStreetAddresses(List("some street address").asJava)
    address.setZip("some_zip")
    address
  }

  def assertContactDetails(patientProto: PatientProto.Patient) = {
    val summaries = SeparatorUtils.separateContactDetails(patientProto)
    assert(summaries.size == 1)
    val contactDetailsSummary = summaries.head
    verifyContactDetails(contactDetailsSummary.getContactInfo, srcContactDetails)
  }

  private def verifyContactDetails(protoContactDetails: ContactInfoOuterClass.ContactInfo, srcContactDetails: ContactDetails) = {
    verifyAddress(protoContactDetails.getAlternateAddresses(0), srcContactDetails.getAlternateAddresses.get(0))
    verifyStrings(protoContactDetails.getAlternateEmails(0), srcContactDetails.getAlternateEmails.get(0))
    verifyPhones(protoContactDetails.getAlternatePhones(0), srcContactDetails.getAlternatePhones.get(0))
    verifyAddress(protoContactDetails.getAddress, srcContactDetails.getPrimaryAddress)
    verifyStrings(protoContactDetails.getPrimaryEmail, srcContactDetails.getPrimaryEmail)
    verifyPhones(protoContactDetails.getPrimaryPhone, srcContactDetails.getPrimaryPhone)
  }

  def verifyAddress(protoAddress: AddressOuterClass.Address, modelAddress: Address): Unit = {
    verifyStrings(protoAddress.getAddressType, modelAddress.getAddressType.name())
    verifyStrings(protoAddress.getCity, modelAddress.getCity)
    verifyStrings(protoAddress.getCountry, modelAddress.getCountry)
    verifyStrings(protoAddress.getCounty, modelAddress.getCounty)
    verifyStrings(protoAddress.getState, modelAddress.getState)
    verifyStrings(protoAddress.getStreetAddresses(0), modelAddress.getStreetAddresses.get(0))
    verifyStrings(protoAddress.getZip, modelAddress.getZip)
  }

  private def verifyPhones(protoPhoneNumber: ContactInfoOuterClass.TelephoneNumber, modelPhoneNumber: TelephoneNumber)
  : Unit = {
    verifyStrings(protoPhoneNumber.getPhoneNumber, modelPhoneNumber.getPhoneNumber)
    verifyStrings(protoPhoneNumber.getTelephoneType, modelPhoneNumber.getPhoneType.name())
  }

}