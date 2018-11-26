/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.math.NormalDistribution;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;

import java.util.ArrayList;

/**
 * Remove satellite peaks around a large peak
 */
public class DynamicBaselineFilter implements Preprocessor {

    private double standardDeviation = 0.05d;
    private double threshold = 0.25d;


    @Override
    public MutableMs2Experiment process(MutableMs2Experiment experiment) {
        final ArrayList<MutableMs2Spectrum> newList = new ArrayList<MutableMs2Spectrum>();
        final NormalDistribution dist = new NormalDistribution(0d, standardDeviation*standardDeviation);
        final double max = dist.getDensity(0d);
        for (MutableMs2Spectrum spec : experiment.getMs2Spectra()) {
            final boolean[] remove = new boolean[spec.size()];
            int counter = 0;
            counter = cleanSpectrum(dist, max, spec, remove, counter);
            final MutableMs2Spectrum newSpec = new MutableMs2Spectrum();
            newSpec.setCollisionEnergy(spec.getCollisionEnergy());
            newSpec.setIonization(spec.getIonization());
            newSpec.setMsLevel(spec.getMsLevel());
            newSpec.setPrecursorMz(spec.getPrecursorMz());
            newSpec.setTotalIonCount(spec.getTotalIonCount());
            for (int i=0; i < remove.length; ++i) {
                if (!remove[i]) newSpec.addPeak(spec.getMzAt(i), spec.getIntensityAt(i));
            }
            if (counter > 0) {
                newList.add(newSpec);
            } else newList.add(spec);
        }
        experiment.setMs2Spectra(newList);
        return experiment;
    }

    private int cleanSpectrum(NormalDistribution dist, double max, MutableMs2Spectrum spec, boolean[] remove, int counter) {
        max/=threshold;
        for (int k=0; k < spec.size(); ++k) {
            final double intensity = spec.getIntensityAt(k);
            final double mz = spec.getMzAt(k);
            final double massLimit = 10*standardDeviation;
            final double a = mz-massLimit;
            final double b = mz+massLimit;
            for (int i = k-1; i >= 0 && spec.getMzAt(i) >= a; --i) {
                if (remove[i]) continue;
                final double limit = (dist.getDensity((mz-spec.getMzAt(i)))/max)*intensity;
                if (spec.getIntensityAt(i) < limit) {
                    remove[i] = true;
                    //System.err.println(String.format(Locale.ENGLISH, "remove %.4f (%.4f %%) due to %.4f (%.4f %%)", spec.getMzAt(i), spec.getIntensityAt(i), spec.getMzAt(k), spec.getIntensityAt(k)));
                    ++counter;
                }
            }
            for (int i = k+1; i < spec.size() && spec.getMzAt(i) <= b; ++i) {
                if (remove[i]) continue;
                final double limit = (dist.getDensity((mz-spec.getMzAt(i)))/max)*intensity;
                if (spec.getIntensityAt(i) < limit) {
                    remove[i] = true;
                    ++counter;
                    //System.err.println(String.format(Locale.ENGLISH, "remove %.4f (%.4f %%) due to %.4f (%.4f %%)", spec.getMzAt(i), spec.getIntensityAt(i), spec.getMzAt(k), spec.getIntensityAt(k)));
                }
            }
        }
        //System.out.println(counter + " (" + ((double)counter/spec.size()) + " %)");
        return counter;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.standardDeviation = document.getDoubleFromDictionary(dictionary, "standardDeviation");
        this.threshold = document.getDoubleFromDictionary(dictionary, "threshold");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "standardDeviation", standardDeviation);
        document.addToDictionary(dictionary, "threshold", threshold);
    }
}
