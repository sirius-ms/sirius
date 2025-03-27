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

import lombok.Getter;
import picocli.CommandLine;

import java.nio.file.Path;

import static de.unijena.bioinf.projectspace.ProjectSpaceManager.PROJECT_FILENAME_VALIDATOR;

public class OutputOptions {
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Getter
    protected Path outputProjectLocation;

    @CommandLine.Option(names = {"--output", "--project", "-o", "-p"}, description = "Specify the project-space to be used (.sirius).", order = 210)
    public void setOutputProjectLocation(Path path) {
        String fileName = path.getFileName().toString();
        String fileWithoutExtension = fileName.indexOf(".") > 0 ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;
        if (!PROJECT_FILENAME_VALIDATOR.matcher(fileWithoutExtension).matches()) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                    String.format("Output project filename '%s' must match '%s'.", fileWithoutExtension, PROJECT_FILENAME_VALIDATOR.pattern()));
        }
        outputProjectLocation = path;
    }

    @Getter
    @CommandLine.Option(names = "--update-fingerprint-version", description = {"Updates Fingerprint versions of the input project to the one used by this SIRIUS version.","WARNING: All Fingerprint related results (CSI:FingerID, CANOPUS) will be lost!"}, order = 230)
    private boolean updateFingerprints;
}
