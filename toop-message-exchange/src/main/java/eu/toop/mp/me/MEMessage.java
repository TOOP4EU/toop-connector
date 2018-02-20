package eu.toop.mp.me;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableObject;

/**
 * @author: myildiz
 * @date: 12.02.2018.
 */
public class MEMessage {
  private final List<MEPayload> _payloads = new ArrayList<> ();

  public MEMessage () {
  }

  public MEMessage (@Nonnull final MEPayload aPayload) {
    ValueEnforcer.notNull (aPayload, "Payload");
    _payloads.add (aPayload);
  }

  /**
   * For ease of use, get the first payload
   *
   * @return the first payload
   * @throws IllegalStateException
   *           in case non is contained
   */
  @Nonnull
  public MEPayload head () {
    if (_payloads.isEmpty ())
      throw new IllegalStateException ("There is no payload");
    return _payloads.get (0);
  }

  @Nonnull
  @ReturnsMutableObject
  public List<MEPayload> getPayloads () {
    return _payloads;
  }
}
