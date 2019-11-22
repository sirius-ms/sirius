package de.unijena.bioinf.ms.frontend.subtools.projectspace;

import de.unijena.bioinf.ms.frontend.io.InputFiles;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public class ProjectSpaceSubToolJob extends PreprocessingJob {

    public ProjectSpaceSubToolJob(@Nullable InputFiles input, @Nullable ProjectSpaceManager space) {
        super(input, space);
    }

    @Override
    protected ProjectSpaceManager compute() throws Exception {
        //todo fill me
        System.out.println("PROJECT-SPACE Subtool: I will allow you to modify a given project space if I am implemented");
        return space;
    }
}
