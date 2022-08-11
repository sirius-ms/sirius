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

package de.unijena.bioinf.ms.frontend.subtools.export.trees;

import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.Instance;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;

/**
 * Options for the tree exporter sub-tool.
 * This tool is intended to export frag trees into various formats.
 * //todo add support for svg and pdf
 */
@CommandLine.Command(name = "ftree-export", description = "<STANDALONE> Exports the fragmentation trees of a project into various formats", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, sortOptions = false)
public class FTreeExporterOptions implements StandaloneTool<FTreeExporterWorkflow> {
    public FTreeExporterOptions() {
    }

    @CommandLine.Option(names = "--json", description = "Export trees as json format (as used by the project-space)")
    public boolean exportJson;

    @CommandLine.Option(names = "--dot", description = "Export trees as dot format (for easy rendering with 3rd party tools.)")
    public boolean exportDot;

    @CommandLine.Option(names = "--all", description = "Export all trees instead of only the top ranked trees.")
    public boolean exportAllTrees;

    @CommandLine.Option(names = {"--output", "-o"}, description = "Specify the output destination directory.")
    public void setOutput(File outputFile) {
        output = outputFile.toPath();
    }
    protected Path output = null;


    @Override
    public FTreeExporterWorkflow makeWorkflow(RootOptions<?, ?, ?, ?> rootOptions, ParameterConfig config) {
        return new FTreeExporterWorkflow((PreprocessingJob<? extends Iterable<Instance>>) rootOptions.makeDefaultPreprocessingJob(), this, config);
    }
}
