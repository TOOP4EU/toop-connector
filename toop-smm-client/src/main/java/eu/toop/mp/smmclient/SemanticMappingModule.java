package eu.toop.mp.smmclient;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.toop.mp.DataRequestInfoType;
import eu.toop.mp.DataRequestType;
import eu.toop.mp.ObjectFactory;

public class SemanticMappingModule implements Module {

	/**
	 * The Log facility of this class.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(SemanticMappingModule.class);

	public String addTOOPConcepts(String messageXml) {
		return convertConcepts(messageXml, true);
	}

	public String addCountryConcepts(String messageXml) {
		return convertConcepts(messageXml, false);
	}

	/**
	 * Lots of the logic is the same when converting between TOOP <-> Country. So a
	 * single method is convienient. The boolean indicates which route we need to
	 * take.
	 * 
	 * @param messageXml
	 *            The message to convert.
	 * @param fromTOOPtoCountry
	 *            {@code true} when to convert from TOOP concepts to Country
	 *            concepts, {@code false} when converting from Country concepts to
	 *            TOOP concepts.
	 * @return The converted (complemented) message.
	 */
	public String convertConcepts(String messageXml, boolean fromTOOPtoCountry) {

		// for now we do nothing with fromTOOPtoCountry.

		LOG.trace("Message: {}", messageXml);

		String result = null;
		try {
			JAXBContext context = JAXBContext.newInstance(ObjectFactory.class);
			Unmarshaller u = context.createUnmarshaller();
			StringReader sr = new StringReader(messageXml);

			@SuppressWarnings("unchecked") // the unchecked cast is unavoidable when working with JAXB.
			JAXBElement<eu.toop.mp.DataRequestType> elem = (JAXBElement<DataRequestType>) u.unmarshal(sr);
			DataRequestType type = (DataRequestType) elem.getValue();

			/**
			 * Somehow I do not manage to avoid the JAXBElement here. According to this it
			 * should be possible: https://stackoverflow.com/a/26549272
			 */
			// DataRequestType type = (DataRequestType) u.unmarshal(sr);

			/*
			 * For now we just change some random field, but when finished it will call the
			 * Semantic Mapping Service here and request the translation of a particular
			 * concept.
			 */
			List<DataRequestInfoType> drit = type.getDataConsumerRequest().getDataRequestInfo();
			if (!drit.isEmpty()) {
				drit.forEach((i) -> i.setToopConcept("Modified!"));
			}

			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			StringWriter sw = new StringWriter();
			m.marshal(elem, sw);
			result = sw.toString();
		} catch (JAXBException e) {
			LOG.error("An error occured while JAXB unmarshalling", e);
		}

		return result;
	}

}
