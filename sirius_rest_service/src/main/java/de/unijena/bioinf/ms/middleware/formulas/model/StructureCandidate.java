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

package de.unijena.bioinf.ms.middleware.formulas.model;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.PubmedLinks;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.projectspace.FormulaScoring;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@Setter
public class StructureCandidate {

    String structureName;
    String smiles;

    Double csiScore;
    Double tanimotoSimilarity;
    Double confidenceScore;

    Integer numOfPubMedIds;
    Double xlogP;
    String inchiKey;

    //Extended Results
    /**
     * Array containing the indices of the molecular fingerprint that are available in the structure (1)
     * OPTIONAL: needs to be added by parameter
     */
    short[] fpBitsSet;
    /**
     * List of structure database links belonging to this structure candidate
     * OPTIONAL: needs to be added by parameter
     */
    List<DBLink> dbLinks;
    /**
     * PubMed IDs belonging to this structure candidate
     * OPTIONAL: needs to be added by parameter
     */
    int[] pubmedIds;


    public static StructureCandidate of(Scored<CompoundCandidate> can, FormulaScoring scorings, boolean includeDB, boolean includePubMed) {
        return of(can, null, scorings, includeDB, includePubMed);
    }

    public static StructureCandidate of(Scored<CompoundCandidate> can, @Nullable Fingerprint fp, @Nullable FormulaScoring confidenceScoreProvider,
                                        boolean includeDB, boolean includePubMed) {


        final StructureCandidate sSum = new StructureCandidate();

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
            if (includePubMed)
                sSum.setPubmedIds(pubMedIds.getCopyOfPubmedIDs());
        }

        if (includeDB)
            sSum.setDbLinks(can.getCandidate().getLinks());

        //FP
        if (fp != null)
            sSum.setFpBitsSet(fp.toIndizesArray());

        return sSum;
    }
}
