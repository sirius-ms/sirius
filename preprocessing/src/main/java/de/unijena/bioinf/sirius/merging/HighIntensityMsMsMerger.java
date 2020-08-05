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

package de.unijena.bioinf.sirius.merging;


import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.sirius.MS2Peak;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;

import java.util.*;

/**
 * - merge all MS/MS spectra such that all two peaks which are close (using allowed mass dev window) to each other
 *   are merged into one, using the mass of the higher intensive peak, but suming up the intensities
 */
public class HighIntensityMsMsMerger implements Ms2Merger {
    @Override
    public void merge(ProcessedInput processedInput) {
        List<ProcessedPeak> processedPeaks = mergePeaks(processedInput);
        Collections.sort(processedPeaks, new ProcessedPeak.MassComparator());
        for (int k=0; k < processedPeaks.size(); ++k) {
            processedPeaks.get(k).setIndex(k);
        }
        processedInput.setMergedPeaks(processedPeaks);
        processedInput.setParentPeak(processedPeaks.get(processedPeaks.size()-1));

    }

    protected List<ProcessedPeak> mergePeaks(ProcessedInput processedInput) {
        final Deviation mergeWindow = processedInput.getAnnotationOrDefault(MS2MassDeviation.class).allowedMassDeviation.multiply(2);

        // step 1: delete close peaks within a spectrum
        final List<MS2Peak> peaks = new ArrayList<>();


        for (MutableMs2Spectrum ms2 : processedInput.getExperimentInformation().getMs2Spectra()) {
            final MutableMs2Spectrum sortedByIntensity = new MutableMs2Spectrum(ms2);
            Spectrums.sortSpectrumByDescendingIntensity(sortedByIntensity);
            final SimpleMutableSpectrum sortedByMass = new SimpleMutableSpectrum(ms2);
            Spectrums.sortSpectrumByMass(sortedByMass);
            final BitSet deletedPeaks = new BitSet(sortedByIntensity.size());
            for (int i = 0; i < sortedByIntensity.size(); ++i) {
                // get index of peak in mass-ordered spectrum
                final double mz = sortedByIntensity.getMzAt(i);
                final int index = Spectrums.binarySearch(sortedByMass, mz);
                assert index >= 0;
                if (deletedPeaks.get(index)) continue; // peak is already deleted
                // delete all peaks within the mass range
                for (int j = index - 1; j >= 0 && mergeWindow.inErrorWindow(mz, sortedByMass.getMzAt(j)); --j)
                    deletedPeaks.set(j, true);
                for (int j = index + 1; j < sortedByIntensity.size() && mergeWindow.inErrorWindow(mz, sortedByMass.getMzAt(j)); ++j)
                    deletedPeaks.set(j, true);
            }
            // add all remaining peaks to the peaklist
            for (int i = 0; i < sortedByMass.size(); ++i) {
                if (!deletedPeaks.get(i)) {
                    peaks.add(new MS2Peak(ms2, sortedByMass.getMzAt(i), sortedByMass.getIntensityAt(i)));
                }
            }
        }

        final List<ProcessedPeak> mergedPeaks = new ArrayList<>();

        final MS2Peak[] mzArray = peaks.toArray(new MS2Peak[peaks.size()]);
        final Comparator<MS2Peak> massComparator = Spectrums.getPeakMassComparator();
        Arrays.sort(mzArray, massComparator);
        int n = mzArray.length;
        // first: Merge parent peak!!!!
        double ionMass = processedInput.getExperimentInformation().getIonMass();
        final int parentIndex = mergeParentPeak(mzArray, mergeWindow, ionMass,mergedPeaks);
        // after this you can merge the other peaks. Ignore all peaks near the parent peak
        int subIndex = parentIndex<0 ? mzArray.length : parentIndex;
        for (; subIndex > 0 && mzArray[subIndex - 1].getMz() + 0.1d >= ionMass; --subIndex) ;
        n = subIndex;
        final MS2Peak[] parray = Arrays.copyOf(mzArray, subIndex);
        Arrays.sort(parray, Spectrums.getPeakIntensityComparatorReversed());
        for (int i = 0; i < parray.length; ++i) {
            final MS2Peak p = parray[i];
            final int index = Arrays.binarySearch(mzArray, 0, n, p, massComparator);
            if (index < 0) continue;
            final double error = mergeWindow.absoluteFor(p.getMz());
            final double min = p.getMz() - error;
            final double max = p.getMz() + error;
            int minIndex = index;
            while (minIndex >= 0 && mzArray[minIndex].getMz() >= min) --minIndex;
            ++minIndex;
            int maxIndex = index;
            while (maxIndex < n && mzArray[maxIndex].getMz() <= max) ++maxIndex;

            final ProcessedPeak mergedPeak = new ProcessedPeak(p);
            mergePeak(mergedPeaks, mergedPeak, mzArray, minIndex, maxIndex);
            System.arraycopy(mzArray, maxIndex, mzArray, minIndex, n - maxIndex);
            n -= (maxIndex - minIndex);
        }

        if (parentIndex < 0) {
            // add artificial parent peak
            ProcessedPeak parent = new ProcessedPeak();
            parent.setMass(ionMass);
            mergedPeaks.add(parent);
        }

        return mergedPeaks;
    }


    protected int mergeParentPeak(MS2Peak[] mzArray, Deviation mergeWindow, double parentMass, List<ProcessedPeak> peakList) {
        Spectrum<MS2Peak> massOrderedSpectrum = Spectrums.wrap(Arrays.asList(mzArray));
        final int properParentPeak = Spectrums.indexOfFirstPeakWithin(massOrderedSpectrum, parentMass, mergeWindow);
        if (properParentPeak < 0) {
            // there is no parent peak in spectrum
            // therefore it is save to merge all peaks
            return -1;
        }
        double maxIntensity = massOrderedSpectrum.getIntensityAt(properParentPeak);
        int lastIndex = properParentPeak;
        for (; lastIndex < massOrderedSpectrum.size(); ++lastIndex) {
            if (!mergeWindow.inErrorWindow(parentMass, massOrderedSpectrum.getMzAt(lastIndex))) {
                break;
            } else {
                maxIntensity = Math.max(massOrderedSpectrum.getIntensityAt(lastIndex), maxIntensity);
            }
        }



        int bestIndex = -1;
        {
            // main peak is: take the two most intensive peaks with at least 10% intensity and select the peak that is
            // nearest to the parent mass
            final double threshold = maxIntensity * 0.1;
            double closestMass = Double.POSITIVE_INFINITY;
            for (int k = properParentPeak; k < lastIndex; ++k) {
                if (massOrderedSpectrum.getIntensityAt(k) >= threshold) {
                    double mzdiff = Math.abs(massOrderedSpectrum.getMzAt(k) - parentMass);
                    if (mzdiff < closestMass) {
                        closestMass = mzdiff;
                        bestIndex = k;
                    }
                }
            }

            //if no best index is found due to super tiny intensities and rounding error, just take the closest
            if (bestIndex < 0) {
                closestMass = Double.POSITIVE_INFINITY;
                for (int k = properParentPeak; k < lastIndex; ++k)
                    if (Math.abs(massOrderedSpectrum.getMzAt(k) - parentMass) < closestMass)
                        bestIndex = k;
            }
        }

        final ProcessedPeak parentPeak = new ProcessedPeak(massOrderedSpectrum.getPeakAt(bestIndex));

        mergePeak(peakList, parentPeak, mzArray, properParentPeak, lastIndex);

        return bestIndex;
    }

    public void mergePeak(List<ProcessedPeak> peakList, ProcessedPeak newPeak, MS2Peak[] peaks, int startIndex, int endIndex) {
        // sum up global intensities, take maximum of local intensities
        double global = 0d;
        CollisionEnergy energy = null;
        final MS2Peak[] originalPeaks = new MS2Peak[endIndex - startIndex];
        for (int u = startIndex; u < endIndex; ++u) {
            global += peaks[u].getIntensity();
            CollisionEnergy collisionEnergy = peaks[u].getSpectrum().getCollisionEnergy();
            energy = energy == null ? collisionEnergy : energy.merge(collisionEnergy);
            originalPeaks[u-startIndex] = peaks[u];
        }
        newPeak.setRelativeIntensity(global);
        newPeak.setOriginalPeaks(Arrays.asList(originalPeaks));
        newPeak.setCollisionEnergy(energy);
        peakList.add(newPeak);
    }
}

