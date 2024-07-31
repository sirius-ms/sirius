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
import de.unijena.bioinf.ChemistryBase.chem.InChIs;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.PubmedLinks;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.projectspace.*;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.unijena.bioinf.projectspace.fingerid.FingerIdLocations.FINGERBLAST;

public class FBCandidatesSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, FBCandidates> {
    public static final List<Class<? extends SerializerParameter>> supportedParameters = List.of(FBCandidateNumber.class);
    protected ArrayList<Scored<CompoundCandidate>> readCandidates(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        if (!reader.exists(FINGERBLAST.relFilePath(id)))
            return null;

        final Pattern dblinkPat = Pattern.compile("^.+?:\\(.*\\)$");
        final ArrayList<Scored<CompoundCandidate>> results = new ArrayList<>();
        final FBCandidateNumber numC = id.getAnnotation(FBCandidateNumber.class).orElse(FBCandidateNumber.ALL);

        reader.table(FINGERBLAST.relFilePath(id), true, 0, numC.value, (row) -> {
            if (row.length == 0) return;
            final double score = Double.parseDouble(row[4]);
            final InChI inchi = InChIs.newInChI(row[0], row[1]);
            final String name = row[5], smiles = row[6];
            final double xlogp = (row[7] != null && !row[7].isBlank() && !row[7].equals("N/A")) ? Double.parseDouble(row[7]) : Double.NaN;

            final CompoundCandidate candidate = new CompoundCandidate(inchi);
            candidate.setName(name);
            candidate.setXlogp(xlogp);
            candidate.setSmiles(smiles);
            candidate.setPubmedIDs(PubmedLinks.fromString(row[8]));

            //here we reconstruct the bitset from db links to add ne non persistent custom
            //db flags
            {
                final List<DBLink> links = new ArrayList<>();
                final List<String> importedNames = new ArrayList<>();
                for (String db : row[9].split(";")) {
                    Matcher matcher = dblinkPat.matcher(db);
                    db = db.trim();
                    if (matcher.matches()) {
                        String[] split = matcher.group().split(":\\(");

                        final String dbName = split[0].trim();
                        final String ids = split[1].trim();
                        if (!ids.isBlank())
                            for (String dbId : ids.substring(0, ids.length() - 1).split(","))
                                links.add(new DBLink(dbName,
                                        dbId.isBlank() || dbId.equalsIgnoreCase("null") || dbId.equalsIgnoreCase("na") || dbId.equalsIgnoreCase("n/a")
                                                ? null : dbId.trim()));
                        if (CustomDataSources.containsDB(dbName))
                            importedNames.add(dbName);
                        else
                            LoggerFactory.getLogger(getClass()).warn("Importing Unmatched DB flag '" + dbName + "'. This might be due to an Custom DB that is not available.");
                    } else {
                        LoggerFactory.getLogger(getClass()).warn("Could not match DB link '" + db + "' Skipping this entry!");
                    }
                }

                candidate.setBitset(CustomDataSources.getDBFlagsFromNames(importedNames));
                candidate.setLinks(links);
            }

            if (row.length > 10 && row[10] != null && !row[10].isBlank() && !row[10].equals("N/A")) {
                candidate.setTanimoto(Double.valueOf(row[10]));
            } else
                candidate.setTanimoto(null);

            if (row.length > 11 && row[11] != null && !row[11].isBlank() && !row[11].equals("N/A")) {
                candidate.setMcesToTopHit(Double.valueOf(row[11]));
            } else
                candidate.setMcesToTopHit(null);

            // we sanity check if reconstructed NON custom db bits match the stored bit set.
            if (row.length > 12 && !row[12].isBlank()) {
                final long linkbasedNonCustomBits = CustomDataSources.removeCustomSourceFromFlag(candidate.getBitset());
                final long bits = Long.parseLong(row[12]);
                if (linkbasedNonCustomBits != bits)
                    LoggerFactory.getLogger(getClass()).warn("Reconstructed db flags differ from imported.'" + linkbasedNonCustomBits + "' vs '" + bits + "'.");
            }

            results.add(new Scored<>(candidate, score));
        });
        return results;
    }

    @Override
    public FBCandidates read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        final ArrayList<Scored<CompoundCandidate>> c = readCandidates(reader, id, container);
        return c == null ? null : new FBCandidates(c);
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<FBCandidates> optFingeridResult) throws IOException {
        final FBCandidates fingerblastResult = optFingeridResult.orElseThrow(() -> new IllegalArgumentException("Could not find FingerIdResult to write for ID: " + id));

        final String[] header = new String[]{
                "inchikey2D", "inchi", "molecularFormula", "rank", "score", "name", "smiles", "xlogp", "PubMedIds", "links", "tanimotoSimilarity","mcesDistanceToTopHit", "dbflags"
        };
        final String[] row = new String[header.length];
        final AtomicInteger ranking = new AtomicInteger(0);
        writer.table(FINGERBLAST.relFilePath(id), header, fingerblastResult.getResults().stream().map((hit) -> {
            CompoundCandidate c = hit.getCandidate();
            row[0] = c.getInchiKey2D();
            row[1] = c.getInchi().in2D;
            row[2] = id.getMolecularFormula().toString();
            row[3] = String.valueOf(ranking.incrementAndGet());
            row[4] = String.valueOf(hit.getScore());
            row[5] = c.getName();
            row[6] = c.getSmiles();
            row[7] = Double.isNaN(c.getXlogp()) ? "N/A" : String.valueOf(c.getXlogp());
            row[8] = c.getPubmedIDs() != null ? c.getPubmedIDs().toString() : "";
            row[9] = c.getLinkedDatabases().entrySet().stream().map((k) -> k.getValue().isEmpty() ? k.getKey() : k.getKey() + ":(" + String.join(", ", k.getValue()) + ")").collect(Collectors.joining("; "));
            row[10] = c.getTanimoto() == null ? "N/A" : String.valueOf(c.getTanimoto());
            row[11] = c.getMcesToTopHit() == null ? "N/A": String.valueOf(c.getMcesToTopHit());
            row[12] = String.valueOf(CustomDataSources.removeCustomSourceFromFlag(c.getBitset())); //We remove custom db bits since they are only valid ad runtime and user dependent.
            return row;
        })::iterator);

    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        writer.deleteIfExists(FINGERBLAST.relFilePath(id));
    }

    @Override
    public void deleteAll(ProjectWriter writer) throws IOException {
        writer.deleteIfExists(FINGERBLAST.relDir());
    }
}
