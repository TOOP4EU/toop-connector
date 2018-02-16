package eu.toop.mp.me;

import com.helger.commons.annotations.DevelopersNote;

import eu.toop.mp.r2d2client.IR2D2Endpoint;

/**
 * @author: myildiz
 * @date: 15.02.2018.
 */
// @DevelopersNote ("Currently there is no IR2D2Endpoint so the endpoint is
// encapsulated directly")
@DevelopersNote ("Now there is :)")
public class GatewayRoutingMetadata
{
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

  public String getDocumentTypeId ()
  {
    return documentTypeId;
  }

  public String getProcessId ()
  {
    return processId;
  }

  public IR2D2Endpoint getEndpoint ()
  {
    return endpoint;
  }

  public void setDocumentTypeId (final String documentTypeId)
  {
    this.documentTypeId = documentTypeId;
  }

  public void setProcessId (final String processId)
  {
    this.processId = processId;
  }

  public void setEndpoint (final IR2D2Endpoint endpoint)
  {
    this.endpoint = endpoint;
  }
}
