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

package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.fingerid.blast.MsNovelistCompoundCandidate;
import de.unijena.bioinf.fingerid.blast.MsNovelistFBCandidates;
import de.unijena.bioinf.projectspace.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static de.unijena.bioinf.projectspace.fingerid.FingerIdLocations.MSNOVELIST_FINGERBLAST;

public class MsNovelistFBCandidatesSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, MsNovelistFBCandidates> {

    @Override
    public @Nullable MsNovelistFBCandidates read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        final ArrayList<Scored<MsNovelistCompoundCandidate>> c = readCandidates(reader, id, container);
        return c == null ? null : new MsNovelistFBCandidates(c);
    }

    private ArrayList<Scored<MsNovelistCompoundCandidate>> readCandidates(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        if (!reader.exists(MSNOVELIST_FINGERBLAST.relFilePath(id)))
            return null;

        final ArrayList<Scored<MsNovelistCompoundCandidate>> results = new ArrayList<>();
        final FBCandidateNumber numC = id.getAnnotation(FBCandidateNumber.class).orElse(FBCandidateNumber.ALL);

        reader.table(MSNOVELIST_FINGERBLAST.relFilePath(id), true, 0, numC.value, (row) -> {
            if (row.length == 0) return;

            final MsNovelistCompoundCandidate candidate = new MsNovelistCompoundCandidate(new InChI(row[0], row[1]));
            candidate.setSmiles(row[5]);
            candidate.setTanimoto(Double.valueOf(row[6]));
            candidate.setRnnScore(Double.parseDouble(row[7]));

            results.add(new Scored<>(candidate, Double.parseDouble(row[4])));
        });
        return results;
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<MsNovelistFBCandidates> optFingeridResult) throws IOException {
        final MsNovelistFBCandidates fingerblastResult = optFingeridResult.orElseThrow(() -> new IllegalArgumentException("Could not find FingerIdResult to write for ID: " + id));
        final String[] header = new String[]{
                "inchikey2D", "inchi", "molecularFormula", "rank", "score", "smiles", "tanimotoSimilarity", "rnnScore"
        };
        final String[] row = new String[header.length];
        final AtomicInteger ranking = new AtomicInteger(0);
        writer.table(MSNOVELIST_FINGERBLAST.relFilePath(id), header, fingerblastResult.getResults().stream().map((hit) -> {
            MsNovelistCompoundCandidate c = hit.getCandidate();
            row[0] = c.getInchiKey2D();
            row[1] = c.getInchi().in2D;
            row[2] = id.getMolecularFormula().toString();
            row[3] = String.valueOf(ranking.incrementAndGet());
            row[4] = String.valueOf(hit.getScore());
            row[5] = c.getSmiles();
            row[6] = String.valueOf(c.getTanimoto());
            row[7] = String.valueOf(c.getRnnScore());
            return row;
        })::iterator);
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        writer.deleteIfExists(MSNOVELIST_FINGERBLAST.relFilePath(id));
    }

    @Override
    public void deleteAll(ProjectWriter writer) throws IOException {
        writer.deleteIfExists(MSNOVELIST_FINGERBLAST.relDir());
    }
}
