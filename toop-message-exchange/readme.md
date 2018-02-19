# Message Exchange Module
This module is a bridge between the Message processor and AS4 gateways. It provides two interfaces:
* `sendMessage()`
* receiveMessage by implementing the `IMessageHandler.handleMessage(MEMessage meMessage);`

In order to access these interfaces please used the <code>eu.toop.mp.me.MEMDelegate</code> class.

## Sending a message

In order to send a message to the gateway please use 
```java
GatewayRoutingMetadata metadata = new GatewayRoutingMetadata();
metadata.setDocumentTypeId("top-sercret-pdf-documents-only");
metadata.setProcessId("dummy-process");
metadata.setEndpoint(sampleEndpoint());

String payloadId = "xmlpayload@dp";
String contentType = "application/xml";
byte[] payloadData = "<sample>xml</sample>".getBytes();

MEPayload payload = MEPayloadFactory.createPayload(payloadId, contentType, payloadData);
MEMessage meMessage = MEMessageFactory.createMEMessage(payload);

MEMDelegate.get().sendMessage(metadata, meMessage);
```

This message will create this AS4 message:

```xml
------=_Part_1_1846412426.1518908192312
Content-Type: text/xml; charset=utf-8

<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"><SOAP-ENV:Header><ns2:Messaging xmlns:ns2="http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/" xmlns:soapenv="http://www.w3.org/2003/05/soap-envelope" soapenv:mustUnderstand="true">
  <ns2:UserMessage>
    <ns2:MessageInfo>
      <ns2:Timestamp>2018-02-17T22:56:32.260Z</ns2:Timestamp>
      <ns2:MessageId>970cde01-bd25-458a-bb23-655cb6931c49@message-exchange.toop.eu</ns2:MessageId>
    </ns2:MessageInfo>
    <ns2:PartyInfo>
      <ns2:From>
        <ns2:PartyId type="urn:oasis:names:tc:ebcore:partyid-type:unregistered">message-exchange</ns2:PartyId>
        <ns2:Role>http://toop.eu/identifiers/roles/dp</ns2:Role>
      </ns2:From>
      <ns2:To>
        <ns2:PartyId type="urn:oasis:names:tc:ebcore:partyid-type:unregistered">holodeck</ns2:PartyId>
        <ns2:Role>http://toop.eu/identifiers/roles/dp</ns2:Role>
      </ns2:To>
    </ns2:PartyInfo>

    <ns2:CollaborationInfo>
      <ns2:Service>http://toop.eu/identifiers/services/submit</ns2:Service>
      <ns2:Action>http://toop.eu/identifiers/actions/submit</ns2:Action>
      <ns2:ConversationId>1</ns2:ConversationId>
    </ns2:CollaborationInfo>

    <ns2:MessageProperties>
            <ns2:Property name="MessageId">423789fa-335d-4290-b14c-67253a60f7c4@message-exchange.toop.eu</ns2:Property>
      <ns2:Property name="ConversationId">1</ns2:Property>
      <ns2:Property name="Service">dummy-process</ns2:Property>
      <ns2:Property name="Action">top-sercret-pdf-documents-only</ns2:Property>
      <ns2:Property name="ToPartyId">DS</ns2:Property>
      <ns2:Property name="ToPartyRole">http://toop.eu/identifiers/roles/dc</ns2:Property>
      <ns2:Property name="FromPartyId">holodeck</ns2:Property>
      <ns2:Property name="FromPartyRole">http://toop.eu/identifiers/roles/dp</ns2:Property>
      <ns2:Property name="originalSender">originalSender</ns2:Property>
      <ns2:Property name="finalRecipient">var1::var2</ns2:Property>
    </ns2:MessageProperties>

    <ns2:PayloadInfo>
      <ns2:PartInfo href="cid:xmlpayload@dp">
        <ns2:PartProperties>
          <ns2:Property name="MimeType">application/xml</ns2:Property>
        </ns2:PartProperties>
      </ns2:PartInfo>
    </ns2:PayloadInfo>

  </ns2:UserMessage>
</ns2:Messaging></SOAP-ENV:Header><SOAP-ENV:Body/></SOAP-ENV:Envelope>
------=_Part_1_1846412426.1518908192312
Content-Type: application/xml
Content-ID: <xmlpayload@dp>

<sample>xml</sample>
------=_Part_1_1846412426.1518908192312--
```

This message will be sent to the gateway, where a new message will be created based on the `//MessageProperties/Property` values.

## Receiving a message

Sample code is as follows:

```java
IMessageHandler handler = new IMessageHandler() {
  @Override
  public void handleMessage(MEMessage meMessage) {
    System.out.println("hooray! I Got a message");
  }
};

MEMDelegate.get().registerMessageHandler(handler);
```

## Configuring the Message Exchange Module

Coming soon...

## Sample Test Case

Please run `eu.toop.mp.me.TestSendReceive.testSendReceive()` with junit, or simply run a `mvn install` or `mvn verify` under the `toop-message-exchange module` folder to run the test.