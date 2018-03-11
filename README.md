# toop-message-processor
The joint message process for both sides of the process

## Done
* Handle the way on DC side from DC backend to AS4 (1/4)
  * Input from DC is received via Servlet `/from-dc` (from Demo UI - `HttpClientInvoker`)
  * ASiC container is unwrapped and the `IMSDataRequest` object is extracted and processed asynchronously in class `MessageProcessorDCOutgoing`
    * A new Request ID (String) is created
    * The call to the SMM client is missing there currently stubbed
    * The call to R2D2 client happens but has no result because we don't have a directory yet
    * Therefore no endpoints are found the calls to MEM are purely theoretical :)
* Handle the way on DP side from AS4 to DP (2/4)
  * Use `MEMDelegate.registerMessageHandler` - only `IMSDataRequest` and `IToopDataRequest` may be contained
  * Handler for this is initialized in `MPWebAppListener`
    * ASiC contianer is parsed and message is forwarded to `MessageProcessorDPIncoming`
* Handle the way on DP side from DP backend (3/4)
  * Servlet `/from-dp` is present, the `ToopResponseMessage` object is extracted and processed asynchronously in class `MessageProcessorDPOutgoing`
    * The call to the SMM client is missing there currently stubbed
    * The call to R2D2 client happens (based on sender PID) but has no result because we don't have a directory yet
    * Therefore no endpoints are found the calls to MEM are purely theoretical :)
* Handle the way on DP side from AS4 to DC (4/4)
  * Use `MEMDelegate.registerMessageHandler` - `IMSDataRequest`, `IToopDataRequest`, `IMSDataResponse` and `IToopDataResponse` must be contained
  * Handler for this is initialized in `MPWebAppListener`
    * ASiC contianer is parsed and message is forwarded to `MessageProcessorDCIncoming`

## Open issues

* Step 1/4
  * Determine what to send to SMM
  * Get R2D2 infrastructure up and running
* Step 2/4
  * Determine what to do
  * Forward to toop-connector
* Step 3/4
  * Determine what to send to SMM
  * Get R2D2 infrastructure up and running
* Step 4/4
  * Determine what to do
  * Forward to toop-connector
  