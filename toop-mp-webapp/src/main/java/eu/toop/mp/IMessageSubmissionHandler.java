package eu.toop.mp;

public interface IMessageSubmissionHandler extends AutoCloseable {
	/**
	 * Explicit start
	 */
	void startProcessing();
}
