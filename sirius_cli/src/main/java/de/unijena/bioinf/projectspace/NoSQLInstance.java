/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.sirius.*;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.properties.ConfigType;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import it.unimi.dsi.fastutil.Pair;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class NoSQLInstance implements Instance {

    private enum State {
        OKAY, COMPUTING, REMOVED
    }

    private final NoSQLProjectSpaceManager manager;
    private final long id;

    private AlignedFeatures alignedFeatures;

    private AtomicReference<State> state = new AtomicReference<>(State.OKAY);

    private long updateListenerId;
    private long removeListenerId;

    @SneakyThrows
    public NoSQLInstance(long id, NoSQLProjectSpaceManager manager) {
        this.id = id;
        this.manager = manager;
        this.alignedFeatures = manager.getProject().getStorage().getByPrimaryKey(id, AlignedFeatures.class)
                .orElseThrow(() -> new IllegalStateException("Could not find feature data of this instance. This should not be possible. Project might have been externally modified."));
        initListeners();
    }

    @SneakyThrows
    public NoSQLInstance(AlignedFeatures alignedFeatures, NoSQLProjectSpaceManager manager) {
        this.id = alignedFeatures.getAlignedFeatureId();
        this.manager = manager;
        this.alignedFeatures = alignedFeatures;
        initListeners();
    }

    private void initListeners() throws IOException {
        // we need to reference the listeners, if not we can not remove them wen the Instance is not used anymore
        // which will result in a memory leak
        updateListenerId = manager.getProject().getStorage().onUpdate(AlignedFeatures.class, (changed) -> {
            if (changed.getAlignedFeatureId() == id) {
                this.alignedFeatures = changed;
            }
        });
        removeListenerId = manager.getProject().getStorage().onRemove(AlignedFeatures.class, (changed) -> {
            if (changed.getAlignedFeatureId() == id) {
                state.set(State.REMOVED);
            }
        });
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
    public double getIonMass() {
        return getAlignedFeatures().getAverageMass();
    }

    @Override
    public PrecursorIonType getIonType() {
        return getAlignedFeatures().getIonType();
    }

    public AlignedFeatures getAlignedFeatures() {
        if (state.get() == State.REMOVED) {
            throw new IllegalStateException("Instance has been removed.");
        }
        return alignedFeatures;
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
        return getMSData()
                .map(ms -> ms.getMergedMSnSpectrum() != null || (ms.getMsnSpectra() != null && !ms.getMsnSpectra().isEmpty()))
                .orElse(false);
    }

    @SneakyThrows
    private Optional<MSData> getMSData() {
        return project().getStorage().getByPrimaryKey(id, MSData.class);
    }

    @Override
    public LCMSPeakInformation getLCMSPeakInformation() {
        return null;
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
        return project().findByFeatureIdStr(id, SpectraMatch.class).collect(Collectors.toList());
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
        return project().getConfig(id, ConfigType.INPUT).map(Parameters::getConfig);
    }

    @Override
    public Optional<ParameterConfig> loadProjectConfig() {
        return project().getConfig(id, ConfigType.PROJECT).map(Parameters::getConfig);

    }

    @Override
    public void updateConfig(@NotNull ParameterConfig config) {
        project().upsertConfig(id, ConfigType.PROJECT, config);
    }

    @Override
    public void clearCompoundCache() {
        //todo this is not the perfect place for cleaning this listeners
        // discuss whether they are needed and if yes implement a proper cleanup mechanism
        try {
            project().getStorage().unsubscribe(AlignedFeatures.class, updateListenerId);
            project().getStorage().unsubscribe(AlignedFeatures.class, removeListenerId);
        } catch (IOException e) {
            log.error("Error when removing listeners from project database. This might cause a memory leak!", e);
        }
    }

    @Override
    public void setComputing(boolean computing) {
        state.updateAndGet((s) -> (s == State.REMOVED) ? State.REMOVED : ((computing) ? State.COMPUTING : State.OKAY));
        //todo I think we need a global compute state managemen.
        // It does not need to be persistent though -> discuss

        //project().setFeatureComputing(id, computing);
    }

    @Override
    public boolean isComputing() {
        return state.get() == State.COMPUTING;
        //todo I think we need a global compute state managemen.
        // It does not need to be persistent though -> discuss

        //return project().isFeatureComputing(id);
    }

    @Override
    public void saveDetectedAdducts(DetectedAdducts detectedAdducts) {

    }

    @Override
    public Optional<DetectedAdducts> getDetectedAdducts() {
        return Optional.empty();
    }

    @Override
    public void saveSpectraSearchResult(SpectralSearchResult result) {
        //todo
    }

    @Override
    public boolean hasSpectraSearchResult() {
        return project().countByFeatureIdStr(id, SpectraMatch.class) > 0;
    }

    @Override
    public void deleteSpectraSearchResult() {
        project().deleteAllByFeatureIdStr(id, SpectraMatch.class);
    }

    @Override
    public void savePassatuttoResult(FCandidate<?> id, Decoy decoy) {
        //todo
    }

    @Override
    public boolean hasPassatuttoResult() {
        //todo
        return false;
    }

    @Override
    public void deletePassatuttoResult() {

    }

    @Override
    public void saveSiriusResult(List<FTree> treesSortedByScore) {
        try {
            List<Pair<FormulaCandidate, FTreeResult>> formulaResults = treesSortedByScore.stream().map(tree -> {
                PrecursorIonType adduct = tree.getAnnotationOrThrow(PrecursorIonType.class);
                FTreeMetricsHelper scores = new FTreeMetricsHelper(tree);
                FormulaCandidate fc = FormulaCandidate.builder()
                        .alignedFeatureId(id)
                        .adduct(adduct)
                        .molecularFormula(
                                tree.getRoot().getFormula()
                                        .add(adduct.getAdduct())
                                        .subtract(adduct.getInSourceFragmentation()))
                        .siriusScore(scores.getSiriusScore())
                        .isotopeScore(scores.getIsotopeMs1Score())
                        .treeScore(scores.getTreeScore())
                        .build();

                FTreeResult treeResult = FTreeResult.builder().fTree(tree).alignedFeatureId(id).build();
                return Pair.of(fc, treeResult);
            }).toList();
            //store candidates and create formula ids
            project().getStorage().insertAll(formulaResults.stream()
                    .map(Pair::first).toList());
            //stores trees
            project().getStorage().insertAll(formulaResults.stream()
                    .peek(p -> p.second().setFormulaId(p.first().getFormulaId()))
                    .map(Pair::second).toList());
        } catch (IOException e) {
            deleteSiriusResult(); //try deleting all results in case of io error so that project stays consistent
            throw new RuntimeException(e);
        }
    }


    @Override
    public boolean hasSiriusResult() {
        return project().countByFeatureIdStr(id, FormulaCandidate.class) > 0;
    }

    @Override
    public void deleteSiriusResult() {
        project().deleteAllByFeatureIdStr(id, FormulaCandidate.class);
        project().deleteAllByFeatureIdStr(id, FTreeResult.class);
        //todo handle detected adducts.
    }

    @SneakyThrows
    @Override
    public void saveZodiacResult(Map<FCandidate<?>, ZodiacScore> zodiacScores) {
        List<FormulaCandidate> candidates = zodiacScores.entrySet().stream().map(e -> {
            FormulaCandidate c = ((NoSqlFCandidate) e.getKey()).getFormulaCandidate();
            c.setZodiacScore(e.getValue().score());
            return c;
        }).toList();
        project().getStorage().upsertAll(candidates);
    }

    @Override
    public boolean hasZodiacResult() {
        return getTopFormulaCandidate().map(FCandidate::getZodiacScore).isPresent();
    }

    @SneakyThrows
    @Override
    public void deleteZodiacResult() {
        project().getStorage().upsertAll(
                project().findByFeatureIdStr(id, FormulaCandidate.class).peek(fc -> fc.setZodiacScore(null)).toList());
    }

    @Override
    public void saveFingerprintResult(@NotNull Map<FCandidate<?>, FingerprintResult> fingerprintResultsByFormula) {

    }

    @Override
    public boolean hasFingerprintResult() {
        return false;
    }

    @Override
    public void deleteFingerprintResult() {

    }

    @Override
    public void saveStructureSearchResult(@NotNull Map<FCandidate<?>, FingerIdResult> structureSearchResults) {

    }

    @Override
    public boolean hasStructureSearchResult() {
        return false;
    }

    @Override
    public void deleteStructureSearchResult() {

    }

    @Override
    public void saveCanopusResult(@NotNull List<FCandidate<?>> canopusResults) {

    }

    @Override
    public boolean hasCanopusResult() {
        return false;
    }

    @Override
    public void deleteCanopusResult() {

    }

    @Override
    public void saveMsNovelistResult(@NotNull Map<FCandidate<?>, FingerIdResult> CanopusResultsByFormula) {

    }

    @Override
    public boolean hasMsNovelistResult() {
        return false;
    }

    @Override
    public void deleteMsNovelistResult() {

    }
}
