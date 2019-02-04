package eu.toop.connector.smmclient;

import eu.toop.commons.codelist.EPredefinedDocumentTypeIdentifier;
import eu.toop.commons.codelist.SMMDocumentTypeMapping;

final class CMockSMM {
  public static final String LOG_PREFIX = "[unit test] ";
  public static final String NS_TOOP = SMMDocumentTypeMapping.getToopSMNamespace (EPredefinedDocumentTypeIdentifier.REQUEST_REGISTEREDORGANIZATION);
  public static final String NS_ELONIA = "http://toop.elo/elonia-business-register";
  public static final String NS_FREEDONIA = "http://toop.fre/freedonia-business-register";

  private CMockSMM () {
  }
}
