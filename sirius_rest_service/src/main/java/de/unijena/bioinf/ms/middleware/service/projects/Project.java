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

import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.annotations.FormulaCandidate;
import de.unijena.bioinf.ms.middleware.model.features.annotations.StructureCandidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.EnumSet;
import java.util.List;

public interface Project {

    Page<AlignedFeature> findAlignedFeatures(Pageable pageable, EnumSet<AlignedFeature.OptFields> optFields);

    default Page<AlignedFeature> findAlignedFeatures(Pageable pageable, AlignedFeature.OptFields... optFields) {
        return findAlignedFeatures(pageable, EnumSet.copyOf(List.of(optFields)));
    }

    AlignedFeature findAlignedFeaturesById(String alignedFeatureId, EnumSet<AlignedFeature.OptFields> optFields);

    default AlignedFeature findAlignedFeaturesById(String alignedFeatureId, AlignedFeature.OptFields... optFields) {
        return findAlignedFeaturesById(alignedFeatureId, EnumSet.copyOf(List.of(optFields)));
    }

    void deleteAlignedFeaturesById(String alignedFeatureId);

    Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, EnumSet<FormulaCandidate.OptFields> optFields);

    default Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignedFeatureId, Pageable pageable, FormulaCandidate.OptFields... optFields) {
        return findFormulaCandidatesByFeatureId(alignedFeatureId, pageable, EnumSet.copyOf(List.of(optFields)));
    }

    FormulaCandidate findFormulaCandidateByFeatureIdAndId(String formulaId, String alignedFeatureId, EnumSet<FormulaCandidate.OptFields> optFields);

    default FormulaCandidate findFormulaCandidateByFeatureIdAndId(String formulaId, String alignedFeatureId, FormulaCandidate.OptFields... optFields) {
        return findFormulaCandidateByFeatureIdAndId(formulaId, alignedFeatureId, EnumSet.copyOf(List.of(optFields)));
    }

    Page<StructureCandidate> findStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, EnumSet<StructureCandidate.OptFields> optFields);

    default Page<StructureCandidate> findStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignedFeatureId, Pageable pageable, StructureCandidate.OptFields... optFields) {
        return findStructureCandidatesByFeatureIdAndFormulaId(formulaId, alignedFeatureId, pageable, EnumSet.copyOf(List.of(optFields)));
    }

    StructureCandidate findTopStructureCandidatesByFeatureId(String alignedFeatureId, EnumSet<StructureCandidate.OptFields> optFields);

    default StructureCandidate findTopStructureCandidatesByFeatureId(String alignedFeatureId, StructureCandidate.OptFields... optFields) {
        return findTopStructureCandidatesByFeatureId(alignedFeatureId, EnumSet.copyOf(List.of(optFields)));
    }
}
