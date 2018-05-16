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
package eu.toop.connector.me.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.mime.CMimeType;

import eu.toop.connector.me.EBMSUtils;
import eu.toop.connector.me.MEMessage;
import eu.toop.connector.me.MEPayload;
import eu.toop.connector.me.SubmissionMessageProperties;

public final class EBSMUtilsTest {
  private static final Logger LOG = LoggerFactory.getLogger (EBSMUtilsTest.class);

  @Test
  public void testFault () throws SOAPException, IOException {
    final SubmissionMessageProperties sd = new SubmissionMessageProperties();
    sd.conversationId = "EBSMUtilsTestConv";
    final MEMessage msg = new MEMessage (new MEPayload(CMimeType.APPLICATION_XML, "blafoo",
                                                        "<?xml version='1.0'?><root demo='true' />".getBytes (StandardCharsets.ISO_8859_1)));
    final SOAPMessage sm = EBMSUtils.convert2MEOutboundAS4Message (sd, msg);
    assertNotNull (sm);
    try (NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ()) {
      sm.writeTo (aBAOS);
      LOG.info (aBAOS.getAsString (StandardCharsets.UTF_8));
    }

    final byte[] aFault = EBMSUtils.createFault (sm, "Unit test fault");
    LOG.info (new String (aFault, StandardCharsets.UTF_8));
  }
}
