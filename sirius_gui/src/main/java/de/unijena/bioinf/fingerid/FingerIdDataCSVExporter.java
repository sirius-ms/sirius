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

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FingerIdDataCSVExporter extends CSVExporter {

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


    public void export(Writer writer, FingerIdData data) throws IOException {
        writer.write("inchikey2D\tinchi\tmolecularFormula\trank\tscore\tname\tsmiles\txlogp\tpubchemids\tlinks\n");
        if (data == null) return;
        for (int i = 0; i < data.compounds.length; ++i) {
            final Compound c = data.compounds[i];
            final double score = data.scores[i];
            final int rank = i + 1;
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
            writer.write(c.smiles != null ? escape(c.smiles.smiles) : escape(""));
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
            if (d == null) continue;
            for (int k = 0; k < d.scores.length; ++k) {
                final Compound c = d.compounds[k];
                final Scored<Compound> sc = new Scored<>(c, d.scores[k]);
                candidates.add(sc);
            }
        }

        Collections.sort(candidates, Scored.<Compound>desc());

        if (data == null) return;
        for (int i = 0; i < candidates.size(); ++i) {
            final Compound c = candidates.get(i).getCandidate();
            final double score = candidates.get(i).getScore();
            final int rank = i + 1;
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
            writer.write(c.smiles != null ? escape(c.smiles.smiles) : escape(null));
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
}
