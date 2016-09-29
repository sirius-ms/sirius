package de.unijena.bioinf.babelms.utils;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 29.09.16.
 */

import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class Compress {
    public static void compressToZipArchive(File zipFile, File... srcFiles) {
        try (
                FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos)
        ) {
            // create byte buffer
            byte[] buffer = new byte[1024];

            for (File srcFile : srcFiles) {
                try (FileInputStream fis = new FileInputStream(srcFile)) {

                    // begin writing a new ZIP entry, positions the stream to the start of the entry data
                    zos.putNextEntry(new ZipEntry(srcFile.getName()));
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                    // close the InputStream
                    fis.close();
                } catch (IOException ioe) {
                    LoggerFactory.getLogger(Compress.class).error("Could not Compress " + srcFile.getAbsolutePath(), ioe);
                    throw ioe;
                }
            }
            // close the ZipOutputStream
            zos.close();
        } catch (IOException ioe) {
            LoggerFactory.getLogger(Compress.class).error("Could not Create zip archive " + zipFile.getAbsolutePath(), ioe);
        }
    }

    public static void compressToZipArchive(File zipFile, Map<InputStream, String> input) {
        try {
            compressToZipArchive(new FileOutputStream(zipFile), input);
        } catch (FileNotFoundException e) {
            LoggerFactory.getLogger(Compress.class).error("Could not Create zip archive " + zipFile.getAbsolutePath(), e);
        }
    }

    public static void compressToZipArchive(OutputStream zipFile, Map<InputStream, String> input) {
        try (
                ZipOutputStream zos = new ZipOutputStream(zipFile)
        ) {
            // create byte buffer
            byte[] buffer = new byte[1024];


            int index = 0;
            for (Map.Entry<InputStream, String> entry : input.entrySet()) {
                String filename = entry.getValue();
                if (filename == null || filename.isEmpty())
                    filename = "file" + index + ".txt";
                index++;
                try (InputStream stream = entry.getKey()) {
                    // begin writing a new ZIP entry, positions the stream to the start of the entry data
                    zos.putNextEntry(new ZipEntry(filename));
                    int length;
                    while ((length = stream.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);

                    }
                    zos.closeEntry();
                    // close the InputStream
                    stream.close();
                } catch (IOException ioe) {
                    LoggerFactory.getLogger(Compress.class).error("Could not Compress " + filename, ioe);
                    throw ioe;
                }
            }

            // close the ZipOutputStream
            zos.close();
        } catch (IOException ioe) {
            LoggerFactory.getLogger(Compress.class).error("Could not Create zip archive", ioe);
        }
    }

    /*public static void main(String[] args) throws IOException {
       *//* compressToZipArchive(File.createTempFile("archive", ".zip"),
                Paths.get(System.getProperty("user.home")).resolve(".sirius").resolve("sirius.log.0").toFile(),
                Paths.get(System.getProperty("user.home")).resolve(".sirius").resolve("logging.properties").toFile(),
                Paths.get(System.getProperty("user.home")).resolve(".sirius").resolve("sirius.properties").toFile()
        );*//*

        Map<InputStream,String> input =  new HashMap<>();
        input.put(new FileInputStream(Paths.get(System.getProperty("user.home")).resolve(".sirius").resolve("sirius.log.0").toFile()),"");
        input.put(new FileInputStream(Paths.get(System.getProperty("user.home")).resolve(".sirius").resolve("logging.properties").toFile()),null);
        input.put(new FileInputStream(Paths.get(System.getProperty("user.home")).resolve(".sirius").resolve("sirius.properties").toFile()),"validName");


        compressToZipArchive(new FileOutputStream(File.createTempFile("archive", ".zip")),
                input
        );
    }*/

}
