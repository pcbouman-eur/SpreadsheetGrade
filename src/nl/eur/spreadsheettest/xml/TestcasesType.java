//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.07.18 at 10:33:17 PM CEST 
//


package nl.eur.spreadsheettest.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for testcasesType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="testcasesType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="testcase" type="{assignment.xsd}testcaseType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="maxCellsFullTest" type="{http://www.w3.org/2001/XMLSchema}int" default="5" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "testcasesType", propOrder = {
    "testcase"
})
public class TestcasesType {

    @XmlElement(required = true)
    protected List<TestcaseType> testcase;
    @XmlAttribute(name = "maxCellsFullTest")
    protected Integer maxCellsFullTest;

    /**
     * Gets the value of the testcase property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the testcase property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTestcase().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TestcaseType }
     * 
     * 
     */
    public List<TestcaseType> getTestcase() {
        if (testcase == null) {
            testcase = new ArrayList<TestcaseType>();
        }
        return this.testcase;
    }

    /**
     * Gets the value of the maxCellsFullTest property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getMaxCellsFullTest() {
        if (maxCellsFullTest == null) {
            return  5;
        } else {
            return maxCellsFullTest;
        }
    }

    /**
     * Sets the value of the maxCellsFullTest property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMaxCellsFullTest(Integer value) {
        this.maxCellsFullTest = value;
    }

}
