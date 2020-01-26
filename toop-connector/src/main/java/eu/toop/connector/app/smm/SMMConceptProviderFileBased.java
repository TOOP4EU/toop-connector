package eu.toop.connector.app.smm;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.serialize.MicroReader;

import eu.toop.connector.api.smm.ISMMConceptProvider;
import eu.toop.connector.api.smm.MappedValueList;

public class SMMConceptProviderFileBased implements ISMMConceptProvider
{
  public static final IReadableResource DEFAULT_RES = new ClassPathResource ("semantic-mapping-default.xml");

  private static final Logger LOGGER = LoggerFactory.getLogger (SMMConceptProviderFileBased.class);

  // Map <toopNS, Map <externalNS, Map <toop, external>>>
  private final ICommonsMap <String, ICommonsMap <String, ICommonsMap <String, String>>> m_aMappings = new CommonsHashMap <> ();

  public SMMConceptProviderFileBased ()
  {
    this (DEFAULT_RES);
  }

  public SMMConceptProviderFileBased (@Nonnull final IReadableResource aRes)
  {
    ValueEnforcer.notNull (aRes, "Res");
    final IMicroDocument aDoc = MicroReader.readMicroXML (aRes);
    if (aDoc == null || aDoc.getDocumentElement () == null)
      throw new IllegalStateException ("Failed to read " + aRes + " as XML");
    int nItems = 0;
    for (final IMicroElement eToop : aDoc.getDocumentElement ().getAllChildElements ("toop"))
    {
      String sNS = eToop.getAttributeValue ("ns");
      final ICommonsMap <String, ICommonsMap <String, String>> aToopMap = m_aMappings.computeIfAbsent (sNS,
                                                                                                       k -> new CommonsHashMap <> ());
      for (final IMicroElement eExternal : eToop.getAllChildElements ("external"))
      {
        sNS = eExternal.getAttributeValue ("ns");
        final ICommonsMap <String, String> aItems = aToopMap.computeIfAbsent (sNS, k -> new CommonsHashMap <> ());
        for (final IMicroElement eItem : eExternal.getAllChildElements ("item"))
        {
          final String sToop = eItem.getAttributeValue ("toop");
          final String sExternal = eItem.getAttributeValue ("external");
          aItems.put (sToop, sExternal);
          nItems++;
        }
      }
    }

    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Read " + nItems + " semantic mapping items from " + aRes.getPath ());
  }

  @Nonnull
  public MappedValueList getAllMappedValues (@Nonnull final String sLogPrefix,
                                             @Nonnull final String sSourceNamespace,
                                             @Nonnull final String sDestNamespace) throws IOException
  {
    final MappedValueList ret = new MappedValueList ();

    final boolean bToopToExternal;
    ICommonsMap <String, ICommonsMap <String, String>> aToopMapping = m_aMappings.get (sSourceNamespace);
    if (aToopMapping != null)
      bToopToExternal = true;
    else
    {
      // Is it external to TOOP mapping?
      aToopMapping = m_aMappings.get (sDestNamespace);
      bToopToExternal = false;
    }

    if (aToopMapping != null)
    {
      // We have a known TOOP NS
      final ICommonsMap <String, String> aExternalMapping = aToopMapping.get (bToopToExternal ? sDestNamespace
                                                                                              : sSourceNamespace);
      if (aExternalMapping != null)
      {
        // We have all we need, to fill the result list
        for (final Map.Entry <String, String> aEntry : aExternalMapping.entrySet ())
          if (bToopToExternal)
            ret.addMappedValue (sSourceNamespace, aEntry.getKey (), sDestNamespace, aEntry.getValue ());
          else
            ret.addMappedValue (sDestNamespace, aEntry.getValue (), sSourceNamespace, aEntry.getKey ());
      }
    }

    return ret;
  }

}
