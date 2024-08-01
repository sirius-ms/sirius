/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 * Chair of Bioinformatics, Friedrich-Schilller University.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.lcms.align.AlignedFeatures;
import de.unijena.bioinf.lcms.align.Cluster;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.model.lcms.*;
import de.unijena.bionf.spectral_alignment.CosineQuerySpectrum;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.lang3.Range;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Ms1Remapping {

    public static LCMSPeakInformation[] remapMS1(LCMSProccessingInstance in, ProcessedSample[] ms1, LCMSPeakInformation[] ms2, Ms2Experiment[] experiments, boolean dropExisting) {
        final long[] retentionTimes = new long[ms2.length];
        final double[] mzs = new double[ms2.length];
        final long[] allowedDeviations = new long[ms2.length];
        final SimpleSpectrum[] ms2Spectra = new SimpleSpectrum[ms2.length];

        AlignedFeatures[] features = new AlignedFeatures[ms2.length];
        for (int i=0; i < ms2.length; ++i) {
            if (dropExisting) {
                findMs2RetentionTime(i, ms2[i], retentionTimes, allowedDeviations, mzs);
            } else {
                findAverageRetentionTime(i, ms2[i], retentionTimes, allowedDeviations,mzs);
            }
            ms2Spectra[i] = Spectrums.mergeSpectra(new Deviation(10), true, false, experiments[i].getMs2Spectra());
        }

        final JobManager globalJobManager = SiriusJobs.getGlobalJobManager();
        final List<BasicJJob<Object>> jobs = new ArrayList<>();

        for (ProcessedSample sample : ms1) {
            for (int i=0;  i < retentionTimes.length; ++i) {
                final int I = i;
                jobs.add(globalJobManager.submitJob(new BasicJJob<Object>() {
                    @Override
                    protected Object compute() throws Exception {
                        final NavigableMap<Integer, Scan> scansByRT = sample.findScansByRT(Range.of(retentionTimes[I] - allowedDeviations[I], retentionTimes[I] + allowedDeviations[I]));
                        if (scansByRT.isEmpty()) return null;
                        int bestRt = scansByRT.values().stream().min(Comparator.comparingDouble(x->Math.abs(x.getRetentionTime()-retentionTimes[I]))).get().getIndex();
                        final Optional<ChromatographicPeak> detect = sample.builder.detectFirst(Range.of(scansByRT.firstEntry().getKey(), scansByRT.lastEntry().getKey()), bestRt, mzs[I]);
                        if (detect.isPresent()) {
                            final ChromatographicPeak peak = detect.get();
                            Optional<ChromatographicPeak.Segment> s = peak.getSegmentForScanId(peak.getScanNumberAt(peak.findClosestIndexByRt(retentionTimes[I])));
                            if (s.isPresent()) {
                                final Scan ms2Scan = experiment2Scan(experiments[I], ms2Spectra[I], s.get(), retentionTimes[I]);
                                FragmentedIon ion = new FragmentedIon(
                                        ms2Scan.getPolarity(),new Scan[]{ms2Scan}, new CollisionEnergy[]{ms2Scan.getCollisionEnergy()}, experiment2querySpectrum(experiments[I], ms2Spectra[I], s.get(),s.get().getApexRt() ), Quality.GOOD,
                                        peak.mutate(), s.get(), new Scan[0]
                                );
                                final CorrelatedPeakDetector detector = new CorrelatedPeakDetector(in.detectableIonTypes);
                                detector.detectCorrelatedPeaks(sample, ion);
                                if (ion.getChargeState()>1) {
                                    return null;
                                }
                                AlignedFeatures f = new AlignedFeatures(sample, ion, ion.getRetentionTime());
                                if (features[I]==null) features[I] = f;
                                else features[I] = features[I].merge(f);
                            }
                        }
                        return null;
                    }
                }));
            }
            jobs.forEach(JJob::takeResult);
            jobs.clear();
        }
        // make clusters
        Cluster c = new Cluster(Arrays.stream(features).filter(Objects::nonNull).toArray(AlignedFeatures[]::new), 0d, null, null, new HashSet<>(in.samples));
        in.detectAdductsWithGibbsSampling(c);
        final LCMSPeakInformation[] replace = new LCMSPeakInformation[ms2.length];
        for (int i=0; i < features.length; ++i) {
            AlignedFeatures f = features[i];
            if (f==null) {
                LoggerFactory.getLogger(Ms1Remapping.class).warn("Do not find " + experiments[i].getSourceString() + " with mz = " +
                        mzs[i] + " and retention time = " + retentionTimes[i] + " +-" + allowedDeviations[i]);
                continue;
            }
            final LCMSPeakInformation info =  new LCMSPeakInformation(f.getFeatures().entrySet().stream().map(x->in.getTraceset(x.getKey(),x.getValue())).toArray(CoelutingTraceSet[]::new));
            replace[i] = info;
        }
        return replace;
    }

    private static Scan experiment2Scan(Ms2Experiment e, SimpleSpectrum ms2spec, ChromatographicPeak.Segment apex, long rt) {
        return new Scan(
                -1,
                e.getPrecursorIonType().getCharge()>0 ? Polarity.POSITIVE : Polarity.NEGATIVE,
                rt, CollisionEnergy.none(), ms2spec.size(), Spectrums.calculateTIC(ms2spec), true, new Precursor(-1, e.getIonMass(), apex.getApexIntensity(), e.getPrecursorIonType().getCharge(), null)
        );
    }
    private static CosineQuerySpectrum experiment2querySpectrum(Ms2Experiment e, SimpleSpectrum ms2spec, ChromatographicPeak.Segment apex, long rt) {
        return new CosineQueryUtils(new IntensityWeightedSpectralAlignment(new Deviation(20))).createQuery(ms2spec, e.getIonMass());
    }

    private static void findAverageRetentionTime(int i, LCMSPeakInformation p, long[] retentionTimes, long[] allowedDeviations, double[] mzs) {
        final TDoubleList buf = new TDoubleArrayList();
        final TDoubleArrayList mzbuff = new TDoubleArrayList();

        double maxIntensity = 0d;
        for (int j=0, n = p.length(); j < n; ++j ) {
            maxIntensity = Math.max(maxIntensity, p.getIntensityOf(j));
        }
        final double intensityThreshold = maxIntensity*0.33d;

        for (int j=0, n = p.length(); j < n; ++j ) {
            if (p.getIntensityOf(j)< intensityThreshold) continue;
            Optional<CoelutingTraceSet> traceset = p.getTracesFor(j);
            traceset.ifPresent(coelutingTraceSet -> buf.add(coelutingTraceSet.getRetentionTimes()[coelutingTraceSet.getIonTrace().getMonoisotopicPeak().getAbsoluteIndexApex()]));
            traceset.ifPresent(x->mzbuff.add(x.getIonTrace().getMonoisotopicPeak().getApexMass()));
        }
        // delete outliers
        if (buf.size() >= 4) {
            buf.sort();
            int a = (int)Math.ceil(buf.size()*0.25);
            int b = (int)Math.floor(buf.size()*0.75);
            buf.remove(b, buf.size()-b);
            buf.remove(0, a);
        }
        //
        retentionTimes[i] = (long)(Statistics.expectation(buf.toArray()));
        allowedDeviations[i] = (long)Math.ceil(Math.sqrt(Statistics.variance(buf.toArray())));
        mzs[i] = Statistics.robustAverage(mzbuff.toArray());
    }

    private static void findMs2RetentionTime(int i, LCMSPeakInformation p, long[] retentionTimes, long[] allowedDeviations, double[] mzs) {
        TDoubleArrayList buf = new TDoubleArrayList();
        TDoubleArrayList buf2 = new TDoubleArrayList();
        TDoubleArrayList mzbuff = new TDoubleArrayList();
        for (int j=0, n = p.length(); j < n; ++j ) {
            Optional<CoelutingTraceSet> traceset = p.getTracesFor(j);
            if (traceset.isPresent()) {
                final long rt = traceset.get().getRetentionTimes()[traceset.get().getIonTrace().getMonoisotopicPeak().getAbsoluteIndexApex()];
                if (traceset.get().getMs2RetentionTimes().length>0) {
                    buf.add(rt);
                    mzbuff.add(traceset.get().getIonTrace().getMonoisotopicPeak().getApexMass());
                } else {
                    buf2.add(rt);
                }
            }
        }
        if (buf.isEmpty()) {
            findAverageRetentionTime(i, p, retentionTimes, allowedDeviations, mzs);
            return;
        }
        buf2.addAll(buf);
        retentionTimes[i] = (long)(Statistics.robustAverage(buf.toArray()));
        {
            buf2.transformValues(x->x-retentionTimes[i]);
            buf2.transformValues(x->x*x);
            buf2.sort();
            if (buf2.size()>4) {
                final double[] subl = buf2.toArray((int) Math.ceil(buf2.size() * 0.25), (int) Math.floor(buf2.size() * 0.75));
                allowedDeviations[i] = (long)Math.ceil(Math.sqrt(Arrays.stream(subl).sum() / subl.length));
            } else allowedDeviations[i] = (long)Math.ceil(Math.sqrt(buf2.sum()/buf2.size()));
        }
        mzs[i] = Statistics.robustAverage(mzbuff.toArray());
    }
}
