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
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.FormulaCandidate;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Setter
public class  FingerprintCandidateWrapper {

    @Id
    @Getter
    String inchiKey;

    @Getter
    String formula;

    @Getter
    double mass;

    private CompoundCandidate candidate;

    @Getter
    private Fingerprint fingerprint;

    private FingerprintCandidateWrapper() {}

    public FormulaCandidate getFormulaCandidate(@Nullable Long dbFlag, @NotNull PrecursorIonType precursorIonType) {
        if (dbFlag != null)
            return enrichCandidate(candidate, null, dbFlag).toFormulaCandidate(precursorIonType);
        return candidate.toFormulaCandidate(precursorIonType);
    }

    public CompoundCandidate getCandidate(@Nullable String dbName, @Nullable Long dbFlag) {
        return enrichCandidate(candidate, dbName, dbFlag);
    }

    public FingerprintCandidate getFingerprintCandidate(@Nullable String dbName, @Nullable Long dbFlag) {
        return enrichCandidate(new FingerprintCandidate(candidate, fingerprint), dbName, dbFlag);
    }

    private static <C extends CompoundCandidate> C enrichCandidate(@NotNull C fpc, @Nullable String dbName, @Nullable Long dbFlag){
        if (dbName != null) {
            List<DBLink> links = fpc.getMutableLinks();
            if (links != null && !links.isEmpty() ) {
                fpc.getMutableLinks().forEach(l -> {
                    if (l.getName() == null || l.getName().isBlank())
                        l.setName(dbName);
                });
            }else {
                fpc.setLinks(new ArrayList<>(List.of(new DBLink(dbName, null))));
            }
        }
        if (dbFlag != null)
            fpc.setBitset(dbFlag);
        return fpc;
    }

    public static FingerprintCandidateWrapper of(String formula, double mass, @NotNull FingerprintCandidate candidate) {
        return new FingerprintCandidateWrapper(candidate.getInchiKey2D(), formula, mass, candidate.toCompoundCandidate(), candidate.getFingerprint());
    }

    public static FingerprintCandidateWrapper of(MolecularFormula formula, @NotNull FingerprintCandidate candidate) {
        return new FingerprintCandidateWrapper(candidate.getInchiKey2D(), formula.toString(), formula.getMass(), candidate.toCompoundCandidate(), candidate.getFingerprint());
    }

    public static FingerprintCandidateWrapper of(@NotNull FingerprintCandidate candidate) throws UnknownElementException {
        MolecularFormula formula = InChIs.extractNeutralFormulaByAdjustingHsOrThrow(candidate.getInchi().in2D);
        return FingerprintCandidateWrapper.of(formula, candidate);
    }
}
