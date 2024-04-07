/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.persistence.model.core.trace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.SuperBuilder;
import it.unimi.dsi.fastutil.floats.FloatList;

import java.util.Arrays;

@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class MergedTrace extends AbstractTrace {

    public MergedTrace(long runId, float[] intensities, int scanIndexOffset, double[] masses) {
        super(runId, new FloatArrayList(intensities), scanIndexOffset);
        this.averageMz = Arrays.stream(masses).average().orElse(0d);
        this.mzDeviationsFromMean = new FloatArrayList(masses.length);
        for (double mz : masses) mzDeviationsFromMean.add((float)(mz - averageMz));
    }
    public MergedTrace(long runId, float[] intensities, int scanIndexOffset, double meanMz, float[] deviationsFromMean) {
        super(runId, new FloatArrayList(intensities), scanIndexOffset);
        this.mzDeviationsFromMean = new FloatArrayList(deviationsFromMean);
        this.averageMz = meanMz;
    }

    /**
     * ID of this trace.
     */
    @Id
    long mergedTraceId;

    /**
     * deviations from average mass in float
     */
    FloatList mzDeviationsFromMean;

    /**
     * Computes the array of exact mass values
     */
    @JsonIgnore
    public double[] getMassTrace() {
        return mzDeviationsFromMean.doubleStream().map(x->x+averageMz).toArray();
    }

    double averageMz;
}
