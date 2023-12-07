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

package de.unijena.bioinf.lcms.detection;

import de.unijena.bioinf.ms.persistence.model.core.Trace;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
public class ChromatographicPeak {

    private final int born;

    @Setter
    private int died = Integer.MIN_VALUE;

    @Setter
    private int left;

    @Setter
    private int right;

    @ToString.Exclude
    private final Trace trace;

    public ChromatographicPeak(int startIndex, Trace trace) {
        this.born = this.left = this.right = startIndex;
        this.trace = trace;
    }

    public long getApexScanId() {
        long id = -1L;
        double maxInt = 0d;
        for (int i = left; i <= right; i++) {
            if (trace.getIntensities().getDouble(i) > maxInt) {
                id = trace.getScanIds().getLong(i);
                maxInt = trace.getIntensities().getDouble(i);
            }
        }
        return id;
    }

    @ToString.Include
    public double getApexMass() {
        double mz = 0d;
        double maxInt = 0d;
        for (int i = left; i <= right; i++) {
            if (trace.getIntensities().getDouble(i) > maxInt) {
                mz = trace.getMzs().getDouble(i);
                maxInt = trace.getIntensities().getDouble(i);
            }
        }
        return mz;
    }

    @ToString.Include
    public double getApexRT() {
        double rt = 0d;
        double maxInt = 0d;
        for (int i = left; i <= right; i++) {
            if (trace.getIntensities().getDouble(i) > maxInt) {
                rt = trace.getRts().getDouble(i);
                maxInt = trace.getIntensities().getDouble(i);
            }
        }
        return rt;
    }

    @ToString.Include
    public double getApexIntensity() {
        double maxInt = 0d;
        for (int i = left; i <= right; i++) {
            if (trace.getIntensities().getDouble(i) > maxInt) {
                maxInt = trace.getIntensities().getDouble(i);
            }
        }
        return maxInt;
    }

    public double getAverageMass() {
        double sum = 0d;
        double norm = 0d;
        for (int i = left; i <= right; i++) {
            sum += trace.getIntensities().getDouble(i) * trace.getMzs().getDouble(i);
            norm += trace.getIntensities().getDouble(i);
        }
        return sum / norm;
    }

    public double getAverageRT() {
        double sum = 0d;
        double norm = 0d;
        for (int i = left; i <= right; i++) {
            sum += trace.getIntensities().getDouble(i) * trace.getRts().getDouble(i);
            norm += trace.getIntensities().getDouble(i);
        }
        return sum / norm;
    }

    public double getStdRT(double mean) {
        double intSum = 0d;
        double var = 0d;
        for (int i = left; i <= right; i++) {
            intSum += trace.getIntensities().getDouble(i);
        }
        for (int i = left; i <= right; i++) {
            var += (trace.getIntensities().getDouble(i) / intSum) * Math.pow((trace.getRts().getDouble(i) - mean), 2);
        }
        return Math.sqrt(var);
    }



    public Trace toTrace() {
        return Trace.builder()
                .runId(trace.getRunId())
                .scanIds(trace.getScanIds().subList(left, right + 1))
                .rts(trace.getRts().subList(left, right + 1))
                .mzs(trace.getMzs().subList(left, right + 1))
                .intensities(trace.getIntensities().subList(left, right + 1))
                .build();
    }

}
