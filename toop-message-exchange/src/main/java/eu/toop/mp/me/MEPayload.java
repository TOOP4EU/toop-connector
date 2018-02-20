package eu.toop.mp.me;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.string.StringHelper;

/**
 * @author: myildiz
 * @date: 15.02.2018.
 */
@Immutable
public final class MEPayload {
  /**
   * Type of the payload
   */
  private final IMimeType _mimeType;

  /**
   * Optional id for the payload. If left empty, a default id will be used. i.e.
   * payload_X@toop.eu
   */
  private final String _payloadId;

  /**
   * The actual payload content
   */
  private final byte[] _data;

  @Nonnull
  @Nonempty
  public static String createRandomPayloadID () {
    return UUID.randomUUID ().toString () + "@mp.toop";
  }

  public MEPayload (@Nonnull final IMimeType aMimeType, @Nullable final String sPayloadID,
                    @Nonnull final byte[] aData) {
    ValueEnforcer.notNull (aMimeType, "MimeType");
    ValueEnforcer.notNull (aData, "Data");

    _mimeType = aMimeType;
    // Ensure a payload is present
    _payloadId = StringHelper.hasText (sPayloadID) ? sPayloadID : createRandomPayloadID ();
    _data = aData;
  }

  @Nonnull
  public IMimeType getMimeType () {
    return _mimeType;
  }

  @Nonnull
  public String getMimeTypeString () {
    return _mimeType.getAsString ();
  }

  @Nonnull
  public String getPayloadId () {
    return _payloadId;
  }

  @Nonnull
  @ReturnsMutableObject
  public byte[] getData () {
    return _data;
  }

  @Override
  public String toString () {
    return "Payload [" + _payloadId + ", " + _mimeType.getAsString () + "], length: " + _data.length;
  }
}
