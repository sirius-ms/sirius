/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.lcms.QuantificationMeasure;
import de.unijena.bioinf.ChemistryBase.ms.lcms.QuantificationTable;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.MsNovelistFingerblastResult;
import de.unijena.bioinf.fingerid.StructureSearchResult;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.Feature;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.sirius.*;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.StorageUtils;
import de.unijena.bioinf.ms.properties.ConfigType;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class NoSQLInstance implements Instance {
    private final NoSQLProjectSpaceManager manager;
    private final long id;
    private AlignedFeatures alignedFeatures;
    private final ReentrantReadWriteUpdateLock alignedFeaturesLock = new ReentrantReadWriteUpdateLock();

    private final AtomicBoolean recompute = new AtomicBoolean(false);

    @SneakyThrows
    public NoSQLInstance(long id, NoSQLProjectSpaceManager manager) {
        this.id = id;
        this.manager = manager;
    }

    @SneakyThrows
    public NoSQLInstance(AlignedFeatures alignedFeatures, NoSQLProjectSpaceManager manager) {
        this(alignedFeatures.getAlignedFeatureId(), manager);
        this.alignedFeatures = alignedFeatures;
    }

    @Override
    public ProjectSpaceManager getProjectSpaceManager() {
        return manager;
    }

    private SiriusProjectDocumentDatabase<?> project() {
        return manager.getProject();
    }

    public long getLongId() {
        return id;
    }

    @Override
    public String getId() {
        return String.valueOf(getLongId());
    }

    @Override
    public Optional<String> getCompoundId() {
        return Optional.ofNullable(getAlignedFeatures().getCompoundId()).map(String::valueOf);
    }

    @Override
    public Optional<String> getExternalFeatureId() {
        return Optional.ofNullable(getAlignedFeatures().getExternalFeatureId());
    }

    @Override
    public String getName() {
        AlignedFeatures f = getAlignedFeatures();
        String r = f.getName();
        if (r == null || r.isBlank())
            r = f.getExternalFeatureId();
        if (r == null || r.isBlank())
            r = getId();
        return r;
    }

    @Override
    public String toString() {
        String name = getName();
        String id = getId();
        if (name != null && !name.equals(id))
            return name + " (" + id + ")";
        return name;
    }

    @Override
    public Optional<RetentionTime> getRT() {
        return Optional.ofNullable(getAlignedFeatures().getRetentionTime());
    }

    @Override
    public double getIonMass() {
        return getAlignedFeatures().getAverageMass();
    }

    @Override
    public PrecursorIonType getIonType() {
        AlignedFeatures f = getAlignedFeatures();
        List<PrecursorIonType> allAdducts = f.getDetectedAdducts().getAllAdducts();
        if (allAdducts.size() == 1) return allAdducts.get(0);
        else return PrecursorIonType.unknown(f.getCharge());
    }

    @SneakyThrows
    public AlignedFeatures getAlignedFeatures() {
        alignedFeaturesLock.updateLock().lock();
        try {
            if (alignedFeatures == null) {
                alignedFeaturesLock.writeLock().lock();
                try {
                    if (alignedFeatures == null)
                        alignedFeatures = manager.getProject().getStorage().getByPrimaryKey(id, AlignedFeatures.class)
                                .orElseThrow(() -> new IllegalStateException("Could not find feature data of this instance. This should not be possible. Project might have been externally modified."));
                } finally {
                    alignedFeaturesLock.writeLock().unlock();
                }
            }
            return alignedFeatures;
        } finally {
            alignedFeaturesLock.updateLock().unlock();
        }
    }

    @Override
    public Ms2Experiment getExperiment() {
        return project().fetchMsDataAndConfigsAsMsExperiment(getAlignedFeatures()).orElseThrow();
    }

    @Override
    public boolean hasMs1() {
        return getMSData().map(ms -> ms.getMergedMs1Spectrum() != null || ms.getIsotopePattern() != null)
                .orElse(false);
    }

    @Override
    public boolean hasMsMs() {
        return getAlignedFeatures().getMSData()
                .map(ms -> ms.getMergedMSnSpectrum() != null || (ms.getMsnSpectra() != null && !ms.getMsnSpectra().isEmpty()))
                .orElse(false);
    }

    @SneakyThrows
    private Optional<MSData> getMSData() {
        return project().getStorage().findStr(Filter.where("alignedFeatureId").eq(id), MSData.class).findFirst();
    }

    @SneakyThrows
    @Override
    public Optional<QuantificationTable> getQuantificationTable() {
        Database<?> storage = project().getStorage();
        Optional<AlignedFeatures> maybeFeature = storage.getByPrimaryKey(getLongId(), AlignedFeatures.class);
        if (maybeFeature.isEmpty())
            return Optional.empty();
        AlignedFeatures feature = maybeFeature.get();
        storage.fetchAllChildren(feature, "alignedFeatureId", "features", Feature.class);
        // only use features with LC/MS information
        List<Feature> features = feature.getFeatures().stream().flatMap(List::stream).filter(x -> x.getApexIntensity() != null).toList();
        List<LCMSRun> samples = new ArrayList<>();
        for (Feature value : features) {
            samples.add(storage.getByPrimaryKey(value.getRunId(), LCMSRun.class).orElse(null));
        }

        QuantTableImpl table = new QuantTableImpl(QuantificationMeasure.APEX);
        String[] sampleNames = samples.stream().map(x -> x.getName() == null ? "unknown" : x.getName()).toArray(String[]::new);
        for (int k = 0; k < features.size(); ++k)
            table.add(sampleNames[k], features.get(k).getApexIntensity());
        return Optional.of(table);
    }


    public Stream<FCandidate<?>> getFormulaCandidatesStr() {
        return project().findByFeatureIdStr(id, FormulaCandidate.class)
                .map(fc -> NoSqlFCandidate.builder().formulaCandidate(fc).build());
    }

    public List<FCandidate<?>> getFormulaCandidates() {
        return getFormulaCandidatesStr().toList();
    }

    @Override
    @SneakyThrows
    public List<SpectralSearchResult.SearchResult> getSpectraMatches() {
        return project().findByFeatureIdStr(id, SpectraMatch.class).map(SpectraMatch::getSearchResult)
                .collect(Collectors.toList());
    }

    @Override
    @SneakyThrows
    public List<FCandidate<?>> getFTrees() {
        return getFTreesStr().toList();
    }

    public Stream<FCandidate<?>> getFTreesStr() {
        return getFormulaCandidatesStr().peek(it -> it.annotate(
                project().findByFormulaIdStr((long) it.getId(), FTreeResult.class).map(FTreeResult::getFTree).findFirst().orElse(null))
        );
    }

    @Override
    public List<FCandidate<?>> getCanopusInput() {
        return getCanopusInputStr().toList();
    }

    public Stream<FCandidate<?>> getCanopusInputStr() {
        return getFormulaCandidatesStr()
                .peek(it -> it.annotate(
                        project().findByFormulaIdStr((long) it.getId(), CsiPrediction.class)
                                .map(CsiPrediction::getFingerprint)
                                .map(FingerprintResult::new).findFirst().orElse(null)));
    }

    @Override
    public List<FCandidate<?>> getMsNovelistInput() {
        return getMsNovelistInputStr().toList();
    }

    public Stream<FCandidate<?>> getMsNovelistInputStr() {
        return getFTreesStr()
                .peek(it -> it.annotate(
                        project().findByFormulaIdStr((long) it.getId(), CsiPrediction.class)
                                .map(CsiPrediction::getFingerprint)
                                .map(FingerprintResult::new).findFirst().orElse(null)));
    }

    @Override
    public List<FCandidate<?>> getFingerblastInput() {
        return getFingerblastInputStr().toList();
    }

    public Stream<FCandidate<?>> getFingerblastInputStr() {
        return getMsNovelistInputStr()
                .peek(it -> it.annotate(
                        project().findByFormulaIdStr((long) it.getId(), CanopusPrediction.class)
                                .map(cp -> new CanopusResult(cp.getCfFingerprint(), cp.getNpcFingerprint()))
                                .findFirst().orElse(null)));
    }

    @Override
    public Optional<FCandidate<?>> getTopFormulaCandidate() {
        return Optional.empty();
    }

    @Override
    public Optional<FCandidate<?>> getTopPredictions() {
        return Optional.empty();
    }

    @Override
    public Optional<FCandidate<?>> getTopFTree() {
        return Optional.empty();
    }

    @Override
    public Optional<ParameterConfig> loadInputFileConfig() {
        return project().getConfig(id, ConfigType.INPUT_FILE).map(Parameters::newParameterConfig);
    }

    @Override
    public Optional<ParameterConfig> loadProjectConfig() {
        return project().getConfig(id, ConfigType.PROJECT).map(Parameters::newParameterConfig);

    }

    @Override
    public void updateProjectConfig(@NotNull ParameterConfig config) {
        //write full config stack to be used as base config when rerunning computations
        project().upsertConfig(id, ConfigType.PROJECT, config, false);
    }

    @Override
    public void clearCompoundCache() {
        alignedFeaturesLock.writeLock().lock();
        try {
            alignedFeatures = null;
        } finally {
            alignedFeaturesLock.writeLock().unlock();
        }
    }

    //region state
    @Override
    public boolean isRecompute() {
        return recompute.get();
    }

    @Override
    public void setRecompute(boolean recompute) {
        this.recompute.set(recompute);
    }

    //endregion

    @Override
    public boolean hasDetectedAdducts() {
        return getAlignedFeatures().getDetectedAdducts() != null;
    }

    @Override
    public void saveDetectedAdductsAnnotation(DetectedAdducts detectedAdducts) {
        de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts adducts = StorageUtils.fromMs2ExpAnnotation(detectedAdducts);
        saveDetectedAdducts(adducts);
    }

    @SneakyThrows
    @Override
    public void saveDetectedAdducts(de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts detectedAdducts) {
        alignedFeaturesLock.writeLock().lock();
        try {
            alignedFeatures.setDetectedAdducts(detectedAdducts);
            project().getStorage().upsert(alignedFeatures);
        } finally {
            alignedFeaturesLock.writeLock().unlock();
        }
    }

    @Override
    public DetectedAdducts getDetectedAdductsAnnotation() {
        return StorageUtils.toMs2ExpAnnotation(getDetectedAdducts());
    }

    @Override
    public de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts getDetectedAdducts() {
        return getAlignedFeatures().getDetectedAdducts();
    }

    @NotNull
    public ComputedSubtools getComputedSubtools() {
        return project().findByFeatureIdStr(id, ComputedSubtools.class).findFirst().orElseGet(() -> ComputedSubtools.builder().alignedFeatureId(id).build());
    }

    @SneakyThrows
    @Override
    public void saveSpectraSearchResult(SpectralSearchResult result) {
        List<SpectraMatch> matches = result.getResults().stream()
                .map(s -> SpectraMatch.builder().alignedFeatureId(id).searchResult(s).build())
                .collect(Collectors.toList());

        project().getStorage().write(() -> {
            project().getStorage().insertAll(matches);
            upsertComputedSubtools(cs -> cs.setLibrarySearch(true));
        });

    }

    @Override
    public boolean hasSpectraSearchResult() {
        return getComputedSubtools().isLibrarySearch();
    }

    @SneakyThrows
    @Override
    public void deleteSpectraSearchResult() {
        project().getStorage().write(() -> {
            project().deleteAllByFeatureId(id, SpectraMatch.class);
            upsertComputedSubtools(c -> c.setLibrarySearch(false));
        });
    }

    @Override
    public void savePassatuttoResult(FCandidate<?> id, Decoy decoy) {
        //todo IMPLEMENT OR REMOVE TOOL
    }

    @Override
    public boolean hasPassatuttoResult() {
        //todo IMPLEMENT OR REMOVE TOOL
        return false;
    }

    @Override
    public void deletePassatuttoResult() {
        //todo IMPLEMENT OR REMOVE TOOL
    }

    @Override
    public void saveSiriusResult(List<FTree> treesSortedByScore) {
        try {
            Comparator<Pair<FormulaCandidate, FTreeResult>> comp =
                    Comparator.<Pair<FormulaCandidate, FTreeResult>>comparingDouble(p -> p.first().getSiriusScore()).reversed() //sort descending by siriusScore
                            .thenComparing(p -> p.first().getAdduct());
            final AtomicInteger rank = new AtomicInteger(1);

            final List<Pair<FormulaCandidate, FTreeResult>> formulaResults = treesSortedByScore.stream()
                    .map(tree -> {
                        PrecursorIonType adduct = tree.getAnnotationOrThrow(PrecursorIonType.class);
                        FTreeMetricsHelper scores = new FTreeMetricsHelper(tree);
                        FormulaCandidate fc = FormulaCandidate.builder()
                                .alignedFeatureId(id)
                                .adduct(adduct)
                                .molecularFormula(tree.getRoot().getFormula())
                                .siriusScore(scores.getSiriusScore())
                                .isotopeScore(scores.getIsotopeMs1Score())
                                .treeScore(scores.getTreeScore())
                                .build();

                        FTreeResult treeResult = FTreeResult.builder().fTree(tree).alignedFeatureId(id).build();
                        return Pair.of(fc, treeResult);
                    })
                    .sorted(comp)
                    .peek(m -> m.first().setFormulaRank(rank.getAndIncrement())) //add rank to sorted candidates
                    .toList();


            //store candidates and create formula ids
            project().getStorage().insertAll(formulaResults.stream()
                    .map(Pair::first).toList());
            //stores trees
            project().getStorage().insertAll(formulaResults.stream()
                    .peek(p -> p.second().setFormulaId(p.first().getFormulaId()))
                    .map(Pair::second).toList());
            //set as computed
            upsertComputedSubtools(cs -> cs.setFormulaSearch(true));
        } catch (IOException e) {
            deleteSiriusResult(); //try deleting all results in case of io error so that project stays consistent
            throw new RuntimeException(e);
        }
    }


    @Override
    public boolean hasSiriusResult() {
        return getComputedSubtools().isFormulaSearch();
//        return project().countByFeatureId(id, FormulaCandidate.class) > 0;
    }

    @SneakyThrows
    @Override
    public void deleteSiriusResult() {
        project().getStorage().write(() -> {
            project().deleteAllByFeatureId(id, FormulaCandidate.class);
            project().deleteAllByFeatureId(id, FTreeResult.class);
            upsertComputedSubtools(cs -> cs.setFormulaSearch(false));
        });

        //todo handle detected adducts.
    }

    @SneakyThrows
    @Override
    public void saveZodiacResult(List<FCandidate<?>> zodiacScores) {
        Comparator<FormulaCandidate> comp = Comparator.comparing(FormulaCandidate::getZodiacScore, Comparator.reverseOrder())
                .thenComparing(FormulaCandidate::getSiriusScore, Comparator.reverseOrder())
                .thenComparing(FormulaCandidate::getAdduct);

        final AtomicInteger rank = new AtomicInteger(1);

        List<FormulaCandidate> candidates = zodiacScores.stream().
                filter(fc -> fc.hasAnnotation(ZodiacScore.class))
                .map(fc -> {
                    FormulaCandidate c = ((NoSqlFCandidate) fc).getFormulaCandidate();
                    c.setZodiacScore(fc.getAnnotationOrThrow(ZodiacScore.class).score());
                    return c;
                })
                .sorted(comp)
                .peek(fc -> fc.setFormulaRank(rank.getAndIncrement()))
                .toList();


        project().getStorage().write(() -> {
            project().getStorage().upsertAll(candidates);
            upsertComputedSubtools(cs -> cs.setZodiac(true));
        });

    }

    @Override
    public boolean hasZodiacResult() {
        return getComputedSubtools().isZodiac();
//        return getTopFormulaCandidate().map(FCandidate::getZodiacScore).isPresent();
    }

    @SneakyThrows
    @Override
    public void deleteZodiacResult() {
        project().getStorage().write(() -> {
            project().getStorage().insertAll(project().findByFeatureIdStr(id, FormulaCandidate.class).peek(fc -> fc.setZodiacScore(null)).toList());
            upsertComputedSubtools(cs -> cs.setZodiac(false));
        });

    }

    @SneakyThrows
    @Override
    public void saveFingerprintResult(@NotNull List<FCandidate<?>> fingerprintResults) {
        List<CsiPrediction> fps = fingerprintResults.stream()
                .filter(fc -> fc.hasAnnotation(FingerprintResult.class))
                .map(fc -> {
                    @NotNull FingerprintResult fpResult = fc.getAnnotationOrThrow(FingerprintResult.class);
                    return CsiPrediction.builder()
                            .formulaId((long) fc.getId())
                            .alignedFeatureId(id)
                            .fingerprint(fpResult.fingerprint)
                            .build();
                }).collect(Collectors.toList());


        project().getStorage().write(() -> {
            project().getStorage().insertAll(fps);
            upsertComputedSubtools(cs -> cs.setFingerprint(true));
        });
    }

    @Override
    public boolean hasFingerprintResult() {
        return getComputedSubtools().isFingerprint();
//        return project().countByFeatureId(id, CsiPrediction.class) > 0;
    }

    @SneakyThrows
    @Override
    public void deleteFingerprintResult() {
        project().getStorage().write(() -> {
            project().deleteAllByFeatureId(id, CsiPrediction.class);
            upsertComputedSubtools(cs -> cs.setFingerprint(false));
        });
    }

    @Override
    public void saveStructureSearchResult(@NotNull List<FCandidate<?>> structureSearchResults) {
        //todo move entity creation to document project space package
        try {
            //create and structure search results
            List<CsiStructureSearchResult> searchResults = structureSearchResults.stream()
                    .filter(fc -> fc.hasAnnotation(FingerIdResult.class))
                    .flatMap(fc -> {
                        final FingerIdResult idResult = fc.getAnnotationOrThrow(FingerIdResult.class);
                        return idResult.getAnnotation(StructureSearchResult.class).map(searchResult ->
                                        CsiStructureSearchResult.builder()
                                                .alignedFeatureId(id)
                                                .confidenceApprox(searchResult.getConfidencScoreApproximate())
                                                .confidenceExact(searchResult.getConfidenceScore())
//                                        .expandedDatabases() //todo add databases to algo result
//                                        .specifiedDatabases() //todo add databases to algo result
                                                .expansiveSearchConfidenceMode(searchResult.getExpansiveSearchConfidenceMode())
                                                .build()
                        ).stream();
                    }).collect(Collectors.toList());
            // write structure search results to db
            project().getStorage().insertAll(searchResults);


            List<CsiStructureMatch> matches = structureSearchResults.stream()
                    .filter(fc -> fc.hasAnnotation(FingerIdResult.class))
                    .flatMap(fc -> fc.getAnnotationOrThrow(FingerIdResult.class).getAnnotation(FingerblastResult.class)
                            .map(csiRes -> csiRes.getResults().stream().map(c -> CsiStructureMatch.builder()
                                    .alignedFeatureId(id)
                                    .formulaId((long) fc.getId())
                                    .csiScore(c.getScore())
                                    .tanimotoSimilarity(c.getCandidate().getTanimoto())
                                    .mcesDistToTopHit(c.getCandidate().getMcesToTopHit())
                                    .candidateInChiKey(c.getCandidate().getInchiKey2D())
                                    .candidate(c.getCandidate())
                                    .build())
                            ).orElseGet(Stream::empty))
                    .sorted(Comparator.comparing(StructureMatch::getCsiScore).reversed())
                    .collect(Collectors.toList());

            //adding ranks
            final AtomicInteger rank = new AtomicInteger(1);
            matches.forEach(m -> m.setStructureRank(rank.getAndIncrement()));
            if (!matches.isEmpty())
                matches.get(0).setMcesDistToTopHit(0d); //it seems that top hit zero is sometimes overwritten during expansive search.
            //insert matches
            project().getStorage().insertAll(matches);

            // write only fingerprint candidates that do not yet exist in a transaction
//            int inserted = project().getStorage().write(() -> {
//                List<FingerprintCandidate> toInsert = new ArrayList<>(matches.size());
//                for (CsiStructureMatch m : matches) {
//                    FingerprintCandidate c = m.getCandidate();
//                    if (!project().getStorage().containsPrimaryKey(c.getInchiKey2D(), FingerprintCandidate.class))
//                        toInsert.add(c);
//                }
//                //insert all candidates that do not exist
//                return project().getStorage().upsertAll(toInsert); //should be insert, workaround to prevent duplicate key error.
//            });

            //always update to allow for updated flags after custom db removal or adding //todo more efficient solution preferred
            int inserted = project().getStorage().upsertAll(matches.stream().map(CsiStructureMatch::getCandidate).toList());
            upsertComputedSubtools(cs -> cs.setStructureSearch(true));
            log.debug("Inserted: {} of {} CSI candidates.", inserted, matches.size());

        } catch (Exception e) {
            deleteStructureSearchResult();
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasStructureSearchResult() {
        return getComputedSubtools().isStructureSearch();
//        return project().countByFeatureId(id, CsiStructureSearchResult.class) > 0;
    }

    @SneakyThrows
    @Override
    public void deleteStructureSearchResult() {
        project().getStorage().write(() -> {
            project().deleteAllByFeatureId(id, CsiStructureSearchResult.class);
            project().deleteAllByFeatureId(id, CsiStructureMatch.class);
            upsertComputedSubtools(cs -> cs.setStructureSearch(false));
        });


    }

    @SneakyThrows
    @Override
    public void saveCanopusResult(@NotNull List<FCandidate<?>> canopusResults) {
        List<CanopusPrediction> cps = canopusResults.stream()
                .filter(fc -> fc.hasAnnotation(CanopusResult.class))
                .map(fc -> {
                    @NotNull CanopusResult canopusResult = fc.getAnnotationOrThrow(CanopusResult.class);
                    return CanopusPrediction.builder()
                            .formulaId((long) fc.getId())
                            .alignedFeatureId(id)
                            .cfFingerprint(canopusResult.getCanopusFingerprint())
                            .npcFingerprint(canopusResult.getNpcFingerprint().orElse(null))
                            .build();
                }).collect(Collectors.toList());

        project().getStorage().write(() -> {
            project().getStorage().insertAll(cps);
            upsertComputedSubtools(cs -> cs.setCanopus(true));
        });
    }

    @Override
    public boolean hasCanopusResult() {
        return getComputedSubtools().isCanopus();
//        return project().countByFeatureId(id, CanopusPrediction.class) > 0;
    }

    @SneakyThrows
    @Override
    public void deleteCanopusResult() {
        project().getStorage().write(() -> {
            project().deleteAllByFeatureId(id, CanopusPrediction.class);
            upsertComputedSubtools(cs -> cs.setCanopus(false));
        });

    }

    @SneakyThrows
    @Override
    public void saveMsNovelistResult(@NotNull List<FCandidate<?>> msNovelistResults) {
        try {
            List<DenovoStructureMatch> matches = msNovelistResults.stream()
                    .filter(fc -> fc.hasAnnotation(FingerIdResult.class))
                    .flatMap(fc -> fc.getAnnotationOrThrow(FingerIdResult.class).getAnnotation(MsNovelistFingerblastResult.class)
                            .map(msnRes -> {
                                        int i = 0;
                                        List<DenovoStructureMatch> m = new ArrayList<>(msnRes.getResults().size());
                                        for (Scored<FingerprintCandidate> c : msnRes.getResults()) {
                                            m.add(DenovoStructureMatch.builder()
                                                    .alignedFeatureId(id)
                                                    .formulaId((long) fc.getId())
                                                    .csiScore(c.getScore())
                                                    .tanimotoSimilarity(c.getCandidate().getTanimoto())
                                                    .modelScore(msnRes.getRnnScore(i++))
                                                    .candidateInChiKey(c.getCandidate().getInchiKey2D())
                                                    .candidate(c.getCandidate())
                                                    .build());
                                        }
                                        return m.stream();
                                    }
                            ).orElseGet(Stream::empty))
                    .sorted(Comparator.comparing(StructureMatch::getCsiScore).reversed())
                    .collect(Collectors.toList());

            //adding ranks
            final AtomicInteger rank = new AtomicInteger(1);
            matches.forEach(m -> m.setStructureRank(rank.getAndIncrement()));
            //insert matches
            project().getStorage().insertAll(matches);

            // write only fingerprint candidates that do not yet exist in a transaction
//            int inserted = project().getStorage().write(() -> {
//                List<FingerprintCandidate> toInsert = new ArrayList<>(matches.size());
//                for (DenovoStructureMatch m : matches) {
//                    FingerprintCandidate c = m.getCandidate();
//                    if (!project().getStorage().containsPrimaryKey(c.getInchiKey2D(), FingerprintCandidate.class))
//                        toInsert.add(c);
//                }
//                return project().getStorage().upsertAll(toInsert); //should be insert, workaround to prevent duplicate key error.
//            });
            //always update to allow for updated flags after custom db removal or adding //todo more efficient solution preferred
            int inserted = project().getStorage().upsertAll(matches.stream().map(DenovoStructureMatch::getCandidate).toList());
            upsertComputedSubtools(cs -> cs.setDeNovoSearch(true));
            log.debug("Inserted: {} of {} DeNovo candidates.", inserted, matches.size());
        } catch (Exception e) {
            deleteMsNovelistResult();
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasMsNovelistResult() {
        return getComputedSubtools().isDeNovoSearch();
//        return project().countByFeatureId(id, DenovoStructureMatch.class) > 0;
    }

    @SneakyThrows
    @Override
    public void deleteMsNovelistResult() {
        project().getStorage().write(() -> {
            project().deleteAllByFeatureId(id, DenovoStructureMatch.class);
            upsertComputedSubtools(cs -> cs.setDeNovoSearch(false));
        });
    }

    @SneakyThrows
    private long upsertComputedSubtools(Consumer<ComputedSubtools> modifier) {
        @NotNull ComputedSubtools it = getComputedSubtools();
        modifier.accept(it);
        return project().getStorage().upsert(it);
    }


    private class QuantTableImpl implements QuantificationTable {
        private List<String> sampleNames = new ArrayList<>();
        private Object2IntMap<String> namesToIndex = new Object2IntOpenHashMap<>();
        private DoubleList abundances = new DoubleArrayList();
        private final QuantificationMeasure measure;

        private QuantTableImpl(QuantificationMeasure measure) {
            this.measure = measure;
        }

        public int add(String name, double abundance) {
            sampleNames.add(name);
            abundances.add(abundance);
            namesToIndex.put(name, namesToIndex.size() - 1);
            return namesToIndex.getInt(name);
        }

        @Override
        public String getName(int i) {
            if (i < 0 || i >= sampleNames.size())
                return null;
            return sampleNames.get(i);
        }

        @Override
        public double getAbundance(int i) {
            if (i < 0 || i >= abundances.size())
                return Double.NaN;
            return abundances.getDouble(i);
        }

        @Override
        public double getAbundance(String name) {
            return getAbundance(namesToIndex.getOrDefault(name, -1));
        }

        @Override
        public Optional<Double> mayGetAbundance(String name) {
            double ab = getAbundance(name);
            if (Double.isNaN(ab))
                return Optional.empty();
            return Optional.of(ab);
        }

        @Override
        public int length() {
            return sampleNames.size();
        }

        @Override
        public QuantificationMeasure getMeasure() {
            return measure;
        }
    }

}
