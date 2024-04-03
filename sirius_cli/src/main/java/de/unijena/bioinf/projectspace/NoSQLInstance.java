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
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class NoSQLInstance implements Instance {

    private enum State {
        OKAY, COMPUTING, REMOVED
    }

    private final NoSQLProjectSpaceManager manager;
    private final long id;

    private AlignedFeatures alignedFeatures;

    private AtomicReference<State> state = new AtomicReference<>(State.OKAY);

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
        manager.getProject().getStorage().onUpdate(AlignedFeatures.class, (changed) -> {
            if (changed.getAlignedFeatureId() == id) {
                this.alignedFeatures = changed;
            }
        });
        manager.getProject().getStorage().onRemove(AlignedFeatures.class, (changed) -> {
            if (changed.getAlignedFeatureId() == id) {
                state.set(State.REMOVED);
            }
        });
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
        return Optional.ofNullable(alignedFeatures.getCompoundId()).map(String::valueOf);
    }

    @Override
    public Optional<String> getProvidedFeatureId() {
        return Optional.ofNullable(alignedFeatures.getExternalFeatureId());
    }

    @Override
    public String getName() {
        return getAlignedFeatures().getDataSource().toString();
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
    public NoSQLProjectSpaceManager getProjectSpaceManager() {
        return manager;
    }

    @Override
    public Ms2Experiment getExperiment() {
        return null;

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
    private Optional<MSData> getMSData(){
        return manager.getProject().getStorage().getByPrimaryKey(id, MSData.class);
    }

    @Override
    public LCMSPeakInformation getLCMSPeakInformation() {
        return null;
    }

    @Override
    public List<FCandidate<?>> getFormulaCandidates() {
        return null;
    }

    @Override
    public List<SpectralSearchResult.SearchResult> getSpectraMatches() {
        return null;
    }

    @Override
    public List<FCandidate<?>> getFTrees() {
        return null;
    }

    @Override
    public List<FCandidate<?>> getCanopusInput() {
        return null;
    }

    @Override
    public List<FCandidate<?>> getMsNovelistInput() {
        return null;
    }

    @Override
    public List<FCandidate<?>> getFingerblastInput() {
        return null;
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
    public @Nullable ParameterConfig loadInputFileConfig() {
        return null;
    }

    @Override
    public @Nullable ParameterConfig loadProjectConfig() {
        return null;
    }

    @Override
    public void updateConfig(@NotNull ParameterConfig config) {

    }

    @Override
    public void clearCompoundCache() {

    }

    @Override
    public void setComputing(boolean computing) {
        state.updateAndGet((s) -> (s == State.REMOVED) ? State.REMOVED : ((computing) ? State.COMPUTING : State.OKAY));
    }

    @Override
    public boolean isComputing() {
        return state.get() == State.COMPUTING;
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
    }

    @Override
    public boolean hasSpectraSearchResult() {
        return false;
    }

    @Override
    public void deleteSpectraSearchResult() {

    }

    @Override
    public void savePassatuttoResult(FCandidate<?> id, Decoy decoy) {

    }

    @Override
    public boolean hasPassatuttoResult() {
        return false;
    }

    @Override
    public void deletePassatuttoResult() {

    }

    @Override
    public void saveSiriusResult(List<FTree> treesSortedByScore) {

    }

    @Override
    public boolean hasSiriusResult() {
        return false;
    }

    @Override
    public void deleteSiriusResult() {

    }

    @Override
    public void saveZodiacResult(Map<FCandidate<?>, ZodiacScore> zodiacScores) {

    }

    @Override
    public boolean hasZodiacResult() {
        return false;
    }

    @Override
    public void deleteZodiacResult() {

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
