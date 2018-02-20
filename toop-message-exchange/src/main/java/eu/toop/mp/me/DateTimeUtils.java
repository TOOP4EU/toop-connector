package eu.toop.mp.me;

import java.time.format.DateTimeFormatter;

import com.helger.commons.datetime.PDTFactory;

/**
 * Utilities related to date-time formatting, parsing etc..
 *
 * @author: myildiz
 * @date: 16.02.2018.
 */
public class DateTimeUtils {
  /**
   * Create the current date time string in format
   * <cod>uuuu-MM-dd'T'HH:mm:ss.SSSX</code> using UTC format
   *
   * @return Formatted date time
   */
  public static String getCurrentTimestamp () {
    // Same as the default W3C format but with milliseconds included
    return PDTFactory.getCurrentZonedDateTimeUTC ().format (DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }
}
