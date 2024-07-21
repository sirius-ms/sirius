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

package de.unijena.bioinf.ms.middleware.model.compute;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBOptions;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@Jacksonized
public class DatabaseImportSubmission {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String databaseId;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
    private List<String> filesToImport;

    @Schema(nullable = true)
    public Integer writeBuffer;

    @JsonIgnore
    public CommandSubmission asCommandSubmission(String location){ //todo make nice
        List<String> command = new ArrayList<>();
        command.add(CustomDBOptions.class.getAnnotation(CommandLine.Command.class).name());
        command.add("--location");
        command.add(location);
        command.add("--input");
        command.add(filesToImport.stream().distinct().collect(Collectors.joining(",")));
        if (writeBuffer != null) {
            command.add("--buffer");
            command.add(String.valueOf(writeBuffer));
        }

        return CommandSubmission.builder()
                .command(command)
                .build();
    }
}
