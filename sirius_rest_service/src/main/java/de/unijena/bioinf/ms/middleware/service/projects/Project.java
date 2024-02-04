/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.middleware.service.projects;

import de.unijena.bioinf.ms.middleware.model.annotations.FormulaCandidate;
import de.unijena.bioinf.ms.middleware.model.annotations.StructureCandidateFormula;
import de.unijena.bioinf.ms.middleware.model.annotations.StructureCandidateScored;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeatureQuality;
import de.unijena.bioinf.ms.middleware.model.features.AnnotatedMsMsData;
import de.unijena.bioinf.ms.middleware.model.spectra.AnnotatedSpectrum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.EnumSet;

import static de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils.toEnumSet;

public interface Project {

    @NotNull
    String getProjectId();

    Page<Compound> findCompounds(Pageable pageable, @NotNull EnumSet<Compound.OptField> optFields,
                                 @NotNull EnumSet<AlignedFeature.OptField> optFeatureFields);

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

    @Deprecated
    Page<AlignedFeatureQuality> findAlignedFeaturesQuality(Pageable pageable, @NotNull EnumSet<AlignedFeatureQuality.OptField> optFields);

    @Deprecated
    default Page<AlignedFeatureQuality> findAlignedFeaturesQuality(Pageable pageable, AlignedFeatureQuality.OptField... optFields) {
        return findAlignedFeaturesQuality(pageable, toEnumSet(AlignedFeatureQuality.OptField.class, optFields));
    }

    @Deprecated
    AlignedFeatureQuality findAlignedFeaturesQualityById(String alignedFeatureId, @NotNull EnumSet<AlignedFeatureQuality.OptField> optFields);

    @Deprecated
    default AlignedFeatureQuality findAlignedFeaturesQualityById(String alignedFeatureId, AlignedFeatureQuality.OptField... optFields) {
        return findAlignedFeaturesQualityById(alignedFeatureId, toEnumSet(AlignedFeatureQuality.OptField.class, optFields));
    }


    Page<AlignedFeature> findAlignedFeatures(Pageable pageable, @NotNull EnumSet<AlignedFeature.OptField> optFields);

    default Page<AlignedFeature> findAlignedFeatures(Pageable pageable, AlignedFeature.OptField... optFields) {
        return findAlignedFeatures(pageable, toEnumSet(AlignedFeature.OptField.class, optFields));
    }

    AlignedFeature findAlignedFeaturesById(String alignedFeatureId, @NotNull EnumSet<AlignedFeature.OptField> optFields);

    default AlignedFeature findAlignedFeaturesById(String alignedFeatureId, AlignedFeature.OptField... optFields) {
        return findAlignedFeaturesById(alignedFeatureId, toEnumSet(AlignedFeature.OptField.class, optFields));
    }

    void deleteAlignedFeaturesById(String alignedFeatureId);

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

    Page<StructureCandidateFormula> findStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, @NotNull EnumSet<StructureCandidateScored.OptField> optFields);

    default Page<StructureCandidateFormula> findStructureCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, StructureCandidateScored.OptField... optFields) {
        return findStructureCandidatesByFeatureId(alignedFeatureId, pageable, toEnumSet(StructureCandidateScored.OptField.class, optFields));
    }

    StructureCandidateScored findTopStructureCandidateByFeatureId(String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields);

    default StructureCandidateScored findTopStructureCandidateByFeatureId(String alignedFeatureId, StructureCandidateScored.OptField... optFields) {
        return findTopStructureCandidateByFeatureId(alignedFeatureId, toEnumSet(StructureCandidateScored.OptField.class, optFields));
    }

    StructureCandidateScored findStructureCandidateById(@NotNull String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId, @NotNull EnumSet<StructureCandidateScored.OptField> optFields);

    default StructureCandidateScored findStructureCandidateById(@NotNull String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId, StructureCandidateScored.OptField... optFields) {
        return findStructureCandidateById(inchiKey, formulaId, alignedFeatureId, toEnumSet(StructureCandidateScored.OptField.class, optFields));
    }

    default AnnotatedSpectrum findAnnotatedSpectrumByFormulaId(int specIndex, @NotNull String formulaId, @NotNull String alignedFeatureId){
        return findAnnotatedSpectrumByStructureId(specIndex,null, formulaId, alignedFeatureId);
    }

    /**
     * Return Annotated MsMs Spectrum (Fragments and Structure)
     * @param specIndex index of the spectrum to annotate if < 0 a Merged Ms/Ms over all spectra will be used
     * @param inchiKey of the structure candidate that will be used
     * @param formulaId of the formula candidate to retrieve the fragments from
     * @param alignedFeatureId the feature the spectrum belongs to
     * @return Annotated MsMs Spectrum (Fragments and Structure)
     */
    AnnotatedSpectrum findAnnotatedSpectrumByStructureId(int specIndex, @Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId);

    default AnnotatedMsMsData findAnnotatedMsMsDataByFormulaId(@NotNull String formulaId, @NotNull String alignedFeatureId){
        return findAnnotatedMsMsDataByStructureId(null, formulaId, alignedFeatureId);
    }

    AnnotatedMsMsData findAnnotatedMsMsDataByStructureId(@Nullable String inchiKey, @NotNull String formulaId, @NotNull String alignedFeatureId);

    String getFingerIdDataCSV(int charge);

    String getCanopusClassyFireDataCSV(int charge);

    String getCanopusNpcDataCSV(int charge);

    @Deprecated
    String findSiriusFtreeJsonById(String formulaId, String alignedFeatureId);
}
