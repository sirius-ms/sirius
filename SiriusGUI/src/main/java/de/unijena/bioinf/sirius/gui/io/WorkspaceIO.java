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

package de.unijena.bioinf.sirius.gui.io;

import com.google.common.base.Function;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.JenaMsParser;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class WorkspaceIO {

    public List<ExperimentContainer> load(File file) throws IOException {
        try (final ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)))) {

            ZipEntry entry;
            final TreeMap<Integer, IdentificationResult> results = new TreeMap<>();
            Ms2Experiment currentExperiment = null;
            final TreeMap<Integer, ExperimentContainer> containers = new TreeMap<>();
            int currentExpId = -1;
            while ((entry=zin.getNextEntry())!=null) {
                final String name = entry.getName();
                if (name.endsWith("/")) {
                    if (currentExpId>=0 && currentExperiment!=null)
                        containers.put(currentExpId, SiriusDataConverter.siriusToMyxoContainer(currentExperiment, new ArrayList<>(results.values())));
                    currentExpId = Integer.parseInt(name.substring(0,name.length()-1));
                    currentExperiment = null;
                    results.clear();
                } else if (name.endsWith(".ms")) {
                    currentExperiment = readZip(new JenaMsParser(), zin);
                } else if (name.endsWith(".json")) {
                    final int rank = Integer.parseInt(name.substring(name.lastIndexOf('/')+1, name.lastIndexOf('.')));
                    final FTree tree = readZip(new FTJsonReader(), zin);
                    results.put(rank, new IdentificationResult(tree, rank));
                }
            }
            if (currentExpId>=0 && currentExperiment!=null)
                containers.put(currentExpId, SiriusDataConverter.siriusToMyxoContainer(currentExperiment, new ArrayList<IdentificationResult>(results.values())));
            return new ArrayList<>(containers.values());
        }
    }

    public void store(List<ExperimentContainer> containers, File file) throws IOException {
        try (final ZipOutputStream stream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            int k=0;
            for (ExperimentContainer c : containers) {
                storeContainer(c, ++k, stream);
            }
        }
    }

    private void storeContainer(ExperimentContainer c, int i, ZipOutputStream stream) throws IOException {
        final String prefix = i + "/";
        final ZipEntry dir = new ZipEntry(prefix);
        stream.putNextEntry(dir);
        // write INPUT data
        final ZipEntry msfile = new ZipEntry(prefix + "experiment.ms");
        stream.putNextEntry(msfile);
        final Ms2Experiment exp = SiriusDataConverter.experimentContainerToSiriusExperiment(c);
        stream.write(buffer(new Function<BufferedWriter, Void>() {
            @Override
            public Void apply(BufferedWriter input) {
                final JenaMsWriter writer = new JenaMsWriter();
                try {
                    writer.write(input, exp);
                } catch (IOException e) {
                    throw new RuntimeException();
                }
                return null;
            }
        }).getBytes(Charset.forName("UTF-8")));
        // if results available, write trees
        if (c.getRawResults()!=null && !c.getRawResults().isEmpty()) {
            final List<IdentificationResult> irs = c.getRawResults();
            for (final IdentificationResult ir : irs) {
                final ZipEntry tree = new ZipEntry(prefix + ir.getRank() + ".json");
                stream.putNextEntry(tree);
                stream.write(buffer(new Function<BufferedWriter, Void>() {
                    @Override
                    public Void apply(BufferedWriter input) {
                        try {
                            new FTJsonWriter().writeTree(input,ir.getTree());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }
                }).getBytes(Charset.forName("UTF-8")));
            }
        }
    }

    private static <T> T readZip(Parser<T> parser, ZipInputStream zin) throws IOException {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream(4096);
        final byte[] buf = new byte[4096];
        int c=0;
        while ((c=zin.read(buf))>0) {
            bout.write(buf, 0, c);
        }
        return parser.parse(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bout.toByteArray()))),null);
    }

    private static String buffer(Function<BufferedWriter, Void> f) {
        try {
            final StringWriter sw = new StringWriter(1024);
            final BufferedWriter bw = new BufferedWriter(sw);
            f.apply(bw);
            bw.close();
            return sw.toString();
        } catch (IOException e) {
            assert false; // StringIO should not raise IO exceptions
            throw new RuntimeException(e);
        }
    }

}
