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

import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.babelms.inputresource.PathInputResource;
import de.unijena.bioinf.ms.middleware.model.annotations.FormulaCandidate;
import de.unijena.bioinf.ms.middleware.model.annotations.SpectralLibraryMatch;
import de.unijena.bioinf.ms.middleware.model.annotations.StructureCandidateFormula;
import de.unijena.bioinf.ms.middleware.model.annotations.StructureCandidateScored;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.features.*;
import de.unijena.bioinf.ms.middleware.model.projects.ImportResult;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.sirius.SpectraMatch;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class NoSQLProjectImpl implements Project<NoSQLProjectSpaceManager> {

    @NotNull
    private final String projectId;

    @NotNull
    private final NoSQLProjectSpaceManager projectSpaceManager;

    private final SiriusProjectDocumentDatabase<? extends Database<?>> database;

    @Getter
    private final Database<?> storage;

    private final Map<Class<?>, AtomicLong> totalCounts = Collections.synchronizedMap(new HashMap<>());

    private final Map<Class<?>, Map<Long, AtomicLong>> totalCountByFeature = Collections.synchronizedMap(new HashMap<>());

    @SneakyThrows
    public NoSQLProjectImpl(@NotNull String projectId, @NotNull NoSQLProjectSpaceManager projectSpaceManager) {
        this.projectId = projectId;
        this.projectSpaceManager = projectSpaceManager;
        this.database = projectSpaceManager.getProject();
        this.storage = database.getStorage();

        initCounter(de.unijena.bioinf.ms.persistence.model.core.Compound.class);
        initCounter(AlignedFeatures.class);

        initCounterByFeature(SpectraMatch.class, SpectraMatch::getAlignedFeatureId);
    }

    @SneakyThrows
    private void initCounter(Class<?> clazz) {
        this.totalCounts.put(clazz, new AtomicLong(this.storage.countAll(clazz)));
        this.storage.onInsert(clazz, c -> totalCounts.get(clazz).getAndIncrement());
        this.storage.onRemove(clazz, c -> totalCounts.get(clazz).getAndDecrement());
    }

    @SneakyThrows
    private <T> void initCounterByFeature(Class<T> clazz, Function<T, Long> featureIdGetter) {
        Map<Long, AtomicLong> counts = Collections.synchronizedMap(new HashMap<>());
        for (AlignedFeatures af : this.storage.findAll(AlignedFeatures.class)) {
            counts.put(af.getAlignedFeatureId(), new AtomicLong(this.storage.count(Filter.where("alignedFeatureId").eq(af.getAlignedFeatureId()), clazz)));
        }
        this.totalCountByFeature.put(clazz, counts);
        this.storage.onInsert(clazz, c -> this.totalCountByFeature.get(clazz).get(featureIdGetter.apply(c)).getAndIncrement());
        this.storage.onRemove(clazz, c -> this.totalCountByFeature.get(clazz).get(featureIdGetter.apply(c)).getAndDecrement());
    }

    @Override
    public @NotNull String getProjectId() {
        return projectId;
    }

    @Override
    public @NotNull NoSQLProjectSpaceManager getProjectSpaceManager() {
        return projectSpaceManager;
    }

    private Compound toMiddlewareCompound(de.unijena.bioinf.ms.persistence.model.core.Compound compound) {
        // TODO opt fields
        database.fetchAdductFeatures(compound);
        // TODO translate
        return Compound.builder()
                .build();
    }

    private de.unijena.bioinf.ms.persistence.model.core.Compound toDatabaseCompound(CompoundImport compound) {
        // TODO translate
        return de.unijena.bioinf.ms.persistence.model.core.Compound.builder()
                .name(compound.getName())
//                .adductFeatures()
                .build();
    }

    private AlignedFeatures toDatabaseAlignedFeatures(FeatureImport feature) {
        return AlignedFeatures.builder()

                .build();
    }

    private MsData toMiddlewareMSData(MSData data) {
        return MsData.builder()
                .mergedMs1(new BasicSpectrum(data.getMergedMs1Spectrum()))
                .mergedMs2(new BasicSpectrum(data.getMergedMSnSpectrum().getPeaks()))
                // TODO ms1 spectra
                .ms2Spectra(data.getMsnSpectra().stream().map(BasicSpectrum::new).toList())
                .build();
    }

    private AlignedFeature toMiddleWareAlignedFeature(AlignedFeatures features, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        AlignedFeature af = new AlignedFeature();
        af.setAlignedFeatureId(String.valueOf(features.getAlignedFeatureId()));
        af.setName(features.getName());
        af.setAdduct(features.getDetectedAdducts().getBestAdduct().toString());
        af.setIonMass(features.getAverageMass());

        database.fetchMsData(features);
        if (optFields.contains(AlignedFeature.OptField.msData)) {
            features.getMSData().ifPresent(data -> af.setMsData(toMiddlewareMSData(data)));
        }
        // TODO top annotations, top annotations de novo
        return af;
    }

    private SpectraMatch toDatabaseSpectralMatch(SpectralLibraryMatch match) {
        return SpectraMatch.builder().build();
    }

    private SpectralLibraryMatch toMiddlewareSpectralMatch(SpectraMatch match) {
        // TODO
        return SpectralLibraryMatch.builder().build();
    }

    private FormulaCandidate toMiddleWareFCandidate(de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate candidate) {
        // TODO
        return FormulaCandidate.builder().build();
    }

    @SneakyThrows
    @Override
    public Page<Compound> findCompounds(Pageable pageable, @NotNull EnumSet<Compound.OptField> optFields, @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields) {
        List<Compound> compounds = storage
                .findAllStr(de.unijena.bioinf.ms.persistence.model.core.Compound.class, (int) pageable.getOffset(), pageable.getPageSize())
                .map(this::toMiddlewareCompound).toList();
        long total = totalCounts.get(de.unijena.bioinf.ms.persistence.model.core.Compound.class).get();

        return new PageImpl<>(compounds, pageable, total);
    }

    @SneakyThrows
    @Override
    public List<Compound> addCompounds(@NotNull List<CompoundImport> compounds, @NotNull EnumSet<Compound.OptField> optFields, @NotNull EnumSet<AlignedFeature.OptField> optFieldsFeatures) {
        List<de.unijena.bioinf.ms.persistence.model.core.Compound> dbc = compounds.stream().map(this::toDatabaseCompound).toList();
        database.importCompounds(dbc);
        return dbc.stream().map(this::toMiddlewareCompound).toList();
    }

    @Override
    public ImportResult importPreprocessedData(Collection<InputResource<?>> inputResources, boolean ignoreFormulas, boolean allowMs1OnlyData) {
        // TODO
        return null;
    }

    @Override
    public ImportResult importMsRunData(Collection<PathInputResource> inputResources, boolean alignRuns, boolean allowMs1OnlyData) {
        return null;
    }

    @SneakyThrows
    @Override
    public Compound findCompoundById(String compoundId, @NotNull EnumSet<Compound.OptField> optFields, @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields) {
        long id = Long.parseLong(compoundId);
        return storage.getByPrimaryKey(id, de.unijena.bioinf.ms.persistence.model.core.Compound.class)
                .map(this::toMiddlewareCompound)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "There is no compound '" + compoundId + "' in project " + projectId + "."));
    }

    @SneakyThrows
    @Override
    public void deleteCompoundById(String compoundId) {
        long id = Long.parseLong(compoundId);
        storage.removeAll(Filter.where("compoundId").eq(id), de.unijena.bioinf.ms.persistence.model.core.Compound.class);
    }

    @Override
    public Page<AlignedFeatureQuality> findAlignedFeaturesQuality(Pageable pageable, @NotNull EnumSet<AlignedFeatureQuality.OptField> optFields) {
        return null;
    }

    @Override
    public AlignedFeatureQuality findAlignedFeaturesQualityById(String alignedFeatureId, @NotNull EnumSet<AlignedFeatureQuality.OptField> optFields) {
        return null;
    }

    @SneakyThrows
    @Override
    public Page<AlignedFeature> findAlignedFeatures(Pageable pageable, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        List<AlignedFeature> features = storage
                .findAllStr(AlignedFeatures.class, (int) pageable.getOffset(), pageable.getPageSize())
                .map(af -> toMiddleWareAlignedFeature(af, optFields)).toList();
        long total = totalCounts.get(AlignedFeatures.class).get();

        return new PageImpl<>(features, pageable, total);
    }

    @Override
    public List<AlignedFeature> addAlignedFeatures(@NotNull List<FeatureImport> features, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        return null;
    }

    @Override
    public AlignedFeature findAlignedFeaturesById(String alignedFeatureId, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        return null;
    }

    @Override
    public void deleteAlignedFeaturesById(String alignedFeatureId) {

    }

    @SneakyThrows
    @Override
    public Page<SpectralLibraryMatch> findLibraryMatchesByFeatureId(String alignedFeatureId, Pageable pageable) {
        long longId = Long.parseLong(alignedFeatureId);
        List<SpectralLibraryMatch> results = storage.findStr(
                Filter.where("alignedFeatureId").eq(longId), SpectraMatch.class, (int) pageable.getOffset(), pageable.getPageSize(), "similarity.similarity", Database.SortOrder.DESCENDING
        ).map(this::toMiddlewareSpectralMatch).toList();
        long total = totalCountByFeature.get(SpectraMatch.class).getOrDefault(longId, new AtomicLong(0)).get();

        return new PageImpl<>(results, pageable, total);
    }

    @SneakyThrows
    @Override
    public Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<FormulaCandidate.OptField> optFields) {
        long longId = Long.parseLong(alignedFeatureId);
        List<FormulaCandidate> results = storage.findStr(
                Filter.where("alignedFeatureId").eq(longId), de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class, (int) pageable.getOffset(), pageable.getPageSize(), "siriusScore", Database.SortOrder.DESCENDING
        ).map(this::toMiddleWareFCandidate).toList();
        long total = totalCountByFeature.get(de.unijena.bioinf.ms.persistence.model.sirius.FormulaCandidate.class).getOrDefault(longId, new AtomicLong(0)).get();

        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public FormulaCandidate findFormulaCandidateByFeatureIdAndId(String formulaId, String alignedFeatureId, @NotNull EnumSet<FormulaCandidate.OptField> optFields) {
        return null;
    }

    @Override
    public Page<StructureCandidateScored> findStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return null;
    }

    @Override
    public Page<StructureCandidateScored> findDeNovoStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return null;
    }

    @Override
    public Page<StructureCandidateFormula> findStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return null;
    }

    @Override
    public Page<StructureCandidateFormula> findDeNovoStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return null;
    }

    @Override
    public StructureCandidateScored findTopStructureCandidateByFeatureId(String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return null;
    }

    @Override
    public StructureCandidateScored findStructureCandidateById(@NotNull String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields) {
        return null;
    }

    @Override
    public AnnotatedSpectrum findAnnotatedSpectrumByStructureId(int specIndex, @Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId) {
        return null;
    }

    @Override
    public AnnotatedMsMsData findAnnotatedMsMsDataByStructureId(@Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId) {
        return null;
    }

    @Override
    public String getFingerIdDataCSV(int charge) {
        return null;
    }

    @Override
    public String getCanopusClassyFireDataCSV(int charge) {
        return null;
    }

    @Override
    public String getCanopusNpcDataCSV(int charge) {
        return null;
    }

    @Override
    public String findSiriusFtreeJsonById(String formulaId, String alignedFeatureId) {
        return null;
    }
}
