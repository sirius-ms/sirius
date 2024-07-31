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
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;

/**
 * NOT TREAD SAFE
 */
public interface ProjectWriter extends ProjectIO {

    public void textFile(String relativePath, IOFunctions.IOConsumer<BufferedWriter> func)  throws IOException;
    public void binaryFile(String relativePath, IOFunctions.IOConsumer<OutputStream> func)  throws IOException;

    public <A extends ProjectSpaceProperty> Optional<A> getProjectSpaceProperty(Class<A> klass);

    public void keyValues(String relativePath, Map<?,?> map) throws IOException;

    public void table(String relativePath,@Nullable String[] header, Iterable<String[]> rows) throws IOException;

    public default void intVector(String relativePath, int[] vector) throws IOException {
        textFile(relativePath, w->FileUtils.writeIntVector(w,vector));
    }

    public default void doubleVector(String relativePath, double[] vector) throws IOException {
        textFile(relativePath, w -> FileUtils.writeDoubleVector(w, vector));
    }

    public default void intMatrix(String relativePath, int[][] matrix) throws IOException {
        textFile(relativePath, w -> FileUtils.writeIntMatrix(w, matrix));
    }

    public default void doubleMatrix(String relativePath, double[][] matrix) throws IOException {
        textFile(relativePath, w -> FileUtils.writeDoubleMatrix(w, matrix));
    }

    public void delete(String relativePath) throws IOException;

    public void deleteIfExists(String relativePath) throws IOException;

    public void move(String directoryName, String newDirName) throws IOException;

    public static interface ForContainer<S extends ProjectSpaceContainerId, T extends ProjectSpaceContainer<S>> {
        public void writeAllComponents(ProjectWriter writer, T container, IOFunctions.ClassValueProducer producer) throws IOException;
    }

    public static interface DeleteContainer<S extends ProjectSpaceContainerId> {
        public void deleteAllComponents(ProjectWriter writer, S containerId) throws IOException;
    }
}
