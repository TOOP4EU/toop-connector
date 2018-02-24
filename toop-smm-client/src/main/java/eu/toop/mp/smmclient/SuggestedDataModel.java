package eu.toop.mp.smmclient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.StringHelper;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.response.ResponseHandlerJson;
import com.helger.json.IJson;
import com.helger.json.IJsonObject;

import eu.toop.mp.api.MPConfig;

public class SuggestedDataModel {
  private final Map<String, List<String>> m_aMap = new HashMap<> ();

  public void addConceptToBeMapped (@Nonnull @Nonempty final String sConceptScheme,
                                    @Nonnull @Nonempty final String sConceptValue) {
    ValueEnforcer.notEmpty (sConceptScheme, "Scheme");
    ValueEnforcer.notEmpty (sConceptValue, "Value");
    m_aMap.computeIfAbsent (sConceptScheme, k -> new ArrayList<> ()).add (sConceptValue);
  }

  private static <T> void _httpClientGet (@Nonnull final String sDestinationURL,
                                          @Nonnull final ResponseHandler<T> aResponseHandler,
                                          @Nonnull final Consumer<T> aResultHandler) throws IOException {
    ValueEnforcer.notEmpty (sDestinationURL, "DestinationURL");
    ValueEnforcer.notNull (aResponseHandler, "ResponseHandler");
    ValueEnforcer.notNull (aResultHandler, "ResultHandler");

    final HttpClientFactory aHCFactory = new HttpClientFactory ();
    // For proxy etc
    aHCFactory.setUseSystemProperties (true);

    try (final HttpClientManager aMgr = new HttpClientManager (aHCFactory)) {
      final HttpGet aGet = new HttpGet (sDestinationURL);
      aGet.addHeader ("Accept", "application/json,*;q=0");

      final T aResponse = aMgr.execute (aGet, aResponseHandler);
      aResultHandler.accept (aResponse);
    }
  }

  @MustImplementEqualsAndHashcode
  public static final class RequestValue {
    private final String m_sScheme;
    private final String m_sValue;

    public RequestValue (@Nonnull @Nonempty final String sScheme, @Nonnull @Nonempty final String sValue) {
      m_sScheme = sScheme;
      m_sValue = sValue;
    }

    @Nonnull
    @Nonempty
    public String getScheme () {
      return m_sScheme;
    }

    @Nonnull
    @Nonempty
    public String getValue () {
      return m_sValue;
    }

    @Override
    public boolean equals (final Object o) {
      if (o == this)
        return true;
      if (o == null || !getClass ().equals (o.getClass ()))
        return false;
      final RequestValue rhs = (RequestValue) o;
      return m_sScheme.equals (rhs.m_sScheme) && m_sValue.equals (rhs.m_sValue);
    }

    @Override
    public int hashCode () {
      return new HashCodeGenerator (this).append (m_sScheme).append (m_sValue).getHashCode ();
    }
  }

  /**
   * Perform the HTTP GET queries on the GRLC server and return the results.
   *
   * @return A non-<code>null</code> but maybe empty map.
   * @throws IOException
   *           in case of HTTP IO error
   */
  @Nonnull
  @ReturnsMutableCopy
  public Map<RequestValue, String> performMapping () throws IOException {
    // Find base URL
    String sBaseURL = MPConfig.getSMMGRLCURL ();
    if (StringHelper.hasNoText (sBaseURL))
      throw new IllegalArgumentException ("SMM GRLC URL is missing!");
    if (!sBaseURL.endsWith ("/"))
      sBaseURL += "/";
    final String sDestinationURL = sBaseURL + "api/JackJackie/toop-sparql/get-mapped-toop-concept";

    // Avoid creating them for every loop instance
    final ResponseHandlerJson aJsonHandler = new ResponseHandlerJson ();
    final Map<RequestValue, String> ret = new HashMap<> ();

    for (final Map.Entry<String, List<String>> aEntry : m_aMap.entrySet ()) {
      final String sSourceScheme = aEntry.getKey ();
      for (final String sSourceValue : aEntry.getValue ()) {
        if ("toop".equals (sSourceScheme)) {
          // XXX Already in "TOOP" scheme? If so, don't map
          ret.put (new RequestValue (sSourceScheme, sSourceValue), sSourceValue);
        } else {
          // Execute HTTP request
          _httpClientGet (sDestinationURL + "?concept=" + sSourceValue, aJsonHandler, aJson -> {
            if (aJson.isObject ()) {
              final IJsonObject aResults = aJson.getAsObject ().getAsObject ("results");
              if (aResults != null)
                for (final IJson aBinding : aResults.getAsArray ("bindings")) {
                  final String sToopConcept = aBinding.getAsObject ().getAsObject ("s").getAsString ("value");
                  ret.put (new RequestValue (sSourceScheme, sSourceValue), sToopConcept);
                }
            }
          });
        }
      }
    }

    return ret;
  }
}
