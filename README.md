# toop-message-processor
The joint message process for both sides of the process

## Status 2018-02-21 [PH]

**Done**
* Handle the way on DC side from DC backend to AS4 (1/4)
  * Input from DC is received via Servlet `/dcinput` (from Demo UI - `HttpClientInvoker`)
  * ASiC container is unwrapped and the `IMSDataRequest` object is extracted and processed asynchronously in class `MessageProcessorDCOutgoing`
    * A new Request ID (String) is created
    * The call to the SMM client is missing there currently stubbed
    * The call to R2D2 client happens but has no result because we don't have a directory yet
    * Therefore no endpoints are found the calls to MEM are purely theoretical :)
* Handle the way on DP side from DP backend (3/4)
  * Servlet `/dpinput` is present, the `ToopResponseMessage` object is extracted and processed asynchronously in class `MessageProcessorDPOutgoing`
    * The call to the SMM client is missing there currently stubbed
    * Call R2D2 on a single endpoint
    * Invoke MEM

**Open issues**
* Input parameter
  * Sender ID is missing 
* Handle the way on DP side from AS4 to DP (2/4)
  * Use `MEMDelegate.registerMessageHandler` - only `IMSDataRequest` and `IToopDataRequest` may be contained
* Handle the way on DP side from AS4 to DC (4/4)
  * Use `MEMDelegate.registerMessageHandler` - `IMSDataRequest`, `IToopDataRequest`, `IMSDataResponse` and `IToopDataResponse` must be contained
