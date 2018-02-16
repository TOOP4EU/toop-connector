package eu.toop.mp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mock implementation of {@link IMessageSubmissionHandler}. This class receives the payload
 * from the toop dc/dp adapter and starts processing on a new thread.
 */

public class MockMessageSubmissionHandler implements IMessageSubmissionHandler {

    private final MessageProcessingThread messageProcessingThread;

    public MockMessageSubmissionHandler(TransferQueue<File> queue) {

        messageProcessingThread = new MessageProcessingThread(queue);
    }

    @Override
    public void startProcessing() {
        Thread t = new Thread(messageProcessingThread);
        t.setDaemon(true);
        t.start();
    }
}

/**
 * Runnable class to be called for starting a new thread.
 */
class MessageProcessingThread implements Runnable {

    private final TransferQueue<File> queue;
    private static final Logger log = LoggerFactory.getLogger(MessageProcessingThread.class);

    MessageProcessingThread(TransferQueue<File> q) {
        this.queue = q;
    }

    @Override
    public void run() {

        boolean flag = true;
        while (flag) {

            try {

                if (queue.isEmpty()) {
                    log.info("Queue is empty... Waiting for files");
                }

                File nextFile = queue.take();
                log.info("Fetched file {} from Transfer Queue", nextFile.getAbsolutePath());

            } catch (InterruptedException e) {
                e.printStackTrace();
                flag = false;
            }
        }
    }
}
