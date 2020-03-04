# toop-message-processor

The joint message process for both sides of the process

Latest release: **0.10.8** (2020-01-26)

# This project is superseded by [TOOP Connector NG](https://github.com/TOOP4EU/toop-connector-ng)

# How it works

Handle the way on DC side from DC backend to AS4 (1/4)
* Input from DC is received via Servlet `/from-dc` (from Demo UI - `HttpClientInvoker`)
* ASiC container is unwrapped and the `TDETOOPRequestType` object is extracted and processed asynchronously in class `MessageProcessorDCOutgoing`
    * A new Request ID (String) is created
    * The call to the Semantic Mapping Module (SMM) client is performed
    * The call to R2D2 client happens (first TOOP Directory than the SMPs)
    * Than the Message Exchange Module (MEM) performs the sending actions (currently interacting with the AS4 module with an HTTP call - so AS4 gateway is NOT part of this project)

Handle the way on DP side from AS4 to DP (2/4)
* Use `MEMDelegate.registerMessageHandler` - only `IMSDataRequest` and `IToopDataRequest` may be contained
* Handler for this is initialized in `MPWebAppListener`
    * ASiC container is parsed and message is forwarded to `MessageProcessorDPIncoming`

Handle the way on DP side from DP backend (3/4)
* Servlet `/from-dp` is present, the `ToopResponseMessage` object is extracted and processed asynchronously in class `MessageProcessorDPOutgoing`
    * The call to the SMM client is performed
    * The call to R2D2 client happens (based on sender PID)
    * Than MEM triggers the message sending via AS4 

Handle the way on DP side from AS4 to DC (4/4)
* Use `MEMDelegate.registerMessageHandler` - `IMSDataRequest`, `IToopDataRequest`, `IMSDataResponse` and `IToopDataResponse` must be contained
* Handler for this is initialized in `MPWebAppListener`
    * ASiC container is parsed and message is forwarded to `MessageProcessorDCIncoming`

# News and noteworthy

* v0.10.8 - 2020-01-26
    * Fixed a bug in the "to-dp" dumping (was always created 0 byte files)
    * Updated to toop-commons 0.10.8 with new GBM concept names
    * Changed the semantic mapping from remote GRLC to local file based
* v0.10.7 - 2020-01-23
    * Updated to phase4 0.9.7
    * Updated base libraries to work around Schematron bug
* v0.10.6-2 - 2019-12-06
    * Updated base libraries for updated truststore, updated Schematron and higher SMP client configurability
* v0.10.6-1 - 2019-11-08
    * Fixed an Exception when using the new WAR deployment
* v0.10.6 - 2019-10-17
    * Abstract all internal interfaces so they can be changed/replaced
    * Updated to the data model 1.4.1
* v0.10.5 - 2019-06-24
    * In case of errors, that lead to an immediate response, the document type is now correctly reversed
    * Added new optional configuration item `toop.keystore.type` with the default value `JKS`.
    * Added new optional configuration items `toop.mp.autoresponse.dpaddressid`, `toop.mp.autoresponse.dpidscheme`, `toop.mp.autoresponse.dpidvalue` and `toop.mp.autoresponse.dpname`
    * Added new optional configuration item `toop.smm.dp.mapping.error.fatal` 
* v0.10.4 - 2019-05-20
    * Fixed error with TOOP Response object layout when pushing back to queues in 1/4 and 2/4
* v0.10.3 - 2019-05-15
    * Fixed a regression in communication with the AS4 gateways
    * Fixed an exception because semantic mapping was partially involved even though it was not supported
* v0.10.2 - 2019-05-13
    * Fixed an error that semantic mapping on DP side was not performed, if the source concept was of type "TC"
    * Added Java 12 support
    * Improved debug logging
    * Added support for handling attachments in TOOP request and response (for 'Document' request and response) 
* v0.10.1 - 2019-03-29
    * Fixed an error that prevented semantic mapping from being invoked
    * Fixed potential startup time delays because of long random number initialization time on Linux
* v0.10.0 - 2019-03-20
    * Updated to data model v1.40
    * Added mandatory Schematron validation for steps 1/4 and 3/4 (that can be turned off)
    * Improved internal logging
    * Made AS4 message exchange more customizable (via SPI interfaces)
    * New subproject `toop-mem-phase4` to use `ph-as4` as AS4 message exchange module
        * perform build with Maven profile `phase4`
        * Set property `toop.mem.implementation` with value `mem-phase4` in file `toop-connector.properties` to use it at runtime
        * Add property `toop.phase4.datapath` with an absolute writable path into `toop-connector.properties` file
        * Ensure to create a ph-as4 configuration file (`as4.properties`) and reference it either via the environment variable `AS4_SERVER_CONFIG` or via the system property `as4.server.configfile`. See https://github.com/phax/ph-as4#as4properties for details.
        * Create a file `crypto.properties` (name is important) and add it to your classpath. See https://github.com/phax/ph-as4#cryptoproperties for details. 
* v0.9.3 - 2018-12-10
    * Backwards compatible update
* v0.9.2 - 2018-11-09
    * Non-backwards compatible update
* v0.9.1 - 2018-06-08
* v0.9.0 - 2018-05-17
