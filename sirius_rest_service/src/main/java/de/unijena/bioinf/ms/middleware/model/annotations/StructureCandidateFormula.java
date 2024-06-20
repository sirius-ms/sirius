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

package de.unijena.bioinf.ms.middleware.model.annotations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.ms.middleware.service.annotations.AnnotationUtils;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.spectraldb.SpectralSearchResult;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(value = {}, ignoreUnknown = true)
public class StructureCandidateFormula extends StructureCandidateScored {
    /**
     * Molecular formula of this candidate
     */
    protected String molecularFormula;
    /**
     * Adduct of this candidate
     */
    protected String adduct;
    /**
     * Id of the corresponding Formula candidate
     */
    protected String formulaId;

    public static StructureCandidateFormula of(Scored<? extends CompoundCandidate> can,
                                               EnumSet<OptField> optFields,
                                               FormulaResultId fid
    ) {
        return of(can, null, null, optFields, fid.getMolecularFormula(), fid.getIonType(), fid.fileName());
    }

    public static StructureCandidateFormula of(Scored<? extends CompoundCandidate> can,
                                               EnumSet<OptField> optFields,
                                               MolecularFormula formula,
                                               PrecursorIonType adduct,
                                               String fomulaId
    ) {
        return of(can, null, null, optFields, formula, adduct, fomulaId);
    }

    public static StructureCandidateFormula of(Scored<? extends CompoundCandidate> can,
                                               @Nullable Fingerprint fp,
                                               @Nullable SpectralSearchResult libraryMatches,
                                               EnumSet<OptField> optFields,
                                               FormulaResultId fid
    ) {
        return of(can, fp, libraryMatches, optFields, fid.getMolecularFormula(), fid.getIonType(), fid.fileName());
    }

    public static StructureCandidateFormula of(Scored<? extends CompoundCandidate> can,
                                               @Nullable Fingerprint fp,
                                               @Nullable SpectralSearchResult libraryMatches,
                                               EnumSet<OptField> optFields,
                                               MolecularFormula formula,
                                               PrecursorIonType adduct,
                                               String fomulaId
    ) {


        final StructureCandidateFormula sSum = new StructureCandidateFormula();
        sSum.setMolecularFormula(formula.toString());
        sSum.setAdduct(adduct.toString());
        sSum.setFormulaId(fomulaId);
        // scores
        sSum.setCsiScore(can.getScore());
        sSum.setTanimotoSimilarity(can.getCandidate().getTanimoto());
        sSum.setMcesDistToTopHit(can.getCandidate().getMcesToTopHit());



        //Structure information
        //check for "null" strings since the database might not be perfectly curated
        final String n = can.getCandidate().getName();
        if (n != null && !n.isEmpty() && !n.equals("null"))
            sSum.setStructureName(n);

        sSum.setSmiles(can.getCandidate().getSmiles());
        sSum.setInchiKey(can.getCandidate().getInchiKey2D());

        double xlogP = can.getCandidate().getXlogp();
        sSum.setXlogP(Double.isNaN(xlogP) || Double.isInfinite(xlogP) ? null : xlogP);

        //meta data
        if (optFields.contains(OptField.dbLinks))
            sSum.setDbLinks(can.getCandidate().getLinks());

        // spectral library matches
        if (libraryMatches != null && optFields.contains(OptField.libraryMatches))
            sSum.setSpectralLibraryMatches(SpectralLibraryMatch.of(libraryMatches, sSum.getInchiKey()));

        //FP
        if (fp != null && optFields.contains(OptField.fingerprint))
            sSum.setFingerprint(AnnotationUtils.asBinaryFingerprint(fp));

        return sSum;
    }
}
