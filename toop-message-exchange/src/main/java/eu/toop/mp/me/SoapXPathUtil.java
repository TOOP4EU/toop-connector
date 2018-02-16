package eu.toop.mp.me;

import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author: myildiz
 * @date: 15.02.2018.
 */
public class SoapXPathUtil {
  private static XPath xPath = createAS4AwareXpath();

  public static XPath createAS4AwareXpath() {
    XPath xPath = XPathFactory.newInstance().newXPath();
    xPath.setNamespaceContext(new SoapNamespaceContext());
    return xPath;
  }

  public static <T> T findSingleNode(org.w3c.dom.Node node, String xpath) {
    try {
      Object o = xPath.evaluate(xpath, node, XPathConstants.NODE);
      if (o == null)
        throw new RuntimeException("No match for [" + xpath + "]");

      return (T) o;
    } catch (XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> List<T> listNodes(org.w3c.dom.Node node, String xpath) {
    try {
      Object o = xPath.evaluate(xpath, node, XPathConstants.NODESET);
      if (o == null)
        throw new RuntimeException("No match for [" + xpath + "]");

      NodeList list = (NodeList) o;

      List<T> els = new ArrayList<>();
      for (int i = 0; i < list.getLength(); ++i) {
        els.add((T) ((NodeList) o).item(i));
      }
      return els;
    } catch (XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }


  static class SoapNamespaceContext implements NamespaceContext {
    private HashMap<String, String> maps = new HashMap<>();

    protected SoapNamespaceContext() {
    }

    @Override
    public String getNamespaceURI(String prefix) {
      return "*";
    }

    @Override
    public String getPrefix(String namespace) {
      return maps.get("namespace");
    }

    @Override
    public Iterator getPrefixes(String namespace) {
      return null;
    }
  }

}
