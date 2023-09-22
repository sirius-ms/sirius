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

    AlignedFeature findAlignedFeaturesById(String alignFeatureId, EnumSet<AlignedFeature.OptFields> optFields);

    default AlignedFeature findAlignedFeaturesById(String alignFeatureId, AlignedFeature.OptFields... optFields) {
        return findAlignedFeaturesById(alignFeatureId, EnumSet.copyOf(List.of(optFields)));
    }

    void deleteAlignedFeaturesById(String alignFeatureId);

    Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignFeatureId, Pageable pageable, EnumSet<FormulaCandidate.OptFields> optFields);

    default Page<FormulaCandidate> findFormulaCandidatesByFeatureId(String alignFeatureId, Pageable pageable, FormulaCandidate.OptFields... optFields) {
        return findFormulaCandidatesByFeatureId(alignFeatureId, pageable, EnumSet.copyOf(List.of(optFields)));
    }

    FormulaCandidate findFormulaCandidateByFeatureIdAndId(String formulaId, String alignFeatureId, EnumSet<FormulaCandidate.OptFields> optFields);

    default FormulaCandidate findFormulaCandidateByFeatureIdAndId(String formulaId, String alignFeatureId, FormulaCandidate.OptFields... optFields) {
        return findFormulaCandidateByFeatureIdAndId(formulaId, alignFeatureId, EnumSet.copyOf(List.of(optFields)));
    }

    Page<StructureCandidate> findStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignFeatureId, Pageable pageable, EnumSet<StructureCandidate.OptFields> optFields);

    default Page<StructureCandidate> findStructureCandidatesByFeatureIdAndFormulaId(String formulaId, String alignFeatureId, Pageable pageable, StructureCandidate.OptFields... optFields) {
        return findStructureCandidatesByFeatureIdAndFormulaId(formulaId, alignFeatureId, pageable, EnumSet.copyOf(List.of(optFields)));
    }

    StructureCandidate findTopStructureCandidatesByFeatureId(String alignFeatureId, EnumSet<StructureCandidate.OptFields> optFields);

    default StructureCandidate findTopStructureCandidatesByFeatureId(String alignFeatureId, StructureCandidate.OptFields... optFields) {
        return findTopStructureCandidatesByFeatureId(alignFeatureId, EnumSet.copyOf(List.of(optFields)));
    }
}
