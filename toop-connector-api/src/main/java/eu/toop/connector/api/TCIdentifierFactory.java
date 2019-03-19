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
package eu.toop.connector.api;

import javax.annotation.Nullable;

import com.helger.commons.string.StringHelper;
import com.helger.peppol.identifier.factory.SimpleIdentifierFactory;
import com.helger.peppol.identifier.generic.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.SimpleParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.SimpleProcessIdentifier;

/**
 * A special {@link TCIdentifierFactory} that trims values.
 *
 * @author Philip Helger
 * @since 0.10.0
 */
public class TCIdentifierFactory extends SimpleIdentifierFactory
{
  public static final TCIdentifierFactory INSTANCE_TC = new TCIdentifierFactory ();

  @Override
  @Nullable
  public SimpleDocumentTypeIdentifier createDocumentTypeIdentifier (@Nullable final String sScheme,
                                                                    @Nullable final String sValue)
  {
    return super.createDocumentTypeIdentifier (StringHelper.trim (sScheme), StringHelper.trim (sValue));
  }

  @Override
  @Nullable
  public SimpleParticipantIdentifier createParticipantIdentifier (@Nullable final String sScheme,
                                                                  @Nullable final String sValue)
  {
    return super.createParticipantIdentifier (StringHelper.trim (sScheme), StringHelper.trim (sValue));
  }

  @Override
  @Nullable
  public SimpleProcessIdentifier createProcessIdentifier (@Nullable final String sScheme, @Nullable final String sValue)
  {
    return super.createProcessIdentifier (StringHelper.trim (sScheme), StringHelper.trim (sValue));
  }
}
