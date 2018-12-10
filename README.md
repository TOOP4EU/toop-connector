# toop-message-processor

The joint message process for both sides of the process

Latest release: **0.9.3** (2018-12-10)

## Done

* Handle the way on DC side from DC backend to AS4 (1/4)
  * Input from DC is received via Servlet `/from-dc` (from Demo UI - `HttpClientInvoker`)
  * ASiC container is unwrapped and the `TDETOOPRequestType` object is extracted and processed asynchronously in class `MessageProcessorDCOutgoing`
    * A new Request ID (String) is created
    * The call to the Semantic Mapping Module (SMM) client is performed
    * The call to R2D2 client happens (first TOOP Directory than the SMPs)
    * Than the Message Exchange Module (MEM) performs the sending actions (currently interacting with the AS4 module with an HTTP call - so AS4 gateway is NOT part of this project)
* Handle the way on DP side from AS4 to DP (2/4)
  * Use `MEMDelegate.registerMessageHandler` - only `IMSDataRequest` and `IToopDataRequest` may be contained
  * Handler for this is initialized in `MPWebAppListener`
    * ASiC container is parsed and message is forwarded to `MessageProcessorDPIncoming`
* Handle the way on DP side from DP backend (3/4)
  * Servlet `/from-dp` is present, the `ToopResponseMessage` object is extracted and processed asynchronously in class `MessageProcessorDPOutgoing`
    * The call to the SMM client is performed
    * The call to R2D2 client happens (based on sender PID)
    * Than MEM triggers the message sending via AS4 
* Handle the way on DP side from AS4 to DC (4/4)
  * Use `MEMDelegate.registerMessageHandler` - `IMSDataRequest`, `IToopDataRequest`, `IMSDataResponse` and `IToopDataResponse` must be contained
  * Handler for this is initialized in `MPWebAppListener`
    * ASiC container is parsed and message is forwarded to `MessageProcessorDCIncoming`

  