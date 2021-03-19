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

package de.unijena.bioinf.lcms.noise;

import de.unijena.bioinf.ChemistryBase.algorithm.Quickselect;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.model.lcms.Scan;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;

public class NoiseStatistics {

    private float[] noise;
    private int offset, len, k;
    private TIntArrayList scanNumbers;
    private TFloatArrayList noiseLevels;
    private float avgNoise;
    private double percentile;

    public NoiseStatistics(int bandWidth, double percentile, int k) {
        this.noise = new float[bandWidth];
        this.offset = 0;
        this.len = 0;
        this.scanNumbers = new TIntArrayList();
        this.noiseLevels = new TFloatArrayList();
        this.avgNoise = 0;
        this.percentile = percentile;
        this.k = k;
    }

    public NoiseModel getLocalNoiseModel() {
        done();
        return new LocalNoiseModel(noiseLevels.toArray(), scanNumbers.toArray());
    }

    private void done() {
        if (len < noise.length) {
            double v = avgNoise/len;
            for (int k = 0; k < len; ++k) noiseLevels.add((float)v);
            noise = Arrays.copyOf(noise,len);
        }
    }

    public NoiseModel getGlobalNoiseModel() {
        done();
        final double noiseLevel = Quickselect.quickselectInplace(noiseLevels.toArray(), 0, noiseLevels.size (),(int)Math.floor(noiseLevels.size()*0.5));
        return new GlobalNoiseModel(noiseLevel, noiseLevel*10);
    }

    public void add(Scan scan, SimpleSpectrum spectrum) {
        scanNumbers.add(scan.getIndex());
        final float nl =calculateNoiseLevel(spectrum);
        if (len < noise.length) {
            noise[len++] = nl;
            avgNoise += nl;
            if (len==noise.length) {
                double v = avgNoise/len;
                for (int k = 0; k < noise.length; ++k) noiseLevels.add((float)v);
            }
        } else {
            avgNoise -= noise[offset];
            avgNoise += nl;
            noise[offset++] = nl;
            if (offset >= noise.length) offset = 0;
            noiseLevels.add(avgNoise/len);
        }
    }

    // TODO: for MS/MS use decomposer
    private float calculateNoiseLevel(SimpleSpectrum spectrum) {
        final double[] array = Spectrums.copyIntensities(spectrum);
        float lowestRecordedIntensity = Float.POSITIVE_INFINITY;
        int numberOnNonZeros = 0;
        for (double i : array) {
            if ((float)i>0) {
                lowestRecordedIntensity = Math.min(lowestRecordedIntensity, (float)i);
                ++numberOnNonZeros;
            }
        }
        if (numberOnNonZeros>=50) {
            int k = Math.max(array.length-this.k, (int)Math.floor(array.length*percentile));
            float fl = (float)Quickselect.quickselectInplace(array,0,array.length, k);
            if (fl<=0) return lowestRecordedIntensity/5f;
            else return fl;
        } else if (numberOnNonZeros > 0) {
            return lowestRecordedIntensity/5f;
        } else return 0;

    }
}
