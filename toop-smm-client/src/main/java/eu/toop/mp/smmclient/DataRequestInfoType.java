
package eu.toop.mp.smmclient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DataRequestInfoType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DataRequestInfoType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="DataRequestIdentifier" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DataConsumerConcept" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DataConsumerConceptIdentifier" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DataConsumerConceptURI" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DataConsumerNamespace" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DataConsumerDataFormat" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ToopConcept" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ToopDataType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ToopConceptSuperclass" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ToopConceptIdentifier" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ToopConceptURI" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ToopConceptDomain" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ToopNamespace" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DataRequestInfoType", propOrder = {
    "dataRequestIdentifier",
    "dataConsumerConcept",
    "dataConsumerConceptIdentifier",
    "dataConsumerConceptURI",
    "dataConsumerNamespace",
    "dataConsumerDataFormat",
    "toopConcept",
    "toopDataType",
    "toopConceptSuperclass",
    "toopConceptIdentifier",
    "toopConceptURI",
    "toopConceptDomain",
    "toopNamespace"
})
public class DataRequestInfoType {

    @XmlElement(name = "DataRequestIdentifier", required = true)
    protected String dataRequestIdentifier;
    @XmlElement(name = "DataConsumerConcept", required = true)
    protected String dataConsumerConcept;
    @XmlElement(name = "DataConsumerConceptIdentifier", required = true)
    protected String dataConsumerConceptIdentifier;
    @XmlElement(name = "DataConsumerConceptURI", required = true)
    protected String dataConsumerConceptURI;
    @XmlElement(name = "DataConsumerNamespace", required = true)
    protected String dataConsumerNamespace;
    @XmlElement(name = "DataConsumerDataFormat", required = true)
    protected String dataConsumerDataFormat;
    @XmlElement(name = "ToopConcept", required = true)
    protected String toopConcept;
    @XmlElement(name = "ToopDataType", required = true)
    protected String toopDataType;
    @XmlElement(name = "ToopConceptSuperclass", required = true)
    protected String toopConceptSuperclass;
    @XmlElement(name = "ToopConceptIdentifier", required = true)
    protected String toopConceptIdentifier;
    @XmlElement(name = "ToopConceptURI", required = true)
    protected String toopConceptURI;
    @XmlElement(name = "ToopConceptDomain", required = true)
    protected String toopConceptDomain;
    @XmlElement(name = "ToopNamespace", required = true)
    protected String toopNamespace;

    /**
     * Gets the value of the dataRequestIdentifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDataRequestIdentifier() {
        return dataRequestIdentifier;
    }

    /**
     * Sets the value of the dataRequestIdentifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDataRequestIdentifier(String value) {
        this.dataRequestIdentifier = value;
    }

    /**
     * Gets the value of the dataConsumerConcept property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDataConsumerConcept() {
        return dataConsumerConcept;
    }

    /**
     * Sets the value of the dataConsumerConcept property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDataConsumerConcept(String value) {
        this.dataConsumerConcept = value;
    }

    /**
     * Gets the value of the dataConsumerConceptIdentifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDataConsumerConceptIdentifier() {
        return dataConsumerConceptIdentifier;
    }

    /**
     * Sets the value of the dataConsumerConceptIdentifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDataConsumerConceptIdentifier(String value) {
        this.dataConsumerConceptIdentifier = value;
    }

    /**
     * Gets the value of the dataConsumerConceptURI property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDataConsumerConceptURI() {
        return dataConsumerConceptURI;
    }

    /**
     * Sets the value of the dataConsumerConceptURI property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDataConsumerConceptURI(String value) {
        this.dataConsumerConceptURI = value;
    }

    /**
     * Gets the value of the dataConsumerNamespace property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDataConsumerNamespace() {
        return dataConsumerNamespace;
    }

    /**
     * Sets the value of the dataConsumerNamespace property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDataConsumerNamespace(String value) {
        this.dataConsumerNamespace = value;
    }

    /**
     * Gets the value of the dataConsumerDataFormat property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDataConsumerDataFormat() {
        return dataConsumerDataFormat;
    }

    /**
     * Sets the value of the dataConsumerDataFormat property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDataConsumerDataFormat(String value) {
        this.dataConsumerDataFormat = value;
    }

    /**
     * Gets the value of the toopConcept property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToopConcept() {
        return toopConcept;
    }

    /**
     * Sets the value of the toopConcept property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToopConcept(String value) {
        this.toopConcept = value;
    }

    /**
     * Gets the value of the toopDataType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToopDataType() {
        return toopDataType;
    }

    /**
     * Sets the value of the toopDataType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToopDataType(String value) {
        this.toopDataType = value;
    }

    /**
     * Gets the value of the toopConceptSuperclass property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToopConceptSuperclass() {
        return toopConceptSuperclass;
    }

    /**
     * Sets the value of the toopConceptSuperclass property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToopConceptSuperclass(String value) {
        this.toopConceptSuperclass = value;
    }

    /**
     * Gets the value of the toopConceptIdentifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToopConceptIdentifier() {
        return toopConceptIdentifier;
    }

    /**
     * Sets the value of the toopConceptIdentifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToopConceptIdentifier(String value) {
        this.toopConceptIdentifier = value;
    }

    /**
     * Gets the value of the toopConceptURI property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToopConceptURI() {
        return toopConceptURI;
    }

    /**
     * Sets the value of the toopConceptURI property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToopConceptURI(String value) {
        this.toopConceptURI = value;
    }

    /**
     * Gets the value of the toopConceptDomain property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToopConceptDomain() {
        return toopConceptDomain;
    }

    /**
     * Sets the value of the toopConceptDomain property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToopConceptDomain(String value) {
        this.toopConceptDomain = value;
    }

    /**
     * Gets the value of the toopNamespace property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToopNamespace() {
        return toopNamespace;
    }

    /**
     * Sets the value of the toopNamespace property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToopNamespace(String value) {
        this.toopNamespace = value;
    }

}
