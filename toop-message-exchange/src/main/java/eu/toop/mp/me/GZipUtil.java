package eu.toop.mp.me;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author: myildiz
 * @date: 15.02.2018.
 */
public class GZipUtil {
   /**
    * compress the given byte array
    *
    * @param plain
    * @return
    */
   public static byte[] gzip(byte[] plain) {
      ByteArrayInputStream bais = new ByteArrayInputStream(plain);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      gzip(bais, baos);
      return baos.toByteArray();
   }


   public static byte[] gunzip(byte[] compressed) {
      ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      gunzip(bais, baos);
      return baos.toByteArray();
   }

   /**
    * Compress the given stream as GZIP
    *
    * @param inputStream
    * @param outputStream
    */
   public static void gzip(InputStream inputStream, OutputStream outputStream) {
      try {
         GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream, true);
         transferData(inputStream, gzipOutputStream);
         gzipOutputStream.close();
      } catch (Exception e) {
         throw new RuntimeException("GZIP Compression failed");
      }
   }

   /**
    * Decompress the given stream that contains gzip data
    *
    * @param inputStream
    * @param outputStream
    */
   public static void gunzip(InputStream inputStream, OutputStream outputStream) {
      try {
         GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
         transferData(gzipInputStream, outputStream);
         gzipInputStream.close();
      } catch (Exception e) {
         throw new RuntimeException("GZIP decompression failed");
      }
   }

   /**
    * Transfer the data from <code>gzipInputStream</code> to <code>gzipOutputStream</code> until the end of stream reaches
    *
    * @param gzipInputStream
    * @param outputStream
    * @throws Exception
    */
   public static void transferData(InputStream gzipInputStream, OutputStream outputStream) throws Exception {
      byte[] chunk = new byte[1024];
      int read = -1;
      while ((read = gzipInputStream.read(chunk, 0, chunk.length)) > 0) {
         outputStream.write(chunk, 0, read);
      }
   }
}
