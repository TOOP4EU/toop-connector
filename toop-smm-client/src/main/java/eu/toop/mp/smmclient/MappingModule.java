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

import java.util.List;

public interface MappingModule {

	/**
	 * Complements the country specific entries with their general TOOP
	 * counterparts. It will make the changes directly in the POJO, so not result
	 * has to be returned.
	 * 
	 * @param request
	 *            The JAXB POJO in which to complement the specific entries.
	 */
	public void addTOOPConcepts(List<DataElementRequestType> dataElements);

	/**
	 * Complements the TOOP generic concepts in the message with Country specific
	 * concepts. It will make the changes directly in the POJO, so not result has to
	 * be returned.
	 * 
	 * @param messageXml
	 *            The JAXB POJO in which to complement the generic TOOP entries.
	 */
	public void addCountryConcepts(List<DataElementRequestType> dataElements);

}
