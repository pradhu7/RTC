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
 * <p>Java class for FamilyHistoryType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FamilyHistoryType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:astm-org:CCR}CCRCodedDataObjectType">
 *       &lt;sequence>
 *         &lt;element name="FamilyMember" maxOccurs="unbounded" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;extension base="{urn:astm-org:CCR}ActorReferenceType">
 *                 &lt;sequence>
 *                   &lt;element name="HealthStatus" type="{urn:astm-org:CCR}CurrentHealthStatusType" minOccurs="0"/>
 *                   &lt;group ref="{urn:astm-org:CCR}SLRCGroup"/>
 *                 &lt;/sequence>
 *               &lt;/extension>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="Problem" maxOccurs="unbounded" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element ref="{urn:astm-org:CCR}Type" minOccurs="0"/>
 *                   &lt;element ref="{urn:astm-org:CCR}Description" minOccurs="0"/>
 *                   &lt;element name="Episodes" type="{urn:astm-org:CCR}EpisodesType" minOccurs="0"/>
 *                   &lt;group ref="{urn:astm-org:CCR}SLRCGroup"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FamilyHistoryType", propOrder = {
    "familyMember",
    "problem"
})
public class FamilyHistoryType
    extends CCRCodedDataObjectType
{

    @XmlElement(name = "FamilyMember")
    protected List<FamilyHistoryType.FamilyMember> familyMember;
    @XmlElement(name = "Problem")
    protected List<FamilyHistoryType.Problem> problem;

    /**
     * Gets the value of the familyMember property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the familyMember property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFamilyMember().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FamilyHistoryType.FamilyMember }
     * 
     * 
     */
    public List<FamilyHistoryType.FamilyMember> getFamilyMember() {
        if (familyMember == null) {
            familyMember = new ArrayList<FamilyHistoryType.FamilyMember>();
        }
        return this.familyMember;
    }

    /**
     * Gets the value of the problem property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the problem property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProblem().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FamilyHistoryType.Problem }
     * 
     * 
     */
    public List<FamilyHistoryType.Problem> getProblem() {
        if (problem == null) {
            problem = new ArrayList<FamilyHistoryType.Problem>();
        }
        return this.problem;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;extension base="{urn:astm-org:CCR}ActorReferenceType">
     *       &lt;sequence>
     *         &lt;element name="HealthStatus" type="{urn:astm-org:CCR}CurrentHealthStatusType" minOccurs="0"/>
     *         &lt;group ref="{urn:astm-org:CCR}SLRCGroup"/>
     *       &lt;/sequence>
     *     &lt;/extension>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "healthStatus",
        "source",
        "internalCCRLink",
        "referenceID",
        "commentID",
        "signature"
    })
    public static class FamilyMember
        extends ActorReferenceType
    {

        @XmlElement(name = "HealthStatus")
        protected CurrentHealthStatusType healthStatus;
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
         * Gets the value of the healthStatus property.
         * 
         * @return
         *     possible object is
         *     {@link CurrentHealthStatusType }
         *     
         */
        public CurrentHealthStatusType getHealthStatus() {
            return healthStatus;
        }

        /**
         * Sets the value of the healthStatus property.
         * 
         * @param value
         *     allowed object is
         *     {@link CurrentHealthStatusType }
         *     
         */
        public void setHealthStatus(CurrentHealthStatusType value) {
            this.healthStatus = value;
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


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element ref="{urn:astm-org:CCR}Type" minOccurs="0"/>
     *         &lt;element ref="{urn:astm-org:CCR}Description" minOccurs="0"/>
     *         &lt;element name="Episodes" type="{urn:astm-org:CCR}EpisodesType" minOccurs="0"/>
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
    @XmlType(name = "", propOrder = {
        "type",
        "description",
        "episodes",
        "source",
        "internalCCRLink",
        "referenceID",
        "commentID",
        "signature"
    })
    public static class Problem {

        @XmlElement(name = "Type")
        protected CodedDescriptionType type;
        @XmlElement(name = "Description")
        protected CodedDescriptionType description;
        @XmlElement(name = "Episodes")
        protected EpisodesType episodes;
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
         * Gets the value of the description property.
         * 
         * @return
         *     possible object is
         *     {@link CodedDescriptionType }
         *     
         */
        public CodedDescriptionType getDescription() {
            return description;
        }

        /**
         * Sets the value of the description property.
         * 
         * @param value
         *     allowed object is
         *     {@link CodedDescriptionType }
         *     
         */
        public void setDescription(CodedDescriptionType value) {
            this.description = value;
        }

        /**
         * Gets the value of the episodes property.
         * 
         * @return
         *     possible object is
         *     {@link EpisodesType }
         *     
         */
        public EpisodesType getEpisodes() {
            return episodes;
        }

        /**
         * Sets the value of the episodes property.
         * 
         * @param value
         *     allowed object is
         *     {@link EpisodesType }
         *     
         */
        public void setEpisodes(EpisodesType value) {
            this.episodes = value;
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

}
