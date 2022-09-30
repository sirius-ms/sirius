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

import de.unijena.bioinf.projectspace.FilenameFormatter;
import de.unijena.bioinf.projectspace.StandardMSFilenameFormatter;
import picocli.CommandLine;

import java.nio.file.Path;
import java.text.ParseException;

public class OutputOptions {
    @CommandLine.Option(names = {"--output", "--project", "-o"}, description = "Specify the project-space to write into. If no [--input] is specified it is also used as input. For compression use the File ending .zip or .sirius.", order = 210)
    protected Path outputProjectLocation;

    public Path getOutputProjectLocation() {
        return outputProjectLocation;
    }

    @CommandLine.Option(names = "--naming-convention", description = "Specify a naming scheme for the  compound directories ins the project-space. Default %%index_%%filename_%%compoundname", order = 220)
    private void setProjectSpaceFilenameFormatter(String projectSpaceFilenameFormatter) throws ParseException {
        this.projectSpaceFilenameFormatter = new StandardMSFilenameFormatter(projectSpaceFilenameFormatter);
    }

    protected FilenameFormatter projectSpaceFilenameFormatter = new StandardMSFilenameFormatter();

    public FilenameFormatter getProjectSpaceFilenameFormatter() {
        return projectSpaceFilenameFormatter;
    }

    @CommandLine.Option(names = "--no-compression", description = {"Does not use compressed project-space format (not recommended) when creating the project-space. If an existing project-space is opened this parameter has no effect."}, order = 225)
    private boolean noCompression = false;

    public boolean isNoCompression() {
        return noCompression;
    }

    @CommandLine.Option(names = "--update-fingerprint-version", description = {"Updates Fingerprint versions of the input project to the one used by this SIRIUS version.","WARNING: All Fingerprint related results (CSI:FingerID, CANOPUS) will be lost!"}, order = 230)
    private boolean updateFingerprints;

    public boolean isUpdateFingerprints() {
        return updateFingerprints;
    }
}
