package eu.toop.mp.daemon;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.toop.mp.IMessageSubmissionHandler;
import eu.toop.mp.MockMessageSubmissionHandler;
import eu.toop.mp.dcadapter.servlet.MessageQueue;

@WebListener
public class MessageProcessorStartUp implements ServletContextListener {
	private static final Logger log = LoggerFactory.getLogger(MessageProcessorStartUp.class);

	private IMessageSubmissionHandler msh = null;

	public void contextInitialized(final ServletContextEvent event) {
		if (msh != null)
			throw new IllegalStateException("msg is already initialized!");

		// Invoke the daemon/background process code
		msh = new MockMessageSubmissionHandler(MessageQueue.INSTANCE);
		log.info("Starting Message Processing Thread");
		msh.startProcessing();
	}

	public void contextDestroyed(final ServletContextEvent event) {
		// Do cleanup operations here
		log.info("Shutting down. Cleaning up");
		msh = null;
	}
}
