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
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.blast.parameters.ParameterStore;
import org.apache.xpath.operations.Bool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface ConfidenceScorer {

    default double computeConfidence(@NotNull final Ms2Experiment exp,
                                     @NotNull List<Scored<FingerprintCandidate>> allDbCandidatesScoreA,
                                     @NotNull List<Scored<FingerprintCandidate>> allDbCandidatesScoreB,
                                     @NotNull ParameterStore parametersWithQuery,
                                     @Nullable Predicate<FingerprintCandidate> filter,
                                     @NotNull boolean structureSearchDBIsPubChem) {
        if (filter == null)
            return computeConfidence(exp, allDbCandidatesScoreA, allDbCandidatesScoreB, allDbCandidatesScoreA, allDbCandidatesScoreB, parametersWithQuery,structureSearchDBIsPubChem);

        return computeConfidence(exp,
                allDbCandidatesScoreA, allDbCandidatesScoreB,
                allDbCandidatesScoreA.stream().filter(c -> filter.test(c.getCandidate())).collect(Collectors.toList()),
                allDbCandidatesScoreB.stream().filter(c -> filter.test(c.getCandidate())).collect(Collectors.toList()),
                parametersWithQuery,structureSearchDBIsPubChem);
    }

    double computeConfidence(@NotNull final Ms2Experiment exp,
                             @NotNull List<Scored<FingerprintCandidate>> allDbCandidatesScoreA,
                             @NotNull List<Scored<FingerprintCandidate>> allDbCandidatesScoreB,
                             @NotNull List<Scored<FingerprintCandidate>> searchDBCandidatesScoreA,
                             @NotNull List<Scored<FingerprintCandidate>> searchDBCandidatesScoreB,
                             @NotNull ParameterStore parametersWithQuery,
                             @NotNull boolean structureSearchDBIsPubChem);
}