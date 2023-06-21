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

package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.AnnotatedSpectrumWriter;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

public class MsIO {

    /**
     * parses a file and return an iterator over all MS/MS experiments contained in this file
     * An experiment consists of all MS and MS/MS spectra belonging to one feature (=compound).
     * <p>
     * Supported file formats are .ms and .mgf
     * <p>
     * The returned iterator supports the close method to close the input stream. The stream is closed automatically,
     * after the last element is iterated. However, it is recommendet to use the following syntax (since java 7):
     * <p>
     * <pre>
     * {@code
     * try ( CloseableIterator<Ms2Experiment> iter = sirius.parse(myfile) ) {
     *   while (iter.hasNext()) {
     *      Ms2Experiment experiment = iter.next();
     *      // ...
     *   }
     * }}
     * </pre>
     *
     * @param file
     * @return
     * @throws IOException
     */

    public static CloseableIterator<Ms2Experiment> readExperimentFromFile(File file) throws IOException {
        return new MsExperimentParser().getParser(file).parseFromFileIterator(file);
    }

    public static FTree readTreeFromFile(File file) throws IOException {
        try (BufferedReader br = FileUtils.getReader(file)) {
            return new FTJsonReader().parse(br, file.toURI());
        }
    }

    public static String getJSONTree(final @NotNull FTree tree) {
        final StringWriter sw = new StringWriter(1024);
        try {
            new FTJsonWriter().writeTree(sw, tree);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sw.toString();
    }

    public static void writeTreeToFile(final @NotNull FTree tree, final @NotNull File target) throws IOException {
        final String name = target.getName();
        if (name.endsWith(".dot")) {
            new FTDotWriter().writeTreeToFile(target, tree);
        } else new FTJsonWriter().writeTreeToFile(target, tree);
    }

    public static void writeAnnotatedSpectrumToFile(final @NotNull FTree tree, final @NotNull File target) throws IOException {
        new AnnotatedSpectrumWriter().writeFile(target, tree);
    }
}
