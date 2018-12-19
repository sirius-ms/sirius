package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.babelms.Index;
import de.unijena.bioinf.sirius.ExperimentResult;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;

public class ProjectSpaceMerger implements ProjectWriter {

    protected File tempFile;
    protected HashSet<String> ignoreSet = new HashSet<>();
    protected boolean zip;
    protected ProjectWriter underlyingWriter;

    protected int numberOfWrittenExperiments;

    protected ReaderWriterFactory readerWriterFactory;

    public ProjectSpaceMerger(ReaderWriterFactory readerWriterFactory, String file, boolean zip) throws IOException {
        this.readerWriterFactory = readerWriterFactory;
        this.zip = zip;
        // move project space into temp
        final ProjectReader reader = zip ? readerWriterFactory.getSiriusOutputReader(file, ProjectSpaceUtils.getWorkspaceReadingEnvironmentForSirius(file)) : readerWriterFactory.getDirectoryOutputReader(file, ProjectSpaceUtils.getWorkspaceReadingEnvironmentForDirectoryOutput(file));

        this.tempFile = File.createTempFile("sirius", ".sirius");
        tempFile.deleteOnExit();
        this.numberOfWrittenExperiments = 0;
        try (final ProjectWriter pw = readerWriterFactory.getSiriusOutputWriter(tempFile.getAbsolutePath(), ProjectSpaceUtils.getWorkspaceWritingEnvironmentForSirius(tempFile.getAbsolutePath()))) {
            while (reader.hasNext()) {
                try {
                    final ExperimentResult er = reader.next();
                    pw.writeExperiment(er);
                    numberOfWrittenExperiments = Math.max(numberOfWrittenExperiments, er.getExperiment().getAnnotation(Index.class, () -> Index.NO_INDEX).index);
                } catch (Throwable t) {
                    LoggerFactory.getLogger(ProjectSpaceMerger.class).error(t.getMessage(), t);
                }
            }
        }
        reader.close();

        // delete file
        Files.walkFileTree(new File(file).toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!attrs.isDirectory()) Files.delete(file);
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return super.postVisitDirectory(dir, exc);
            }
        });

        // recreate file
        this.underlyingWriter = zip ? readerWriterFactory.getSiriusOutputWriter(file, ProjectSpaceUtils.getWorkspaceWritingEnvironmentForSirius(file)) : readerWriterFactory.getDirectoryOutputWriter(file, ProjectSpaceUtils.getWorkspaceWritingEnvironmentForDirectoryOutput(file));
    }

    public int getNumberOfWrittenExperiments() {
        return numberOfWrittenExperiments;
    }

    @Override
    public void writeExperiment(ExperimentResult result) throws IOException {
        underlyingWriter.writeExperiment(result);
        ignoreSet.add(result.getExperimentSource() + "_" + result.getExperimentName());
    }

    @Override
    public void close() throws IOException {
        // re-add previous experiments
        try (ProjectReader reader = readerWriterFactory.getSiriusOutputReader(tempFile.getAbsolutePath(), ProjectSpaceUtils.getWorkspaceReadingEnvironmentForSirius(tempFile.getAbsolutePath()))) {
            while (reader.hasNext()) {
                final ExperimentResult result = reader.next();
                if (!ignoreSet.contains(result.getExperimentSource() + "_" + result.getExperimentName())) {
                    underlyingWriter.writeExperiment(result);
                }
            }
        } finally {
            underlyingWriter.close();
        }

    }

}
