/**
 * Copyright (C) 2018-2019 toop.eu
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
package eu.toop.connector.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.io.stream.WrappedInputStream;
import com.helger.commons.io.stream.WrappedOutputStream;

/**
 * Dump helper function.
 *
 * @author Philip Helger
 */
@Immutable
public final class TCDumpHelper
{
  private static final Logger LOGGER = LoggerFactory.getLogger (TCDumpHelper.class);

  private TCDumpHelper ()
  {}

  @Nonnull
  public static InputStream getDumpInputStream (@Nonnull final InputStream aSrcIS,
                                                @Nullable final File aDumpDirectory,
                                                @Nonnull @Nonempty final String sContextAndExtension)
  {
    ValueEnforcer.notNull (aSrcIS, "SrcIS");
    ValueEnforcer.notEmpty (sContextAndExtension, "ContextAndExtension");

    if (aDumpDirectory != null)
    {
      // Only if the dump directory is present and existing
      final String sFilename = Long.toString (System.nanoTime ()) + sContextAndExtension;
      final File aDumpFile = new File (aDumpDirectory, sFilename);

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Trying to open DumpInputStream file " + aDumpFile.getAbsolutePath ());

      // Open log file (creates parent directories if necessary)
      final FileOutputStream aDebugFOS = FileHelper.getOutputStream (aDumpFile);
      if (aDebugFOS != null)
      {
        if (LOGGER.isInfoEnabled ())
          LOGGER.info ("Dumping InputStream to " + aDumpFile.getAbsolutePath ());

        return new WrappedInputStream (aSrcIS)
        {
          @Override
          public int read () throws IOException
          {
            final int ret = super.read ();
            if (ret >= 0)
              aDebugFOS.write ((byte) ret);
            return ret;
          }

          @Override
          public int read (final byte [] aBuf, final int nOfs, final int nLen) throws IOException
          {
            final int ret = super.read (aBuf, nOfs, nLen);
            if (ret >= 0)
              aDebugFOS.write (aBuf, nOfs, ret);
            return ret;
          }

          @Override
          public void close () throws IOException
          {
            try
            {
              // Close and flush dump file as well
              StreamHelper.close (aDebugFOS);
            }
            finally
            {
              super.close ();
            }
          }
        };
      }

      if (LOGGER.isWarnEnabled ())
        LOGGER.warn ("Failed to open DumpInputStream file '" + aDumpFile.getAbsolutePath () + "' for writing");
    }

    return aSrcIS;
  }

  @Nonnull
  public static OutputStream getDumpOutputStream (@Nonnull final OutputStream aSrcOS,
                                                  @Nullable final File aDumpDirectory,
                                                  @Nonnull @Nonempty final String sContextAndExtension)
  {
    ValueEnforcer.notNull (aSrcOS, "SrcOS");
    ValueEnforcer.notEmpty (sContextAndExtension, "ContextAndExtension");

    if (aDumpDirectory != null)
    {
      // Only if the dump directory is set
      final String sFilename = Long.toString (System.nanoTime ()) + sContextAndExtension;
      final File aDumpFile = new File (aDumpDirectory, sFilename);

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Trying to open DumpOutputStream file " + aDumpFile.getAbsolutePath ());

      // Open log file (creates parent directories if necessary)
      final FileOutputStream aDebugFOS = FileHelper.getOutputStream (aDumpFile);
      if (aDebugFOS != null)
      {
        if (LOGGER.isInfoEnabled ())
          LOGGER.info ("Dumping OutputStream to " + aDumpFile.getAbsolutePath ());

        return new WrappedOutputStream (aSrcOS)
        {
          @Override
          public void write (final int b) throws IOException
          {
            super.write (b);
            aDebugFOS.write (b);
          }

          @Override
          public void write (final byte [] aBuf, final int nOfs, final int nLen) throws IOException
          {
            super.write (aBuf, nOfs, nLen);
            aDebugFOS.write (aBuf, nOfs, nLen);
          }

          @Override
          public void flush () throws IOException
          {
            try
            {
              aDebugFOS.flush ();
            }
            finally
            {
              super.flush ();
            }
          }

          @Override
          public void close () throws IOException
          {
            try
            {
              // Close dump file as well
              aDebugFOS.close ();
            }
            finally
            {
              super.close ();
            }
          }
        };
      }

      if (LOGGER.isWarnEnabled ())
        LOGGER.warn ("Failed to open DumpOutputStream file '" + aDumpFile.getAbsolutePath () + "' for writing");
    }

    return aSrcOS;
  }
}
