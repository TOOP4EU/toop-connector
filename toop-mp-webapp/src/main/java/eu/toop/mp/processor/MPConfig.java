package eu.toop.mp.processor;

import java.io.File;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.asic.SignatureHelper;
import com.helger.commons.io.file.FileHelper;

/**
 * Message Processor configuration
 *
 * @author Philip Helger
 *
 */
@Immutable
public final class MPConfig {
  private static final SignatureHelper SH = new SignatureHelper (FileHelper.getInputStream (new File ("src/main/resources/demo-keystore.jks")),
                                                                 "password", null, "password");

  private MPConfig () {
  }

  /**
   *
   * @return The {@link SignatureHelper} singleton.
   */
  @Nonnull
  public static SignatureHelper getSignatureHelper () {
    return SH;
  }
}
