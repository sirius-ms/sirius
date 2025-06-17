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

package de.unijena.bioinf.ms.frontend.subtools.custom_db;

import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import picocli.CommandLine;

/**
 * This is for parameters needed to create a custom DB.
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "custom-db", aliases = {"DB"}, description = "<STANDALONE> Operations with custom searchable structure/spectral databases. See subcommands help for more info.", versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true, sortOptions = false,
subcommands = {
        AddDBOptions.class,
        CreateDBOptions.class,
        ShowDBOptions.class,
        ImportDBOptions.class,
        RemoveDBOptions.class})
public class CustomDBOptions implements StandaloneTool<Workflow> {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec ownSpec;

    @Override
    public Workflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return () -> {
            String subtools = String.join(",", ownSpec.subcommands().keySet());
            System.out.printf("Please specify one of the subcommands for %s: [%s]%n", ownSpec.name(), subtools);
        };
    }
}
