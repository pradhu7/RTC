//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.12.06 at 03:14:04 PM PST 
//


package com.apixio.converters.ccr.jaxb;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for IndicationType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="IndicationType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="PRNFlag" type="{urn:astm-org:CCR}CodedDescriptionType" minOccurs="0"/>
 *         &lt;element ref="{urn:astm-org:CCR}Description" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="Problem" type="{urn:astm-org:CCR}ProblemType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="PhysiologicalParameter" maxOccurs="unbounded" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;extension base="{urn:astm-org:CCR}MeasureType">
 *                 &lt;sequence>
 *                   &lt;element name="ParameterSequencePosition" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
 *                   &lt;element name="VariableParameterModifier" type="{urn:astm-org:CCR}CodedDescriptionType" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/extension>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;group ref="{urn:astm-org:CCR}SLRCGroup"/>
 *         &lt;element name="IndicationSequencePosition" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
 *         &lt;element name="MultipleIndicationModifier" type="{urn:astm-org:CCR}CodedDescriptionType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IndicationType", propOrder = {
    "prnFlag",
    "description",
    "problem",
    "physiologicalParameter",
    "source",
    "internalCCRLink",
    "referenceID",
    "commentID",
    "signature",
    "indicationSequencePosition",
    "multipleIndicationModifier"
})
public class IndicationType {

    @XmlElement(name = "PRNFlag")
    protected CodedDescriptionType prnFlag;
    @XmlElement(name = "Description")
    protected List<CodedDescriptionType> description;
    @XmlElement(name = "Problem")
    protected List<ProblemType> problem;
    @XmlElement(name = "PhysiologicalParameter")
    protected List<IndicationType.PhysiologicalParameter> physiologicalParameter;
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
    @XmlElement(name = "IndicationSequencePosition")
    protected BigInteger indicationSequencePosition;
    @XmlElement(name = "MultipleIndicationModifier")
    protected CodedDescriptionType multipleIndicationModifier;

    /**
     * Gets the value of the prnFlag property.
     * 
     * @return
     *     possible object is
     *     {@link CodedDescriptionType }
     *     
     */
    public CodedDescriptionType getPRNFlag() {
        return prnFlag;
    }

    /**
     * Sets the value of the prnFlag property.
     * 
     * @param value
     *     allowed object is
     *     {@link CodedDescriptionType }
     *     
     */
    public void setPRNFlag(CodedDescriptionType value) {
        this.prnFlag = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the description property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDescription().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CodedDescriptionType }
     * 
     * 
     */
    public List<CodedDescriptionType> getDescription() {
        if (description == null) {
            description = new ArrayList<CodedDescriptionType>();
        }
        return this.description;
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
     * {@link ProblemType }
     * 
     * 
     */
    public List<ProblemType> getProblem() {
        if (problem == null) {
            problem = new ArrayList<ProblemType>();
        }
        return this.problem;
    }

    /**
     * Gets the value of the physiologicalParameter property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the physiologicalParameter property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPhysiologicalParameter().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link IndicationType.PhysiologicalParameter }
     * 
     * 
     */
    public List<IndicationType.PhysiologicalParameter> getPhysiologicalParameter() {
        if (physiologicalParameter == null) {
            physiologicalParameter = new ArrayList<IndicationType.PhysiologicalParameter>();
        }
        return this.physiologicalParameter;
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

    /**
     * Gets the value of the indicationSequencePosition property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getIndicationSequencePosition() {
        return indicationSequencePosition;
    }

    /**
     * Sets the value of the indicationSequencePosition property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setIndicationSequencePosition(BigInteger value) {
        this.indicationSequencePosition = value;
    }

    /**
     * Gets the value of the multipleIndicationModifier property.
     * 
     * @return
     *     possible object is
     *     {@link CodedDescriptionType }
     *     
     */
    public CodedDescriptionType getMultipleIndicationModifier() {
        return multipleIndicationModifier;
    }

    /**
     * Sets the value of the multipleIndicationModifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link CodedDescriptionType }
     *     
     */
    public void setMultipleIndicationModifier(CodedDescriptionType value) {
        this.multipleIndicationModifier = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;extension base="{urn:astm-org:CCR}MeasureType">
     *       &lt;sequence>
     *         &lt;element name="ParameterSequencePosition" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
     *         &lt;element name="VariableParameterModifier" type="{urn:astm-org:CCR}CodedDescriptionType" minOccurs="0"/>
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
        "parameterSequencePosition",
        "variableParameterModifier"
    })
    public static class PhysiologicalParameter
        extends MeasureType
    {

        @XmlElement(name = "ParameterSequencePosition")
        protected BigInteger parameterSequencePosition;
        @XmlElement(name = "VariableParameterModifier")
        protected CodedDescriptionType variableParameterModifier;

        /**
         * Gets the value of the parameterSequencePosition property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getParameterSequencePosition() {
            return parameterSequencePosition;
        }

        /**
         * Sets the value of the parameterSequencePosition property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setParameterSequencePosition(BigInteger value) {
            this.parameterSequencePosition = value;
        }

        /**
         * Gets the value of the variableParameterModifier property.
         * 
         * @return
         *     possible object is
         *     {@link CodedDescriptionType }
         *     
         */
        public CodedDescriptionType getVariableParameterModifier() {
            return variableParameterModifier;
        }

        /**
         * Sets the value of the variableParameterModifier property.
         * 
         * @param value
         *     allowed object is
         *     {@link CodedDescriptionType }
         *     
         */
        public void setVariableParameterModifier(CodedDescriptionType value) {
            this.variableParameterModifier = value;
        }

    }

}
