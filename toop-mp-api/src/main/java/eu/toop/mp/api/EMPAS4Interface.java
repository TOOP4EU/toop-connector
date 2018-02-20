package eu.toop.mp.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * Contains all supported AS4 interfaces by MP/MEM. It requires that
 * {@link EMPProtocol#AS4} was selected.
 *
 * @author Philip Helger
 */
public enum EMPAS4Interface implements IHasID<String> {
  /**
   * CEF conformance test WebService interface
   */
  CEF_CONFORMANCE_TEST ("cef-conformance-test");

  public static final EMPAS4Interface DEFAULT = CEF_CONFORMANCE_TEST;

  private final String m_sID;

  private EMPAS4Interface (@Nonnull @Nonempty final String sID) {
    m_sID = sID;
  }

  @Nonnull
  @Nonempty
  public String getID () {
    return m_sID;
  }

  @Nullable
  public static EMPAS4Interface getFromIDOrNull (@Nullable final String sID) {
    return EnumHelper.getFromIDOrNull (EMPAS4Interface.class, sID);
  }
}
