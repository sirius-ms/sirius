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
import de.unijena.bioinf.ms.middleware.model.annotations.FormulaCandidate;
import de.unijena.bioinf.ms.middleware.model.annotations.SpectralLibraryMatch;
import de.unijena.bioinf.ms.middleware.model.annotations.StructureCandidateFormula;
import de.unijena.bioinf.ms.middleware.model.annotations.StructureCandidateScored;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeatureQuality;
import de.unijena.bioinf.ms.middleware.model.features.AnnotatedMsMsData;
import de.unijena.bioinf.ms.middleware.model.features.FeatureImport;
import de.unijena.bioinf.ms.middleware.model.projects.ImportResult;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public class NoSQLProjectImpl implements Project {

    @NotNull
    private final String projectId;

    private final SiriusProjectDocumentDatabase<? extends Database<?>> database;

    private final Database<?> storage;

    public NoSQLProjectImpl(@NotNull String projectId, @NotNull SiriusProjectDocumentDatabase<? extends Database<?>> database) {
        this.projectId = projectId;
        this.database = database;
        this.storage = database.getStorage();
    }

    @Override
    public @NotNull String getProjectId() {
        return projectId;
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

    @SneakyThrows
    @Override
    public Page<Compound> findCompounds(Pageable pageable, @NotNull EnumSet<Compound.OptField> optFields, @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields) {
        List<Compound> compounds = storage
                .findAllStr(de.unijena.bioinf.ms.persistence.model.core.Compound.class, (int) pageable.getOffset(), pageable.getPageSize())
                .map(this::toMiddlewareCompound).toList();
        int total = storage.countAll(de.unijena.bioinf.ms.persistence.model.core.Compound.class);

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
    public ImportResult importMsRunData(Collection<InputResource<?>> inputResources, boolean alignRuns, boolean allowMs1OnlyData) {
        // TODO
        return null;
    }

    @SneakyThrows
    @Override
    public Compound findCompoundById(String compoundId, @NotNull EnumSet<Compound.OptField> optFields, @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields) {
        long id = Long.parseLong(compoundId);
        return storage.getByPrimaryKey(id, de.unijena.bioinf.ms.persistence.model.core.Compound.class)
                .map(this::toMiddlewareCompound)
                .orElseThrow(() -> new IllegalArgumentException(compoundId));
    }

    @SneakyThrows
    @Override
    public void deleteCompoundById(String compoundId) {
        long id = Long.parseLong(compoundId);
        storage.removeAll(Filter.build().eq("compoundId", id), de.unijena.bioinf.ms.persistence.model.core.Compound.class);
    }

    @Override
    public Page<AlignedFeatureQuality> findAlignedFeaturesQuality(Pageable pageable, @NotNull EnumSet<AlignedFeatureQuality.OptField> optFields) {
        return null;
    }

    @Override
    public AlignedFeatureQuality findAlignedFeaturesQualityById(String alignedFeatureId, @NotNull EnumSet<AlignedFeatureQuality.OptField> optFields) {
        return null;
    }

    @Override
    public Page<AlignedFeature> findAlignedFeatures(Pageable pageable, @NotNull EnumSet<AlignedFeature.OptField> optFields) {
        return null;
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

    @Override
    public Page<SpectralLibraryMatch> findLibraryMatchesByFeatureId(String alignedFeatureId, Pageable pageable) {
        return null;
    }

    @Override
    public Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<FormulaCandidate.OptField> optFields) {
        return null;
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
