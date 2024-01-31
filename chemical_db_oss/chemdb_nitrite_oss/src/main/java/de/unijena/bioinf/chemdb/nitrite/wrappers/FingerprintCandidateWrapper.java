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

package de.unijena.bioinf.chemdb.nitrite.wrappers;

import de.unijena.bioinf.ChemistryBase.chem.InChIs;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class  FingerprintCandidateWrapper {

    @Id
    long pk;

//    @Id
    String inchiKey;

    String formula;
    double mass;
    CompoundCandidate candidate;
    Fingerprint fingerprint;

    public FingerprintCandidate getFingerprintCandidate() {
        return new FingerprintCandidate(candidate, fingerprint);
    }
    public static FingerprintCandidateWrapper of(MolecularFormula formula, FingerprintCandidate candidate) {
        return new FingerprintCandidateWrapper(0, candidate.getInchiKey2D(), formula.toString(), formula.getMass(), candidate.toCompoundCandidate(), candidate.getFingerprint());
    }

    public static FingerprintCandidateWrapper of(FingerprintCandidate candidate) throws UnknownElementException {
        MolecularFormula formula = InChIs.extractNeutralFormulaByAdjustingHsOrThrow(candidate.getInchi().in2D);
        return FingerprintCandidateWrapper.of(formula, candidate);
    }
}
