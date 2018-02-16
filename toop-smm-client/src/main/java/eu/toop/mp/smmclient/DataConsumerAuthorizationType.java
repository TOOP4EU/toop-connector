
package eu.toop.mp.smmclient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DataConsumerAuthorizationType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DataConsumerAuthorizationType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ConsentToken" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DataConsumerAuthorizationType", propOrder = {
    "consentToken"
})
public class DataConsumerAuthorizationType {

    @XmlElement(name = "ConsentToken", required = true)
    protected String consentToken;

    /**
     * Gets the value of the consentToken property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConsentToken() {
        return consentToken;
    }

    /**
     * Sets the value of the consentToken property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConsentToken(String value) {
        this.consentToken = value;
    }

}
