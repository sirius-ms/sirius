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

package de.unijena.bioinf.ms.frontend.subtools.spectra_search;

import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.spectraldb.SpectralAlignmentType;
import picocli.CommandLine;

@CommandLine.Command(name = "spectra-search", aliases = {"spectral-search"}, description = "<STANDALONE> Computes the similarity between all compounds/features in the project-space (queries) one vs all compounds/features in the second project-space (library).",  versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class SpectraSearchOptions implements StandaloneTool<SpectraSearchWorkflow> {
 // todo we want to implement a spectral library searches bases on cosine and similarity search.

    @CommandLine.Option(names = {"--db"}, required = true, description = {"Spectral database location."})
    String dbLocation;

    @CommandLine.Option(names = {"--ppm-precursor"}, defaultValue = "20", description = {"Relative precursor mass error in ppm."})
    int ppmPrecursor;

    @CommandLine.Option(names = {"--abs-precursor"}, defaultValue = "0.001", description = {"Absolute precursor mass error in Dalton."})
    double absPrecursor;

    @CommandLine.Option(names = {"--ppm-peak"}, defaultValue = "40", description = {"Relative peak mass error in ppm."})
    int ppmPeak;

    @CommandLine.Option(names = {"--abs-peak"}, defaultValue = "0.002", description = {"Absolute peak mass error in Dalton."})
    double absPeak;

    @CommandLine.Option(names = {"--print"}, defaultValue = "10", description = {"Print this many matches per query spectrum to the log."})
    int log;

    @CommandLine.Option(names = {"--alignment"}, defaultValue = "INTENSITY",
            description = {"Set the spectral alignment type.","Possible types: INTENSITY (intensity weighted spectral alignment), GAUSSIAN (Gaussian spectral alignment)"})
    SpectralAlignmentType alignmentType;

    @Override
    public SpectraSearchWorkflow makeWorkflow(RootOptions<?, ?, ?, ?> rootOptions, ParameterConfig config) {
        return new SpectraSearchWorkflow((PreprocessingJob<ProjectSpaceManager<?>>) rootOptions.makeDefaultPreprocessingJob(), this);
    }

}
