
package eu.toop.mp.smmclient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DataSubjectType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DataSubjectType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="OrganisationIdentifer" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="OrganisationName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="CountryOfRegistration" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DataSubjectType", propOrder = {
    "organisationIdentifer",
    "organisationName",
    "countryOfRegistration"
})
public class DataSubjectType {

    @XmlElement(name = "OrganisationIdentifer", required = true)
    protected String organisationIdentifer;
    @XmlElement(name = "OrganisationName", required = true)
    protected String organisationName;
    @XmlElement(name = "CountryOfRegistration", required = true)
    protected String countryOfRegistration;

    /**
     * Gets the value of the organisationIdentifer property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrganisationIdentifer() {
        return organisationIdentifer;
    }

    /**
     * Sets the value of the organisationIdentifer property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrganisationIdentifer(String value) {
        this.organisationIdentifer = value;
    }

    /**
     * Gets the value of the organisationName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrganisationName() {
        return organisationName;
    }

    /**
     * Sets the value of the organisationName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrganisationName(String value) {
        this.organisationName = value;
    }

    /**
     * Gets the value of the countryOfRegistration property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCountryOfRegistration() {
        return countryOfRegistration;
    }

    /**
     * Sets the value of the countryOfRegistration property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCountryOfRegistration(String value) {
        this.countryOfRegistration = value;
    }

}
