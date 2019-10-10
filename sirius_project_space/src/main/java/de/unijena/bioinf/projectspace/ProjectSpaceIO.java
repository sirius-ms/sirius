package de.unijena.bioinf.projectspace;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ProjectSpaceIO {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectSpaceIO.class);

    protected final ProjectSpaceConfiguration configuration;

    public ProjectSpaceIO(ProjectSpaceConfiguration configuration) {
        this.configuration = configuration;
    }

    public SiriusProjectSpace openExistingProjectSpace(File path) throws IOException {
        final SiriusProjectSpace space = new SiriusProjectSpace(configuration,path);
        space.open();
        return space;
    }

    public SiriusProjectSpace createNewProjectSpace(File path) throws IOException {
        if (!path.mkdir())
            throw new IOException("Could not create new directory '" + path + "'");
        final SiriusProjectSpace space = new SiriusProjectSpace(configuration,path);
        space.open();
        return space;
    }

    public SiriusProjectSpace createTemporaryProjectSpace() throws IOException {
        final File tempFile = Files.createTempDirectory("-tmp-project-space").toFile();
        final SiriusProjectSpace space = new SiriusProjectSpace(configuration,tempFile);
        space.addProjectSpaceListener(new TemporaryProjectSpaceCleanUp(tempFile));
        space.open();
        return space;
    }


    /**
     * Check for a compressed project-space by file ending
     */
    public static boolean isCompressedProjectSpace(File file) {
        if (!file.isFile()) return false;
        final String lowercaseName = file.getName().toLowerCase();
        return lowercaseName.endsWith(".workspace") || lowercaseName.endsWith(".zip") || lowercaseName.endsWith(".sirius");
    }

    /**
     * Just a quick check to discriminate a project-space for an arbitrary folder
     */
    public static boolean isExistingProjectspaceDirectory(@NotNull File f) {
        if (!f.exists() || f.list().length == 0)
            return false;

        try {
            try (SiriusProjectSpace space = new SiriusProjectSpace(new ProjectSpaceConfiguration(), f)) {
                space.open();
                return space.size() > 0;
            } catch (IOException ignored) {
                return false;
            }
        } catch (Exception e) {
            // not critical: if file cannot be read, it is not a valid workspace
            LOG.error("Workspace check failed! This is not a valid Project-Space!", e);
            return false;
        }
    }


}
