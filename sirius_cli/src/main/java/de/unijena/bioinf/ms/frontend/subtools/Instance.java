package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class Instance {
    final protected Ms2Experiment inputExperient;

    final protected ProjectSpaceManager spaceManager;
    final protected CompoundContainerId projectSpaceID;

    public Instance(@NotNull ProjectSpaceManager spaceManager, @NotNull CompoundContainerId projectSpaceID) {
        this(null, spaceManager, projectSpaceID);
    }

    public Instance(@NotNull Ms2Experiment inputExperient, @NotNull ProjectSpaceManager spaceManager) {
        this(inputExperient, spaceManager, null);
    }

    protected Instance(Ms2Experiment inputExperient, @NotNull ProjectSpaceManager spaceManager, CompoundContainerId projectSpaceID) {
        if (inputExperient == null && projectSpaceID == null)
            throw new IllegalArgumentException("Either the InputExperiment or the CompoundID must not be NULL!");
        if (inputExperient == null) {
            try {
                this.inputExperient = spaceManager.projectSpace().getCompound(projectSpaceID, Ms2Experiment.class).getAnnotationOrThrow(Ms2Experiment.class);
            } catch (IOException e) {
                throw new RuntimeException("Could not create read Input Experiment from Project Space.");
            }
        } else {
            this.inputExperient = inputExperient;
        }

        if (projectSpaceID == null) {
            final String name = spaceManager.nameFormatter().apply(inputExperient);
            this.projectSpaceID = spaceManager.projectSpace().newUniqueCompoundId(name, (idx) -> idx + "_" + name)
                    .orElseThrow(() -> new RuntimeException("Could not create an project space ID for the Instance"));
        } else {
            this.projectSpaceID = projectSpaceID;
        }

        this.spaceManager = spaceManager;
    }

    public Ms2Experiment getExperiment() {
        return inputExperient;
    }

    public CompoundContainerId getID() {
        return projectSpaceID;
    }

    public SiriusProjectSpace getProjectSpace() {
        return getProjectSpaceManager().projectSpace();
    }

    public ProjectSpaceManager getProjectSpaceManager() {
        return spaceManager;
    }
}
