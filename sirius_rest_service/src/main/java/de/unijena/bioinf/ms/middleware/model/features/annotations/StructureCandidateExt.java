/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.model.features.annotations;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.PubmedLinks;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.FormulaScoring;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StructureCandidateExt extends StructureCandidate {
    /**
     * molecular formula of this candidate
     */
    protected String molecularFormula;
    /**
     * Adduct of this candidate
     */
    protected String adduct;

    public static StructureCandidateExt of(Scored<CompoundCandidate> can, FormulaScoring scorings,
                                           EnumSet<OptFields> optFields,
                                           FormulaResultId fid
    ) {
        return of(can, null, scorings, optFields, fid.getMolecularFormula(), fid.getIonType());
    }

    public static StructureCandidateExt of(Scored<CompoundCandidate> can, FormulaScoring scorings,
                                           EnumSet<OptFields> optFields,
                                           MolecularFormula formula,
                                           PrecursorIonType adduct
    ) {
        return of(can, null, scorings, optFields, formula, adduct);
    }

    public static StructureCandidateExt of(Scored<CompoundCandidate> can, @Nullable Fingerprint fp,
                                           @Nullable FormulaScoring confidenceScoreProvider,
                                           EnumSet<OptFields> optFields,
                                           FormulaResultId fid
    ) {
        return of(can, fp, confidenceScoreProvider, optFields, fid.getMolecularFormula(), fid.getIonType());
    }

    public static StructureCandidateExt of(Scored<CompoundCandidate> can, @Nullable Fingerprint fp,
                                           @Nullable FormulaScoring confidenceScoreProvider,
                                           EnumSet<OptFields> optFields,
                                           MolecularFormula formula,
                                           PrecursorIonType adduct
    ) {


        final StructureCandidateExt sSum = new StructureCandidateExt();
        sSum.setMolecularFormula(formula.toString());
        sSum.setAdduct(adduct.toString());
        // scores
        sSum.setCsiScore(can.getScore());
        sSum.setTanimotoSimilarity(can.getCandidate().getTanimoto());
        if (confidenceScoreProvider != null)
            confidenceScoreProvider.getAnnotation(ConfidenceScore.class).map(ConfidenceScore::score).ifPresent(sSum::setConfidenceScore);

        //Structure information
        //check for "null" strings since the database might not be perfectly curated
        final String n = can.getCandidate().getName();
        if (n != null && !n.isEmpty() && !n.equals("null"))
            sSum.setStructureName(n);

        sSum.setSmiles(can.getCandidate().getSmiles());
        sSum.setInchiKey(can.getCandidate().getInchiKey2D());
        sSum.setXlogP(can.getCandidate().getXlogp());

        //meta data
        PubmedLinks pubMedIds = can.getCandidate().getPubmedIDs();
        if (pubMedIds != null) {
            sSum.setNumOfPubMedIds(pubMedIds.getNumberOfPubmedIDs());
            if (optFields.contains(OptFields.pubmedIds))
                sSum.setPubmedIds(pubMedIds.getCopyOfPubmedIDs());
        }

        if (optFields.contains(OptFields.dbLinks))
            sSum.setDbLinks(can.getCandidate().getLinks());

        if (optFields.contains(OptFields.refSpectraLinks))
            sSum.setRefSpectraLinks(List.of());
        //todo add reference spectra links
//            sSum.setDbLinks(can.getCandidate().getReferenceSpectraSplash());

        //FP
        if (fp != null && optFields.contains(OptFields.fingerprint))
            sSum.setFingerprint(BinaryFingerprint.from(fp));

        return sSum;
    }
}
