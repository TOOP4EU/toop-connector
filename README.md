# toop-message-processor
The joint message process for both sides of the process

## Status 2018-02-20 [PH]

**Done**
* Handle the way on DC side from DC backend to AS4 (1/4)
  * Input from DC is received via Servlet `/dcinput` (from Demo UI - `HttpClientInvoker`)
  * ASiC container is unwrapped and the `IMSDataRequest` object is extracted and processed asynchronously in class `MessageProcessorDCOutgoing`
    * A new message ID (String) is created
    * The call to the SMM client is missing there currently stubbed
    * The call to R2D2 client happens but has no result because we don't have a directory yet
    * Therefore no endpoints are found the calls to MEM are purely theoretical :)

**Next steps**
* Get an initial message out
* Handle the way on DP side from AS4 to DP (2/4)
  * Use `MEMDelegate.registerMessageHandler` - only `IMSDataRequest` and `IToopDataRequest` may be contained
* Handle the way on DP side from DP backend (3/4)
  * Create servlet `/dpinput` and define the data structure
  * Call SMM
  * Call R2D2 on a single endpoint
  * Invoke MEM
* Handle the way on DP side from AS4 to DC (4/4)
  * Use `MEMDelegate.registerMessageHandler` - `IMSDataRequest`, `IToopDataRequest`, `IMSDataResponse` and `IToopDataResponse` must be contained
