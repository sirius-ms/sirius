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
package de.unijena.bioinf.spectralign;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Warning;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NoiseThresholdFilter;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.NormalizeToSumPreprocessor;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.HighIntensityMerger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.Merger;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.merging.PeakMerger;
import de.unijena.bioinf.MassDecomposer.Chemistry.DecomposerCache;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.MassDecomposer.Interval;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.MS2Peak;
import de.unijena.bioinf.sirius.ProcessedPeak;

import java.util.*;

public class SpectralAligner {

    // 1. apply cutoff
    // 2. merge peaks
    // 3. remove peaks without decomposition to parent
    // 4.pairwise alignment

    private DecomposerCache decomposers;

    private double threshold;
    private int limit;

    public SpectralAligner() {
        decomposers = new DecomposerCache();
        limit = 0;
        threshold = 0.01;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public double align(final Ms2Experiment a, final Ms2Experiment b) {
        final MissingValueValidator v = new MissingValueValidator();
        MS1MassDeviation md = PropertyManager.DEFAULTS.createInstanceWithDefaults(MS1MassDeviation.class).withAllowedMassDeviation(new Deviation(20, 1e-3));

        MutableMs2Experiment xa = new MutableMs2Experiment(a);
        xa.setAnnotation(MS1MassDeviation.class, md);

        MutableMs2Experiment xb = new MutableMs2Experiment(b);
        xb.setAnnotation(MS1MassDeviation.class, md);

        final Spectrum<Peak> as = preprocess(v.validate(xa, new Warning.Noop(), true));
        final Spectrum<Peak> bs = preprocess(v.validate(xb, new Warning.Noop(), true));
        return align(as, bs);
    }

    public Spectrum<Peak> preprocessExperiment(final MutableMs2Experiment a) {
        final MissingValueValidator v = new MissingValueValidator();
        MS1MassDeviation md = PropertyManager.DEFAULTS.createInstanceWithDefaults(MS1MassDeviation.class).withAllowedMassDeviation(new Deviation(20, 1e-3));
        MutableMs2Experiment xa = new MutableMs2Experiment(a);
        xa.setAnnotation(MS1MassDeviation.class, md);
        final Spectrum<Peak> as = preprocess(v.validate(xa, new Warning.Noop(), true));
        return as;
    }

    public double align(Spectrum<Peak> xs, Spectrum<Peak> ys) {
        final Deviation dev = new Deviation(20, 1e-3);
        int k = 0;
        double score = 0d;
        for (int i = 0; i < xs.size(); ++i) {
            for (int j = k; j < ys.size(); ++j) {
                final double delta = xs.getMzAt(i) - ys.getMzAt(j);
                if (delta > 1) {
                    k = j + 1;
                } else if (delta < -1) {
                    break;
                } else {
                    final double sigma = dev.absoluteFor((xs.getMzAt(i) + ys.getMzAt(j)) / 2d);
                    if (Math.abs(delta) < sigma) {
                        // align!
                        ++score;
                        k = j + 1;
                        break;
                    }
                }
            }
        }
        return score / Math.min(xs.size(), ys.size());
    }

    private Spectrum<Peak> preprocess(MutableMs2Experiment a) {
        // delete peaks with intensities below threshold
        final NoiseThresholdFilter filter = new NoiseThresholdFilter(threshold);
        a = filter.process(a);

        // normalize spectrum
        final NormalizeToSumPreprocessor n2s = new NormalizeToSumPreprocessor();
        a = n2s.process(a);
        final List<ProcessedPeak> normalized = normalize(a);

        // merge spectrum
        ArrayList<ProcessedPeak> merged = mergePeaks(a, normalized);

        // delete again peaks with intensity < 1%
        Iterator<ProcessedPeak> iter = merged.iterator();
        while (iter.hasNext()) if (iter.next().getRelativeIntensity() < threshold) iter.remove();

        // use only at most n peaks
        if (limit > 0) {
            merged = new ArrayList<ProcessedPeak>(merged.subList(0, limit));
        }

        final ChemicalAlphabet alphabet = new ChemicalAlphabet(a.getMolecularFormula().elementArray());
        final MassToFormulaDecomposer decomposer = decomposers.getDecomposer(alphabet);
        final Map<Element, Interval> boundary = alphabet.toMap();
        a.getMolecularFormula().visit((element, amount) -> boundary.put(element, new Interval(0, amount)));
        // delete peaks with no decomposition
        /*
        final ListIterator<ProcessedPeak> iter = merged.listIterator();
        while (iter.hasNext()) {
            final ProcessedPeak peak = iter.next();
            if (peak.getUnmodifiedMass()+0.5d > a.getMolecularFormula().getMass() || decomposer.decomposeToFormulas(peak.getUnmodifiedMass(), new Deviation(20, 1e-3), boundary).isEmpty()) {
                iter.remove();
            };
        }
        */

        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
        for (ProcessedPeak p : merged) {
            spec.addPeak(new Peak(p.getMass(), p.getRelativeIntensity()));
        }

        // add neutral losses
        final double ionMass = a.getPrecursorIonType().neutralMassToPrecursorMass(a.getMoleculeNeutralMass());
        for (ProcessedPeak peak : merged) {
            final double subtract = ionMass - peak.getMz();
            if (subtract > 1d) {
                spec.addPeak(new Peak(subtract, 0d));
            }
        }

        Spectrums.sortSpectrumByMass(spec);
        return spec;
    }



    /*

     */

    ArrayList<ProcessedPeak> mergePeaks(Ms2Experiment experiment, List<ProcessedPeak> peaklist) {
        final ArrayList<ProcessedPeak> mergedPeaks = new ArrayList<ProcessedPeak>(peaklist.size());
        final PeakMerger merger = new HighIntensityMerger(2e-3);
        merger.mergePeaks(peaklist, experiment, new Deviation(20, 1e-3), new Merger() {
            @Override
            public ProcessedPeak merge(List<ProcessedPeak> peaks, int index, double newMz) {
                final ProcessedPeak newPeak = peaks.get(index);
                // sum up global intensities, take maximum of local intensities
                double local = 0d, global = 0d, relative = 0d;
                for (ProcessedPeak p : peaks) {
                    local = Math.max(local, p.getLocalRelativeIntensity());
                    global += p.getGlobalRelativeIntensity();
                    relative += p.getRelativeIntensity();
                }
                newPeak.setMass(newMz);
                newPeak.setLocalRelativeIntensity(local);
                newPeak.setGlobalRelativeIntensity(global);
                newPeak.setRelativeIntensity(relative);
                final MS2Peak[] originalPeaks = new MS2Peak[peaks.size()];
                for (int i = 0; i < peaks.size(); ++i) originalPeaks[i] = peaks.get(i).getOriginalPeaks().get(0);
                newPeak.setOriginalPeaks(Arrays.asList(originalPeaks));
                mergedPeaks.add(newPeak);
                return newPeak;
            }
        });
        return mergedPeaks;
    }

    ArrayList<ProcessedPeak> normalize(Ms2Experiment experiment) {
        final double parentMass = experiment.getIonMass();
        final ArrayList<ProcessedPeak> peaklist = new ArrayList<ProcessedPeak>(100);
        final Deviation mergeWindow = new Deviation(20, 1e-3);
        final PrecursorIonType ion = experiment.getPrecursorIonType();
        double globalMaxIntensity = 0d;
        for (Ms2Spectrum s : experiment.getMs2Spectra()) {
            // merge peaks: iterate them from highest to lowest intensity and remove peaks which
            // are in the mass range of a high intensive peak
            final MutableSpectrum<Peak> sortedByIntensity = new SimpleMutableSpectrum(s);
            Spectrums.sortSpectrumByDescendingIntensity(sortedByIntensity);
            // simple spectra are always ordered by mass
            final SimpleSpectrum sortedByMass = new SimpleSpectrum(s);
            final BitSet deletedPeaks = new BitSet(s.size());
            for (int i = 0; i < s.size(); ++i) {
                // get index of peak in mass-ordered spectrum
                final double mz = sortedByIntensity.getMzAt(i);
                final int index = Spectrums.binarySearch(sortedByMass, mz);
                assert index >= 0;
                if (deletedPeaks.get(index)) continue; // peak is already deleted
                // delete all peaks within the mass range
                for (int j = index - 1; j >= 0 && mergeWindow.inErrorWindow(mz, sortedByMass.getMzAt(j)); --j)
                    deletedPeaks.set(j, true);
                for (int j = index + 1; j < s.size() && mergeWindow.inErrorWindow(mz, sortedByMass.getMzAt(j)); ++j)
                    deletedPeaks.set(j, true);
            }
            final int offset = peaklist.size();
            // add all remaining peaks to the peaklist
            for (int i = 0; i < s.size(); ++i) {
                if (!deletedPeaks.get(i)) {
                    final ProcessedPeak propeak = new ProcessedPeak(new MS2Peak(s, sortedByMass.getMzAt(i), sortedByMass.getIntensityAt(i)));
                    // propeak.setIon(ion.getIonization());
                    peaklist.add(propeak);
                    throw new UnsupportedOperationException();
                }
            }
            // now normalize spectrum. Ignore peaks near to the parent peak
            final double lowerbound = parentMass - 0.1d;
            double scale = 0d;
            for (int i = offset; i < peaklist.size() && peaklist.get(i).getMz() < lowerbound; ++i) {
                scale = Math.max(scale, peaklist.get(i).getIntensity());
            }
            // now set local relative intensities
            for (int i = offset; i < peaklist.size(); ++i) {
                final ProcessedPeak peak = peaklist.get(i);
                peak.setLocalRelativeIntensity(peak.getIntensity() / scale);
            }
            // and adjust global relative intensity
            globalMaxIntensity = Math.max(globalMaxIntensity, scale);
        }
        // now calculate global normalized intensities
        for (ProcessedPeak peak : peaklist) {
            peak.setGlobalRelativeIntensity(peak.getIntensity() / globalMaxIntensity);
            peak.setRelativeIntensity(peak.getGlobalRelativeIntensity());
        }
        // finished!
        return peaklist;
    }

}
