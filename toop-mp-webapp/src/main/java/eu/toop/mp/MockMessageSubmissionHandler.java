package eu.toop.mp;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TransferQueue;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock implementation of {@link IMessageSubmissionHandler}. This class receives
 * the payload from the toop dc/dp adapter and starts processing on a new
 * thread.
 */
public class MockMessageSubmissionHandler implements IMessageSubmissionHandler {

	private final MessageProcessingThread messageProcessor;
	private Thread m_aThread;

	public MockMessageSubmissionHandler(final TransferQueue<File> queue) {
		messageProcessor = new MessageProcessingThread(queue);
	}

	@Override
	public void startProcessing() {
		if (m_aThread != null)
			throw new IllegalStateException("Thread already running!");
		m_aThread = new Thread(messageProcessor, "MessageSubmissionHandler");
		m_aThread.setDaemon(true);
		m_aThread.start();
	}

	public void close() throws IOException {
		if (m_aThread != null)
			m_aThread.interrupt();
		m_aThread = null;
	}
}

/**
 * Runnable class to be called for starting a new thread.
 */
class MessageProcessingThread implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(MessageProcessingThread.class);

	private final TransferQueue<File> queue;

	MessageProcessingThread(@Nonnull final TransferQueue<File> q) {
		this.queue = q;
	}

	@Override
	public void run() {
		while (true) {
			try {
				if (queue.isEmpty()) {
					log.info("Queue is empty... Waiting for files");
				}

				final File nextFile = queue.take();
				log.info("Fetched file {} from Transfer Queue", nextFile.getAbsolutePath());
			} catch (final InterruptedException e) {
				break;
			}
		}
	}
}
