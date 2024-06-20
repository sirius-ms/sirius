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

package de.unijena.bioinf.ms.middleware.model.compute.tools;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import picocli.CommandLine;

import java.util.Map;
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Tool<C> {
    //todo we can delegate command infos as separate files to provide all infos to clients e.g. SIRIUS GUI
    @JsonIgnore
    private final CommandLine.Command command;

    /**
     * tags whether the tool is enabled
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * Call in empty constructor of superclass that does not call anything else to allow for Jackson compatibility.
     * Everything else should be done with builders.
     *
     * @param annotatedObject the corresponding commandline annotation.
     */
    protected Tool(Class<C> annotatedObject) {
        command = annotatedObject.getAnnotation(CommandLine.Command.class);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @JsonIgnore
    public CommandLine.Command getCommand() {
        return command;
    }

    @JsonIgnore
    public abstract Map<String, String> asConfigMap();
}
