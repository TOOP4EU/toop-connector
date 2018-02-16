
package eu.toop.mp.smmclient;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the eu.toop.mp.smmclient package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _DataRequest_QNAME = new QName("", "DataRequest");
    private final static QName _DataConsumerTypeDataOwner_QNAME = new QName("", "DataOwner");
    private final static QName _DataConsumerTypeDCName_QNAME = new QName("", "DCName");
    private final static QName _DataConsumerTypeDataConsumerAuthorization_QNAME = new QName("", "DataConsumerAuthorization");
    private final static QName _DataConsumerTypeDataSubject_QNAME = new QName("", "DataSubject");
    private final static QName _DataConsumerTypeDCElectronicAddressIdentifier_QNAME = new QName("", "DCElectronicAddressIdentifier");
    private final static QName _DataConsumerTypeDCCountry_QNAME = new QName("", "DCCountry");
    private final static QName _DataConsumerTypeDCIdentifier_QNAME = new QName("", "DCIdentifier");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: eu.toop.mp.smmclient
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link DataRequestType }
     * 
     */
    public DataRequestType createDataRequestType() {
        return new DataRequestType();
    }

    /**
     * Create an instance of {@link DataConsumerAuthorizationType }
     * 
     */
    public DataConsumerAuthorizationType createDataConsumerAuthorizationType() {
        return new DataConsumerAuthorizationType();
    }

    /**
     * Create an instance of {@link DataOwnerType }
     * 
     */
    public DataOwnerType createDataOwnerType() {
        return new DataOwnerType();
    }

    /**
     * Create an instance of {@link DataRequestInfoType }
     * 
     */
    public DataRequestInfoType createDataRequestInfoType() {
        return new DataRequestInfoType();
    }

    /**
     * Create an instance of {@link DataConsumerType }
     * 
     */
    public DataConsumerType createDataConsumerType() {
        return new DataConsumerType();
    }

    /**
     * Create an instance of {@link DataConsumerRequestType }
     * 
     */
    public DataConsumerRequestType createDataConsumerRequestType() {
        return new DataConsumerRequestType();
    }

    /**
     * Create an instance of {@link DataSubjectType }
     * 
     */
    public DataSubjectType createDataSubjectType() {
        return new DataSubjectType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DataRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "DataRequest")
    public JAXBElement<DataRequestType> createDataRequest(DataRequestType value) {
        return new JAXBElement<DataRequestType>(_DataRequest_QNAME, DataRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DataOwnerType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "DataOwner", scope = DataConsumerType.class)
    public JAXBElement<DataOwnerType> createDataConsumerTypeDataOwner(DataOwnerType value) {
        return new JAXBElement<DataOwnerType>(_DataConsumerTypeDataOwner_QNAME, DataOwnerType.class, DataConsumerType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "DCName", scope = DataConsumerType.class)
    public JAXBElement<String> createDataConsumerTypeDCName(String value) {
        return new JAXBElement<String>(_DataConsumerTypeDCName_QNAME, String.class, DataConsumerType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DataConsumerAuthorizationType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "DataConsumerAuthorization", scope = DataConsumerType.class)
    public JAXBElement<DataConsumerAuthorizationType> createDataConsumerTypeDataConsumerAuthorization(DataConsumerAuthorizationType value) {
        return new JAXBElement<DataConsumerAuthorizationType>(_DataConsumerTypeDataConsumerAuthorization_QNAME, DataConsumerAuthorizationType.class, DataConsumerType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DataSubjectType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "DataSubject", scope = DataConsumerType.class)
    public JAXBElement<DataSubjectType> createDataConsumerTypeDataSubject(DataSubjectType value) {
        return new JAXBElement<DataSubjectType>(_DataConsumerTypeDataSubject_QNAME, DataSubjectType.class, DataConsumerType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "DCElectronicAddressIdentifier", scope = DataConsumerType.class)
    public JAXBElement<String> createDataConsumerTypeDCElectronicAddressIdentifier(String value) {
        return new JAXBElement<String>(_DataConsumerTypeDCElectronicAddressIdentifier_QNAME, String.class, DataConsumerType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "DCCountry", scope = DataConsumerType.class)
    public JAXBElement<String> createDataConsumerTypeDCCountry(String value) {
        return new JAXBElement<String>(_DataConsumerTypeDCCountry_QNAME, String.class, DataConsumerType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "DCIdentifier", scope = DataConsumerType.class)
    public JAXBElement<String> createDataConsumerTypeDCIdentifier(String value) {
        return new JAXBElement<String>(_DataConsumerTypeDCIdentifier_QNAME, String.class, DataConsumerType.class, value);
    }

}
