/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;
import de.unijena.bioinf.fragmenter.CombinatorialFragment;
import de.unijena.bioinf.fragmenter.CombinatorialSubtree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface ConfidenceScorer {

    default double computeConfidence(@NotNull final Ms2Experiment exp,
                                     @NotNull List<Scored<FingerprintCandidate>> allDbCandidatesScore,
                                     @NotNull ParameterStore parametersWithQuery,
                                     @Nullable Predicate<FingerprintCandidate> filter,
                                     @NotNull boolean structureSearchDBIsPubChem,
                                     @NotNull FTree[] ftrees,
                                     @NotNull CombinatorialSubtree[] combSubtrees,
                                     @NotNull Map<Fragment, ArrayList<CombinatorialFragment>>[] mappings,
                                     @NotNull CanopusResult canopusResult,
                                     @NotNull CanopusResult canopusResultTopHit) {
        if (filter == null)
            return computeConfidence(exp, allDbCandidatesScore, allDbCandidatesScore,parametersWithQuery,structureSearchDBIsPubChem,ftrees,combSubtrees,mappings,canopusResult,canopusResultTopHit);

        return computeConfidence(exp,
                allDbCandidatesScore,
                allDbCandidatesScore.stream().filter(c -> filter.test(c.getCandidate())).collect(Collectors.toList()),
                parametersWithQuery,structureSearchDBIsPubChem,ftrees,combSubtrees,mappings,canopusResult,canopusResultTopHit);
    }

    double computeConfidence(@NotNull final Ms2Experiment exp,
                             @NotNull List<Scored<FingerprintCandidate>> allDbCandidatesScore,
                             @NotNull List<Scored<FingerprintCandidate>> searchDBCandidatesScore,
                             @NotNull ParameterStore parametersWithQuery,
                             @NotNull boolean structureSearchDBIsPubChem,
                             @NotNull FTree[] ftrees,
                             @NotNull CombinatorialSubtree[] combSubtrees,
                             @NotNull Map<Fragment, ArrayList<CombinatorialFragment>>[] mappings,
                             @NotNull CanopusResult canopusResult,
                             @NotNull CanopusResult canopusResultTopHit);
}