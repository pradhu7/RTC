//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.12.06 at 03:14:04 PM PST 
//


package com.apixio.converters.ccr.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SocialHistoryType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SocialHistoryType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:astm-org:CCR}CCRCodedDataObjectType">
 *       &lt;sequence>
 *         &lt;element name="Episodes" type="{urn:astm-org:CCR}EpisodesType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SocialHistoryType", propOrder = {
    "episodes"
})
public class SocialHistoryType
    extends CCRCodedDataObjectType
{

    @XmlElement(name = "Episodes")
    protected EpisodesType episodes;

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

}
