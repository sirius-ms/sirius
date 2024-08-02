
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

package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import it.unimi.dsi.fastutil.Pair;
import org.apache.commons.lang3.Range;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Spectrums {

    public final static double DELTA = 1e-8;

    public static Spectrum<Peak> wrap(double[] mz, double[] intensities) {
        return new ArrayWrapperSpectrum(mz, intensities);
    }
    public static Spectrum<Peak> wrap(float[] mz, float[] intensities) {
        return new ArrayWrapperSpectrum.Float(mz, intensities);
    }
    public static Spectrum<Peak> wrap(double[] mz, float[] intensities) {
        return new ArrayWrapperSpectrum.DoubleFloat(mz, intensities);
    }

    public static <P extends Peak> Spectrum<P> wrap(final List<P> peaks) {
        return new Spectrum<P>() {
            @Override
            public double getMzAt(int index) {
                return peaks.get(index).getMass();
            }

            @Override
            public double getIntensityAt(int index) {
                return peaks.get(index).getIntensity();
            }

            @Override
            public P getPeakAt(int index) {
                return peaks.get(index);
            }

            @Override
            public int size() {
                return peaks.size();
            }

            @Override
            public Iterator<P> iterator() {
                return peaks.iterator();
            }
        };
    }

    public static <P extends Peak, S extends Spectrum<P>>
    SimpleSpectrum neutralMassSpectrum(final S spectrum, final Ionization ionization) {
        return map(spectrum, new Transformation<P, Peak>() {
            @Override
            public Peak transform(P input) {
                return new SimplePeak(ionization.subtractFromMass(input.getMass()), input.getIntensity());
            }
        });
    }

    public static <P extends Peak, S extends Spectrum<P>>
    SimpleSpectrum neutralMassSpectrum(final S spectrum, final PrecursorIonType ionization) {
        return map(spectrum, new Transformation<P, Peak>() {
            @Override
            public Peak transform(P input) {
                return new SimplePeak(ionization.precursorMassToNeutralMass(input.getMass()), input.getIntensity());
            }
        });
    }

    public static <P extends Peak, S extends Spectrum<P>>
    SimpleSpectrum mergeSpectra(@SuppressWarnings("unchecked") final S... spectra) {
        if (spectra.length==0) return new SimpleSpectrum(new double[0], new double[0]);
        if (spectra.length==1) return new SimpleSpectrum(spectra[0]);
        final SimpleMutableSpectrum ms = new SimpleMutableSpectrum();
        for (S s : spectra) {
            for (Peak p : s) {
                ms.addPeak(p); // TODO: improve performance by concatenating arrays
            }
        }
        return new SimpleSpectrum(ms);
    }

    public static <P extends Peak, S extends Spectrum<P>>
    SimpleSpectrum mergeSpectra(@SuppressWarnings("unchecked") final List<S> spectra) {
        if (spectra.size()==0) return new SimpleSpectrum(new double[0], new double[0]);
        if (spectra.size()==1) return new SimpleSpectrum(spectra.get(0));
        final SimpleMutableSpectrum ms = new SimpleMutableSpectrum();
        for (S s : spectra) {
            for (Peak p : s) {
                ms.addPeak(p); // TODO: improve performance by concatenating arrays
            }
        }
        return new SimpleSpectrum(ms);
    }

    /**
     * Merges the given mass spectra such that all the resulting spectra contains all
     * peaks from all mass spectra but all peaks within the given mass window are merged
     * into one peak
     * @param massWindow determines the mass window in which peaks should be merged
     * @param sumIntenstities if true, the intensity is the sum of merged peak intensities. Otherwise, it is the maximum of peak intensities.
     * @param mergeMasses if true, the mass is the weighted averaged mass over merged peaks. Otherwise it is the mass of the most intensive peak.
     * @param spectra the list of spectra to merge
     * @param <P> class of the input peak
     * @param <S> class of the input spectrum
     * @return
     */
    public static <P extends Peak, S extends Spectrum<P>>
    SimpleSpectrum mergeSpectra(Deviation massWindow, boolean sumIntenstities, boolean mergeMasses, @SuppressWarnings("unchecked") final S... spectra) {
        final SimpleSpectrum merged = mergeSpectra(spectra);
        return performPeakMerging(merged, massWindow, sumIntenstities, mergeMasses);
    }

    private static <P extends Peak, S extends Spectrum<P>>
    SimpleSpectrum performPeakMerging(SimpleSpectrum merged, Deviation massWindow, boolean sumIntenstities, boolean mergeMasses) {
        assert isMassOrderedSpectrum(merged);
        final Spectrum<Peak> intensityOrdered = getIntensityOrderedSpectrum(merged);
        final BitSet alreadyMerged = new BitSet(merged.size());
        final SimpleMutableSpectrum mergedSpectrum = new SimpleMutableSpectrum();
        for (int k=0; k < intensityOrdered.size(); ++k) {
            final double mz = intensityOrdered.getMzAt(k);
            final int index = binarySearch(merged, mz);

            if (alreadyMerged.get(index)) continue;
            // merge all surrounding peaks
            final double dev = massWindow.absoluteFor(mz);
            final double min=mz-dev, max=mz+dev;
            int a=index,b=index+1;
            while (a >= 0 && merged.getMzAt(a) >= min) --a;
            ++a;
            while (b < merged.size() && merged.getMzAt(b) <= max) ++b;

            double mzSum=0d,intensitySum=0d;
            for (int j=a; j < b; ++j) {
                if (!alreadyMerged.get(j)) {
                    alreadyMerged.set(j);
                    mzSum += merged.getMzAt(j)*merged.getIntensityAt(j);
                    intensitySum += merged.getIntensityAt(j);
                }
            }
            final double mergedIntensity, mergedMz;
            if (sumIntenstities) {
                mergedIntensity = intensitySum;
            } else mergedIntensity = intensityOrdered.getIntensityAt(k);
            if (mergeMasses) {
                mergedMz = mzSum / intensitySum;
            } else mergedMz = mz;

            mergedSpectrum.addPeak(mergedMz, mergedIntensity);
        }
        return new SimpleSpectrum(mergedSpectrum);
    }

    public static <P extends Peak, S extends Spectrum<P>> int getFirstPeakGreaterOrEqualThan(S spec, double mass) {
        if (spec instanceof OrderedSpectrum) {
            final int k = binarySearch(spec, mass);
            if (k < 0) {
                return -(k+1);
            } else {
                return k;
            }
        } else {
            double smallestDist = Double.MAX_VALUE;
            int bestIndex = spec.size();
            for (int k=0; k < spec.size(); ++k) {
                double dist = spec.getMzAt(k)-mass;
                if (dist >= 1e-12 && dist < smallestDist) {
                    smallestDist = dist;
                    bestIndex = k;
                }
            }
            return bestIndex;
        }
    }

    public static <P extends Peak, S extends Spectrum<P>, P2 extends Peak, S2 extends Spectrum<P2>>
    double cosineProduct(S left, S2 right, Deviation deviation) {
        return dotProductPeaks(left, right, deviation) / Math.sqrt(dotProductPeaks(left,left,deviation)*dotProductPeaks(right,right,deviation));
    }

    public static <P extends Peak, S extends Spectrum<P>, P2 extends Peak, S2 extends Spectrum<P2>>
    double cosineProductWithLosses(S left, S2 right, Deviation deviation, double precursorLeft, double precursorRight) {
        return (cosineProduct(left,right,deviation) + cosineProduct(getInversedSpectrum(left, precursorLeft), getInversedSpectrum(right, precursorRight), deviation))/2d;
    }

    public static <P extends Peak, S extends Spectrum<P>> SimpleSpectrum getInversedSpectrum(S spec, double precursor) {
        if (spec instanceof OrderedSpectrum) {
            final SimpleMutableSpectrum mut = new SimpleMutableSpectrum(spec.size());
            int index = getFirstPeakGreaterOrEqualThan(spec, precursor) - 1;
            if (index < 0) {
                return new SimpleSpectrum(mut);
            }
            for (int k = index; k >= 0; --k) mut.addPeak(precursor - spec.getMzAt(k), spec.getIntensityAt(k));
            return new SimpleSpectrum(getAlreadyOrderedSpectrum(mut));
        } else {
            final SimpleMutableSpectrum mut = new SimpleMutableSpectrum(spec.size());
            for (int k=0; k < spec.size(); ++k) {
                final double loss = precursor-spec.getMzAt(k);
                if (loss > 0) mut.addPeak(loss, spec.getIntensityAt(k));
            }
            return new SimpleSpectrum(mut);
        }
    }

    public static <P extends Peak, S extends Spectrum<P>, P2 extends Peak, S2 extends Spectrum<P2>>
    double dotProductPeaks(S left, S2 right, Deviation deviation) {
        int i=0, j=0;
        final int nl=left.size(), nr=right.size();
        double score=0d;
        while (i < nl && left.getMzAt(i) < 0.5d) ++i;
        while (j < nr && right.getMzAt(j) < 0.5d) ++j;
        while (i < nl && j < nr) {
            final double difference = left.getMzAt(i)- right.getMzAt(j);
            final double allowedDifference = deviation.absoluteFor(Math.min(left.getMzAt(i), right.getMzAt(j)));
            if (Math.abs(difference) <= allowedDifference) {
                score += left.getIntensityAt(i)*right.getIntensityAt(j);
                for (int k=i+1; k < nl; ++k) {
                    final double difference2 = left.getMzAt(k)- right.getMzAt(j);
                    if (Math.abs(difference2) <= allowedDifference) {
                        score += left.getIntensityAt(k)*right.getIntensityAt(j);
                    } else break;
                }
                for (int l=j+1; l < nr; ++l) {
                    final double difference2 = left.getMzAt(i)- right.getMzAt(l);
                    if (Math.abs(difference2) <= allowedDifference) {
                        score += left.getIntensityAt(i)*right.getIntensityAt(l);
                    } else break;
                }
                ++i; ++j;
            } else if (difference > 0) {
                ++j;
            } else {
                ++i;
            }
        }
        return score;
    }

    public static <P extends Peak, S extends Spectrum<P>>
    SimpleSpectrum mergeSpectra(Deviation massWindow, boolean sumIntenstities, boolean mergeMasses, final List<S> spectra) {
        final SimpleSpectrum merged = mergeSpectra(spectra);
        return performPeakMerging(merged, massWindow, sumIntenstities, mergeMasses);
    }

    public static <T extends Peak> Comparator<T> getPeakIntensityComparatorReversed() {
        return new Comparator<T>(){
            @Override
            public int compare(T o1, T o2) {
                return Double.compare(o2.getIntensity(),o1.getIntensity());
            }
        };
    }
    public static <T extends Peak> Comparator<T> getPeakMassComparator() {
        return new Comparator<T>(){
            @Override
            public int compare(T o1, T o2) {
                return Double.compare(o1.getMass(),o2.getMass());
            }
        };
    }

    public static <P extends Peak, S extends Spectrum<P>>
    Spectrum<P> getIntensityOrderedSpectrum(S spectrum) {
        final PeaklistSpectrum<P> wrapper = new PeaklistSpectrum<>(spectrum);
        Collections.sort(wrapper.peaks, getPeakIntensityComparatorReversed());
        return wrapper;
    }

    public static <P extends Peak, S extends MutableSpectrum<P>, P2 extends Peak, S2 extends Spectrum<P2>>
    void addOffset(S s, double mzOffset, double intensityOffset) {
        for (int i = 0; i < s.size(); ++i) {
            s.setMzAt(i, s.getMzAt(i) + mzOffset);
            s.setIntensityAt(i, s.getIntensityAt(i) + intensityOffset);
        }
    }

    public static <P extends Peak, S extends MutableSpectrum<P>, P2 extends Peak, S2 extends Spectrum<P2>>
    void scale(S s, double mzScale, double intensityScale) {
        for (int i = 0; i < s.size(); ++i) {
            s.setMzAt(i, s.getMzAt(i) * mzScale);
            s.setIntensityAt(i, s.getIntensityAt(i) * intensityScale);
        }
    }

    public static <P extends Peak, S extends Spectrum<P>, P2 extends Peak, S2 extends Spectrum<P2>>
    boolean haveEqualPeaks(S a, S2 b) {
        if (a == b) return true;
        final int n = a.size();
        if (n != b.size()) return false;
        for (int i = 0; i < n; ++i) {
            if (Math.abs(a.getMzAt(i) - b.getMzAt(i)) > DELTA || Math.abs(a.getIntensityAt(i) - b.getIntensityAt(i)) > DELTA) {
                return false;
            }
        }
        return true;
    }

    public static <P extends Peak, S extends MutableSpectrum<P>>
    S subtractAdductsFromSpectrum(S spectrum, Ionization ionization) {
        final int n = spectrum.size();
        for (int i = 0; i < n; ++i) {
            spectrum.setMzAt(i, ionization.subtractFromMass(spectrum.getMzAt(i)));
        }
        return spectrum;
    }

    public static <P1 extends Peak, S extends Spectrum<P1>>
    SimpleSpectrum map(S spectrum, Transformation<P1, Peak> t) {
        final int n = spectrum.size();
        final double[] mzs = new double[n];
        final double[] intensities = new double[n];
        for (int i = 0; i < n; ++i) {
            final Peak p = t.transform(spectrum.getPeakAt(i));
            mzs[i] = p.getMass();
            intensities[i] = p.getIntensity();
        }
        return new SimpleSpectrum(mzs, intensities);
    }

    public static <P extends Peak, S extends MutableSpectrum<P>>
    S transform(S spectrum, Transformation<P, P> t) {
        final int n = spectrum.size();
        for (int i = 0; i < n; ++i) {
            spectrum.setPeakAt(i, t.transform(spectrum.getPeakAt(i)));
        }
        return spectrum;
    }

    public static <P extends Peak, S extends MutableSpectrum<P>>
    S filter(S spectrum, Predicate<P> predicate) {
        final TIntArrayList keep = new TIntArrayList();
        final int n = spectrum.size();
        for (int i = 0; i < n; ++i) {
            if (predicate.test(spectrum.getPeakAt(i))) keep.add(i);
        }
        for (int i=0; i < keep.size(); ++i) {
            if (i != keep.get(i)) spectrum.swap(keep.get(i), i);
        }
        for (int i=spectrum.size()-1; i >= keep.size() ; --i) {
            spectrum.removePeakAt(i);
        }
        return spectrum;
    }

    public static <P extends Peak, S extends MutableSpectrum<P>>
    S filter(S spectrum, PeakPredicate predicate) {
        final TIntArrayList keep = new TIntArrayList();
        final int n = spectrum.size();
        for (int i = 0; i < n; ++i) {
            if (predicate.apply(spectrum.getMzAt(i), spectrum.getIntensityAt(i))) keep.add(i);
        }
        for (int i = 0; i < keep.size(); ++i) {
            if (i != keep.get(i)) spectrum.swap(keep.get(i), i);
        }
        for (int i = spectrum.size() - 1; i >= keep.size(); --i) {
            spectrum.removePeakAt(i);
        }
        return spectrum;
    }

    /**
     * Merge peaks within a single spectrum
     *
     * @param msms           input spectrum
     * @param mergeWindow    mass window within peaks should be merged
     * @param sumIntensities if true, peak intensities are summed up. Otherwise, only max intensity is chosen
     * @param mergeMasses    if true, take weighted average of peak masses. Otherwise, chose mz of most intensive peak
     * @return merged spectrum
     */
    public static <P extends Peak, S extends Spectrum<P>> SimpleSpectrum mergePeaksWithinSpectrum(S msms, Deviation mergeWindow, boolean sumIntensities, boolean mergeMasses) {
        final SimpleSpectrum massOrdered = new SimpleSpectrum(msms);
        final SimpleMutableSpectrum intensityOrdered = new SimpleMutableSpectrum(msms);
        sortSpectrumByDescendingIntensity(intensityOrdered);
        final SimpleMutableSpectrum buffer = new SimpleMutableSpectrum(msms.size() / 4);
        final boolean[] chosen = new boolean[massOrdered.size()];
        for (int k = 0; k < intensityOrdered.size(); ++k) {
            final double mz = intensityOrdered.getMzAt(k);
            final int a = indexOfFirstPeakWithin(massOrdered, mz, mergeWindow);
            if (a < 0 || a > massOrdered.size())
                continue;
            final double threshold = mz + Math.abs(mergeWindow.absoluteFor(mz));
            double selectedMz = 0, selectedIntensity = 0, maxIntensity = Double.NEGATIVE_INFINITY;
            int maxPeak = 0;
            for (int b = a; b < massOrdered.size(); ++b) {
                final double m = massOrdered.getMzAt(b);
                if (m > threshold) break;
                if (chosen[b]) continue;
                final double p = massOrdered.getIntensityAt(b);
                chosen[b] = true;
                if (p > maxIntensity) {
                    maxPeak = b;
                    maxIntensity = p;
                }
                selectedMz += p * m;
                selectedIntensity += p;
            }
            if (maxIntensity<=0) continue; // we already merged this entire window
            if (mergeMasses) selectedMz /= selectedIntensity;
            else selectedMz = massOrdered.getMzAt(maxPeak);
            if (!sumIntensities) selectedIntensity = massOrdered.getIntensityAt(maxPeak);
            buffer.addPeak(selectedMz, selectedIntensity);
        }
        return new SimpleSpectrum(buffer);
    }

    public static <P extends Peak, S extends MutableSpectrum<P>> void cutByMassThreshold(S msms, double maximalMass) {
        int k = 0;
        for (int i = 0; i < msms.size(); ++i) {
            if (msms.getMzAt(i) <= maximalMass) {
                msms.swap(i, k);
                ++k;
            }
        }
        for (int i = msms.size() - 1; i >= k; --i) {
            msms.removePeakAt(i);
        }
    }

    public static <P extends Peak, S extends MutableSpectrum<P>> void applyBaseline(S msms, double intensityThreshold) {
        int k = 0;
        for (int i = 0; i < msms.size(); ++i) {
            if (msms.getIntensityAt(i) >= intensityThreshold) {
                msms.swap(i, k);
                ++k;
            }
        }
        for (int i = msms.size() - 1; i >= k; --i) {
            msms.removePeakAt(i);
        }
    }
    public static <P extends Peak, S extends Spectrum<P>>  SimpleSpectrum getBaselined(S msms, double intensityThreshold) {
        final SimpleMutableSpectrum buf = new SimpleMutableSpectrum(msms.size());
        for (int k=0; k < msms.size(); ++k) {
            if (msms.getIntensityAt(k)>intensityThreshold) {
                buf.addPeak(msms.getMzAt(k), msms.getIntensityAt(k));
            }
        }
        return new SimpleSpectrum(buf);
    }

    public static <P extends Peak, S extends MutableSpectrum<P>> void filterIsotpePeaks(S spec, Deviation deviation) {
        filterIsotpePeaks(spec, deviation, 0.2, 0.55, 3, new ChemicalAlphabet()); //a fixed 0.45 ratio would filter about 95% of CHONPS in 100-800Da
    }


    public static <P extends Peak, S extends MutableSpectrum<P>> void filterIsotpePeaks(S spec, Deviation deviation, double maxIntensityRatioAt0, double maxIntensityRatioAt1000, int maxNumberOfIsotopePeaks, ChemicalAlphabet alphabet) {
        filterIsotopePeaks(spec, deviation, maxIntensityRatioAt0, maxIntensityRatioAt1000, maxNumberOfIsotopePeaks, alphabet,false);
    }

    /**
     * remove isotope peaks from spectrum
     *
     * @param spec
     * @param deviation               allowed mass deviation
     * @param maxIntensityRatioAt0    intensity ratio at 0 Da above which peaks are treated as independent, non-isotope peaks
     * @param maxIntensityRatioAt1000 intensity ratio at 1000 Da above which peaks are treated as independent, non-isotope peaks
     * @param maxNumberOfIsotopePeaks maximum number of iosotope peaks
     * @param alphabet                {@link ChemicalAlphabet} which is used to compute the mass windows in which isotope peaks are expected.
     * @param checkForConsistentIsotopeAssignment if true, only remove isotopes of peaks when all peaks with higher intensity also have an isotope pattern
     */
    public static <P extends Peak, S extends MutableSpectrum<P>> void filterIsotopePeaks(S spec, Deviation deviation, double maxIntensityRatioAt0, double maxIntensityRatioAt1000, int maxNumberOfIsotopePeaks, ChemicalAlphabet alphabet, boolean checkForConsistentIsotopeAssignment) {
        final PeriodicTable pt = PeriodicTable.getInstance();

        final SimpleMutableSpectrum byInt = new SimpleMutableSpectrum(spec);
        Spectrums.sortSpectrumByDescendingIntensity(byInt);
        Spectrums.sortSpectrumByMass(spec);
        for (int i = 0; i < byInt.size(); ++i) {
            final Peak peak = byInt.getPeakAt(i);
            final int index = Spectrums.binarySearch(spec, peak.getMass());
            if (index >= 0) {
                TIntArrayList toDelete = new TIntArrayList(maxNumberOfIsotopePeaks);

                int offset = 1;
                Range<Double> range = pt.getIsotopicMassWindow(alphabet, deviation, peak.getMass(), offset);
                double lower = range.getMinimum();
                double upper = range.getMaximum();

                boolean isotopePeakFound = false;
                boolean atLeastOneIsotopePeakFound = false;
                int isoIndex = index + 1;
                while (isoIndex < spec.size()) {
                    final double mass = spec.getMzAt(isoIndex);
                    if (mass < lower) {
                        ++isoIndex;
                    } else if (mass <= upper) {
                        final double maxIntensityRatio = (maxIntensityRatioAt1000 - maxIntensityRatioAt0) * mass / 1000d + maxIntensityRatioAt0;
                        if (spec.getIntensityAt(isoIndex) / peak.getIntensity() <= maxIntensityRatio) {
                            //remove peak (multiple peak are allowed to be in the same window and removed)
                            toDelete.add(isoIndex);
                            isotopePeakFound = true;
                            atLeastOneIsotopePeakFound=true;
                        }
                        ++isoIndex;
                    } else {
                        if (isotopePeakFound && offset < maxNumberOfIsotopePeaks) {
                            //look for next isotope peak
                            ++offset;
                            range = pt.getIsotopicMassWindow(alphabet, deviation, peak.getMass(), offset);
                            lower = range.getMinimum();
                            upper = range.getMaximum();
                            isotopePeakFound = false;
                        } else {
                            //end
                            break;
                        }
                    }
                }

                if (checkForConsistentIsotopeAssignment && !atLeastOneIsotopePeakFound)
                    break;


                for (int j = 0; j < toDelete.size(); j++) {
                    int pos = toDelete.get(j);
                    spec.removePeakAt(pos - j);
                }

            }
        }
    }

    public static <P extends Peak, S extends Spectrum<P>> SimpleSpectrum extractIsotopePattern(S ms1Spec, Ms2Experiment exp) {
        return extractIsotopePattern(ms1Spec, exp, true);
    }

    public static <P extends Peak, S extends Spectrum<P>> SimpleSpectrum extractIsotopePattern(S ms1Spec, Ms2Experiment exp, boolean mergePeaks) {
        return extractIsotopePattern(ms1Spec, exp.getAnnotationOrDefault(MS1MassDeviation.class), exp.getIonMass(), exp.getPrecursorIonType().getCharge(), mergePeaks);
    }


    /**
     * extract hypothetical isotope pattern for a given mass
     *
     * @param ms1Spec
     * @param targetMz
     * @return
     */
    public static <P extends Peak, S extends Spectrum<P>> SimpleSpectrum extractIsotopePattern(S ms1Spec, MassDeviation deviation, double targetMz) {
        return extractIsotopePattern(ms1Spec, deviation, targetMz, 1);
    }

    public static <P extends Peak, S extends Spectrum<P>> SimpleSpectrum extractIsotopePattern(S ms1Spec, MassDeviation deviation, double targetMz, int absCharge) {
        return extractIsotopePattern(ms1Spec, deviation, targetMz, absCharge, true);
    }

    public static <P extends Peak, S extends Spectrum<P>> SimpleSpectrum extractIsotopePattern(S ms1Spec, MassDeviation deviation, double targetMz, int absCharge, boolean mergePeaks) {
        // extract all isotope peaks starting from the given target mz
        final ChemicalAlphabet stdalphabet = ChemicalAlphabet.getExtendedAlphabet();
        final Spectrum<Peak> massOrderedSpectrum = Spectrums.getMassOrderedSpectrum(ms1Spec);
        final int index = Spectrums.mostIntensivePeakWithin(massOrderedSpectrum, targetMz, deviation.allowedMassDeviation);
        if (index < 0) return null;
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
        spec.addPeak(massOrderedSpectrum.getPeakAt(index));
        // add additional peaks
        final double monoMass = spec.getMzAt(0);
        for (int k = 1; k <= 5; ++k) {
            final Range<Double> nextMz = PeriodicTable.getInstance().getIsotopicMassWindow(stdalphabet, deviation.allowedMassDeviation, monoMass, k);

            final double a = (nextMz.getMinimum() - monoMass) / absCharge + monoMass;
            final double b = (nextMz.getMaximum() - monoMass) / absCharge + monoMass;
            final double startPoint = a - deviation.massDifferenceDeviation.absoluteFor(a);
            final double endPoint = b + deviation.massDifferenceDeviation.absoluteFor(b);
            final int nextIndex = Spectrums.indexOfFirstPeakWithin(massOrderedSpectrum, startPoint, endPoint);
            if (nextIndex < 0) break;
            double mzBuffer = 0d;
            double intensityBuffer = 0d;
            for (int i = nextIndex; i < massOrderedSpectrum.size(); ++i) {
                final double mz = massOrderedSpectrum.getMzAt(i);
                if (mz > endPoint) break;
                final double intensity = massOrderedSpectrum.getIntensityAt(i);
                if (mergePeaks) {
                    mzBuffer += mz * intensity;
                    intensityBuffer += intensity;
                } else if (intensity > intensityBuffer) {
                    //don't merge. just take most intense peak within window
                    mzBuffer = mz;
                    intensityBuffer = intensity;
                }
            }
            if (mergePeaks) {
                mzBuffer /= intensityBuffer;
            }
            spec.addPeak(mzBuffer, intensityBuffer);

        }
        return new SimpleSpectrum(spec);
    }

    public static <P extends Peak, S extends Spectrum<P>> SimpleSpectrum extractIsotopePatternFromMultipleSpectra(List<S> ms1Spectra, MS1MassDeviation deviation, double targetMz, int absCharge, boolean mergePeaks, double minIsoPeakFreq) {
        List<SimpleSpectrum> isotopePatterns = ms1Spectra.stream().map(s -> extractIsotopePattern(s, deviation, targetMz, absCharge, mergePeaks)).collect(Collectors.toList());
        int maxLength = 0;
        Iterator<SimpleSpectrum> patternIterator = isotopePatterns.iterator();
        while (patternIterator.hasNext()) {
            SimpleSpectrum isotopePattern = patternIterator.next();
            if (isotopePattern==null){
                patternIterator.remove();
            } else {
                maxLength = Math.max(maxLength, isotopePattern.size());
            }
        }
        if (isotopePatterns.size()==0) return null;
        if (isotopePatterns.size()==1) return new SimpleSpectrum(isotopePatterns.get(0));

        double minNumberOfOccurrence = isotopePatterns.size()*minIsoPeakFreq;
        final SimpleMutableSpectrum mergedIsotopePattern = new SimpleMutableSpectrum();
        for (int i = 0; i < maxLength; i++) {
            int numberOfPeaks = 0;
            double mzBuffer = 0d;
            double intensityBuffer = 0d;
            for (SimpleSpectrum isotopePattern : isotopePatterns) {
                //todo rather use more robust way of merging? median? but higher intese peas might be more trustworthy
                if (isotopePattern.size()>i){
                    ++numberOfPeaks;
                    final double intensity = isotopePattern.getIntensityAt(i);
                    final double mz = isotopePattern.getMzAt(i);
                    assert (Math.abs((mz-targetMz)*absCharge-i)<0.2);//should be at very most 0.5
                    intensityBuffer += intensity;
                    mzBuffer += intensity * mz;
                }
            }
            if (numberOfPeaks<minNumberOfOccurrence) break;

            mzBuffer /= intensityBuffer;
            intensityBuffer /= numberOfPeaks;
            mergedIsotopePattern.addPeak(mzBuffer, intensityBuffer);
        }
        if (mergedIsotopePattern.size()==0) return null; //same behaviour as normal extractPatternMethod
        return new SimpleSpectrum(mergedIsotopePattern);
    }


    /**
     *
     * @param mainPattern high quality pattern, e.g. retrieved by MS1 feature finding, which might missed some isotope peaks
     * @param longerPattern pattern retrieved from normal input MS1, which might be of worse quality but contains more potential isotope peaks
     * @return
     */
    public static SimpleSpectrum extendPattern(Spectrum<Peak> mainPattern, Spectrum<Peak> longerPattern, double minIntensityCutoff) {
        if (mainPattern.size()>=longerPattern.size()) return new SimpleSpectrum(mainPattern);
        assert IntStream.range(0, mainPattern.size()).mapToDouble(i-> Math.abs(mainPattern.getMzAt(i)-longerPattern.getMzAt(i))).max().getAsDouble()<0.1;  //same targetMz, do patterns agree?

        SimpleMutableSpectrum pattern = new SimpleMutableSpectrum(mainPattern);
        SimpleMutableSpectrum longerPatternNormalized = new SimpleMutableSpectrum(longerPattern);
        double monoIntensity = pattern.getIntensityAt(0);
        Spectrums.normalizeByFirstPeak(longerPatternNormalized, monoIntensity);

        final double monoMass1 = mainPattern.getMzAt(0);
        final double monoMass2 = longerPatternNormalized.getMzAt(0);

        for (int i = mainPattern.size(); i < longerPatternNormalized.size(); i++) {
            final double mz = longerPatternNormalized.getMzAt(i);
            final double intensity = longerPatternNormalized.getIntensityAt(i);
            if (intensity/monoIntensity<minIntensityCutoff){
                break;
            }
            final double newMz = mz-monoMass2+monoMass1; //use mz differences
            pattern.addPeak(newMz, intensity);

        }

        return new SimpleSpectrum(pattern);
    }


    /**
     * try to guess the ionization by looking for mass differences to other peaks which might be the same compound ionized with a different adduct.
     * Before usage APPLY A BASELINE! THIS METHOD IGNORES INTENSITIES
     * In doubt [M]+/[M-H]- is ignored (cannot distinguish from isotope pattern)!
     *
     * @param ms1
     * @param ionMass   peak of interest
     * @param deviation
     * @param ionTypes  possible ion types
     * @return
     */
    public static PrecursorIonType[] guessIonization(Spectrum<Peak> ms1, double ionMass, Deviation deviation, PrecursorIonType[] ionTypes) {
        if (ionTypes.length==0) return ionTypes;
        SimpleMutableSpectrum spectrum = new SimpleMutableSpectrum(ms1);
        sortSpectrumByMass(spectrum);

        final PrecursorIonType lighterType, heavierType;

        if (ionTypes[0].getCharge() > 0) {
            lighterType = PrecursorIonType.getPrecursorIonType("[M]+");
            heavierType = PrecursorIonType.getPrecursorIonType("[M+H]+");
        } else {
            lighterType = PrecursorIonType.getPrecursorIonType("[M-H]-");
            heavierType = PrecursorIonType.getPrecursorIonType("[M]-");
        }

        // remove intrinsical charged from list
        int intrinsic = -1, protonated = -1, numberOfIons = ionTypes.length;
        PrecursorIonType protonation = null, intrinsicType = null;
        for (int k=0; k < ionTypes.length; ++k) {
            if (ionTypes[k].isIntrinsicalCharged()) {
                intrinsic = k;
                intrinsicType = ionTypes[k];
            } else if (ionTypes[k].isPlainProtonationOrDeprotonation()) {
                protonated = k;
                protonation = ionTypes[k];
            }
        }
        if (intrinsic >= 0 && protonated >= 0) {
            // remove intrinsic from list
            ionTypes[intrinsic] = ionTypes[ionTypes.length-1];
            --numberOfIons;
        }

        HashMap<PrecursorIonType, Set<PrecursorIonType>> adductDiffs = new HashMap<>();
        for (int i = 0; i < numberOfIons; i++) {
            final PrecursorIonType removedIT = ionTypes[i];
            for (int j = 0; j < numberOfIons; j++) {
                final PrecursorIonType addedIT = ionTypes[j];
                if (i == j) continue;
                if (removedIT.equals(lighterType) && addedIT.equals(heavierType))
                    continue; //probably just +1 isotope peak
                double diffAdductMass = addedIT.getModificationMass() - (removedIT.getModificationMass());
                int idx = Spectrums.binarySearch(spectrum, ionMass + diffAdductMass, deviation);
                if (idx < 0) continue; // no corresponding mass found;
                Set<PrecursorIonType> addedList = adductDiffs.computeIfAbsent(removedIT, k -> new HashSet<>());
                addedList.add(addedIT);
            }
        }

        if (adductDiffs.containsKey(lighterType) && adductDiffs.containsKey(heavierType)) {
            if (adductDiffs.get(heavierType).containsAll(adductDiffs.get(lighterType))) { //probably just isotopes;
                adductDiffs.remove(lighterType);
            }
        }

        Set<PrecursorIonType> set = adductDiffs.keySet();

        // if we removed intrinsic in the list, add it back again
        if (intrinsic>=0 && protonated>=0 && set.contains(protonation)) {
            set = new HashSet<>(set);
            set.add(intrinsicType);
        }

        return set.toArray(new PrecursorIonType[0]);
    }

    private final static SimpleSpectrum EMPTY_SPEC = new SimpleSpectrum(new double[0], new double[0]);
    public static SimpleSpectrum empty() {
        return EMPTY_SPEC;
    }

    public static <P extends Peak,S extends Spectrum<P>>double calculateTIC(S spec) {
        double intens = 0d;
        for (int k=0; k < spec.size(); ++k) {
            intens += spec.getIntensityAt(k);
        }
        return intens;
    }

    public static <P extends Peak,S extends Spectrum<P>>double calculateTIC(S spec, Range<Double> massRange, double intensityBaseline) {
        double intens = 0d;
        for (int k=0; k < spec.size(); ++k) {
            if (spec.getIntensityAt(k)>=intensityBaseline && massRange.contains(spec.getMzAt(k)))
            intens += spec.getIntensityAt(k);
        }
        return intens;
    }

    public static <P extends Peak,S extends Spectrum<P>> double calculateTIC(S spec, double intensityBaseLine) {
        double intens = 0d;
        for (int k=0; k < spec.size(); ++k) {
            if (spec.getIntensityAt(k) > intensityBaseLine)
                intens += spec.getIntensityAt(k);
        }
        return intens;
    }

    public static <P extends Peak,S extends Spectrum<P>>  SimpleSpectrum getCleanedSpectrumByDeletingClosePeaks(S spectrum, Deviation dev ) {
        final Spectrum<P> intensityOrderedSpectrum = getIntensityOrderedSpectrum(spectrum);
        final byte[] state = new byte[spectrum.size()];
        for (int k=0; k < intensityOrderedSpectrum.size(); ++k) {
            final double mass = intensityOrderedSpectrum.getMzAt(k);
            final double delta = dev.absoluteFor(mass);
            int i=getFirstPeakGreaterOrEqualThan(spectrum, mass-delta);
            int hi=i;
            int j=i;
            final double to = mass+delta;
            for (; j < spectrum.size(); ++j) {
                if (spectrum.getMzAt(j)>to) break;
                if (spectrum.getIntensityAt(j)>spectrum.getIntensityAt(hi)) {
                    hi=j;
                }
            }
            if (state[hi]==0) {
                for (int x=i; x < j; ++x ) {
                    state[x]=-1; // delete
                }
                state[hi] = 1;
            }
        }
        final SimpleMutableSpectrum buffer = new SimpleMutableSpectrum();
        for (int k=0; k < state.length; ++k) {
            if (state[k]==1) {
                buffer.addPeak(spectrum.getMzAt(k), spectrum.getIntensityAt(k));
            }
        }
        return new SimpleSpectrum(buffer);

    }

    public static <P extends Peak,S extends Spectrum<P>> SimpleSpectrum extractMostIntensivePeaks(S spectrum, int numberOfPeaksPerMassWindow, double slidingWindowWidth) {
        if (spectrum.isEmpty()) return Spectrums.empty();
        final Spectrum<? extends Peak> spec = getIntensityOrderedSpectrum(spectrum);
        final SimpleMutableSpectrum buffer = new SimpleMutableSpectrum();
        for (int k=0; k < spec.size(); ++k) {
            // only insert k in buffer, if there are no more than numberOfPeaksPerMassWindow peaks closeby
            final double mz = spec.getMzAt(k);
            final double wa = mz - slidingWindowWidth/2d, wb = mz + slidingWindowWidth/2d;

            int count = 0;
            for (int i=0; i < buffer.size(); ++i) {
                final double mz2 = buffer.getMzAt(i);
                if (mz2 >= wa && mz2 <= wb) {
                    if (++count >= numberOfPeaksPerMassWindow)
                        break;
                }
            }
            if (count < numberOfPeaksPerMassWindow) {
                buffer.addPeak(spec.getMzAt(k),spec.getIntensityAt(k));
            }
        }

        return new SimpleSpectrum(buffer);
    }

    public interface Transformation<P1 extends Peak, P2 extends Peak> {
        P2 transform(P1 input);
    }

    public interface PeakPredicate {
        boolean apply(double mz, double intensity);
    }

    public static SimpleSpectrum from(Collection<? extends Peak> peaks) {
        final double[] mzs = new double[peaks.size()];
        final double[] intensities = new double[peaks.size()];
        int k = 0;
        for (Peak p : peaks) {
            mzs[k] = p.getMass();
            intensities[k++] = p.getIntensity();
        }
        return new SimpleSpectrum(mzs, intensities);
    }

    public static SimpleSpectrum from(List<Number> mzsL, List<Number> intensitiesL) {
        if (mzsL.size() != intensitiesL.size())
            throw new IllegalArgumentException("size of masses and intensities differ");
        final double[] mzs = new double[mzsL.size()];
        final double[] intensities = new double[intensitiesL.size()];
        for (int i = 0; i < mzsL.size(); ++i) {
            mzs[i] = mzsL.get(i).doubleValue();
            intensities[i] = intensitiesL.get(i).doubleValue();
        }
        return new SimpleSpectrum(mzs, intensities);
    }

    public static SimpleSpectrum from(TDoubleArrayList mzsL, TDoubleArrayList intensitiesL) {
        if (mzsL.size() != intensitiesL.size())
            throw new IllegalArgumentException("size of masses and intensities differ");
        return new SimpleSpectrum(mzsL.toArray(), intensitiesL.toArray());
    }

    public static <P extends Peak, S extends Spectrum<P>> SimpleSpectrum subspectrum(S spectrum, int from, int length) {
        final double[] mz = new double[Math.min(length, spectrum.size() - from)];
        final double[] intensities = new double[mz.length];
        for (int k = from, i = 0, n = Math.min(from + length, spectrum.size()); k < n; ++k) {
            mz[i] = spectrum.getMzAt(k);
            intensities[i++] = spectrum.getIntensityAt(k);
        }
        return new SimpleSpectrum(mz, intensities, spectrum instanceof OrderedSpectrum);
    }

    public static <P extends Peak, S extends Spectrum<P>> List<P> extractPeakList(S spectrum) {
        final int n = spectrum.size();
        final ArrayList<P> peaks = new ArrayList<P>(n);
        for (int i = 0; i < n; ++i) {
            peaks.add(spectrum.getPeakAt(i));
        }
        return peaks;
    }

    public static <P extends Peak, S extends Spectrum<P>> double getMinimalIntensity(S spectrum) {
        final int n = spectrum.size();
        double min = Double.MAX_VALUE;
        for (int i = 0; i < n; ++i) {
            min = Math.min(min, spectrum.getIntensityAt(i));
        }
        return min;
    }

    public static <P extends Peak, S extends Spectrum<P>> double getMaximalIntensity(S spectrum) {
        final int n = spectrum.size();
        double max = 0d;
        for (int i = 0; i < n; ++i) {
            max = Math.max(max, spectrum.getIntensityAt(i));
        }
        return max;
    }

    public static <P extends Peak, S extends Spectrum<P>> int getIndexOfPeakWithMinimalIntensity(S spectrum) {
        final int n = spectrum.size();
        double min = Double.POSITIVE_INFINITY;
        int minIndex = 0;
        for (int i = 0; i < n; ++i) {
            if (spectrum.getIntensityAt(i) < min) {
                minIndex = i;
                min = spectrum.getIntensityAt(i);
            }
        }
        return minIndex;
    }

    public static <P extends Peak, S extends Spectrum<P>> int getIndexOfPeakWithMaximalIntensity(S spectrum) {
        final int n = spectrum.size();
        double max = Double.NEGATIVE_INFINITY;
        int maxIndex = 0;
        for (int i = 0; i < n; ++i) {
            if (spectrum.getIntensityAt(i) > max) {
                maxIndex = i;
                max = spectrum.getIntensityAt(i);
            }
        }
        return maxIndex;
    }

    public static <P extends Peak, S extends Spectrum<P>> int getIndexOfPeakWithMinimalMass(S spectrum) {
        if (spectrum instanceof OrderedSpectrum) return 0;
        final int n = spectrum.size();
        double min = Double.POSITIVE_INFINITY;
        int minIndex = 0;
        for (int i = 0; i < n; ++i) {
            if (spectrum.getMzAt(i) < min) {
                minIndex = i;
                min = spectrum.getMzAt(i);
            }
        }
        return minIndex;
    }

    public static <P extends Peak, S extends Spectrum<P>> int getIndexOfPeakWithMaximalMass(S spectrum) {
        if (spectrum instanceof OrderedSpectrum) return spectrum.size() - 1;
        final int n = spectrum.size();
        double max = Double.NEGATIVE_INFINITY;
        int maxIndex = 0;
        for (int i = 0; i < n; ++i) {
            if (spectrum.getMzAt(i) > max) {
                maxIndex = i;
                max = spectrum.getMzAt(i);
            }
        }
        return maxIndex;
    }

    public static <P extends Peak, S extends Spectrum<P>> List<Peak> copyPeakList(S spectrum) {
        final int n = spectrum.size();
        final ArrayList<Peak> peaks = new ArrayList<Peak>(n);
        for (int i = 0; i < n; ++i) {
            peaks.add(new SimplePeak(spectrum.getMzAt(i), spectrum.getIntensityAt(i)));
        }
        return peaks;
    }

    @SuppressWarnings("rawtypes")
    public static <P extends Peak, S extends Spectrum<P>> double[] copyIntensities(S spectrum, double[] buffer, int offset) {
        final int n = spectrum.size();
        if (spectrum instanceof BasicSpectrum) {
            System.arraycopy(((BasicSpectrum) spectrum).intensities, 0, buffer, offset, n);
        } else {
            for (int i = 0; i < n; ++i) {
                buffer[i + offset] = spectrum.getIntensityAt(i);
            }
        }
        return buffer;
    }

    public static <P extends Peak, S extends Spectrum<P>> double[] copyIntensities(S spectrum, double[] buffer) {
        return copyIntensities(spectrum, buffer, 0);
    }

    public static <P extends Peak, S extends Spectrum<P>> double[] copyIntensities(S spectrum) {
        return copyIntensities(spectrum, new double[spectrum.size()], 0);
    }

    @SuppressWarnings("rawtypes")
    public static <P extends Peak, S extends Spectrum<P>> double[] copyMasses(S spectrum, double[] buffer, int offset) {
        final int n = spectrum.size();
        if (spectrum instanceof BasicSpectrum) {
            System.arraycopy(((BasicSpectrum) spectrum).masses, 0, buffer, offset, n);
        } else {
            for (int i = 0; i < n; ++i) {
                buffer[i + offset] = spectrum.getMzAt(i);
            }
        }
        return buffer;
    }

    public static <P extends Peak, S extends Spectrum<P>> double[] copyMasses(S spectrum, double[] buffer) {
        return copyMasses(spectrum, buffer, 0);
    }

    public static <P extends Peak, S extends Spectrum<P>> double[] copyMasses(S spectrum) {
        return copyMasses(spectrum, new double[spectrum.size()], 0);
    }


    public static <P extends Peak, S extends MutableSpectrum<P>> double normalize(S spectrum, Normalization norm) {
        if (spectrum.size() == 0)
            return 1d;
        return switch (norm.getMode()) {
            case MAX -> normalizeToMax(spectrum, norm.getBase());
            case SUM -> normalizeToSum(spectrum, norm.getBase());
            case FIRST -> normalizeByFirstPeak(spectrum, norm.getBase());
            case L2 -> normalizeByL2Norm(spectrum, norm.getBase());
        };
    }

    private static <P extends Peak, S extends MutableSpectrum<P>> double normalizeByFirstPeak(S spectrum, double base) {
        return normalizeByPeak(spectrum, 0, base);
    }

    public static <P extends Peak, S extends MutableSpectrum<P>> double normalizeByPeak(S spectrum, int peakIdx, double base) {
        final double firstPeak = base / spectrum.getIntensityAt(peakIdx);
        for (int i = 0; i < spectrum.size(); ++i) {
            spectrum.setIntensityAt(i, spectrum.getIntensityAt(i) * firstPeak);
        }
        return firstPeak;
    }

    public static <P extends Peak, S extends Spectrum<P>> Pair<SimpleSpectrum, Double> getNormalizedSpectrumWithScale(S spectrum, Normalization norm) {
        final SimpleMutableSpectrum s = new SimpleMutableSpectrum(spectrum);
        double scale = normalize(s, norm);
        return Pair.of(new SimpleSpectrum(s), scale);
    }

    public static <P extends Peak, S extends Spectrum<P>> SimpleSpectrum getNormalizedSpectrum(S spectrum, Normalization norm) {
        final SimpleMutableSpectrum s = new SimpleMutableSpectrum(spectrum);
        normalize(s, norm);
        return new SimpleSpectrum(s);
    }

    public static <P extends Peak, S extends MutableSpectrum<P>> double normalizeToMax(S spectrum, double norm) {
        final int n = spectrum.size();
        double maxIntensity = 0d;
        for (int i = 0; i < n; ++i) {
            final double intensity = spectrum.getIntensityAt(i);
            if (maxIntensity < intensity) {
                maxIntensity = intensity;
            }
        }
        final double scale = norm / maxIntensity;
        for (int i = 0; i < n; ++i) {
            spectrum.setIntensityAt(i, spectrum.getIntensityAt(i) * scale);
        }
        return scale;
    }

    private static <P extends Peak, S extends MutableSpectrum<P>> double normalizeByL2Norm(S spectrum, double norm) {
        final int n = spectrum.size();
        double sumIntensity = 0d;
        for (int i = 0; i < n; ++i) {
            sumIntensity += spectrum.getIntensityAt(i)*spectrum.getIntensityAt(i);
        }
        final double scale = norm / Math.sqrt(sumIntensity);
        for (int i = 0; i < n; ++i) {
            spectrum.setIntensityAt(i, spectrum.getIntensityAt(i) * scale);
        }
        return scale;
    }

    public static <P extends Peak, S extends MutableSpectrum<P>> double normalizeToSum(S spectrum, double norm) {
        final int n = spectrum.size();
        double sumIntensity = 0d;
        for (int i = 0; i < n; ++i) {
            sumIntensity += spectrum.getIntensityAt(i);
        }
        final double scale = norm / sumIntensity;
        for (int i = 0; i < n; ++i) {
            spectrum.setIntensityAt(i, spectrum.getIntensityAt(i) * scale);
        }
        return scale;
    }

    public static <S extends Spectrum<P>, P extends Peak> Range<Double> getMassRange(S spectrum) {
        if (spectrum instanceof OrderedSpectrum<?>) {
            return Range.of(spectrum.getMzAt(0), spectrum.getMzAt(spectrum.size()-1));
        } else {
            double minMass=Double.POSITIVE_INFINITY, maxMass=Double.NEGATIVE_INFINITY;
            for (int k=0; k < spectrum.size(); ++k) {
                minMass = Math.min(spectrum.getMzAt(k), minMass);
                maxMass = Math.max(spectrum.getMzAt(k), maxMass);
            }
            return Range.of(minMass, maxMass);
        }
    }

    public static <S extends Spectrum<P>, P extends Peak> Range<Double> getIntensityRange(S spectrum) {
        double minIntensity=Double.POSITIVE_INFINITY, maxIntensity=Double.NEGATIVE_INFINITY;
        for (int k=0; k < spectrum.size(); ++k) {
            minIntensity = Math.min(spectrum.getIntensityAt(k), minIntensity);
            maxIntensity = Math.max(spectrum.getIntensityAt(k), maxIntensity);
        }
        return Range.of(minIntensity, maxIntensity);
    }


    /**
     * search for the given peak using {@link Object#equals(Object)}. If the spectrum implements
     * OrderedSpectrum, this search is done using binary search. Otherwise, a linear search is
     * used.
     *
     * @param spectrum
     * @param peak
     * @return negative number if peak is not contained in the spectrum, otherwise index of the peak in
     * the spectrum.
     */
    public static <S extends Spectrum<P>, P extends Peak> int indexOfPeak(S spectrum, P peak) {
        final int pos = (spectrum instanceof OrderedSpectrum) ? binarySearch(spectrum, peak.getMass())
                : linearSearch(spectrum, peak.getMass());
        if (pos < 0) return -1;
        return (peak.equals(spectrum.getPeakAt(pos))) ? pos : -1;
    }

    /**
     * search for a peak with the lowest distance to the given mz value which respects the given
     * mass deviation. If the spectrum implements
     * OrderedSpectrum, this search is done using binary search. Otherwise, a linear search is
     * used.
     *
     * @param spectrum
     * @param mz
     * @param d        allowed deviation (relative and absolute) from the mz value
     * @return negative number if peak is not contained in the spectrum, otherwise index of the peak in
     * the spectrum.
     */
    public static <S extends Spectrum<P>, P extends Peak> int search(S spectrum, double mz, Deviation d) {
        return (spectrum instanceof OrderedSpectrum) ? binarySearch(spectrum, mz, d)
                : linearSearch(spectrum, mz, d);
    }

    private static <S extends Spectrum<P>, P extends Peak> int linearSearch(S spectrum, double mz) {
        double minDiff = Double.POSITIVE_INFINITY;
        int bestPos = -1;
        for (int i = 0; i < spectrum.size(); ++i) {
            final double diff = Math.abs(spectrum.getMzAt(i) - mz);
            if (diff < minDiff) {
                minDiff = diff;
                bestPos = i;
            }
        }
        return bestPos;
    }


    private static <S extends Spectrum<P>, P extends Peak> int linearSearch(S spectrum, double mz, Deviation d) {
        final int bestPos = linearSearch(spectrum, mz);
        if (bestPos >= 0 && d.inErrorWindow(mz, spectrum.getMzAt(bestPos))) return bestPos;
        else return -1;
    }

    public static <S extends Spectrum<P>, P extends Peak> int mostIntensivePeakWithin(S spectrum, double mz, Deviation dev) {
        final double diff = dev.absoluteFor(mz);
        final double a = mz - diff, b = mz + diff;

        return (spectrum instanceof OrderedSpectrum) ? mostIntensivePeakWithinBinarySearch(spectrum, a, b)
                : mostIntensivePeakWithinLinearSearch(spectrum, a, b);
    }

    public static <S extends Spectrum<P>, P extends Peak> int mostIntensivePeakWithin(S spectrum, double begin, double end) {
        return (spectrum instanceof OrderedSpectrum) ? mostIntensivePeakWithinBinarySearch(spectrum, begin, end)
                : mostIntensivePeakWithinLinearSearch(spectrum, begin, end);
    }


    private static <S extends Spectrum<P>, P extends Peak> int mostIntensivePeakWithinLinearSearch(S spectrum, double begin, double end) {
        if (spectrum.size() <= 0) return -1;
        double intensity = Double.NEGATIVE_INFINITY;
        int opt = -1;
        for (int k = 0; k < spectrum.size(); ++k) {
            final double m = spectrum.getMzAt(k);
            if (m >= begin && m <= end && spectrum.getIntensityAt(k) > intensity) {
                intensity = spectrum.getIntensityAt(k);
                opt = k;
            }
        }
        return opt;
    }

    private static <S extends Spectrum<P>, P extends Peak> int mostIntensivePeakWithinBinarySearch(S spectrum,  double begin, double end) {
        int k = indexOfFirstPeakWithin(spectrum, begin, end);
        if (k < 0) return k;
        double intensity = spectrum.getIntensityAt(k);
        for (int j = k + 1; j < spectrum.size(); ++j) {
            if (spectrum.getMzAt(j) > end)
                break;
            if (spectrum.getIntensityAt(j) > intensity) {
                k = j;
                intensity = spectrum.getIntensityAt(j);
            }
        }
        return k;
    }


    public static <S extends Spectrum<P>, P extends Peak> int indexOfFirstPeakWithin(S spectrum, double mz, Deviation dev) {
        final double a = dev.absoluteFor(mz);
        return indexOfFirstPeakWithin(spectrum, mz - a, mz + a);
    }

    public static <S extends Spectrum<P>, P extends Peak> int indexOfFirstPeakWithin(S spectrum, double begin, double end) {
        int pos = binarySearch(spectrum, begin);
        if (pos < 0) {
            pos = (-pos) - 1;
        }
        if (pos < spectrum.size() && spectrum.getMzAt(pos) >= begin && spectrum.getMzAt(pos) <= end) {
            return pos;
        } else return -1;
    }

    public static <S extends Spectrum<P>, P extends Peak> int indexOfPeakClosestToMassWithin(S spectrum, double mz, Deviation dev) {
        return (spectrum instanceof OrderedSpectrum) ? indexOfPeakClosestToMassWithinBinarySearch(spectrum, mz, dev)
                : indexOfPeakClosestToMassWithinLinear(spectrum, mz, dev);
    }

    public static <S extends Spectrum<P>, P extends Peak> int indexOfPeakClosestToMassWithinBinarySearch(S spectrum, double mz, Deviation dev) {
        final double a = dev.absoluteFor(mz);
        int k = indexOfFirstPeakWithin(spectrum, mz - a, mz + a);
        if (k < 0) return k;
        final double end = mz + a;
        double mzDiff = Math.abs(spectrum.getMzAt(k) - mz);
        for (int j = k + 1; j < spectrum.size(); ++j) {
            if (spectrum.getMzAt(j) > end)
                break;
            final double currentMzDiff = Math.abs(spectrum.getMzAt(j) - mz);
            if (currentMzDiff < mzDiff) {
                k = j;
                mzDiff = currentMzDiff;
            }
        }
        return k;
    }

    public static <S extends Spectrum<P>, P extends Peak> int indexOfPeakClosestToMassWithinLinear(S spectrum, double mz, Deviation dev) {
        if (spectrum.size() <= 0) return -1;
        double mzDiff = Double.POSITIVE_INFINITY;
        final double diff = dev.absoluteFor(mz);
        final double a = mz - diff, b = mz + diff;
        int opt = -1;
        for (int k = 0; k < spectrum.size(); ++k) {
            final double m = spectrum.getMzAt(k);
            final double currentMzDiff = Math.abs(m - mz);
            if (m >= a && m <= b && currentMzDiff < mzDiff) {
                mzDiff = currentMzDiff;
                opt = k;
            }
        }
        return opt;
    }


    /**
     * Binary Search algorithm to find the given the mz value with the lowest distance to the
     * given mz value which respects the given mass deviation.
     *
     * @param spectrum
     * @param mz
     * @param d        allowed deviation (relative and absolute) from the mz value
     * @return index of the search key, if it is contained in the array within the specified range;
     * otherwise, (-(insertion point) - 1). The insertion point is defined as the point at which the
     * key would be inserted into the array: the index of the first element in the range greater
     * than the key, or toIndex if all elements in the range are less than the specified key.
     * Note that this guarantees that the return value will be {@literal >=} 0 if and only if the key is found.
     */
    public static <S extends Spectrum<P>, P extends Peak> int binarySearch(S spectrum, double mz, Deviation d) {
        int pos = binarySearch(spectrum, mz);
        if (pos >= 0) return pos;
        final int insertionPoint = -pos - 1;
        final double dev1 = insertionPoint >= spectrum.size() ? Double.POSITIVE_INFINITY : Math.abs(mz - spectrum.getMzAt(insertionPoint));
        final double dev2 = insertionPoint <= 0 ? Double.POSITIVE_INFINITY : Math.abs(mz - spectrum.getMzAt(insertionPoint - 1));
        if (dev1 < dev2 && dev1 < d.absoluteFor(mz)) return insertionPoint;
        if (dev2 <= dev1 && dev2 < d.absoluteFor(mz)) return insertionPoint - 1;
        return pos;
    }

    /**
     * Search for an exact mz value.
     *
     * @see Spectrums#binarySearch(Spectrum, double)
     */
    public static <S extends Spectrum<P>, P extends Peak> int binarySearch(S spectrum, double mz) {
        if (spectrum.size() > 0) {
            int low = 0;
            int high = spectrum.size() - 1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                int c = Double.compare(spectrum.getMzAt(mid), mz);
                if (c < 0)
                    low = mid + 1;
                else if (c > 0)
                    high = mid - 1;
                else
                    return mid; // key found
            }
            return -(low + 1);
        }
        return -1;
    }

    /**
     * Search for an exact mz and intensity value. spectrum sorted by mz.
     *
     * @see Spectrums#binarySearch(Spectrum, double)
     */
    public static <S extends Spectrum<P>, P extends Peak> int binarySearch(S spectrum, double mz, double intensity) {
        if (spectrum.size() > 0) {
            int low = 0;
            int high = spectrum.size() - 1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                int c = Double.compare(spectrum.getMzAt(mid), mz);
                if (c < 0)
                    low = mid + 1;
                else if (c > 0)
                    high = mid - 1;
                else {
                    // key found
                    if (spectrum.getIntensityAt(mid) == intensity) return mid;
                    int mid2 = mid;
                    while (mid2 > 0) {
                        --mid2;
                        if (spectrum.getMzAt(mid2) != mz) break;
                        if (spectrum.getIntensityAt(mid2) == intensity) return mid;
                    }
                    mid2 = mid;
                    while (mid2 < spectrum.size() - 2) {
                        ++mid2;
                        if (spectrum.getMzAt(mid2) != mz) break;
                        if (spectrum.getIntensityAt(mid2) == intensity) return mid;
                    }
                    return -mid - 1;
                }

            }
            return -(low + 1);
        }
        return -1;
    }

    // TODO: Might want to use an more efficient algorithm, e.g. median of medians
    static double __getMedianIntensity(Spectrum<? extends Peak> spec) {
        final int N = spec.size();
        if (N == 0) return 0;
        if (N == 1) return spec.getIntensityAt(0);
        final double[] array = copyIntensities(spec);
        if (N > 2) Arrays.sort(array);
        return array[array.length / 2];
    }

    public static double getMedianIntensity(Spectrum<? extends Peak> spec) {
        final int N = spec.size();
        if (N <= 40) return __getMedianIntensity(spec);
        final double[] array = copyIntensities(spec);
        final int i = __quickselect(array, 0, array.length, array.length / 2);
        return array[i];
    }

    /**
     * Use quicksort to sort a spectrum by its masses in ascending order
     *
     * @param spectrum
     */
    public static Spectrum<Peak> getMassOrderedSpectrum(Spectrum<? extends Peak> spectrum) {
        if (spectrum instanceof OrderedSpectrum) return (Spectrum<Peak>) spectrum;
        return new SimpleSpectrum(spectrum);

    }

    public static boolean isMassOrderedSpectrum(Spectrum<? extends Peak> spectrum) {
        double mz = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < spectrum.size(); i++) {
            final double mz2 = spectrum.getMzAt(i);
            if (mz2 < mz) return false;
            mz = mz2;
        }
        return true;
    }

    /**
     * Use quicksort to sort a spectrum by its masses in ascending order
     *
     * @param spectrum
     */
    public static <T extends Peak, S extends MutableSpectrum<T>>
    void sortSpectrumByMass(S spectrum) {
        if (spectrum instanceof OrderedSpectrum) return;
        __sortSpectrum__(spectrum, new PeakComparator<T, S>() {
            @Override
            public int compare(S left, S right, int i, int j) {
                return Double.compare(left.getMzAt(i), right.getMzAt(j));
            }
        });
    }

    /**
     * Use quicksort to sort a spectrum by its masses in descending order
     *
     * @param spectrum
     */
    public static <T extends Peak, S extends MutableSpectrum<T>>
    void sortSpectrumByDescendingMass(S spectrum) {
        __sortSpectrum__(spectrum, new PeakComparator<T, S>() {
            @Override
            public int compare(S left, S right, int i, int j) {
                return Double.compare(right.getMzAt(j), left.getMzAt(i));
            }
        });
    }

    /**
     * Use quicksort to sort a spectrum by its intensities in ascending order
     *
     * @param spectrum
     */
    public static <T extends Peak, S extends MutableSpectrum<T>>
    void sortSpectrumByIntensity(S spectrum) {
        __sortSpectrum__(spectrum, new PeakComparator<T, S>() {
            @Override
            public int compare(S left, S right, int i, int j) {
                return Double.compare(left.getIntensityAt(i), right.getIntensityAt(j));
            }
        });
    }

    /**
     * Use quicksort to sort a spectrum by its intensities in descending order
     *
     * @param spectrum
     */
    public static <T extends Peak, S extends MutableSpectrum<T>>
    void sortSpectrumByDescendingIntensity(S spectrum) {
        __sortSpectrum__(spectrum, new PeakComparator<T, S>() {
            @Override
            public int compare(S left, S right, int i, int j) {
                return Double.compare(right.getIntensityAt(j), left.getIntensityAt(i));
            }
        });
    }


    /**
     * select the spectrum from a list of spectra which contains the most intense peak with the specified window
     *
     * @param spectra
     * @param precursorMass
     * @param dev
     * @param <S>
     * @param <P>
     * @return
     */
    public static <S extends Spectrum<P>, P extends Peak> S selectSpectrumWithMostIntensePrecursor(List<S> spectra, double precursorMass, Deviation dev) {
        int mostIntenseIdx = -1;
        double highestIntensity = -1d;
        int idx = 0;
        for (S spectrum : spectra) {
            int i = mostIntensivePeakWithin(spectrum, precursorMass, dev);
            if (i < 0) continue;
            double intensity = spectrum.getIntensityAt(i);
            if (mostIntenseIdx < 0 || intensity > highestIntensity) {
                mostIntenseIdx = idx;
                highestIntensity = intensity;
            }
            ++idx;
        }
        if (mostIntenseIdx < 0) return null;
        return spectra.get(mostIntenseIdx);
    }

    /* *******************************************************************************************
     *
     * 								Private static methods
     *
     * ******************************************************************************************* */

    private interface PeakComparator<P extends Peak, S extends Spectrum<P>> {
        int compare(S left, S right, int i, int j);
    }

    private static <T extends Peak, S extends MutableSpectrum<T>>
    void __sortSpectrum__(S spectrum, PeakComparator<T, S> comp) {
        final int n = spectrum.size();
        // Insertion sort on smallest arrays
        if (n <= 20) {
            for (int i = 0; i < n; i++) {
                for (int j = i; j > 0 && comp.compare(spectrum, spectrum, j, j - 1) < 0; j--) {
                    spectrum.swap(j, j - 1);
                }
            }
            return;
        }
        // quicksort on larger arrays
        if (n > 0) {
            int i = 1;
            for (; i < n; ++i) {
                if (comp.compare(spectrum, spectrum, i, i - 1) < 0) break;
            }
            if (i < n) __quickSort__(spectrum, comp, 0, n - 1, 0);
        }

    }

    private static final short[] ALMOST_RANDOM = new short[]{9205, 23823, 4568, 17548, 15556, 31788, 3, 580, 17648, 22647, 17439, 24971, 10767, 9388, 6174, 21774, 4527, 19015, 22379, 12727, 23433, 11160, 15808, 27189, 17833, 7758, 32619, 12980, 31234, 31103, 5140, 571, 4439};

    /**
     * http://en.wikipedia.org/wiki/Quicksort#In-place_version
     *
     * @param low
     * @param high
     */
    private static <T extends Peak, S extends MutableSpectrum<T>>
    void __quickSort__(S s, PeakComparator<T, S> comp, int low, int high, int depth) {
        int n = high - low + 1;
        if (n >= 20 && depth <= 32) {
            if (low < high) {
                int pivot = ALMOST_RANDOM[depth] % n + low;
                pivot = __partition__(s, comp, low, high, pivot);
                __quickSort__(s, comp, low, pivot - 1, depth + 1);
                __quickSort__(s, comp, pivot + 1, high, depth + 1);
            }
        } else if (n < 40) {
            for (int i = low; i <= high; i++) {
                for (int j = i; j > low && comp.compare(s, s, j, j - 1) < 0; j--) {
                    s.swap(j, j - 1);
                }
            }
            return;
        } else heap_sort(s, comp, low, n);
    }

    private static <T extends Peak, S extends MutableSpectrum<T>> void heap_sort(S s, PeakComparator<T, S> comp, int offset, int length) {
        heap_build(s, comp, offset, length);
        int n = length;
        while (n > 1) {
            s.swap(offset, offset + n - 1);
            heap_heapify(s, comp, offset, --n, 0);
        }

    }

    private static <T extends Peak, S extends MutableSpectrum<T>> void heap_heapify(S s, PeakComparator<T, S> comp, int offset, int length, int i) {
        do {
            int max = i;
            final int right_i = 2 * i + 2;
            final int left_i = right_i - 1;
            if (left_i < length && comp.compare(s, s, offset + left_i, offset + max) > 0)
                max = left_i;
            if (right_i < length && comp.compare(s, s, offset + right_i, offset + max) > 0)
                max = right_i;
            if (max == i)
                break;
            s.swap(offset + i, offset + max);
            i = max;
        } while (true);
    }

    private static <T extends Peak, S extends MutableSpectrum<T>> void heap_build(S s, PeakComparator<T, S> comp, int offset, int length) {
        if (length == 0) return;
        for (int i = (length >> 1) - 1; i >= 0; --i)
            heap_heapify(s, comp, offset, length, i);
    }

    /**
     * http://en.wikipedia.org/wiki/Quicksort#In-place_version
     *
     * @param low
     * @param high
     * @param pivot
     * @return
     */
    private static <T extends Peak, S extends MutableSpectrum<T>>
    int __partition__(S s, PeakComparator<T, S> comp, int low, int high, int pivot) {
        s.swap(high, pivot);
        int store = low;
        for (int i = low; i < high; i++) {
            if (comp.compare(s, s, i, high) < 0) {
                if (i != store) s.swap(i, store);
                store++;
            }
        }
        s.swap(store, high);
        return store;
    }

    // source: http://stackoverflow.com/questions/10662013/finding-the-median-of-an-unsorted-array
    private static int __quickselect(double[] list, int lo, int hi, int k) {
        int n = hi - lo;
        if (n < 2)
            return lo;

        double pivot = list[lo + (ALMOST_RANDOM[k % ALMOST_RANDOM.length]) % n]; // Pick a random pivot

        // Triage list to [<pivot][=pivot][>pivot]
        int nLess = 0, nSame = 0, nMore = 0;
        int lo3 = lo;
        int hi3 = hi;
        while (lo3 < hi3) {
            double e = list[lo3];
            int cmp = Double.compare(e, pivot);
            if (cmp < 0) {
                nLess++;
                lo3++;
            } else if (cmp > 0) {
                __swap(list, lo3, --hi3);
                if (nSame > 0)
                    __swap(list, hi3, hi3 + nSame);
                nMore++;
            } else {
                nSame++;
                __swap(list, lo3, --hi3);
            }
        }
        assert (nSame > 0);
        assert (nLess + nSame + nMore == n);
        assert (list[lo + nLess] == pivot);
        assert (list[hi - nMore - 1] == pivot);
        if (k >= n - nMore)
            return __quickselect(list, hi - nMore, hi, k - nLess - nSame);
        else if (k < nLess)
            return __quickselect(list, lo, lo + nLess, k);
        return lo + k;
    }

    private static void __swap(double[] list, int a, int b) {
        final double z = list[a];
        list[a] = list[b];
        list[b] = z;
    }

    /**
     * Can be used (but is not encouraged!) when you are 100% sure that your spectrum is ordered and just
     * want to cast it to an OrderedSpectrum
     */
    public static <T extends Peak, S extends Spectrum<T>> OrderedSpectrumDelegate<T> getAlreadyOrderedSpectrum(S spec) {
        return new OrderedSpectrumDelegate<>(spec);
    }

    public static void writePeaks(BufferedWriter writer, Spectrum spec) throws IOException {
        for (int k = 0; k < spec.size(); ++k) {
            writer.write(String.valueOf(spec.getMzAt(k)));
            writer.write(" ");
            writer.write(String.valueOf(spec.getIntensityAt(k)));
            writer.newLine();
        }
    }

}
