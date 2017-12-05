package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.sirius.projectspace.ExperimentResult;
import de.unijena.bioinf.sirius.projectspace.ProjectReader;
import de.unijena.bioinf.sirius.projectspace.ProjectWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;

public class ProjectSpaceMerger implements ProjectWriter{

    protected CLI<?> cli;

    protected File tempFile;
    protected HashSet<String> ignoreSet = new HashSet<>();
    protected boolean zip;
    protected ProjectWriter underlyingWriter;

    public ProjectSpaceMerger(CLI<?> cli, String file, boolean zip) throws IOException {
        this.cli = cli;
        this.zip = zip;
        // move project space into temp
        final ProjectReader reader = zip ? cli.getSiriusOutputReader(file, cli.getWorkspaceReadingEnvironmentForSirius(file)) : cli.getDirectoryOutputReader(file, cli.getWorkspaceReadingEnvironmentForDirectoryOutput(file));

        this.tempFile = File.createTempFile("sirius", ".sirius");
        tempFile.deleteOnExit();
        try (final ProjectWriter pw = cli.getSiriusOutputWriter(tempFile.getAbsolutePath(), cli.getWorkspaceWritingEnvironmentForSirius(tempFile.getAbsolutePath()))) {
            while (reader.hasNext()) {
                final ExperimentResult er = reader.next();
                pw.writeExperiment(er);
            }
        }
        reader.close();

        // delete file
        Files.walkFileTree(new File(file).toPath(), new SimpleFileVisitor<Path>(){
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
        this.underlyingWriter = zip ? cli.getSiriusOutputWriter(file, cli.getWorkspaceWritingEnvironmentForSirius(file)) : cli.getDirectoryOutputWriter(file, cli.getWorkspaceWritingEnvironmentForDirectoryOutput(file));
    }


    @Override
    public void writeExperiment(ExperimentResult result) throws IOException {
        underlyingWriter.writeExperiment(result);
        ignoreSet.add(result.getExperimentSource() + "_" + result.getExperimentName());
    }

    @Override
    public void close() throws IOException {
        // re-add previous experiments
        try (ProjectReader reader = cli.getSiriusOutputReader(tempFile.getAbsolutePath(), cli.getWorkspaceReadingEnvironmentForSirius(tempFile.getAbsolutePath()))) {
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
