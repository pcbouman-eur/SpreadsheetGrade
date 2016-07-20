//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.07.20 at 04:45:06 PM CEST 
//


package nl.eur.spreadsheettest.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for inputType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="inputType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="range" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="lb" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="ub" type="{http://www.w3.org/2001/XMLSchema}double" />
 *       &lt;attribute name="precision" type="{http://www.w3.org/2001/XMLSchema}double" default="0.01" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "inputType")
public class InputType {

    @XmlAttribute(name = "range", required = true)
    protected String range;
    @XmlAttribute(name = "lb", required = true)
    protected double lb;
    @XmlAttribute(name = "ub")
    protected Double ub;
    @XmlAttribute(name = "precision")
    protected Double precision;

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
     * Gets the value of the lb property.
     * 
     */
    public double getLb() {
        return lb;
    }

    /**
     * Sets the value of the lb property.
     * 
     */
    public void setLb(double value) {
        this.lb = value;
    }

    /**
     * Gets the value of the ub property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getUb() {
        return ub;
    }

    /**
     * Sets the value of the ub property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setUb(Double value) {
        this.ub = value;
    }

    /**
     * Gets the value of the precision property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public double getPrecision() {
        if (precision == null) {
            return  0.01D;
        } else {
            return precision;
        }
    }

    /**
     * Sets the value of the precision property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setPrecision(Double value) {
        this.precision = value;
    }

}
