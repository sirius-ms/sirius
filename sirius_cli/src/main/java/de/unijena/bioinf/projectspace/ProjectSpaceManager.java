/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintData;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.ChemistryBase.utils.IterableWithSize;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.babelms.projectspace.PassatuttoSerializer;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.FBCandidateFingerprints;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.networks.serialization.ConnectionTable;
import de.unijena.bioinf.networks.serialization.ConnectionTableSerializer;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.projectspace.canopus.CanopusCfDataProperty;
import de.unijena.bioinf.projectspace.canopus.CanopusNpcDataProperty;
import de.unijena.bioinf.projectspace.canopus.CanopusSerializer;
import de.unijena.bioinf.projectspace.fingerid.*;
import de.unijena.bioinf.projectspace.summaries.CanopusSummaryWriter;
import de.unijena.bioinf.projectspace.summaries.FormulaSummaryWriter;
import de.unijena.bioinf.projectspace.summaries.StructureSummaryWriter;
import de.unijena.bioinf.projectspace.summaries.mztab.MztabMExporter;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;
import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Manage the project space.
 * e.g. iteration on Instance level.
 * maybe some type of caching?
 */
public class ProjectSpaceManager<I extends Instance> implements IterableWithSize<I> {
    @NotNull
    public static Supplier<ProjectSpaceConfiguration> DEFAULT_CONFIG = () -> {
        final ProjectSpaceConfiguration config = new ProjectSpaceConfiguration();
        //configure ProjectSpaceProperties
        config.defineProjectSpaceProperty(FilenameFormatter.PSProperty.class, new FilenameFormatter.PSPropertySerializer());
        config.defineProjectSpaceProperty(CompressionFormat.class, new CompressionFormat.Serializer());
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
        config.defineProjectSpaceProperty(FingerIdDataProperty.class, new FingerIdDataSerializer());
        config.registerComponent(FormulaResult.class, FingerprintResult.class, new FingerprintSerializer());
        config.registerComponent(FormulaResult.class, FBCandidates.class, new FBCandidatesSerializer());
        config.registerComponent(FormulaResult.class, FBCandidateFingerprints.class, new FBCandidateFingerprintSerializer());
        //canopus
        config.defineProjectSpaceProperty(CanopusCfDataProperty.class, new CanopusCfDataProperty.Serializer());
        config.defineProjectSpaceProperty(CanopusNpcDataProperty.class, new CanopusNpcDataProperty.Serializer());
        config.registerComponent(FormulaResult.class, CanopusResult.class, new CanopusSerializer());

        config.registerComponent(CompoundContainer.class, ConnectionTable.class, new ConnectionTableSerializer());
        config.registerComponent(CompoundContainer.class, LCMSPeakInformation.class, new LCMSPeakSerializer());

        return config;
    };

    private final SiriusProjectSpace space;
    public final Function<Ms2Experiment, String> nameFormatter;
    public final BiFunction<Integer, String, String> namingScheme;
    private Predicate<CompoundContainerId> compoundIdFilter;
    protected final InstanceFactory<I> instFac;


    public ProjectSpaceManager(@NotNull SiriusProjectSpace space, @NotNull InstanceFactory<I> factory, @Nullable Function<Ms2Experiment, String> formatter) {
        this.space = space;
        this.instFac = factory;
        this.nameFormatter = space.getProjectSpaceProperty(FilenameFormatter.PSProperty.class)
                .map(p -> (Function<Ms2Experiment, String>) new StandardMSFilenameFormatter(p.formatExpression))
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
    public I newCompoundWithUniqueId(Ms2Experiment inputExperiment) {
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
    private I newInstanceFromCompound(CompoundContainerId id, Class<? extends DataAnnotation>... components) {
        try {
            CompoundContainer c = projectSpace().getCompound(id, components);
            return instFac.create(c, this);
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Could not create read Input Experiment from Project Space.");
            throw new RuntimeException("Could not create read Input Experiment from Project Space.", e);
        }
    }

    private static final ReferenceMap instanceCache = new ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK, true);

    @SafeVarargs
    public final I getInstanceFromCompound(CompoundContainerId id, Class<? extends DataAnnotation>... components) {
        I instance;
        synchronized (instanceCache) {
             instance = (I) instanceCache.computeIfAbsent(id, i -> newInstanceFromCompound(id));
        }
        instance.loadCompoundContainer(components);
        return instance;
    }

    public final List<I> getInstancesFromCompounds(Collection<CompoundContainerId> ids, Class<? extends DataAnnotation>... components) {
        List<I> instances = new ArrayList<>(ids.size());
        synchronized (instanceCache) {
            instances = ids.stream().map(id -> (I) instanceCache.computeIfAbsent(id, i -> newInstanceFromCompound(id)))
                    .collect(Collectors.toList());
        }
        instances.forEach(i -> i.loadCompoundContainer(components));
        return instances;
    }


    public <T extends ProjectSpaceProperty> Optional<T> getProjectSpaceProperty(Class<T> key) {
        return projectSpace().getProjectSpaceProperty(key);
    }

    public <T extends ProjectSpaceProperty> T setProjectSpaceProperty(T value) {
        return setProjectSpaceProperty((Class<T>) value.getClass(), value);
    }

    public <T extends ProjectSpaceProperty> T setProjectSpaceProperty(Class<T> key, T value) {
        if (PosNegFpProperty.class.isAssignableFrom(key))
            synchronized (dataCompatibilityCache) {
                dataCompatibilityCache.remove(key);
                return projectSpace().setProjectSpaceProperty(key, value);
            }
        else
            return projectSpace().setProjectSpaceProperty(key, value);
    }

    public <T extends ProjectSpaceProperty> T deleteProjectSpaceProperty(Class<T> key) {
        if (PosNegFpProperty.class.isAssignableFrom(key))
            synchronized (dataCompatibilityCache) {
                dataCompatibilityCache.remove(key);
                return projectSpace().deleteProjectSpaceProperty(key);
            }
        else
            return projectSpace().deleteProjectSpaceProperty(key);
    }

    @NotNull
    public Iterator<I> filteredIterator(@Nullable Predicate<CompoundContainerId> cidFilter, @Nullable final Predicate<CompoundContainer> compoundFilter) {
        if (compoundFilter == null && cidFilter == null)
            return iterator();
        final Predicate<CompoundContainerId> cidF = (cidFilter != null && compoundFilter != null)
                ? (cid) -> cidFilter.test(cid) && this.compoundIdFilter.test(cid)
                : cidFilter == null ? this.compoundIdFilter : cidFilter;
        return makeInstanceIterator(space.filteredCompoundIterator(cidF, compoundFilter, Ms2Experiment.class));
    }


    @NotNull
    @Override
    public Iterator<I> iterator() {
        return instanceIterator();
    }

    public Iterator<I> instanceIterator(Class<? extends DataAnnotation>... c) {
        if (compoundIdFilter != null)
            return filteredIterator(compoundIdFilter, null);
        return makeInstanceIterator(space.compoundIterator(c));
    }

    private Iterator<I> makeInstanceIterator(@NotNull final Iterator<CompoundContainer> compoundIt) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return compoundIt.hasNext();
            }

            @Override
            public I next() {
                final CompoundContainer c = compoundIt.next();
                if (c == null) return null;
                return instFac.create(c, ProjectSpaceManager.this);
            }
        };
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

    public void writeSummaries(@Nullable Path summaryLocation, @Nullable Collection<CompoundContainerId> inclusionList, @NotNull Summarizer... summarizers) throws ExecutionException {
        if (summaryLocation == null)
            writeSummaries(null, false, inclusionList, summarizers);
        else
            writeSummaries(summaryLocation, summaryLocation.toString().endsWith(".zip"), inclusionList, summarizers);
    }

    public void writeSummaries(@Nullable Path summaryLocation, boolean compressed, @Nullable Collection<CompoundContainerId> inclusionList, @NotNull Summarizer... summarizers) throws ExecutionException {
        space.writeSummaries(summaryLocation, compressed, inclusionList, summarizers);
    }

    public void close() throws IOException {
        space.close();
    }

    private final Map<Class<? extends PosNegFpProperty<?, ?>>, Boolean> dataCompatibilityCache = new HashMap<>();

    /**
     * This checks whether the data files are compatible with them on the server. Since have had versions of the PS with
     * incomplete data files it also loads missing files from the server but only if the existing ones are compatible.
     * <p>
     * Results are cached!
     *
     * @param interrupted Tell the waiting job how it can check if it was interrupted
     * @return true if data files are  NOT incompatible with the Server version (compatible or not existent)
     * @throws TimeoutException     if server request times out
     * @throws InterruptedException if waiting for server request is interrupted
     */
    public boolean checkAndFixDataFiles(NetUtils.InterruptionCheck interrupted) throws TimeoutException, InterruptedException {
        if (PropertyManager.getBoolean("de.unijena.bioinf.sirius.project-check", null, false))
            return true;

        synchronized (dataCompatibilityCache) {
            try {

                checkFingerprintData(FingerIdDataProperty.class, FingerIdData.class, ApplicationCore.WEB_API::getFingerIdData, interrupted);
                checkFingerprintData(CanopusCfDataProperty.class, CanopusCfData.class, ApplicationCore.WEB_API::getCanopusCfData, interrupted);
                checkFingerprintData(CanopusNpcDataProperty.class, CanopusNpcData.class, ApplicationCore.WEB_API::getCanopusNpcData, interrupted);

                return dataCompatibilityCache.values().stream().reduce((a, b) -> a && b).orElse(true);
            } catch (Exception e) {
                dataCompatibilityCache.clear();
                LoggerFactory.getLogger(getClass()).warn("Could not retrieve FingerprintData from server! \n" + e.getMessage());
                throw e;
            }
        }
    }

    private <F extends FingerprintVersion, D extends FingerprintData<F>, P extends PosNegFpProperty<F, D>> void checkFingerprintData(
            Class<P> propClz, Class<D> propDataClz, IOFunctions.IOFunction<PredictorType, D> dataLoader, NetUtils.InterruptionCheck interrupted)
            throws InterruptedException, TimeoutException {
        try {
            if (!dataCompatibilityCache.containsKey(propClz)) {
                final P cd = getProjectSpaceProperty(propClz).orElse(null);
                if (cd != null) {
                    dataCompatibilityCache.put(propClz, true);
                    final D pos = NetUtils.tryAndWait(() -> dataLoader.apply(PredictorType.CSI_FINGERID_POSITIVE), interrupted);
                    final D neg = NetUtils.tryAndWait(() -> dataLoader.apply(PredictorType.CSI_FINGERID_NEGATIVE), interrupted);
                    if (cd.getPositive() != null) {
                        if (!cd.getPositive().identical(pos)) {
                            dataCompatibilityCache.put(propClz, false);
                        } else if (cd.getNegative() == null) {
                            LoggerFactory.getLogger(InstanceImporter.class).warn("Negative '" + propDataClz.getName() + "' file missing in project. Try to repair by reloading from webservice.");

                            setProjectSpaceProperty(propClz,
                                    propClz.getConstructor(propDataClz, propDataClz).newInstance(cd.getPositive(), neg));
                        }
                    }

                    if (cd.getNegative() != null) {
                        if (!cd.getNegative().identical(neg)) {
                            dataCompatibilityCache.put(propClz, false);
                        } else if (cd.getPositive() == null) {
                            LoggerFactory.getLogger(InstanceImporter.class).warn("Positive '" + propDataClz.getName() + "' file missing in project. Try to repair by reloading from webservice.");
                            setProjectSpaceProperty(propClz,
                                    propClz.getConstructor(propDataClz, propDataClz).newInstance(neg, cd.getNegative()));
                        }
                    }
                }
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException("Error during java reflection Object instantiation of '" + propClz.getName() + "'.", e);
        }
    }

    //region static helper
    public static Summarizer[] defaultSummarizer() {
        return new Summarizer[]{
                new FormulaSummaryWriter(),
                new StructureSummaryWriter(),
                new CanopusSummaryWriter(),
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


    public static ProjectSpaceConfiguration newDefaultConfig() {
        return DEFAULT_CONFIG.get();
    }
    //end region
}
