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

package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.annotations.ResultAnnotation;
import de.unijena.bioinf.sirius.IdentificationResult;

import java.util.List;

public class FingerIdResult implements Annotated<ResultAnnotation>, DataAnnotation {
    protected final Annotations<ResultAnnotation> annotations;
    public final FTree sourceTree;

    @Override
    public Annotations<ResultAnnotation> annotations() {
        return annotations;
    }

    public FingerIdResult(IdentificationResult formulaResult) {
        this(formulaResult.getTree());
    }

    public FingerIdResult(FTree sourceTree) {
        this.annotations = new Annotations<>();
        this.sourceTree = sourceTree;
    }

    public FTree getSourceTree() {
        return sourceTree;
    }

    public MolecularFormula getMolecularFormula() {
        return sourceTree.getRoot().getFormula();
    }

    public PrecursorIonType getPrecursorIonType() {
        return sourceTree.getAnnotationOrThrow(PrecursorIonType.class);
    }

    public ConfidenceScore getConfidence() {
        return getAnnotation(ConfidenceResult.class).orElse(ConfidenceResult.NaN).score;
    }

    public ProbabilityFingerprint getPredictedFingerprint() {
        return getAnnotation(FingerprintResult.class).map(r -> r.fingerprint).orElse(null);
    }

    public List<Scored<FingerprintCandidate>> getFingerprintCandidates() {
        return getAnnotation(FingerblastResult.class).map(FingerblastResult::getResults).orElse(null);
    }

    public List<Scored<FingerprintCandidate>> getMsNovelistFingerprintCandidates() {
        return getAnnotation(MsNovelistFingerblastResult.class).map(MsNovelistFingerblastResult::getResults).orElse(null);
    }
}
