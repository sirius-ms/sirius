package de.unijena.bioinf.ms.frontend.io.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.babelms.projectspace.PassatuttoSerializer;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.FBCandidateFingerprints;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.io.projectspace.summaries.FormulaSummaryWriter;
import de.unijena.bioinf.ms.frontend.io.projectspace.summaries.StructureSummaryWriter;
import de.unijena.bioinf.ms.frontend.io.projectspace.summaries.mztab.MztabMExporter;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.projectspace.*;
import de.unijena.bioinf.projectspace.fingerid.*;
import de.unijena.bioinf.projectspace.sirius.*;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Manage the project space.
 * e.g. iteration on Instance level.
 * maybe some type of caching?
 */
public class ProjectSpaceManager implements Iterable<Instance> {

    private final SiriusProjectSpace space;
    public final Function<Ms2Experiment, String> nameFormatter;
    public final BiFunction<Integer, String, String> namingScheme;
    private Predicate<CompoundContainerId> compoundIdFilter;
    protected final InstanceFactory<?> instFac;

    public ProjectSpaceManager(@NotNull SiriusProjectSpace space, @NotNull InstanceFactory<?> factory, @Nullable Function<Ms2Experiment, String> formatter) {
        this.space = space;
        this.instFac = factory;
        this.nameFormatter = space.getProjectSpaceProperty(FilenameFormatter.PSProperty.class).map(p -> (Function<Ms2Experiment, String>) new StandardMSFilenameFormatter(p.formatExpression))
                .orElseGet(() -> {
                    Function<Ms2Experiment, String> f = (formatter != null) ? formatter : new StandardMSFilenameFormatter();
                    if (f instanceof FilenameFormatter)
                        space.setProjectSpaceProperty(FilenameFormatter.PSProperty.class, new FilenameFormatter.PSProperty((FilenameFormatter) f));
                    return f;
                });

        this.namingScheme = (idx, name) -> idx + "_" + name;
    }

    public SiriusProjectSpace projectSpace() {
        return space;
    }


    @NotNull
    public Instance newCompoundWithUniqueId(Ms2Experiment inputExperiment) {
        final String name = nameFormatter.apply(inputExperiment);
        final CompoundContainer container = projectSpace().newCompoundWithUniqueId(name, (idx) -> namingScheme.apply(idx, name), inputExperiment).orElseThrow(() -> new RuntimeException("Could not create an project space ID for the Instance"));
        return instFac.create(container, this);
    }

    public Predicate<CompoundContainerId> getCompoundIdFilter() {
        return compoundIdFilter;
    }

    public void setCompoundIdFilter(Predicate<CompoundContainerId> compoundFilter) {
        this.compoundIdFilter = compoundFilter;
    }

    @SafeVarargs
    public final Instance newInstanceFromCompound(CompoundContainerId id, Class<? extends DataAnnotation>... components) {
        try {
            CompoundContainer c = projectSpace().getCompound(id, components);
            return instFac.create(c, this);
        } catch (IOException e) {
            LoggerFactory.getLogger(Instance.class).error("Could not create read Input Experiment from Project Space.");
            throw new RuntimeException("Could not create read Input Experiment from Project Space.", e);
        }
    }

    public <T extends ProjectSpaceProperty> Optional<T> getProjectSpaceProperty(Class<T> key) {
        return projectSpace().getProjectSpaceProperty(key);
    }

    public <T extends ProjectSpaceProperty> T setProjectSpaceProperty(Class<T> key, T value) {
        return projectSpace().setProjectSpaceProperty(key, value);
    }

    @NotNull
    public Iterator<Instance> filteredIterator(@Nullable Predicate<CompoundContainerId> cidFilter, @Nullable final Predicate<CompoundContainer> compoundFilter) {
        if (compoundFilter == null && cidFilter == null)
            return iterator();
        final Predicate<CompoundContainerId> cidF = (cidFilter != null && compoundFilter != null)
                ? (cid) -> cidFilter.test(cid) && this.compoundIdFilter.test(cid)
                : cidFilter == null ? this.compoundIdFilter : cidFilter;
        return makeInstanceIterator(space.filteredCompoundIterator(cidF, compoundFilter, Ms2Experiment.class));
    }


    @NotNull
    @Override
    public Iterator<Instance> iterator() {
        if (compoundIdFilter != null)
            return filteredIterator(compoundIdFilter, null);
        return makeInstanceIterator(space.compoundIterator(Ms2Experiment.class));
    }

    private Iterator<Instance> makeInstanceIterator(@NotNull final Iterator<CompoundContainer> compoundIt) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return compoundIt.hasNext();
            }

            @Override
            public Instance next() {
                final CompoundContainer c = compoundIt.next();
                if (c == null) return null;
                return instFac.create(c, ProjectSpaceManager.this);
            }
        };
    }

    public static Summarizer[] defaultSummarizer() {
        return new Summarizer[]{
                new FormulaSummaryWriter(),
                new StructureSummaryWriter(),
                new MztabMExporter()
        };
    }

    public static List<Class<? extends FormulaScore>> scorePriorities() {
        final LinkedList<Class<? extends FormulaScore>> list = new LinkedList<>();
        list.add(ConfidenceScore.class);
        list.add(TopCSIScore.class);
        list.add(ZodiacScore.class);
        list.add(SiriusScore.class);
        list.add(TreeScore.class);
        list.add(IsotopeScore.class);
        return list;
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
        config.defineProjectSpaceProperty(FingerIdData.class, new CsiClientSerializer());
        config.registerComponent(FormulaResult.class, FingerprintResult.class, new FingerprintSerializer());
        config.registerComponent(FormulaResult.class, FBCandidates.class, new FBCandidatesSerializer());
        config.registerComponent(FormulaResult.class, FBCandidateFingerprints.class, new FBCandidateFingerprintSerializer());
        //canopus
        config.defineProjectSpaceProperty(CanopusData.class, new CanopusClientSerializer());
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
