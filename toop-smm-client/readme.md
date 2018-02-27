Semantic Mapping Module
=======================
The Semantic Mapping Module (SMM) works with two sets of concepts:
* TOOP generic concepts
* Country specific concepts

The TOOP Message Processor handles both Data Requests and Data Responses and for both types of messages the SMM will complement them. There are two scenario's:
* The first scenario is that the TOOP message contains Country Specific concepts that need to be complemented with TOOP generic concepts.
* The second scenario is that the TOOP message contains TOOP generic concepts that need to be complemented with Country specific concepts.

For each of the two scenario's the Module interface contains a method.   