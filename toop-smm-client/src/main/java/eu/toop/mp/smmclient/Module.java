package eu.toop.mp.smmclient;

public interface Module {

	/**
	 * Complements the country specific entries with their general TOOP
	 * counterparts.
	 * 
	 * @param messageXml
	 *            The XML in which to complement the specific entries.
	 * @return The given messageXml complemented with TOOP generic concepts.
	 */
	public String addTOOPConcepts(String messageXml);

	/**
	 * Complements the TOOP generic concepts in the message with Country specific
	 * concepts.
	 * 
	 * @param messageXml
	 *            The XML in which to complement the generic TOOP entries.
	 * @return The given messageXml complemented with the corresponding Country
	 *         specific entries.
	 */
	public String addCountryConcepts(String messageXml);

}
