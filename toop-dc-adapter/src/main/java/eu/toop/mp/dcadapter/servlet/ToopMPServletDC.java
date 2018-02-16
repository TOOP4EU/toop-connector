package eu.toop.mp.dcadapter.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToopMPServletDC extends HttpServlet {


    private static final Logger log = LoggerFactory.getLogger(ToopMPServletDC.class);

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {


        File targetFile = Files.createTempFile("toop-mp",".temp").toFile();
        targetFile.deleteOnExit();
        log.info("Created Temp File {}", targetFile.getAbsolutePath());


        log.info("Copy Stream to Temp file");
        Files.copy(
                request.getInputStream(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);

        // add file in queue for consumption
            MessageQueue.INSTANCE.add(targetFile);
        log.info("Added file to MessageQueue");
    }

}
