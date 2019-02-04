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

import eu.toop.commons.error.EToopErrorCode;

/**
 * A separate runtime exception to make it easier for the users to distinguish
 * between the 'source path' to the underlying problem.
 *
 * @author yerlibilgin
 */
public class MEException extends IllegalStateException
{
  private EToopErrorCode toopErrorCode;

  public MEException (final String sMsg)
  {
    super (sMsg);
  }

  public MEException (final Throwable aCause)
  {
    super (aCause);
  }

  public MEException (final String sMsg, final Throwable aCause)
  {
    super (sMsg, aCause);
  }

  public MEException(final EToopErrorCode toopErrorCode) {
    super(toopErrorCode.toString());
    this.toopErrorCode = toopErrorCode;
  }
  public MEException(final EToopErrorCode toopErrorCode, final Throwable aCause) {
    super(toopErrorCode.toString(), aCause);
    this.toopErrorCode = toopErrorCode;
  }
  public MEException(final EToopErrorCode toopErrorCode, String sMsg) {
    super(toopErrorCode.toString() + " - " + sMsg);
    this.toopErrorCode = toopErrorCode;
  }

  public EToopErrorCode getToopErrorCode() {
    return toopErrorCode;
  }
}
