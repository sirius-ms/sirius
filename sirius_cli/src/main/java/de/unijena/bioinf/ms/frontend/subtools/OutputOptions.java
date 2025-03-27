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

@Getter
public class OutputOptions {
    @CommandLine.Option(names = {"--output", "--project", "-o", "-p"}, description = "Specify the project-space to write into. If no [--input] is specified it is also used as input. For compression use the File ending .zip or .sirius.", order = 210)
    protected Path outputProjectLocation;

    @CommandLine.Option(names = "--update-fingerprint-version", description = {"Updates Fingerprint versions of the input project to the one used by this SIRIUS version.","WARNING: All Fingerprint related results (CSI:FingerID, CANOPUS) will be lost!"}, order = 230)
    private boolean updateFingerprints;

}
