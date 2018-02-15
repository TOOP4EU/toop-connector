package eu.toop.mp.dcadapter.servlet;

import java.io.File;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

/**
 * Using an unbounded thread-safe queue for communication with MessageProcessor
 */

public class MessageQueue {
    public static final TransferQueue<File> INSTANCE = new LinkedTransferQueue<>();
}
