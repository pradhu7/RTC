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
 * <p>Java class for MethodType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MethodType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:astm-org:CCR}CodedDescriptionType">
 *       &lt;sequence>
 *         &lt;element name="MethodSequencePosition" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
 *         &lt;element name="MultipleMethodModifier" type="{urn:astm-org:CCR}CodedDescriptionType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MethodType", propOrder = {
    "methodSequencePosition",
    "multipleMethodModifier"
})
public class MethodType
    extends CodedDescriptionType
{

    @XmlElement(name = "MethodSequencePosition")
    protected BigInteger methodSequencePosition;
    @XmlElement(name = "MultipleMethodModifier")
    protected CodedDescriptionType multipleMethodModifier;

    /**
     * Gets the value of the methodSequencePosition property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getMethodSequencePosition() {
        return methodSequencePosition;
    }

    /**
     * Sets the value of the methodSequencePosition property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setMethodSequencePosition(BigInteger value) {
        this.methodSequencePosition = value;
    }

    /**
     * Gets the value of the multipleMethodModifier property.
     * 
     * @return
     *     possible object is
     *     {@link CodedDescriptionType }
     *     
     */
    public CodedDescriptionType getMultipleMethodModifier() {
        return multipleMethodModifier;
    }

    /**
     * Sets the value of the multipleMethodModifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link CodedDescriptionType }
     *     
     */
    public void setMultipleMethodModifier(CodedDescriptionType value) {
        this.multipleMethodModifier = value;
    }

}
