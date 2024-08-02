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

package de.unijena.bioinf.ms.rest.model.msnovelist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.jdbi.v3.json.Json;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Output data of a MsNovelist Job, see {@link de.unijena.bioinf.ms.rest.model.JobTable}
 */
@Getter
public class MsNovelistJobOutput {
    // can be regularly null if there was not enough data to compute the tree
    @Json
    @Nullable protected List<MsNovelistCandidate> candidates;


    public MsNovelistJobOutput(@Nullable List<MsNovelistCandidate> candidates) {
        this.candidates = candidates;
    }

    private MsNovelistJobOutput() {
        this(null);
    }

    @JsonIgnore
    @NotNull
    public Optional<List<MsNovelistCandidate>> getCandidatesOpt() {
        return Optional.ofNullable(getCandidates());
    }
}
