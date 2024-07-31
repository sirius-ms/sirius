/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.projectspace.summaries;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.fingerid.blast.TopCSIScore;
import de.unijena.bioinf.projectspace.FormulaResultId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

public class StructureCSVExporter {
    public static final List<String> HEADER_LIST = List.of(new TopCSIScore(0).name(), "molecularFormula", "adduct", "InChIkey2D", "InChI", "name", "smiles", "xlogp", "pubchemids", "links", "dbflags");
    public static final String HEADER = String.join("\t", HEADER_LIST);

    public void exportFingerIdResult(Writer writer, Scored<? extends CompoundCandidate> r, @NotNull FormulaResultId id, boolean writeHeader, @Nullable Integer rank) throws IOException {
        @NotNull Map<String, List<String>> dbMap = r.getCandidate().getLinkedDatabases();


        if (writeHeader) {
            if (rank != null)
                writer.write("rank\t");
            writer.write(HEADER);
            writer.write("\n");
        }

        if (rank != null) {
            writer.write(rank);
            writer.write('\t');
        }

        writer.write(r.getScoreObject().toString());
        writer.write('\t');
        writer.write(id.getMolecularFormula().toString());
        writer.write('\t');
        writer.write(id.getIonType().toString());
        writer.write('\t');
        writer.write(r.getCandidate().getInchiKey2D());
        writer.write('\t');
        writer.write(r.getCandidate().getInchi().in2D);
        writer.write('\t');
        writer.write(escape(r.getCandidate().getName()));
        writer.write('\t');
        writer.write(escape(r.getCandidate().getSmiles()));
        writer.write('\t');
        if (Double.isNaN(r.getCandidate().getXlogp())) writer.write("\"\"");
        else writer.write(String.valueOf(r.getCandidate().getXlogp()));
        writer.write('\t');
        list(writer, dbMap.get(DataSource.PUBCHEM.name()).stream().filter(Objects::nonNull).collect(Collectors.toList())); //is this a hack or ok?
        writer.write('\t');
        links(writer, dbMap);
        writer.write('\t');
        writer.write(String.valueOf(r.getCandidate().getBitset()));
        writer.write('\n');
    }


    public static void list(Writer writer, Collection<String> pubchemIds) throws IOException {

        if (pubchemIds == null || pubchemIds.isEmpty()) {
            writer.write("\"\"");
        } else {
            final Iterator<String> it = pubchemIds.iterator();
            writer.write(it.next());
            while (it.hasNext()) {
                writer.write(';');
                writer.write(it.next());
            }
        }
    }

    public static void links(Writer w, Map<String, List<String>> databases) throws IOException {
        final Iterator<Map.Entry<String, List<String>>> iter = databases.entrySet().iterator();
        if (!iter.hasNext()) {
            w.write("\"\"");
            return;
        }
        Map.Entry<String, List<String>> x = iter.next();
        w.write(x.getKey());
        Collection<String> col = withoutNulls(x.getValue());
        if (!col.isEmpty()) {
            w.write(":(");
            w.write(escape(String.join(" ", col)));
            w.write(")");
        }
        while (iter.hasNext()) {
            w.write(';');
            x = iter.next();
            w.write(x.getKey());
            col = withoutNulls(x.getValue());
            if (!col.isEmpty()) {
                w.write(":(");
                w.write(escape(String.join(" ", col)));
                w.write(")");
            }
        }
    }

    public static String escape(String name) {
        if (name == null) return "\"\"";
        return name.replace('\t', ' ').replace('"', '\'');
    }

    public static List<String> withoutNulls(List<String> in) {
        return in.stream().filter(Objects::nonNull).toList();
    }

}
