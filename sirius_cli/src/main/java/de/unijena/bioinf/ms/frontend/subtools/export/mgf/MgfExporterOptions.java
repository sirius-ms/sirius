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
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.Instance;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Options for the mgf exporter sub-tool.
 */
@CommandLine.Command(name = "mgf-export", aliases = {"MGF"}, description = "<STANDALONE> Exports the spectra of a given input as mgf.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, sortOptions = false)
public class MgfExporterOptions implements StandaloneTool<MgfExporterWorkflow> {
    protected Path output = null;
    protected double ppmDev = Double.NaN;

    public MgfExporterOptions() {
    }

    @CommandLine.Option(names = "--write-ms1", description = "Write MS1 spectra into file.")
    public boolean writeMs1;

    @CommandLine.Option(names = "--merge-ms2", description = "Merge all MS2 of a compound into one single spectrum.")
    public boolean mergeMs2;

    @CommandLine.Option(names = "--quant-table", description = "Quantification table file name for Feature Based Molecular Networking.")
    public File quantTable;

    @CommandLine.Option(names = "--merge-ppm", description = "Maximum allowed deviation (in ppm) for peaks of MS2 spectra to be merged.", defaultValue = "10")
    public void setMergePpm(DefaultParameter value) throws Exception {
        this.ppmDev = value.asDouble();
    }

    //fewer parameter is probably better // todo hidden then?
//    @CommandLine.Option(names = "--merge-abs", description = "Maximum allowed absolute difference for peaks of MS2 spectra to be merged.", defaultValue = "0.005")
//    public void setMergeAbs(DefaultParameter value) throws Exception {
//        this.absDev = value.asDouble();
//    }


    @CommandLine.Option(names = {"--output", "-o"}, description = "Specify the mgf file destination.")
    public void setOutput(String outputPath) {
        output = Paths.get(outputPath);
    }


    @Override
    public MgfExporterWorkflow makeWorkflow(RootOptions<?, ?, ?, ?> rootOptions, ParameterConfig config) {
        return new MgfExporterWorkflow((PreprocessingJob<? extends Iterable<Instance>>) rootOptions.makeDefaultPreprocessingJob(), this, config);
    }
}
