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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Consumer;

/**
 * NOT TREAD SAFE
 */
public interface ProjectReader extends ProjectIO {
    public <A> A textFile(String relativePath, IOFunctions.IOFunction<BufferedReader, A> func)  throws IOException;
    public <A> A binaryFile(String relativePath, IOFunctions.IOFunction<InputStream, A> func)  throws IOException;

    /*
    This methods might be redundant (as they are just special textfiles) but we might later use different ways to serialize key/value or
    tables, e.g. when using databases
     */
    public default Map<String,String> keyValues(String relativePath) throws IOException {
        return textFile(relativePath, FileUtils::readKeyValues);
    }

    public default int[] intVector(String relativePath) throws IOException {
        return textFile(relativePath, FileUtils::readAsIntVector);
    }
    public default double[] doubleVector(String relativePath) throws IOException {
        return textFile(relativePath, FileUtils::readAsDoubleVector);
    }
    public default int[][] intMatrix(String relativePath) throws IOException {
        return textFile(relativePath, FileUtils::readAsIntMatrix);
    }
    public default double[][] doubleMatrix(String relativePath) throws IOException {
        return textFile(relativePath, FileUtils::readAsDoubleMatrix);
    }

    public void table(String relativePath, boolean skipHeader, int fromLine, int toLine, Consumer<String[]> f) throws IOException;


    public static interface ForContainer<S extends ProjectSpaceContainerId,T extends ProjectSpaceContainer<S>> {
        public void readAllComponents(ProjectReader reader, T container, IOFunctions.ClassValueConsumer consumer)  throws IOException;
    }
}
