/**
 * Copyright (C) 2018 toop.eu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.toop.connector.me;

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
