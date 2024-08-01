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

package de.unijena.bioinf.lcms.align;

import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.lcms.*;
import de.unijena.bioinf.lcms.peakshape.PeakShape;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.model.lcms.*;
import org.apache.commons.lang3.Range;

import java.io.IOException;
import java.util.*;

public class GapFilling {

    public Cluster gapFilling(LCMSProccessingInstance instance, Cluster cluster, double rtError, double peakShapeError, Quality minShapeQuality) {

        for (ProcessedSample sample : instance.getSamples()) {
            gapFill(instance, cluster, rtError, peakShapeError, minShapeQuality, sample);
        }
        return cluster;
    }

    private static class Gaps {
        private final AlignedFeatures feature;
        private final ProcessedSample sample;
        private final GapFilledIon gapFilledIon;

        public Gaps(AlignedFeatures feature, ProcessedSample sample, GapFilledIon gapFilledIon) {
            this.feature = feature;
            this.sample = sample;
            this.gapFilledIon = gapFilledIon;
        }
    }

    private  List<Gaps> gapFill(LCMSProccessingInstance instance, Cluster cluster, double rtError, double peakShapeError, Quality minShapeQuality, ProcessedSample sample) {
        final ArrayList<Gaps> gaps = new ArrayList<>();
        if (sample.storage instanceof MemoryFileStorage)
            ((MemoryFileStorage) sample.storage).keepInMemory();
        final Set<SegmentPointer> segments = new HashSet<>();
        for (FragmentedIon ion : sample.ions)
            segments.add(new SegmentPointer(ion));
        for (FragmentedIon ion : sample.gapFilledIons)
            segments.add(new SegmentPointer(ion));
        for (FragmentedIon ion : sample.otherIons)
            segments.add(new SegmentPointer(ion));

        for (AlignedFeatures f : cluster.features) {
            // most abundant ion
            FragmentedIon mostAbundant = f.representativeFeature!=null ? f.getFeatures().get(f.representativeFeature) : null;
            if ((mostAbundant==null || mostAbundant.getPeakShape().getPeakShapeQuality().worseThan(minShapeQuality)))
                continue;
            // don't trust compounds without isotopes
            if (mostAbundant.getIsotopes().size()<=1)
                continue;
            if (f.features.containsKey(sample) && !(f.features.get(sample) instanceof GapFilledIon)) {
                // no need for gap filling
            } else {
                final HashMap<ProcessedSample, FragmentedIon> ions =  new HashMap<>(f.features);
                final double[] rets;
                rets = ions.entrySet().stream().filter(x -> !(x instanceof GapFilledIon)).mapToDouble(e -> e.getKey().getRecalibratedRT(e.getValue().getRetentionTime())).toArray();
                Arrays.sort(rets);
                double lowest = rets[(int)(0.25*rets.length)] - rtError;
                double highest = rets[(int)(0.75*rets.length)] + rtError;
                final double middle = rets[(int)(rets.length*0.5)];
                double avg = f.rt;
                final Range<Double> tolerance = Range.of(lowest,highest);
                //System.out.println(String.valueOf(lowest/1000d) + " ... " + (highest/1000d) + " seconds tolerance");

                ArrayList<Scan> scans = new ArrayList<>(sample.findScansByRecalibratedRT(Range.of(lowest,highest)).values());
                if (scans.isEmpty())
                    continue;

                final HashSet<ChromatographicPeak> alreadyTried = new HashSet<>();
                outerLoop:
                for (final double tol : new double[]{4,2,1}) {
                    final double LOW = middle - (middle-lowest)/tol;
                    final double HIGH = middle + (highest-middle)/tol;
                    scans = new ArrayList<>(sample.findScansByRecalibratedRT(Range.of(LOW,HIGH)).values());
                    if (scans.isEmpty())
                        continue;
                    Range<Long> rtRange = Range.of(scans.getFirst().getRetentionTime(),scans.getLast().getRetentionTime());
                    final Optional<ChromatographicPeak> peak = sample.builder.detect(Range.of(scans.getFirst().getIndex(), scans.getLast().getIndex()), f.mass); // TODO: recalibrate mass, too?
                    if (peak.isPresent()) {
                        final MutableChromatographicPeak P = peak.get().mutate();
                        if (alreadyTried.contains(peak.get()))
                            continue ; // could be critical
                        alreadyTried.add(peak.get());
                        DivisionPointer potentialDivision = null;
                        SegmentPointer potentialSegment = null;
                        double bestFoundSoFar = Double.POSITIVE_INFINITY;
                        for (ChromatographicPeak.Segment seg : peak.get().getSegments().values()) {
                            final double peakRt = sample.getRecalibratedRT(peak.get().getRetentionTimeAt(seg.getApexIndex()));
                            if (tolerance.contains(peakRt)) {
                                if (segments.contains(new SegmentPointer(seg))) {

                                    final Optional<DivisionPointer> p = splitSegment(sample, peak.get().mutate(), seg, rtRange);
                                    if (p.isPresent()) {
                                        if (potentialDivision == null || potentialDivision.score < p.get().score)
                                            potentialDivision = p.get();
                                    }
                                } else {
                                    double rtDiff = Math.abs(peakRt - middle);
                                    if (potentialSegment == null || rtDiff < bestFoundSoFar) {
                                        bestFoundSoFar = rtDiff;
                                        potentialSegment = new SegmentPointer(seg);
                                    }
                                }
                            }
                        }
                        {
                            ChromatographicPeak.Segment seg = null;
                            if (potentialSegment!=null) {
                                seg = P.getSegmentWithApexId(potentialSegment.apexIndex).get();
                            } else if (potentialDivision!=null) {
                                seg = P.getSegmentWithApexId(potentialDivision.performDivision(P).apexIndex).get();
                            }
                            if (seg!=null){
                                final GapFilledIon pseudoIon = new GapFilledIon(sample.run.getScanByNumber(seg.getPeak().getScanPointAt(seg.getApexIndex()).getScanNumber()).map(Scan::getPolarity).orElse(Polarity.UNKNOWN),  peak.get().mutate(), seg, mostAbundant);
                                // search for isotopes
                                if (new CorrelatedPeakDetector(instance.getDetectableIonTypes()).detectCorrelatedPeaks(sample, pseudoIon)) {

                                    {
                                        if (minShapeQuality.betterThan(Quality.DECENT) && pseudoIon.getIsotopes().size()<1)
                                            continue;
                                        final PeakShape shape = instance.fitPeakShape(sample, pseudoIon);
                                        if (shape.getPeakShapeQuality().worseThan(minShapeQuality))
                                            continue;
                                    }

                                    double avgError = 0d; int n=0;
                                    for (FragmentedIon ion : ions.values()) {
                                        if (!(ion instanceof GapFilledIon)) {
                                            avgError += ion.comparePeakWidthSmallToLarge(pseudoIon);
                                            ++n;
                                        }
                                    }
                                    avgError /= n;

                                    if (avgError >= 5*peakShapeError) {
                                        //System.err.println("REJECTED DUE TO PEAK SHAPE OF " + avgError);
                                        continue;
                                    }
                                    pseudoIon.setPeakShape(instance.fitPeakShape(sample, pseudoIon));

                                    FragmentedIon ion = ions.get(sample);
                                    if (ion == null || (Math.abs(ion.getRetentionTime() - middle) > Math.abs(pseudoIon.getRetentionTime() - middle))) {
                                        gaps.add(new Gaps(f,sample,pseudoIon));
                                        segments.add(new SegmentPointer(seg));
                                        break outerLoop;
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
        if (sample.storage instanceof MemoryFileStorage) {
            try {
                ((MemoryFileStorage) sample.storage).backOnDisc();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return gaps;
    }

    private  Optional<DivisionPointer> splitSegment(ProcessedSample sample, MutableChromatographicPeak peak, ChromatographicPeak.Segment segment, Range<Long> rtRange) {
        // search in segment for a second apex close to the expected peak.
        // Split segment into two parts
        int start = segment.getStartIndex(), ende = segment.getEndIndex();
        // if the segment is too small, we do not want to split it any further
        if (ende-start < 6) return Optional.empty();
        Extrema extrema = new Extrema();
        extrema.addExtremum(start, peak.getIntensityAt(start));
        boolean isMinimum = (peak.getIntensityAt(start) <= peak.getIntensityAt(start+1));
        int regionStart = start, regionEnd = ende;
        for (int k=start+1; k < ende; ++k) {
            if (rtRange.getMinimum() >= peak.getRetentionTimeAt(k)) {
                regionStart = k;
            }
            if (rtRange.getMaximum() <= peak.getRetentionTimeAt(k)) {
                regionEnd = k;
            }
            double a = peak.getIntensityAt(k-1), b = peak.getIntensityAt(k), c = peak.getIntensityAt(k+1);
            if (isMinimum && b >= a && b >= c) {
                extrema.addExtremum(k, b);
                isMinimum = false;
            } else if (!isMinimum && b <= a && b <= c) {
                extrema.addExtremum(k,b);
                isMinimum = true;
            }
        }
        // we are only interested in maxima within the tolerance region
        extrema.trimToRegion(regionStart, regionEnd);
        if (extrema.numberOfExtrema()<3) return Optional.empty();
        // is the existing apex part of this region?
        int knownApex = -1;
        for (int j=0; j < extrema.numberOfExtrema(); ++j) {
            if (segment.getApexIndex() == extrema.getIndexAt(j)) {
                knownApex = j;
                break;
            }
        }
        if (knownApex>=0) {
            final int[] secondApex = extrema.findSecondApex(knownApex);
            if (secondApex.length==0) return Optional.empty();
            return divide(sample, peak, segment, secondApex[1],secondApex[0],secondApex[2] );
        } else {
            // the peak with maximum intensity is the new apex
            int mx = extrema.getIndexAt(extrema.globalMaximum());
            int minimum = -1;
            double minInt = Double.POSITIVE_INFINITY;
            if (mx < segment.getApexIndex()) {
                for (int k=mx+1; k < segment.getApexIndex(); ++k) {
                    if (peak.getIntensityAt(k) < minInt) {
                        minInt = peak.getIntensityAt(k);
                        minimum=k;
                    }
                }
                if (minimum<0) return Optional.empty();
                return divide(sample, peak, segment, minimum, segment.getApexIndex(), mx);
            } else {
                for (int k=mx-1; k > segment.getApexIndex(); --k) {
                    if (peak.getIntensityAt(k) < minInt) {
                        minInt = peak.getIntensityAt(k);
                        minimum=k;
                    }
                }
                if (minimum<0) return Optional.empty();
                return divide(sample, peak, segment, minimum, segment.getApexIndex(), mx);

            }

        }



    }

    private  Optional<DivisionPointer> divide(ProcessedSample sample, MutableChromatographicPeak peak, ChromatographicPeak.Segment segment, int minimum, int apex, int secondApex) {
        double noiseLevel = sample.ms1NoiseModel.getNoiseLevel(peak.getScanNumberAt(apex), peak.getMzAt(apex));

        double left = peak.getIntensityAt(segment.getStartIndex());
        double right = peak.getIntensityAt(segment.getEndIndex());
        double secApexInt = peak.getIntensityAt(secondApex);
        double minimumInt = peak.getIntensityAt(minimum);
        double steepness;
        if (apex > secondApex) {
            steepness = Math.sqrt((secApexInt-right)*(secApexInt-minimumInt));
            if (steepness<noiseLevel) return Optional.empty();
            //peak.divideSegment(segment, minimum, secondApex,apex);
            return Optional.of(new DivisionPointer(segment, minimum, apex, secondApex, steepness));
        } else {
            steepness = Math.sqrt((secApexInt-left)*(secApexInt-minimumInt));
            if (steepness<noiseLevel) return Optional.empty();
            //peak.divideSegment(segment, minimum, apex,secondApex);
            return Optional.of(new DivisionPointer(segment, minimum, apex, secondApex, steepness));
        }
    }


    public BasicJJob<Cluster> gapFillingInParallel(LCMSProccessingInstance instance, Cluster cluster, double rtError, double peakShapeError, Quality minShapeQuality) {
        return new BasicMasterJJob<Cluster>(JJob.JobType.SCHEDULER) {
            @Override
            protected Cluster compute() throws Exception {
                final ArrayList<BasicJJob<List<Gaps>>> jobs = new ArrayList<>();
                for (ProcessedSample s : new HashSet<>(instance.getSamples())) {
                    jobs.add(submitSubJob(new BasicJJob<List<Gaps>>() {
                        @Override
                        protected List<Gaps> compute() throws Exception {
                            return gapFill(instance,cluster,rtError,peakShapeError,minShapeQuality,s);
                        }
                    }));
                }
                jobs.forEach(JJob::takeResult);
                for (BasicJJob<List<Gaps>> job : jobs) {
                    List<Gaps> gaps = job.takeResult();
                    for (Gaps g : gaps) {
                        g.feature.getFeatures().putIfAbsent(g.sample,g.gapFilledIon);
                        g.sample.gapFilledIons.add(g.gapFilledIon);
                    }
                }
                return cluster;
            }
        };
    }

    private static class DivisionPointer extends SegmentPointer {
        private final int min, apex, secondApex;
        private final double score;

        private DivisionPointer(ChromatographicPeak.Segment segment, int min, int apex, int secondApex, double score) {
            super(segment);
            this.min = min;
            this.apex = apex;
            this.secondApex = secondApex;
            this.score = score;
        }

        public SegmentPointer performDivision(MutableChromatographicPeak peak) {
            peak.divideSegment(peak.getSegmentWithApexId(apex).orElseThrow(), min, Math.min(apex, secondApex), Math.max(apex, secondApex));
            return new SegmentPointer(peak, secondApex);
        }
    }

    private static class SegmentPointer {
        private final ChromatographicPeak peak;
        private final int apexIndex;

        public SegmentPointer(IonGroup ion) {
            this(ion.getPeak(), ion.getApexIndex());
        }

        public SegmentPointer(ChromatographicPeak.Segment segment) {
            this.peak = segment.getPeak();
            this.apexIndex = segment.getApexIndex();
        }

        public SegmentPointer(ChromatographicPeak peak, int apexIndex) {
            this.peak = peak;
            this.apexIndex = apexIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SegmentPointer that = (SegmentPointer) o;
            return apexIndex == that.apexIndex && peak.equals(that.peak);
        }

        @Override
        public String toString() {
            return peak.getSegmentWithApexId(apexIndex).map(x->x.toString()).orElseGet(()->peak.toString() + " with apex " + peak.getRetentionTimeAt(apexIndex) + " min unknown");
        }

        @Override
        public int hashCode() {
            return peak.hashCode() + (1+apexIndex);
        }
    }
}
