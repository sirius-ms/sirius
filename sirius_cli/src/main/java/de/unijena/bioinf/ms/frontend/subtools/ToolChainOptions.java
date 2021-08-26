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

import de.unijena.bioinf.projectspace.Instance;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ToolChainOptions<O extends ToolChainJob<?>, F extends ToolChainJob.Factory<O>> extends Callable<F> {

    //todo this should be moved to the corresponding subtool job so that it is independent from the CLI options
    Consumer<Instance> getInvalidator();

    /**
     * Merged list of all subtools that should be reachable from this options. Usually no need to Override.
     *
     * @return List subtool option classes
     */
    default List<Class<? extends ToolChainOptions<?, ?>>> getSubCommands() {
        return Stream.concat(getDependentSubCommands().stream(), getFollowupSubCommands().stream()).distinct().collect(Collectors.toList());
    }

    /**
     * List of Tools that are listed as subtools of this tool but do NOT have direct dependencies on this tool.
     * Results of these tools will be NOT invalidated if this tool is invalidated
     *
     * @return List subtool option classes
     */
    default List<Class<? extends ToolChainOptions<?, ?>>> getFollowupSubCommands() {
        return List.of();
    }

    /**
     * List of Tools that are listed as subtools of this tool and also depend on the results of this tool.
     * Such tools will be invalidated if this tool is invalidated
     *
     * @return List subtool option classes that depend on this tool
     */
    List<Class<? extends ToolChainOptions<?, ?>>> getDependentSubCommands();
}
