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

package de.unijena.bioinf.ms.frontend.subtools.fingerprinter;

import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(name = "fingerprinter", aliases = {"FP"}, description = "<STANDALONE> Compute SIRIUS compatible fingerprints from PubChem standardized SMILES in tsv format. %n %n",  versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class FingerprinterOptions implements StandaloneTool<FingerprinterWorkflow> {

    @CommandLine.Option(names = {"--output", "-o"}, description = "Specify output tsv file.", required = true)
    private Path outputPath;

    @CommandLine.Option(names = {"--charge", "-c"}, description = "Specify charge (1 for positive ion mode, -1 for negative ion mode)", required = true)
    private int charge;

    @CommandLine.Option(names = {"--version", "-v"}, description = "Specify file to write fingerprint version information to", required = false)
    private Path version;

    @CommandLine.Option(names = {"--bufferSize", "-b"}, description = "Specify buffer size to adjust memory usage. If not given buffer size is set to 5x CPU threads.", required = false)
    private int bufferSize;

    @Override
    public FingerprinterWorkflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return new FingerprinterWorkflow(rootOptions, outputPath,charge,version, bufferSize);
    }
}
