
package eu.toop.mp.smmclient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DataRequestType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DataRequestType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="DocumentIdentifier" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DocumentIssueDate" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DocumentIssueTime" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DocumentVerseionIdentifier" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="CopyIndicator" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="BusinessProcessTypeIdentifier" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="SpecificationIdentification" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DataConsumer" type="{}DataConsumerType"/>
 *         &lt;element name="DataConsumerRequest" type="{}DataConsumerRequestType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DataRequestType", propOrder = {
    "documentIdentifier",
    "documentIssueDate",
    "documentIssueTime",
    "documentVerseionIdentifier",
    "copyIndicator",
    "businessProcessTypeIdentifier",
    "specificationIdentification",
    "dataConsumer",
    "dataConsumerRequest"
})
public class DataRequestType {

    @XmlElement(name = "DocumentIdentifier", required = true)
    protected String documentIdentifier;
    @XmlElement(name = "DocumentIssueDate", required = true)
    protected String documentIssueDate;
    @XmlElement(name = "DocumentIssueTime", required = true)
    protected String documentIssueTime;
    @XmlElement(name = "DocumentVerseionIdentifier", required = true)
    protected String documentVerseionIdentifier;
    @XmlElement(name = "CopyIndicator", required = true)
    protected String copyIndicator;
    @XmlElement(name = "BusinessProcessTypeIdentifier", required = true)
    protected String businessProcessTypeIdentifier;
    @XmlElement(name = "SpecificationIdentification", required = true)
    protected String specificationIdentification;
    @XmlElement(name = "DataConsumer", required = true)
    protected DataConsumerType dataConsumer;
    @XmlElement(name = "DataConsumerRequest", required = true)
    protected DataConsumerRequestType dataConsumerRequest;

    /**
     * Gets the value of the documentIdentifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDocumentIdentifier() {
        return documentIdentifier;
    }

    /**
     * Sets the value of the documentIdentifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDocumentIdentifier(String value) {
        this.documentIdentifier = value;
    }

    /**
     * Gets the value of the documentIssueDate property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDocumentIssueDate() {
        return documentIssueDate;
    }

    /**
     * Sets the value of the documentIssueDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDocumentIssueDate(String value) {
        this.documentIssueDate = value;
    }

    /**
     * Gets the value of the documentIssueTime property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDocumentIssueTime() {
        return documentIssueTime;
    }

    /**
     * Sets the value of the documentIssueTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDocumentIssueTime(String value) {
        this.documentIssueTime = value;
    }

    /**
     * Gets the value of the documentVerseionIdentifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDocumentVerseionIdentifier() {
        return documentVerseionIdentifier;
    }

    /**
     * Sets the value of the documentVerseionIdentifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDocumentVerseionIdentifier(String value) {
        this.documentVerseionIdentifier = value;
    }

    /**
     * Gets the value of the copyIndicator property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCopyIndicator() {
        return copyIndicator;
    }

    /**
     * Sets the value of the copyIndicator property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCopyIndicator(String value) {
        this.copyIndicator = value;
    }

    /**
     * Gets the value of the businessProcessTypeIdentifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBusinessProcessTypeIdentifier() {
        return businessProcessTypeIdentifier;
    }

    /**
     * Sets the value of the businessProcessTypeIdentifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBusinessProcessTypeIdentifier(String value) {
        this.businessProcessTypeIdentifier = value;
    }

    /**
     * Gets the value of the specificationIdentification property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSpecificationIdentification() {
        return specificationIdentification;
    }

    /**
     * Sets the value of the specificationIdentification property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSpecificationIdentification(String value) {
        this.specificationIdentification = value;
    }

    /**
     * Gets the value of the dataConsumer property.
     * 
     * @return
     *     possible object is
     *     {@link DataConsumerType }
     *     
     */
    public DataConsumerType getDataConsumer() {
        return dataConsumer;
    }

    /**
     * Sets the value of the dataConsumer property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataConsumerType }
     *     
     */
    public void setDataConsumer(DataConsumerType value) {
        this.dataConsumer = value;
    }

    /**
     * Gets the value of the dataConsumerRequest property.
     * 
     * @return
     *     possible object is
     *     {@link DataConsumerRequestType }
     *     
     */
    public DataConsumerRequestType getDataConsumerRequest() {
        return dataConsumerRequest;
    }

    /**
     * Sets the value of the dataConsumerRequest property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataConsumerRequestType }
     *     
     */
    public void setDataConsumerRequest(DataConsumerRequestType value) {
        this.dataConsumerRequest = value;
    }

}
