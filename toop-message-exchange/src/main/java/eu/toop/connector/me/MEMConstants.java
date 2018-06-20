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

/**
 * @author myildiz
 */
public final class MEMConstants {

  public static final String MEM_AS4_SUFFIX = "message-exchange.toop.eu";
  public static final String MEM_PARTY_ROLE = "http://www.toop.eu/edelivery/backend";
  public static final String GW_PARTY_ROLE = "http://www.toop.eu/edelivery/gateway";

  public static final String ACTION_SUBMIT = "Submit";
  public static final String ACTION_DELIVER = "Deliver";
  //this is recommended to be a Relay instead of Notify
  //but its kept like this for a while
  public static final String ACTION_RELAY = "Notify";
  public static final String ACTION_SUBMISSION_RESULT = "SubmissionResult";
  public static final String SERVICE = "http://www.toop.eu/edelivery/bit";

  private MEMConstants () {}
}
