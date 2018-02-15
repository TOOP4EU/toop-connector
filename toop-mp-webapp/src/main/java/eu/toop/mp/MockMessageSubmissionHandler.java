package eu.toop.mp;

import java.io.File;

import static java.lang.Thread.*;

/**
 * Mock implementation of {@link IMessageSubmissionHandler}. This class receives the payload
 * from the toop dc/dp adapter and starts processing on a new thread.
 */

public class MockMessageSubmissionHandler implements IMessageSubmissionHandler {

    private final File submittedFile;

    public MockMessageSubmissionHandler(File aFile){

        this.submittedFile = aFile;
    }

    @Override
    public void startProcessing() {
        new Thread(new MessageProcessingThread(submittedFile)).run();
    }
}

/**
 * Runnable class to be called for starting a new thread.
 */
class MessageProcessingThread implements Runnable {

    private final File submitFile;

    MessageProcessingThread(File f) {
        this.submitFile = f;
    }
    @Override
    public void run() {

        try {
            sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
