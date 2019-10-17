package eu.toop.connector.app.searchdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;

import org.junit.Test;

import com.helger.commons.state.ESuccess;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.wrapper.Wrapper;
import com.helger.xml.microdom.IMicroDocument;

/**
 * Test class for class {@link SearchDPByDPTypeHandler}.
 *
 * @author Philip Helger
 */
public final class SearchDPByDPTypeHandlerTest
{
  @Test
  public void testExtract ()
  {
    SearchDPByDPTypeInputParams aIP = SearchDPByDPTypeHandler.extractInputParams ("/test");
    assertNotNull (aIP);
    assertEquals ("test", aIP.getDPType ());

    aIP = SearchDPByDPTypeHandler.extractInputParams ("/test/");
    assertNotNull (aIP);
    assertEquals ("test", aIP.getDPType ());

    aIP = SearchDPByDPTypeHandler.extractInputParams ("/test/bla");
    assertNotNull (aIP);
    assertEquals ("test", aIP.getDPType ());

    aIP = SearchDPByDPTypeHandler.extractInputParams ("/test/bla/foo/bar");
    assertNotNull (aIP);
    assertEquals ("test", aIP.getDPType ());

    aIP = SearchDPByDPTypeHandler.extractInputParams ("blub");
    assertNotNull (aIP);
    assertEquals ("blub", aIP.getDPType ());

    aIP = SearchDPByDPTypeHandler.extractInputParams ("blub/dadoop");
    assertNotNull (aIP);
    assertEquals ("blub", aIP.getDPType ());

    aIP = SearchDPByDPTypeHandler.extractInputParams ("");
    assertNotNull (aIP);
    assertNull (aIP.getDPType ());

    aIP = SearchDPByDPTypeHandler.extractInputParams ("/");
    assertNotNull (aIP);
    assertNull (aIP.getDPType ());

    aIP = SearchDPByDPTypeHandler.extractInputParams ("//////////?x=y");
    assertNotNull (aIP);
    assertNull (aIP.getDPType ());
  }

  @Test
  public void testPerformSearch () throws IOException
  {
    final SearchDPByDPTypeInputParams aIP = SearchDPByDPTypeHandler.extractInputParams ("/abc");
    assertNotNull (aIP);
    assertEquals ("abc", aIP.getDPType ());

    final Wrapper <ESuccess> aWrapper = new Wrapper <> ();
    final ISearchDPCallback aCallback = new ISearchDPCallback ()
    {
      public void onQueryDirectorySuccess (final IMicroDocument aDoc)
      {
        aWrapper.set (ESuccess.SUCCESS);
      }

      public void onQueryDirectoryError (final ISimpleURL aQueryURL)
      {
        aWrapper.set (ESuccess.FAILURE);
      }
    };
    SearchDPByDPTypeHandler.performSearch (aIP, aCallback);
    assertSame (ESuccess.SUCCESS, aWrapper.get ());
  }
}
