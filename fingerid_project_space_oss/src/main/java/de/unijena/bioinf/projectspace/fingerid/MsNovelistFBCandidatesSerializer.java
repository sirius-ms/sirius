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
import de.unijena.bioinf.fingerid.blast.MsNovelistCompoundCandidate;
import de.unijena.bioinf.fingerid.blast.MsNovelistFBCandidates;
import de.unijena.bioinf.projectspace.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static de.unijena.bioinf.projectspace.fingerid.MsNovelistFingerIdLocations.MSNOVELIST_FINGERBLAST;

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
        final MsNovelistFBCandidateNumber numC = id.getAnnotation(MsNovelistFBCandidateNumber.class).orElse(MsNovelistFBCandidateNumber.ALL);

        reader.table(MSNOVELIST_FINGERBLAST.relFilePath(id), true, 0, numC.value, (row) -> {
            if (row.length == 0) return;
            final String smiles = row[2];
            final double score = Double.parseDouble(row[3]);

            final MsNovelistCompoundCandidate candidate = new MsNovelistCompoundCandidate(smiles, Double.parseDouble(row[5]));
            candidate.setTanimoto(Double.valueOf(row[4]));

            results.add(new Scored<>(candidate, score));
        });
        return results;
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<MsNovelistFBCandidates> optFingeridResult) throws IOException {
        final MsNovelistFBCandidates fingerblastResult = optFingeridResult.orElseThrow(() -> new IllegalArgumentException("Could not find FingerIdResult to write for ID: " + id));
        final String[] header = new String[]{
                "molecularFormula", "rank", "smiles", "score", "tanimotoSimilarity", "rnnScore"
        };
        final String[] row = new String[header.length];
        final AtomicInteger ranking = new AtomicInteger(0);
        writer.table(MSNOVELIST_FINGERBLAST.relFilePath(id), header, fingerblastResult.getResults().stream().map((hit) -> {
            MsNovelistCompoundCandidate c = hit.getCandidate();
            row[0] = id.getMolecularFormula().toString();
            row[1] = String.valueOf(ranking.incrementAndGet());
            row[2] = c.getSmiles();
            row[3] = String.valueOf(hit.getScore());
            row[4] = String.valueOf(c.getTanimoto());
            row[5] = String.valueOf(c.getRnnScore());
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
