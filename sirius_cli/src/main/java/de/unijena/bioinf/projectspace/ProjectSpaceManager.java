/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.utils.IterableWithSize;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.rest.NetUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

public interface ProjectSpaceManager extends IterableWithSize<Instance> {
    @NotNull Instance newCompoundWithUniqueId(Ms2Experiment inputExperiment);

    Optional<Instance> findInstance(String id, Class<? extends DataAnnotation>... components);

    <T extends ProjectSpaceProperty> Optional<T> getProjectSpaceProperty(Class<T> key);

    <T extends ProjectSpaceProperty> T setProjectSpaceProperty(T value);

    <T extends ProjectSpaceProperty> T setProjectSpaceProperty(Class<T> key, T value);

    <T extends ProjectSpaceProperty> T deleteProjectSpaceProperty(Class<T> key);

    @NotNull Iterator<Instance> filteredIterator(@Nullable Predicate<CompoundContainerId> cidFilter, @Nullable Predicate<CompoundContainer> compoundFilter);

    @NotNull
    @Override
    Iterator<Instance> iterator();

    Iterator<Instance> instanceIterator(Class<? extends DataAnnotation>... c);

    int size();

    boolean containsCompound(String dirName);

    void writeSummaries(@Nullable Path summaryLocation, @Nullable Collection<CompoundContainerId> inclusionList, @NotNull Summarizer... summarizers) throws ExecutionException;

    void writeSummaries(@Nullable Path summaryLocation, boolean compressed, @Nullable Collection<CompoundContainerId> inclusionList, @NotNull Summarizer... summarizers) throws ExecutionException;

    void close() throws IOException;

    /**
     * This checks whether the data files are compatible with them on the server. Since have had versions of the PS with
     * incomplete data files it also loads missing files from the server but only if the existing ones are compatible.
     * <p>
     * Results are cached!
     *
     * @param interrupted Tell the waiting job how it can check if it was interrupted
     * @return true if data files are  NOT incompatible with the Server version (compatible or not existent)
     * @throws TimeoutException     if server request times out
     * @throws InterruptedException if waiting for server request is interrupted
     */
    boolean checkAndFixDataFiles(NetUtils.InterruptionCheck interrupted) throws TimeoutException, InterruptedException;

    String getName();

    String getLocation();

    void flush() throws IOException;
}
