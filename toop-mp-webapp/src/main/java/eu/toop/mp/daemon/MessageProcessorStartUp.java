package eu.toop.mp.daemon;

import eu.toop.mp.IMessageSubmissionHandler;
import eu.toop.mp.MockMessageSubmissionHandler;
import eu.toop.mp.dcadapter.servlet.MessageQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class MessageProcessorStartUp implements ServletContextListener{

    IMessageSubmissionHandler msh = null;
    Logger log = LoggerFactory.getLogger(MessageProcessorStartUp.class);

    public void contextDestroyed(ServletContextEvent event) {
        // Do cleanup operations here
        log.info("Shutting down. Cleaning up");
        msh = null;
    }

    public void contextInitialized(ServletContextEvent event) {
        // Invoke the daemon/background process code
        msh = new MockMessageSubmissionHandler(MessageQueue.INSTANCE);
        log.info("Starting Message Processing Thread");
        msh.startProcessing();
    }

}
