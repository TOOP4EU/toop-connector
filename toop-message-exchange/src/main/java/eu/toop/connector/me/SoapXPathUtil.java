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
package eu.toop.connector.me;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.xml.namespace.MapBasedNamespaceContext;
import com.helger.xml.xpath.XPathHelper;

/**
 * @author myildiz at 15.02.2018.
 */
@Immutable
public final class SoapXPathUtil {
  private SoapXPathUtil () {
  }

  @Nonnull
  @ReturnsMutableCopy
  private static XPath _createXPath () {
    // XPath instances are not thread safe!
    final XPath ret = XPathHelper.createNewXPath ();
    final MapBasedNamespaceContext aNamespaceCtx = new MapBasedNamespaceContext();
    aNamespaceCtx.addDefaultNamespaceURI(EBMSUtils.NS_EBMS);
    ret.setNamespaceContext(aNamespaceCtx);
    return ret;
  }

  /**
   * Tries to find a single not wrt the provided XPATH and returns null if not found
   * @param node source node
   * @param xpath XPath to evaluate
   * @return The resolved {@link Node}. May be <code>null</code>.
   */
  @Nullable
  public static Node findSingleNode(@Nonnull final Node node,
      @Nonnull final String xpath) {
    try {
      return (Node) _createXPath ().evaluate(xpath, node, XPathConstants.NODE);
    } catch (final XPathExpressionException e) {
      throw new MEException(e);
    }
  }

  /**
   * Tries to find a single not wrt the provided XPATH. Throws an exception if no value is found
   * @param node Source node
   * @param xpath XPath to evaluate
   * @return A non-<code>null</code> {@link Node}.
   * @throws MEException If no result was found (for whatever reason)
   */
  @Nonnull
  public static Node safeFindSingleNode(@Nonnull final Node node,
      @Nonnull final String xpath) {
    final Node o = findSingleNode (node, xpath);
    if (o == null) {
      throw new MEException("No match for [" + xpath + "]");
    }
    return o;
  }

  @Nullable
  public static String getSingleNodeTextContent(@Nullable final Node node,
      @Nonnull final String xpath) {
    if (node != null)
      return safeFindSingleNode (node, xpath).getTextContent ();
    return null;
  }

  @Nonnull
  public static List<Node> listNodes(@Nonnull final Node node,
      @Nonnull final String xpath) {
    try {
      final NodeList o = (NodeList) _createXPath ().evaluate(xpath, node, XPathConstants.NODESET);
      if (o == null) {
        throw new MEException("No match for [" + xpath + "]");
      }

      final List<Node> els = new ArrayList<>();
      for (int i = 0; i < o.getLength(); ++i) {
        els.add(o.item(i));
      }
      return els;
    } catch (final XPathExpressionException e) {
      throw new MEException(e);
    }
  }
}
