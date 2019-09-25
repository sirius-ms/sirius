package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.properties.FinalConfig;
import de.unijena.bioinf.babelms.ProjectSpaceManager;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.ProjectSpaceConfig;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class Instance {
    final protected Ms2Experiment inputExperient;

    final protected ProjectSpaceManager spaceManager;
    final protected CompoundContainerId projectSpaceID;

    public Instance(@NotNull ProjectSpaceManager spaceManager, @NotNull CompoundContainerId projectSpaceID) {
        this(null, spaceManager, projectSpaceID);
    }

    public Instance(@Nullable Ms2Experiment inputExperient, @NotNull ProjectSpaceManager spaceManager, @NotNull CompoundContainerId projectSpaceID) {
        if (inputExperient == null) {
            try {
                this.inputExperient = spaceManager.projectSpace().getCompound(projectSpaceID, Ms2Experiment.class).getAnnotationOrThrow(Ms2Experiment.class);
            } catch (IOException e) {
                LoggerFactory.getLogger(Instance.class).error("Could not create read Input Experiment from Project Space.", e);
                throw new RuntimeException("Could not create read Input Experiment from Project Space.");
            }
        } else {
            this.inputExperient = inputExperient;
        }

        this.projectSpaceID = projectSpaceID;
        this.spaceManager = spaceManager;
    }

    public Ms2Experiment getExperiment() {
        return inputExperient;
    }

    public CompoundContainerId getID() {
        return projectSpaceID;
    }

    @Override
    public String toString() {
        return getID().toString();
    }

    public SiriusProjectSpace getProjectSpace() {
        return getProjectSpaceManager().projectSpace();
    }

    public ProjectSpaceManager getProjectSpaceManager() {
        return spaceManager;
    }


    public Optional<ProjectSpaceConfig> loadConfig() {
        return loadCompoundContainer(ProjectSpaceConfig.class).getAnnotation(ProjectSpaceConfig.class);
    }

    public List<? extends SScored<FormulaResult, ? extends FormulaScore>> loadFormulaResults(Class<? extends FormulaScore> rankingScoreType, Class<? extends DataAnnotation>... components) {
        try {
            return getProjectSpace().getFormulaResultsOrderedBy(getID(),
                    rankingScoreType,
                    components);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public CompoundContainer loadCompoundContainer(Class<? extends DataAnnotation>... components) {
        try {
            return getProjectSpace().getCompound(getID(), components);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateFormulaResult(FormulaResult result, Class<? extends DataAnnotation>... components) {
        try {
            getProjectSpace().updateFormulaResult(result, components);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateCompound(CompoundContainer result, Class<? extends DataAnnotation>... components) {
        try {
            getProjectSpace().updateCompound(result, components);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateExperiment() {
        CompoundContainer c = loadCompoundContainer();
        c.setAnnotation(Ms2Experiment.class, inputExperient);
        updateCompound(c, Ms2Experiment.class);
    }

    public void updateConfig() {
        CompoundContainer c = loadCompoundContainer();
        c.setAnnotation(ProjectSpaceConfig.class, new ProjectSpaceConfig(inputExperient.getAnnotationOrThrow(FinalConfig.class).config));
        updateCompound(c, ProjectSpaceConfig.class);
    }
}