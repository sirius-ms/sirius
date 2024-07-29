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

package de.unijena.bioinf.ms.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class JobWithPredictor<O> extends Job<O> {
    protected Long predictors;

    protected JobWithPredictor(String workerPrefix, JobState state, JobTable table) {
        super(workerPrefix, state, table);
    }

    protected JobWithPredictor(String workerPrefix, Long jobId, JobState state, JobTable table) {
        super(workerPrefix, jobId, state, table);
    }


    @JsonIgnore
    public boolean containsPredictor(PredictorType predictor) {
        //proof if coresponding bit is set
        return predictor.isBitSet(predictors);
    }
}
