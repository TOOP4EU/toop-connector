
package eu.toop.mp.smmclient;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DataOwnerType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DataOwnerType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="NaturalPersonFirstName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="NaturalPersonFamilyName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="NaturalPersonBirthPlace" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="NaturalPersonBirthData" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="NaturalPersonIdentifier" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="NaturalPersonNationality" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DataOwnerType", propOrder = {
    "naturalPersonFirstName",
    "naturalPersonFamilyName",
    "naturalPersonBirthPlace",
    "naturalPersonBirthData",
    "naturalPersonIdentifier",
    "naturalPersonNationality"
})
public class DataOwnerType {

    @XmlElement(name = "NaturalPersonFirstName", required = true)
    protected String naturalPersonFirstName;
    @XmlElement(name = "NaturalPersonFamilyName", required = true)
    protected String naturalPersonFamilyName;
    @XmlElement(name = "NaturalPersonBirthPlace", required = true)
    protected String naturalPersonBirthPlace;
    @XmlElement(name = "NaturalPersonBirthData", required = true)
    protected String naturalPersonBirthData;
    @XmlElement(name = "NaturalPersonIdentifier", required = true)
    protected String naturalPersonIdentifier;
    @XmlElement(name = "NaturalPersonNationality", required = true)
    protected String naturalPersonNationality;

    /**
     * Gets the value of the naturalPersonFirstName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNaturalPersonFirstName() {
        return naturalPersonFirstName;
    }

    /**
     * Sets the value of the naturalPersonFirstName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNaturalPersonFirstName(String value) {
        this.naturalPersonFirstName = value;
    }

    /**
     * Gets the value of the naturalPersonFamilyName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNaturalPersonFamilyName() {
        return naturalPersonFamilyName;
    }

    /**
     * Sets the value of the naturalPersonFamilyName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNaturalPersonFamilyName(String value) {
        this.naturalPersonFamilyName = value;
    }

    /**
     * Gets the value of the naturalPersonBirthPlace property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNaturalPersonBirthPlace() {
        return naturalPersonBirthPlace;
    }

    /**
     * Sets the value of the naturalPersonBirthPlace property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNaturalPersonBirthPlace(String value) {
        this.naturalPersonBirthPlace = value;
    }

    /**
     * Gets the value of the naturalPersonBirthData property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNaturalPersonBirthData() {
        return naturalPersonBirthData;
    }

    /**
     * Sets the value of the naturalPersonBirthData property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNaturalPersonBirthData(String value) {
        this.naturalPersonBirthData = value;
    }

    /**
     * Gets the value of the naturalPersonIdentifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNaturalPersonIdentifier() {
        return naturalPersonIdentifier;
    }

    /**
     * Sets the value of the naturalPersonIdentifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNaturalPersonIdentifier(String value) {
        this.naturalPersonIdentifier = value;
    }

    /**
     * Gets the value of the naturalPersonNationality property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNaturalPersonNationality() {
        return naturalPersonNationality;
    }

    /**
     * Sets the value of the naturalPersonNationality property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNaturalPersonNationality(String value) {
        this.naturalPersonNationality = value;
    }

}
