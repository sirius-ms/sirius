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

import com.google.common.collect.Range;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.lcms.CorrelatedPeakDetector;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.MemoryFileStorage;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.lcms.peakshape.PeakShape;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.model.lcms.*;

import java.io.IOException;
import java.util.*;

public class GapFilling {

    public Cluster gapFilling(LCMSProccessingInstance instance, Cluster cluster, double rtError, double peakShapeError, boolean onlyGoodShapes) {

        for (ProcessedSample sample : instance.getSamples()) {
            gapFill(instance, cluster, rtError, peakShapeError, onlyGoodShapes, sample);
        }
        return cluster;
    }

    private static class Gaps {
        private AlignedFeatures feature;
        private ProcessedSample sample;
        private GapFilledIon gapFilledIon;

        public Gaps(AlignedFeatures feature, ProcessedSample sample, GapFilledIon gapFilledIon) {
            this.feature = feature;
            this.sample = sample;
            this.gapFilledIon = gapFilledIon;
        }
    }

    private List<Gaps> gapFill(LCMSProccessingInstance instance, Cluster cluster, double rtError, double peakShapeError, boolean onlyGoodShapes, ProcessedSample sample) {
        final ArrayList<Gaps> gaps = new ArrayList<>();
        if (sample.storage instanceof MemoryFileStorage)
            ((MemoryFileStorage) sample.storage).keepInMemory();
        final Set<ChromatographicPeak.Segment> segments = new HashSet<>();
        for (FragmentedIon ion : sample.ions)
            segments.add(ion.getSegment());
        for (FragmentedIon ion : sample.gapFilledIons)
            segments.add(ion.getSegment());
        for (FragmentedIon ion : sample.otherIons)
            segments.add(ion.getSegment());

        for (AlignedFeatures f : cluster.features) {
            // most abundant ion
            FragmentedIon mostAbundant = f.representativeFeature!=null ? f.getFeatures().get(f.representativeFeature) : null;
            if (onlyGoodShapes && (mostAbundant==null || mostAbundant.getPeakShape().getPeakShapeQuality().notBetterThan(Quality.BAD)))
                continue;
            if (onlyGoodShapes && mostAbundant.getIsotopes().size()<=1)
                continue;
            if (f.features.containsKey(sample) && !(f.features.get(sample) instanceof GapFilledIon)) {
                // no need for gap filling
            } else {
                final HashMap<ProcessedSample, FragmentedIon> ions =  new HashMap<>(f.features);
                final double retentionTimeTolerance = rtError;
                final double[] rets;
                rets = ions.entrySet().stream().filter(x -> !(x instanceof GapFilledIon)).mapToDouble(e -> e.getKey().getRecalibratedRT(e.getValue().getRetentionTime())).toArray();
                Arrays.sort(rets);
                double lowest = rets[(int)(0.25*rets.length)] - retentionTimeTolerance;
                double highest = rets[(int)(0.75*rets.length)] + retentionTimeTolerance;
                final double middle = rets[(int)(rets.length*0.5)];
                double avg = f.rt;
                final Range<Double> tolerance = Range.closed(lowest,highest);
                //System.out.println(String.valueOf(lowest/1000d) + " ... " + (highest/1000d) + " seconds tolerance");

                ArrayList<Scan> scans = new ArrayList<>(sample.findScansByRecalibratedRT(Range.closed(lowest,highest)).values());
                if (scans.isEmpty())
                    continue;

                final HashSet<ChromatographicPeak> alreadyTried = new HashSet<>();
                outerLoop:
                for (final double tol : new double[]{4,2,1}) {
                    final double LOW = middle - (middle-lowest)/tol;
                    final double HIGH = middle + (highest-middle)/tol;
                    scans = new ArrayList<>(sample.findScansByRecalibratedRT(Range.closed(LOW,HIGH)).values());
                    if (scans.isEmpty())
                        continue;
                    final Optional<ChromatographicPeak> peak = sample.builder.detect(Range.closed(scans.get(0).getIndex(), scans.get(scans.size()-1).getIndex()), f.mass); // TODO: recalibrate mass, too?
                    if (peak.isPresent()) {
                        if (alreadyTried.contains(peak.get()))
                            continue ;
                        alreadyTried.add(peak.get());
                        for (ChromatographicPeak.Segment seg : peak.get().getSegments()) {
                            final double peakRt = sample.getRecalibratedRT(peak.get().getRetentionTimeAt(seg.getApexIndex()));
                            if (tolerance.contains(peakRt)) {
                                if (segments.contains(seg))
                                    continue ; // we already know this ion
                                final GapFilledIon pseudoIon = new GapFilledIon(sample.run.getScanByNumber(seg.getPeak().getScanPointAt(seg.getApexIndex()).getScanNumber()).map(Scan::getPolarity).orElse(Polarity.UNKNOWN),  peak.get(), seg, mostAbundant);
                                // search for isotopes
                                if (new CorrelatedPeakDetector(instance.getDetectableIonTypes()).detectCorrelatedPeaks(sample, pseudoIon)) {

                                    if (onlyGoodShapes) {
                                        if (pseudoIon.getIsotopes().size()<=1)
                                            continue;
                                        final PeakShape shape = instance.fitPeakShape(sample, pseudoIon);
                                        if (shape.getPeakShapeQuality().notBetterThan(Quality.BAD))
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
                                        segments.add(seg);
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

    public BasicJJob<Cluster> gapFillingInParallel(LCMSProccessingInstance instance, Cluster cluster, double rtError, double peakShapeError, boolean onlyGoodShapes) {
        return new BasicMasterJJob<Cluster>(JJob.JobType.SCHEDULER) {
            @Override
            protected Cluster compute() throws Exception {
                final ArrayList<BasicJJob<List<Gaps>>> jobs = new ArrayList<>();
                for (ProcessedSample s : new HashSet<>(instance.getSamples())) {
                    jobs.add(submitSubJob(new BasicJJob<List<Gaps>>() {
                        @Override
                        protected List<Gaps> compute() throws Exception {
                            return gapFill(instance,cluster,rtError,peakShapeError,onlyGoodShapes,s);
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
}
