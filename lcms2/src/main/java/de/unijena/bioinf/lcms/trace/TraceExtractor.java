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

package de.unijena.bioinf.lcms.trace;

import de.unijena.bioinf.lcms.merge.MergedTrace;
import de.unijena.bioinf.lcms.statistics.SampleStats;
import de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

import java.util.Iterator;

public class TraceExtractor implements TraceExtractionStrategy {
    @Override
    public Iterator<AbstractTrace> extractTrace(ProcessedSample mergedSample, ProcessedSample[] samplesInTrace, MergedTrace alignedFeature)  {
        // segments for merged trace
        Trace mergedTrace = alignedFeature.toTrace(mergedSample);
        SampleStats stats = mergedSample.getStorage().getStatistics();

        return new Iterator<>() {
            private int traceIndex = -1;

            @Override
            public boolean hasNext() {
                return traceIndex < samplesInTrace.length;
            }

            @Override
            public AbstractTrace next() {
                if (traceIndex > -1) {
                    final ProcessedSample S = samplesInTrace[traceIndex];
                    final ContiguousTrace t = mergedSample.getStorage().getMergeStorage().getTrace(alignedFeature.getTraceIds().getInt(traceIndex));
                    DoubleList rts = new DoubleArrayList();
                    for (int s = mergedTrace.startId(); s <= mergedTrace.endId(); ++s) {
                        rts.add(S.getScanPointInterpolator().interpolateIntensity(t, s));
                    }
                    traceIndex++;
                    // TODO scan numbers
                    // TODO run id
                    return SourceTrace.builder().rts(rts).build();
                } else if (traceIndex == -1) {
                    DoubleList rts = new DoubleArrayList();
                    DoubleList mzs = new DoubleArrayList();
                    DoubleList ints = new DoubleArrayList();
                    DoubleList noise = new DoubleArrayList();
                    for (int k = mergedTrace.startId(); k <= mergedTrace.endId(); k++) {
                        rts.add(mergedTrace.retentionTime(k));
                        mzs.add(mergedTrace.mz(k));
                        ints.add(mergedTrace.intensity(k));
                        noise.add(stats.noiseLevel(k));
                    }

                    traceIndex++;
                    // TODO run id
                    return de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace.builder().rts(rts).mzs(mzs).intensities(ints).noise(noise).build();
                }
                return null;
            }
        };
    }

}
