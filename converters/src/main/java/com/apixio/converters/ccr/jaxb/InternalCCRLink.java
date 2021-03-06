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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


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
 *         &lt;element name="LinkID" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="LinkRelationship" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="Source" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;extension base="{urn:astm-org:CCR}SourceType">
 *               &lt;/extension>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
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
    "linkID",
    "linkRelationship",
    "source"
})
@XmlRootElement(name = "InternalCCRLink")
public class InternalCCRLink {

    @XmlElement(name = "LinkID", required = true)
    protected String linkID;
    @XmlElement(name = "LinkRelationship")
    protected List<String> linkRelationship;
    @XmlElement(name = "Source")
    protected InternalCCRLink.Source source;

    /**
     * Gets the value of the linkID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLinkID() {
        return linkID;
    }

    /**
     * Sets the value of the linkID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLinkID(String value) {
        this.linkID = value;
    }

    /**
     * Gets the value of the linkRelationship property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the linkRelationship property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLinkRelationship().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getLinkRelationship() {
        if (linkRelationship == null) {
            linkRelationship = new ArrayList<String>();
        }
        return this.linkRelationship;
    }

    /**
     * Gets the value of the source property.
     * 
     * @return
     *     possible object is
     *     {@link InternalCCRLink.Source }
     *     
     */
    public InternalCCRLink.Source getSource() {
        return source;
    }

    /**
     * Sets the value of the source property.
     * 
     * @param value
     *     allowed object is
     *     {@link InternalCCRLink.Source }
     *     
     */
    public void setSource(InternalCCRLink.Source value) {
        this.source = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;extension base="{urn:astm-org:CCR}SourceType">
     *     &lt;/extension>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class Source
        extends SourceType
    {


    }

}
