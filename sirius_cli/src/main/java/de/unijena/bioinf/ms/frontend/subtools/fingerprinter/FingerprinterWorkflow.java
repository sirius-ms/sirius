/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.fingerprinter;

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.webapi.WebAPI;

import java.nio.file.Path;
import java.util.List;

public class FingerprinterWorkflow implements Workflow {

    private final Path outputFile;
    private final RootOptions rootOptions;

    public FingerprinterWorkflow(RootOptions<?, ?, ?, ?> rootOptions, Path outputFile) {
        this.outputFile = outputFile;
        this.rootOptions = rootOptions;

    }

    @Override
    public void run() {
        List<Path> in = rootOptions.getInput().getAllFiles();
        if (in.isEmpty())
            throw new IllegalArgumentException("No input file given!");
        Path inputFile = in.iterator().next();

        // get WEB API
        WebAPI<?> api = ApplicationCore.WEB_API;
        System.out.println("Happily Computing fingerprints from: " + inputFile.toString() + " to " + outputFile.toString());
    }
}
