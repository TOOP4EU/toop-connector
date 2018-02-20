package eu.toop.mp.me.mocAS4;

import eu.toop.mp.me.SoapUtil;

import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author: myildiz
 * @date: 20.02.2018.
 */
public class SOAPMessageAccumulator {

  private MimeHeaders newHeaders;
  private PipedOutputStream pipedOutputStream;
  private PipedInputStream pipedInputStream;
  private SOAPMessage nextMessage;
  private Executor messageProcessingExecutor = Executors.newSingleThreadExecutor();
  private CountDownLatch messageConstructionLatch;

  public void reset(MimeHeaders newHeaders) throws IOException, SOAPException {
    messageConstructionLatch = new CountDownLatch(1);
    //for each new http chunk, we create a new input stream and pipe it through this mechanism
    //to the underlying soap message factory. So we are saved from caching data.
    pipedOutputStream = new PipedOutputStream();
    pipedInputStream = new PipedInputStream(pipedOutputStream);

    messageProcessingExecutor.execute(() -> {
      try {
        nextMessage = SoapUtil.createMessage(newHeaders, pipedInputStream);
      } catch (IOException e) {
        e.printStackTrace();
      } catch (SOAPException e) {
        e.printStackTrace();
      } finally {
        try {
          //good or bad, we are done reading.
          messageConstructionLatch.countDown();
        } catch (Exception ignored) {

        }
      }
    });

  }


  /**
   * Consume the next stream and feed the content to the soap message creator
   *
   * @param inputStream
   * @throws IOException
   */
  public void accumulate(InputStream inputStream) throws IOException {
    int read;
    while ((read = inputStream.read()) > 0) {
      pipedOutputStream.write(read);
    }
  }


  public SOAPMessage doFinal() throws SOAPException, IOException {
    pipedOutputStream.close();
    try {
      messageConstructionLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    if(nextMessage == null){
      //latch is zero but we don't have any message
      throw new NullPointerException("Couldn't internalize the SOAP Message");
    }
    nextMessage.saveChanges();
    return nextMessage;
  }
}
