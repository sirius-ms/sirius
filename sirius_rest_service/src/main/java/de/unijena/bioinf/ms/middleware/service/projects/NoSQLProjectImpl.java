/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ChemistryBase.chem.Charge;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.ms.middleware.controller.mixins.TagController;
import de.unijena.bioinf.ms.middleware.model.annotations.CanopusPrediction;
import de.unijena.bioinf.ms.middleware.model.annotations.FormulaCandidate;
import de.unijena.bioinf.ms.middleware.model.annotations.*;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.compute.InstrumentProfile;
import de.unijena.bioinf.ms.middleware.model.features.*;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.middleware.model.spectra.Spectrums;
import de.unijena.bioinf.ms.middleware.model.statistics.FoldChange;
import de.unijena.bioinf.ms.middleware.model.statistics.StatisticsTable;
import de.unijena.bioinf.ms.middleware.model.tags.Tag;
import de.unijena.bioinf.ms.middleware.model.tags.TagCategory;
import de.unijena.bioinf.ms.middleware.model.tags.TagCategoryImport;
import de.unijena.bioinf.ms.middleware.model.tags.TagGroup;
import de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils;
import de.unijena.bioinf.ms.middleware.service.lucene.LuceneUtils;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.feature.*;
import de.unijena.bioinf.ms.persistence.model.core.run.InstrumentConfig;
import de.unijena.bioinf.ms.persistence.model.core.networks.AdductNetwork;
import de.unijena.bioinf.ms.persistence.model.core.networks.AdductNode;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.RetentionTimeAxis;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantificationMeasure;
import de.unijena.bioinf.ms.persistence.model.core.trace.*;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectType;
import de.unijena.bioinf.ms.persistence.model.sirius.*;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.StorageUtils;
import de.unijena.bioinf.ms.persistence.storage.exceptions.ProjectTypeException;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.*;
import jakarta.persistence.Id;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.similarity.LongestCommonSubsequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.*;


public class NoSQLProjectImpl implements Project<NoSQLProjectSpaceManager> {
    @NotNull
    private final String projectId;

    @NotNull
    private final NoSQLProjectSpaceManager projectSpaceManager;

    private final @NotNull BiFunction<Project<?>, String, Boolean> computeStateProvider;

    @SneakyThrows
    public NoSQLProjectImpl(@NotNull String projectId, @NotNull NoSQLProjectSpaceManager projectSpaceManager, @NotNull BiFunction<Project<?>, String, Boolean> computeStateProvider) {
        this.projectId = projectId;
        this.projectSpaceManager = projectSpaceManager;
        this.computeStateProvider = computeStateProvider;
    }

    //using private methods instead of references for easier refactoring or changes.
    // compiler will inline the method call since projectmanager is final.
    private Database<?> storage() {
        return projectSpaceManager.getProject().getStorage();
    }

    private SiriusProjectDocumentDatabase<? extends Database<?>> project() {
        return projectSpaceManager.getProject();
    }

    @Override
    public @NotNull String getProjectId() {
        return projectId;
    }

    @Override
    public @NotNull NoSQLProjectSpaceManager getProjectSpaceManager() {
        return projectSpaceManager;
    }

    @SneakyThrows
    @Override
    public Optional<QuantificationTable> getQuantification(QuantificationMeasure type, QuantificationTable.RowType rowType) {
        Optional<QuantificationTable> table = initQuantTable(type, rowType);
        if (table.isEmpty())
            return Optional.empty();

        List<double[]> values = new ArrayList<>();
        LongList rowIds = new LongArrayList();
        List<String> rowNames = new ArrayList<>();

        if (rowType == QuantificationTable.RowType.FEATURES) {
            storage().findAllStr(AlignedFeatures.class).forEach(alignedFeatures -> addToTable(alignedFeatures, values, rowIds, rowNames, table.get()));
        } else {
            storage().findAllStr(de.unijena.bioinf.ms.persistence.model.core.Compound.class).forEach(compound -> addToTable(compound, values, rowIds, rowNames, table.get()));
        }

        table.get().setValues(values.toArray(double[][]::new));
        table.get().setRowIds(rowIds.toLongArray());
        table.get().setRowNames(rowNames.toArray(String[]::new));

        return table;
    }

    @SneakyThrows
    @Override
    public Optional<QuantificationTable> getQuantificationForAlignedFeatureOrCompound(String objectId, QuantificationMeasure type, QuantificationTable.RowType rowType) {
        Optional<QuantificationTable> table = initQuantTable(type, rowType);
        if (table.isEmpty())
            return Optional.empty();

        List<double[]> values = new ArrayList<>();
        LongList rowIds = new LongArrayList();
        List<String> rowNames = new ArrayList<>();

        if (rowType == QuantificationTable.RowType.FEATURES) {
            Optional<AlignedFeatures> alignedFeature = storage().getByPrimaryKey(Long.parseLong(objectId), AlignedFeatures.class);
            if (alignedFeature.isEmpty())
                return Optional.empty();

            addToTable(alignedFeature.get(), values, rowIds, rowNames, table.get());
        } else { //must be COMPOUND
            Optional<de.unijena.bioinf.ms.persistence.model.core.Compound> compound = storage().getByPrimaryKey(Long.parseLong(objectId), de.unijena.bioinf.ms.persistence.model.core.Compound.class);
            if (compound.isEmpty())
                return Optional.empty();

            addToTable(compound.get(), values, rowIds, rowNames, table.get());
        }

        table.get().setValues(values.toArray(double[][]::new));
        table.get().setRowIds(rowIds.toLongArray());
        table.get().setRowNames(rowNames.toArray(String[]::new));

        return table;
    }

    private Optional<QuantificationTable> initQuantTable(QuantificationMeasure type, QuantificationTable.RowType rowType) throws IOException {
        List<LCMSRun> runs = storage().findAllStr(LCMSRun.class, "runId", Database.SortOrder.ASCENDING).toList();

        if (runs.isEmpty())
            return Optional.empty();

        long[] runIds = new long[runs.size()];
        String[] runNames = new String[runs.size()];
        for (int i = 0; i < runs.size(); i++) {
            runIds[i] = runs.get(i).getRunId();
            runNames[i] = runs.get(i).getName();
        }

        return Optional.of(QuantificationTable
                .builder()
                .rowType(rowType)
                .quantificationMeasure(type)
                .columnType(QuantificationTable.ColumnType.SAMPLES)
                .columnIds(runIds)
                .columnNames(runNames)
                .build()
        );
    }

    @SneakyThrows
    private <T> void addToTable(T parent, List<double[]> values, LongList rowIds, List<String> rowNames, QuantificationTable table) {
        Long2ObjectMap<List<Feature>> features = new Long2ObjectOpenHashMap<>();
        if (parent instanceof AlignedFeatures alignedFeature) {
            rowIds.add(alignedFeature.getAlignedFeatureId());
            rowNames.add(alignedFeature.getName());

            storage().findStr(Filter.where("alignedFeatureId").eq(alignedFeature.getAlignedFeatureId()), Feature.class).forEach(feature -> {
                features.put(feature.getRunId(), List.of(feature));
            });
        } else if (parent instanceof de.unijena.bioinf.ms.persistence.model.core.Compound compound) {
            rowIds.add(compound.getCompoundId());
            rowNames.add(compound.getName());

            storage().findStr(Filter.where("compoundId").eq(compound.getCompoundId()), AlignedFeature.class).forEach(alignedFeature -> {
                try {
                    storage().findStr(Filter.where("alignedFeatureId").eq(alignedFeature.getAlignedFeatureId()), Feature.class).forEach(feature -> {
                        if (!features.containsKey(feature.getRunId())) {
                            features.put(feature.getRunId(), new ArrayList<>());
                        }
                        features.get(feature.getRunId()).add(feature);
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        values.add(getQuantTableRow(features, table));
    }

    private double[] getQuantTableRow(Long2ObjectMap<List<Feature>> features, QuantificationTable table) {
        double[] row = new double[table.getColumnIds().length];
        for (int i = 0; i < row.length; i++) {
            if (features.containsKey(table.getColumnIds()[i])) {
                row[i] = switch (table.getQuantificationMeasure()) {
                    case APEX_INTENSITY -> features.get(table.getColumnIds()[i]).stream().mapToDouble(Feature::getApexIntensity).average().orElse(Double.NaN);
                    case AREA_UNDER_CURVE -> features.get(table.getColumnIds()[i]).stream().mapToDouble(Feature::getAreaUnderCurve).average().orElse(Double.NaN);
                };
            } else {
                row[i] = Double.NaN;
            }
        }
        return row;
    }

    @Override
    @SneakyThrows
    public Optional<TraceSet> getTraceSetForAlignedFeature(String alignedFeatureId, boolean includeAll) {
        if (includeAll) return getCompleteTraceSetForAlignedFeature(alignedFeatureId);
        Database<?> storage = storage();
        Optional<AlignedFeatures> maybeFeature = storage.getByPrimaryKey(Long.parseLong(alignedFeatureId), AlignedFeatures.class);
        if (maybeFeature.isEmpty()) return Optional.empty();
        AlignedFeatures feature = maybeFeature.get();
        storage.fetchAllChildren(feature, "alignedFeatureId", "features", Feature.class);
        project().fetchMsData(feature);
        // only use features with LC/MS information
        List<Feature> features = feature.getFeatures().stream().flatMap(List::stream).filter(x -> x.getApexIntensity() != null).toList();

        List<LCMSRun> samples = new ArrayList<>();
        for (int k = 0; k < features.size(); ++k) {
            samples.add(storage.getByPrimaryKey(features.get(k).getRunId(), LCMSRun.class).orElse(null));
            storage.fetchChild(samples.get(k), "runId", "retentionTimeAxis", RetentionTimeAxis.class);
        }

        MergedLCMSRun merged = storage.getByPrimaryKey(feature.getRunId(), MergedLCMSRun.class).orElse(null);
        if (merged == null) return Optional.empty();
        storage.fetchChild(merged, "runId", "retentionTimeAxis", RetentionTimeAxis.class);
        if (merged.getRetentionTimeAxis().isEmpty()) return Optional.empty();
        RetentionTimeAxis mergedAxis = merged.getRetentionTimeAxis().get();
        TraceSet traceSet = new TraceSet();

        TraceRef ref = feature.getTraceRef();
        Optional<MergedTrace> maybeMergedTrace = storage.getByPrimaryKey(ref.getTraceId(), MergedTrace.class);
        if (maybeMergedTrace.isEmpty()) return Optional.empty();
        MergedTrace mergedTrace = maybeMergedTrace.get();

        Long2ObjectOpenHashMap<IntArrayList> ms2annotations = new Long2ObjectOpenHashMap<>();

        feature.getMSData().ifPresent(x -> {
            if (x.getMsnSpectra() != null) {
                for (MergedMSnSpectrum spec : x.getMsnSpectra()) {
                    long[] sampleIds = spec.getSampleIds();
                    int[][] scanIds = spec.getProjectedPrecursorScanIds();
                    for (int i = 0; i < sampleIds.length; ++i) {
                        ms2annotations.computeIfAbsent(sampleIds[i], (q) -> new IntArrayList()).addAll(IntList.of(scanIds[i]));
                    }
                }
            }
        });


        int firstTraceId = mergedTrace.getScanIndexOffset();
        List<TraceSet.Trace> traces = new ArrayList<>();
        {
            // add merged trace
            TraceSet.Trace mergedtrace = new TraceSet.Trace();
            mergedtrace.setMz(feature.getAverageMass());
            mergedtrace.setId(String.valueOf(feature.getAlignedFeatureId()));
            mergedtrace.setSampleId(String.valueOf(merged.getRunId()));
            mergedtrace.setSampleName(merged.getName());
            mergedtrace.setLabel(merged.getName());
            mergedtrace.setNormalizationFactor(1d);
            mergedtrace.setAnnotations(new TraceSet.Annotation[]{new TraceSet.Annotation(TraceSet.AnnotationType.FEATURE, "",
                    feature.getTraceRef().getApex(), feature.getTraceRef().getStart(), feature.getTraceRef().getEnd())});
            mergedtrace.setMerged(true);
            mergedtrace.setIntensities(mergedTrace.getIntensities().doubleStream().toArray());
            mergedtrace.setNoiseLevel((double) (mergedAxis.getNoiseLevelPerScan()[feature.getTraceRef().getScanIndexOffsetOfTrace() + feature.getTraceRef().getApex()]));
            traces.add(mergedtrace);
        }

        for (int k = 0; k < features.size(); ++k) {
            Optional<RawTraceRef> traceReference = features.get(k).getTraceReference();
            if (traceReference.isPresent()) {
                RawTraceRef r = traceReference.get();
                Optional<SourceTrace> sourceTrace = storage.getByPrimaryKey(r.getTraceId(), SourceTrace.class);
                if (sourceTrace.isPresent()) {
                    // remap trace
                    FloatList intensities = sourceTrace.get().getIntensities();
                    int offset = sourceTrace.get().getScanIndexOffset();

                    int len = intensities.size();
                    int startIdx = 0, shift = 0;
                    // this should never happen. Just in case the single trace appears before the merged trace
                    // we cut it of
                    if (offset < firstTraceId) {
                        startIdx = firstTraceId - offset;
                        shift = 0;
                        len -= startIdx;
                    }

                    // this might happen from time to time
                    if (offset > firstTraceId) {
                        startIdx = 0;
                        shift = offset - firstTraceId;
                        len += shift;
                    }

                    double[] vec = new double[len];
                    for (int i = startIdx; i < intensities.size(); ++i) {
                        vec[i + shift] = intensities.getFloat(i);
                    }

                    TraceSet.Trace trace = new TraceSet.Trace();
                    trace.setId(String.valueOf(features.get(k).getFeatureId()));
                    trace.setSampleId(String.valueOf(features.get(k).getRunId()));
                    trace.setSampleName(samples.get(k) == null ? "unknown" : samples.get(k).getName());

                    trace.setIntensities(vec);
                    trace.setLabel(trace.getSampleName());
                    trace.setMz(features.get(k).getAverageMass());

                    // add annotations
                    ArrayList<TraceSet.Annotation> annotations = new ArrayList<>();
                    // feature annotation
                    annotations.add(new TraceSet.Annotation(TraceSet.AnnotationType.FEATURE, "",
                            r.getApex() + shift, r.getStart() + shift, r.getEnd() + shift));

                    // ms2 annotations
                    IntArrayList scanIds = ms2annotations.get(features.get(k).getRunId());
                    if (scanIds != null) {
                        for (int id : scanIds) {
                            annotations.add(new TraceSet.Annotation(TraceSet.AnnotationType.MS2, "",
                                    id - r.getScanIndexOffsetOfTrace() + shift));

                        }
                    }
                    trace.setAnnotations(annotations.toArray(TraceSet.Annotation[]::new));
                    RetentionTimeAxis axis = samples.get(k).getRetentionTimeAxis().get();
                    trace.setNormalizationFactor(axis.getNormalizationFactor());
                    trace.setNoiseLevel((double) axis.getNoiseLevelPerScan()[r.getRawScanIndexOfset() + r.getRawApex()]);
                    traces.add(trace);
                }
            }
        }
        traceSet.setTraces(traces.toArray(TraceSet.Trace[]::new));

        TraceSet.Axes axes = new TraceSet.Axes();
        int traceTo = mergedTrace.getScanIndexOffset() + traces.stream().mapToInt(x -> x.getIntensities().length).max().orElse(0);
        /*
            Merged traces do not have scan numbers....
         */
        //axes.setScanNumber(Arrays.copyOfRange(mergedAxis.getScanNumbers(), firstTraceId, traceTo));
        //axes.setScanIds(Arrays.copyOfRange(mergedAxis.getScanIdentifiers(), firstTraceId, traceTo));
        traceTo = Math.max(traceTo, Math.min(mergedAxis.getRetentionTimes().length, firstTraceId + (int) Math.ceil(merged.getSampleStats().getMedianPeakWidthInSeconds() * 4 / (mergedAxis.getRetentionTimes()[1] - mergedAxis.getRetentionTimes()[0]))));
        axes.setRetentionTimeInSeconds(Arrays.copyOfRange(mergedAxis.getRetentionTimes(), firstTraceId, traceTo));
        traceSet.setAxes(axes);

        traceSet.setSampleName(merged.getName());
        traceSet.setSampleId(String.valueOf(merged.getRunId()));

        return Optional.of(traceSet);
    }


    @Override
    @SneakyThrows
    public Optional<TraceSet> getTraceSetsForFeatureWithCorrelatedIons(String alignedFeatureId) {
        Database<?> storage = storage();
        Optional<AlignedFeatures> maybeMainFeature = storage.getByPrimaryKey(Long.parseLong(alignedFeatureId), AlignedFeatures.class);
        if (maybeMainFeature.isEmpty()) return Optional.empty();
        AlignedFeatures mainFeature = maybeMainFeature.get();
        if (mainFeature.getTraceReference().isEmpty()) return Optional.empty(); // no trace information available
        Optional<MergedTrace> maybeMergedTrace = storage.getByPrimaryKey(mainFeature.getTraceRef().getTraceId(), MergedTrace.class);
        if (maybeMergedTrace.isEmpty()) return Optional.empty(); // no trace information available

        TraceSet traceSet = new TraceSet();
        IntArrayList offsets = new IntArrayList();
        MergedLCMSRun merged = storage.getByPrimaryKey(mainFeature.getRunId(), MergedLCMSRun.class).orElse(null);
        if (merged == null) return Optional.empty();
        storage.fetchChild(merged, "runId", "retentionTimeAxis", RetentionTimeAxis.class);
        if (merged.getRetentionTimeAxis().isEmpty()) return Optional.empty();
        RetentionTimeAxis retentionTimeAxis = merged.getRetentionTimeAxis().get();
        ArrayList<TraceSet.Trace> traces = new ArrayList<>();
        {
            traces.add(TraceSet.Trace.of("[MAIN]", merged, mainFeature, maybeMergedTrace.get(), retentionTimeAxis));
            offsets.add(maybeMergedTrace.get().getScanIndexOffset());
        }


        // also add isotopes
        {
            storage.fetchAllChildren(mainFeature, "alignedFeatureId", "isotopicFeatures", AlignedIsotopicFeatures.class);
            for (AlignedIsotopicFeatures g  : mainFeature.getIsotopicFeatures().orElse(Collections.emptyList())) {
                Optional<MergedTrace> isotopicTrace = Optional.empty();
                if (g.getTraceReference().isPresent()) {
                    isotopicTrace = storage.getByPrimaryKey(g.getTraceReference().get().getTraceId(), MergedTrace.class);
                }
                if (isotopicTrace.isPresent()) {
                    traces.add(TraceSet.Trace.of("[ISOTOPE]", merged, g, isotopicTrace.get(), retentionTimeAxis));
                    offsets.add(isotopicTrace.get().getScanIndexOffset());

                }
            }
        }
        LongOpenHashSet alreadyFetched = new LongOpenHashSet();
        alreadyFetched.add(maybeMergedTrace.get().getMergedTraceId());
        if (mainFeature.getAdductNetworkId()!=null) {
            Optional<AdductNetwork> maybeNetwork = storage.getByPrimaryKey(mainFeature.getAdductNetworkId(), AdductNetwork.class);
            if (maybeNetwork.isPresent()) {
                AdductNetwork network = maybeNetwork.get();
                for (AdductNode node : network.getNodes()) {
                    if (node.getAlignedFeatureId()==mainFeature.getAlignedFeatureId()) continue;
                    Optional<MergedTrace> tr = storage.getByPrimaryKey(node.getTraceId(), MergedTrace.class);
                    Optional<AlignedFeatures> fr =storage.getByPrimaryKey(node.getAlignedFeatureId(), AlignedFeatures.class);
                    if (tr.isPresent() && fr.isPresent()) {
                        traces.add(TraceSet.Trace.of(String.format(Locale.US, "[CORRELATED] m/z = %.4f", fr.get().getAverageMass()), merged, fr.get(), tr.get(), retentionTimeAxis));
                        offsets.add(tr.get().getScanIndexOffset());
                        storage.fetchAllChildren(fr.get(), "alignedFeatureId", "isotopicFeatures", AlignedIsotopicFeatures.class);
                        for (AlignedIsotopicFeatures g  : fr.get().getIsotopicFeatures().orElse(Collections.emptyList())) {
                            Optional<MergedTrace> isotopicTrace = Optional.empty();
                            if (g.getTraceReference().isPresent()) {
                                isotopicTrace = storage.getByPrimaryKey(g.getTraceReference().get().getTraceId(), MergedTrace.class);
                            }
                            if (isotopicTrace.isPresent()) {
                                traces.add(TraceSet.Trace.of("[CORRELATED][ISOTOPE]", merged, g, isotopicTrace.get(), retentionTimeAxis));
                                offsets.add(isotopicTrace.get().getScanIndexOffset());
                            }
                        }
                    }
                }
                traceSet.setAdductNetwork(de.unijena.bioinf.ms.middleware.model.networks.AdductNetwork.from(network));
            }
        }
        traceSet.setTraces(traces.toArray(TraceSet.Trace[]::new));
        traceSet.setSampleName(merged.getName());
        traceSet.setSampleId(String.valueOf(merged.getRunId()));
        traceSet.harmonizeTraces(retentionTimeAxis, offsets.toIntArray());
        return Optional.of(traceSet);
    }


    /**
     * This method wilreturn Optional.empty();l collect all aligned features belonging to the same traceset
     */
    @SneakyThrows
    public Optional<TraceSet> getCompleteTraceSetForAlignedFeature(String alignedFeatureId) {
        Database<?> storage = storage();
        Optional<AlignedFeatures> maybeFeature = storage.getByPrimaryKey(Long.parseLong(alignedFeatureId), AlignedFeatures.class);
        if (maybeFeature.isEmpty()) return Optional.empty();
        final AlignedFeatures mainFeature = maybeFeature.get();

        // now get the corresponding merged trace
        MergedLCMSRun merged = storage.getByPrimaryKey(mainFeature.getRunId(), MergedLCMSRun.class).orElse(null);
        if (merged == null) return Optional.empty();
        storage.fetchChild(merged, "runId", "retentionTimeAxis", RetentionTimeAxis.class);
        if (merged.getRetentionTimeAxis().isEmpty()) return Optional.empty();
        RetentionTimeAxis mergedAxis = merged.getRetentionTimeAxis().get();
        TraceSet traceSet = new TraceSet();

        TraceRef ref = mainFeature.getTraceRef();
        Optional<MergedTrace> maybeMergedTrace = storage.getByPrimaryKey(ref.getTraceId(), MergedTrace.class);
        if (maybeMergedTrace.isEmpty()) return Optional.empty();
        MergedTrace mergedTrace = maybeMergedTrace.get();

        // now collect ALL features belonging to this trace
        List<AlignedFeatures> allMergedFeatures = new ArrayList<>(storage.findStr(Filter.where("traceRef.traceId").eq(ref.getTraceId()), AlignedFeatures.class).toList());
        allMergedFeatures.removeIf(x -> x.getAlignedFeatureId() == mainFeature.getAlignedFeatureId());
        allMergedFeatures.addFirst(mainFeature);

        for (AlignedFeatures singleFeature : allMergedFeatures) {
            storage.fetchAllChildren(singleFeature, "alignedFeatureId", "features", Feature.class);
        }
        project().fetchMsData(mainFeature); // we only fetch ms data from main feature for now
        // only use features with LC/MS information
        //List<Feature> features = feature.getFeatures().stream().flatMap(List::stream).filter(x -> x.getApexIntensity() != null).toList();

        HashMap<Long, LCMSRun> samples = new HashMap<>();
        HashMap<Long, SourceTrace> sources = new HashMap<>();
        HashMap<Long, Set<Long>> sample2sources = new HashMap<>();

        HashMap<Long, List<Feature>> sample2Feature = new HashMap<>();
        for (int k = 0; k < allMergedFeatures.size(); ++k) {
            for (Feature sampleFeature : allMergedFeatures.get(k).getFeatures().orElse(Collections.emptyList())) {
                if (sampleFeature.getRunId() != null) {
                    sample2Feature.computeIfAbsent(sampleFeature.getRunId(), (x) -> new ArrayList<>()).add(sampleFeature);
                    samples.computeIfAbsent(sampleFeature.getRunId(), (key) -> {
                        try {
                            return storage.getByPrimaryKey(key, LCMSRun.class).orElse(null);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    Long tr = sampleFeature.getTraceReference().map(TraceRef::getTraceId).orElse(null);
                    if (tr != null) {
                        sources.computeIfAbsent(tr, (key) -> {
                            try {
                                return storage.getByPrimaryKey(key, SourceTrace.class).orElse(null);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        sample2sources.computeIfAbsent(sampleFeature.getRunId(), (key) -> new HashSet<>());
                        sample2sources.get(sampleFeature.getRunId()).add(tr);
                    }
                }
            }
        }
        for (LCMSRun r : samples.values()) {
            storage.fetchChild(r, "runId", "retentionTimeAxis", RetentionTimeAxis.class);
        }

        Long2ObjectOpenHashMap<IntArrayList> ms2annotations = new Long2ObjectOpenHashMap<>();

        mainFeature.getMSData().ifPresent(x -> {
            if (x.getMsnSpectra() != null) {
                for (MergedMSnSpectrum spec : x.getMsnSpectra()) {
                    long[] sampleIds = spec.getSampleIds();
                    int[][] scanIds = spec.getProjectedPrecursorScanIds();
                    for (int i = 0; i < sampleIds.length; ++i) {
                        ms2annotations.computeIfAbsent(sampleIds[i], (q) -> new IntArrayList()).addAll(IntList.of(scanIds[i]));
                    }
                }
            }
        });


        int firstTraceId = mergedTrace.getScanIndexOffset();
        List<TraceSet.Trace> traces = new ArrayList<>();
        {
            // add merged trace
            TraceSet.Trace mergedtrace = new TraceSet.Trace();
            mergedtrace.setMz(mainFeature.getAverageMass());
            mergedtrace.setId(String.valueOf(mainFeature.getAlignedFeatureId()));
            mergedtrace.setSampleId(String.valueOf(merged.getRunId()));
            mergedtrace.setSampleName(merged.getName());
            mergedtrace.setLabel(merged.getName());
            mergedtrace.setNormalizationFactor(1d);
            ArrayList<TraceSet.Annotation> anos = new ArrayList<>();
            for (AlignedFeatures features : allMergedFeatures) {
                String anoPrefix = features == mainFeature ? "[MAIN]" : "";
                anoPrefix += "[" + features.getDataQuality().name().toUpperCase() + "]";
                anos.add(new TraceSet.Annotation(TraceSet.AnnotationType.FEATURE, anoPrefix + features.getAlignedFeatureId(),
                        features.getTraceRef().getApex(), features.getTraceRef().getStart(), features.getTraceRef().getEnd()));
            }
            mergedtrace.setAnnotations(anos.toArray(TraceSet.Annotation[]::new));
            mergedtrace.setMerged(true);
            mergedtrace.setIntensities(mergedTrace.getIntensities().doubleStream().toArray());
            mergedtrace.setNoiseLevel((double) (mergedAxis.getNoiseLevelPerScan()[mainFeature.getTraceRef().getScanIndexOffsetOfTrace() + mainFeature.getTraceRef().getApex()]));
            traces.add(mergedtrace);
        }
        final TraceSet.Trace primaryTrace = traces.get(0);
        Long[] sampleKeys = samples.keySet().toArray(Long[]::new);

        for (long sampleKey : sampleKeys) {
            List<SourceTrace> sourceTraces = sample2sources.getOrDefault(sampleKey, Collections.emptySet()).stream().map(sources::get).toList();
            final double[] traceIntensities = new double[primaryTrace.getIntensities().length];
            for (SourceTrace t : sourceTraces) {
                if (sourceTraces.size() > 1) {
                    LoggerFactory.getLogger(NoSQLProjectImpl.class).warn("It is unusual to have two source traces for the same sample in the same merged trace...");
                }
                int offset = t.getScanIndexOffset() - mergedTrace.getScanIndexOffset();
                FloatList fl = t.getIntensities();
                for (int k = 0; k < fl.size(); ++k) {
                    final int targetLocation = offset + k;
                    if (targetLocation >= 0 && targetLocation < traceIntensities.length) {
                        traceIntensities[targetLocation] += fl.getFloat(k);
                    }
                }
            }

            TraceSet.Trace trace = new TraceSet.Trace();
            trace.setId("-1");
            trace.setSampleId(String.valueOf(sampleKey));
            trace.setSampleName(samples.get(sampleKey) == null ? "unknown" : samples.get(sampleKey).getName());

            trace.setIntensities(traceIntensities);
            trace.setLabel(trace.getSampleName());
            trace.setMz(sourceTraces.stream().mapToDouble(AbstractTrace::getAverageMz).average().orElse(mainFeature.getAverageMass()));

            // add annotations
            ArrayList<TraceSet.Annotation> annotations = new ArrayList<>();
            // feature annotation
            for (Feature features : sample2Feature.get(sampleKey)) {
                if (features.getTraceReference().isEmpty()) continue;
                RawTraceRef reference = features.getTraceReference().get();
                int apex = (reference.getApex() + reference.getScanIndexOffsetOfTrace()) - mergedTrace.getScanIndexOffset();
                int left = (reference.getStart() + reference.getScanIndexOffsetOfTrace()) - mergedTrace.getScanIndexOffset();
                int right = (reference.getEnd() + reference.getScanIndexOffsetOfTrace()) - mergedTrace.getScanIndexOffset();

                annotations.add(new TraceSet.Annotation(TraceSet.AnnotationType.FEATURE,
                        (features.getAlignedFeatureId() == mainFeature.getAlignedFeatureId()) ? "[MAIN]" + String.valueOf(features.getAlignedFeatureId()) : String.valueOf(features.getAlignedFeatureId()),
                        apex, left, right));
            }

            // ms2 annotations
            IntArrayList scanIds = ms2annotations.get(sampleKey);
            if (scanIds != null) {
                for (int id : scanIds) {
                    annotations.add(new TraceSet.Annotation(TraceSet.AnnotationType.MS2, "",
                            id - mergedTrace.getScanIndexOffset()));
                }
            }
            trace.setAnnotations(annotations.toArray(TraceSet.Annotation[]::new));
            RetentionTimeAxis axis = samples.get(sampleKey).getRetentionTimeAxis().get();
            trace.setNormalizationFactor(axis.getNormalizationFactor());
            trace.setNoiseLevel(/*(double) axis.getNoiseLevelPerScan()[mainFeature.getTraceRef().absoluteApexId()]*/0d); // this value makes no sense for projected anyways
            traces.add(trace);
        }
        traceSet.setTraces(traces.toArray(TraceSet.Trace[]::new));

        TraceSet.Axes axes = new TraceSet.Axes();
        int traceTo = mergedTrace.getScanIndexOffset() + traces.stream().mapToInt(x -> x.getIntensities().length).max().orElse(0);
        /*
            Merged traces do not have scan numbers....
         */
        //axes.setScanNumber(Arrays.copyOfRange(mergedAxis.getScanNumbers(), firstTraceId, traceTo));
        //axes.setScanIds(Arrays.copyOfRange(mergedAxis.getScanIdentifiers(), firstTraceId, traceTo));
        traceTo = Math.max(traceTo, Math.min(mergedAxis.getRetentionTimes().length, firstTraceId + (int) Math.ceil(merged.getSampleStats().getMedianPeakWidthInSeconds() * 4 / (mergedAxis.getRetentionTimes()[1] - mergedAxis.getRetentionTimes()[0]))));
        axes.setRetentionTimeInSeconds(Arrays.copyOfRange(mergedAxis.getRetentionTimes(), firstTraceId, traceTo));
        traceSet.setAxes(axes);

        traceSet.setSampleName(merged.getName());
        traceSet.setSampleId(String.valueOf(merged.getRunId()));

        return Optional.of(traceSet);
    }

    @Override
    @SneakyThrows
    public Optional<TraceSet> getTraceSetForCompound(String compoundId, Optional<String> currentFeatureId) {
        Database<?> storage = storage();
        Optional<de.unijena.bioinf.ms.persistence.model.core.Compound> maybeCompound = storage.getByPrimaryKey(Long.parseLong(compoundId), de.unijena.bioinf.ms.persistence.model.core.Compound.class);
        if (maybeCompound.isEmpty()) return Optional.empty();
        de.unijena.bioinf.ms.persistence.model.core.Compound compound = maybeCompound.get();
        storage.fetchAllChildren(compound, "compoundId", "adductFeatures", AlignedFeatures.class);
        ArrayList<AbstractAlignedFeatures> allFeatures = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        Long fid = currentFeatureId.map(Long::valueOf).orElse(null);
        for (AlignedFeatures f : compound.getAdductFeatures().stream().flatMap(Collection::stream).toList()) {
            if (f.getApexIntensity() == null) continue; // ignore features without lcms information
            String prefix = "[CORRELATED]";
            if (fid != null && fid == f.getAlignedFeatureId()) {
                prefix = "[SELECTED]";
            }
            String mainLabel;
            if (f.getDetectedAdducts() == null || f.getDetectedAdducts().getAllAdducts().isEmpty()) {
                mainLabel = prefix + " " + PrecursorIonType.unknown(f.getCharge());
            } else {
                mainLabel = prefix + " " + f.getDetectedAdducts().getAllAdducts().stream().sorted()
                        .map(PrecursorIonType::toString)
                        .map(s -> s.replaceAll("\\s+", ""))
                        .collect(Collectors.joining(" | "));
            }

            storage.fetchAllChildren(f, "alignedFeatureId", "isotopicFeatures", AlignedIsotopicFeatures.class);
            allFeatures.add(f);
            labels.add(mainLabel + String.format(Locale.US, " (%.2f m/z) ", f.getAverageMass()));
            List<AlignedIsotopicFeatures> isotopes = f.getIsotopicFeatures().orElse(new ArrayList<>()).stream().filter(x -> x.getApexIntensity() != null).sorted(Comparator.comparingDouble(AbstractFeature::getAverageMass)).toList();
            for (int k = 0; k < isotopes.size(); ++k) {
                allFeatures.add(isotopes.get(k));
                labels.add(mainLabel + String.format(Locale.US, " (%.2f m/z) ", isotopes.get(k).getAverageMass()) + (k + 1) + "-th isotope");
            }
        }
        if (allFeatures.isEmpty()) return Optional.empty();

        TraceSet traceSet = new TraceSet();
        MergedLCMSRun merged = storage.getByPrimaryKey(allFeatures.get(0).getRunId(), MergedLCMSRun.class).orElse(null);
        if (merged == null) return Optional.empty();
        storage.fetchChild(merged, "runId", "retentionTimeAxis", RetentionTimeAxis.class);
        if (merged.getRetentionTimeAxis().isEmpty()) return Optional.empty();

        traceSet.setSampleId(String.valueOf(merged.getRunId()));
        traceSet.setSampleName(merged.getName());

        int startIndexOfTraces = Integer.MAX_VALUE;
        for (AbstractAlignedFeatures f : allFeatures) {
            startIndexOfTraces = Math.min(startIndexOfTraces, f.getTraceRef().getScanIndexOffsetOfTrace());
        }
        RetentionTimeAxis mergedAxis = merged.getRetentionTimeAxis().get();
        int maximumIndex = 0;
        ArrayList<TraceSet.Trace> traces = new ArrayList<>();
        for (int k = 0; k < allFeatures.size(); ++k) {
            AbstractAlignedFeatures f = allFeatures.get(k);
            String label = labels.get(k);
            TraceRef r = f.getTraceRef();
            MergedTrace mergedTrace = storage.getByPrimaryKey(r.getTraceId(), MergedTrace.class).orElse(null);
            if (mergedTrace == null) continue;
            maximumIndex = Math.max(maximumIndex, mergedTrace.getScanIndexOffset() + mergedTrace.getIntensities().size());
            TraceSet.Trace trace = new TraceSet.Trace();
            trace.setMz(f.getAverageMass());
            trace.setId(String.valueOf(f instanceof AlignedIsotopicFeatures ? ((AlignedIsotopicFeatures) f).getAlignedIsotopeFeatureId() : (f instanceof AlignedFeatures ? ((AlignedFeatures) f).getAlignedFeatureId() : 0)));
            trace.setLabel(label);

            int shift = mergedTrace.getScanIndexOffset() - startIndexOfTraces;
            FloatList fs = mergedTrace.getIntensities();
            int len = fs.size() + shift;
            double[] vec = new double[len];
            for (int i = 0; i < fs.size(); ++i) {
                vec[i + shift] = fs.getFloat(i);
            }
            trace.setIntensities(vec);

            // add annotations
            ArrayList<TraceSet.Annotation> annotations = new ArrayList<>();
            // feature annotation
            annotations.add(new TraceSet.Annotation(TraceSet.AnnotationType.FEATURE, "[MAIN]" + label, //this ensures that lcms view shows the intensity of this features
                    r.getApex() + shift, r.getStart() + shift, r.getEnd() + shift));

            trace.setAnnotations(annotations.toArray(TraceSet.Annotation[]::new));
            traces.add(trace);

        }
        TraceSet.Axes axes = new TraceSet.Axes();

        /*
         * little dirty trick: to make the plots look nicer, we set the minimum width of the retention time
         * axis to 4xwidth. It would be nicer doing this in the UI directly, but then we would have to add
         * another API endpoint which would be a bit stupid for such a single number...
         */
        maximumIndex = Math.max(maximumIndex, Math.min(mergedAxis.getRetentionTimes().length, (startIndexOfTraces + (int) Math.ceil(merged.getSampleStats().getMedianPeakWidthInSeconds() * 4 / (mergedAxis.getRetentionTimes()[1] - mergedAxis.getRetentionTimes()[0])))));
        axes.setRetentionTimeInSeconds(Arrays.copyOfRange(mergedAxis.getRetentionTimes(), startIndexOfTraces, maximumIndex));
        traceSet.setAxes(axes);

        traceSet.setTraces(traces.toArray(TraceSet.Trace[]::new));

        return Optional.of(traceSet);
    }

    private Pair<String[], Database.SortOrder[]> sort(Sort sort, Pair<String, Database.SortOrder> defaults, Function<String, String> translator) {
        if (sort == null || sort.isEmpty() || sort == Sort.unsorted())
            return Pair.of(new String[]{defaults.getLeft()}, new Database.SortOrder[]{defaults.getRight()});

        List<String> properties = new ArrayList<>();
        List<Database.SortOrder> orders = new ArrayList<>();
        sort.stream().forEach(s -> {
            properties.add(translator.apply(s.getProperty()));
            orders.add(s.getDirection().isAscending() ? Database.SortOrder.ASCENDING : Database.SortOrder.DESCENDING);
        });
        return Pair.of(properties.toArray(String[]::new), orders.toArray(Database.SortOrder[]::new));
    }

    private Pair<String[], Database.SortOrder[]> sortRun(Sort sort) {
        return sort(sort, Pair.of("name", Database.SortOrder.ASCENDING), Function.identity());
    }

    private Pair<String[], Database.SortOrder[]> sortCompound(Sort sort) {
        return sort(sort, Pair.of("name", Database.SortOrder.ASCENDING), s -> switch (s) {
            case "rtStartSeconds" -> "rt.start";
            case "rtEndSeconds" -> "rt.end";
            default -> s;
        });
    }

    private Pair<String[], Database.SortOrder[]> sortFeature(Sort sort) {
        return sort(sort, Pair.of("name", Database.SortOrder.ASCENDING), s -> switch (s) {
            case "rtStartSeconds" -> "retentionTime.start";
            case "rtEndSeconds" -> "retentionTime.end";
            case "ionMass" -> "averageMass";
            default -> s;
        });
    }

    private Pair<String[], Database.SortOrder[]> sortMatch(Sort sort) {
        return sort(sort, Pair.of("searchResult.rank", Database.SortOrder.ASCENDING), s -> switch (s) {
            case "rank" -> "searchResult.rank";
            case "similarity" -> "searchResult.similarity.similarity";
            case "sharedPeaks" -> "searchResult.similarity.sharedPeaks";
            default -> s;
        });
    }

    private Pair<String[], Database.SortOrder[]> sortFoldChange(Sort sort) {
        return sort(sort, Pair.of("foldChange", Database.SortOrder.DESCENDING), Function.identity());
    }

    private Filter spectralMatchFilter(String alignedFeatureId, int minSharedPeaks, double minSimilarity) {
        long longId = Long.parseLong(alignedFeatureId);
        return Filter.and(
                Filter.where("alignedFeatureId").eq(longId),
                Filter.where("searchResult.similarity.sharedPeaks").gte(minSharedPeaks),
                Filter.where("searchResult.similarity.similarity").gte(minSimilarity)
        );
    }

    private Filter spectralMatchInchiFilter(String alignedFeatureId, String candidateInchi, int minSharedPeaks, double minSimilarity) {
        long longId = Long.parseLong(alignedFeatureId);
        return Filter.and(
                Filter.where("alignedFeatureId").eq(longId),
                Filter.where("searchResult.candidateInChiKey").eq(candidateInchi),
                Filter.where("searchResult.similarity.sharedPeaks").gte(minSharedPeaks),
                Filter.where("searchResult.similarity.similarity").gte(minSimilarity)
        );
    }

    private Pair<String[], Database.SortOrder[]> sortFormulaCandidate(Sort sort) {
        return sort(sort, Pair.of("formulaRank", Database.SortOrder.ASCENDING), Function.identity());
    }

    private Pair<String[], Database.SortOrder[]> sortStructureMatch(Sort sort) {
        return sort(sort, Pair.of("structureRank", Database.SortOrder.ASCENDING), Function.identity());
    }

    private Compound convertCompound(de.unijena.bioinf.ms.persistence.model.core.Compound compound,
                                     @NotNull EnumSet<Compound.OptField> optFields,
                                     @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields) {
        Compound.CompoundBuilder builder = Compound.builder()
                .compoundId(String.valueOf(compound.getCompoundId()))
                .name(compound.getName())
                .neutralMass(compound.getNeutralMass());

        RetentionTime rt = compound.getRt();
        if (rt != null) {
            if (Double.isFinite(rt.getStartTime()) && Double.isFinite(rt.getEndTime())) {
                builder.rtStartSeconds(rt.getStartTime());
                builder.rtEndSeconds(rt.getEndTime());
            } else {
                builder.rtStartSeconds(rt.getMiddleTime());
                builder.rtEndSeconds(rt.getMiddleTime());
            }
        }

        // merge optional field config
        final EnumSet<AlignedFeature.OptField> mergedFeatureFields = EnumSet.copyOf(optFeatureFields);
        if (optFields.contains(Compound.OptField.consensusAnnotations))
            mergedFeatureFields.add(AlignedFeature.OptField.topAnnotations);
        if (optFields.contains(Compound.OptField.consensusAnnotationsDeNovo))
            mergedFeatureFields.add(AlignedFeature.OptField.topAnnotationsDeNovo);

        // features
        List<AlignedFeature> features = compound.getAdductFeatures().stream().flatMap(featuresList -> featuresList.stream()
                .map(f -> convertToApiFeature(f, mergedFeatureFields))).toList();
        builder.features(features);

        if (optFields.contains(Compound.OptField.consensusAnnotations))
            builder.consensusAnnotations(AnnotationUtils.buildConsensusAnnotationsCSI(features));
        if (optFields.contains(Compound.OptField.consensusAnnotationsDeNovo))
            builder.consensusAnnotationsDeNovo(AnnotationUtils.buildConsensusAnnotationsDeNovo(features));
        if (optFields.contains(Compound.OptField.customAnnotations))
            builder.customAnnotations(ConsensusAnnotationsCSI.builder().build()); //todo implement custom annotations -> storage needed

        //remove optionals if not requested
        if (!optFeatureFields.contains(AlignedFeature.OptField.topAnnotations))
            features.forEach(f -> f.setTopAnnotations(null));
        if (!optFeatureFields.contains(AlignedFeature.OptField.topAnnotationsDeNovo))
            features.forEach(f -> f.setTopAnnotationsDeNovo(null));

        return builder.build();
    }

    private de.unijena.bioinf.ms.persistence.model.core.Compound convertCompound(CompoundImport compoundImport, @Nullable InstrumentProfile profile) {
        List<AlignedFeatures> features = compoundImport.getFeatures().stream()
                .map(f -> convertToProjectFeature(f, profile))
                .toList();

        de.unijena.bioinf.ms.persistence.model.core.Compound.CompoundBuilder builder = de.unijena.bioinf.ms.persistence.model.core.Compound.builder()
                .name(compoundImport.getName())
                .adductFeatures(features);

        if (features.size() == 1) {
            RetentionTime rt = features.getFirst().getRetentionTime();
            if (rt != null)
                builder.rt(rt);
        } else {
            List<RetentionTime> rts = features.stream().map(AlignedFeatures::getRetentionTime).filter(Objects::nonNull).toList();
            double start = rts.stream().mapToDouble(rt -> rt.isInterval() ? rt.getStartTime() : rt.getRetentionTimeInSeconds()).min().orElse(Double.NaN);
            double end = rts.stream().mapToDouble(rt -> rt.isInterval() ? rt.getEndTime() : rt.getRetentionTimeInSeconds()).max().orElse(Double.NaN);

            if (Double.isFinite(start) && Double.isFinite(end))
                builder.rt(new RetentionTime(start, end));
        }

        features.stream()
                .filter(AlignedFeatures::hasSingleAdduct)
                .mapToDouble(af -> af.getDetectedAdducts().getAllAdducts().getFirst().precursorMassToMeasuredNeutralMass(af.getAverageMass()))
                .average().ifPresent(builder::neutralMass);

        return builder.build();
    }

    private AlignedFeatures convertToProjectFeature(FeatureImport featureImport, @Nullable InstrumentProfile profile) {

        AlignedFeatures.AlignedFeaturesBuilder<?, ?> builder = AlignedFeatures.builder()
                .name(featureImport.getName())
                .externalFeatureId(featureImport.getExternalFeatureId())
                .averageMass(featureImport.getIonMass());

        if (featureImport.getDataQuality() != null)
            builder.dataQuality(featureImport.getDataQuality());

        MSData.MSDataBuilder msDataBuilder = MSData.builder();
        builder.charge((byte) featureImport.getCharge());

        if (featureImport.getMergedMs1() != null) {
            SimpleSpectrum mergedMs1 = new SimpleSpectrum(featureImport.getMergedMs1().getMasses(), featureImport.getMergedMs1().getIntensities());
            msDataBuilder.mergedMs1Spectrum(mergedMs1);
        } else if (featureImport.getMs1Spectra() != null && !featureImport.getMs1Spectra().isEmpty()) {
            Sirius sirius = StorageUtils.siriusProvider().sirius(profile != null ? profile.name() : MsInstrumentation.Unknown.getRecommendedProfile());
            List<ProcessedPeak> mergeMSPeaks = sirius.getMs2Preprocessor().preprocess(FeatureImports.toExperiment(featureImport)).getMergedPeaks();
            msDataBuilder.mergedMs1Spectrum(de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.from(mergeMSPeaks));
        }

        if (featureImport.getMs2Spectra() != null && !featureImport.getMs2Spectra().isEmpty()) {
            List<MutableMs2Spectrum> msnSpectra = new ArrayList<>();
            DoubleList pmz = new DoubleArrayList();
            for (int i = 0; i < featureImport.getMs2Spectra().size(); i++) {
                BasicSpectrum spectrum = featureImport.getMs2Spectra().get(i);
                MutableMs2Spectrum mutableMs2 = new MutableMs2Spectrum(spectrum);
                mutableMs2.setMsLevel(spectrum.getMsLevel());
                if (spectrum.getScanNumber() != null) {
                    mutableMs2.setScanNumber(spectrum.getScanNumber());
                }
                if (spectrum.getCollisionEnergy() != null) {
                    mutableMs2.setCollisionEnergy(spectrum.getCollisionEnergy());
                }
                if (spectrum.getPrecursorMz() != null) {
                    mutableMs2.setPrecursorMz(spectrum.getPrecursorMz());
                    pmz.add(spectrum.getPrecursorMz());
                }
                msnSpectra.add(mutableMs2);
                {
                    final Charge c = new Charge(featureImport.getCharge());
                    msDataBuilder.msnSpectra(msnSpectra.stream()
                            .peek(spec -> spec.setIonization(c))
                            .map(MergedMSnSpectrum::fromMs2Spectrum).toList());
                }
            }
            SimpleSpectrum merged = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.getNormalizedSpectrum(de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.mergeSpectra(new Deviation(10), true, false, msnSpectra), Normalization.Sum);
            msDataBuilder.mergedMSnSpectrum(merged);
        }
        MSData msData = msDataBuilder.build();
        builder.msData(msData);

        if (msData != null) {
            builder.hasMs1(msData.getMergedMs1Spectrum() != null);
            builder.hasMsMs((msData.getMsnSpectra() != null && !msData.getMsnSpectra().isEmpty()) || (msData.getMergedMSnSpectrum() != null));
        }

        builder.retentionTime(RetentionTime.of(featureImport.getRtStartSeconds(), featureImport.getRtEndSeconds(), featureImport.getRtApexSeconds()));

        if (featureImport.getDetectedAdducts() != null && !featureImport.getDetectedAdducts().isEmpty()) {
            de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts da = new de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts();
            featureImport.getDetectedAdducts().stream().map(PrecursorIonType::fromString).distinct().forEach(ionType ->
                    da.addAll(DetectedAdduct.builder().adduct(ionType).source(DetectedAdducts.Source.INPUT_FILE).build()));
            builder.detectedAdducts(da);
        } else {
            builder.detectedAdducts(new de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts());
        }
        return builder.build();
    }

    private AlignedFeature convertToApiFeature(AlignedFeatures features, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        final String fid = String.valueOf(features.getAlignedFeatureId());
        AlignedFeature.AlignedFeatureBuilder builder = AlignedFeature.builder()
                .alignedFeatureId(fid)
                .name(features.getName())
                .externalFeatureId(features.getExternalFeatureId())
                .compoundId(features.getCompoundId() == null ? null : features.getCompoundId().toString())
                .ionMass(features.getAverageMass())
                .quality(features.getDataQuality())
                .hasMs1(features.isHasMs1())
                .hasMsMs(features.isHasMsMs())
                .computing(computeStateProvider.apply(this, fid))
                .charge(features.getCharge());
        if (features.getDetectedAdducts() != null) {
            de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts adducts = features.getDetectedAdducts().clone();
            adducts.removeAllWithSource(DetectedAdducts.Source.SPECTRAL_LIBRARY_SEARCH);
            adducts.removeAllWithSource(DetectedAdducts.Source.MS1_PREPROCESSOR); //todo do not remove if detection runs during import.
            builder.detectedAdducts(adducts.getAllAdducts().stream().map(PrecursorIonType::toString)
                    .collect(Collectors.toSet()));
        } else {
            builder.detectedAdducts(Set.of());
        }
        RetentionTime rt = features.getRetentionTime();
        if (rt != null) {
            if (rt.isInterval() && Double.isFinite(rt.getStartTime()) && Double.isFinite(rt.getEndTime())) {
                builder.rtStartSeconds(rt.getStartTime());
                builder.rtApexSeconds(rt.getMiddleTime());
                builder.rtEndSeconds(rt.getEndTime());
            } else {
                builder.rtStartSeconds(rt.getMiddleTime());
                builder.rtApexSeconds(rt.getMiddleTime());
                builder.rtEndSeconds(rt.getMiddleTime());
            }
        }

        if (optFields.contains(AlignedFeature.OptField.msData)) {
            project().fetchMsData(features);
            features.getMSData().map(this::convertMSData).ifPresent(builder::msData);
        }
        if (optFields.contains(AlignedFeature.OptField.topAnnotations))
            builder.topAnnotations(extractTopCsiNovoAnnotations(features.getAlignedFeatureId()));
        if (optFields.contains(AlignedFeature.OptField.topAnnotationsDeNovo))
            builder.topAnnotationsDeNovo(extractTopDeNovoAnnotations(features.getAlignedFeatureId()));
        if (optFields.contains(AlignedFeature.OptField.computedTools))
            builder.computedTools(
                    project().findByFeatureIdStr(features.getAlignedFeatureId(), ComputedSubtools.class)
                            .findFirst().orElseGet(() -> ComputedSubtools.builder().build())
            );

        return builder.build();
    }

    private de.unijena.bioinf.ms.middleware.model.features.Feature convertToApiFeature0(Feature feature) {
        de.unijena.bioinf.ms.middleware.model.features.Feature.FeatureBuilder builder = de.unijena.bioinf.ms.middleware.model.features.Feature.builder()
                .featureId(Long.toString(feature.getFeatureId()))
                .alignedFeatureId(Long.toString(feature.getAlignedFeatureId()))
                .runId(Long.toString(feature.getRunId()))
                .averageMz(feature.getAverageMass())
                .rtFWHM(feature.getFwhm())
                .apexIntensity(feature.getApexIntensity())
                .areaUnderCurve(feature.getAreaUnderCurve());

        RetentionTime rt = feature.getRetentionTime();
        if (rt != null) {
            if (rt.isInterval() && Double.isFinite(rt.getStartTime()) && Double.isFinite(rt.getEndTime())) {
                builder.rtStartSeconds(rt.getStartTime());
                builder.rtApexSeconds(rt.getMiddleTime());
                builder.rtEndSeconds(rt.getEndTime());
            } else {
                builder.rtStartSeconds(rt.getMiddleTime());
                builder.rtApexSeconds(rt.getMiddleTime());
                builder.rtEndSeconds(rt.getMiddleTime());
            }
        }

        return builder.build();
    }

    @SneakyThrows
    private Run convertToApiRun(LCMSRun run, EnumSet<Run.OptField> optFields, @Nullable Map<String, de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory> categories) {
        Run.RunBuilder builder = Run.builder()
                .runId(Long.toString(run.getRunId()))
                .name(run.getName());

        if (run.getChromatography() != null) builder.chromatography(run.getChromatography().getFullName());
        if (run.getFragmentation() != null) builder.fragmentation(run.getFragmentation().getFullName());
        if (run.getIonization() != null) builder.ionization(run.getIonization().getFullName());
        if (run.getMassAnalyzers() != null && !run.getMassAnalyzers().isEmpty()) builder.massAnalyzers(run.getMassAnalyzers().stream().map(InstrumentConfig::getFullName).toList());

        if (optFields.contains(Run.OptField.tags) && categories != null) {
            builder.tags(storage()
                    .findStr(
                            Filter.where("taggedObjectId").eq(run.getRunId()),
                            de.unijena.bioinf.ms.persistence.model.core.tags.Tag.class)
                    .filter(tag -> categories.containsKey(tag.getCategory()))
                    .map(tag -> convertToApiTag(tag, categories.get(tag.getCategory())))
                    .collect(Collectors.toMap(Tag::getCategory, Function.identity())));
        }

        return builder.build();
    }

    private Tag convertToApiTag(de.unijena.bioinf.ms.persistence.model.core.tags.Tag tag, de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory category) {
        return switch (category.getValueType()) {
            case NONE -> Tag.builder().valueType(TagCategoryImport.ValueType.NONE).category(category.getName()).build();
            case BOOLEAN -> Tag.builder().valueType(TagCategoryImport.ValueType.BOOLEAN).category(category.getName()).bool(tag.isBool()).build();
            case INTEGER -> Tag.builder().valueType(TagCategoryImport.ValueType.INTEGER).category(category.getName()).integer(tag.getInt32()).build();
            case DOUBLE -> Tag.builder().valueType(TagCategoryImport.ValueType.DOUBLE).category(category.getName()).real(tag.getReal()).build();
            case STRING -> Tag.builder().valueType(TagCategoryImport.ValueType.STRING).category(category.getName()).text(tag.getText()).build();
            case DATE -> Tag.builder().valueType(TagCategoryImport.ValueType.DATE).category(category.getName()).date(TagController.DATE_FORMAT.format(new Date(tag.getInt64()))).build();
            case TIME -> Tag.builder().valueType(TagCategoryImport.ValueType.TIME).category(category.getName()).time(TagController.TIME_FORMAT.format(new Date(tag.getInt64()))).build();
        };
    }

    private de.unijena.bioinf.ms.persistence.model.core.tags.Tag setProjectTagValue(de.unijena.bioinf.ms.persistence.model.core.tags.Tag projectTag, Tag apiTag) throws ParseException {
        switch (apiTag.getValueType()) {
            case BOOLEAN: projectTag.setBool(Boolean.TRUE.equals(apiTag.getBool())); break;
            case INTEGER: projectTag.setInt32(apiTag.getInteger() != null ? apiTag.getInteger() : 0); break;
            case DOUBLE: projectTag.setReal(apiTag.getReal() != null ? apiTag.getReal() : Double.NaN); break;
            case STRING: projectTag.setText(apiTag.getText()); break;
            case DATE:
                try {
                    projectTag.setInt64(TagController.DATE_FORMAT.parse(apiTag.getDate()).getTime());
                } catch (Exception e) {
                    projectTag.setInt64(0);
                }
                break;
            case TIME:
                try {
                    projectTag.setInt64(TagController.TIME_FORMAT.parse(apiTag.getTime()).getTime());
                } catch (Exception e) {
                    projectTag.setInt64(0);
                }
                break;
        }
        return projectTag;
    }

    private de.unijena.bioinf.ms.persistence.model.core.tags.Tag convertToProjectTag(Tag tag, long taggedObjectId, String taggedObjectClass) throws ParseException {
        de.unijena.bioinf.ms.persistence.model.core.tags.Tag projectTag = de.unijena.bioinf.ms.persistence.model.core.tags.Tag.builder()
                .taggedObjectId(taggedObjectId)
                .category(tag.getCategory())
                .taggedObjectClass(taggedObjectClass).build();

        return setProjectTagValue(projectTag, tag);
    }

    private TagCategory convertToApiCategory(de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory category) {
        return TagCategory.builder()
                .name(category.getName())
                .categoryType(category.getCategoryType())
                .valueTypeAndPossibleValues(switch (category.getValueType()) {
                    case NONE -> TagCategory.ValueType.NONE;
                    case BOOLEAN -> TagCategory.ValueType.BOOLEAN;
                    case INTEGER -> TagCategory.ValueType.INTEGER;
                    case DOUBLE -> TagCategory.ValueType.DOUBLE;
                    case STRING -> TagCategory.ValueType.STRING;
                    case DATE -> TagCategory.ValueType.DATE;
                    case TIME -> TagCategory.ValueType.TIME;
                }, category.getPossibleValues()).build();
    }

    private de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory convertToProjectCategory(TagCategoryImport category, boolean editable) {
        de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.ValueType valueType = switch (category.getValueType()) {
            case NONE -> de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.ValueType.NONE;
            case BOOLEAN -> de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.ValueType.BOOLEAN;
            case INTEGER -> de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.ValueType.INTEGER;
            case DOUBLE -> de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.ValueType.DOUBLE;
            case STRING -> de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.ValueType.STRING;
            case DATE -> de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.ValueType.DATE;
            case TIME -> de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.ValueType.TIME;
        };

        de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.TagCategoryBuilder builder = de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.builder()
                .name(category.getName())
                .categoryType(category.getCategoryType())
                .valueType(valueType)
                .editable(editable);

        if (category.getValueType() != TagCategory.ValueType.NONE) {
            List<?> values = category.getPossibleValues();
            if (values != null) {
                for (Object value : values) {
                    if (value.getClass() != valueType.getValueClass()) {
                        throw new ResponseStatusException(BAD_REQUEST, "Wrong values provided for category " + category.getName() + ".");
                    }
                }
                builder.possibleValues(values);
            }
        }

        return builder.build();
    }

    private TagGroup convertToApiTagGroup(de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup group) {
        return TagGroup.builder()
                .name(group.getName())
                .luceneQuery(group.getLuceneQuery())
                .groupType(group.getGroupType())
                .build();
    }

    private FoldChange.AlignedFeatureFoldChange convertToApiFoldChange(de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange.AlignedFeaturesFoldChange foldChange) {
        return FoldChange.AlignedFeatureFoldChange.builder()
                .alignedFeatureId(Long.toString(foldChange.getAlignedFeatureId()))
                .leftGroup(foldChange.getLeftGroup())
                .rightGroup(foldChange.getRightGroup())
                .aggregation(foldChange.getAggregation())
                .quantification(foldChange.getQuantification())
                .foldChange(foldChange.getFoldChange())
                .build();
    }

    private FoldChange.CompoundFoldChange convertToApiFoldChange(de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange.CompoundFoldChange foldChange) {
        return FoldChange.CompoundFoldChange.builder()
                .compoundId(Long.toString(foldChange.getCompoundId()))
                .leftGroup(foldChange.getLeftGroup())
                .rightGroup(foldChange.getRightGroup())
                .aggregation(foldChange.getAggregation())
                .quantification(foldChange.getQuantification())
                .foldChange(foldChange.getFoldChange())
                .build();
    }

    private FeatureAnnotations extractTopCsiNovoAnnotations(long longAFIf) {
        return extractTopAnnotations(longAFIf, CsiStructureMatch.class);
    }

    private FeatureAnnotations extractTopDeNovoAnnotations(long longAFIf) {
        return extractTopAnnotations(longAFIf, DenovoStructureMatch.class);

    }

    private FeatureAnnotations extractTopAnnotations(long longAFIf, Class<? extends StructureMatch> clzz) {
        final FeatureAnnotations cSum = new FeatureAnnotations();

        StructureMatch structureMatch = project().findByFeatureIdStr(longAFIf, clzz, "structureRank", Database.SortOrder.ASCENDING)
                .findFirst().orElse(null);


        de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate formulaCandidate;
        if (structureMatch != null) {
            formulaCandidate = project().findByFormulaIdStr(structureMatch.getFormulaId(), de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class)
                    .findFirst().orElseThrow();

            //set Structure match
            cSum.setStructureAnnotation(convertStructureMatch(structureMatch, EnumSet.of(StructureCandidateScored.OptField.dbLinks, StructureCandidateScored.OptField.libraryMatches)));

            if (structureMatch instanceof CsiStructureMatch) //csi only but not denovo
                project().findByFeatureIdStr(longAFIf, CsiStructureSearchResult.class)
                        .findFirst().ifPresent(it -> {
                            cSum.setConfidenceExactMatch(it.getConfidenceExact());
                            cSum.setConfidenceApproxMatch(it.getConfidenceApprox());
                            cSum.setExpansiveSearchState(it.getExpansiveSearchConfidenceMode());
                            cSum.setSpecifiedDatabases(it.getSpecifiedDatabases());
                            cSum.setExpandedDatabases(it.getExpandedDatabases());
                        });
        } else {
            Pair<String[], Database.SortOrder[]> formSort = sortFormulaCandidate(null); //null == default
            formulaCandidate = project().findByFeatureIdStr(longAFIf, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class, formSort.getLeft()[0], formSort.getRight()[0])
                    .findFirst().orElse(null); //todo should we call a page of size one instead?
        }

        //get Canopus result. either for
        if (formulaCandidate != null) {
            cSum.setFormulaAnnotation(convertFormulaCandidate(formulaCandidate));
//            if (structureMatch != null)
//                cSum.getFormulaAnnotation().setTopCSIScore(structureMatch.getCsiScore());
            project().findByFormulaIdStr(formulaCandidate.getFormulaId(), de.unijena.bioinf.ms.persistence.model.sirius.CanopusPrediction.class)
                    .findFirst().map(cc -> CompoundClasses.of(cc.getNpcFingerprint(), cc.getCfFingerprint()))
                    .ifPresent(cSum::setCompoundClassAnnotation);
        }
        return cSum;
    }

    private MsData convertMSData(MSData msData) {
        MsData.MsDataBuilder builder = MsData.builder();
        if (msData.getMergedMs1Spectrum() != null)
            builder.mergedMs1(Spectrums.createMs1(msData.getMergedMs1Spectrum()));
        if (msData.getMergedMSnSpectrum() != null)
            builder.mergedMs2(Spectrums.createMergedMsMs(msData.getMergedMSnSpectrum(), msData.getMsnSpectra().get(0).getMergedPrecursorMz()));

        builder.ms2Spectra(msData.getMsnSpectra() != null ? msData.getMsnSpectra().stream().map(Spectrums::createMsMs).toList() : List.of());
        //MS1Spectra are not set since they are not stored in default MSData object.
        return builder.build();
    }

    private static final EnumSet<FormulaCandidate.OptField> needTree = EnumSet.of(
            FormulaCandidate.OptField.fragmentationTree, FormulaCandidate.OptField.annotatedSpectrum,
            FormulaCandidate.OptField.isotopePattern, FormulaCandidate.OptField.lipidAnnotation,
            FormulaCandidate.OptField.statistics
    );

    private FormulaCandidate convertFormulaCandidate(de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate candidate) {
        return convertFormulaCandidate(null, candidate, EnumSet.noneOf(FormulaCandidate.OptField.class));
    }

    private FormulaCandidate convertFormulaCandidate(@Nullable MSData msData, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate candidate, EnumSet<FormulaCandidate.OptField> optFields) {
        final long fid = candidate.getFormulaId();
        FormulaCandidate.FormulaCandidateBuilder builder = FormulaCandidate.builder()
                .formulaId(String.valueOf(fid))
                .molecularFormula(candidate.getMolecularFormula().toString())
                .adduct(candidate.getAdduct().toString())
                .rank(candidate.getFormulaRank())
                .siriusScoreNormalized(candidate.getSiriusScoreNormalized())
                .siriusScore(candidate.getSiriusScore())
                .isotopeScore(candidate.getIsotopeScore())
                .treeScore(candidate.getTreeScore())
                .zodiacScore(candidate.getZodiacScore());

        //todo post 6.0: we need the scores in the gui without the tree -> do we want to store stats separately from the tree?
        final FTree ftree = optFields.stream().anyMatch(needTree::contains)
                ? project().findByFormulaIdStr(fid, FTreeResult.class).findFirst().map(FTreeResult::getFTree).orElse(null)
                : null;

        if (ftree != null) {
            if (optFields.contains(FormulaCandidate.OptField.statistics)) {
                FTreeMetricsHelper scores = new FTreeMetricsHelper(ftree);
                builder.numOfExplainablePeaks(scores.getNumberOfExplainablePeaks())
                        .numOfExplainedPeaks(scores.getNumOfExplainedPeaks())
                        .totalExplainedIntensity(scores.getExplainedIntensityRatio())
                        .medianMassDeviation(scores.getMedianMassDeviation());
            }
            if (optFields.contains(FormulaCandidate.OptField.fragmentationTree))
                builder.fragmentationTree(FragmentationTree.fromFtree(ftree));
            if (optFields.contains(FormulaCandidate.OptField.lipidAnnotation))
                builder.lipidAnnotation(AnnotationUtils.asLipidAnnotation(ftree));
            if (optFields.contains(FormulaCandidate.OptField.annotatedSpectrum))
                //todo this is not efficient an loads spectra a second time as well as the whole experiment. we need no change spectra annotation code to improve this.
                builder.annotatedSpectrum(findAnnotatedMsMsSpectrum(-1, null, candidate.getFormulaId(), candidate.getAlignedFeatureId()));
            if (msData != null && optFields.contains(FormulaCandidate.OptField.isotopePattern)) {
                SimpleSpectrum isotopePattern = msData.getIsotopePattern();
                if (isotopePattern != null) {
                    builder.isotopePatternAnnotation(Spectrums.createIsotopePatternAnnotation(isotopePattern, ftree));
                }
            }
        }


        if (optFields.contains(FormulaCandidate.OptField.predictedFingerprint))
            project().findByFormulaIdStr(fid, CsiPrediction.class).findFirst()
                    .map(fpp -> fpp.getFingerprint().toProbabilityArray()).ifPresent(builder::predictedFingerprint);


        if (optFields.contains(FormulaCandidate.OptField.canopusPredictions) || optFields.contains(FormulaCandidate.OptField.compoundClasses)) {
            project().findByFormulaIdStr(fid, de.unijena.bioinf.ms.persistence.model.sirius.CanopusPrediction.class)
                    .findFirst().ifPresent(cr -> {
                        if (optFields.contains(FormulaCandidate.OptField.canopusPredictions))
                            builder.canopusPrediction(CanopusPrediction.of(cr.getNpcFingerprint(), cr.getCfFingerprint()));
                        if (optFields.contains(FormulaCandidate.OptField.compoundClasses))
                            builder.compoundClasses(CompoundClasses.of(cr.getNpcFingerprint(), cr.getCfFingerprint()));
                    });
        }
        return builder.build();

    }

    @SneakyThrows
    @Override
    public Page<Compound> findCompounds(Pageable pageable,
                                        @NotNull EnumSet<Compound.OptField> optFields,
                                        @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields) {
        Pair<String[], Database.SortOrder[]> sort = sortCompound(pageable.getSort());
        Stream<de.unijena.bioinf.ms.persistence.model.core.Compound> stream;
        if (pageable.isPaged()) {
            stream = storage().findAllStr(de.unijena.bioinf.ms.persistence.model.core.Compound.class, pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight());
        } else {
            stream = storage().findAllStr(de.unijena.bioinf.ms.persistence.model.core.Compound.class, sort.getLeft(), sort.getRight());
        }
        stream = stream.peek(project()::fetchAdductFeatures);

        if (optFeatureFields.contains(AlignedFeature.OptField.msData)) {
            stream = stream.peek(c -> c.getAdductFeatures().ifPresent(features -> features.forEach(project()::fetchMsData)));
        }

        List<Compound> compounds = stream.map(c -> convertCompound(c, optFields, optFeatureFields)).toList();

        long total = storage().countAll(de.unijena.bioinf.ms.persistence.model.core.Compound.class);

        return new PageImpl<>(compounds, pageable, total);
    }

    private void setProjectTypeOrThrow(SiriusProjectDocumentDatabase<? extends Database<?>> ps) {
        Optional<ProjectType> psType = ps.findProjectType();
        if (psType.isPresent()) {
            switch (psType.get()) {
                case ALIGNED_RUNS:
                case UNALIGNED_RUNS: {
                    ProjectTypeException reason = new ProjectTypeException("Project contains data from MS runs (.mzml, .mzxml) that have been preprocessed in SIRIUS. Additional data cannot be added to such project. Please create a new project to import your data.", ProjectType.ALIGNED_RUNS, psType.get());
                    throw new ResponseStatusException(BAD_REQUEST, reason.getMessage(), reason);
                }
            }
        }else {
            ps.upsertProjectType(ProjectType.DIRECT_IMPORT);
        }
    }

    @SneakyThrows
    @Override
    public List<Compound> addCompounds(@NotNull List<CompoundImport> compounds, InstrumentProfile profile, @NotNull EnumSet<Compound.OptField> optFields, @NotNull EnumSet<AlignedFeature.OptField> optFieldsFeatures) {
        setProjectTypeOrThrow(project());
        List<de.unijena.bioinf.ms.persistence.model.core.Compound> dbc = compounds.stream().map(ci -> convertCompound(ci, profile)).toList();
        project().importCompounds(dbc);
        return dbc.stream().map(c -> convertCompound(c, optFields, optFieldsFeatures)).toList();
    }

    @SneakyThrows
    @Override
    public Compound findCompoundById(String compoundId, @NotNull EnumSet<Compound.OptField> optFields, @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields) {
        long id = Long.parseLong(compoundId);
        return storage().getByPrimaryKey(id, de.unijena.bioinf.ms.persistence.model.core.Compound.class)
                .map(c -> {
                    project().fetchAdductFeatures(c);
                    if (optFeatureFields.contains(AlignedFeature.OptField.msData)) {
                        c.getAdductFeatures().ifPresent(features -> features.forEach(project()::fetchMsData));
                    }
                    return convertCompound(c, optFields, optFeatureFields);
                })
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "There is no compound '" + compoundId + "' in project " + projectId + "."));
    }

    @SneakyThrows
    @Override
    public void deleteCompoundById(String compoundId) {
        project().cascadeDeleteCompound(Long.parseLong(compoundId));
    }


    @SneakyThrows
    @Override
    public AlignedFeatureQuality findAlignedFeaturesQualityById(String alignedFeatureId) {
        return storage().getByPrimaryKey(Long.parseLong(alignedFeatureId), QualityReport.class).map(this::convertToFeatureQuality)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Not Quality information found for feature '" + alignedFeatureId + "' in project " + projectId + "."));
    }

    @SneakyThrows
    @Override
    public Page<AlignedFeatureQuality> findAlignedFeaturesQuality(Pageable pageable) {
        Stream<QualityReport> stream;
        if (pageable.isUnpaged() && pageable.getSort().isUnsorted()) {
            stream = storage().findAllStr(QualityReport.class);
        } else {
            Pair<String[], Database.SortOrder[]> sort = sortFeature(pageable.getSort());
            stream = storage().findAllStr(QualityReport.class, pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight());
        }

        List<AlignedFeatureQuality> features = stream.map(this::convertToFeatureQuality).toList();

        long total = storage().countAll(QualityReport.class);

        return new PageImpl<>(features, pageable, total);
    }

    private AlignedFeatureQuality convertToFeatureQuality(QualityReport report) {
        return AlignedFeatureQuality.builder()
                .alignedFeatureId(String.valueOf(report.getAlignedFeatureId()))
                .overallQuality(report.getOverallQuality())
                .categories(report.getCategories())
                .build();
    }

    @SneakyThrows
    @Override
    public Page<AlignedFeature> findAlignedFeatures(Pageable pageable, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        Stream<AlignedFeatures> stream;
        if (pageable.isUnpaged() && pageable.getSort().isUnsorted()) {
            stream = storage().findAllStr(AlignedFeatures.class);
        } else {
            Pair<String[], Database.SortOrder[]> sort = sortFeature(pageable.getSort());
            stream = storage().findAllStr(AlignedFeatures.class, pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight());
        }

        List<AlignedFeature> features = stream.map(alf -> convertToApiFeature(alf, optFields)).toList();

        long total = storage().countAll(AlignedFeatures.class);

        return new PageImpl<>(features, pageable, total);
    }

    @SneakyThrows
    @Override
    public List<de.unijena.bioinf.ms.middleware.model.features.Feature> findFeaturesByAlignedFeatureId(String alignedFeatureId) {
        return storage().findStr(Filter.where("alignedFeatureId").eq(Long.parseLong(alignedFeatureId)), Feature.class).map(this::convertToApiFeature0).toList();
    }

    @Override
    public List<AlignedFeature> addAlignedFeatures(@NotNull List<FeatureImport> features, @Nullable InstrumentProfile profile, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        LongestCommonSubsequence lcs = new LongestCommonSubsequence();
        String name = features.stream().map(FeatureImport::getName)
                .filter(Objects::nonNull)
                .filter(Predicate.not(String::isBlank))
                .reduce((a, b) -> lcs.longestCommonSubsequence(a, b).toString())
                .filter(Predicate.not(String::isBlank))
                .orElse(null);

        CompoundImport ci = CompoundImport.builder().name(name).features(features).build();
        Compound compound = addCompounds(List.of(ci), profile, EnumSet.of(Compound.OptField.none), optFields).stream().findFirst().orElseThrow(
                () -> new ResponseStatusException(NOT_FOUND, "Compound could not be imported to " + projectId + ".")
        );
        return compound.getFeatures();
    }

    @SneakyThrows
    @Override
    public AlignedFeature findAlignedFeaturesById(String alignedFeatureId, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        long id = Long.parseLong(alignedFeatureId);
        return storage().getByPrimaryKey(id, AlignedFeatures.class)
                .map(a -> convertToApiFeature(a, optFields)).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "There is no aligned feature '" + alignedFeatureId + "' in project " + projectId + "."));
    }

    @SneakyThrows
    @Override
    public void deleteAlignedFeaturesById(String alignedFeatureId) {
        project().cascadeDeleteAlignedFeatures(Long.parseLong(alignedFeatureId));
    }

    @Override
    @SneakyThrows
    public void deleteAlignedFeaturesByIds(List<String> alignedFeatureIds) {
        project().cascadeDeleteAlignedFeatures(alignedFeatureIds.stream().map(Long::parseLong).sorted().toList());
    }

    @SneakyThrows
    @Override
    public Page<Run> findRuns(Pageable pageable, @NotNull EnumSet<Run.OptField> optFields) {
        long total;
        List<Run> objects;
        final Map<String, de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory> categories;
        if (optFields.contains(Run.OptField.tags)) {
            categories = storage()
                    .findAllStr(de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.class)
                    .collect(Collectors.toMap(de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory::getName, Function.identity()));
        } else {
            categories = null;
        }
        if (pageable.isUnpaged() && pageable.getSort().isUnsorted()) {
            objects = storage().findAllStr(LCMSRun.class).map(run -> convertToApiRun(run, optFields, categories)).toList();
            total = objects.size();
        } else {
            Pair<String[], Database.SortOrder[]> sort = sortRun(pageable.getSort());
            objects = storage().findAllStr(LCMSRun.class, pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight()).map(run -> convertToApiRun(run, optFields, categories)).toList();
            total = storage().countAll(LCMSRun.class);
        }
        return new PageImpl<>(objects, pageable, total);
    }

    @SneakyThrows
    @Override
    public Run findRunById(String runId, @NotNull EnumSet<Run.OptField> optFields) {
        final Map<String, de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory> categories;
        if (optFields.contains(Run.OptField.tags)) {
            categories = storage()
                    .findAllStr(de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.class)
                    .collect(Collectors.toMap(de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory::getName, Function.identity()));
        } else {
            categories = null;
        }
        return storage().getByPrimaryKey(Long.parseLong(runId), LCMSRun.class)
                .map(run -> convertToApiRun(run, optFields, categories))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "There is no run '" + runId + "' in project " + projectId + "."));
    }

    private Page<Run> findRunsByFilter(Pageable pageable, Filter filter, EnumSet<Run.OptField> optFields) throws IOException {
        long total;
        List<Run> objects;

        final Map<String, de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory> categories;
        if (optFields.contains(Run.OptField.tags)) {
            categories = storage()
                    .findAllStr(de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.class)
                    .collect(Collectors.toMap(de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory::getName, Function.identity()));
        } else {
            categories = null;
        }

        if (pageable.isUnpaged() && pageable.getSort().isUnsorted()) {
            objects = storage().findStr(filter, LCMSRun.class)
                    .map(run -> convertToApiRun(run, optFields, categories)).toList();
            total = objects.size();
        } else {
            Pair<String[], Database.SortOrder[]> sort = sortRun(pageable.getSort());
            objects = storage().findStr(filter, LCMSRun.class, pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight())
                    .map(run -> convertToApiRun(run, optFields, categories)).toList();
            total = storage().count(filter, LCMSRun.class);
        }

        return new PageImpl<>(objects, pageable, total);
    }

    @SneakyThrows
    @Override
    @SuppressWarnings("unchecked")
    public <T, O extends Enum<O>> Page<T> findObjectsByTag(Class<?> target, @NotNull String filter, Pageable pageable, @NotNull EnumSet<O> optFields) {
        Class<?> taggedObjectClass = getTaggedObjectClass(target);
        AtomicReference<String> fieldName = new AtomicReference<>(null);
        ReflectionUtils.doWithFields(
                taggedObjectClass,
                field -> {
                    fieldName.set(field.getName());
                },
                field -> field.getAnnotation(Id.class) != null);
        if (fieldName.get() == null)
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "No @Id field in " + taggedObjectClass);

        Filter tagFilter;
        try {
            tagFilter = LuceneUtils.translateTagFilter(filter);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "Parse error: " + filter);
        }
        tagFilter = Filter.and(
                Filter.where("taggedObjectClass").eq(taggedObjectClass.toString()),
                tagFilter
        );
        Long[] objectIds = storage().findStr(tagFilter, de.unijena.bioinf.ms.persistence.model.core.tags.Tag.class)
                .map(de.unijena.bioinf.ms.persistence.model.core.tags.Tag::getTaggedObjectId).toArray(Long[]::new);

        if (objectIds.length == 0)
            return Page.empty();

        Filter objectFilter = Filter.where(fieldName.get()).in(objectIds);
        return (Page<T>) findRunsByFilter(pageable, objectFilter, (EnumSet<Run.OptField>) optFields);
    }

    private Class<?> getTaggedObjectClass(Class<?> taggable) {
        if (taggable.equals(Run.class)) {
            return LCMSRun.class;
        }
        throw new IllegalStateException("Unknown taggable: " + taggable);
    }

    private Object getValueFromTag(Tag tag) throws ParseException {
        return switch (tag.getValueType()) {
            case NONE -> null;
            case BOOLEAN -> tag.getBool();
            case INTEGER -> tag.getInteger();
            case DOUBLE -> tag.getReal();
            case STRING -> tag.getText();
            case DATE -> {
                try {
                    yield TagController.DATE_FORMAT.parse(tag.getDate()).getTime();
                } catch (Exception e) {
                    yield 0L;
                }
            }
            case TIME -> {
                try {
                    yield TagController.TIME_FORMAT.parse(tag.getTime()).getTime();
                } catch (Exception e) {
                    yield 0L;
                }
            }
        };
    }

    @Override
    public List<Tag> addTagsToObject(Class<?> target, String objectId, List<Tag> tags) {
        try {
            Class<?> taggedObjectClass = getTaggedObjectClass(target);
            long objId = Long.parseLong(objectId);
            if (storage().getByPrimaryKey(objId, taggedObjectClass).isEmpty())
                throw new ResponseStatusException(NOT_FOUND, "There is no object '" + objectId + "' in project " + projectId + ".");

            final Map<String, de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory> categories = storage()
                    .findAllStr(de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.class)
                    .collect(Collectors.toMap(de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory::getName, Function.identity()));

            for (Tag tag : tags) {
                if (!categories.containsKey(tag.getCategory())) {
                    throw new ResponseStatusException(NOT_FOUND, "There is no category '" + tag.getCategory() + "' in project " + projectId + ".");
                }
                de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory category = categories.get(tag.getCategory());
                if (switch (category.getValueType()) {
                    case STRING -> tag.getValueType() != TagCategoryImport.ValueType.STRING;
                    case BOOLEAN -> tag.getValueType() != TagCategoryImport.ValueType.BOOLEAN;
                    case INTEGER -> tag.getValueType() != TagCategoryImport.ValueType.INTEGER;
                    case DOUBLE -> tag.getValueType() != TagCategoryImport.ValueType.DOUBLE;
                    case DATE -> tag.getValueType() != TagCategoryImport.ValueType.DATE;
                    case TIME -> tag.getValueType() != TagCategoryImport.ValueType.TIME;
                    case NONE -> tag.getValueType() != TagCategoryImport.ValueType.NONE;
                }) {
                    throw new ResponseStatusException(BAD_REQUEST, "Wrong tag type '" + tag.getClass() + " for category " + tag.getCategory() + ".");
                }
                if (category.getValueType() != de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.ValueType.NONE &&
                        category.getPossibleValues() != null &&
                        !category.getPossibleValues().isEmpty() &&
                        !category.getPossibleValues().contains(getValueFromTag(tag))) {
                    throw new ResponseStatusException(BAD_REQUEST, "Forbidden value '" + getValueFromTag(tag) + " for category " + tag.getCategory() + ".");
                }
            }

            Map<String, de.unijena.bioinf.ms.persistence.model.core.tags.Tag> existingTags = storage().findStr(Filter.where("taggedObjectId").eq(objId), de.unijena.bioinf.ms.persistence.model.core.tags.Tag.class)
                    .collect(Collectors.toMap(de.unijena.bioinf.ms.persistence.model.core.tags.Tag::getCategory, Function.identity()));
            List<de.unijena.bioinf.ms.persistence.model.core.tags.Tag> upsertTags = new ArrayList<>();
            List<de.unijena.bioinf.ms.persistence.model.core.tags.Tag> insertTags = new ArrayList<>();

            for (Tag tag : tags) {
                if (existingTags.containsKey(tag.getCategory())) {
                    de.unijena.bioinf.ms.persistence.model.core.tags.Tag old = existingTags.get(tag.getCategory());
                    de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory category = categories.get(tag.getCategory());
                    switch (category.getValueType()) {
                        case NONE:
                            break;
                        case BOOLEAN:
                            if (!Objects.equals(old.isBool(), getValueFromTag(tag))) {
                                setProjectTagValue(old, tag);
                                upsertTags.add(old);
                            }
                            break;
                        case INTEGER:
                            if (!Objects.equals(old.getInt32(), getValueFromTag(tag))) {
                                setProjectTagValue(old, tag);
                                upsertTags.add(old);
                            }
                            break;
                        case DOUBLE:
                            if (!Objects.equals(old.getReal(), getValueFromTag(tag))) {
                                setProjectTagValue(old, tag);
                                upsertTags.add(old);
                            }
                            break;
                        case STRING:
                            if (!Objects.equals(old.getText(), getValueFromTag(tag))) {
                                setProjectTagValue(old, tag);
                                upsertTags.add(old);
                            }
                            break;
                        case DATE, TIME:
                            if (!Objects.equals(old.getInt64(), getValueFromTag(tag))) {
                                setProjectTagValue(old, tag);
                                upsertTags.add(old);
                            }
                            break;
                    }

                } else {
                    insertTags.add(convertToProjectTag(tag, objId, taggedObjectClass.toString()));
                }
            }

            storage().upsertAll(upsertTags);
            storage().insertAll(insertTags);

            return Stream.concat(upsertTags.stream(), insertTags.stream()).map(tag -> convertToApiTag(tag, categories.get(tag.getCategory()))).toList();
        } catch (IOException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR);
        } catch (ParseException e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage());
        }
    }

    @SneakyThrows
    @Override
    public void deleteTagsFromObject(String objectId, List<String> categoryNames) {
        for (de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory category : storage ().find(Filter.where("name").in(categoryNames.toArray(String[]::new)), de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.class)) {
            storage().removeAll(Filter.and(
                    Filter.where("taggedObjectId").eq(Long.parseLong(objectId)),
                    Filter.where("category").eq(category.getName())
            ), de.unijena.bioinf.ms.persistence.model.core.tags.Tag.class);
        }
    }

    @SneakyThrows
    @Override
    public List<TagCategory> findCategories() {
        return storage().findAllStr(de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.class)
                .map(this::convertToApiCategory).toList();
    }

    @SneakyThrows
    @Override
    public List<TagCategory> findCategoriesByType(String categoryType) {
        return storage().findStr(Filter.where("categoryType").eq(categoryType), de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.class)
                .map(this::convertToApiCategory).toList();
    }

    @SneakyThrows
    @Override
    public TagCategory findCategoryByName(String categoryName) {
        return storage().findStr(Filter.where("name").eq(categoryName), de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.class)
                .findFirst()
                .map(this::convertToApiCategory)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "There is no tag category '" + categoryName + "' in project " + projectId + "."));
    }

    @SneakyThrows
    @Override
    public List<TagCategory> addCategories(List<TagCategoryImport> categories, boolean editable) {
        Set<String> existingNames = storage().findAllStr(de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.class)
                .map(de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory::getName)
                .collect(Collectors.toSet());
        List<de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory> filtered = categories.stream()
                .filter(category -> !existingNames.contains(category.getName()))
                .map(category -> convertToProjectCategory(category, editable)).toList();
        storage().insertAll(filtered);
        return filtered.stream().map(this::convertToApiCategory).toList();
    }

    @SneakyThrows
    @Override
    public void deleteCategory(String categoryName) {
        Optional<de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory> category = storage().findStr(Filter.where("name").eq(categoryName), de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.class).findFirst();
        if (category.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "No such category: " + categoryName);
        }
        if (!category.get().isEditable()) {
            throw new ResponseStatusException(BAD_REQUEST, "Category can not be edited: " + categoryName);
        }
        storage().removeAll(Filter.where("category").eq(categoryName), de.unijena.bioinf.ms.persistence.model.core.tags.Tag.class);
        storage().remove(category.get());
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    @Override
    public TagCategory addPossibleValuesToCategory(String categoryName, List<?> values) {
        Optional<de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory> category = storage().findStr(Filter.where("name").eq(categoryName), de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.class).findFirst();
        if (category.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "No such category: " + categoryName);
        }
        if (!category.get().isEditable()) {
            throw new ResponseStatusException(BAD_REQUEST, "Category can not be edited: " + categoryName);
        }
        if (category.get().getValueType() == de.unijena.bioinf.ms.persistence.model.core.tags.TagCategory.ValueType.NONE) {
            throw new ResponseStatusException(BAD_REQUEST, "Can not add values to NONE type category " + categoryName);
        }
        List<Object> possibleValues = category.get().getPossibleValues() != null ? (List<Object>) category.get().getPossibleValues() : new ArrayList<>();
        List<?> filtered = values.stream().filter(value -> !possibleValues.contains(value)).toList();
        for (Object value : filtered) {
            if (value.getClass() != category.get().getValueType().getValueClass()) {
                throw new ResponseStatusException(BAD_REQUEST, "Wrong values provided for category " + categoryName + ".");
            }
            possibleValues.add(value);
        }
        category.get().setPossibleValues(possibleValues);
        storage().upsert(category.get());
        return convertToApiCategory(category.get());
    }

    @SneakyThrows
    @Override
    public <T, O extends Enum<O>> Page<T> findObjectsByTagGroup(Class<?> target, @NotNull String group, Pageable pageable, @NotNull EnumSet<O> optFields) {
        Optional<de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup> tagGroup = storage().findStr(Filter.where("name").eq(group), de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup.class).findFirst();
        if (tagGroup.isEmpty())
            return Page.empty();

        return findObjectsByTag(target, tagGroup.get().getLuceneQuery(), pageable, optFields);
    }

    @SneakyThrows
    @Override
    public List<TagGroup> findTagGroups() {
        return storage()
                .findAllStr(de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup.class)
                .map(this::convertToApiTagGroup)
                .toList();
    }

    @SneakyThrows
    @Override
    public List<TagGroup> findTagGroupsByType(String type) {
        List<TagGroup> groups = storage()
                .findStr(Filter.where("groupType").eq(type), de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup.class)
                .map(this::convertToApiTagGroup)
                .toList();
        if (groups.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "No tag category group of type: " + type);
        }
        return groups;
    }

    @SneakyThrows
    @Override
    public TagGroup findTagGroup(String name) {
        return convertToApiTagGroup(storage()
                .findStr(Filter.where("name").eq(name), de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup.class)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No such tag category group: " + name)));
    }

    @SneakyThrows
    @Override
    public TagGroup addTagGroup(String name, String filter, String type) {
        if (storage().findStr(Filter.where("name").eq(name), de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup.class).count() > 0) {
            throw new ResponseStatusException(NOT_ACCEPTABLE, "Tag category group " + name + " already exists");
        }

        de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup group = de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup
                .builder()
                .name(name)
                .luceneQuery(filter)
                .groupType(type)
                .build();

        storage().insert(group);
        return convertToApiTagGroup(group);
    }

    @SneakyThrows
    @Override
    public void deleteTagGroup(String name) {
        Optional<de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup> group = storage().findStr(Filter.where("name").eq(name), de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup.class).findFirst();
        if (group.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "No such category group: " + name);
        }

        storage().remove(group.get());
    }

    @SneakyThrows
    @Override
    public StatisticsTable getFoldChangeTable(Class<?> target, AggregationType aggregation, QuantificationMeasure quantification) {
        StatisticsTable table = StatisticsTable.builder()
                .statisticsType(StatisticsTable.StatisticsType.FOLD_CHANGE)
                .quantificationMeasure(quantification)
                .aggregationType(aggregation)
                .build();

        if (AlignedFeature.class.equals(target)) {
            table.setRowType(StatisticsTable.RowType.FEATURES);
            fillFoldChangeTable(table, de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange.AlignedFeaturesFoldChange.class, aggregation, quantification);
        } else if (Compound.class.equals(target)) {
            table.setRowType(StatisticsTable.RowType.COMPOUNDS);
            fillFoldChangeTable(table, de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange.CompoundFoldChange.class, aggregation, quantification);
        } else {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Type not supported: " + target);
        }
        return table;
    }

    private <F extends de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange> void fillFoldChangeTable(StatisticsTable table, Class<F> fcClass, AggregationType aggregation, QuantificationMeasure quantification) throws IOException {
        List<F> foldChanges = storage().findStr(Filter.and(
                Filter.where("aggregation").eq(aggregation.toString()),
                Filter.where("quantification").eq(quantification.toString())
        ), fcClass).sorted(Comparator.comparingLong(de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange::getForeignId)).toList();

        Set<Pair<String, String>> pairSet = new HashSet<>();
        for (de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange fc : foldChanges) {
            pairSet.add(Pair.of(fc.getLeftGroup(), fc.getRightGroup()));
        }
        List<Pair<String, String>> pairs = new ArrayList<>(pairSet);

        LongList rowIds = new LongArrayList();
        List<double[]> values = new ArrayList<>();
        for (de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange fc : foldChanges) {
            if (rowIds.isEmpty() || fc.getForeignId() != rowIds.getLast()) {
                rowIds.add(fc.getForeignId());
                values.add(new double[pairSet.size()]);
            }
            int index = pairs.indexOf(Pair.of(fc.getLeftGroup(), fc.getRightGroup()));
            values.getLast()[index] = fc.getFoldChange();
        }

        table.setColumnNames(pairs.stream().map(pair -> pair.getLeft() + " / " + pair.getRight()).toArray(String[]::new));
        table.setColumnLeftGroups(pairs.stream().map(Pair::getLeft).toArray(String[]::new));
        table.setColumnRightGroups(pairs.stream().map(Pair::getRight).toArray(String[]::new));
        table.setRowIds(rowIds.toLongArray());
        table.setValues(values.toArray(double[][]::new));
    }

    @SneakyThrows
    @Override
    @SuppressWarnings("unchecked")
    public <F extends FoldChange> Page<F> listFoldChanges(Class<?> target, Pageable pageable) {
        List<F> objects;
        long total;
        if (pageable.isUnpaged() && pageable.getSort().isUnsorted()) {
            if (AlignedFeature.class.equals(target)) {
                objects = (List<F>) storage()
                        .findAllStr(de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange.AlignedFeaturesFoldChange.class)
                        .map(this::convertToApiFoldChange)
                        .toList();
            } else if (Compound.class.equals(target)) {
                objects = (List<F>) storage()
                        .findAllStr(de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange.CompoundFoldChange.class)
                        .map(this::convertToApiFoldChange)
                        .toList();
            } else {
                throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Type not supported: " + target);
            }
            total = objects.size();
        } else {
            Pair<String[], Database.SortOrder[]> sort = sortRun(pageable.getSort());
            if (AlignedFeature.class.equals(target)) {
                objects = (List<F>) storage().findAllStr(
                                de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange.AlignedFeaturesFoldChange.class,
                                pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight())
                        .map(fc -> convertToApiFoldChange(fc))
                        .toList();
                total = storage().countAll(de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange.AlignedFeaturesFoldChange.class);
            } else if (Compound.class.equals(target)) {
                objects = (List<F>) storage().findAllStr(
                                de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange.CompoundFoldChange.class,
                                pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight())
                        .map(fc -> convertToApiFoldChange(fc))
                        .toList();
                total = storage().countAll(de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange.CompoundFoldChange.class);
            } else {
                throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Type not supported: " + target);
            }
        }

        return new PageImpl<>(objects, pageable, total);
    }

    @SneakyThrows
    @Override
    @SuppressWarnings("unchecked")
    public <F extends FoldChange> List<F> getFoldChanges(Class<?> target, String objectId) {
        if (AlignedFeature.class.equals(target)) {
            return (List<F>) storage()
                    .findStr(Filter.where("foreignId").eq(Long.parseLong(objectId)), de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange.AlignedFeaturesFoldChange.class)
                    .map(this::convertToApiFoldChange)
                    .toList();
        } else if (Compound.class.equals(target)) {
            return  (List<F>) storage()
                    .findStr(Filter.where("foreignId").eq(Long.parseLong(objectId)), de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange.CompoundFoldChange.class)
                    .map(this::convertToApiFoldChange)
                    .toList();
        } else {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Type not supported: " + target);
        }
    }

    @SneakyThrows
    @Override
    public void deleteFoldChange(Class<?> target, String left, String right, AggregationType aggregation, QuantificationMeasure quantification) {
        if (AlignedFeature.class.equals(target)) {
            storage().removeAll(
                    Filter.and(
                            Filter.where("leftGroup").eq(left),
                            Filter.where("rightGroup").eq(right),
                            Filter.where("aggregation").eq(aggregation.toString()),
                            Filter.where("quantification").eq(quantification.toString())
                    ),
                    de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange.AlignedFeaturesFoldChange.class
            );
        } else if (Compound.class.equals(target)) {
            storage().removeAll(
                    Filter.and(
                            Filter.where("leftGroup").eq(left),
                            Filter.where("rightGroup").eq(right),
                            Filter.where("aggregation").eq(aggregation.toString()),
                            Filter.where("quantification").eq(quantification.toString())
                    ),
                    de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange.CompoundFoldChange.class
            );
        }else {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Type not supported: " + target);
        }
    }

    private SpectralLibraryMatchSummary summarize(Filter filter) throws IOException {
        LongSet refSpecSet = new LongOpenHashSet();
        long total = 0;
        Set<String> compoundSet = new HashSet<>();
        SpectraMatch bestMatch = null;
        for (SpectraMatch match : project().getStorage().find(filter, SpectraMatch.class, "searchResult.similarity.similarity", Database.SortOrder.DESCENDING)) {
            refSpecSet.add(match.getUuid());
            compoundSet.add(match.getCandidateInChiKey());
            if (bestMatch == null) {
                bestMatch = match;
            } else if (
                    Math.abs(bestMatch.getSimilarity().similarity - match.getSimilarity().similarity) < 1E-3 &&
                            bestMatch.getSimilarity().sharedPeaks < match.getSimilarity().sharedPeaks
            ) {
                bestMatch = match;
            } else if (bestMatch.getSimilarity().similarity < match.getSimilarity().similarity) {
                bestMatch = match;
            }
        }

        return SpectralLibraryMatchSummary.builder()
                .bestMatch(bestMatch != null ? SpectralLibraryMatch.of(bestMatch) : null)
                .spectralMatchCount(total)
                .referenceSpectraCount(refSpecSet.size())
                .databaseCompoundCount(compoundSet.size()).build();
    }

    @SneakyThrows
    @Override
    public SpectralLibraryMatchSummary summarizeLibraryMatchesByFeatureId(String alignedFeatureId, int minSharedPeaks, double minSimilarity) {
        Filter filter = spectralMatchFilter(alignedFeatureId, minSharedPeaks, minSimilarity);
        return summarize(filter);
    }

    @SneakyThrows
    @Override
    public SpectralLibraryMatchSummary summarizeLibraryMatchesByFeatureIdAndInchi(String alignedFeatureId, String candidateInchi, int minSharedPeaks, double minSimilarity) {
        Filter filter = spectralMatchInchiFilter(alignedFeatureId, candidateInchi, minSharedPeaks, minSimilarity);
        return summarize(filter);
    }

    private Page<SpectralLibraryMatch> findLibMatches(Filter filter, Pageable pageable) throws IOException {
        Pair<String[], Database.SortOrder[]> sort = sortMatch(pageable.getSort());

        Stream<SpectraMatch> matches;
        if (pageable.isPaged()) {
            matches = project().getStorage().findStr(filter, SpectraMatch.class, pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight()
            );
        } else {
            matches = project().getStorage().findStr(filter, SpectraMatch.class, sort.getLeft(), sort.getRight());
        }

        long total = project().getStorage().count(filter, SpectraMatch.class);

        return new PageImpl<>(matches.map(SpectralLibraryMatch::of).toList(), pageable, total);
    }

    @SneakyThrows
    @Override
    public Page<SpectralLibraryMatch> findLibraryMatchesByFeatureId(String alignedFeatureId, int minSharedPeaks, double minSimilarity, Pageable pageable) {
        Filter filter = spectralMatchFilter(alignedFeatureId, minSharedPeaks, minSimilarity);
        return findLibMatches(filter, pageable);
    }

    @SneakyThrows
    @Override
    public Page<SpectralLibraryMatch> findLibraryMatchesByFeatureIdAndInchi(String alignedFeatureId, String candidateInchi, int minSharedPeaks, double minSimilarity, Pageable pageable) {
        Filter filter = spectralMatchInchiFilter(alignedFeatureId, candidateInchi, minSharedPeaks, minSimilarity);
        return findLibMatches(filter, pageable);
    }

    @SneakyThrows
    @Override
    public SpectralLibraryMatch findLibraryMatchesByFeatureIdAndMatchId(String alignedFeatureId, String matchId) {
        long specMatchId = Long.parseLong(matchId);
        return storage().getByPrimaryKey(specMatchId, SpectraMatch.class).map(SpectralLibraryMatch::of)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Not Spectral match with ID '" + matchId + "' Exists."));
    }

    @SneakyThrows
    @Override
    public Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<FormulaCandidate.OptField> optFields) {
        long longAFId = Long.parseLong(alignedFeatureId);
        Pair<String[], Database.SortOrder[]> sort = sortFormulaCandidate(pageable.getSort());

        //load ms data only once per formula candidate
        final MSData msData = Stream.of(/*FormulaCandidate.OptField.annotatedSpectrum,*/ FormulaCandidate.OptField.isotopePattern).anyMatch(optFields::contains)
                ? project().findByFeatureIdStr(longAFId, MSData.class).findFirst().orElse(null) : null;

        List<FormulaCandidate> candidates;
        if (pageable.isPaged()) {
            candidates = project().findByFeatureIdStr(longAFId, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class, pageable.getOffset(), pageable.getPageSize(), sort.getLeft()[0], sort.getRight()[0])
                    .map(fc -> convertFormulaCandidate(msData, fc, optFields)).toList();
        } else {
            candidates = project().findByFeatureIdStr(longAFId, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class, sort.getLeft()[0], sort.getRight()[0])
                    .map(fc -> convertFormulaCandidate(msData, fc, optFields)).toList();
        }
        long total = project().countByFeatureId(longAFId, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class);

        return new PageImpl<>(candidates, pageable, total);
    }

    @SneakyThrows
    @Override
    public FormulaCandidate findFormulaCandidateByFeatureIdAndId(String formulaId, String alignedFeatureId, @NotNull EnumSet<FormulaCandidate.OptField> optFields) {
        long longFId = Long.parseLong(formulaId);
        long longAFId = Long.parseLong(alignedFeatureId);

        final MSData msData = Stream.of(/*FormulaCandidate.OptField.annotatedSpectrum,*/ FormulaCandidate.OptField.isotopePattern).anyMatch(optFields::contains)
                ? project().findByFeatureIdStr(longAFId, MSData.class).findFirst().orElse(null) : null;

        return project().findByFormulaIdStr(longFId, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class)
                .peek(fc -> {
                    if (fc.getAlignedFeatureId() != longAFId)
                        throw new ResponseStatusException(BAD_REQUEST, "Formula candidate exists but FormulaID does not belong to the requested FeatureID. Are you using the correct Ids?");
                }).map(fc -> convertFormulaCandidate(msData, fc, optFields)).findFirst().orElse(null);
    }

    @Override
    public Page<StructureCandidateScored> findStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return findStructureCandidatesByFeatureIdAndFormulaId(CsiStructureMatch.class, formulaId, alignedFeatureId, pageable, optFields);
    }

    @Override
    public Page<StructureCandidateScored> findDeNovoStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return findStructureCandidatesByFeatureIdAndFormulaId(DenovoStructureMatch.class, formulaId, alignedFeatureId, pageable, optFields);
    }

    private <T extends StructureMatch> Page<StructureCandidateScored> findStructureCandidatesByFeatureIdAndFormulaId(Class<T> clzz, String formulaId, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        long longAFId = Long.parseLong(alignedFeatureId);
        long longFId = Long.parseLong(formulaId);
        Pair<String[], Database.SortOrder[]> sort = sortStructureMatch(pageable.getSort());
        List<StructureCandidateScored> candidates = project().findByFeatureIdAndFormulaIdStr(longAFId, longFId, clzz, pageable.getOffset(), pageable.getPageSize(), sort.getLeft()[0], sort.getRight()[0])
                .map(s -> convertStructureMatch(s, optFields)).map(s -> (StructureCandidateScored) s).toList();

        long total = project().countByFeatureId(longFId, clzz);

        return new PageImpl<>(candidates, pageable, total);
    }


    @Override
    public Page<StructureCandidateFormula> findStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return findStructureCandidatesByFeatureId(CsiStructureMatch.class, alignedFeatureId, pageable, optFields);
    }

    @Override
    public Page<StructureCandidateFormula> findDeNovoStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return findStructureCandidatesByFeatureId(DenovoStructureMatch.class, alignedFeatureId, pageable, optFields);
    }

    private <T extends StructureMatch> Page<StructureCandidateFormula> findStructureCandidatesByFeatureId(Class<T> clzz, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        long longAFId = Long.parseLong(alignedFeatureId);
        Pair<String[], Database.SortOrder[]> sort = sortStructureMatch(pageable.getSort());

        Long2ObjectMap<de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate> fidToFC = new Long2ObjectOpenHashMap<>();

        List<StructureCandidateFormula> candidates = project().findByFeatureIdStr(longAFId, clzz, pageable.getOffset(), pageable.getPageSize(), sort.getLeft()[0], sort.getRight()[0])
                .map(candidate -> {
                    de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate fc = fidToFC
                            .computeIfAbsent(candidate.getFormulaId(), k -> project()
                                    .findByFormulaIdStr(k, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class)
                                    .findFirst().orElseThrow());
                    return convertStructureMatch(fc.getMolecularFormula(), fc.getAdduct(), candidate, optFields);
                }).toList();

        long total = project().countByFeatureId(longAFId, clzz);
        return new PageImpl<>(candidates, pageable, total);
    }


    @Override
    public StructureCandidateScored findTopStructureCandidateByFeatureId(String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        long longAFId = Long.parseLong(alignedFeatureId);
        Pair<String[], Database.SortOrder[]> sort = sortStructureMatch(Sort.by(Sort.Direction.DESC, "csiScore"));
        return project().findByFeatureIdStr(longAFId, CsiStructureMatch.class, sort.getLeft()[0], sort.getRight()[0])
                .findFirst().map(s -> convertStructureMatch(s, optFields)).orElse(null);
    }

    @Override
    public StructureCandidateScored findStructureCandidateById(@NotNull String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        long longAFId = Long.parseLong(alignedFeatureId);
        long longFId = Long.parseLong(formulaId);
        CsiStructureMatch match = project().findByFeatureIdAndFormulaIdAndInChIStr(longAFId, longFId, inchiKey, CsiStructureMatch.class)
                .findFirst().orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Structure Candidate with InChIKey: " + inchiKey + "| formulaId: " + formulaId + "| alignedFeatureId: " + alignedFeatureId + " could not be found!"));
        return convertStructureMatch(match, optFields);
    }

    private StructureCandidateFormula convertStructureMatch(MolecularFormula molecularFormula, PrecursorIonType adduct, StructureMatch match, EnumSet<StructureCandidateScored.OptField> optFields) {
        StructureCandidateFormula sSum = convertStructureMatch(match, optFields);
        if (molecularFormula != null)
            sSum.setMolecularFormula(molecularFormula.toString());
        if (adduct != null)
            sSum.setAdduct(adduct.toString());
        return sSum;
    }

    private StructureCandidateFormula convertStructureMatch(StructureMatch match, EnumSet<StructureCandidateScored.OptField> optFields) {
        final StructureCandidateFormula sSum = new StructureCandidateFormula();
        //FP
        if (match.getCandidate() == null)
            project().fetchFingerprintCandidate(match, optFields.contains(StructureCandidateScored.OptField.fingerprint));

        if (optFields.contains(StructureCandidateScored.OptField.fingerprint))
            sSum.setFingerprint(AnnotationUtils.asBinaryFingerprint(match.getCandidate().getFingerprint()));

        sSum.setFormulaId(String.valueOf(match.getFormulaId()));
        sSum.setRank(match.getStructureRank());
        // scores
        sSum.setCsiScore(match.getCsiScore());
        sSum.setTanimotoSimilarity(match.getTanimotoSimilarity());

        if (match instanceof CsiStructureMatch csi)
            sSum.setMcesDistToTopHit(csi.getMcesDistToTopHit());
//        else if (match instanceof DenovoStructureMatch mn)
        //todo do we want to add dnn score for denovo?


        //Structure information
        //check for "null" strings since the database might not be perfectly curated
        final String n = match.getCandidate().getName();
        if (n != null && !n.isEmpty() && !n.equals("null"))
            sSum.setStructureName(n);

        sSum.setSmiles(match.getCandidate().getSmiles());
        sSum.setInchiKey(match.getCandidateInChiKey());
        sSum.setXlogP(match.getCandidate().getXlogp());

        //meta data
        if (optFields.contains(StructureCandidateScored.OptField.dbLinks))
            sSum.setDbLinks(match.getCandidate().getLinks());

        // spectral library matches
        if (optFields.contains(StructureCandidateScored.OptField.libraryMatches)) {
            List<SpectralLibraryMatch> libraryMatches = project().findByInChIStr(sSum.getInchiKey(), SpectraMatch.class)
                    .map(SpectralLibraryMatch::of).toList();
            sSum.setSpectralLibraryMatches(libraryMatches);
        }

        return sSum;
    }

    @Override
    public AnnotatedSpectrum findAnnotatedSpectrumByStructureId(int specIndex, @Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId) {
        long longFId = Long.parseLong(formulaId);
        long longAFId = Long.parseLong(alignedFeatureId);
        return findAnnotatedMsMsSpectrum(specIndex, inchiKey, longFId, longAFId);
    }

    @SneakyThrows
    private AnnotatedSpectrum findAnnotatedMsMsSpectrum(int specIndex, @Nullable String inchiKey, long formulaId, long alignedFeatureId) {
        //todo we want to do this without ms2 experiment
        Ms2Experiment exp = project().findAlignedFeatureAsMsExperiment(alignedFeatureId)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Could not load ms data needed to create annotated spectrum for id: " + alignedFeatureId));

        FTree ftree = project().findByFormulaIdStr(formulaId, FTreeResult.class).findFirst().map(FTreeResult::getFTree)
                .orElse(null);

        //todo we retrieve the complete candidate just for the smile. Maybe add smiles to match?
        String smiles = storage().getByPrimaryKey(inchiKey, FingerprintCandidate.class)
                .map(CompoundCandidate::getSmiles)
                .orElse(null);

        if (specIndex < 0)
            return Spectrums.createMergedMsMsWithAnnotations(exp, ftree, smiles);
        else
            return Spectrums.createMsMsWithAnnotations(exp.getMs2Spectra().get(specIndex), ftree, smiles);
    }

    @SneakyThrows
    @Override
    public AnnotatedMsMsData findAnnotatedMsMsDataByStructureId(@Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId) {
        long longFId = Long.parseLong(formulaId);
        long longAFId = Long.parseLong(alignedFeatureId);

        //todo we want to do this without ms2 experiment
        Ms2Experiment exp = project().findAlignedFeatureAsMsExperiment(longAFId)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Could not load ms data needed to create annotated spectrum for id: " + alignedFeatureId));

        FTree ftree = project().findByFormulaIdStr(longFId, FTreeResult.class).findFirst().map(FTreeResult::getFTree)
                .orElse(null);

        //todo we retrieve the complete candidate just for the smile. Maybe add smiles to match?
        String smiles = storage().getByPrimaryKey(inchiKey, FingerprintCandidate.class)
                .map(CompoundCandidate::getSmiles)
                .orElse(null);

        return AnnotatedMsMsData.of(exp, ftree, smiles);
    }

    @SneakyThrows
    @Override
    public String getFingerIdDataCSV(int charge) {
        Optional<FingerIdData> dataOpt = projectSpaceManager.getProject().findFingerprintData(FingerIdData.class, charge);
        if (dataOpt.isEmpty())
            return null;
        StringWriter writer = new StringWriter();
        FingerIdData.write(writer, dataOpt.get(), true); //sneaky throws because it's a string writer and no real io.
        return writer.toString();
    }

    @SneakyThrows
    @Override
    public String getCanopusClassyFireDataCSV(int charge) {
        Optional<CanopusCfData> dataOpt = projectSpaceManager.getProject().findFingerprintData(CanopusCfData.class, charge);
        if (dataOpt.isEmpty())
            return null;
        StringWriter writer = new StringWriter();
        CanopusCfData.write(writer, dataOpt.get()); //sneaky throws because it's a string writer and no real io.
        return writer.toString();
    }

    @SneakyThrows
    @Override
    public String getCanopusNpcDataCSV(int charge) {
        Optional<CanopusNpcData> dataOpt = projectSpaceManager.getProject().findFingerprintData(CanopusNpcData.class, charge);
        if (dataOpt.isEmpty())
            return null;
        StringWriter writer = new StringWriter();
        CanopusNpcData.write(writer, dataOpt.get()); //sneaky throws because it's a string writer and no real io.
        return writer.toString();
    }

    @SneakyThrows
    @Override
    public String findSiriusFtreeJsonById(String formulaId, String alignedFeatureId) {
        long formId = Long.parseLong(formulaId);
        return project().findByFormulaIdStr(formId, FTreeResult.class).findFirst()
                .map(ftreeRes -> {
                    if (ftreeRes.getAlignedFeatureId() != Long.parseLong(alignedFeatureId))
                        throw new ResponseStatusException(BAD_REQUEST, "Tree exists but FormulaID does not belong to the requested FeatureID. Are you using the correct Ids?");
                    return new FTJsonWriter().treeToJsonString(ftreeRes.getFTree());
                }).orElse(null);
    }
}
