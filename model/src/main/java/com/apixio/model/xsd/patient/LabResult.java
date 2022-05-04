//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.09.05 at 03:21:47 PM EDT 
//


package com.apixio.model.xsd.patient;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for LabResult complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LabResult">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="labName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="value" type="{}StringOrNumber"/>
 *         &lt;element name="range" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="flag">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;enumeration value="NORMAL"/>
 *               &lt;enumeration value="HIGH"/>
 *               &lt;enumeration value="LOW"/>
 *               &lt;enumeration value="ABNORMAL"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="units" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="labNote" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="specimen" type="{}Code"/>
 *         &lt;element name="panel" type="{}Code"/>
 *         &lt;element name="superPanel" type="{}Code"/>
 *         &lt;element name="sampleDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="careSiteId" type="{http://www.w3.org/2001/XMLSchema}IDREF"/>
 *         &lt;element name="sequenceNumber" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="alternateDate" type="{}TypedDate" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LabResult", propOrder = {
    "labName",
    "value",
    "range",
    "flag",
    "units",
    "labNote",
    "specimen",
    "panel",
    "superPanel",
    "sampleDate",
    "careSiteId",
    "sequenceNumber",
    "alternateDate"
})
public class LabResult {

    @XmlElement(required = true)
    protected String labName;
    @XmlElement(required = true)
    protected String value;
    @XmlElement(required = true)
    protected String range;
    @XmlElement(required = true)
    protected String flag;
    @XmlElement(required = true)
    protected String units;
    @XmlElement(required = true)
    protected String labNote;
    @XmlElement(required = true)
    protected Code specimen;
    @XmlElement(required = true)
    protected Code panel;
    @XmlElement(required = true)
    protected Code superPanel;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar sampleDate;
    @XmlElement(required = true)
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object careSiteId;
    @XmlElement(required = true, type = Integer.class, nillable = true)
    protected Integer sequenceNumber;
    protected List<TypedDate> alternateDate;

    /**
     * Gets the value of the labName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLabName() {
        return labName;
    }

    /**
     * Sets the value of the labName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLabName(String value) {
        this.labName = value;
    }

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the range property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRange() {
        return range;
    }

    /**
     * Sets the value of the range property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRange(String value) {
        this.range = value;
    }

    /**
     * Gets the value of the flag property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFlag() {
        return flag;
    }

    /**
     * Sets the value of the flag property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFlag(String value) {
        this.flag = value;
    }

    /**
     * Gets the value of the units property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUnits() {
        return units;
    }

    /**
     * Sets the value of the units property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUnits(String value) {
        this.units = value;
    }

    /**
     * Gets the value of the labNote property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLabNote() {
        return labNote;
    }

    /**
     * Sets the value of the labNote property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLabNote(String value) {
        this.labNote = value;
    }

    /**
     * Gets the value of the specimen property.
     * 
     * @return
     *     possible object is
     *     {@link Code }
     *     
     */
    public Code getSpecimen() {
        return specimen;
    }

    /**
     * Sets the value of the specimen property.
     * 
     * @param value
     *     allowed object is
     *     {@link Code }
     *     
     */
    public void setSpecimen(Code value) {
        this.specimen = value;
    }

    /**
     * Gets the value of the panel property.
     * 
     * @return
     *     possible object is
     *     {@link Code }
     *     
     */
    public Code getPanel() {
        return panel;
    }

    /**
     * Sets the value of the panel property.
     * 
     * @param value
     *     allowed object is
     *     {@link Code }
     *     
     */
    public void setPanel(Code value) {
        this.panel = value;
    }

    /**
     * Gets the value of the superPanel property.
     * 
     * @return
     *     possible object is
     *     {@link Code }
     *     
     */
    public Code getSuperPanel() {
        return superPanel;
    }

    /**
     * Sets the value of the superPanel property.
     * 
     * @param value
     *     allowed object is
     *     {@link Code }
     *     
     */
    public void setSuperPanel(Code value) {
        this.superPanel = value;
    }

    /**
     * Gets the value of the sampleDate property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getSampleDate() {
        return sampleDate;
    }

    /**
     * Sets the value of the sampleDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setSampleDate(XMLGregorianCalendar value) {
        this.sampleDate = value;
    }

    /**
     * Gets the value of the careSiteId property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getCareSiteId() {
        return careSiteId;
    }

    /**
     * Sets the value of the careSiteId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setCareSiteId(Object value) {
        this.careSiteId = value;
    }

    /**
     * Gets the value of the sequenceNumber property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Sets the value of the sequenceNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setSequenceNumber(Integer value) {
        this.sequenceNumber = value;
    }

    /**
     * Gets the value of the alternateDate property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the alternateDate property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAlternateDate().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TypedDate }
     * 
     * 
     */
    public List<TypedDate> getAlternateDate() {
        if (alternateDate == null) {
            alternateDate = new ArrayList<TypedDate>();
        }
        return this.alternateDate;
    }

}