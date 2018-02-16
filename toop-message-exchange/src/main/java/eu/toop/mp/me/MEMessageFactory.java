package eu.toop.mp.me;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author: myildiz
 * @date: 15.02.2018.
 */
public class MEMessageFactory {


  /**
   * Default constructor
   */
  public static MEMessage createMEMessage() {
    MEMessage meMessage = new MEMessage();
    //initialize an empty payload list
    ArrayList<MEPayload> list = new ArrayList<>();
    meMessage.setPayloads(list);
    return meMessage;
  }

  /**
   * Constructor for creating a message with one payload.
   *
   * @param payload
   */
  public static MEMessage createMEMessage(MEPayload payload) {
    return createMEMessage(Arrays.asList(payload));
  }


  /**
   * Constructor for creating a message with one payload
   *
   * @param payload
   */
  public static MEMessage createMEMessage(String contentType, byte[] payload) {
    return createMEMessage(Arrays.asList(MEPayloadFactory.createPayload(contentType, payload)));
  }

  /**
   * Construct a message with multiple payloads
   *
   * @param payloadList
   */
  public static MEMessage createMEMessage(List<MEPayload> payloadList) {
    MEMessage meMessage = new MEMessage();
    ArrayList<MEPayload> list = new ArrayList<>(payloadList);
    meMessage.setPayloads(list);
    return meMessage;
  }
}
