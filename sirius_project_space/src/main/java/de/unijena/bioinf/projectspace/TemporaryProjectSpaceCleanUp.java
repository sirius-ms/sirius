package de.unijena.bioinf.projectspace;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Be careful: This listener will delete all files in the given directory after closing the project space.
 * To avoid data loss, this object should only be initialized with a subdirectory of /tmp
 */
public class TemporaryProjectSpaceCleanUp implements ProjectSpaceListener{

    protected final File cleanupPath;

    TemporaryProjectSpaceCleanUp(File cleanupPath) {
        this.cleanupPath = cleanupPath;
    }

    @Override
    public void projectSpaceChanged(ProjectSpaceEvent event) {
        if (event==ProjectSpaceEvent.CLOSED) {
            // cleanup complete temporary directory
            if (cleanupPath.exists()) {
                try {
                    Files.walkFileTree(cleanupPath.toPath(), new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    LoggerFactory.getLogger("Could not delete directory " + cleanupPath);
                }
            }
        }
    }
}
