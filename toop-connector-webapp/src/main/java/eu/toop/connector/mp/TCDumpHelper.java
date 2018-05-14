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
package eu.toop.connector.mp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.io.file.FileHelper;

/**
 * Dump helper function.
 *
 * @author Philip Helger
 *
 */
public final class TCDumpHelper {
  private static final Logger s_aLogger = LoggerFactory.getLogger (TCDumpHelper.class);

  private TCDumpHelper () {
  }

  @Nonnull
  public static InputStream getDumpInputStream (@Nonnull final InputStream aSrcIS, @Nullable final File aDumpDirectory,
                                                @Nonnull @Nonempty final String sContextAndExtension) {
    ValueEnforcer.notNull (aSrcIS, "SrcIS");
    ValueEnforcer.notEmpty (sContextAndExtension, "ContextAndExtension");

    if (aDumpDirectory != null && aDumpDirectory.exists ()) {
      // Only if the dump directory is present and existing
      final String sFilename = Long.toString (System.nanoTime ()) + sContextAndExtension;
      final File aDumpFile = new File (aDumpDirectory, sFilename);

      // Open log file
      final FileOutputStream aDebugFOS = FileHelper.getOutputStream (aDumpFile);
      if (aDebugFOS != null) {
        return new FilterInputStream (aSrcIS) {
          @Override
          public void close () throws IOException {
            try {
              // Close dump file as well
              aDebugFOS.close ();
            } finally {
              super.close ();
            }
          }

          @Override
          public int read () throws IOException {
            final int ret = super.read ();
            if (ret >= 0)
              aDebugFOS.write ((byte) ret);
            return ret;
          }

          @Override
          public int read (final byte[] aBuf, final int nOfs, final int nLen) throws IOException {
            final int ret = super.read (aBuf, nOfs, nLen);
            if (ret >= 0)
              aDebugFOS.write (aBuf, nOfs, ret);
            return ret;
          }
        };
      }

      s_aLogger.warn ("Failed to open dump file '" + aDumpFile.getAbsolutePath () + "' for writing");
    }

    return aSrcIS;
  }
}