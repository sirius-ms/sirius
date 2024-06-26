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

package de.unijena.bioinf.ms.frontend.subtools.export.mgf;

import de.unijena.bioinf.ms.frontend.DefaultParameter;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Options for the mgf exporter sub-tool.
 */
@CommandLine.Command(name = "mgf-export", aliases = {"MGF"}, description = "<STANDALONE> Exports the spectra of a given input as mgf. %n %n", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, sortOptions = false)
public class MgfExporterOptions implements StandaloneTool<MgfExporterWorkflow> {
    protected Path output = null;
    protected double ppmDev = Double.NaN;

    @CommandLine.Option(names = "--write-ms1", description = "Write MS1 spectra into file.")
    public boolean writeMs1;

    @CommandLine.Option(names = "--ignore-ms1-only", description = "Ignore features without MS/MS spectrum.")
    public boolean ignoreMs1Only;


    @CommandLine.Option(names = "--merge-ms2", description = "Merge all MS2 of a compound into one single spectrum.")
    public boolean mergeMs2;

    @CommandLine.Option(names = "--feature-id", description = "If available use the feature ids from the input data instead of the SIRIUS internal id. internal id will be used as fallback if the given feature ids are not available or contain duplicates.", defaultValue = "true")
    public boolean featureId;

    @CommandLine.Option(names = "--quant-table", description = "Quantification table file name for Feature Based Molecular Networking.")
    public File quantTable;

    @CommandLine.Option(names = "--merge-ppm", description = "Maximum allowed deviation (in ppm) for peaks of MS2 spectra to be merged.", defaultValue = "10")
    public void setMergePpm(DefaultParameter value) throws Exception {
        this.ppmDev = value.asDouble();
    }

    @CommandLine.Option(names = {"--output", "-o"}, description = "Specify the mgf file destination.")
    public void setOutput(String outputPath) {
        output = Paths.get(outputPath);
    }


    @Override
    public MgfExporterWorkflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return new MgfExporterWorkflow(rootOptions.makeDefaultPreprocessingJob(), this);
    }
}
