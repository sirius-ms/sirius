package de.unijena.bioinf.ms.middleware.service.search.dynamic;

import org.apache.lucene.store.*;
import java.io.*;
import java.util.zip.*;

public class LuceneDirectoryPersistenceUtils {

    // --- CONVENIENCE METHODS (Optional flag) ---

    public static byte[] serialize(Directory dir) throws IOException {
        return serializeUncompressed(dir);
    }

    public static byte[] serialize(Directory dir, boolean compress) throws IOException {
        return compress ? serializeZipped(dir) : serializeUncompressed(dir);
    }

    public static void deserialize(byte[] bytes, Directory targetDir) throws IOException {
        deserializeUncompressed(bytes, targetDir);
    }

    public static void deserialize(byte[] bytes, Directory targetDir, boolean isCompressed) throws IOException {
        if (isCompressed) {
            deserializeZipped(bytes, targetDir);
        } else {
            deserializeUncompressed(bytes, targetDir);
        }
    }

    // --- ZIPPED METHODS (Original behavior) ---

    public static byte[] serializeZipped(Directory dir) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (String file : dir.listAll()) {
                // EXTREMELY IMPORTANT: Never persist the write.lock file
                if ("write.lock".equals(file)) {
                    continue;
                }

                ZipEntry entry = new ZipEntry(file);
                zos.putNextEntry(entry);
                try (IndexInput input = dir.openInput(file, IOContext.DEFAULT)) {
                    byte[] buffer = new byte[8192];
                    long bytesLeft = input.length();
                    while (bytesLeft > 0) {
                        int toRead = (int) Math.min(buffer.length, bytesLeft);
                        input.readBytes(buffer, 0, toRead);
                        zos.write(buffer, 0, toRead);
                        bytesLeft -= toRead;
                    }
                }
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    public static void deserializeZipped(byte[] zipBytes, Directory targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                try (IndexOutput output = targetDir.createOutput(entry.getName(), IOContext.DEFAULT)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = zis.read(buffer)) != -1) {
                        output.writeBytes(buffer, read);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    // --- UNCOMPRESSED METHODS (New behavior) ---

    public static byte[] serializeUncompressed(Directory dir) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            String[] files = dir.listAll();

            // Count files first to write the header (ignoring write.lock)
            int fileCount = 0;
            for (String file : files) {
                if (!"write.lock".equals(file)) fileCount++;
            }
            dos.writeInt(fileCount);

            // Write each file's metadata and content
            for (String file : files) {
                if ("write.lock".equals(file)) {
                    continue;
                }

                dos.writeUTF(file);
                try (IndexInput input = dir.openInput(file, IOContext.DEFAULT)) {
                    long length = input.length();
                    dos.writeLong(length); // Write the exact file length

                    byte[] buffer = new byte[8192];
                    long bytesLeft = length;
                    while (bytesLeft > 0) {
                        int toRead = (int) Math.min(buffer.length, bytesLeft);
                        input.readBytes(buffer, 0, toRead);
                        dos.write(buffer, 0, toRead);
                        bytesLeft -= toRead;
                    }
                }
            }
        }
        return baos.toByteArray();
    }

    public static void deserializeUncompressed(byte[] bytes, Directory targetDir) throws IOException {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int fileCount = dis.readInt();

            for (int i = 0; i < fileCount; i++) {
                String fileName = dis.readUTF();
                long length = dis.readLong();

                try (IndexOutput output = targetDir.createOutput(fileName, IOContext.DEFAULT)) {
                    byte[] buffer = new byte[8192];
                    long bytesLeft = length;
                    while (bytesLeft > 0) {
                        int toRead = (int) Math.min(buffer.length, bytesLeft);
                        // Using readFully prevents partial array reads
                        dis.readFully(buffer, 0, toRead);
                        output.writeBytes(buffer, toRead);
                        bytesLeft -= toRead;
                    }
                }
            }
        }
    }
}