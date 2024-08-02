/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.webapi.rest;

import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.ms.rest.model.JobId;
import de.unijena.bioinf.ms.webapi.WebJJob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RestWebJJob<I, D, R> extends WebJJob<I, D, R, JobId> {

    public RestWebJJob(@NotNull IOFunctions.IOFunction<D, R> outputConverter) {
        this(null, null, outputConverter);
    }
    public RestWebJJob(@Nullable JobId jobId, @NotNull IOFunctions.IOFunction<D, R> outputConverter) {
        this(jobId, null, outputConverter);
    }

    public RestWebJJob(@Nullable I input, @NotNull IOFunctions.IOFunction<D, R> outputConverter) {
        this(null, input, outputConverter);
    }
    public RestWebJJob(@Nullable JobId jobId, @Nullable I input, @NotNull IOFunctions.IOFunction<D, R> outputConverter) {
        super(jobId, input, outputConverter);
    }
}