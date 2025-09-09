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
import de.unijena.bioinf.ChemistryBase.ms.Quantification;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.canopus.CanopusCfDataProperty;
import de.unijena.bioinf.projectspace.canopus.CanopusNpcDataProperty;
import de.unijena.bioinf.projectspace.fingerid.FingerIdDataProperty;
import de.unijena.bioinf.projectspace.summaries.CanopusSummaryWriter;
import de.unijena.bioinf.projectspace.summaries.FormulaSummaryWriter;
import de.unijena.bioinf.projectspace.summaries.StructureSummaryWriter;
import de.unijena.bioinf.projectspace.summaries.mztab.MztabMExporter;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.sirius.scores.IsotopeScore;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import de.unijena.bioinf.sirius.scores.TreeScore;
import org.apache.commons.collections4.map.AbstractReferenceMap;
import org.apache.commons.collections4.map.ReferenceMap;
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
import java.util.stream.Collectors;

/**
 * Manage the project space.
 * e.g. iteration on Instance level.
 * maybe some type of caching?
 */
public class SiriusProjectSpaceManager extends AbstractProjectSpaceManager {
    private final SiriusProjectSpace space;
    private final Function<Ms2Experiment, String> nameFormatter;
    private final BiFunction<Integer, String, String> namingScheme;


    public SiriusProjectSpaceManager(@NotNull SiriusProjectSpace space, @Nullable Function<Ms2Experiment, String> formatter) {
        this.space = space;
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

    public SiriusProjectSpace getProjectSpaceImpl() {
        return space;
    }


    @Override
    @NotNull
    public SiriusProjectSpaceInstance importInstanceWithUniqueId(Ms2Experiment inputExperiment) {
        return importInstanceWithUniqueId(inputExperiment, true);
    }
    public SiriusProjectSpaceInstance importInstanceWithUniqueId(Ms2Experiment inputExperiment, boolean importLcMsInfo) {
        final String name = nameFormatter.apply(inputExperiment);
        final CompoundContainer container = getProjectSpaceImpl().newCompoundWithUniqueId(name, (idx) -> namingScheme.apply(idx, name), inputExperiment).orElseThrow(() -> new RuntimeException("Could not create an project space ID for the Instance"));

        final SiriusProjectSpaceInstance inst = new SiriusProjectSpaceInstance(container, this);
        {
            // store LC/MS data into project space
            // might change in future. Its important that the trace is written after
            // importing from mzml/mzxml
            if (importLcMsInfo && inputExperiment != null) {
                LCMSPeakInformation lcmsInfo = inputExperiment.getAnnotation(LCMSPeakInformation.class, LCMSPeakInformation::empty);
                if (lcmsInfo.isEmpty()) {
                    // check if there are quantification information
                    // grab them and remove them
                    final Quantification quant = inputExperiment.getAnnotationOrNull(Quantification.class);
                    if (quant != null) {
                        lcmsInfo = new LCMSPeakInformation(quant.asQuantificationTable());
                        inputExperiment.removeAnnotation(Quantification.class);
                    }
                }

                if (!lcmsInfo.isEmpty()) {
                    // store this information into the compound container instead
                    final CompoundContainer compoundContainer = inst.loadCompoundContainer(LCMSPeakInformation.class);
                    final Optional<LCMSPeakInformation> annotation = compoundContainer.getAnnotation(LCMSPeakInformation.class);
                    if (annotation.orElseGet(LCMSPeakInformation::empty).isEmpty()) {
                        compoundContainer.setAnnotation(LCMSPeakInformation.class, lcmsInfo);
                        inst.updateCompound(compoundContainer, LCMSPeakInformation.class);
                    }
                }
                // remove annotation from experiment
                {
                    inputExperiment.removeAnnotation(LCMSPeakInformation.class);
                    inputExperiment.removeAnnotation(Quantification.class);
                    inst.updateExperiment();
                }
            }
        }

        return inst;
    }

    @SafeVarargs
    private SiriusProjectSpaceInstance newInstanceFromCompound(CompoundContainerId id, Class<? extends DataAnnotation>... components) {
        try {
            CompoundContainer c = getProjectSpaceImpl().getCompound(id, components);
            return new SiriusProjectSpaceInstance(c, this);
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Could not create read Input Experiment from Project Space.");
            throw new RuntimeException("Could not create read Input Experiment from Project Space.", e);
        }
    }

    private static final ReferenceMap<CompoundContainerId, SiriusProjectSpaceInstance> instanceCache = new ReferenceMap<>(AbstractReferenceMap.ReferenceStrength.HARD, AbstractReferenceMap.ReferenceStrength.WEAK, true);

    @SafeVarargs
    @Deprecated
    public final SiriusProjectSpaceInstance getInstanceFromCompound(CompoundContainerId id, Class<? extends DataAnnotation>... components) {
        SiriusProjectSpaceInstance instance;
        synchronized (instanceCache) {
            instance = instanceCache.computeIfAbsent(id, i -> newInstanceFromCompound(id));
        }
        instance.loadCompoundContainer(components);
        return instance;
    }

    @Override
    public final @NotNull Optional<SiriusProjectSpaceInstance> findInstance(Object id) {
        if (id instanceof String strId)
            return findInstance(strId);
        return findInstance(id.toString());

    }

    public final @NotNull Optional<SiriusProjectSpaceInstance> findInstance(String id) {
        return getProjectSpaceImpl().findCompound(id).map(this::getInstanceFromCompound);
    }

    private List<SiriusProjectSpaceInstance> getInstancesFromCompounds(Collection<CompoundContainerId> ids, Class<? extends DataAnnotation>... components) {
        List<SiriusProjectSpaceInstance> instances;
        synchronized (instanceCache) {
            instances = ids.stream().map(id -> instanceCache.computeIfAbsent(id, i -> newInstanceFromCompound(id)))
                    .collect(Collectors.toList());
        }
        instances.forEach(i -> i.loadCompoundContainer(components));
        return instances;
    }

    @Override
    public void writeFingerIdData(@NotNull FingerIdData pos, @NotNull FingerIdData neg) {
        setProjectSpaceProperty(FingerIdDataProperty.class, new FingerIdDataProperty(pos, neg));
    }

    @Override
    public void writeCanopusData(@NotNull CanopusCfData cfPos, @NotNull CanopusCfData cfNeg, @NotNull CanopusNpcData npcPos, @NotNull CanopusNpcData npcNeg) {
        setProjectSpaceProperty(new CanopusCfDataProperty(cfPos, cfNeg));
        setProjectSpaceProperty(new CanopusNpcDataProperty(npcPos, npcNeg));
    }

    @Override
    public void deleteFingerprintData() {
        deleteProjectSpaceProperty(FingerIdDataProperty.class);
        deleteProjectSpaceProperty(CanopusCfDataProperty.class);
        deleteProjectSpaceProperty(CanopusNpcDataProperty.class);
    }

    @Override
    public @NotNull Optional<FingerIdData> getFingerIdData(int charge) {
        return getProjectSpaceProperty(FingerIdDataProperty.class).map(fp -> fp.getByCharge(charge));
    }

    @Override
    public @NotNull Optional<CanopusCfData> getCanopusCfData(int charge) {
        return getProjectSpaceProperty(CanopusCfDataProperty.class).map(fp -> fp.getByCharge(charge));

    }

    @Override
    public @NotNull Optional<CanopusNpcData> getCanopusNpcData(int charge) {
        return getProjectSpaceProperty(CanopusNpcDataProperty.class).map(fp -> fp.getByCharge(charge));
    }

    private <T extends ProjectSpaceProperty> Optional<T> getProjectSpaceProperty(Class<T> key) {
        return getProjectSpaceImpl().getProjectSpaceProperty(key);
    }

    private <T extends ProjectSpaceProperty> T setProjectSpaceProperty(T value) {
        return setProjectSpaceProperty((Class<T>) value.getClass(), value);
    }

    private <T extends ProjectSpaceProperty> T setProjectSpaceProperty(Class<T> key, T value) {
        if (PosNegFpProperty.class.isAssignableFrom(key))
            synchronized (dataCompatibilityCache) {
                dataCompatibilityCache.remove(key);
                return getProjectSpaceImpl().setProjectSpaceProperty(key, value);
            }
        else
            return getProjectSpaceImpl().setProjectSpaceProperty(key, value);
    }

    private <T extends ProjectSpaceProperty> T deleteProjectSpaceProperty(Class<T> key) {
        if (PosNegFpProperty.class.isAssignableFrom(key))
            synchronized (dataCompatibilityCache) {
                dataCompatibilityCache.remove(key);
                return getProjectSpaceImpl().deleteProjectSpaceProperty(key);
            }
        else
            return getProjectSpaceImpl().deleteProjectSpaceProperty(key);
    }

    @NotNull
    public Iterator<Instance> filteredIterator(@Nullable Predicate<CompoundContainerId> cidFilter, @Nullable final Predicate<CompoundContainer> compoundFilter) {
        if (compoundFilter == null && cidFilter == null)
            return iterator();
        return makeInstanceIterator(space.filteredCompoundIterator(cidFilter, compoundFilter, Ms2Experiment.class));
    }


    @NotNull
    @Override
    public Iterator<Instance> iterator() {
        return instanceIterator();
    }

    @SafeVarargs
    public final Iterator<Instance> instanceIterator(Class<? extends DataAnnotation>... c) {
        return makeInstanceIterator(space.compoundIterator(c));
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
                return new SiriusProjectSpaceInstance(c, SiriusProjectSpaceManager.this);
            }
        };
    }

    @Override
    public int size() {
        return space.size();
    }

    @Override
    public int countAllFeatures() {
        return space.size();
    }

    @Override
    public int countAllCompounds() {
        int count = (int) space.stream().map(CompoundContainerId::getGroupId).distinct().filter(Objects::nonNull).count();
        return (int) (count + space.stream().filter(Objects::isNull).count());
    }

    @Override
    public long sizeInBytes() {
        return FileUtils.getFolderSizeOrThrow(space.getLocation());
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

    @Override
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
    @Override
    public boolean checkAndFixDataFiles(NetUtils.InterruptionCheck interrupted) throws TimeoutException, InterruptedException {
        if (PropertyManager.getBoolean("de.unijena.bioinf.sirius.project-check", null, false))
            return true;

        synchronized (dataCompatibilityCache) {
            try {

                checkFingerprintData(FingerIdDataProperty.class, FingerIdData.class, ApplicationCore.WEB_API()::getFingerIdData, interrupted);
                checkFingerprintData(CanopusCfDataProperty.class, CanopusCfData.class, ApplicationCore.WEB_API()::getCanopusCfData, interrupted);
                checkFingerprintData(CanopusNpcDataProperty.class, CanopusNpcData.class, ApplicationCore.WEB_API()::getCanopusNpcData, interrupted);

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

    @Override
    public String getName() {
        return getProjectSpaceImpl().getLocation().getFileName().toString();
    }

    @Override
    public String getLocation() {
        return getProjectSpaceImpl().getLocation().toAbsolutePath().toString();
    }

    @Override
    public void flush() throws IOException {
        getProjectSpaceImpl().flush();
    }

    @Override
    public void compact() {
        throw new UnsupportedOperationException("Compacting is not supported.");
    }

    //region static helper
    public static Summarizer[] defaultSummarizer(boolean writeTopHitGlobal, boolean writeTopHitWithAdductsGlobal, boolean writeFullGlobal) {
        return new Summarizer[]{
                new FormulaSummaryWriter(writeTopHitGlobal, writeTopHitWithAdductsGlobal, writeFullGlobal),
                new StructureSummaryWriter(writeTopHitGlobal, writeTopHitWithAdductsGlobal, writeFullGlobal),
                new CanopusSummaryWriter(writeTopHitGlobal, writeTopHitWithAdductsGlobal, writeFullGlobal),
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
    //end region
}
