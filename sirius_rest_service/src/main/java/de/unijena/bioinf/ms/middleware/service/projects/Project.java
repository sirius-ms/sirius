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

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.ms.backgroundruns.ImportMsFromResourceWorkflow;
import de.unijena.bioinf.ms.backgroundruns.ImportPeaksFomResourceWorkflow;
import de.unijena.bioinf.ms.middleware.model.annotations.*;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.compute.AbstractImportSubmission;
import de.unijena.bioinf.ms.middleware.model.compute.InstrumentProfile;
import de.unijena.bioinf.ms.middleware.model.features.*;
import de.unijena.bioinf.ms.middleware.model.projects.ImportResult;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils.toEnumSet;

public interface Project<PSM extends ProjectSpaceManager> {

    @NotNull
    String getProjectId();

    @NotNull
    PSM getProjectSpaceManager();

    Optional<QuantificationTable> getQuantificationForAlignedFeature(String alignedFeatureId, QuantificationTable.QuantificationType type);

    Optional<TraceSet> getTraceSetForAlignedFeature(String alignedFeatureId);
    Optional<TraceSet> getTraceSetForCompound(String compoundId);

    Page<Compound> findCompounds(Pageable pageable, @NotNull EnumSet<Compound.OptField> optFields,
                                 @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields);

    List<Compound> addCompounds(@NotNull List<CompoundImport> compounds,
                                @Nullable InstrumentProfile profile,
                                @NotNull EnumSet<Compound.OptField> optFields,
                                @NotNull EnumSet<AlignedFeature.OptField> optFieldsFeatures);

    default ImportResult importPreprocessedData(Collection<InputResource<?>> inputResources, boolean ignoreFormulas, boolean allowMs1OnlyData) {
        ImportPeaksFomResourceWorkflow importTask = new ImportPeaksFomResourceWorkflow(getProjectSpaceManager(), inputResources, ignoreFormulas, allowMs1OnlyData);

        importTask.run();

        return ImportResult.builder()
                .affectedAlignedFeatureIds(importTask.getImportedInstancesStr()
                        .map(Instance::getId)
                        .collect(Collectors.toList()))
                .affectedCompoundIds(importTask.getImportedInstancesStr()
                        .map(Instance::getCompoundId)
                        .filter(Optional::isPresent)
                        .flatMap(Optional::stream)
                        .distinct().collect(Collectors.toList()))
                .build();
    }

    default ImportResult importMsRunData(AbstractImportSubmission submission) {
        ImportMsFromResourceWorkflow importTask = new ImportMsFromResourceWorkflow(getProjectSpaceManager(), submission, true);

        importTask.run();
        return ImportResult.builder()
                .affectedAlignedFeatureIds(importTask.getImportedInstancesStr()
                        .map(Instance::getId)
                        .collect(Collectors.toList()))
                .affectedCompoundIds(importTask.getImportedInstancesStr()
                        .map(Instance::getCompoundId)
                        .filter(Optional::isPresent).flatMap(Optional::stream)
                        .distinct().collect(Collectors.toList()))
                .build();
    }

    default Page<Compound> findCompounds(Pageable pageable, Compound.OptField... optFields) {
        return findCompounds(pageable, toEnumSet(Compound.OptField.class, optFields),
                EnumSet.of(AlignedFeature.OptField.topAnnotations));
    }

    Compound findCompoundById(String compoundId, @NotNull EnumSet<Compound.OptField> optFields,
                              @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields);

    default Compound findCompoundById(String compoundId, Compound.OptField... optFields) {
        return findCompoundById(compoundId, toEnumSet(Compound.OptField.class, optFields),
                EnumSet.of(AlignedFeature.OptField.topAnnotations));
    }

    void deleteCompoundById(String compoundId);

    Page<AlignedFeatureQuality> findAlignedFeaturesQuality(Pageable pageable);

    AlignedFeatureQuality findAlignedFeaturesQualityById(String alignedFeatureId);

    Page<AlignedFeature> findAlignedFeatures(Pageable pageable, @NotNull EnumSet<AlignedFeature.OptField> optFields);

    List<AlignedFeature> addAlignedFeatures(@NotNull List<FeatureImport> features,
                                            @Nullable InstrumentProfile profile,
                                            @NotNull EnumSet<AlignedFeature.OptField> optFields);

    default Page<AlignedFeature> findAlignedFeatures(Pageable pageable, AlignedFeature.OptField... optFields) {
        return findAlignedFeatures(pageable, toEnumSet(AlignedFeature.OptField.class, optFields));
    }

    AlignedFeature findAlignedFeaturesById(String alignedFeatureId, @NotNull EnumSet<AlignedFeature.OptField> optFields);

    default AlignedFeature findAlignedFeaturesById(String alignedFeatureId, AlignedFeature.OptField... optFields) {
        return findAlignedFeaturesById(alignedFeatureId, toEnumSet(AlignedFeature.OptField.class, optFields));
    }

    void deleteAlignedFeaturesById(String alignedFeatureId);
    void deleteAlignedFeaturesByIds(List<String> alignedFeatureId);

    SpectralLibraryMatchSummary summarizeLibraryMatchesByFeatureId(String alignedFeatureId, int minSharedPeaks, double minSimilarity);

    SpectralLibraryMatchSummary summarizeLibraryMatchesByFeatureIdAndInchi(String alignedFeatureId, String candidateInchi, int minSharedPeaks, double minSimilarity);

    Page<SpectralLibraryMatch> findLibraryMatchesByFeatureId(String alignedFeatureId, int minSharedPeaks, double minSimilarity, Pageable pageable);

    Page<SpectralLibraryMatch> findLibraryMatchesByFeatureIdAndInchi(String alignedFeatureId, String candidateInchi, int minSharedPeaks, double minSimilarity, Pageable pageable);

    SpectralLibraryMatch findLibraryMatchesByFeatureIdAndMatchId(String alignedFeatureId, String matchId);

    Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<FormulaCandidate.OptField> optFields);

    default Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, FormulaCandidate.OptField... optFields) {
        return findFormulaCandidatesByFeatureId(alignedFeatureId, pageable, toEnumSet(FormulaCandidate.OptField.class, optFields));
    }

    FormulaCandidate findFormulaCandidateByFeatureIdAndId(String formulaId, String alignedFeatureId, @NotNull EnumSet<FormulaCandidate.OptField> optFields);

    default FormulaCandidate findFormulaCandidateByFeatureIdAndId(String formulaId, String alignedFeatureId, FormulaCandidate.OptField... optFields) {
        return findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId, toEnumSet(FormulaCandidate.OptField.class, optFields));
    }

    Page<StructureCandidateScored> findStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields);

    default Page<StructureCandidateScored> findStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, StructureCandidateScored.OptField... optFields) {
        return findStructureCandidatesByFeatureIdAndFormulaId(formulaId, alignedFeatureId, pageable, toEnumSet(StructureCandidateScored.OptField.class, optFields));
    }

    Page<StructureCandidateScored> findDeNovoStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields);

    default Page<StructureCandidateScored> findDeNovoStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, StructureCandidateScored.OptField... optFields) {
        return findStructureCandidatesByFeatureIdAndFormulaId(formulaId, alignedFeatureId, pageable, toEnumSet(StructureCandidateScored.OptField.class, optFields));
    }

    Page<StructureCandidateFormula> findStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields);

    default Page<StructureCandidateFormula> findStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, StructureCandidateScored.OptField... optFields) {
        return findStructureCandidatesByFeatureId(alignedFeatureId, pageable, toEnumSet(StructureCandidateScored.OptField.class, optFields));
    }

    Page<StructureCandidateFormula> findDeNovoStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields);

    default Page<StructureCandidateFormula> findDeNovoStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, StructureCandidateScored.OptField... optFields) {
        return findDeNovoStructureCandidatesByFeatureId(alignedFeatureId, pageable, toEnumSet(StructureCandidateScored.OptField.class, optFields));
    }

    StructureCandidateScored findTopStructureCandidateByFeatureId(String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields);

    default StructureCandidateScored findTopStructureCandidateByFeatureId(String alignedFeatureId, StructureCandidateScored.OptField... optFields) {
        return findTopStructureCandidateByFeatureId(alignedFeatureId, toEnumSet(StructureCandidateScored.OptField.class, optFields));
    }

    StructureCandidateScored findStructureCandidateById(@NotNull String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields);

    default StructureCandidateScored findStructureCandidateById(@NotNull String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId, StructureCandidateScored.OptField... optFields) {
        return findStructureCandidateById(inchiKey, formulaId, alignedFeatureId, toEnumSet(StructureCandidateScored.OptField.class, optFields));
    }

    default AnnotatedSpectrum findAnnotatedSpectrumByFormulaId(int specIndex, @NotNull String formulaId, @NotNull String alignedFeatureId) {
        return findAnnotatedSpectrumByStructureId(specIndex, null, formulaId, alignedFeatureId);
    }

    /**
     * Return Annotated MsMs Spectrum (Fragments and Structure)
     *
     * @param specIndex        index of the spectrum to annotate if < 0 a Merged Ms/Ms over all spectra will be used
     * @param inchiKey         of the structure candidate that will be used
     * @param formulaId        of the formula candidate to retrieve the fragments from
     * @param alignedFeatureId the feature the spectrum belongs to
     * @return Annotated MsMs Spectrum (Fragments and Structure)
     */
    AnnotatedSpectrum findAnnotatedSpectrumByStructureId(int specIndex, @Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId);

    default AnnotatedMsMsData findAnnotatedMsMsDataByFormulaId(@NotNull String formulaId, @NotNull String alignedFeatureId) {
        return findAnnotatedMsMsDataByStructureId(null, formulaId, alignedFeatureId);
    }

    AnnotatedMsMsData findAnnotatedMsMsDataByStructureId(@Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId);

    String getFingerIdDataCSV(int charge);

    String getCanopusClassyFireDataCSV(int charge);

    String getCanopusNpcDataCSV(int charge);

    @Deprecated
    String findSiriusFtreeJsonById(String formulaId, String alignedFeatureId);
}
