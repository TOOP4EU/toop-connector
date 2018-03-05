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
package eu.toop.mp.me.mocAS4;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import eu.toop.mp.me.SoapUtil;

/**
 * @author: myildiz
 * @date: 20.02.2018.
 */
public class SOAPMessageAccumulator {
  private PipedOutputStream pipedOutputStream;
  private PipedInputStream pipedInputStream;
  private SOAPMessage nextMessage;
  private final Executor messageProcessingExecutor = Executors.newSingleThreadExecutor();
  private CountDownLatch messageConstructionLatch;

  /**
   * Resets the accumulator and prepares it for a new soap message
   *
   * @param newHeaders the MimeHeaders parsed from the HTTP headers
   */
  public void reset(final MimeHeaders newHeaders) {
    messageConstructionLatch = new CountDownLatch(1);
    //for each new http chunk, we create a new input stream and pipe it through this mechanism
    //to the underlying soap message factory. So we are saved from caching data.
    pipedOutputStream = new PipedOutputStream();
    try {
      pipedInputStream = new PipedInputStream(pipedOutputStream);
    } catch (final IOException ex) {
      throw new IllegalStateException("PipedInputStream couldn't be created", ex);
    }

    messageProcessingExecutor.execute(() -> {
      try {
        nextMessage = SoapUtil.createMessage(newHeaders, pipedInputStream);
      } catch (final IOException e) {
        e.printStackTrace();
      } catch (final SOAPException e) {
        e.printStackTrace();
      } finally {
        try {
          //good or bad, we are done reading.
          messageConstructionLatch.countDown();
        } catch (final Exception ignored) {

        }
      }
    });

  }

  /**
   * Consume the next stream and feed the content to the soap message creator
   *
   * @param inputStream input stream
   */
  public void accumulate(final InputStream inputStream) {
    try {
      int read;
      while ((read = inputStream.read()) > 0) {
        pipedOutputStream.write(read);
      }
    } catch (final IOException ex) {
      throw new IllegalStateException("Stream piping failed", ex);
    }
  }


  /**
   * Close the input of the pipe so that the underlying message factory will finish reading the
   * stream chunks. Wait for the message to be ready and then return the message
   *
   * @return never <code>null</code>.
   */
  public SOAPMessage doFinal() {
    try {
      pipedOutputStream.close();
      messageConstructionLatch.await();
    } catch (final InterruptedException e) {
      throw new IllegalStateException("Cannot wait for the message to be created");
    } catch (final IOException ex) {
      throw new IllegalStateException("PipedInputStream couldn't be created", ex);
    }
    if (nextMessage == null) {
      //latch is zero but we don't have any message
      throw new NullPointerException("Couldn't internalize the SOAP Message");
    }

    return nextMessage;
  }
}
