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
 * <p>Java class for InstructionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="InstructionType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:astm-org:CCR}CodedDescriptionType">
 *       &lt;sequence>
 *         &lt;element name="instructionSequencePosition" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
 *         &lt;element name="MultipleInstructionModifier" type="{urn:astm-org:CCR}CodedDescriptionType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InstructionType", propOrder = {
    "instructionSequencePosition",
    "multipleInstructionModifier"
})
public class InstructionType
    extends CodedDescriptionType
{

    protected BigInteger instructionSequencePosition;
    @XmlElement(name = "MultipleInstructionModifier")
    protected CodedDescriptionType multipleInstructionModifier;

    /**
     * Gets the value of the instructionSequencePosition property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getInstructionSequencePosition() {
        return instructionSequencePosition;
    }

    /**
     * Sets the value of the instructionSequencePosition property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setInstructionSequencePosition(BigInteger value) {
        this.instructionSequencePosition = value;
    }

    /**
     * Gets the value of the multipleInstructionModifier property.
     * 
     * @return
     *     possible object is
     *     {@link CodedDescriptionType }
     *     
     */
    public CodedDescriptionType getMultipleInstructionModifier() {
        return multipleInstructionModifier;
    }

    /**
     * Sets the value of the multipleInstructionModifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link CodedDescriptionType }
     *     
     */
    public void setMultipleInstructionModifier(CodedDescriptionType value) {
        this.multipleInstructionModifier = value;
    }

}
