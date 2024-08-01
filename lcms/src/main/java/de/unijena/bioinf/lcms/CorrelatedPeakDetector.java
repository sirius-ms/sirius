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

package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.ionidentity.AdductMassDifference;
import de.unijena.bioinf.lcms.ionidentity.CorrelationGroupScorer2;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.model.lcms.*;
import de.unijena.bionf.spectral_alignment.CosineQuerySpectrum;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.lang3.Range;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class CorrelatedPeakDetector {

    protected static final double MZ_ISO_ERRT = 0.002;
    protected static final Range<Double>[] ISO_RANGES = new Range[]{
            Range.of(0.99664664 - MZ_ISO_ERRT, 1.00342764 + MZ_ISO_ERRT),
            Range.of(1.99653883209004 - MZ_ISO_ERRT, 2.0067426280592295 + MZ_ISO_ERRT),
            Range.of(2.9950584 - MZ_ISO_ERRT, 3.00995027 + MZ_ISO_ERRT),
            Range.of(3.99359037 - MZ_ISO_ERRT, 4.01300058 + MZ_ISO_ERRT),
            Range.of(4.9937908 - MZ_ISO_ERRT, 5.01572941 + MZ_ISO_ERRT)
    };

    /*
    protected final static double ISOTOPE_COSINE_THRESHOLD=0.8,//0.97,
                                    COSINE_THRESHOLD = 0.98d,
                                  STRICT_COSINE_THRESHOLD = 0.99;
*/

    protected final static double PROBABILITY_THRESHOLD_ISOTOPES = 0.5d,
            PROBABILITY_THRESHOLD_ADDUCTS = 0.5d, STRICT_THRESHOLD = 0.9;


    protected Set<PrecursorIonType> detectableIonTypes;
    protected Map<Long, AdductMassDifference> possibleIonPairsPositive, possibleIonPairsNegative;

    public CorrelatedPeakDetector(Set<PrecursorIonType> detectableIonTypes) {
        this.detectableIonTypes = detectableIonTypes;
        this.possibleIonPairsPositive = AdductMassDifference.getAllDifferences(detectableIonTypes.stream().filter(x -> x.getCharge() > 0 && !x.isIntrinsicalCharged()).collect(Collectors.toSet()));
        this.possibleIonPairsNegative = AdductMassDifference.getAllDifferences(detectableIonTypes.stream().filter(x -> x.getCharge() < 0 && !x.isIntrinsicalCharged()).collect(Collectors.toSet()));
    }

    public static Range<Double> getIsotopeMassRange(int niso) {
        return ISO_RANGES[niso - 1];
    }

    public static boolean hasMassOfAnIsotope(double peak, double isoPeak) {
        final double massdiff = isoPeak - peak;
        for (Range<Double> r : ISO_RANGES) {
            if (r.contains(massdiff)) return true;
        }
        return false;
    }

    public boolean doIAmAnIsotope(ProcessedSample sample, FragmentedIon ion, TDoubleArrayList alreadyAnnotatedMzs) {
        // we assume that the peak might be either the second or the third isotopic peak
        Scan ms1Scan = sample.run.getScanByNumber(ion.getSegment().getApexScanNumber()).get();
        final SimpleSpectrum ms1 = sample.storage.getScan(ms1Scan);
        final ScanPoint ionPeak = ion.getPeak().getScanPointAt(ion.getSegment().getApexIndex());
        int peakBefore = Spectrums.mostIntensivePeakWithin(ms1, ionPeak.getMass() - 1.0034d, new Deviation(20, 0.01));
        if (peakBefore < 0) return false;
        double intensity = ms1.getIntensityAt(peakBefore);
        if (intensity / ionPeak.getIntensity() < 0.33) {
            // this is not the second peak. Maybe the third peak of a bromine/chlorine pattern?
            peakBefore = Spectrums.mostIntensivePeakWithin(ms1, ionPeak.getMass() - 2.0016d, new Deviation(20, 0.01));
            if (peakBefore < 0) return false;
            intensity = ms1.getIntensityAt(peakBefore);
            if (intensity / ionPeak.getIntensity() < 0.33)
                return false;
        }
        Optional<ChromatographicPeak> peakBeforeChr = sample.builder.detectExact(ms1Scan, ms1.getMzAt(peakBefore));
        if (!peakBeforeChr.isPresent()) return false;
        Optional<ChromatographicPeak.Segment> segmentForScanId = peakBeforeChr.get().getSegmentForScanId(ms1Scan.getIndex());
        if (!segmentForScanId.isPresent()) return false;
        if (segmentForScanId.get().getApexScanNumber() != ion.getSegment().getApexScanNumber()) {
            // we don't trust this peak...
            return false;
        }
        List<CorrelationGroup> correlationGroups = new ArrayList<>();
        detectIsotopesFor(sample, peakBeforeChr.get().mutate(), segmentForScanId.get(), ion.getChargeState(), correlationGroups, new SimpleMutableSpectrum());
        for (CorrelationGroup g : correlationGroups) {
            int scanNumber = g.getRight().findScanNumber(ms1Scan.getIndex());
            if (scanNumber >= 0 && new CorrelationGroupScorer2().predictProbability(g.getLeftSegment(), g.getRightSegment()) >= STRICT_THRESHOLD  /*g.getCosine() >= STRICT_COSINE_THRESHOLD*/ && Math.abs(g.getRight().getMzAt(scanNumber) - ionPeak.getMass()) < 1e-8) {
                final SimpleMutableSpectrum buffer = new SimpleMutableSpectrum();
                for (CorrelationGroup h : correlationGroups) {
                    int sc = h.getRight().findScanNumber(ms1Scan.getIndex());
                    if (sc >= 0) {
                        buffer.addPeak(h.getRight().getScanPointAt(sc));
                    }
                }
                if (isIsotopePattern(new SimpleSpectrum(buffer))) {
                    //System.out.println(ion + " :: IS AN ISOTOPE!!! ");
                    //System.out.println(g.getLeft().getScanPointAt(g.getLeft().findScanNumber(ms1Scan.getScanNumber())));
                    for (CorrelationGroup h : correlationGroups) {
                        final int scanNumber1 = h.getRight().findScanNumber(segmentForScanId.get().getApexScanNumber());
                        //System.out.println(h.getRight().getScanPointAt(scanNumber1) + " " + h);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isIsotopePattern(SimpleSpectrum spectrum) {
        if (spectrum.size() < 2) return false;
        // 1. contains strange elements?
        // TODO: add DNN here
        // 2. is super large mass?
        if (spectrum.getMzAt(0) > 1200 && spectrum.getIntensityAt(1) / spectrum.getIntensityAt(0) > 1.25)
            return false;
        // 3. otherwise, has to be monotonic
        if (spectrum.getIntensityAt(1) / spectrum.getIntensityAt(0) > 0.66)
            return false;

        return true;
    }

    /**
     * Detect and annotate correlated adducts and isotopes.
     *
     * @param ion
     * @return false, if the ion itself is an isotope
     */
    public boolean detectCorrelatedPeaks(ProcessedSample sample, FragmentedIon ion) {
        if (ion.isAdductDetectionDone()) return true;
        // ensure that we do not double-annotate a peak
        final TDoubleArrayList alreadyAnnotatedMzs = new TDoubleArrayList();
        // 1. detect isotopes of main feature
        detectIsotopesAndSetChargeStateForAndChimerics(sample, ion, alreadyAnnotatedMzs);

        if (ion.getChargeState() > 1) {
            LoggerFactory.getLogger(getClass()).warn("Multiple charged Ion detected! Ion = " + ion.toString() + "\t" + ion.getIntensityAfterPrecursor());
        }
        // do I am an isotope peak myself?
        if (doIAmAnIsotope(sample, ion, alreadyAnnotatedMzs)) {
            return false;
        }

        // 2. detect adducts and in-source fragments
        detectAdductsAndInSourceFor(sample, ion, alreadyAnnotatedMzs);
        // 3. detect isotopes of adducts/in-source fragments
        if (ion.getMsMs() != null) detectInSourceFragmentsFor(sample, ion, alreadyAnnotatedMzs);

        ion.setAdductDetectionDone(true);
        return true;

    }

    private void detectInSourceFragmentsFor(ProcessedSample sample, FragmentedIon ion, TDoubleArrayList alreadyAnnotatedMzs) {
        final Deviation dev = new Deviation(20);
        final Spectrum<Peak> spectrum = ion.getMsMs();
        Scan ms1Scan = sample.run.getScanByNumber(ion.getSegment().getApexScanNumber()).get();
        final SimpleSpectrum ms1 = sample.storage.getScan(ms1Scan);
        final double basePeak = Spectrums.getMaximalIntensity(spectrum);
        double precursor = ion.getPrecursor().getMass();
        for (int k = 0; k < spectrum.size(); ++k) {
            if (spectrum.getMzAt(k) >= (precursor - 2))
                continue;
            if (spectrum.getIntensityAt(k) / basePeak >= 0.05) {
                final double peak = spectrum.getMzAt(k);
                int l = Spectrums.mostIntensivePeakWithin(ms1, peak, dev);
                if (l < 0)
                    continue;
                final double intensity = ms1.getIntensityAt(l) / ion.getPeak().getIntensityAt(ion.getSegment().getApexIndex());
                if (intensity >= 0.1) {
                    if (alreadyFound(alreadyAnnotatedMzs, ms1.getMzAt(l)))
                        continue;
                    // might be a in-source fragment
                    final Optional<ChromatographicPeak> detection = sample.builder.detectExact(ms1Scan, ms1.getMzAt(l));
                    if (detection.isPresent()) {

                        Optional<CorrelationGroup> correlate = correlate(ion.getPeak(), ion.getSegment(), detection.get().mutate());
                        if (correlate.isEmpty()) continue;
                        if (correlate.get().score >= STRICT_THRESHOLD) {
                            final IonGroup ion1 = ionWithIsotopes(sample, correlate.get().getRight(), correlate.get().getRightSegment(), ion.getChargeState(), alreadyAnnotatedMzs);
                            if (ion1 == null) continue;
                            ion.getInSourceFragments().add(new CorrelatedIon(correlate.get(), ion1));
                            //System.out.println(ion + " :: Found IN-SOURCE FRAGMENT with delta m/z = " + (ion.getMsMs().getPrecursor().getMass() - peak)  + ", " + correlate );
                            //System.out.println("Add insource " + correlate.getRight().getScanPointForScanId(ms1Scan.getScanNumber()).getMass());
                            List<CorrelationGroup> isos = new ArrayList<>();
                            detectIsotopesFor(sample, correlate.get().getRight(), correlate.get().getRightSegment(), ion.getChargeState(), isos, new SimpleMutableSpectrum());
                            alreadyAnnotatedMzs.add(ms1.getMzAt(l));
                            for (CorrelationGroup isotopePeak : isos) {
                                alreadyAnnotatedMzs.add(isotopePeak.getRight().getMzAt(isotopePeak.getRightSegment().getApexIndex()));
                                //System.out.println("Add insource isotope " + scanPointForScanId.getMass());
                            }
                            //System.out.println(Arrays.toString(alreadyAnnotatedMzs.toArray()));
                        }
                    }
                }
            }
        }
    }

    private boolean alreadyFound(TDoubleArrayList alreadyannotated, double mz) {
        for (int aap = 0; aap < alreadyannotated.size(); ++aap) {
            if (Math.abs(mz - alreadyannotated.getQuick(aap)) < 1e-6) {
                return true;
            }
        }
        return false;
    }

    private void detectAdductsAndInSourceFor(ProcessedSample sample, FragmentedIon ion, TDoubleArrayList alreadyAnnotatedMzs) {
        final ArrayList<PrecursorIonType> detectedIonTypes = new ArrayList<>();
        final ArrayList<CorrelatedIon> adducts = new ArrayList<>();
        ScanPoint scanPoint = ion.getPeak().getScanPointAt(ion.getSegment().getApexIndex());
        Scan ms1Scan = sample.run.getScanByNumber(scanPoint.getScanNumber()).get();
        for (AdductMassDifference pair : (ion.getPolarity() >= 0 ? this.possibleIonPairsPositive : this.possibleIonPairsNegative).values()) {
            final double delta = pair.getDeltaMass();
            sample.builder.detectWithFallback(ms1Scan, scanPoint.getMass() + delta,
                    (trace)->{
                        double peakMass = trace.getScanPointForScanId(ms1Scan.getIndex()).getMass();
                        if (alreadyFound(alreadyAnnotatedMzs, peakMass))
                            return;
                        // add ion as possibleIonType. But first make correlation analysis
                        Optional<CorrelationGroup> maybeCorrelate = correlate(ion.getPeak(), ion.getSegment(), trace.mutate());
                        CorrelationGroup correlate = null;
                        if (maybeCorrelate.isPresent()) {
                            correlate = maybeCorrelate.get();
                            correlate.setAdductAssignment(pair);
                        }
                        if (maybeCorrelate.isPresent() && correlate.score >= PROBABILITY_THRESHOLD_ADDUCTS && correlate.getNumberOfCorrelatedPeaks() >= 4) {
                            final IonGroup ion1 = ionWithIsotopes(sample, correlate.getRight(), correlate.getRightSegment(), ion.getChargeState(), alreadyAnnotatedMzs);
                            if (ion1 == null) return;
                            adducts.add(new CorrelatedIon(correlate, ion1));
                            detectedIonTypes.addAll(Arrays.asList(pair.getAllLeftTypes()));
                            //System.out.println(ion.toString() + " :: Found " + ionType.toString() + " -> " + other.toString() + " with correlation " + correlate);
                            alreadyAnnotatedMzs.add(peakMass);
                            correlate.setAnnotation(Arrays.stream(pair.getAllLeftTypes()).map(PrecursorIonType::toString).collect(Collectors.joining(",")));
                        }
                        if (maybeCorrelate.isPresent() && correlate.score>=0.5) {
                            // just add as peak
                            ion.addAdductPeak(trace.getScanPointForScanId(ms1Scan.getIndex()));
                        }
                    },
                    ion::addAdductPeak,
                    ()-> {
                        // nothing
                    });
        }
        //if (detectedIonTypes.size()==0)
        //    System.out.println(ion.toString() + " :: NO adducts found.");
        ion.getAdducts().addAll(adducts);

        if (detectedIonTypes.size() == 1) {
            ion.setDetectedIonType(detectedIonTypes.get(0));
        }
        ion.setPossibleAdductTypes(new HashSet<>(detectedIonTypes));

    }

    public void detectIsotopesAndSetChargeStateForAndChimerics(ProcessedSample sample, IonGroup ion, TDoubleArrayList alreadyAnnotatedMzs) {
        final Scan ms1Scan = sample.run.getScanByNumber(ion.getSegment().getApexScanNumber()).get();

        double numberOfPeaksWithStrangeMass = 0, totalNumberOfPeaks = 0d;
        if (ion instanceof FragmentedIon && ((FragmentedIon) ion).getMsMs() != null) {
            final CosineQuerySpectrum c = ((FragmentedIon) ion).getMsMs();
            double maxInt = 0d;
            for (int k = 0; k < c.size(); ++k) {
                double mz = c.getMzAt(k);
                if (mz < 250) {
                    double massDefect = mz - Math.floor(mz);
                    if (massDefect >= 0.3 && massDefect <= 0.7) {
                        numberOfPeaksWithStrangeMass += c.getIntensityAt(k);
                    }
                    totalNumberOfPeaks += c.getIntensityAt(k);
                }
                maxInt = Math.max(c.getIntensityAt(k), maxInt);
            }
            double base = Math.max(totalNumberOfPeaks, maxInt);
            numberOfPeaksWithStrangeMass = base > 0 ? numberOfPeaksWithStrangeMass / base : 0d;
        }
        // check precursor, too
        boolean precursorWithStrangeMass;
        {
            double massDefect = ion.getMass() - Math.floor(ion.getMass());
            precursorWithStrangeMass = ion.getMass() <= 250 && (massDefect >= 0.3 && massDefect <= 0.7);
        }

        // try different charge states
        List<CorrelationGroup> bestPattern = new ArrayList<>();
        SimpleMutableSpectrum bestSpectrum = new SimpleMutableSpectrum();
        int bestChargeState = 0;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int charge = 1; charge < 4; ++charge) {
            SimpleMutableSpectrum buf = new SimpleMutableSpectrum();
            final List<CorrelationGroup> isoPeaks = new ArrayList<>();
            double score = detectIsotopesFor(sample, ion.getPeak(), ion.getSegment(), charge, isoPeaks, buf);
            if (charge > 1)
                score /= 4; // we have twice as many iso peaks, because every even peak picked before is part of this pattern
            if (Double.isFinite(score) && ion instanceof FragmentedIon && ((FragmentedIon) ion).getMsMsQuality().betterThan(Quality.BAD) && sample.intensityAfterPrecursorDistribution != null) {
                if (charge > 1) {
                    if (precursorWithStrangeMass || numberOfPeaksWithStrangeMass >= 0.03) {
                        score *= 8;
                    } else {
                        score *= Math.max(0.1, sample.intensityAfterPrecursorDistribution.getCumulativeProbability(((FragmentedIon) ion).getIntensityAfterPrecursor()));
                    }
                }
            }
            if (score > bestScore) {
                bestChargeState = charge;
                bestPattern = isoPeaks;
                bestScore = score;
                bestSpectrum = buf;
            }
        }
        if (bestPattern.size() > 0) {
            for (CorrelationGroup isotopePeak : bestPattern) {
                alreadyAnnotatedMzs.add(isotopePeak.getRight().getScanPointForScanId(ms1Scan.getIndex()).getMass());
            }
            // do not trust a single isotope peak charge state...
            if (bestChargeState == 1 || bestPattern.size() > 1) {
                ion.setChargeState(bestChargeState);
                ion.addIsotopes(bestPattern, bestSpectrum);
            }
        } else {
            // we still look for an isotope peak. If we cannot correlate anything, so what
            final SimpleSpectrum iso = detectIsotopesWithoutCorrelationFor(sample, ion.getPeak(), ion.getSegment(), 1);
            ion.assignIsotopePeaksWithoutCorrelation(iso);
        }
    }

    private SimpleSpectrum detectIsotopesWithoutCorrelationFor(ProcessedSample sample, ChromatographicPeak peak, ChromatographicPeak.Segment segment, int charge) {
        Scan scan = sample.run.getScanByNumber(segment.getApexScanNumber()).get();
        final SimpleSpectrum spectrum = sample.storage.getScan(scan);
        final SimpleMutableSpectrum buffer = new SimpleMutableSpectrum();
        buffer.addPeak(segment.getApexMass(), segment.getApexIntensity());
        final double mz = peak.getMzAt(segment.getApexIndex());
        double score = 0d;
        forEachIsotopePeak:
        for (int k = 0; k < 2; ++k) {
            // try to detect +k isotope peak
            final double maxMz = mz + ISO_RANGES[k].getMaximum() / charge;
            final int a = Spectrums.indexOfFirstPeakWithin(spectrum, mz + ISO_RANGES[k].getMinimum() / charge, maxMz);
            if (a < 0) break forEachIsotopePeak;
            boolean found = false;
            for (int i = a; i < spectrum.size(); ++i) {
                if (spectrum.getMzAt(i) > maxMz)
                    break;
                // check if this peak is another feature
                final Optional<ChromatographicPeak> cpeak = sample.builder.detectExact(scan, spectrum.getMzAt(i));
                if (cpeak.isPresent() && cpeak.get().numberOfScans() > 3) {
                    // do not trust this one. We should habe found it in the correlation
                    // analysis
                    continue;
                }
                buffer.addPeak(spectrum.getMzAt(i), spectrum.getIntensityAt(i));
                found = true;
            }
            if (!found) {
                break forEachIsotopePeak;
            }
        }
        return Spectrums.getNormalizedSpectrum(buffer, Normalization.Max);
    }

    public IonGroup ionWithIsotopes(ProcessedSample sample, MutableChromatographicPeak peak, ChromatographicPeak.Segment segment, int charge, TDoubleArrayList alreadyAnnotatedMzs) {
        IonGroup ion = new IonGroup(peak, segment, new ArrayList<>());
        detectIsotopesAndSetChargeStateForAndChimerics(sample, ion, alreadyAnnotatedMzs);
        if (ion.getChargeState() == charge || ion.getChargeState() == 0) return ion;
        else return null;
    }

    // some peaks might not correlate because we have not enough data points. We still
    // add them as isotopes. isoPeaks is the list of correlated peaks, buffer is the
    // spectrum of all isotopes
    public double detectIsotopesFor(ProcessedSample sample, MutableChromatographicPeak peak, ChromatographicPeak.Segment segment, int charge, List<CorrelationGroup> isoPeaks, SimpleMutableSpectrum buffer) {

        Scan scan = sample.run.getScanByNumber(segment.getApexScanNumber()).get();
        final SimpleSpectrum spectrum = sample.storage.getScan(scan);
        final double mz = peak.getMzAt(segment.getApexIndex());
        final double monoisotopicIntensity = peak.getIntensityAt(segment.getApexIndex());
        final TIntArrayList indizes = new TIntArrayList();
        double score = 0d;
        int lastCorrelatedPeak = -1;
        forEachIsotopePeak:
        for (int k = 0; k < ISO_RANGES.length; ++k) {
            // try to detect +k isotope peak
            final double maxMz = mz + ISO_RANGES[k].getMaximum() / charge;
            final int a = Spectrums.indexOfFirstPeakWithin(spectrum, mz + ISO_RANGES[k].getMinimum() / charge, maxMz);
            boolean anyCorrelatedPeak = false;
            if (a < 0) break forEachIsotopePeak;
            for (int i = a; i < spectrum.size(); ++i) {
                if (spectrum.getMzAt(i) > maxMz)
                    break;
                final Optional<CorrelationGroup> potentialIsotopePeak = sample.builder.detectExact(scan, spectrum.getMzAt(i)).flatMap(x -> correlate(peak, segment, x.mutate()));
                if (potentialIsotopePeak.isPresent()) {
                    CorrelationGroup iso = potentialIsotopePeak.get();
                    if (iso.score < PROBABILITY_THRESHOLD_ISOTOPES) {
                        // this might be another feature. We rather skip this
                    } else {
                        isoPeaks.add(iso);
                        anyCorrelatedPeak = true;
                        lastCorrelatedPeak = k;
                    }
                } else if (k <= 2 && lastCorrelatedPeak == k - 1) {
                    // just add this peak even when we are not sure if it is really an isotope peak
                    indizes.add(i);
                }
            }
            if (anyCorrelatedPeak) {
                indizes.clear();
            } else if (indizes.size() > 0) {
                for (int i : indizes.toArray()) {
                    buffer.addPeak(spectrum.getMzAt(i), spectrum.getIntensityAt(i) / monoisotopicIntensity);
                }
                indizes.clear();
            } else {
                break; // no isotope peak
            }
        }
        // add all other peaks into the spectrum
        SimpleMutableSpectrum buf2 = LCMSProccessingInstance.toIsotopeSpectrum(isoPeaks, mz);
        for (Peak p : buf2) buffer.addPeak(p);
        Spectrums.sortSpectrumByMass(buffer);
        score += isoPeaks.stream().mapToInt(CorrelationGroup::getNumberOfCorrelatedPeaks).sum();
        if (isoPeaks.isEmpty() && buffer.isEmpty()) return Double.NEGATIVE_INFINITY;
        else return score;
    }


    public Optional<CorrelationGroup> correlate(MutableChromatographicPeak main, ChromatographicPeak.Segment mainSegment, MutableChromatographicPeak mightBeCorrelated) {
        /*
        // overlap both
        int start = mainSegment.getStartScanNumber();
        int end = mainSegment.getEndScanNumber();

        int k = mightBeCorrelated.findScanNumber(start);
        if (k < 0) {
            k = -(k+1);
        }
        int otherStart = k;
        if (otherStart >= mightBeCorrelated.numberOfScans())
            return Optional.empty();
        int otherEnd;
        for (otherEnd=otherStart+1; otherEnd < mightBeCorrelated.numberOfScans(); ++otherEnd) {
            if (mightBeCorrelated.getScanNumberAt(otherEnd) > end)
                break;
        }
        --otherEnd;
        assert otherEnd < mightBeCorrelated.numberOfScans();
        if (otherEnd < otherStart) {
            // no overlap
            return Optional.empty();
        }

        // there should be at least one peak after the apex
        /*
        if (mightBeCorrelated.getScanNumberAt(otherEnd)<=mainSegment.getApexScanNumber())
            return null;
        */

        //final ChromatographicPeak.Segment otherSegment = mightBeCorrelated.getOrCreateSegmentFromIndizes(otherStart,otherEnd);
        final Optional<ChromatographicPeak.Segment> mayHaveOtherSegment = mightBeCorrelated.getSegmentForScanId(mainSegment.getApexScanNumber());
        if (mayHaveOtherSegment.isEmpty()) return Optional.empty();
        ChromatographicPeak.Segment otherSegment = mayHaveOtherSegment.get();


        // the the apex of one peak should be in the fhm of the other and vice-versa
        if (mainSegment.getApexScanNumber() > otherSegment.getPeak().getScanNumberAt(otherSegment.getFwhmEndIndex()) || mainSegment.getApexScanNumber() < otherSegment.getPeak().getScanNumberAt(otherSegment.getFwhmStartIndex()) || otherSegment.getApexScanNumber() > mainSegment.getPeak().getScanNumberAt(mainSegment.getFwhmEndIndex()) || otherSegment.getApexScanNumber() < mainSegment.getPeak().getScanNumberAt(mainSegment.getFwhmStartIndex())) {
            return Optional.empty();
        }


        if (main.getIntensityAt(mainSegment.getApexIndex()) > mightBeCorrelated.getIntensityAt(otherSegment.getApexIndex())) {
            return Optional.ofNullable(correlateBiggerToSmaller(main, mainSegment, mightBeCorrelated, otherSegment));
        } else
            return Optional.ofNullable(correlateBiggerToSmaller(mightBeCorrelated, otherSegment, main, mainSegment)).map(CorrelationGroup::invert);


    }

    private @Nullable CorrelationGroup correlateBiggerToSmaller(MutableChromatographicPeak large, ChromatographicPeak.Segment largeSegment, MutableChromatographicPeak small, ChromatographicPeak.Segment smallSegment) {
        final TDoubleArrayList a = new TDoubleArrayList(), b = new TDoubleArrayList();

        // find index that is above 25% intensity of main peak
        final Range<Integer> t25 = smallSegment.calculateFWHMMinPeaks(0.15d, 3);
        // if small segment is outside boundary of large segment, the peaks do not correlate

        for (int i = t25.getMinimum(); i <= t25.getMaximum(); ++i) {
            int j = large.findScanNumber(small.getScanNumberAt(i));
            if (j >= 0) a.add(large.getIntensityAt(j));
            else {
                return null;
                //a.add(0d);
            }
            b.add(small.getIntensityAt(i));
        }
        if (a.size() < 3) return null;
        final double correlation = pearson(a, b);
        final double kl = kullbackLeibler(a, b, a.size());
        final CorrelationGroup correlationGroup = new CorrelationGroup(large, small, largeSegment, smallSegment, smallSegment.getPeak().getScanNumberAt(t25.getMinimum()), smallSegment.getPeak().getScanNumberAt(t25.getMaximum()), t25.getMaximum()-t25.getMinimum()+1, correlation, kl, cosine(a, b), 0);
        correlationGroup.score = new CorrelationGroupScorer2().predictProbability(correlationGroup.getLeftSegment(), correlationGroup.getRightSegment());

        return correlationGroup;
    }


    public static double cosine(TDoubleArrayList a, TDoubleArrayList b) {
        double[] x = normalized(a);
        double[] y = normalized(b);
        double cosine = 0d, l = 0d, r = 0d;
        for (int k = 0; k < a.size(); ++k) {
            cosine += x[k] * y[k];
            l += x[k] * x[k];
            r += y[k] * y[k];
        }
        if (l == 0 || r == 0) return 0d;
        return cosine / Math.sqrt(l * r);
    }

    public static double kullbackLeibler(TDoubleArrayList a, TDoubleArrayList b, int size) {
        double[] x = normalized(a);
        double[] y = normalized(b);
        double l = 0d, r = 0d;
        for (int k = 0; k < size; ++k) {
            double lx = Math.log(x[k]), ly = Math.log(y[k]);
            l += x[k] * (lx - ly);
            r += y[k] * (ly - lx);
        }
        return l + r;
    }

    public static double[] normalized(TDoubleArrayList a) {
        final double[] b = a.toArray();
        if (b.length < 1) return b;
        double sum = a.sum();
        for (int k = 0; k < b.length; ++k) {
            b[k] /= sum;
        }
        return b;
    }

    public static double[] normalizedToMax(TDoubleArrayList a) {
        final double[] b = a.toArray();
        if (b.length < 1) return b;
        double mx = a.max();
        for (int k = 0; k < b.length; ++k) {
            b[k] /= mx;
        }
        return b;
    }


    public static double pearson(TDoubleArrayList a, TDoubleArrayList b) {
        final int n = b.size();
        double meanA = 0d;
        double meanB = 0d;
        for (int i = 0; i < n; ++i) {
            meanA += a.getQuick(i);
            meanB += b.getQuick(i);
        }
        meanA /= n;
        meanB /= n;
        double va = 0d, vb = 0d, vab = 0d;
        for (int i = 0; i < n; ++i) {
            final double x = a.getQuick(i) - meanA;
            final double y = b.getQuick(i) - meanB;
            va += x * x;
            vb += y * y;
            vab += x * y;
        }
        if (va * vb == 0) return 0d;
        return vab / Math.sqrt(va * vb);
    }

    private final static double LAMBDA = 100d, SIGMA_ABS = 0.05, SIGMA_REL = 0.01;

    private final static double LAMBDA2 = 200d, SIGMA_ABS2 = 0.1, SIGMA_REL2 = 0.01;

    public static double maximumLikelihoodIsotopeScore(TDoubleArrayList xslist, TDoubleArrayList yslist) {
        double currentScore = 0d;
        double maxScore = 0d;
        int maxLenLeft, maxLenRight = 0;
        int apex = findApex(xslist);
        double penalty = 0d;
        final double[] xs = normalizedToMax(xslist);
        final double[] ys = normalizedToMax(yslist);
        final int n = xs.length;
        // we start at position apex and then extend pattern to both sides
        for (int i = apex - 1; i >= 0; --i) {
            final double intensityScore = LAMBDA * xs[i];
            penalty += intensityScore;
            final double delta = xs[i] - ys[i];
            final double matchScore = (-(delta * delta) / (2 * (SIGMA_ABS * SIGMA_ABS + xs[i] * xs[i] * SIGMA_REL * SIGMA_REL))) - Math.log(2 * Math.PI * xs[i] * SIGMA_REL * SIGMA_REL);
            final double score = intensityScore + matchScore;
            currentScore += score;
            if (currentScore >= maxScore) {
                maxScore = currentScore;
                maxLenLeft = apex - i;
            }
        }
        currentScore = maxScore;
        // we start at position apex and then extend pattern to both sides
        for (int i = apex + 1; i < xs.length; ++i) {
            final double intensityScore = LAMBDA * xs[i];
            penalty += intensityScore;
            final double delta = xs[i] - ys[i];
            final double matchScore = (-(delta * delta) / (2 * (SIGMA_ABS * SIGMA_ABS + xs[i] * xs[i] * SIGMA_REL * SIGMA_REL))) - Math.log(2 * Math.PI * xs[i] * SIGMA_REL * SIGMA_REL);
            final double score = intensityScore + matchScore;
            currentScore += score;
            if (currentScore >= maxScore) {
                maxScore = currentScore;
                maxLenRight = i - apex;
            }
        }
        return maxScore - penalty;
    }

    public static double maximumLikelihoodIsotopeScore2(TDoubleArrayList xslist, TDoubleArrayList yslist) {
        double currentScore = 0d;
        double maxScore = 0d;
        int maxLenLeft, maxLenRight = 0;
        int apex = findApex(xslist);
        double penalty = 0d;
        final double[] xs = normalizedToMax(xslist);
        final double[] ys = normalizedToMax(yslist);
        final int n = xs.length;
        // we start at position apex and then extend pattern to both sides
        for (int i = apex - 1; i >= 0; --i) {
            final double intensityScore = LAMBDA2 * xs[i];
            penalty += intensityScore;
            final double delta = xs[i] - ys[i];
            final double matchScore = Math.exp(-(delta * delta) / (2 * (SIGMA_ABS2 * SIGMA_ABS2 + xs[i] * xs[i] * SIGMA_REL2 * SIGMA_REL2))) / (2 * Math.PI * xs[i] * SIGMA_REL2 * SIGMA_REL2);
            final double score = intensityScore + Math.log(matchScore);
            currentScore += score;
            if (currentScore >= maxScore) {
                maxScore = currentScore;
                maxLenLeft = apex - i;
            }
        }
        currentScore = maxScore;
        // we start at position apex and then extend pattern to both sides
        for (int i = apex + 1; i < xs.length; ++i) {
            final double intensityScore = LAMBDA2 * xs[i];
            penalty += intensityScore;
            final double delta = xs[i] - ys[i];
            final double matchScore = Math.exp(-(delta * delta) / (2 * (SIGMA_ABS2 * SIGMA_ABS2 + xs[i] * xs[i] * SIGMA_REL2 * SIGMA_REL2))) / (2 * Math.PI * xs[i] * SIGMA_REL2 * SIGMA_REL2);
            final double score = intensityScore + Math.log(matchScore);
            currentScore += score;
            if (currentScore >= maxScore) {
                maxScore = currentScore;
                maxLenRight = i - apex;
            }
        }
        return maxScore - penalty;
    }

    public static int findApex(TDoubleArrayList xs) {
        int apex = 0;
        double apexInt = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < xs.size(); ++i) {
            if (xs.getQuick(i) > apexInt) {
                apex = i;
                apexInt = xs.getQuick(i);
            }
        }
        return apex;
    }

}