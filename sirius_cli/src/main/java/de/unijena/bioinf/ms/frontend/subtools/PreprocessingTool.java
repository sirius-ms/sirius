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


import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

public interface PreprocessingTool<T extends PreprocessingJob<?>> {

    T makePreprocessingJob(@NotNull RootOptions<?> rootOptions, @NotNull ProjectSpaceManagerFactory<?> projectFactory, @Nullable ParameterConfig config);

    /**
     * Checks whether this tool can be run with the given options and input before
     * {@link #makePreprocessingJob(RootOptions, ProjectSpaceManagerFactory, ParameterConfig)} is called. The input is
     * given via the root options and hence not part of the parse result of this tool.
     * <p>
     * The parse result is passed in because it cannot be determined from an injected
     * {@link CommandLine.Model.CommandSpec}, see {@link ToolChainOptions#validate(CommandLine.ParseResult)}.
     *
     * @param parseResult parse result of this tool
     * @param rootOptions root options the tool will be run with
     * @throws CommandLine.ParameterException if this tool cannot be run with the given options and input
     */
    default void validate(@NotNull CommandLine.ParseResult parseResult, @NotNull RootOptions<?> rootOptions) throws CommandLine.ParameterException {
    }

}
