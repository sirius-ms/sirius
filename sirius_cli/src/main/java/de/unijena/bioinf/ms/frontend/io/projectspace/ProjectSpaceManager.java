package de.unijena.bioinf.ms.frontend.io.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.projectspace.PassatuttoSerializer;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.projectspace.fingerid.*;
import de.unijena.bioinf.projectspace.sirius.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Manage the project space.
 * e.g. iteration on Instance level.
 * maybe some type of caching?
 */
public final class ProjectSpaceManager implements Iterable<Instance> {

    private final SiriusProjectSpace space;
    private final Function<Ms2Experiment, String> nameFormatter;
    private final Predicate<CompoundContainerId> compoundFilter;

    public ProjectSpaceManager(@NotNull SiriusProjectSpace space) {
        this(space, null, null);
    }

    public ProjectSpaceManager(@NotNull SiriusProjectSpace space, @Nullable Function<Ms2Experiment, String> formatter, @Nullable Predicate<CompoundContainerId> compoundFilter) {
        this.space = space;
        this.nameFormatter = formatter != null
                ? formatter
                : new StandardMSFilenameFormatter();
        this.compoundFilter = compoundFilter;
    }

    SiriusProjectSpace projectSpace() {
        return space;
    }

    public Function<Ms2Experiment, String> nameFormatter() {
        return nameFormatter;
    }


    @NotNull
    public CompoundContainer newCompoundWithUniqueId(Ms2Experiment inputExperiment) {
        final String name = nameFormatter().apply(inputExperiment);
        try {
            final CompoundContainer container = projectSpace().newCompoundWithUniqueId(name, (idx) -> idx + "_" + name).orElseThrow(() -> new RuntimeException("Could not create an project space ID for the Instance"));
            container.setAnnotation(Ms2Experiment.class, inputExperiment);
            projectSpace().updateCompound(container, Ms2Experiment.class);
            return container;
        } catch (IOException e) {
            LoggerFactory.getLogger(ProjectSpaceManager.class).error("Could not create an project space ID for the Instance", e);
            throw new RuntimeException("Could not create an project space ID for the Instance");
        }
    }

    public <T extends ProjectSpaceProperty> Optional<T> getProjectSpaceProperty(Class<T> key) {
        return projectSpace().getProjectSpaceProperty(key);
    }

    public <T extends ProjectSpaceProperty> T setProjectSpaceProperty(Class<T> key, T value) {
        return projectSpace().setProjectSpaceProperty(key, value);
    }


    @NotNull
    @Override
    public Iterator<Instance> iterator() {
        return new Iterator<>() {
            final Iterator<CompoundContainerId> it = compoundFilter == null
                    ? space.iterator()
                    : space.filteredIterator(compoundFilter);

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Instance next() {
                final CompoundContainerId id = it.next();
                if (id == null) return null;
                return new Instance(id,ProjectSpaceManager.this);
            }
        };
    }

    public static ProjectSpaceConfiguration newDefaultConfig(){
        final ProjectSpaceConfiguration config = new ProjectSpaceConfiguration();
        //configure ProjectSpaceProperties
        config.defineProjectSpaceProperty(FilenameFormatter.PSProperty.class, new FilenameFormatter.PSPropertySerializer());
        //configure compound container
        config.registerContainer(CompoundContainer.class, new CompoundContainerSerializer());
        config.registerComponent(CompoundContainer.class, ProjectSpaceConfig.class, new ProjectSpaceConfigSerializer());
        config.registerComponent(CompoundContainer.class, Ms2Experiment.class, new MsExperimentSerializer());
        //configure formula result
        config.registerContainer(FormulaResult.class, new FormulaResultSerializer());
        config.registerComponent(FormulaResult.class, FTree.class, new TreeSerializer());
        config.registerComponent(FormulaResult.class, FormulaScoring.class, new FormulaScoringSerializer());
        //pssatuto components
        config.registerComponent(FormulaResult.class, Decoy.class, new PassatuttoSerializer());
        //fingerid components
        config.defineProjectSpaceProperty(CSIClientData.class, new CsiClientSerializer());
        config.registerComponent(FormulaResult.class, FingerprintResult.class, new FingerprintSerializer());
        config.registerComponent(FormulaResult.class, FingerblastResult.class, new FingerblastResultSerializer());
        //canopus
        config.defineProjectSpaceProperty(CanopusClientData.class, new CanopusClientSerializer());
        config.registerComponent(FormulaResult.class, CanopusResult.class, new CanopusSerializer());
        return config;
    }

    public int size() {
        return space.size();
    }

    public boolean containsCompound(String dirName) {
        return space.containsCompound(dirName);
    }

    public boolean containsCompound(CompoundContainerId id) {
        return space.containsCompound(id);
    }

    public void updateSummaries(Summarizer... summarizers) throws IOException {
        space.updateSummaries(summarizers);
    }

    public void close() throws IOException {
        space.close();
    }
}
