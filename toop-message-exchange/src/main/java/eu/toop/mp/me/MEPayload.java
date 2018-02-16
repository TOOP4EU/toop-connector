package eu.toop.mp.me;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author: myildiz
 * @date: 15.02.2018.
 */
public class MEPayload {
  /**
   * Type of the payload
   */
  @Nonnull
  private String contentType;

  /**
   * Optional name (filen name, content disposition ) in the SWA payload
   */
  @Nullable
  private String name;

  /**
   * Optional id for the payload. If left empty, a default id will be used. i.e. payload_X@toop.eu
   */
  @Nullable
  private String payloadId;

  /**
   * Mimetype for the attachment object. If left emtpy the default value of <code>contentType</code> will be used
   */
  private String mimeType;

  /**
   * optional charset for text type payloads
   */
  @Nullable
  private String characterSet;


  /**
   * The actual payload content
   */
  @Nonnull
  private byte[] data;

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    /*
     * TODO: a deep copy might be necessary
     */
    this.data = data;
  }

  public String getPayloadId() {
    return payloadId;
  }

  public void setPayloadId(String payloadId) {
    this.payloadId = payloadId;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getCharacterSet() {
    return characterSet;
  }

  public void setCharacterSet(String characterSet) {
    this.characterSet = characterSet;
  }

  @Override
  public String toString() {
    int len = 0;
    if (data != null)
      len = data.length;

    return "Payload [" + payloadId + ", " + mimeType + "], length: " + len;
  }

}
