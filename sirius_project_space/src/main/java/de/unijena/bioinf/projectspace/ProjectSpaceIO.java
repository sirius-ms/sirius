package de.unijena.bioinf.projectspace;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;

public class ProjectSpaceIO {

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
        final File tempFile = Files.createTempDir();
        final SiriusProjectSpace space = new SiriusProjectSpace(configuration,tempFile);
        space.addProjectSpaceListener(new TemporaryProjectSpaceCleanUp(tempFile));
        space.open();
        return space;
    }

}
