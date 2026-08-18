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

import de.unijena.bioinf.ChemistryBase.ms.lcms.MsDataSourceReference;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.fp.ClassyFireFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.NPCFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.Partition;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.configs.ColorGenerator;
import de.unijena.bioinf.ms.middleware.Pages;
import de.unijena.bioinf.ms.middleware.model.annotations.*;
import de.unijena.bioinf.ms.middleware.model.annotations.CanopusPrediction;
import de.unijena.bioinf.ms.middleware.model.annotations.FormulaCandidate;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.compute.InstrumentProfile;
import de.unijena.bioinf.ms.middleware.model.features.*;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import de.unijena.bioinf.ms.middleware.model.spectra.Spectrums;
import de.unijena.bioinf.ms.middleware.model.statistics.FoldChange;
import de.unijena.bioinf.ms.middleware.model.statistics.Statistics;
import de.unijena.bioinf.ms.middleware.model.statistics.StatisticsTable;
import de.unijena.bioinf.ms.middleware.model.statistics.StatisticsType;
import de.unijena.bioinf.ms.middleware.model.tags.*;
import de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils;
import de.unijena.bioinf.ms.middleware.service.search.SearchService;
import de.unijena.bioinf.ms.middleware.service.search.description.FieldVocabulary;
import de.unijena.bioinf.ms.middleware.service.search.description.IndexFacts;
import de.unijena.bioinf.ms.middleware.service.search.description.SearchableFieldService;
import de.unijena.bioinf.ms.middleware.service.search.description.DetectedAdductPossibleValues;
import de.unijena.bioinf.ms.middleware.service.search.description.TagDefinitionPossibleValues;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectDetectedAdducts;
import de.unijena.bioinf.ms.middleware.service.search.dynamic.Taggable;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.*;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.networks.AdductNetwork;
import de.unijena.bioinf.ms.persistence.model.core.networks.AdductNode;
import de.unijena.bioinf.ms.persistence.model.core.run.*;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import de.unijena.bioinf.ms.persistence.model.core.statistics.AggregationType;
import de.unijena.bioinf.ms.persistence.model.core.statistics.QuantMeasure;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueDefinition;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueFormatter;
import de.unijena.bioinf.ms.persistence.model.core.tags.ValueType;
import de.unijena.bioinf.ms.persistence.model.core.trace.*;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectSourceFormats;
import de.unijena.bioinf.ms.persistence.model.properties.ProjectType;
import de.unijena.bioinf.ms.persistence.model.sirius.*;
import de.unijena.bioinf.ms.persistence.storage.ProjectSchemaMigrator;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.exceptions.ProjectTypeException;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.*;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.similarity.LongestCommonSubsequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.awt.*;
import java.net.URI;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.stream.Stream;

import static de.unijena.bioinf.ChemistryBase.utils.Utils.LARGE_BATCH_SIZE;
import static de.unijena.bioinf.ms.middleware.Pages.*;
import static de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils.convertToFeatureQuality;
import static de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils.convertToQualityMap;
import static org.springframework.http.HttpStatus.*;


@Slf4j
public class NoSQLProjectImpl implements Project<NoSQLProjectSpaceManager> {
    private static final LongestCommonSubsequence lcs = new LongestCommonSubsequence();

    @NotNull
    private final String projectId;

    @NotNull
    private final NoSQLProjectSpaceManager projectSpaceManager;

    @Getter
    private final SearchService searchService;

    /**
     * Describes this project's searchable fields. Built here because the vocabularies it reports are project
     * state: the tag definitions stored in this project and the adducts its imports detected. Both are read
     * when the fields are described, so a definition extended or an import run later shows up right away.
     */
    @Getter
    private final SearchableFieldService searchableFieldService;

    /**
     * Per-project lock serializing this project's search-index build/update/remove operations against each
     * other (they do check-then-act on the same index). It is intentionally NOT the shared searchService
     * monitor: different projects have independent indices and must be able to (re)build concurrently.
     */
    private final Object searchIndexLock = new Object();

    private final @NotNull BiFunction<Project<?>, String, Boolean> computeStateProvider;

    @SneakyThrows
    public NoSQLProjectImpl(@NotNull String projectId, @NotNull NoSQLProjectSpaceManager projectSpaceManager, SearchService searchService, @NotNull BiFunction<Project<?>, String, Boolean> computeStateProvider) {
        this.projectId = projectId;
        this.projectSpaceManager = projectSpaceManager;
        this.computeStateProvider = computeStateProvider;
        this.searchService = searchService;
        this.searchableFieldService = searchService == null ? null : new SearchableFieldService(
                IndexFacts.of(searchService, projectId),
                FieldVocabulary.firstOf(
                        new TagDefinitionPossibleValues(tagName -> project().findTagDefinitionByName(tagName)),
                        new DetectedAdductPossibleValues(() -> project().findDetectedAdducts()
                                .map(ProjectDetectedAdducts::getDetectedAdducts).orElse(null))));

        if (this.searchService != null) {
            synchronized (searchIndexLock) {
                try {
                    //todo fix event actions so that new tags are added to features
                    //todo think whether we want store tags on the tagged object because we have lucene index..
                    searchService.openOrCreateProjectIndex(this);
                    // Upgrade legacy ("pre-index") projects in-place before building the index. Old projects may
                    // lack fields the index relies on (e.g. hasMs1/hasMsMs, project detected adducts); without
                    // this backfill the default feature filter would hide everything until a manual filter reset.
                    // If the migration rewrote index-relevant feature data, force a rebuild so a possibly stale
                    // index (built by an earlier version from the missing/default values) is regenerated.
                    createSearchIndex(ProjectSchemaMigrator.migrateIfNeeded(project()));

                    //handle tag valuetype cache
                    storage().onInsert(de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition.class,
                            tagDef -> searchService.addTagValueType(projectId, tagDef.getTagName(), tagDef.getValueType()));
                    storage().onRemove(de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition.class,
                            tagDef -> searchService.removeTagValueType(projectId, tagDef.getTagName()));


                } catch (Exception e) {
                    log.error("Error while initializing project space index. Search will be unavailable for this project.", e);
                    try {
                        searchService.closeProjectIndex(this);
                    } catch (Exception closeError) {
                        log.error("Error while closing partially initialized project search index.", closeError);
                    }
                }
            }
        }
    }

    //todo should we be able to cancel this?
    //todo does parallelization improve performance?

    @SneakyThrows
    @Override
    public void createSearchIndex(boolean force) {
        if (searchService != null) {
            synchronized (searchIndexLock) {
                if (force)
                    searchService.clearIndex(this);

                //todo finde good page size.
                //load feature index in pages to have content memory consumption
                if (searchService.isEmpty(projectId, AlignedFeature.class)) {
                    // Mark incomplete for the duration of the (re)build so an interrupted build is not
                    // persisted and gets rebuilt on the next open (M5).
                    searchService.setIndexComplete(projectId, AlignedFeature.class, false);
                    Pages.forEach(
                            pageable -> findAlignedFeatures(pageable, false, AlignedFeature.INDEXED_OPT_FIELDS),
                            page -> searchService.addDocuments(projectId, page.getContent())
                    );
                    searchService.setIndexComplete(projectId, AlignedFeature.class, true);
                }

                //load Run index in pages to have content memory consumption
                if (searchService.isEmpty(projectId, Run.class)) {
                    searchService.setIndexComplete(projectId, Run.class, false);
                    Pages.forEach(
                            pageable -> findRuns(pageable, Run.OptField.tags),
                            page -> searchService.addDocuments(projectId, page.getContent())
                    );
                    searchService.setIndexComplete(projectId, Run.class, true);
                }

                //load compound index
                //todo remuse features.
                //todo we need to store analyzers and point configs.
//                Page<Compound> compounds = findCompounds(Pageable.unpaged(), Compound.OptField.tags);
//                if (compounds.hasContent())
//                    searchService.getSearchIndexWriter().addBeans(projectId,
//                            compounds.getContent());

            }
        }
    }


    /**
     * Add newly imported data into search index.
     * Data to index will be requested from project based on the given aligned feature ids (e.g. runs, compounds, results)
     *
     * @param alignedFeaturesToUpdate ids of the features to be added.
     */
    @Override
    public void addToSearchIndex(@Nullable Collection<String> alignedFeaturesToUpdate, @Nullable Collection<String> runIds) {
        addToSearchIndexLongIds(
                alignedFeaturesToUpdate == null ? null : alignedFeaturesToUpdate.stream().filter(Objects::nonNull).map(Long::parseLong).toList(),
                runIds == null ? null : runIds.stream().filter(Objects::nonNull).map(Long::parseLong).toList()

        );
    }

    @SneakyThrows
    public void addToSearchIndexLongIds(@Nullable Collection<Long> alignedFeaturesToUpdate, @Nullable Collection<Long> runIds) {

        if (searchService != null) {
            synchronized (searchIndexLock) {
                //Handle FEATURES
                if (Utils.notNullOrEmpty(alignedFeaturesToUpdate)) {
                    Partition<Long> partition = Partition.ofSize(alignedFeaturesToUpdate.stream().sorted().toList(), LARGE_BATCH_SIZE);
                    for (List<Long> ids : partition) {
                        List<AlignedFeature> alfs = findAlignedFeaturesByIds(ids, false, EnumSet.of(AlignedFeature.OptField.qualities));
                        searchService.addDocuments(projectId, alfs);
                    }
                }

                //Handle COMPOUNDS
                //todo IMPLEMENT!

                //Handle Runs
                if (Utils.notNullOrEmpty(runIds)) {
                    List<Run> runsToUpdate = storage().findAllByPrimaryKeyStr(runIds, LCMSRun.class)
                            .map(run -> convertToApiRun(run, EnumSet.of(Run.OptField.tags))) //tag might have been added during preprocessing.
                            .toList();
                    searchService.addDocuments(projectId, runsToUpdate);
                }
            }
        }

    }


    /**
     * Add newly imported data into search index.
     * Data to index will be requested from project based on the given aligned feature ids (e.g. runs, compounds, results)
     *
     * @param alignedFeaturesToUpdate ids of the features to be added.
     */
    @Override
    public void updateSearchIndex(Collection<String> alignedFeaturesToUpdate) {
        updateSearchIndexLongIds(alignedFeaturesToUpdate.stream().filter(Objects::nonNull).map(Long::parseLong).toList());

    }

    @SneakyThrows
    public void updateSearchIndexLongIds(Collection<Long> alignedFeaturesToUpdate) {
        if (searchService != null) {
            synchronized (searchIndexLock) {
                LongList idsToUpdate = alignedFeaturesToUpdate.stream().sorted().collect(Collectors.toCollection(LongArrayList::new));

                // request results from db.
                if (!idsToUpdate.isEmpty()) {
                    Partition<Long> partition = Partition.ofSize(idsToUpdate, LARGE_BATCH_SIZE);
                    for (List<Long> ids : partition) {
                        List<AlignedFeature> alfs = findAlignedFeaturesByIds(ids, false, AlignedFeature.INDEXED_OPT_FIELDS);
                        searchService.updateDocuments(projectId, alfs);
                    }
                }
            }
        }

    }

    @Override
    public void removeFromSearchIndex(@Nullable Collection<String> alignedFeaturesIds, @Nullable Collection<String> compoundIds, @Nullable Collection<String> runIds) {
        removeFromSearchIndexLongIds(
                alignedFeaturesIds == null ? null : alignedFeaturesIds.stream().filter(Objects::nonNull).map(Long::parseLong).toList(),
                compoundIds == null ? null : compoundIds.stream().filter(Objects::nonNull).map(Long::parseLong).toList(),
                runIds == null ? null : runIds.stream().filter(Objects::nonNull).map(Long::parseLong).toList()
        );
    }

    @SneakyThrows
    public void removeFromSearchIndexLongIds(@Nullable Collection<Long> alignedFeaturesIds, @Nullable Collection<Long> compoundIds, @Nullable Collection<Long> runIds) {
        if (searchService != null) {
            synchronized (searchIndexLock) {
                // Delete by id per POJO type. removeDocuments(beans) would infer the index type from
                // the element type (Long) and thus target a bogus index, so we must use removeDocumentsById.
                if (Utils.notNullOrEmpty(alignedFeaturesIds))
                    searchService.removeDocumentsById(projectId, alignedFeaturesIds, AlignedFeature.class);
                if (Utils.notNullOrEmpty(compoundIds))
                    searchService.removeDocumentsById(projectId, compoundIds, Compound.class);
                if (Utils.notNullOrEmpty(runIds))
                    searchService.removeDocumentsById(projectId, runIds, Run.class);
            }
        }
    }


    //using private methods instead of references for easier refactoring or changes.
    // compiler will inline the method call since projectmanager is final.
    public Database<?> storage() {
        return projectSpaceManager.getProject().getStorage();
    }

    public SiriusProjectDocumentDatabase<? extends Database<?>> project() {
        return projectSpaceManager.getProject();
    }

    @Override
    public @NotNull String getSystemUID() {
        return projectSpaceManager.getProject().getStorage().systemUID();
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
    public Optional<QuantTable> getFeatureQuantification(@Nullable String searchQuery, QuantMeasure type,
                                                         EnumSet<QuantTable.OptField> optFields) {
        if (searchService == null)
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Cannot perform search query. Search service not available!");

        //quantifying everything is a selection too: a feature of an isotopic alignment references it in the very same
        //field as an ordinary feature, so without the aligned feature ids the isotopes would become rows of their own
        List<Long> alignedFeatureIds = searchService.searchIds(projectId, searchQuery, Pageable.unpaged(), AlignedFeature.class)
                .stream().map(Long::parseLong).toList();

        return fillQuantTable(alignedFeatureIds, Feature::getAlignedFeatureId, QuantRowType.FEATURES, type, optFields);
    }

    private static final String ALIGNED_FEATURE_ID_FIELD = "alignedFeatureId";
    private static final String RUN_ID_FIELD = "runId";
    private static final String RUN_NAME_FIELD = "name";
    private static final String RUN_SOURCE_FIELD = "source";
    private static final String COMPOUND_ID_FIELD = "compoundId";

    @SneakyThrows
    @Override
    public Optional<QuantTable> getCompoundQuantification(@Nullable String searchQuery, QuantMeasure type,
                                                          EnumSet<QuantTable.OptField> optFields) {
        //the query is validated against the supported subset, but selects aligned features by their compound id
        String indexQuery = null;
        if (Utils.notNullOrBlank(searchQuery)) {
            CompoundIdQuery selection = CompoundIdQuery.parse(searchQuery);
            if (selection.selectsNothing())
                return Optional.empty();
            if (!selection.selectsEverything())
                indexQuery = searchQuery;
        }

        if (searchService == null)
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Cannot perform search query. Search service not available!");

        //which compound a feature belongs to is read from the index, so no aligned features have to be loaded
        Map<Long, Long> compoundByAlignedFeature = searchService
                .searchFields(projectId, indexQuery, Pageable.unpaged(), AlignedFeature.class,
                        Set.of(ALIGNED_FEATURE_ID_FIELD, COMPOUND_ID_FIELD),
                        fields -> Pair.of(fields.getLong(ALIGNED_FEATURE_ID_FIELD), fields.getLong(COMPOUND_ID_FIELD)))
                .stream()
                .filter(pair -> pair.getLeft() != null && pair.getRight() != null) //features without a compound
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

        return fillQuantTable(compoundByAlignedFeature.keySet(),
                feature -> compoundByAlignedFeature.get(feature.getAlignedFeatureId()),
                QuantRowType.COMPOUNDS, type, optFields);
    }

    /**
     * Loads the features of the given aligned features. The 'in' filter looks every id up in the index of the feature
     * collection, so the cost grows with the number of requested aligned features and not with the project size.
     */
    /**
     * Reads the features of the given aligned features and accumulates them into the rows of a quantification table.
     * <p>
     * Every aligned feature is asked for on its own, and the ids are handed out from one cursor so that the workers
     * stay close to each other in the index and a feature with many detections does not hold up a worker that is
     * already done. An 'in' filter over many ids looks like the obvious alternative but costs hundreds of times more
     * per call, so many small queries are cheaper than few large ones, and they parallelize.
     *
     * @param rowOf assigns a feature to the row it is quantified in, its aligned feature or its compound
     */
    private Map<Long, double[]> readRows(Collection<Long> alignedFeatureIds, Function<Feature, Long> rowOf,
                                         QuantMeasure type, Object2IntMap<String> columnOfRun, int columns) {
        if (alignedFeatureIds.isEmpty())
            return Map.of();

        //ascending, so the workers walk the index in one direction instead of jumping around in it
        Long[] ids = alignedFeatureIds.stream().sorted().toArray(Long[]::new);
        Map<Long, double[]> rows = new ConcurrentHashMap<>();
        AtomicInteger cursor = new AtomicInteger();

        BasicMasterJJob<Object> reader = new BasicMasterJJob<>(JJob.JobType.CPU) {
            @Override
            protected Object compute() throws Exception {
                int workers = Math.max(1, Math.min(ids.length, SiriusJobs.getGlobalJobManager().getCPUThreads()));
                for (int i = 0; i < workers; i++)
                    submitSubJob(new BasicJJob<Object>(JJob.JobType.CPU) {
                        @Override
                        protected Object compute() throws Exception {
                            int index;
                            while ((index = cursor.getAndIncrement()) < ids.length) {
                                checkForInterruption();
                                accumulateFeaturesOf(ids[index], rows, rowOf, type, columnOfRun, columns);
                            }
                            return Boolean.TRUE;
                        }
                    });
                awaitAllSubJobs();
                return Boolean.TRUE;
            }
        };
        //an interactive read should not wait behind a queue of computations, but it does not outrank what the user
        //explicitly asked to run now either
        reader.setPriority(JJob.JobPriority.HIGH);
        SiriusJobs.getGlobalJobManager().submitJob(reader).takeResult();

        return rows;
    }

    @SneakyThrows
    private void accumulateFeaturesOf(long alignedFeatureId, Map<Long, double[]> rows, Function<Feature, Long> rowOf,
                                      QuantMeasure type, Object2IntMap<String> columnOfRun, int columns) {
        try (Stream<Feature> features = storage()
                .findStr(Filter.where("alignedFeatureId").eq(alignedFeatureId), Feature.class)) {
            features.forEach(feature -> {
                if (feature.getRunId() == null)
                    return; //features of a project that was imported from preprocessed data have no run

                int column = columnOfRun.getInt(Long.toString(feature.getRunId()));
                Long row = rowOf.apply(feature);
                if (column < 0 || row == null)
                    return;

                double[] quantities = rows.computeIfAbsent(row, id -> nanRow(columns));
                double quantity = switch (type) {
                    case APEX_INTENSITY -> feature.getApexIntensity();
                    case AREA_UNDER_CURVE -> feature.getAreaUnderCurve();
                };
                //a compound row collects the features of several aligned features, so it is written by several workers
                synchronized (quantities) {
                    quantities[column] = Double.isNaN(quantities[column]) ? quantity : quantities[column] + quantity;
                }
            });
        }
    }

    /**
     * Adds the quantities of the given features to the table they belong to. The features are accumulated into the
     * table right away, so that only the table itself is held in memory and not the features of the whole project.
     *
     * @param rowOf assigns a feature to the row it is quantified in, its aligned feature or its compound
     * @return the quantification table or nothing if the project has no runs or nothing could be quantified
     */
    private Optional<QuantTable> fillQuantTable(Collection<Long> alignedFeatureIds, Function<Feature, Long> rowOf,
                                                QuantRowType rowType, QuantMeasure type,
                                                EnumSet<QuantTable.OptField> optFields) {
        Optional<QuantTable> table = initQuantTable(type, rowType, optFields);
        if (table.isEmpty())
            return Optional.empty();

        String[] columnIds = table.get().getColumnIds();
        Object2IntMap<String> columnOfRun = new Object2IntOpenHashMap<>(columnIds.length);
        columnOfRun.defaultReturnValue(-1);
        for (int i = 0; i < columnIds.length; i++)
            columnOfRun.put(columnIds[i], i);

        Map<Long, double[]> rows = readRows(alignedFeatureIds, rowOf, type, columnOfRun, columnIds.length);

        // nothing in this project belongs to an LC/MS run, so there is nothing to quantify
        if (rows.isEmpty())
            return Optional.empty();

        //rows are ordered by id, like the columns are
        List<Long> rowIds = rows.keySet().stream().sorted().toList();

        table.get().setValues(rowIds.stream().map(rows::get).toArray(double[][]::new));
        table.get().setRowIds(rowIds.stream().map(id -> Long.toString(id)).toArray(String[]::new));

        return table;
    }

    private static double[] nanRow(int columns) {
        double[] row = new double[columns];
        Arrays.fill(row, Double.NaN);
        return row;
    }

    private Optional<QuantTable> initQuantTable(QuantMeasure type, QuantRowType rowType,
                                               EnumSet<QuantTable.OptField> optFields) {
        boolean withNames = optFields.contains(QuantTable.OptField.columnNames);
        boolean withSources = optFields.contains(QuantTable.OptField.columnSources);

        //the columns are the runs of the project, and their ids are all that is needed to read the table. Names and
        //files are read from the index along with them, so nothing has to be loaded unless they are asked for.
        Set<String> fields = new HashSet<>(Set.of(RUN_ID_FIELD));
        if (withNames) fields.add(RUN_NAME_FIELD);
        if (withSources) fields.add(RUN_SOURCE_FIELD);

        List<Column> columns = searchService
                .searchFields(projectId, null, Pageable.unpaged(), Run.class, fields,
                        f -> new Column(f.getString(RUN_ID_FIELD),
                                withNames ? f.getString(RUN_NAME_FIELD) : null,
                                withSources ? f.getString(RUN_SOURCE_FIELD) : null))
                .stream()
                .filter(c -> c.id() != null)
                //columns are ordered by run id, which is a number and must not be ordered as text
                .sorted(Comparator.comparingLong(c -> Long.parseLong(c.id())))
                .toList();

        if (columns.isEmpty())
            return Optional.empty();

        return Optional.of(QuantTable
                .builder()
                .rowType(rowType)
                .quantificationMeasure(type)
                .columnIds(columns.stream().map(Column::id).toArray(String[]::new))
                .columnNames(withNames ? columns.stream().map(Column::name).toArray(String[]::new) : null)
                .columnSources(withSources ? columns.stream().map(Column::source).toArray(String[]::new) : null)
                .build()
        );
    }

    /** one column of a quantification table: the run it refers to, with the optional properties that were requested */
    private record Column(String id, @Nullable String name, @Nullable String source) {
    }

    private double[] getQuantTableRow(Map<String, List<Feature>> features, QuantTable table) {
        double[] row = new double[table.getColumnIds().length];
        for (int i = 0; i < row.length; i++) {
            if (features.containsKey(table.getColumnIds()[i])) {
                row[i] = switch (table.getQuantificationMeasure()) {
                    case APEX_INTENSITY ->
                            features.get(table.getColumnIds()[i]).stream().mapToDouble(Feature::getApexIntensity).sum();
                    case AREA_UNDER_CURVE ->
                            features.get(table.getColumnIds()[i]).stream().mapToDouble(Feature::getAreaUnderCurve).sum();
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

        // get all samples in the project
        List<LCMSRun> allSamples = storage.findAllStr(LCMSRun.class, "runId", Database.SortOrder.ASCENDING).toList();
        long[] allSampleIds = allSamples.stream().mapToLong(AbstractLCMSRun::getRunId).toArray();
        // generate colors
        List<Color> allColors = ColorGenerator.generateColors(allSamples.size());

        List<LCMSRun> samples = new ArrayList<>();
        List<Color> colors = new ArrayList<>();
        for (int k = 0; k < features.size(); ++k) {
            int index = Arrays.binarySearch(allSampleIds, features.get(k).getRunId());
            samples.add(index >= 0 ? allSamples.get(index) : null);
            colors.add(index >= 0 ? allColors.get(index) : null);
            if (index >= 0)
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

                    trace.setColor(ColorGenerator.colorToCss(colors.get(k)));

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
            for (AlignedIsotopicFeatures g : mainFeature.getIsotopicFeatures().orElse(Collections.emptyList())) {
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
        int numOfColors = 1;
        if (mainFeature.getAdductNetworkId() != null) {
            Optional<AdductNetwork> maybeNetwork = storage.getByPrimaryKey(mainFeature.getAdductNetworkId(), AdductNetwork.class);
            if (maybeNetwork.isPresent()) {
                AdductNetwork network = maybeNetwork.get();
                for (AdductNode node : network.getNodes()) {
                    if (node.getAlignedFeatureId() == mainFeature.getAlignedFeatureId()) continue;
                    Optional<MergedTrace> tr = storage.getByPrimaryKey(node.getTraceId(), MergedTrace.class);
                    Optional<AlignedFeatures> fr = storage.getByPrimaryKey(node.getAlignedFeatureId(), AlignedFeatures.class);
                    if (tr.isPresent() && fr.isPresent()) {
                        traces.add(TraceSet.Trace.of(String.format(Locale.US, "[CORRELATED] m/z = %.4f", fr.get().getAverageMass()), merged, fr.get(), tr.get(), retentionTimeAxis));
                        numOfColors++;
                        offsets.add(tr.get().getScanIndexOffset());
                        storage.fetchAllChildren(fr.get(), "alignedFeatureId", "isotopicFeatures", AlignedIsotopicFeatures.class);
                        for (AlignedIsotopicFeatures g : fr.get().getIsotopicFeatures().orElse(Collections.emptyList())) {
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

        List<Color> allColors = ColorGenerator.generateColors(numOfColors);
        traces.getFirst().setColor(ColorGenerator.colorToCss(allColors.getFirst()));

        // choose a new color for each adduct,
        // desaturate the last adducts' color for isotopes
        Color color = allColors.getFirst();
        for (int i = 1, j = 0; i < traces.size(); i++) {
            if (!traces.get(i).getLabel().contains("[ISOTOPE]")) {
                color = allColors.get(++j);
            } else {
                color = ColorGenerator.desaturate(color);
            }
            traces.get(i).setColor(ColorGenerator.colorToCss(color));
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

        // get all samples in the project
        long[] allSampleIds = storage.findAllStr(LCMSRun.class, "runId", Database.SortOrder.ASCENDING).mapToLong(LCMSRun::getRunId).toArray();
        // generate colors
        List<Color> allColors = ColorGenerator.generateColors(allSampleIds.length);

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

            int colorIndex = Arrays.binarySearch(allSampleIds, sampleKey);
            trace.setColor(colorIndex >= 0 ? ColorGenerator.colorToCss(allColors.get(colorIndex)) : null);

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


    private Compound convertToApiCompound(de.unijena.bioinf.ms.persistence.model.core.Compound compound,
                                          boolean msDataSearchPrepared,
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
                .map(f -> convertToApiFeature(f, msDataSearchPrepared, mergedFeatureFields))).toList();
        builder.features(features);

        if (optFields.contains(Compound.OptField.consensusAnnotations))
            builder.consensusAnnotations(AnnotationUtils.buildConsensusAnnotationsCSI(features));
        if (optFields.contains(Compound.OptField.consensusAnnotationsDeNovo))
            builder.consensusAnnotationsDeNovo(AnnotationUtils.buildConsensusAnnotationsDeNovo(features));
        if (optFields.contains(Compound.OptField.customAnnotations))
            builder.customAnnotations(ConsensusAnnotationsCSI.builder().build()); //todo implement custom annotations -> storage needed
        if (optFields.contains(Compound.OptField.tags)) {
            builder.tags(findTagsByObject(compound.getClass(), compound.getCompoundId())
                    .collect(Collectors.toMap(Tag::getTagName, Function.identity())));
        }

        //remove optionals if not requested
        if (!optFeatureFields.contains(AlignedFeature.OptField.topAnnotations))
            features.forEach(f -> f.setTopAnnotations(null));
        if (!optFeatureFields.contains(AlignedFeature.OptField.topAnnotationsDeNovo))
            features.forEach(f -> f.setTopAnnotationsDeNovo(null));


        return builder.build();
    }

    @Nullable
    private de.unijena.bioinf.ms.persistence.model.core.Compound convertToProjectCompound(CompoundImport compoundImport, @Nullable InstrumentProfile profile) {
        List<AlignedFeatures> features = compoundImport.getFeatures().stream()
                .map(f -> convertToProjectFeature(f, profile))
                .filter(Objects::nonNull).toList();

        if (features.isEmpty()) {
            log.warn("Compound named '{}' does not contains a single supported feature. Skipping!", compoundImport.getName());
            return null;
        }

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

    @Nullable
    private AlignedFeatures convertToProjectFeature(FeatureImport featureImport, @Nullable InstrumentProfile profile) {
        try {
            AlignedFeatures.AlignedFeaturesBuilder<?, ?> builder = AlignedFeatures.builder()
                    .name(featureImport.getName())
                    .externalFeatureId(featureImport.getExternalFeatureId())
                    .averageMass(featureImport.getIonMass())
                    .detectedAdducts(FeatureImports.extractDetectedAdducts(featureImport));

            if (featureImport.getDataQuality() != null)
                builder.dataQuality(featureImport.getDataQuality());

            builder.charge((byte) featureImport.getCharge());

            MSData msData = FeatureImports.extractMsData(featureImport, profile);
            builder.msData(msData);
            builder.hasMs1(msData.getMergedMs1Spectrum() != null);
            builder.hasMsMs((msData.getMsnSpectra() != null && !msData.getMsnSpectra().isEmpty()) || (msData.getMergedMSnSpectrum() != null));

            builder.retentionTime(RetentionTime.of(featureImport.getRtStartSeconds(), featureImport.getRtEndSeconds(), featureImport.getRtApexSeconds()));

            return builder.build();
        } catch (IllegalArgumentException e) {
            log.warn("Error when parsing FeatureImport with id '{}'. Cause: {}", featureImport.getExternalFeatureId(), e.getMessage(), e);
            return null;
        }
    }


    public AlignedFeature convertToApiFeature(AlignedFeatures feature, boolean msDataAsCosineQuery) {
        return convertToApiFeature(feature, msDataAsCosineQuery, EnumSet.noneOf(AlignedFeature.OptField.class));
    }

    public AlignedFeature convertToApiFeature(AlignedFeatures feature, boolean msDataSearchPrepared, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        final String fid = String.valueOf(feature.getAlignedFeatureId());
        AlignedFeature.AlignedFeatureBuilder builder = AlignedFeature.builder()
                .alignedFeatureId(fid)
                .name(feature.getName())
                .externalFeatureId(feature.getExternalFeatureId())
                .compoundId(feature.getCompoundId() == null ? null : String.valueOf(feature.getCompoundId()))
                .ionMass(feature.getAverageMass())
                .quality(feature.getDataQuality() == null ? DataQuality.NOT_APPLICABLE : feature.getDataQuality())
                .hasMs1(feature.isHasMs1())
                .hasMsMs(feature.isHasMsMs())
                .computing(computeStateProvider.apply(this, fid))
                .charge(feature.getCharge());
        if (feature.getDetectedAdducts() != null) {
            de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts adducts = feature.getDetectedAdducts().clone();
            adducts.removeAllWithSource(DetectedAdducts.Source.SPECTRAL_LIBRARY_SEARCH);
            adducts.removeAllWithSource(DetectedAdducts.Source.MS1_PREPROCESSOR); //todo do not remove if detection runs during import.
            @NotNull Set<String> cleanedAdducts = adducts.getAllAdducts().stream()
                    .map(PrecursorIonType::toString)
                    .collect(Collectors.toSet());
            if (cleanedAdducts.isEmpty())
                cleanedAdducts.add(PrecursorIonType.unknown(feature.getCharge()).toString());
            builder.detectedAdducts(cleanedAdducts);
        } else {
            builder.detectedAdducts(Set.of(PrecursorIonType.unknown(feature.getCharge()).toString()));
        }
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
        return annotateApiFeature(feature.getAlignedFeatureId(), builder.build(), msDataSearchPrepared, optFields);
    }

    @SneakyThrows
    private AlignedFeature annotateApiFeature(long alignedFeatureId, AlignedFeature feature, boolean msDataSearchPrepared, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        //todo: we could use computedSubtool to decide whether we have to request results at all...

        if (optFields.contains(AlignedFeature.OptField.msData)) {
            if (feature.getMsData() == null)
                project().findByFeatureIdStr(alignedFeatureId, MSData.class).findAny()
                        .map(msd -> MsData.of(msd, msDataSearchPrepared))
                        .ifPresent(feature::setMsData);
        } else {
            feature.setMsData(null);
        }

        if (optFields.contains(AlignedFeature.OptField.topAnnotations)) {
            feature.setTopAnnotations(extractTopCsiAnnotations(alignedFeatureId));
        } else if (optFields.contains(AlignedFeature.OptField.topAnnotationsSummary)) { // fast confidence score retrieval without any additional data.
            if (feature.getTopAnnotations() == null)
                feature.setTopAnnotations(extractSearchIndexTopAnnotations(alignedFeatureId));
        } else {
            feature.setTopAnnotations(null);
        }

        if (optFields.contains(AlignedFeature.OptField.topAnnotationsDeNovo)) {
            if (feature.getTopAnnotationsDeNovo() == null)
                feature.setTopAnnotationsDeNovo(extractTopDeNovoAnnotations(alignedFeatureId));
        } else {
            feature.setTopAnnotationsDeNovo(null);
        }

        if (optFields.contains(AlignedFeature.OptField.computedTools)) {
            if (feature.getComputedTools() == null)
                feature.setComputedTools(
                        project().findByFeatureIdStr(alignedFeatureId, ComputedSubtools.class)
                                .findFirst().orElseGet(() -> ComputedSubtools.builder().build())
                );
        } else {
            feature.setComputedTools(null);
        }

        if (optFields.contains(AlignedFeature.OptField.qualities)) {
            if (feature.getQualities() == null) {
                if (feature.getQuality() == null || feature.getQuality() == DataQuality.NOT_APPLICABLE) {
                    feature.setQualities(Map.of());
                } else {
                    feature.setQualities(convertToQualityMap(findQualityReportById(alignedFeatureId)));
                }
            }
        } else {
            feature.setQualities(null);
        }

        if (optFields.contains(AlignedFeature.OptField.tags)) {
            if (feature.getTags() == null)
                feature.setTags(findTagsByObject(AlignedFeatures.class, alignedFeatureId)
                        .collect(Collectors.toMap(Tag::getTagName, Function.identity())));
        } else {
            feature.setTags(null);
        }
        return feature;
    }

    private de.unijena.bioinf.ms.middleware.model.features.Feature convertToApiUnalignedFeature(Feature feature) {
        de.unijena.bioinf.ms.middleware.model.features.Feature.FeatureBuilder builder = de.unijena.bioinf.ms.middleware.model.features.Feature.builder()
                .featureId(Long.toString(feature.getFeatureId()))
                .alignedFeatureId(Long.toString(feature.getAlignedFeatureId()))
                .averageMz(feature.getAverageMass())
                .apexMz(feature.getApexMass())
                .rtFwhmSeconds(feature.getFwhm())
                .apexIntensity(feature.getApexIntensity())
                .areaUnderCurve(feature.getAreaUnderCurve());

        //a feature of a project that was imported from preprocessed data belongs to no run
        if (feature.getRunId() != null)
            builder.runId(Long.toString(feature.getRunId()));

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

    /**
     * The file a run has been imported from, which is what relates a result back to the input data. Falls back to the
     * bare file name if the location is unknown. Null if the run does not know where it came from.
     */
    private static @Nullable String sourceOf(LCMSRun run) {
        MsDataSourceReference sourceReference = run.getSourceReference();
        if (sourceReference == null)
            return null;
        return sourceReference.getSource().map(URI::toString)
                .or(sourceReference::getFileName)
                .orElse(null);
    }

    @SneakyThrows
    public Run convertToApiRun(LCMSRun run, EnumSet<Run.OptField> optFields) {
        Run.RunBuilder builder = Run.builder()
                .runId(Long.toString(run.getRunId()))
                .name(run.getName());

        //the file this run has been imported from, so that results can be mapped back to the input data
        String source = sourceOf(run);
        if (source != null)
            builder.source(source);

        if (run.getChromatography() != null) builder.chromatography(run.getChromatography().getFullName());
        if (run.getFragmentation() != null) builder.fragmentation(run.getFragmentation().getFullName());
        if (run.getIonization() != null) builder.ionization(run.getIonization().getFullName());
        if (run.getMassAnalyzers() != null && !run.getMassAnalyzers().isEmpty())
            builder.massAnalyzers(run.getMassAnalyzers().stream().map(InstrumentConfig::getFullName).toList());

        if (optFields.contains(Run.OptField.tags)) {
            builder.tags(findTagsByObject(run.getClass(), run.getRunId())
                    .collect(Collectors.toMap(Tag::getTagName, Function.identity())));
        }

        return builder.build();
    }

    private Tag convertToApiTag(de.unijena.bioinf.ms.persistence.model.core.tags.Tag tag) {
        return Tag.builder().tagName(tag.getTagName()).value(tag.getValueFormatted()).build();
    }

    @SneakyThrows
    private TagDefinition convertToApiDefinition(de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition projectDefinition) {
        ValueFormatter<?, ?> formatter = projectDefinition.getValueDefinition().getValueType().getFormatter();
        ValueDefinition<?> valueDef = projectDefinition.getValueDefinition();
        return TagDefinition.builder()
                .tagName(projectDefinition.getTagName())
                .tagType(projectDefinition.getTagType())
                .description(projectDefinition.getDescription())
                .valueType(projectDefinition.getValueDefinition().getValueType())
                .possibleValues(valueDef.getPossibleValues()
                        .stream()
                        .map(formatter::toFormattedGeneric)
                        .collect(Collectors.toList()))
                .minValue(formatter.toFormattedGeneric(valueDef.getMinValue()))
                .maxValue(formatter.toFormattedGeneric(valueDef.getMaxValue()))
                .build();
    }

    private de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition convertToProjectDefinition(TagDefinitionImport tagDefinitionImport, boolean editable) {
        ValueFormatter<?, ?> formatter = tagDefinitionImport.getValueType().getFormatter();

        List<?> psConverted = tagDefinitionImport.getPossibleValues() == null ? null :
                tagDefinitionImport.getPossibleValues().stream().map(formatter::fromFormattedGeneric)
                        .collect(Collectors.toList());

        return de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition.builder()
                .tagName(tagDefinitionImport.getTagName())
                .tagType(tagDefinitionImport.getTagType())
                .description(tagDefinitionImport.getDescription())
                .editable(editable)
                .valueDefinition(new ValueDefinition<>(tagDefinitionImport.getValueType(),
                        psConverted,
                        formatter.fromFormattedGeneric(tagDefinitionImport.getMinValue()),
                        formatter.fromFormattedGeneric(tagDefinitionImport.getMaxValue())
                )).build();
    }

    private TagGroup convertToApiTagGroup(de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup group) {
        return TagGroup.builder()
                .groupName(group.getGroupName())
                .luceneQuery(group.getLuceneQuery())
                .groupType(group.getGroupType())
                .build();
    }

    public static FoldChange convertToApiFoldChange(de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange.AlignedFeaturesFoldChange foldChange) {
        return convertToApiFoldChange(foldChange, QuantRowType.FEATURES);
    }

    public static FoldChange convertToApiFoldChange(de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange.CompoundFoldChange foldChange) {
        return convertToApiFoldChange(foldChange, QuantRowType.COMPOUNDS);
    }

    private static FoldChange convertToApiFoldChange(de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange foldChange, QuantRowType quantRowType) {
        return FoldChange.builder()
                .quantType(quantRowType)
                .objectId(Long.toString(foldChange.getForeignId()))
                .leftGroup(foldChange.getLeftGroup())
                .rightGroup(foldChange.getRightGroup())
                .aggregation(foldChange.getAggregation())
                .quantification(foldChange.getQuantification())
                .foldChange(foldChange.getFoldChange())
                .leftAbundance(foldChange.getLeftAbundance())
                .rightAbundance(foldChange.getRightAbundance())
                .build();
    }

    @SneakyThrows
    private FeatureAnnotations extractTopCsiAnnotations(long longAFIf) {
        return extractTopAnnotations(longAFIf, CsiStructureMatch.class);
    }

    private FeatureAnnotations extractTopDeNovoAnnotations(long longAFIf) {
        return extractTopAnnotations(longAFIf, DenovoStructureMatch.class);
    }

    @SneakyThrows
    private FeatureAnnotations extractSearchIndexTopAnnotations(long longAFIf) {
        return extractSearchIndexTopAnnotations(longAFIf, storage().getByPrimaryKey(longAFIf, CsiStructureSearchResult.class)
                .orElse(null));
    }

    @SneakyThrows
    private FeatureAnnotations extractSearchIndexTopAnnotations(long longAFIf, @Nullable CsiStructureSearchResult structureSearchResult) {
        final FeatureAnnotations cSum = new FeatureAnnotations();

        de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate formulaCandidate = null;

        if (structureSearchResult != null) {
            cSum.setConfidenceExactMatch(structureSearchResult.getConfidenceExact());
            cSum.setConfidenceApproxMatch(structureSearchResult.getConfidenceApprox());
            cSum.setExpansiveSearchState(structureSearchResult.getExpansiveSearchConfidenceMode());
            cSum.setSpecifiedDatabases(structureSearchResult.getSpecifiedDatabases());
            cSum.setExpandedDatabases(structureSearchResult.getExpandedDatabases());
            cSum.setMatchedDatabases(structureSearchResult.getMatchedDatabases());

            StructureMatch structureMatch = project().findTopStructureMatchByFeatureId(longAFIf, CsiStructureMatch.class).orElse(null);

            formulaCandidate = storage().getByPrimaryKey(structureMatch.getFormulaId(), de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class)
                    .orElse(null);
            cSum.setStructureAnnotation(convertStructureMatch(structureMatch, EnumSet.noneOf(StructureCandidateScored.OptField.class)));
        }

        if (formulaCandidate == null)
            formulaCandidate = project().findTopFormulaCandidateByFeatureId(longAFIf).orElse(null);

        if (formulaCandidate != null) {
            cSum.setFormulaAnnotation(convertFormulaCandidate(formulaCandidate));

            storage().getByPrimaryKey(formulaCandidate.getFormulaId(), de.unijena.bioinf.ms.persistence.model.sirius.CanopusPrediction.class)
                    .map(cc -> CompoundClasses.of(cc.getNpcFingerprint(), cc.getCfFingerprint()))
                    .ifPresent(cSum::setCompoundClassAnnotation);
        }

        return cSum;
    }

    @SneakyThrows
    private FeatureAnnotations extractTopAnnotations(long longAFIf, @NotNull Class<? extends StructureMatch> clzz) {
        final FeatureAnnotations cSum = new FeatureAnnotations();

        @Nullable CsiStructureSearchResult structureSearchResult = null;
        if (clzz == CsiStructureMatch.class)
            structureSearchResult = storage().getByPrimaryKey(longAFIf, CsiStructureSearchResult.class)
                    .orElse(null);

        if (structureSearchResult != null) {
            cSum.setConfidenceExactMatch(structureSearchResult.getConfidenceExact());
            cSum.setConfidenceApproxMatch(structureSearchResult.getConfidenceApprox());
            cSum.setExpansiveSearchState(structureSearchResult.getExpansiveSearchConfidenceMode());
            cSum.setSpecifiedDatabases(structureSearchResult.getSpecifiedDatabases());
            cSum.setExpandedDatabases(structureSearchResult.getExpandedDatabases());
            cSum.setMatchedDatabases(structureSearchResult.getMatchedDatabases());
        }

        de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate formulaCandidate;

        StructureMatch structureMatch = (clzz != CsiStructureMatch.class || structureSearchResult != null)
                ? project().findTopStructureMatchByFeatureId(longAFIf, clzz).orElse(null)
                : null;

        if (structureMatch != null) {
            formulaCandidate = storage().getByPrimaryKey(structureMatch.getFormulaId(), de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class)
                    .orElseThrow();

            //set Structure match
            cSum.setStructureAnnotation(convertStructureMatch(structureMatch, EnumSet.of(StructureCandidateScored.OptField.dbLinks, StructureCandidateScored.OptField.libraryMatches)));
        } else {
            formulaCandidate = project().findTopFormulaCandidateByFeatureId(longAFIf).orElse(null);
        }

        //get Canopus result. either for
        if (formulaCandidate != null) {
            cSum.setFormulaAnnotation(convertFormulaCandidate(formulaCandidate));
            storage().getByPrimaryKey(formulaCandidate.getFormulaId(), de.unijena.bioinf.ms.persistence.model.sirius.CanopusPrediction.class)
                    .map(cc -> CompoundClasses.of(cc.getNpcFingerprint(), cc.getCfFingerprint()))
                    .ifPresent(cSum::setCompoundClassAnnotation);
        }
        return cSum;
    }

    private static final EnumSet<FormulaCandidate.OptField> needTree = EnumSet.of(
            FormulaCandidate.OptField.fragmentationTree, FormulaCandidate.OptField.annotatedSpectrum,
            FormulaCandidate.OptField.isotopePattern,
            FormulaCandidate.OptField.statistics
    );

    private FormulaCandidate convertFormulaCandidate(de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate candidate) {
        return convertFormulaCandidate(null, false, candidate, EnumSet.noneOf(FormulaCandidate.OptField.class));
    }

    @SneakyThrows
    private FormulaCandidate convertFormulaCandidate(@Nullable MSData msData, boolean msDataSearchPrepared, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate candidate, EnumSet<FormulaCandidate.OptField> optFields) {
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
                .zodiacScore(candidate.getZodiacScore())
                .lipidAnnotation(AnnotationUtils.asLipidAnnotation(candidate.getLipidSpecies()));

        //todo We need the scores in the gui without the tree -> do we want to store stats separately from the tree?
        final FTree ftree = optFields.stream().anyMatch(needTree::contains)
                ? storage().getByPrimaryKey(fid, FTreeResult.class).map(FTreeResult::getFTree).orElse(null)
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
            if (optFields.contains(FormulaCandidate.OptField.annotatedSpectrum))
                //todo this is not efficient an loads spectra a second time as well as the whole experiment. we need no change spectra annotation code to improve this.
                builder.annotatedSpectrum(findAnnotatedMsMsSpectrum(-1, null, candidate.getFormulaId(), candidate.getAlignedFeatureId(), msDataSearchPrepared));
            if (msData != null && optFields.contains(FormulaCandidate.OptField.isotopePattern)) {
                SimpleSpectrum isotopePattern = msData.getIsotopePattern();
                if (isotopePattern != null) {
                    builder.isotopePatternAnnotation(Spectrums.createIsotopePatternAnnotation(isotopePattern, ftree));
                }
            }
        }


        if (optFields.contains(FormulaCandidate.OptField.predictedFingerprint))
            storage().getByPrimaryKey(fid, CsiPrediction.class)
                    .map(fpp -> fpp.getFingerprint().toProbabilityArray()).ifPresent(builder::predictedFingerprint);


        if (optFields.contains(FormulaCandidate.OptField.canopusPredictions) || optFields.contains(FormulaCandidate.OptField.compoundClasses)) {
            storage().getByPrimaryKey(fid, de.unijena.bioinf.ms.persistence.model.sirius.CanopusPrediction.class)
                    .ifPresent(cr -> {
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
    public Page<Compound> findCompounds(@Nullable String searchQuery,
                                        Pageable pageable,
                                        boolean msDataSearchPrepared,
                                        @NotNull EnumSet<Compound.OptField> optFields,
                                        @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields) {
        if (searchQuery == null || searchQuery.isBlank())
            return findCompounds(pageable, msDataSearchPrepared, optFields, optFeatureFields);

        throw new ResponseStatusException(METHOD_NOT_ALLOWED, "Searching compounds is not yet supported!");


        /*
        if (searchService == null)
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Cannot perform search query. Search service not available!");

        Page<Compound> compounds = searchService.search(projectId, searchQuery, pageable, Compound.class);


       if (!EnumSet.of(Compound.OptField.tags).containsAll(optFields)) {
            List<Compound> cps = storage().findAllByPrimaryKeyStr(compounds.stream().map(Compound::getCompoundId).map(Long::parseLong).toList(), de.unijena.bioinf.ms.persistence.model.core.Compound.class)
                    .map(r -> convertToApiCompound(r, optFields, optFeatureFields)).toList();
            compounds = new PageImpl<>(cps, compounds.getPageable(), compounds.getTotalElements());
        }

        return compounds;*/
    }

    @SneakyThrows
    @Override
    public Page<Compound> findCompounds(Pageable pageable,
                                        boolean msDataSearchPrepared,
                                        @NotNull EnumSet<Compound.OptField> optFields,
                                        @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields
    ) {
        Stream<de.unijena.bioinf.ms.persistence.model.core.Compound> stream =
                findPageStr(storage(), de.unijena.bioinf.ms.persistence.model.core.Compound.class, pageable)
                        .peek(project()::fetchAdductFeatures);

        if (optFeatureFields.contains(AlignedFeature.OptField.msData))
            stream = stream.peek(c -> c.getAdductFeatures().ifPresent(features -> features.forEach(project()::fetchMsData)));

        List<Compound> compounds = stream.map(c -> convertToApiCompound(c, msDataSearchPrepared, optFields, optFeatureFields)).toList();

        long total = storage().countAll(de.unijena.bioinf.ms.persistence.model.core.Compound.class);

        return new PageImpl<>(compounds, pageable, total);
    }

    @SneakyThrows
    @Override
    public Page<Compound> findCompoundsByGroup(@NotNull String groupName,
                                               Pageable pageable,
                                               boolean msDataSearchPrepared,
                                               @NotNull EnumSet<Compound.OptField> optFields,
                                               @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields
    ) {
        Optional<de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup> tagGroup = storage().findStr(Filter.where("groupName").eq(groupName), de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup.class).findFirst();
        if (tagGroup.isEmpty())
            return Page.empty(pageable);

        if (searchService == null)
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Cannot perform group query. Search service not available!");

        // INTERMEDIATE SOLUTION: compound-level tagging and indexing are not supported yet, so a tag group
        // can only match aligned features. We resolve the group on the aligned-feature index and aggregate
        // the matching features up to their (distinct) compounds. Replace this with a real compound-level
        // search once compound-based tagging and indexing are implemented.
        List<String> matchingFeatureIds = searchService
                .searchIds(projectId, tagGroup.get().getLuceneQuery(), Pageable.unpaged(), AlignedFeature.class)
                .getContent();
        if (matchingFeatureIds.isEmpty())
            return Page.empty(pageable);

        Long[] featureIds = matchingFeatureIds.stream().map(Long::parseLong).toArray(Long[]::new);
        // alignedFeatureId is the AlignedFeatures primary key. For a modest match set, point-looking-up each id
        // (deserializing only the matches) beats a `Filter...in(...)`, which Nitrite resolves with a full
        // collection scan that deserializes every feature. But once the match set approaches the whole
        // collection the scan wins, so guard on the (O(1)) collection size and only fan out below half of it.
        // The point-lookup ids are sorted so both branches yield the same natural feature-id order.
        long totalFeatures = storage().countAll(AlignedFeatures.class);
        Stream<AlignedFeatures> matchingFeatures = (long) featureIds.length * 2 <= totalFeatures
                ? storage().findAllByPrimaryKeyStr(Arrays.stream(featureIds).sorted().toList(), AlignedFeatures.class)
                : storage().findStr(Filter.where("alignedFeatureId").in(featureIds), AlignedFeatures.class);
        // distinct compound ids of the matching features (stable paging)
        List<Long> compoundIds = matchingFeatures
                .map(AlignedFeatures::getCompoundId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (compoundIds.isEmpty())
            return Page.empty(pageable);

        List<Long> pageIds = pageable.isUnpaged()
                ? compoundIds
                : compoundIds.stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).toList();

        Map<Long, de.unijena.bioinf.ms.persistence.model.core.Compound> byId = storage()
                .findAllByPrimaryKeyStr(pageIds, de.unijena.bioinf.ms.persistence.model.core.Compound.class)
                .collect(Collectors.toMap(de.unijena.bioinf.ms.persistence.model.core.Compound::getCompoundId, Function.identity()));

        List<Compound> compounds = pageIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .peek(project()::fetchAdductFeatures)
                .peek(c -> {
                    if (optFeatureFields.contains(AlignedFeature.OptField.msData))
                        c.getAdductFeatures().ifPresent(features -> features.forEach(project()::fetchMsData));
                })
                .map(c -> convertToApiCompound(c, msDataSearchPrepared, optFields, optFeatureFields))
                .toList();

        return new PageImpl<>(compounds, pageable, compoundIds.size());
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
        } else {
            ps.upsertProjectType(ProjectType.DIRECT_IMPORT);
        }
    }

    @SneakyThrows
    @Override
    public List<Compound> addCompounds(@NotNull List<CompoundImport> compounds, InstrumentProfile profile, @NotNull EnumSet<Compound.OptField> optFields, @NotNull EnumSet<AlignedFeature.OptField> optFieldsFeatures, @NotNull String importSource) {
        setProjectTypeOrThrow(project());
        List<de.unijena.bioinf.ms.persistence.model.core.Compound> dbc = compounds.stream()
                .peek(ci -> {
                    //create a name from the longest common subsequence of all feature names if the compound name is null/blank.
                    if (Utils.isNullOrBlank(ci.getName())) {
                        ci.getFeatures().stream().map(FeatureImport::getName)
                                .filter(Objects::nonNull)
                                .filter(Predicate.not(String::isBlank))
                                .reduce((a, b) -> lcs.longestCommonSubsequence(a, b).toString())
                                .filter(Predicate.not(String::isBlank))
                                .ifPresent(ci::setName);
                    }
                }).map(ci -> convertToProjectCompound(ci, profile))
                .filter(Objects::nonNull)
                .toList();
        if (dbc.isEmpty())
            return Collections.emptyList();

        project().importCompounds(dbc);

        // specify the source of the direct import. e.g. to specify an explorer source.
        ProjectSourceFormats format = project().findProjectSourceFormats().map(m -> {
            m.addDirectImport(importSource);
            return m;
        }).orElse(ProjectSourceFormats.fromDirectImports(importSource));
        project().upsertProjectSourceFormats(format);

        // Data-import (SSE) events are fired by the REST controllers (CompoundController /
        // AlignedFeatureController) after this call returns; the shared service layer (also used by the
        // CLI, which has no event service) intentionally does not fire them.

        // Keep the search index in sync with the imported data. importCompounds() assigned ids to the
        // aligned features in-place, so we can collect them and add them to the index via the dedicated
        // add path (no-op if no search service is configured). This is the single indexing chokepoint for
        // direct imports: addAlignedFeatures() delegates here and must NOT index again (addDocument is
        // add-only and would create duplicate documents).
        addToSearchIndexLongIds(
                dbc.stream()
                        .map(c -> c.getAdductFeatures())
                        .flatMap(Optional::stream)
                        .flatMap(List::stream)
                        .map(AlignedFeatures::getAlignedFeatureId)
                        .toList(),
                null);

        return dbc.stream().map(c -> convertToApiCompound(c, false, optFields, optFieldsFeatures)).toList();
    }

    @SneakyThrows
    @Override
    public Compound findCompoundById(String compoundId, boolean msDataSearchPrepared, @NotNull EnumSet<Compound.OptField> optFields, @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields) {
        long id = Long.parseLong(compoundId);
        return storage().getByPrimaryKey(id, de.unijena.bioinf.ms.persistence.model.core.Compound.class)
                .map(c -> {
                    project().fetchAdductFeatures(c);
                    if (optFeatureFields.contains(AlignedFeature.OptField.msData)) {
                        c.getAdductFeatures().ifPresent(features -> features.forEach(project()::fetchMsData));
                    }
                    return convertToApiCompound(c, msDataSearchPrepared, optFields, optFeatureFields);
                })
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "There is no compound '" + compoundId + "' in project " + projectId + "."));
    }

    @SneakyThrows
    @Override
    public void deleteCompoundById(String compoundId) {
        // Collect the compound's aligned-feature ids before deletion; those are the documents in the
        // search index (the compound itself is not part of the searchable index).
        List<String> affectedFeatureIds = List.of();
        if (searchService != null) {
            try {
                Compound compound = findCompoundById(compoundId, false,
                        EnumSet.noneOf(Compound.OptField.class), EnumSet.noneOf(AlignedFeature.OptField.class));
                if (compound.getFeatures() != null)
                    affectedFeatureIds = compound.getFeatures().stream()
                            .map(AlignedFeature::getAlignedFeatureId)
                            .filter(Objects::nonNull)
                            .toList();
            } catch (ResponseStatusException ignored) {
                // compound does not exist -> nothing to remove from the index
            }
        }
        project().cascadeDeleteCompound(Long.parseLong(compoundId));
        removeFromSearchIndex(affectedFeatureIds, null, null);
    }


    @Override
    public AlignedFeatureQuality findAlignedFeaturesQualityById(String alignedFeatureId) {
        return convertToFeatureQuality(findQualityReportById(Long.parseLong(alignedFeatureId)));
    }

    @SneakyThrows
    @Override
    public Page<AlignedFeatureQuality> findAlignedFeaturesQuality(Pageable pageable) {
        return findPage(storage(), QualityReport.class, pageable)
                .map(AnnotationUtils::convertToFeatureQuality);
    }

    @SneakyThrows
    public QualityReport findQualityReportById(long alignedFeatureId) {
        return storage().getByPrimaryKey(alignedFeatureId, QualityReport.class)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Not Quality information found for feature '" + alignedFeatureId + "' in project " + projectId + "."));
    }

    @SneakyThrows
    @Override
    public Page<AlignedFeature> findAlignedFeatures(@Nullable String searchQuery, Pageable pageable, boolean msDataSearchPrepared, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        if (searchService == null)
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Cannot perform search query. Search service not available!");

        Page<AlignedFeature> features = searchService.search(projectId, searchQuery, pageable, AlignedFeature.class);

        if (features.isEmpty())
            return Page.empty(pageable);

        if (!AlignedFeature.INDEXED_OPT_FIELDS.equals(optFields)) {
            features.stream().parallel().forEach(f ->
                    annotateApiFeature(Long.parseLong(f.getAlignedFeatureId()), f, msDataSearchPrepared, optFields));
        }

        return features;
    }

    /**
     * Find features paged
     *
     * @param pageable
     * @param optFields
     * @return
     */
    @SneakyThrows
    @Override
    public Page<AlignedFeature> findAlignedFeatures(Pageable pageable, boolean msDataAsCosineQuery, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        List<AlignedFeature> features = annotateApiFeatures(
                pageable.isUnpaged()
                        ? storage().findAllStr(AlignedFeatures.class)
                        : storage().findAllStr(AlignedFeatures.class, pageable.getOffset(), pageable.getPageSize()),
                msDataAsCosineQuery,
                optFields);

        long total = pageable.isUnpaged() ? features.size() : storage().countAll(AlignedFeatures.class);

        return new PageImpl<>(features, pageable, total);
    }


    private List<AlignedFeature> findAlignedFeaturesByIds(Collection<Long> featureIds, boolean msDataAsCosineQuery, @NotNull EnumSet<AlignedFeature.OptField> optFields) throws IOException {
        return annotateApiFeatures(
                storage().findAllByPrimaryKeyStr(featureIds, AlignedFeatures.class),
                msDataAsCosineQuery,
                optFields);
    }

    private List<AlignedFeature> annotateApiFeatures(@NotNull Stream<AlignedFeatures> apifeatures, boolean msDataAsCosineQuery, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        //third
        Long2ObjectOpenHashMap<AlignedFeatures> dbalf = apifeatures.collect(Collectors.toMap(AlignedFeatures::getAlignedFeatureId, Function.identity(),
                (existing, replacement) -> existing, Long2ObjectOpenHashMap::new));

        if (dbalf.isEmpty())
            return List.of();

        Long[] ids = dbalf.keySet().stream().sorted().toArray(Long[]::new);

        TinyBackgroundJJob<Long2ObjectOpenHashMap<QualityReport>> qualJob = null;
        if (optFields.contains(AlignedFeature.OptField.qualities)) {
            Long[] filterIds = Arrays.stream(ids).filter(fid -> dbalf.get(fid).getDataQuality() != DataQuality.NOT_APPLICABLE).toArray(Long[]::new);
            if (filterIds.length > 0) {
                List<Long> qualIds = Arrays.asList(filterIds);
                qualJob = SiriusJobs.runInBackground(() -> storage().findAllByIndexedFieldStr("alignedFeatureId", qualIds, QualityReport.class)
                        .collect(Collectors.toMap(AlignedFeatureAnnotation::getAlignedFeatureId, c -> c
                                , (existing, replacement) -> existing, Long2ObjectOpenHashMap::new
                        )));
            }
        }


        TinyBackgroundJJob<Long2ObjectOpenHashMap<ComputedSubtools>> compJob = null;
        if (optFields.contains(AlignedFeature.OptField.computedTools) || optFields.contains(AlignedFeature.OptField.topAnnotationsSummary) || optFields.contains(AlignedFeature.OptField.topAnnotations)) {
            List<Long> compIds = Arrays.asList(ids);
            compJob = SiriusJobs.runInBackground(() -> storage().findAllByIndexedFieldStr("alignedFeatureId", compIds, ComputedSubtools.class)
                    .collect(Collectors.toMap(AlignedFeatureAnnotation::getAlignedFeatureId, c -> c
                            , (existing, replacement) -> existing, Long2ObjectOpenHashMap::new
                    )));
        }

        TinyBackgroundJJob<Long2ObjectOpenHashMap<Map<String, Tag>>> tagJob = null;
        if (optFields.contains(AlignedFeature.OptField.tags)) {
            tagJob = SiriusJobs.runInBackground(() -> {
                // Tags are stored with a taggedObjectId (not an alignedFeatureId) field; filtering on the
                // latter matched nothing, so this bulk query silently returned no tags and the per-feature
                // path (annotateApiFeature) was the only thing that ever populated them.
                final String afClass = AlignedFeatures.class.getName();
                final Long2ObjectOpenHashMap<Map<String, Tag>> tagmap = new Long2ObjectOpenHashMap<>();
                // One eq lookup per feature on the (taggedObjectClass, taggedObjectId) prefix of the Tag index,
                // instead of a taggedObjectId `in` that Nitrite would resolve with a full collection scan.
                for (Long id : ids) {
                    storage().find(Filter.and(
                            Filter.where("taggedObjectClass").eq(afClass),
                            Filter.where("taggedObjectId").eq(id)
                    ), de.unijena.bioinf.ms.persistence.model.core.tags.Tag.class).forEach(tag ->
                            tagmap.computeIfAbsent(tag.getTaggedObjectId(), k -> new HashMap<>())
                                    // convert like every other tag path so DATE/TIME values are returned formatted
                                    // (e.g. "yyyy-MM-dd"), not as the raw stored epoch millis.
                                    .put(tag.getTagName(), convertToApiTag(tag)));
                }
                return tagmap;
            });
        }

        TinyBackgroundJJob<Long2ObjectOpenHashMap<List<Statistics>>> statJob = null;
        if (optFields.contains(AlignedFeature.OptField.topAnnotationsSummary)) {
            statJob = SiriusJobs.runInBackground(() -> {
                final Long2ObjectOpenHashMap<List<Statistics>> statsMap = new Long2ObjectOpenHashMap<>();
                storage().findAllByIndexedFieldStr("alignedFeatureId", Arrays.asList(ids), de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange.AlignedFeaturesFoldChange.class)
                        .forEach(foldChange ->
                                statsMap.computeIfAbsent(foldChange.getAlignedFeatureId(), id -> new ArrayList<>())
                                        .add(convertToApiFoldChange(foldChange)));
                return statsMap;
            });
        }

        @NotNull Long2ObjectOpenHashMap<QualityReport> quality = qualJob != null ? qualJob.getResult() : new Long2ObjectOpenHashMap<>();
        @NotNull Long2ObjectOpenHashMap<ComputedSubtools> computed = compJob != null ? compJob.getResult() : new Long2ObjectOpenHashMap<>();
        @NotNull Long2ObjectOpenHashMap<Map<String, Tag>> tags = tagJob != null ? tagJob.getResult() : new Long2ObjectOpenHashMap<>();
        @NotNull Long2ObjectOpenHashMap<List<Statistics>> stats = statJob != null ? statJob.getResult() : new Long2ObjectOpenHashMap<>();


        TinyBackgroundJJob<Long2ObjectOpenHashMap<CsiStructureSearchResult>> csiJob = null;
        if (optFields.contains(AlignedFeature.OptField.topAnnotationsSummary) || optFields.contains(AlignedFeature.OptField.topAnnotations)) {
            Long[] filterIds = Arrays.stream(ids).filter(computed::containsKey).filter(id -> computed.get(id).hasResults()).toArray(Long[]::new);
            if (filterIds.length > 0) {
                List<Long> csiIds = Arrays.asList(filterIds);
                csiJob = SiriusJobs.runInBackground(() -> storage().findAllByIndexedFieldStr("alignedFeatureId", csiIds, CsiStructureSearchResult.class)
                        .collect(Collectors.toMap(AlignedFeatureAnnotation::getAlignedFeatureId, c -> c
                                , (existing, replacement) -> existing, Long2ObjectOpenHashMap::new
                        )));
            }
        }

        @NotNull Long2ObjectOpenHashMap<CsiStructureSearchResult> csires = csiJob != null ? csiJob.getResult() : new Long2ObjectOpenHashMap<>();

        List<AlignedFeature> features = dbalf.values().stream()
                .parallel()
                .map(alf -> {
                    long fid = alf.getAlignedFeatureId();
                    AlignedFeature apiFeture = convertToApiFeature(alf, msDataAsCosineQuery, EnumSet.noneOf(AlignedFeature.OptField.class));
                    apiFeture.setQualities(convertToQualityMap(quality.get(fid)));
                    apiFeture.setComputedTools(computed.get(fid));
                    apiFeture.setTags(tags.get(fid));
                    apiFeture.setStats(stats.get(fid));
                    if (optFields.contains(AlignedFeature.OptField.topAnnotationsSummary) && !optFields.contains(AlignedFeature.OptField.topAnnotations))
                        apiFeture.setTopAnnotations(extractSearchIndexTopAnnotations(fid, csires.get(fid)));
                    //add additional anotations that are not yes covered by bulk retrieval.
                    annotateApiFeature(fid, apiFeture, msDataAsCosineQuery, optFields);
                    return apiFeture;
                }).toList();

        return features;
    }

    /**
     * Imports features without compound grouping. Since grouping is unknown, each feature needs to belong to its own compound.
     * To group features as compounds together, please use add compounds instead.
     *
     * @param features  the features to be imported into the project
     * @param profile   the instrument the features have been measured on.
     * @param optFields opt fields to be returned as part of the imported features/
     * @return imported features with selected opt fields and UUIDs for features and compounds.
     */
    @Override
    public List<AlignedFeature> addAlignedFeatures(@NotNull List<FeatureImport> features, @Nullable InstrumentProfile profile, @NotNull EnumSet<AlignedFeature.OptField> optFields, @NotNull String importSource) {
        List<CompoundImport> cis = features.stream().map(f -> CompoundImport.builder().name(f.getName()).features(List.of(f)).build()).toList();
        // addCompounds() already keeps the search index in sync for the imported features; do not index
        // again here (addDocument is add-only and a second pass would create duplicate documents).
        List<AlignedFeature> importedFeatures = addCompounds(cis, profile, EnumSet.of(Compound.OptField.none), optFields, importSource).stream()
                .flatMap(c -> c.getFeatures().stream()).toList();
        return importedFeatures;
    }

    @SneakyThrows
    @Override
    public Page<AlignedFeature> findAlignedFeaturesByGroup(@NotNull String groupName, Pageable pageable, boolean msDataAsCosineQuery, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        Optional<TagGroup> tagGroup = storage().findStr(Filter.where("groupName").eq(groupName), TagGroup.class).findFirst();
        if (tagGroup.isEmpty())
            return Page.empty(pageable);
        return findAlignedFeatures(tagGroup.get().getLuceneQuery(), pageable, msDataAsCosineQuery, optFields);
    }

    @SneakyThrows
    @Override
    public AlignedFeature findAlignedFeaturesById(String alignedFeatureId, boolean msDataSearchPrepared, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        long id = Long.parseLong(alignedFeatureId);
        return storage().getByPrimaryKey(id, AlignedFeatures.class)
                .map(a -> convertToApiFeature(a, msDataSearchPrepared, optFields)).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "There is no aligned feature '" + alignedFeatureId + "' in project " + projectId + "."));
    }

    @SneakyThrows
    @Override
    public void deleteAlignedFeaturesById(String alignedFeatureId) {
        project().cascadeDeleteAlignedFeatures(Long.parseLong(alignedFeatureId));
        removeFromSearchIndex(List.of(alignedFeatureId), null, null);
    }

    @Override
    @SneakyThrows
    public void deleteAlignedFeaturesByIds(List<String> alignedFeatureIds) {
        project().cascadeDeleteAlignedFeatures(alignedFeatureIds.stream().map(Long::parseLong).sorted().toList());
        removeFromSearchIndex(alignedFeatureIds, null, null);
    }

    @Override
    @SneakyThrows
    public void deleteAlignedFeaturesByQuery(@NotNull String searchQuery) {
        if (searchService == null)
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Cannot delete by query. Search service not available!");
        // Require an explicit query: a blank query matches every feature and would delete the whole project.
        if (Utils.isNullOrBlank(searchQuery))
            throw new ResponseStatusException(BAD_REQUEST, "A non-empty search query is required to delete aligned features by query.");

        // Resolve the query against the same index the listing uses, then reuse the id-based delete
        // (which cascades the DB delete and keeps the search index in sync).
        List<String> matchingFeatureIds = searchService
                .searchIds(projectId, searchQuery, Pageable.unpaged(), AlignedFeature.class)
                .getContent();
        if (!matchingFeatureIds.isEmpty())
            deleteAlignedFeaturesByIds(matchingFeatureIds);
    }

    @SneakyThrows
    @Override
    public List<de.unijena.bioinf.ms.middleware.model.features.Feature> findFeaturesByAlignedFeatureId(String alignedFeatureId) {
        return findFeaturesByAlignedFeatureId(alignedFeatureId, Pageable.unpaged()).getContent();
    }

    @SneakyThrows
    @Override
    public Page<de.unijena.bioinf.ms.middleware.model.features.Feature> findFeaturesByAlignedFeatureId(
            String alignedFeatureId, Pageable pageable) {
        Filter ofAlignedFeature = Filter.where("alignedFeatureId").eq(parseIdOrThrow(alignedFeatureId, "alignedFeatureId"));
        return Pages.findPage(storage(), Feature.class, pageable, ofAlignedFeature)
                .map(this::convertToApiUnalignedFeature);
    }

    /**
     * Ids are numbers, so anything else is a request that cannot be answered rather than a failure on our side.
     */
    private static long parseIdOrThrow(String id, String name) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(BAD_REQUEST, "'" + id + "' is not a valid " + name + ".", e);
        }
    }


    @SneakyThrows
    @Override
    public Page<Run> findRuns(@Nullable String searchQuery, Pageable pageable, @NotNull EnumSet<Run.OptField> optFields) {
        if (searchQuery == null || searchQuery.isBlank())
            return findRuns(pageable, optFields);

        if (searchService == null)
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Cannot perform search query. Search service not available!");

        // runs are fully indexed, we do not have to load anything from db.
        return searchService.search(projectId, searchQuery, pageable, Run.class);
    }

    @SneakyThrows
    @Override
    public Page<Run> findRuns(Pageable pageable, @NotNull EnumSet<Run.OptField> optFields) {
        return findPage(storage(), LCMSRun.class, pageable)
                .map(run -> convertToApiRun(run, optFields));
    }

    @SneakyThrows
    @Override
    public Page<Run> findRunsByGroup(@NotNull String groupName, Pageable pageable, @NotNull EnumSet<Run.OptField> optFields) {
        Optional<de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup> tagGroup = storage()
                .findStr(Filter.where("groupName").eq(groupName), de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup.class).findFirst();
        if (tagGroup.isEmpty())
            return Page.empty(pageable);

        return findRuns(tagGroup.get().getLuceneQuery(), pageable, optFields);
    }

    @SneakyThrows
    @Override
    public Run findRunById(String runId, @NotNull EnumSet<Run.OptField> optFields) {
        return storage().getByPrimaryKey(Long.parseLong(runId), LCMSRun.class)
                .map(run -> convertToApiRun(run, optFields))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "There is no run '" + runId + "' in project " + projectId + "."));
    }

    private Class<?> convertToProjectObjectClass(Class<? extends Taggable> taggable) {
        if (taggable.equals(Run.class))
            return LCMSRun.class;

        if (taggable.equals(Compound.class))
            return de.unijena.bioinf.ms.persistence.model.core.Compound.class;

        if (taggable.equals(AlignedFeature.class))
            return AlignedFeatures.class;

        throw new IllegalStateException("Unknown taggable: " + taggable);
    }

    private Class<?> convertToApiObjectClass(Class<?> taggable) {
        if (taggable.equals(LCMSRun.class))
            return Run.class;

        if (taggable.equals(de.unijena.bioinf.ms.persistence.model.core.Compound.class))
            return Compound.class;

        if (taggable.equals(AlignedFeatures.class))
            return AlignedFeature.class;

        throw new IllegalStateException("Unknown taggable: " + taggable);
    }

    @Override
    public List<Tag> addTagsToObject(Class<? extends Taggable> taggedObjectClass, String objectId, List<Tag> tags) {
        addTagsToObjects(taggedObjectClass, Map.of(objectId, tags));
        return findTagsByObject(taggedObjectClass, objectId);
    }

    @Override
    public void addTagsToObjects(Class<? extends Taggable> taggedObjectClass, List<TagSubmission> tags) {
        addTagsToObjects(taggedObjectClass, tags.stream().collect(Collectors.groupingBy(TagSubmission::getTaggedObjectId)));
    }

    private void addTagsToObjects(Class<? extends Taggable> taggedObjectClass, Map<String, ? extends Collection<? extends Tag>> objectToTags) {
        try {
            final Class<?> taggedProjectObjectClass = convertToProjectObjectClass(taggedObjectClass);

            Map<String, de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition> tagDefByName = new HashMap<>();
            List<de.unijena.bioinf.ms.persistence.model.core.tags.Tag> upsertTags = new ArrayList<>();
            List<de.unijena.bioinf.ms.persistence.model.core.tags.Tag> insertTags = new ArrayList<>();

            //todo add would be 20% faster if we would separate update an insert of tags to object.
            //todo can be upsert without knowing the primary key?
            for (Map.Entry<String, ? extends Collection<? extends Tag>> entry : objectToTags.entrySet()) {
                long objId = Long.parseLong(entry.getKey());
                if (!storage().containsPrimaryKey(objId, taggedProjectObjectClass))
                    throw new ResponseStatusException(NOT_FOUND, "There is no object '" + objId + "' in project " + projectId + ".");

                for (Tag tag : entry.getValue()) {
                    de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition tagDef = tagDefByName.computeIfAbsent(tag.getTagName(), tagName -> project().findTagDefinitionByName(tagName).orElse(null));

                    if (tagDef == null)
                        throw new ResponseStatusException(NOT_FOUND, "There is no TagDefinition '" + tag.getTagName() + "' in project " + projectId + ".");

                    try {
                        Filter.FilterClause filter = Filter.and(
                                Filter.where("taggedObjectClass").eq(taggedProjectObjectClass.getName()),
                                Filter.where("taggedObjectId").eq(objId),
                                Filter.where("tagName").eq(tag.getTagName())
                        );

                        storage().findStr(filter, de.unijena.bioinf.ms.persistence.model.core.tags.Tag.class)
                                .findFirst().ifPresentOrElse(
                                        existing -> upsertTags.add(tagDef.setFormattedValueOfTag(existing, tag.getValue())),
                                        () -> insertTags.add(tagDef.newTagWithFormattedValue(tag.getValue(), taggedProjectObjectClass, objId)));

                    } catch (IllegalArgumentException e) {
                        throw new ResponseStatusException(BAD_REQUEST, "Forbidden value '" + tag.getValue() + " for TagDefinition " + tag.getTagName() + ".");
                    } catch (Exception e) {
                        throw new ResponseStatusException(BAD_REQUEST, "Error when parsing tag. Wrong tag type '" + tag.getClass() + " for TagDefinition " + tag.getTagName() + ".");
                    }
                }
            }
            if (!upsertTags.isEmpty())
                storage().upsertAll(upsertTags);
            if (!insertTags.isEmpty())
                storage().insertAll(insertTags);

            if (searchService != null)
                searchService.addTagsToDocuments(projectId, objectToTags, taggedObjectClass);
        } catch (IOException e) {
            log.error("Error when assigning tags to Object", e);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR);
        }
    }

    @SneakyThrows
    @Override
    public void removeTagsFromObject(Class<? extends Taggable> taggedOobjectClass, String taggedObjectId, List<String> tagNames) {
        storage().removeAll(Filter.and(
                Filter.where("taggedObjectClass").eq(convertToProjectObjectClass(taggedOobjectClass).getName()),
                Filter.where("taggedObjectId").eq(Long.parseLong(taggedObjectId)),
                Filter.where("tagName").in(tagNames.toArray(String[]::new))
        ), de.unijena.bioinf.ms.persistence.model.core.tags.Tag.class);

        //todo optimize performance / find better solution
        if (searchService != null)
            searchService.removeTagsFromDocument(projectId, taggedObjectId, tagNames, (Class<? extends Taggable>) taggedOobjectClass);
    }

    @SneakyThrows
    @Override
    public List<TagDefinition> findTags() {
        return project().findAllTagDefinitionsStr().map(this::convertToApiDefinition).collect(Collectors.toList());
    }

    @Override
    public List<Tag> findTagsByObject(@NotNull Class<? extends Taggable> taggedObjectClass, @NotNull String objectId) {
        final Class<?> taggedProjectObjectClass = convertToProjectObjectClass(taggedObjectClass);
        return findTagsByObject(taggedProjectObjectClass, Long.parseLong(objectId)).toList();
    }

    private Stream<Tag> findTagsByObject(Class<?> taggedProjectObjectClass, long objectId) {
        return project()
                .findTagsForObject(taggedProjectObjectClass, objectId)
                .map(this::convertToApiTag);
    }

    @SneakyThrows
    @Override
    public List<TagDefinition> findTagsByType(@NotNull String tagType) {
        return storage().findStr(Filter.where("tagType").eq(tagType), de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition.class)
                .map(this::convertToApiDefinition).collect(Collectors.toList());
    }

    @SneakyThrows
    @Override
    public TagDefinition findTagByName(String tagName) {
        return storage().findStr(Filter.where("tagName").eq(tagName), de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition.class)
                .findFirst().map(this::convertToApiDefinition)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "There is no tag definition '" + tagName + "' in project " + projectId + "."));
    }

    @SneakyThrows
    @Override
    public List<TagDefinition> createTags(List<TagDefinitionImport> tagDefinitions, boolean editable) {
        Set<String> existingNames = storage().findAllStr(de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition.class)
                .map(de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition::getTagName)
                .collect(Collectors.toSet());
        List<de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition> filtered = tagDefinitions.stream()
                .filter(tagDef -> !existingNames.contains(tagDef.getTagName()))
                .map(tagDef -> convertToProjectDefinition(tagDef, editable)).toList();
        storage().insertAll(filtered);
        storage().flush();

        // The onInsert listener that feeds the search context its value types runs on a Nitrite worker
        // thread, so flushing does NOT guarantee it has run by the time we return. A caller that tags an
        // object with a definition it just created would then index against a missing ValueType and NPE
        // inside the Lucene TagMapper. Register here, on the calling thread, so the definition is usable
        // as soon as this method returns; the event doing it again is harmless, registration is idempotent.
        if (searchService != null)
            filtered.forEach(tagDef ->
                    searchService.addTagValueType(projectId, tagDef.getTagName(), tagDef.getValueType()));

        return filtered.stream().map(this::convertToApiDefinition).toList();
    }

    @SneakyThrows
    @Override
    public void deleteTags(String tagName) {
        de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition tagDef = storage().findStr(Filter.where("tagName").eq(tagName), de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition.class)
                .findFirst().orElse(null);
        if (tagDef == null) {
            throw new ResponseStatusException(NOT_FOUND, "No such tag: " + tagName);
        }
        if (!tagDef.isEditable()) {
            throw new ResponseStatusException(BAD_REQUEST, "TagDefinition can not be edited: " + tagName);
        }
        storage().removeAll(Filter.where("tagName").eq(tagName), de.unijena.bioinf.ms.persistence.model.core.tags.Tag.class);
        storage().remove(tagDef);
        //todo optimize find different solution

        if (searchService != null)
            searchService.removeTagValueType(projectId, tagName);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    @Override
    public TagDefinition addPossibleValuesToTagDefinition(String tagName, List<?> formattedPossibleValues) {
        de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition tagDef = storage().findStr(Filter.where("tagName").eq(tagName), de.unijena.bioinf.ms.persistence.model.core.tags.TagDefinition.class)
                .findFirst().orElse(null);
        if (tagDef == null)
            throw new ResponseStatusException(NOT_FOUND, "No such tag: " + tagName);

        if (!tagDef.isEditable())
            throw new ResponseStatusException(BAD_REQUEST, "TagDefinition cannot be edited: " + tagName);

        ValueDefinition<?> valueDef = tagDef.getValueDefinition();
        if (valueDef.getValueType() == ValueType.NONE)
            throw new ResponseStatusException(BAD_REQUEST, "Can not add values to NONE type tag definition " + tagName);

        ValueFormatter<?, ?> formatter = tagDef.getValueDefinition().getValueType().getFormatter();
        formattedPossibleValues.stream().map(formatter::fromFormattedGeneric).forEach(val -> {
            try {
                valueDef.addPossibleValue(val);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(BAD_REQUEST, e.getMessage());
            }
        });


        storage().upsert(tagDef);
        storage().flush(); //flush to ensure search service cache is updated before returning.
        return convertToApiDefinition(tagDef);
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
            throw new ResponseStatusException(NOT_FOUND, "No tag group of type: " + type);
        }
        return groups;
    }

    @SneakyThrows
    @Override
    public TagGroup findTagGroup(String name) {
        return convertToApiTagGroup(storage()
                .findStr(Filter.where("groupName").eq(name), de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup.class)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No such tag group: " + name)));
    }

    @SneakyThrows
    @Override
    public TagGroup addTagGroup(String name, String filter, String type) {
        if (storage().findStr(Filter.where("groupName").eq(name), de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup.class).count() > 0) {
            throw new ResponseStatusException(NOT_ACCEPTABLE, "Tag  group " + name + " already exists");
        }

        de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup group = de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup
                .builder()
                .groupName(name)
                .luceneQuery(filter)
                .groupType(type)
                .build();

        storage().insert(group);
        return convertToApiTagGroup(group);
    }

    @SneakyThrows
    @Override
    public void deleteTagGroup(String name) {
        Optional<de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup> group = storage().findStr(Filter.where("groupName").eq(name), de.unijena.bioinf.ms.persistence.model.core.tags.TagGroup.class).findFirst();
        if (group.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "No such group: " + name);
        }

        storage().remove(group.get());
    }

    @SneakyThrows
    @Override
    public StatisticsTable getFoldChangeTable(QuantRowType statsTarget, AggregationType aggregation, QuantMeasure quantification) {
        StatisticsTable table = StatisticsTable.builder()
                .statisticsType(StatisticsType.FOLD_CHANGE)
                .quantificationMeasure(quantification)
                .aggregationType(aggregation)
                .rowType(statsTarget)
                .build();
        fillFoldChangeTable(table, statsTarget.getProjectFoldChangeClass(), aggregation, quantification);
        return table;
    }

    private <F extends de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange> void fillFoldChangeTable(StatisticsTable table, Class<F> fcClass, AggregationType aggregation, QuantMeasure quantification) throws IOException {
        List<F> foldChanges = storage().findStr(Filter.and(
                Filter.where("aggregation").eq(aggregation.toString()),
                Filter.where("quantification").eq(quantification.toString())
        ), fcClass).sorted(Comparator.comparingLong(de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange::getForeignId)).toList();

        Set<Pair<String, String>> pairSet = new HashSet<>();
        for (de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange fc : foldChanges)
            pairSet.add(Pair.of(fc.getLeftGroup(), fc.getRightGroup()));
        List<Pair<String, String>> pairs = new ArrayList<>(pairSet);

        LongList rowIds = new LongArrayList();
        List<double[]> values = new ArrayList<>();
        List<double[]> leftAbundances = new ArrayList<>();
        List<double[]> rightAbundances = new ArrayList<>();

        for (de.unijena.bioinf.ms.persistence.model.core.statistics.FoldChange fc : foldChanges) {
            if (rowIds.isEmpty() || fc.getForeignId() != rowIds.getLast()) {
                rowIds.add(fc.getForeignId());
                values.add(new double[pairSet.size()]);
                leftAbundances.add(new double[pairSet.size()]);
                rightAbundances.add(new double[pairSet.size()]);
            }
            int index = pairs.indexOf(Pair.of(fc.getLeftGroup(), fc.getRightGroup()));
            values.getLast()[index] = fc.getFoldChange();
            leftAbundances.getLast()[index] = fc.getLeftAbundance();
            rightAbundances.getLast()[index] = fc.getRightAbundance();
        }

        table.setValues(values.toArray(double[][]::new));
        table.setLeftAbundances(leftAbundances.toArray(double[][]::new));
        table.setRightAbundances(rightAbundances.toArray(double[][]::new));

        table.setColumnNames(pairs.stream().map(pair -> pair.getLeft() + " / " + pair.getRight()).toArray(String[]::new));
        table.setColumnLeftGroups(pairs.stream().map(Pair::getLeft).toArray(String[]::new));
        table.setColumnRightGroups(pairs.stream().map(Pair::getRight).toArray(String[]::new));
        table.setRowIds(rowIds.stream().map(String::valueOf).toArray(String[]::new));

        // populate class names and levels for compound class types
        switch (table.getRowType()) {
            case NPC_CLASSES -> {
                NPCFingerprintVersion npc = NPCFingerprintVersion.get();
                table.setRowNames(rowIds.longStream()
                        .mapToObj(id -> npc.getMolecularProperty((int) id).getName())
                        .toArray(String[]::new));
                table.setRowLevels(rowIds.longStream()
                        .mapToObj(id -> npc.getMolecularProperty((int) id).getLevel().name)
                        .toArray(String[]::new));
            }
            case CLASSYFIRE_CLASSES -> {
                ClassyFireFingerprintVersion cf = ClassyFireFingerprintVersion.getDefault();
                table.setRowNames(rowIds.longStream()
                        .mapToObj(id -> cf.getPropertyWithChemontId((int) id).getName())
                        .toArray(String[]::new));
                table.setRowLevels(rowIds.longStream()
                        .mapToObj(id -> String.valueOf(cf.getPropertyWithChemontId((int) id).getLevel()))
                        .toArray(String[]::new));
            }
            default -> {}
        }
    }

    @SneakyThrows
    @Override
    @SuppressWarnings("unchecked")
    public Page<FoldChange> listFoldChanges(QuantRowType statsTarget, Pageable pageable) {
        return findPage(storage(), statsTarget.getProjectFoldChangeClass(), pageable)
                .map(fc -> convertToApiFoldChange(fc, statsTarget));
    }

    @SneakyThrows
    @Override
    @SuppressWarnings("unchecked")
    public List<FoldChange> getFoldChanges(QuantRowType statsTarget, String objectId) {
        String filterField = switch (statsTarget) {
            case NPC_CLASSES -> "npcIndex";
            case CLASSYFIRE_CLASSES -> "classyfireIndex";
            default -> statsTarget.getTargetIdFieldName();
        };
        return storage()
                .findStr(Filter.where(filterField).eq(Long.parseLong(objectId)), statsTarget.getProjectFoldChangeClass())
                .map(fc -> convertToApiFoldChange(fc, statsTarget))
                .collect(Collectors.toList());
    }


    @SneakyThrows
    @Override
    public void deleteFoldChange(QuantRowType statsTarget, String left, String right, AggregationType aggregation, QuantMeasure quantification) {
        Map<String, List<FoldChange>> foldChanges = new HashMap<>();
        List<Long> toDelete = new ArrayList<>();

        //finde folc changes to delte
        storage().find(
                Filter.and(
                        Filter.where("leftGroup").eq(left),
                        Filter.where("rightGroup").eq(right),
                        Filter.where("aggregation").eq(aggregation.toString()),
                        Filter.where("quantification").eq(quantification.toString())
                ),
                statsTarget.getProjectFoldChangeClass()).forEach(fc -> {
            toDelete.add(fc.getId());
            FoldChange apiFc = convertToApiFoldChange(fc, statsTarget);
            foldChanges.computeIfAbsent(apiFc.getObjectId(), s -> new ArrayList<>()).add(apiFc);
        });

       // delete fold changes from index.
       switch (statsTarget) {
            case FEATURES -> {
                if (searchService != null)
                    searchService.updateDocumentsFields(projectId, foldChanges.keySet(), af -> {
                        if (af.getStats() != null)
                            af.getStats().removeAll(foldChanges.get(af.getAlignedFeatureId()));
                    }, AlignedFeature.class);
            }
            case COMPOUNDS -> {
                if (searchService != null)
                    searchService.updateDocumentsFields(projectId, foldChanges.keySet(), af -> {
                        if (af.getStats() != null)
                            af.getStats().removeAll(foldChanges.get(af.getCompoundId()));
                    }, Compound.class);
            }
            case NPC_CLASSES, CLASSYFIRE_CLASSES -> {} // no index to update
            default -> throw new IllegalArgumentException("Unknown fold change target!");
        }

        //delete fold changes from db: point-delete by primary key (id) under one write lock so the batch stays
        //atomic; a `Filter.where("id").in(...)` would scan the whole collection instead of using the id map.
        storage().write(() -> {
            storage().removeAllByPrimaryKey(toDelete, statsTarget.getProjectFoldChangeClass());
        });
    }

    private SpectralLibraryMatchSummary summarize(Filter filter) throws IOException {
        LongSet refSpecSet = new LongOpenHashSet();
        long total = 0;
        Set<String> compoundSet = new HashSet<>();
        SpectraMatch bestMatch = null;
        for (SpectraMatch match : storage().find(filter, SpectraMatch.class, "searchResult.similarity.similarity", Database.SortOrder.DESCENDING)) {
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
        return findPage(storage(), SpectraMatch.class, pageable, filter).map(SpectralLibraryMatch::of);
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
    public Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, boolean msDataSearchPrepared, @NotNull EnumSet<FormulaCandidate.OptField> optFields) {
        long longAFId = Long.parseLong(alignedFeatureId);

        //load ms data only once per formula candidate
        final MSData msData = Stream.of(FormulaCandidate.OptField.isotopePattern).anyMatch(optFields::contains)
                ? project().findByFeatureIdStr(longAFId, MSData.class).findFirst().orElse(null) : null;


        Filter.FilterClause defaultSortFilter = Filter.and(Filter.where("alignedFeatureId").eq(longAFId), Filter.where("formulaRank").gt(0));
        Pair<String[], Database.SortOrder[]> sort = sortFormulaCandidate(pageable.getSort());
        final boolean defaultSort = pageable.getSort().isUnsorted() || sort.getLeft().length == 0 || "formulaRank".equals(sort.getLeft()[0]);

        Stream<de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate> stream;
        if (pageable.isUnpaged() && defaultSort)
            stream = storage().findStr(defaultSortFilter, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class);
        else if (defaultSort)
            stream = storage().findStr(defaultSortFilter, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class, pageable.getOffset(), pageable.getPageSize());
        else if (pageable.isUnpaged()) {
            stream = storage().findStr(defaultSortFilter, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class, sort.getLeft(), sort.getRight());
        } else {
            stream = storage().findStr(defaultSortFilter, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class, pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight());
        }

        List<FormulaCandidate> candidates = stream.map(fc -> convertFormulaCandidate(msData, msDataSearchPrepared, fc, optFields)).toList();

        long total = project().countByFeatureId(longAFId, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class);

        return new PageImpl<>(candidates, pageable, total);
    }

    @SneakyThrows
    @Override
    public FormulaCandidate findFormulaCandidateByFeatureIdAndId(String formulaId, String alignedFeatureId, boolean msDataSearchPrepared, @NotNull EnumSet<FormulaCandidate.OptField> optFields) {
        long longFId = Long.parseLong(formulaId);
        long longAFId = Long.parseLong(alignedFeatureId);

        final MSData msData = Stream.of(/*FormulaCandidate.OptField.annotatedSpectrum,*/ FormulaCandidate.OptField.isotopePattern).anyMatch(optFields::contains)
                ? project().findByFeatureIdStr(longAFId, MSData.class).findFirst().orElse(null) : null;

        return project().findByFormulaIdStr(longFId, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class)
                .peek(fc -> {
                    if (fc.getAlignedFeatureId() != longAFId)
                        throw new ResponseStatusException(BAD_REQUEST, "Formula candidate exists but FormulaID does not belong to the requested FeatureID. Are you using the correct Ids?");
                }).map(fc -> convertFormulaCandidate(msData, msDataSearchPrepared, fc, optFields)).findFirst().orElse(null);
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

    @SneakyThrows
    private <T extends StructureMatch> Page<StructureCandidateFormula> findStructureCandidatesByFeatureId(Class<T> clz, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        long longAFId = Long.parseLong(alignedFeatureId);
        Filter.FilterClause defaultSortFilter = Filter.and(Filter.where("alignedFeatureId").eq(longAFId), Filter.where("structureRank").gt(0));
        Pair<String[], Database.SortOrder[]> sort = sortStructureMatch(pageable.getSort());
        final boolean defaultSort = pageable.getSort().isUnsorted() || sort.getLeft().length == 0 || "structureRank".equals(sort.getLeft()[0]);

        Stream<T> stream;
        if (pageable.isUnpaged() && defaultSort)
            stream = storage().findStr(defaultSortFilter, clz);
        else if (defaultSort)
            stream = storage().findStr(defaultSortFilter, clz, pageable.getOffset(), pageable.getPageSize());
        else if (pageable.isUnpaged()) {
            stream = storage().findStr(defaultSortFilter, clz, sort.getLeft(), sort.getRight());
        } else {
            stream = storage().findStr(defaultSortFilter, clz, pageable.getOffset(), pageable.getPageSize(), sort.getLeft(), sort.getRight());
        }


        Long2ObjectMap<de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate> fidToFC = new Long2ObjectOpenHashMap<>();

        List<StructureCandidateFormula> candidates = stream.map(candidate -> {
            de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate fc = fidToFC
                    .computeIfAbsent(candidate.getFormulaId(), k -> project()
                            .findByFormulaIdStr(k, de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class)
                            .findFirst().orElseThrow());
            return convertStructureMatch(fc.getMolecularFormula(), fc.getAdduct(), candidate, optFields);
        }).toList();

        long total = project().countByFeatureId(longAFId, clz);
        return new PageImpl<>(candidates, pageable, total);
    }


    @Override
    public StructureCandidateScored findTopStructureCandidateByFeatureId(String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        long longAFId = Long.parseLong(alignedFeatureId);
        return project().findTopStructureMatchByFeatureId(longAFId, CsiStructureMatch.class)
                .map(s -> convertStructureMatch(s, optFields)).orElse(null);
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

    @SneakyThrows
    private StructureCandidateFormula convertStructureMatch(StructureMatch match, EnumSet<StructureCandidateScored.OptField> optFields) {
        final StructureCandidateFormula sSum = new StructureCandidateFormula();
        //FP
        if (match.getCandidate() == null)
            project().fetchFingerprintCandidate(match, optFields.contains(StructureCandidateScored.OptField.fingerprint));

        if (optFields.contains(StructureCandidateScored.OptField.fingerprint))
            sSum.setFingerprint(AnnotationUtils.asBinaryFingerprint(match.getCandidate().getFingerprint()));

        if (optFields.contains(StructureCandidateScored.OptField.structureSvg))
            sSum.setStructureSvg(Spectrums.smilesToSVGSilent(match.getCandidate().getSmiles()));

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
            /// Index:
            /// "alignedFeatureId", "searchResult.candidateInChiKey", "searchResult.rank"
            List<SpectralLibraryMatch> libraryMatches = storage().findStr(
                            Filter.and(
                                    Filter.where("alignedFeatureId").eq(match.getAlignedFeatureId()),
                                    Filter.where("searchResult.candidateInChiKey").eq(sSum.getInchiKey())
                            ), SpectraMatch.class)
                    .map(SpectralLibraryMatch::of).toList();
            sSum.setSpectralLibraryMatches(libraryMatches);
        }

        return sSum;
    }

    @Override
    public AnnotatedSpectrum findAnnotatedSpectrumByStructureId(int specIndex, @Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId, boolean searchPrepared) {
        long longFId = Long.parseLong(formulaId);
        long longAFId = Long.parseLong(alignedFeatureId);
        return findAnnotatedMsMsSpectrum(specIndex, inchiKey, longFId, longAFId, searchPrepared);
    }

    @SneakyThrows
    private AnnotatedSpectrum findAnnotatedMsMsSpectrum(int specIndex, @Nullable String inchiKey, long formulaId, long alignedFeatureId, boolean searchPrepared) {
        MSData msdata = storage().getByPrimaryKey(alignedFeatureId, MSData.class)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Could not load ms data needed to create annotated spectrum for id: " + alignedFeatureId));

        FTree ftree = project().findByFormulaIdStr(formulaId, FTreeResult.class).findFirst().map(FTreeResult::getFTree)
                .orElse(null);

        //todo we retrieve the complete candidate just for the smile. Maybe add smiles to match?
        FingerprintCandidate candidate = storage().getByPrimaryKey(inchiKey, FingerprintCandidate.class).orElse(null);
        String smiles = candidate == null ? null : candidate.getSmiles();
        String name = candidate == null ? null : candidate.getName();

        if (specIndex < 0) {
            Spectrum<Peak> mergedSpec = msdata.getMergedMSnSpectrum();
            if (mergedSpec == null)
                throw new ResponseStatusException(NOT_FOUND, "Merged MS2 was requested (idx = -1) but does not exist!");

            double precursorMz = msdata.getMsnSpectra().stream().mapToDouble(MergedMSnSpectrum::getMergedPrecursorMz)
                    .average().orElseThrow();

            return Spectrums.createMergedMsMsWithAnnotations(precursorMz, mergedSpec, ftree, smiles, name, searchPrepared);
        } else {
            return Spectrums.createMsMsWithAnnotations(msdata.getMsnSpectra().get(specIndex), ftree, smiles, name, searchPrepared);
        }
    }

    @SneakyThrows
    @Override
    public AnnotatedMsMsData findAnnotatedMsMsDataByStructureId(@Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId, boolean searchPrepared) {
        long longFId = Long.parseLong(formulaId);
        long longAFId = Long.parseLong(alignedFeatureId);

        MSData msdata = storage().getByPrimaryKey(longAFId, MSData.class)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Could not load ms data needed to create annotated spectrum for id: " + alignedFeatureId));

        if (msdata.getMsnSpectra() == null || msdata.getMsnSpectra().isEmpty())
            throw new ResponseStatusException(BAD_REQUEST, "Could not find MS/MS spectra to annotate for feature with id: " + alignedFeatureId);

        FTree ftree = project().findByFormulaIdStr(longFId, FTreeResult.class).findFirst().map(FTreeResult::getFTree)
                .orElse(null);

        //todo we retrieve the complete candidate just for the smile. Maybe add smiles to match?
        FingerprintCandidate candidate = storage().getByPrimaryKey(inchiKey, FingerprintCandidate.class).orElse(null);
        String smiles = candidate == null ? null : candidate.getSmiles();
        String name = candidate == null ? null : candidate.getName();

        return AnnotatedMsMsData.of(msdata, ftree, smiles, name, searchPrepared);
    }

    @SneakyThrows
    @Override
    public String getFingerIdDataCSV(int charge) {
        Optional<FingerIdData> dataOpt = project().findFingerprintData(FingerIdData.class, charge);
        if (dataOpt.isEmpty())
            return null;
        StringWriter writer = new StringWriter();
        FingerIdData.write(writer, dataOpt.get(), true); //sneaky throws because it's a string writer and no real io.
        return writer.toString();
    }

    @SneakyThrows
    @Override
    public String getCanopusClassyFireDataCSV(int charge) {
        Optional<CanopusCfData> dataOpt = project().findFingerprintData(CanopusCfData.class, charge);
        if (dataOpt.isEmpty())
            return null;
        StringWriter writer = new StringWriter();
        CanopusCfData.write(writer, dataOpt.get()); //sneaky throws because it's a string writer and no real io.
        return writer.toString();
    }

    @SneakyThrows
    @Override
    public String getCanopusNpcDataCSV(int charge) {
        Optional<CanopusNpcData> dataOpt = project().findFingerprintData(CanopusNpcData.class, charge);
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

    @Override
    public void close() throws IOException {
        close(false);
    }

    @Override
    public void close(boolean compact) throws IOException {
        // Capture the project file while the manager is still open; reanchor runs after it is closed.
        java.nio.file.Path projectFile = java.nio.file.Path.of(projectSpaceManager.getLocation());
        try {
            if (searchService != null)
                // Delete a temp project's (ephemeral) index on close so its on-disk index dir does not linger
                // until app shutdown; persistent projects keep their index for a fast reopen.
                searchService.closeProjectIndex(this, isTempProject());
            if (compact)
                projectSpaceManager.compact();
        } finally {
            projectSpaceManager.close();
        }
        // Reanchor the persisted index versions to the compacted DB commit AFTER the project-space manager
        // is closed, so we never hold two Nitrite handles on the same file at once. reanchor opens its own
        // short-lived handle on the (now closed) compacted file.
        if (compact && searchService != null)
            searchService.reanchorStorageCommitVersion(this, projectFile);
    }

    @Override
    public Optional<ProjectType> getProjectType() {
        return project().findProjectType();
    }

    @Override
    public Optional<ProjectSourceFormats> getProjectSourceFormats() {
        return project().findProjectSourceFormats();
    }
}
