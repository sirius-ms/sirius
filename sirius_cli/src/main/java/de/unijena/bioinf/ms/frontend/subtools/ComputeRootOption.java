/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ChemistryBase.utils.IterableWithSize;
import de.unijena.bioinf.projectspace.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CommandLine.Command(name = "background-computation", aliases = {"bc"}, versionProvider = Provide.Versions.class, sortOptions = false)
public class ComputeRootOption<P extends ProjectSpaceManager<I>, I extends Instance> implements RootOptions<I, P, PreprocessingJob<Iterable<I>>, PostprocessingJob<?>> {

    protected final P projectSpace;
    protected Iterable<I> instances;

    protected InputFilesOptions inputFiles = null;

    public ComputeRootOption(@NotNull P projectSpace) {
        this(projectSpace, (Iterable<I>) null);

    }
    public ComputeRootOption(@NotNull P projectSpace, @Nullable Iterable<I> instances) {
        this.projectSpace = projectSpace;
        this.instances = instances;
    }

    public ComputeRootOption(@NotNull P projectSpace, @NotNull List<CompoundContainerId> containerIds) {
        this.projectSpace = projectSpace;
        instances = new IterableWithSize<>() {
            @Override
            public int size() {
                return containerIds.size();
            }

            @NotNull
            @Override
            public Iterator<I> iterator() {
                return makeInstanceIterator(containerIds.iterator());
            }
        };
    }

    /**
     * here we need to provide the PP to write on.
     */
    @Override
    public P getProjectSpace() {
        return projectSpace;
    }

    @Override
    public InputFilesOptions getInput() {
        return inputFiles;
    }

    public void setNonCompoundInput(Path... genericFiles) {
        setNonCompoundInput(List.of(genericFiles));
    }

    public void setNonCompoundInput(List<Path> genericFiles) {
        inputFiles = new InputFilesOptions();
        Map<Path, Integer> map = genericFiles.stream().sequential().collect(Collectors.toMap(k -> k, k -> (int) k.toFile().length()));
        inputFiles.msInput = new InputFilesOptions.MsInput(null, null, map);
    }

    @Override
    public OutputOptions getOutput() {
        throw new UnsupportedOperationException("Output parameters are not supported by this cli entry point.");
    }

    /**
     * here we provide an iterator about the Instances we want to compute with this configured workflow
     *
     * @return PreprocessingJob that provides the iterable of the Instances to be computed
     */
    @Override
    public @NotNull PreprocessingJob<Iterable<I>> makeDefaultPreprocessingJob() {
        return new PreprocessingJob<>() {
            @Override
            protected Iterable<I> compute() {
                return instances;
            }
        };
    }

    private Iterator<I> makeInstanceIterator(@NotNull final Iterator<CompoundContainerId> compoundIDs) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return compoundIDs.hasNext();
            }

            @Override
            public I next() {
                final CompoundContainerId c = compoundIDs.next();
                if (c == null) return null;
                return projectSpace.getInstanceFromCompound(c);
            }
        };
    }

    @Nullable
    @Override
    public PostprocessingJob<?> makeDefaultPostprocessingJob() {

        return new PostprocessingJob<Boolean>() {
            @Override
            protected Boolean compute() throws Exception {
                return true;
            }
        };
    }

    @Override
    public ProjectSpaceManagerFactory<I, P> getSpaceManagerFactory() {
        return null;
    }
}
