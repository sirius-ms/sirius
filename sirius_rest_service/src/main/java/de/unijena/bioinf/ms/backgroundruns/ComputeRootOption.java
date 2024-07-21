/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ms.backgroundruns;

import de.unijena.bioinf.ms.frontend.subtools.*;
import de.unijena.bioinf.projectspace.Instance;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

@CommandLine.Command(name = "background-computation", aliases = {"bc"}, versionProvider = Provide.Versions.class, sortOptions = false)
public class ComputeRootOption implements RootOptions<PreprocessingJob<Iterable<Instance>>> {

    protected Iterable<Instance> instances;

    public ComputeRootOption(@NotNull Iterable<Instance> instances) {
        this.instances = instances;
    }


    @Override
    public InputFilesOptions getInput() {
        return null;
    }

    @Override
    public OutputOptions getOutput() {
        return null;
    }

    /**
     * here we provide an iterator about the Instances we want to compute with this configured workflow
     *
     * @return PreprocessingJob that provides the iterable of the Instances to be computed
     */
    @Override
    public @NotNull PreprocessingJob<Iterable<Instance>> makeDefaultPreprocessingJob() {
        return new PreprocessingJob<>() {
            @Override
            protected Iterable<Instance> compute() {
                return instances;
            }
        };
    }
}
