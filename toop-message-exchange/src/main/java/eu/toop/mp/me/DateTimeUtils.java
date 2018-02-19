package eu.toop.mp.me;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilities related to date-time formatting, parsing etc..
 *
 * @author: myildiz
 * @date: 16.02.2018.
 */
public class DateTimeUtils {
  /**
   * Create the current date time string in format
   *
   * uuuu-MM-dd'T'HH:mm:ss.SSSX
   *
   *
   * @return
   */
  public static String getCurrentTimestamp() {
    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
    return now.format(DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSX"));
  }
}
