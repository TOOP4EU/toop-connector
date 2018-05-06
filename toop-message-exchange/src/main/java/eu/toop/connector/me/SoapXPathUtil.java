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
    XPATH = XPathFactory.newInstance().newXPath();
    final MapBasedNamespaceContext aNamespaceCtx = new MapBasedNamespaceContext();
    aNamespaceCtx.addDefaultNamespaceURI(EBMSUtils.NS_EBMS);
    XPATH.setNamespaceContext(aNamespaceCtx);
  }

  /**
   * Tries to find a single not wrt the provided XPATH and returns null if not found
   * @param node
   * @param xpath
   * @return
   */
  @Nullable
  public static Node findSingleNode(@Nonnull final Node node,
      @Nonnull final String xpath) {
    try {
      final Node o = (Node) XPATH.evaluate(xpath, node, XPathConstants.NODE);
      return o;
    } catch (final XPathExpressionException e) {
      throw new MEException(e);
    }
  }

  /**
   * Tries to find a single not wrt the provided XPATH. Throws an exception if no value is found
   * @param node
   * @param xpath
   * @return
   * @throws MEException
   */
  @Nonnull
  public static Node safeFindSingleNode(@Nonnull final Node node,
      @Nonnull final String xpath) throws MEException {
    try {
      final Node o = (Node) XPATH.evaluate(xpath, node, XPathConstants.NODE);
      if (o == null) {
        throw new MEException("No match for [" + xpath + "]");
      }

      return o;
    } catch (final XPathExpressionException e) {
      throw new MEException(e);
    }
  }

  @Nonnull
  public static List<Node> listNodes(@Nonnull final Node node,
      @Nonnull final String xpath) throws MEException {
    try {
      final NodeList o = (NodeList) XPATH.evaluate(xpath, node, XPathConstants.NODESET);
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
