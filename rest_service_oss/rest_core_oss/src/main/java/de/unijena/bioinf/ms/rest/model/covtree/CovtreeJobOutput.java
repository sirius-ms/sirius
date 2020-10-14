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

package de.unijena.bioinf.ms.rest.model.covtree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Output data of a Covtree Job, see {@link de.unijena.bioinf.ms.rest.model.JobTable}
 */
public class CovtreeJobOutput {
    // can be regularly null if there was not enough data to compute the tree
    @Nullable private final String covtree;

    public CovtreeJobOutput(@Nullable String covtree) {
        this.covtree = covtree;
    }

    private CovtreeJobOutput() {
        this(null);
    }

    @Nullable
    public String getCovtree() {
        return covtree;
    }

    @JsonIgnore
    @NotNull
    public Optional<String> getCovtreeOpt() {
        return Optional.ofNullable(getCovtree());
    }
}
