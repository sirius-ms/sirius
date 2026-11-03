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

package de.unijena.bioinf.lcms.statistics;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.datatypes.CustomDataType;
import org.h2.mvstore.WriteBuffer;

import java.nio.ByteBuffer;

public class SampleStatsDataType extends CustomDataType<SampleStats> {

    @Override
    public int getMemory(SampleStats obj) {
        int correlations = 0;
        for (int k = 0; k < TraceCorrelationStatistics.binCount(); ++k) {
            correlations += obj.getTraceCorrelations().observations(k).length * 4 + 4;
        }
        return obj.getNoiseLevelPerScan().length * 4 + 48 + correlations;
    }

    @Override
    public void write(WriteBuffer buff, SampleStats obj) {
        buff.putFloat(obj.getMs2NoiseLevel());
        writeFloat(buff, obj.getNoiseLevelPerScan());
        buff.putDouble(obj.getMs1MassDeviationWithinTraces().getPpm());
        buff.putDouble(obj.getMs1MassDeviationWithinTraces().getAbsolute());
        buff.putDouble(obj.getMinimumMs1MassDeviationBetweenTraces().getPpm());
        buff.putDouble(obj.getMinimumMs1MassDeviationBetweenTraces().getAbsolute());
        // These two used to be left out, so whether they survived depended on whether the store happened to hand
        // back a cached instance or deserialised one - the expected peak width in particular is set on the merged
        // sample after merging and read all over feature extraction, where losing it silently turns every rule that
        // depends on it off. An absent peak width is -1 rather than zero, so it has to round trip as it is.
        buff.putFloat(obj.getMinimumIntensity());
        buff.putDouble(obj.getExpectedPeakWidth().orElse(-1d));
        for (int k = 0; k < TraceCorrelationStatistics.binCount(); ++k) {
            writeFloat(buff, obj.getTraceCorrelations().observations(k));
        }
    }

    @Override
    public SampleStats read(ByteBuffer buff) {
        final SampleStats stats = SampleStats.builder().ms2NoiseLevel(buff.getFloat())
                .noiseLevelPerScan(readFloat(buff))
                .ms1MassDeviationWithinTraces(new Deviation(buff.getDouble(), buff.getDouble()))
                .minimumMs1MassDeviationBetweenTraces(new Deviation(buff.getDouble(), buff.getDouble()))
                .minimumIntensity(buff.getFloat())
                .build();
        stats.setExpectedPeakWidth(buff.getDouble());
        final float[][] correlations = new float[TraceCorrelationStatistics.binCount()][];
        for (int k = 0; k < correlations.length; ++k) correlations[k] = readFloat(buff);
        stats.setTraceCorrelations(new TraceCorrelationStatistics(correlations));
        return stats;
    }

    @Override
    public SampleStats[] createStorage(int i) {
        return new SampleStats[i];
    }
}
