/**
 * Copyright (C) 2018-2019 toop.eu
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
package eu.toop.connector.api.as4;

/**
 * A separate runtime exception to make it easier for the users to distinguish between the 'source path' to the
 * underlying problem.
 *
 * @author yerlibilgin
 */
public class MEException extends IllegalStateException {

  public MEException(String message) {
    super(message);
  }


  public MEException(String message, Throwable cause) {
    super(message, cause);
  }


  public MEException(Throwable cause) {
    super(cause);
  }

}