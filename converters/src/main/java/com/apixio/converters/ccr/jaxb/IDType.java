//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.12.06 at 03:14:04 PM PST 
//


package com.apixio.converters.ccr.jaxb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for IDType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="IDType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="DateTime" type="{urn:astm-org:CCR}DateTimeType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{urn:astm-org:CCR}Type" minOccurs="0"/>
 *         &lt;element name="ID" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="IssuedBy" type="{urn:astm-org:CCR}ActorReferenceType" minOccurs="0"/>
 *         &lt;group ref="{urn:astm-org:CCR}SLRCGroup"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IDType", propOrder = {
    "dateTime",
    "type",
    "id",
    "issuedBy",
    "source",
    "internalCCRLink",
    "referenceID",
    "commentID",
    "signature"
})
public class IDType {

    @XmlElement(name = "DateTime")
    protected List<DateTimeType> dateTime;
    @XmlElement(name = "Type")
    protected CodedDescriptionType type;
    @XmlElement(name = "ID", required = true)
    protected String id;
    @XmlElement(name = "IssuedBy")
    protected ActorReferenceType issuedBy;
    @XmlElement(name = "Source", required = true)
    protected List<SourceType> source;
    @XmlElement(name = "InternalCCRLink")
    protected List<InternalCCRLink> internalCCRLink;
    @XmlElement(name = "ReferenceID")
    protected List<String> referenceID;
    @XmlElement(name = "CommentID")
    protected List<String> commentID;
    @XmlElement(name = "Signature")
    protected List<CCRCodedDataObjectType.Signature> signature;

    /**
     * Gets the value of the dateTime property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dateTime property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDateTime().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DateTimeType }
     * 
     * 
     */
    public List<DateTimeType> getDateTime() {
        if (dateTime == null) {
            dateTime = new ArrayList<DateTimeType>();
        }
        return this.dateTime;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link CodedDescriptionType }
     *     
     */
    public CodedDescriptionType getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link CodedDescriptionType }
     *     
     */
    public void setType(CodedDescriptionType value) {
        this.type = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getID() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setID(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the issuedBy property.
     * 
     * @return
     *     possible object is
     *     {@link ActorReferenceType }
     *     
     */
    public ActorReferenceType getIssuedBy() {
        return issuedBy;
    }

    /**
     * Sets the value of the issuedBy property.
     * 
     * @param value
     *     allowed object is
     *     {@link ActorReferenceType }
     *     
     */
    public void setIssuedBy(ActorReferenceType value) {
        this.issuedBy = value;
    }

    /**
     * Gets the value of the source property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the source property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSource().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SourceType }
     * 
     * 
     */
    public List<SourceType> getSource() {
        if (source == null) {
            source = new ArrayList<SourceType>();
        }
        return this.source;
    }

    /**
     * Gets the value of the internalCCRLink property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the internalCCRLink property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInternalCCRLink().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link InternalCCRLink }
     * 
     * 
     */
    public List<InternalCCRLink> getInternalCCRLink() {
        if (internalCCRLink == null) {
            internalCCRLink = new ArrayList<InternalCCRLink>();
        }
        return this.internalCCRLink;
    }

    /**
     * Gets the value of the referenceID property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the referenceID property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReferenceID().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getReferenceID() {
        if (referenceID == null) {
            referenceID = new ArrayList<String>();
        }
        return this.referenceID;
    }

    /**
     * Gets the value of the commentID property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the commentID property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCommentID().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getCommentID() {
        if (commentID == null) {
            commentID = new ArrayList<String>();
        }
        return this.commentID;
    }

    /**
     * Gets the value of the signature property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the signature property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSignature().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CCRCodedDataObjectType.Signature }
     * 
     * 
     */
    public List<CCRCodedDataObjectType.Signature> getSignature() {
        if (signature == null) {
            signature = new ArrayList<CCRCodedDataObjectType.Signature>();
        }
        return this.signature;
    }

}
