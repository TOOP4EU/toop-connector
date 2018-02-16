package eu.toop.mp.me;

/**
 * Factory methods for creating payload objects
 *
 * @author: myildiz
 * @date: 15.02.2018.
 */
public class MEPayloadFactory {
  public static MEPayload createPayload(String payloadId, String contentType, String name, byte[] data) {
    MEPayload pl = new MEPayload();
    pl.setPayloadId(payloadId);
    pl.setContentType(contentType);
    pl.setMimeType(contentType);
    pl.setName(name);
    pl.setData(data);
    return pl;
  }

  public static MEPayload createPayload(String payloadId, String contentType, String mimeType, String characterSet, byte[] data) {
    MEPayload pl = new MEPayload();
    pl.setContentType(contentType);
    pl.setPayloadId(payloadId);
    pl.setMimeType(mimeType);
    pl.setCharacterSet(characterSet);
    pl.setData(data);
    return pl;
  }

  public static MEPayload createPayload(String payloadId, String contentType, byte[] data) {
    MEPayload pl = new MEPayload();
    pl.setPayloadId(payloadId);
    pl.setMimeType(contentType);
    pl.setContentType(contentType);
    pl.setData(data);
    return pl;
  }

  public static MEPayload createPayload(String contentType, byte[] data) {
    MEPayload pl = new MEPayload();
    pl.setContentType(contentType);
    pl.setMimeType(contentType);
    pl.setData(data);
    return pl;
  }
}
