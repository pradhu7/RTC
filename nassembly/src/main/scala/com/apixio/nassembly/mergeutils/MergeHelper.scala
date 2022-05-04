package com.apixio.nassembly.mergeutils

import com.apixio.datacatalog.OrganizationOuterClass
import com.apixio.util.nassembly.{ContactInfoUtils, IdentityFunctions}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

object MergeHelper {


  def mergeOrganization(orgs: Seq[OrganizationOuterClass.Organization]): OrganizationOuterClass.Organization = {
    if (orgs.isEmpty) null
    else {
      val builder = orgs.head.toBuilder
      // Set name
      orgs.map(_.getName).find(_.nonEmpty).foreach(builder.setName)

      // Set alternateIds
      val alternateIds = orgs.flatMap(_.getAlternateIdsList)
        .groupBy(IdentityFunctions.getIdentity).map {
        case (identity, duplicates) => duplicates.head
      }

      if (alternateIds.nonEmpty) {
        builder.clearAlternateIds()
        if (builder.hasPrimaryId) builder.addAlternateIds(builder.getPrimaryId)
        builder.addAllAlternateIds(alternateIds.asJava)

      }
      // Set contact info
      val contactInfo = ContactInfoUtils.mergeContactDetails(orgs.filter(_.hasContactDetails).map(_.getContactDetails).toList)
      Option(contactInfo).foreach(builder.setContactDetails)

      // build
      builder.build()
    }
  }


}
