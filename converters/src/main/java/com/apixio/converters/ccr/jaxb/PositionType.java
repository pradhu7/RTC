//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.12.06 at 03:14:04 PM PST 
//


package com.apixio.converters.ccr.jaxb;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for PositionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PositionType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:astm-org:CCR}CodedDescriptionType">
 *       &lt;sequence>
 *         &lt;element name="PositionSequencePosition" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
 *         &lt;element name="MultiplePositionModifier" type="{urn:astm-org:CCR}CodedDescriptionType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PositionType", propOrder = {
    "positionSequencePosition",
    "multiplePositionModifier"
})
public class PositionType
    extends CodedDescriptionType
{

    @XmlElement(name = "PositionSequencePosition")
    protected BigInteger positionSequencePosition;
    @XmlElement(name = "MultiplePositionModifier")
    protected CodedDescriptionType multiplePositionModifier;

    /**
     * Gets the value of the positionSequencePosition property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getPositionSequencePosition() {
        return positionSequencePosition;
    }

    /**
     * Sets the value of the positionSequencePosition property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setPositionSequencePosition(BigInteger value) {
        this.positionSequencePosition = value;
    }

    /**
     * Gets the value of the multiplePositionModifier property.
     * 
     * @return
     *     possible object is
     *     {@link CodedDescriptionType }
     *     
     */
    public CodedDescriptionType getMultiplePositionModifier() {
        return multiplePositionModifier;
    }

    /**
     * Sets the value of the multiplePositionModifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link CodedDescriptionType }
     *     
     */
    public void setMultiplePositionModifier(CodedDescriptionType value) {
        this.multiplePositionModifier = value;
    }

}
