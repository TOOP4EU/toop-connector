package eu.toop.mp.me;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.helger.xml.namespace.MapBasedNamespaceContext;

/**
 * @author: myildiz
 * @date: 15.02.2018.
 */
public class SoapXPathUtil {
  private static final XPath XPATH;

  static {
    XPATH = XPathFactory.newInstance ().newXPath ();
    final MapBasedNamespaceContext aNamespaceCtx = new MapBasedNamespaceContext ();
    // TODO add if needed
    XPATH.setNamespaceContext (aNamespaceCtx);
  }

  @Nonnull
  public static Node findSingleNode (@Nonnull final Node node,
                                     @Nonnull final String xpath) throws IllegalStateException {
    try {
      final Node o = (Node) XPATH.evaluate (xpath, node, XPathConstants.NODE);
      if (o == null)
        throw new IllegalStateException ("No match for [" + xpath + "]");

      return o;
    } catch (final XPathExpressionException e) {
      throw new IllegalStateException (e);
    }
  }

  @Nonnull
  public static List<Node> listNodes (@Nonnull final Node node,
                                      @Nonnull final String xpath) throws IllegalStateException {
    try {
      final NodeList o = (NodeList) XPATH.evaluate (xpath, node, XPathConstants.NODESET);
      if (o == null)
        throw new IllegalStateException ("No match for [" + xpath + "]");

      final List<Node> els = new ArrayList<> ();
      for (int i = 0; i < o.getLength (); ++i) {
        els.add (o.item (i));
      }
      return els;
    } catch (final XPathExpressionException e) {
      throw new IllegalStateException (e);
    }
  }
}
