/**
 * Copyright (C) 2018-2020 toop.eu
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
package eu.toop.connector.mem.def.notifications;

/**
 * A java representation of a notification C2 --- C3 message relay. See TOOP AS4 GW backend interface specification
 *
 * @author yerlibilgin
 */
public class RelayResult extends Notification {

  private String shortDescription;
  private String severity;


  public String getShortDescription() {
    return shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public String getSeverity() {
    return severity;
  }
}
