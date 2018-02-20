package eu.toop.mp.me;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;

import eu.toop.mp.r2d2client.IR2D2Endpoint;

/**
 * @author: myildiz
 * @date: 15.02.2018.
 */
@Immutable
public class GatewayRoutingMetadata implements Serializable {
  /**
   * document type ID
   */
  private final String _documentTypeId;

  /**
   * Process ID
   */
  private final String _processId;

  /**
   * The target endpoint
   */
  private final IR2D2Endpoint _endpoint;

  public GatewayRoutingMetadata (@Nonnull @Nonempty final String sDocumentTypeID,
                                 @Nonnull @Nonempty final String sProcessID, @Nonnull final IR2D2Endpoint aEndpoint) {
    ValueEnforcer.notEmpty (sDocumentTypeID, "DocumentTypeID");
    ValueEnforcer.notEmpty (sProcessID, "ProcessID");
    ValueEnforcer.notNull (aEndpoint, "Endpoint");
    _documentTypeId = sDocumentTypeID;
    _processId = sProcessID;
    _endpoint = aEndpoint;
  }

  @Nonnull
  @Nonempty
  public String getDocumentTypeId () {
    return _documentTypeId;
  }

  @Nonnull
  @Nonempty
  public String getProcessId () {
    return _processId;
  }

  @Nonnull
  public IR2D2Endpoint getEndpoint () {
    return _endpoint;
  }
}
