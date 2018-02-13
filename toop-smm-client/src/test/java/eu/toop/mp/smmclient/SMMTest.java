package eu.toop.mp.smmclient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SMMTest {

	private static final Logger LOG = LoggerFactory.getLogger(SMMTest.class);

	private static final String EXAMPLE = "/datarequest.xml";
	private Module m = null;
	private String exampleXml = null;

	public SMMTest() throws IOException {
		m = new SemanticMappingModule();
		InputStream is = SMMTest.class.getResourceAsStream(EXAMPLE);
		exampleXml = convertStreamToString(is);
		is.close();
	}

	@Test
	void testAddTOOPConcepts() {
		String result = m.addCountryConcepts(this.exampleXml);
		LOG.debug(result);
		assertNotNull(result);
		assertTrue(!result.isEmpty());
	}

	@Test
	void testAddCountryConcepts() {
		String result = m.addTOOPConcepts(this.exampleXml);

		LOG.debug(result);

		assertNotNull(result);
		assertTrue(!result.isEmpty());
	}

	/**
	 * This method does *not* close the input stream. You should do it yourself.
	 * 
	 * @param is
	 *            An open InputStream that will not be closed by this method!
	 * @return The string contained in this InputStream.
	 */
	public String convertStreamToString(InputStream is) {
		Scanner s = new Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

}
