package eu.toop.mp.me;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;

import javax.xml.soap.SOAPMessage;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.mime.CMimeType;

public final class EBSMUtilsTest {
  private static final Logger LOG = LoggerFactory.getLogger (EBSMUtilsTest.class);

  @Test
  public void testFault () {
    final SubmissionData sd = new SubmissionData ();
    final MEMessage msg = new MEMessage (new MEPayload (CMimeType.APPLICATION_XML, "blafoo",
                                                        "<?xml version='1.0'?><root demo='true' />".getBytes (StandardCharsets.ISO_8859_1)));
    final SOAPMessage sm = EBMSUtils.convert2MEOutboundAS4Message (sd, msg);
    assertNotNull (sm);

    final byte[] aFault = EBMSUtils.createFault (sm, "Unit test fault");
    LOG.info (new String (aFault, StandardCharsets.UTF_8));
  }
}
