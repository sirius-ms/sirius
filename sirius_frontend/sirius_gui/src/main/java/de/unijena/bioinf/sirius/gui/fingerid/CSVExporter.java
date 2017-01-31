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

package de.unijena.bioinf.sirius.gui.fingerid;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.sirius.fingerid.FingerIdResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;

public class CSVExporter {

    public void exportToFile(File file, FingerIdData data) throws IOException {
        try (final BufferedWriter bw = Files.newBufferedWriter(file.toPath(), Charset.defaultCharset())) {
            export(bw, data);
        }
    }
    public void exportToFile(File file, List<FingerIdData> data) throws IOException {
        try (final BufferedWriter bw = Files.newBufferedWriter(file.toPath(), Charset.defaultCharset())) {
            export(bw, data);
        }
    }

    public void exportFingerIdResults(Writer writer, List<FingerIdResult> results) throws IOException {
        writer.write("inchikey2D\tinchi\tmolecularFormula\trank\tscore\tname\tsmiles\txlogp\tpubchemids\tlinks\n");
        final ArrayList<Scored<FingerprintCandidate>> candidates = new ArrayList<>();
        for (FingerIdResult r : results) candidates.addAll(r.getCandidates());
        Collections.sort(candidates, Scored.<FingerprintCandidate>desc());
        final Multimap<String,String> dbMap = HashMultimap.create();
        final List<String> pubchemIds = new ArrayList<>();
        int rank = 0;
        for (Scored<FingerprintCandidate> r : candidates)  {
            writer.write(r.getCandidate().getInchiKey2D());
            writer.write('\t');
            writer.write(r.getCandidate().getInchi().in2D);
            writer.write('\t');
            writer.write(r.getCandidate().getInchi().extractFormula().toString());
            writer.write('\t');
            writer.write(String.valueOf(++rank));
            writer.write('\t');
            writer.write(String.valueOf(r.getScore()));
            writer.write('\t');
            writer.write(escape(r.getCandidate().getName()));
            writer.write('\t');
            writer.write(escape(r.getCandidate().getSmiles()));
            writer.write('\t');
            writer.write(""); // TODO: add XLOGP
            writer.write('\t');
            pubchemIds.clear();
            dbMap.clear();
            for (DBLink l : r.getCandidate().getLinks()) {
                if (l.name.equals(DatasourceService.Sources.PUBCHEM.name)) {
                    pubchemIds.add(l.id);
                } else {
                    dbMap.put(l.name, l.id);
                }
            }
            writer.write(Joiner.on(';').join(pubchemIds));
            writer.write('\t');
            links(writer, dbMap);
            writer.write('\n');

        }

    }

    public void export(Writer writer, FingerIdData data) throws IOException {
        writer.write("inchikey2D\tinchi\tmolecularFormula\trank\tscore\tname\tsmiles\txlogp\tpubchemids\tlinks\n");
        if (data==null) return;
        for (int i=0; i < data.compounds.length; ++i) {
            final Compound c = data.compounds[i];
            final double score = data.scores[i];
            final int rank = i+1;
            writer.write(c.inchi.key2D());
            writer.write('\t');
            writer.write(c.inchi.in2D);
            writer.write('\t');
            writer.write(c.inchi.extractFormula().toString());
            writer.write('\t');
            writer.write(String.valueOf(rank));
            writer.write('\t');
            writer.write(String.valueOf(score));
            writer.write('\t');
            writer.write(escape(c.name));
            writer.write('\t');
            writer.write(c.smiles!=null ? escape(c.smiles.smiles) : escape(""));
            writer.write('\t');
            if (Double.isNaN(c.xlogP)) writer.write("\"\"");
            else writer.write(String.valueOf(c.xlogP));
            writer.write('\t');
            list(writer, c.pubchemIds);
            writer.write('\t');
            links(writer, c);
            writer.write('\n');
        }
    }

    public void export(Writer writer, List<FingerIdData> data) throws IOException {
        writer.write("inchikey2D\tinchi\tmolecularFormula\trank\tscore\tname\tsmiles\txlogp\tpubchemids\tlinks\n");
        final List<Scored<Compound>> candidates = new ArrayList<>();
        for (FingerIdData d : data) {
            if (d==null) continue;
            for (int k=0; k < d.scores.length; ++k) {
                final Compound c = d.compounds[k];
                final Scored<Compound> sc = new Scored<>(c, d.scores[k]);
                candidates.add(sc);
            }
        }

        Collections.sort(candidates, Scored.<Compound>desc());;

        if (data==null) return;
        for (int i=0; i < candidates.size(); ++i) {
            final Compound c = candidates.get(i).getCandidate();
            final double score = candidates.get(i).getScore();
            final int rank = i+1;
            writer.write(c.inchi.key2D());
            writer.write('\t');
            writer.write(c.inchi.in2D);
            writer.write('\t');
            writer.write(c.inchi.extractFormula().toString());
            writer.write('\t');
            writer.write(String.valueOf(rank));
            writer.write('\t');
            writer.write(String.valueOf(score));
            writer.write('\t');
            writer.write(escape(c.name));
            writer.write('\t');
            writer.write(c.smiles!=null ? escape(c.smiles.smiles) : escape(null));
            writer.write('\t');
            if (Double.isNaN(c.xlogP)) writer.write("\"\"");
            else writer.write(String.valueOf(c.xlogP));
            writer.write('\t');
            list(writer, c.pubchemIds);
            writer.write('\t');
            links(writer, c);
            writer.write('\n');
        }
    }

    public static void list(Writer writer, int[] pubchemIds) throws IOException {
        if (pubchemIds==null || pubchemIds.length==0) {
            writer.write("\"\"");
        } else {
            writer.write(String.valueOf(pubchemIds[0]));
            for (int i=1; i < pubchemIds.length; ++i) {
                writer.write(';');
                writer.write(String.valueOf(pubchemIds[i]));
            }
        }
    }

    public static void links(Writer w, Compound c) throws IOException {
        if (c.databases==null) {
            w.write("\"\"");
            return;
        } else links(w, c.databases);
    }

    public static void links(Writer w, Multimap<String,String> databases) throws IOException {
        final Iterator<Map.Entry<String, Collection<String>>> iter = databases.asMap().entrySet().iterator();
        if (!iter.hasNext()) {
            w.write("\"\"");
            return;
        }
        Map.Entry<String, Collection<String>> x = iter.next();
        w.write(x.getKey());
        Collection<String> col = withoutNulls(x.getValue());
        if (col.size()>0) {
            w.write(":(");
            w.write(escape(Joiner.on(' ').join(col)));
            w.write(")");
        }
        while (iter.hasNext()) {
            w.write(';');
            x = iter.next();
            w.write(x.getKey());
            col = withoutNulls(x.getValue());
            if (col.size()>0) {
                w.write(":(");
                w.write(escape(Joiner.on(' ').join(col)));
                w.write(")");
            }
        }
    }

    public static String escape(String name) {
        if (name==null) return "\"\"";
        return name.replace('\t', ' ').replace('"', '\'');
    }

    public static Collection<String> withoutNulls(Collection<String> in) {
        return Collections2.filter(in, new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input != null;
            }
        });
    }

}
