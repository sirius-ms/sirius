package de.unijena.bioinf.babelms.utils;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 29.09.16.
 */

import org.slf4j.LoggerFactory;

import java.io.*;
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

    public static void compressToZipArchive(File zipFile, InputStream[] ins, String[] fileNames) {
        try {
            compressToZipArchive(new FileOutputStream(zipFile), ins, fileNames);
        } catch (FileNotFoundException e) {
            LoggerFactory.getLogger(Compress.class).error("Could not Create zip archive " + zipFile.getAbsolutePath(), e);
        }
    }

    public static void compressToZipArchive(OutputStream zipFile, InputStream[] ins, String[] fileNames) {
        try (
                ZipOutputStream zos = new ZipOutputStream(zipFile)
        ) {
            // create byte buffer
            byte[] buffer = new byte[1024];

            String filename = null;
            for (int i = 0; i < ins.length; i++) {
                try (InputStream stream = ins[i];) {
                    if (fileNames != null && fileNames.length > i)
                        filename = fileNames[i];
                    else
                        filename = "file" + i + ".txt";

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
        compressToZipArchive(File.createTempFile("archive", ".zip"),
                Paths.get(System.getProperty("user.home")).resolve(".sirius").resolve("sirius.log.0").toFile(),
                Paths.get(System.getProperty("user.home")).resolve(".sirius").resolve("logging.properties").toFile(),
                Paths.get(System.getProperty("user.home")).resolve(".sirius").resolve("sirius.properties").toFile()
        );
        compressToZipArchive(new FileOutputStream(File.createTempFile("archive", ".zip")),
                new FileInputStream[]{
                        new FileInputStream(Paths.get(System.getProperty("user.home")).resolve(".sirius").resolve("sirius.log.0").toFile()),
                        new FileInputStream(Paths.get(System.getProperty("user.home")).resolve(".sirius").resolve("logging.properties").toFile()),
                        new FileInputStream(Paths.get(System.getProperty("user.home")).resolve(".sirius").resolve("sirius.properties").toFile())
                },
                new String[]{"file1","file2"}
        );
    }*/

}
