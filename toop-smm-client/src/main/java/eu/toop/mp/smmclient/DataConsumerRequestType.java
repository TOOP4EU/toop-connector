
package eu.toop.mp.smmclient;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DataConsumerRequestType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DataConsumerRequestType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="DocumentRequestIndicator" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DocumentRequestTypeCode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DataConsumerRequestIdentifer" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DataRequestInfo" type="{}DataRequestInfoType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DataConsumerRequestType", propOrder = {
    "documentRequestIndicator",
    "documentRequestTypeCode",
    "dataConsumerRequestIdentifer",
    "dataRequestInfo"
})
public class DataConsumerRequestType {

    @XmlElement(name = "DocumentRequestIndicator", required = true)
    protected String documentRequestIndicator;
    @XmlElement(name = "DocumentRequestTypeCode", required = true)
    protected String documentRequestTypeCode;
    @XmlElement(name = "DataConsumerRequestIdentifer", required = true)
    protected String dataConsumerRequestIdentifer;
    @XmlElement(name = "DataRequestInfo")
    protected List<DataRequestInfoType> dataRequestInfo;

    /**
     * Gets the value of the documentRequestIndicator property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDocumentRequestIndicator() {
        return documentRequestIndicator;
    }

    /**
     * Sets the value of the documentRequestIndicator property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDocumentRequestIndicator(String value) {
        this.documentRequestIndicator = value;
    }

    /**
     * Gets the value of the documentRequestTypeCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDocumentRequestTypeCode() {
        return documentRequestTypeCode;
    }

    /**
     * Sets the value of the documentRequestTypeCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDocumentRequestTypeCode(String value) {
        this.documentRequestTypeCode = value;
    }

    /**
     * Gets the value of the dataConsumerRequestIdentifer property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDataConsumerRequestIdentifer() {
        return dataConsumerRequestIdentifer;
    }

    /**
     * Sets the value of the dataConsumerRequestIdentifer property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDataConsumerRequestIdentifer(String value) {
        this.dataConsumerRequestIdentifer = value;
    }

    /**
     * Gets the value of the dataRequestInfo property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dataRequestInfo property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDataRequestInfo().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DataRequestInfoType }
     * 
     * 
     */
    public List<DataRequestInfoType> getDataRequestInfo() {
        if (dataRequestInfo == null) {
            dataRequestInfo = new ArrayList<DataRequestInfoType>();
        }
        return this.dataRequestInfo;
    }

}
