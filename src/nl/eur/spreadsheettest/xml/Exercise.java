//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.11.20 at 02:41:11 PM CET 
//


package nl.eur.spreadsheettest.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
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
 *         &lt;element name="shorttitle" type="{assignment.xsd}shortType"/>
 *         &lt;element name="title" type="{assignment.xsd}collapseString"/>
 *         &lt;element name="category" type="{assignment.xsd}collapseString"/>
 *         &lt;element name="instructions" type="{assignment.xsd}collapseString"/>
 *         &lt;element name="testcases" type="{assignment.xsd}testcasesType"/>
 *         &lt;element name="comparisons" type="{assignment.xsd}comparisonsType"/>
 *         &lt;element name="styles" type="{assignment.xsd}stylesType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="seed" type="{http://www.w3.org/2001/XMLSchema}long" default="54321" />
 *       &lt;attribute name="reportDigits" type="{http://www.w3.org/2001/XMLSchema}int" default="4" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "shorttitle",
    "title",
    "category",
    "instructions",
    "testcases",
    "comparisons",
    "styles"
})
@XmlRootElement(name = "exercise")
public class Exercise {

    @XmlElement(required = true)
    protected String shorttitle;
    @XmlElement(required = true)
    protected String title;
    @XmlElement(required = true)
    protected String category;
    @XmlElement(required = true)
    protected String instructions;
    @XmlElement(required = true)
    protected TestcasesType testcases;
    @XmlElement(required = true)
    protected ComparisonsType comparisons;
    @XmlElement(required = true)
    protected StylesType styles;
    @XmlAttribute(name = "seed")
    protected Long seed;
    @XmlAttribute(name = "reportDigits")
    protected Integer reportDigits;

    /**
     * Gets the value of the shorttitle property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getShorttitle() {
        return shorttitle;
    }

    /**
     * Sets the value of the shorttitle property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setShorttitle(String value) {
        this.shorttitle = value;
    }

    /**
     * Gets the value of the title property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the value of the title property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTitle(String value) {
        this.title = value;
    }

    /**
     * Gets the value of the category property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the value of the category property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCategory(String value) {
        this.category = value;
    }

    /**
     * Gets the value of the instructions property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInstructions() {
        return instructions;
    }

    /**
     * Sets the value of the instructions property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInstructions(String value) {
        this.instructions = value;
    }

    /**
     * Gets the value of the testcases property.
     * 
     * @return
     *     possible object is
     *     {@link TestcasesType }
     *     
     */
    public TestcasesType getTestcases() {
        return testcases;
    }

    /**
     * Sets the value of the testcases property.
     * 
     * @param value
     *     allowed object is
     *     {@link TestcasesType }
     *     
     */
    public void setTestcases(TestcasesType value) {
        this.testcases = value;
    }

    /**
     * Gets the value of the comparisons property.
     * 
     * @return
     *     possible object is
     *     {@link ComparisonsType }
     *     
     */
    public ComparisonsType getComparisons() {
        return comparisons;
    }

    /**
     * Sets the value of the comparisons property.
     * 
     * @param value
     *     allowed object is
     *     {@link ComparisonsType }
     *     
     */
    public void setComparisons(ComparisonsType value) {
        this.comparisons = value;
    }

    /**
     * Gets the value of the styles property.
     * 
     * @return
     *     possible object is
     *     {@link StylesType }
     *     
     */
    public StylesType getStyles() {
        return styles;
    }

    /**
     * Sets the value of the styles property.
     * 
     * @param value
     *     allowed object is
     *     {@link StylesType }
     *     
     */
    public void setStyles(StylesType value) {
        this.styles = value;
    }

    /**
     * Gets the value of the seed property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public long getSeed() {
        if (seed == null) {
            return  54321L;
        } else {
            return seed;
        }
    }

    /**
     * Sets the value of the seed property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setSeed(Long value) {
        this.seed = value;
    }

    /**
     * Gets the value of the reportDigits property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getReportDigits() {
        if (reportDigits == null) {
            return  4;
        } else {
            return reportDigits;
        }
    }

    /**
     * Sets the value of the reportDigits property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setReportDigits(Integer value) {
        this.reportDigits = value;
    }

}
