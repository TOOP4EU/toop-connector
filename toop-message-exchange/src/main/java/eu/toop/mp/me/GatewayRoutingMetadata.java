package eu.toop.mp.me;

import eu.toop.mp.r2d2client.IR2D2Endpoint;

/**
 * @author: myildiz
 * @date: 15.02.2018.
 */
public class GatewayRoutingMetadata {
  /**
   * document type ID
   */
  private String documentTypeId;

  /**
   * Process ID
   */
  private String processId;

  /**
   * The target endpoint
   */
  private IR2D2Endpoint endpoint;

  public String getDocumentTypeId() {
    return documentTypeId;
  }

  public String getProcessId() {
    return processId;
  }

  public IR2D2Endpoint getEndpoint() {
    return endpoint;
  }

  public void setDocumentTypeId(final String documentTypeId) {
    this.documentTypeId = documentTypeId;
  }

  public void setProcessId(final String processId) {
    this.processId = processId;
  }

  public void setEndpoint(final IR2D2Endpoint endpoint) {
    this.endpoint = endpoint;
  }
}
