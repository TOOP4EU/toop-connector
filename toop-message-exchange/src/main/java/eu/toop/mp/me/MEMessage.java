package eu.toop.mp.me;

import java.util.List;

/**
 * @author: myildiz
 * @date: 12.02.2018.
 */
public class MEMessage {

  private List<MEPayload> payloads;

  /**
   * For ease of use, get the first payload
   *
   * @return
   */
  public MEPayload head() {
    if (payloads.size() == 0)
      throw new IllegalStateException("There is no payload");

    return payloads.get(0);
  }

  public List<MEPayload> getPayloads() {
    return payloads;
  }

  public void setPayloads(List<MEPayload> payloads) {
    this.payloads = payloads;
  }


}
