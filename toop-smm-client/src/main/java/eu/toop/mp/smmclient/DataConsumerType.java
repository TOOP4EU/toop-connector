
package eu.toop.mp.smmclient;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DataConsumerType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DataConsumerType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="DCName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DCIdentifier" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DCElectronicAddressIdentifier" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DCCountry" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DCName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="DataSubject" type="{}DataSubjectType" minOccurs="0"/>
 *         &lt;element name="DataOwner" type="{}DataOwnerType" minOccurs="0"/>
 *         &lt;element name="DataConsumerAuthorization" type="{}DataConsumerAuthorizationType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DataConsumerType", propOrder = {
    "content"
})
public class DataConsumerType {

    @XmlElementRefs({
        @XmlElementRef(name = "DataSubject", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "DataOwner", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "DataConsumerAuthorization", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "DCCountry", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "DCIdentifier", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "DCElectronicAddressIdentifier", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "DCName", type = JAXBElement.class, required = false)
    })
    protected List<JAXBElement<?>> content;

    /**
     * Gets the rest of the content model. 
     * 
     * <p>
     * You are getting this "catch-all" property because of the following reason: 
     * The field name "DCName" is used by two different parts of a schema. See: 
     * line 24 of file:/Users/yerlibilgin/dev/TOOP/toop-message-processor/toop-smm-client/src/main/resources/datarequest.xsd
     * line 20 of file:/Users/yerlibilgin/dev/TOOP/toop-message-processor/toop-smm-client/src/main/resources/datarequest.xsd
     * <p>
     * To get rid of this property, apply a property customization to one 
     * of both of the following declarations to change their names: 
     * Gets the value of the content property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the content property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getContent().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link DataSubjectType }{@code >}
     * {@link JAXBElement }{@code <}{@link DataOwnerType }{@code >}
     * {@link JAXBElement }{@code <}{@link DataConsumerAuthorizationType }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * 
     */
    public List<JAXBElement<?>> getContent() {
        if (content == null) {
            content = new ArrayList<JAXBElement<?>>();
        }
        return this.content;
    }

}
