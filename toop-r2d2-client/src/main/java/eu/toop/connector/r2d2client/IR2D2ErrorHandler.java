package eu.toop.connector.r2d2client;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.error.level.EErrorLevel;

import eu.toop.commons.error.IToopErrorCode;

/**
 * Custom R2D2 error handler callback
 *
 * @author Philip Helger
 * @since 0.10.6
 */
public interface IR2D2ErrorHandler extends Serializable
{
  /**
   * The main error handler method to be implemented
   *
   * @param eErrorLevel
   *        Error level. Never <code>null</code>.
   * @param sMsg
   *        Error text. Never <code>null</code>.
   * @param t
   *        Optional exception. May be <code>null</code>.
   * @param eCode
   *        The TOOP specific error code. Never <code>null</code>.
   */
  void onMessage (@Nonnull EErrorLevel eErrorLevel,
                  @Nonnull String sMsg,
                  @Nullable Throwable t,
                  @Nonnull IToopErrorCode eCode);

  default void onWarning (@Nonnull final String sMsg, @Nonnull final IToopErrorCode eCode)
  {
    onMessage (EErrorLevel.WARN, sMsg, null, eCode);
  }

  default void onError (@Nonnull final String sMsg, @Nonnull final IToopErrorCode eCode)
  {
    onMessage (EErrorLevel.ERROR, sMsg, null, eCode);
  }

  default void onError (@Nonnull final String sMsg, @Nullable final Throwable t, @Nonnull final IToopErrorCode eCode)
  {
    onMessage (EErrorLevel.ERROR, sMsg, t, eCode);
  }
}
