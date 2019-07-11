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

package de.unijena.bioinf.fingerid;

import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.FingerprintCandidate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class CSVExporter {

    public void exportFingerIdResultsToFile(File file, FingerIdResult data) throws IOException {
        exportFingerIdResultsToFile(file, Collections.singletonList(data));
    }

    public void exportFingerIdResultsToFile(File file, Iterable<FingerIdResult> data) throws IOException {
        try (final BufferedWriter bw = Files.newBufferedWriter(file.toPath(), Charset.defaultCharset())) {
            exportFingerIdResults(bw, data);
        }
    }

    public void exportFingerIdResults(Writer writer, FingerIdResult results) throws IOException {
        exportFingerIdResults(writer, Collections.singletonList(results));

    }

    public void exportFingerIdResults(Writer writer, Iterable<FingerIdResult> results) throws IOException {
        writer.write("inchikey2D\tinchi\tmolecularFormula\trank\tscore\tname\tsmiles\txlogp\tpubchemids\tlinks\n");
        final ArrayList<Scored<FingerprintCandidate>> candidates = new ArrayList<>();
        for (FingerIdResult r : results) candidates.addAll(r.getCandidates());
        candidates.sort(Scored.desc());
        int rank = 0;
        for (Scored<FingerprintCandidate> r : candidates) {
            final Multimap<String, String> dbMap = r.getCandidate().getLinkedDatabases();

            writer.write(r.getCandidate().getInchiKey2D());
            writer.write('\t');
            writer.write(r.getCandidate().getInchi().in2D);
            writer.write('\t');
            writer.write(r.getCandidate().getInchi().extractFormulaOrThrow().toString());
            writer.write('\t');
            writer.write(String.valueOf(++rank));
            writer.write('\t');
            writer.write(String.valueOf(r.getScore()));
            writer.write('\t');
            writer.write(escape(r.getCandidate().getName()));
            writer.write('\t');
            writer.write(escape(r.getCandidate().getSmiles()));
            writer.write('\t');
            if (Double.isNaN(r.getCandidate().getXlogp())) writer.write("\"\"");
            else writer.write(String.valueOf(r.getCandidate().getXlogp()));
            writer.write('\t');
            list(writer, dbMap.get(DataSource.PUBCHEM.realName).stream().filter(Objects::nonNull).collect(Collectors.toList())); //is this a hack or ok?
            writer.write('\t');
            links(writer, dbMap);
            writer.write('\n');
        }
    }


    public static void list(Writer writer, Collection<String> pubchemIds) throws IOException {

        if (pubchemIds == null || pubchemIds.size() == 0) {
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

    public static void links(Writer w, Multimap<String, String> databases) throws IOException {
        final Iterator<Map.Entry<String, Collection<String>>> iter = databases.asMap().entrySet().iterator();
        if (!iter.hasNext()) {
            w.write("\"\"");
            return;
        }
        Map.Entry<String, Collection<String>> x = iter.next();
        w.write(x.getKey());
        Collection<String> col = withoutNulls(x.getValue());
        if (col.size() > 0) {
            w.write(":(");
            w.write(escape(Joiner.on(' ').join(col)));
            w.write(")");
        }
        while (iter.hasNext()) {
            w.write(';');
            x = iter.next();
            w.write(x.getKey());
            col = withoutNulls(x.getValue());
            if (col.size() > 0) {
                w.write(":(");
                w.write(escape(Joiner.on(' ').join(col)));
                w.write(")");
            }
        }
    }

    public static String escape(String name) {
        if (name == null) return "\"\"";
        return name.replace('\t', ' ').replace('"', '\'');
    }

    public static Collection<String> withoutNulls(Collection<String> in) {
        return Collections2.filter(in, input -> input != null);
    }

}
