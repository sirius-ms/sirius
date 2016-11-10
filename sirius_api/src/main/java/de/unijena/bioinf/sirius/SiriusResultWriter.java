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

package de.unijena.bioinf.sirius;

import com.google.common.base.Function;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SiriusResultWriter implements Closeable {

    private ZipOutputStream zout;
    private int entries;

    public SiriusResultWriter(OutputStream outputStream) {
        this.zout = new ZipOutputStream(outputStream);
        this.entries = 0;
    }

    public synchronized void add(Ms2Experiment experiment, List<IdentificationResult> results) throws IOException {
        storeEntry(experiment, results, ++entries, zout);
    }

    private static void storeEntry(final Ms2Experiment exp, final List<IdentificationResult> results, int i, ZipOutputStream stream) throws IOException {

        final String prefix = i + "/";
        final ZipEntry dir = new ZipEntry(prefix);
        stream.putNextEntry(dir);
        stream.closeEntry();

        // write INPUT data

        final ZipEntry msfile = new ZipEntry(prefix + "experiment.ms");
        stream.putNextEntry(msfile);
        stream.write(buffer(new Function<BufferedWriter, Void>() {
            @Override
            public Void apply(BufferedWriter input) {
                final JenaMsWriter writer = new JenaMsWriter();
                try {
                    writer.write(input, exp);
                } catch (IOException e) {
                    LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
                    throw new RuntimeException();
                }
                return null;
            }
        }).getBytes(Charset.forName("UTF-8")));
        stream.closeEntry();

        // if results available, write trees
        if (results.size()>0) {
            for (final IdentificationResult ir : results) {
                final ZipEntry tree = new ZipEntry(prefix + (ir.getRank()+1) + ".json");
                stream.putNextEntry(tree);
                stream.write(buffer(new Function<BufferedWriter, Void>() {
                    @Override
                    public Void apply(BufferedWriter input) {
                        try {
                            new FTJsonWriter().writeTree(input,ir.getRawTree());
                        } catch (IOException e) {
                            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
                            throw new RuntimeException(e);
                        }
                        return null;
                    }
                }).getBytes(Charset.forName("UTF-8")));
                stream.closeEntry();
            }
        }
    }

    @Override
    public synchronized void close() throws IOException {
        zout.close();
    }

    private static String buffer(Function<BufferedWriter, Void> f) {
        try {
            final StringWriter sw = new StringWriter(1024);
            final BufferedWriter bw = new BufferedWriter(sw);
            f.apply(bw);
            bw.close();
            sw.close();
            return sw.toString();
        } catch (IOException e) {
            assert false; // StringIO should not raise IO exceptions
            LoggerFactory.getLogger(SiriusResultWriter.class).error(e.getMessage(),e);
            throw new RuntimeException(e);
        }
    }
}
