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
package eu.toop.mp.smmclient;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Scanner;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SMMTest {

	private static final String MODIFIED = "Modified!";

	private static final Logger LOG = LoggerFactory.getLogger(SMMTest.class);

	private static final String EXAMPLE = "/datarequest.xml";
	private MappingModule m = null;
	private String exampleXml = null;

	public SMMTest() throws IOException {
		m = new SemanticMappingModule();
		InputStream is = SMMTest.class.getResourceAsStream(EXAMPLE);
		exampleXml = convertStreamToString(is);
		is.close();
	}

	@Disabled
	@Test
	void testAddTOOPConcepts() {

		JAXBContext context;
		try {
			LOG.info("testAddTOOPConcepts has started");
			context = JAXBContext.newInstance(ObjectFactory.class);
			final Unmarshaller u = context.createUnmarshaller();
			final StringReader sr = new StringReader(exampleXml);
			@SuppressWarnings("unchecked") // the unchecked cast is unavoidable when working with JAXB.
			final JAXBElement<DataRequestType> elem = (JAXBElement<DataRequestType>) u.unmarshal(sr);
			final DataRequestType requestTypeBefore = elem.getValue();

			List<DataElementRequestType> elements = requestTypeBefore.getDataConsumerRequest().getDataElementRequest();

			m.addCountryConcepts(elements);

			final Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			String requestTypeAfter = null;
			try (final StringWriter sw = new StringWriter()) {
				m.marshal(elem, sw);
				requestTypeAfter = sw.toString();
			}
			LOG.debug(requestTypeAfter);
			assertTrue(!requestTypeAfter.isEmpty());
			assertTrue(requestTypeAfter.contains(MODIFIED));
		} catch (JAXBException | IOException e) {
			LOG.error("An error occurred.", e);
		}
	}

	@Disabled
	@Test
	void testAddCountryConcepts() {
		JAXBContext context;
		try {
			LOG.info("testAddCountryConcepts has started");
			context = JAXBContext.newInstance(ObjectFactory.class);
			final Unmarshaller u = context.createUnmarshaller();
			final StringReader sr = new StringReader(exampleXml);
			@SuppressWarnings("unchecked") // the unchecked cast is unavoidable when working with JAXB.
			final JAXBElement<DataRequestType> elem = (JAXBElement<DataRequestType>) u.unmarshal(sr);
			final DataRequestType requestTypeBefore = elem.getValue();

			List<DataElementRequestType> elements = requestTypeBefore.getDataConsumerRequest().getDataElementRequest();

			m.addTOOPConcepts(elements);

			final Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			String requestTypeAfter = null;
			try (final StringWriter sw = new StringWriter()) {
				m.marshal(elem, sw);
				requestTypeAfter = sw.toString();
			}
			LOG.debug(requestTypeAfter);
			assertTrue(!requestTypeAfter.isEmpty());
			assertTrue(requestTypeAfter.contains(MODIFIED));
		} catch (JAXBException | IOException e) {
			LOG.error("An error occurred.", e);
		}
	}

	/**
	 * This method does *not* close the input stream. You should do it yourself.
	 * 
	 * @param is
	 *            An open InputStream that will not be closed by this method!
	 * @return The string contained in this InputStream.
	 */
	@SuppressWarnings("resource")
	public String convertStreamToString(InputStream is) {
		Scanner s = new Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

}
