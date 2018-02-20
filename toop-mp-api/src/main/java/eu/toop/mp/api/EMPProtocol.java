package eu.toop.mp.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * Contains all protocols supported by MP
 *
 * @author Philip Helger
 */
public enum EMPProtocol implements IHasID<String> {
  AS4 ("as4");

  public static final EMPProtocol DEFAULT = AS4;

  private final String m_sID;

  private EMPProtocol (@Nonnull @Nonempty final String sID) {
    m_sID = sID;
  }

  @Nonnull
  @Nonempty
  public String getID () {
    return m_sID;
  }

  @Nullable
  public static EMPProtocol getFromIDOrNull (@Nullable final String sID) {
    return EnumHelper.getFromIDOrNull (EMPProtocol.class, sID);
  }
}
