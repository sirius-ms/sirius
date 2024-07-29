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

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.math3.distribution.LaplaceDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.special.Erf;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Aligner2 {

    private RealDistribution retentionTimeErrorModel;

    public Aligner2(RealDistribution error) {
        this.retentionTimeErrorModel = error;
    }

    //protected double retentionTimeError;
    protected Deviation dev = new Deviation(20);

    protected double minRetentionTimeError = 3000;

    public BasicMasterJJob<Cluster> align(List<ProcessedSample> samples) {
        return new BasicMasterJJob<Cluster>(JJob.JobType.SCHEDULER) {
            @Override
            protected Cluster compute() throws Exception {
                final TIntObjectHashMap<TIntArrayList> mass2msms = new TIntObjectHashMap<TIntArrayList>();
                final ArrayList<ProcessedSample> xs = new ArrayList<>(samples);
                xs.sort(Comparator.comparingInt((ProcessedSample u) -> u.ions.size()).reversed());
                AlignedFeatures[] features = init(xs.get(0));
                xs.remove(0);
                final ArrayList<BasicJJob<AlignmentResult>> subjobs = new ArrayList<>();
                double totalScore = 0d;
                while (xs.size()>0) {
                    mass2msms.clear();
                    subjobs.clear();
                    addFeatures(mass2msms,features);
                    for (int j=0; j < xs.size(); ++j) {
                        int J = j;
                        final AlignedFeatures[] F = features;
                        subjobs.add(submitSubJob(new BasicJJob<AlignmentResult>() {
                            @Override
                            protected AlignmentResult compute() throws Exception {
                                return computeAlignment(J, F, xs.get(J), mass2msms);
                            }
                        }));
                    }
                    final AlignmentResult results = subjobs.stream().map(JJob::takeResult).max(AlignmentResult::compareTo).get();
                    features = merge(features, results);
                    xs.remove(results.index);
                    totalScore += results.score;
                }
                //features = rejoin(this, features);
                return new Cluster(features, totalScore, null,null,new HashSet<>(samples));
            }
        };
    }


    private AlignedFeatures[] merge(AlignedFeatures[] features, AlignmentResult results) {
        final HashSet<FragmentedIon> alreadyJoinedFragmentsDEBUG = new HashSet<>();

        final ArrayList<AlignedFeatures> newFeatures = new ArrayList<>();
        final List<FragmentedIon> ions = results.ions;
        int[] as = results.assignments;
        for (int i = 0; i < as.length; ++i) {
            final int j = as[i];
            if (j>=0) {
                newFeatures.add(features[i].merge(results.mergedSample, ions.get(j)));
                for (FragmentedIon I : newFeatures.get(newFeatures.size()-1).getFeatures().values()) {
                    if (!alreadyJoinedFragmentsDEBUG.add(I)) {
                        throw new RuntimeException("Already added fragment " + I);
                    }
                }
                ions.set(j,null);
            } else {
                newFeatures.add(features[i]); // do not merge
            }
        }
        for (FragmentedIon ion : ions) {
            if (ion!=null) {
                newFeatures.add(new AlignedFeatures(results.mergedSample, ion, results.mergedSample.getRecalibratedRT(ion.getRetentionTime())));
            }
        }
        return newFeatures.toArray(new AlignedFeatures[newFeatures.size()]);

    }

    private void addFeatures(TIntObjectHashMap<TIntArrayList> mass2msms, AlignedFeatures[] features) {
        for (int i=0; i < features.length; ++i) {
            final AlignedFeatures f = features[i];
            int l = (int)Math.floor(f.getMass()*10), h = (int)Math.ceil(f.getMass()*10);
            if (!mass2msms.containsKey(l)) mass2msms.put(l,new TIntArrayList());
            mass2msms.get(l).add(i);
            if (l!=h) {
                if (!mass2msms.containsKey(h)) mass2msms.put(h, new TIntArrayList());
                mass2msms.get(h).add(i);
            }
        }
    }

    protected AlignmentResult computeAlignment(int index, AlignedFeatures[] left, ProcessedSample right, TIntObjectHashMap<TIntArrayList> mass2msms) {
        final TreeSet<Alignment> possibleAlignments = new TreeSet<>();
        final ArrayList<FragmentedIon> allIons = new ArrayList<>(right.ions);
        allIons.addAll(right.gapFilledIons);
        final TIntHashSet candidates = new TIntHashSet();
        for (int i = 0; i < allIons.size(); ++i) {
            final int I = i;
            candidates.clear();
            final FragmentedIon ion = allIons.get(i);
            int l = (int)Math.floor(ion.getMass()*10), h = (int)Math.ceil(ion.getMass()*10);
            TIntArrayList cs = mass2msms.get(l);
            if (cs!=null) cs.forEach(c->{candidates.add(c); return true;});
            cs = mass2msms.get(h);
            if (cs!=null) cs.forEach(c->{candidates.add(c); return true;});
            //////////////////////////
            candidates.forEach(c->{
                final AlignedFeatures f = left[c];
                final float score = getScore(f, right, ion);
                if (score > 0) possibleAlignments.add(new Alignment(c, I, score));
                return true;
            });
        }
        final int[] assignments = new int[left.length];
        Arrays.fill(assignments,-1);
        final BitSet assigned = new BitSet(allIons.size());
        double score = 0d;
        for (Alignment alignment : possibleAlignments.descendingSet()) {
            if (assignments[alignment.i]<0 && !assigned.get(alignment.j)) {
                assignments[alignment.i] = alignment.j;
                assigned.set(alignment.j);
                score += alignment.score;
            }
        }
        return new AlignmentResult(index, right, allIons, assignments, score);
    }

    private float getScore(AlignedFeatures f, ProcessedSample s, FragmentedIon ion) {
        final double rightRt = s.getRecalibratedRT(ion.getRetentionTime());
        if (!dev.inErrorWindow(f.getMass(),ion.getMass()) || !f.chargeStateIsNotDifferent(ion.getChargeState()) || Math.abs(f.rt - rightRt) > maxRetentionError())
            return 0f;

        return (float) getRtScore(f.rt-rightRt);
    }

    private double getRtScore(double diff) {
        return 2*(1d-retentionTimeErrorModel.cumulativeProbability(Math.abs(diff)));
    }

    public double maxRetentionError() {
        if (retentionTimeErrorModel instanceof LaplaceDistribution)
            return 3*((LaplaceDistribution)retentionTimeErrorModel).getScale() < minRetentionTimeError ? minRetentionTimeError :  3*((LaplaceDistribution)retentionTimeErrorModel).getScale();
        else if (retentionTimeErrorModel instanceof NormalDistribution)
            return 3*((NormalDistribution) retentionTimeErrorModel).getStandardDeviation();
        else return Math.sqrt(retentionTimeErrorModel.getNumericalVariance())*3;
    }

    private double scoreIsotopes(FragmentedIon a, FragmentedIon b) {
        SimpleSpectrum sa = a.getIsotopesAsSpectrum();
        SimpleSpectrum sb = b.getIsotopesAsSpectrum();
        final double dev = new Deviation(8).absoluteFor(Math.max(a.getMass(),b.getMass()));
        int n = Math.min(sa.size(),sb.size());
        final double[] scores = new double[n];
        double score = 0d;
        final double sigmaA = 0.02, sigmaR = 0.08;
        double ia = sa.getIntensityAt(0), ib = sb.getIntensityAt(0);
        for (int k=1; k < n; ++k) {
            final double diff = Math.abs((sa.getMzAt(k)-sa.getMzAt(0))-(sb.getMzAt(k)-sb.getMzAt(0)));
            scores[k] += Math.log(Erf.erfc(Math.abs(diff)/(Math.sqrt(2)*dev)));
            final double delta = sa.getIntensityAt(k)-sb.getIntensityAt(k);
            final double mi = Math.max(sa.getIntensityAt(k),sb.getIntensityAt(k));
            scores[k] += Math.log(Math.exp(-(delta*delta)/(2*(sigmaA*sigmaA + mi*mi*sigmaR*sigmaR)))/(2*Math.PI*mi*sigmaR*sigmaR));
            final double sigma = mi*2*sigmaR + 2*sigmaA;
            scores[k] -= Math.log(Math.exp(-(sigma*sigma)/(2*(sigmaA*sigmaA + mi*mi*sigmaR*sigmaR)))/(2*Math.PI*mi*sigmaR*sigmaR));
        }
        double mis = 0d;
        for (int k=n; k < sa.size(); ++k) {
            mis += Math.log(1-sa.getIntensityAt(k));
        }
        for (int k=n; k < sb.size(); ++k) {
            mis += Math.log(1-sb.getIntensityAt(k));
        }
        for (int k=n-1; k >= 0; --k) {
            scores[k] += mis;
            mis += Math.log(1-sa.getIntensityAt(k));
            mis += Math.log(1-sb.getIntensityAt(k));
        }
        return Arrays.stream(scores).max().orElse(1d);
    }

    private AlignedFeatures[] rejoin(BasicMasterJJob<Cluster> basicMasterJJob, AlignedFeatures[] features) {
        // check if two separate features might be one and the same
        final ArrayList<AlignedFeatures> fs = new ArrayList<>(Arrays.asList(features));
        fs.sort(Comparator.comparingInt((AlignedFeatures u) -> u.features.size()).reversed());
        //fs.sort(Comparator.comparingDouble(AlignedFeatures::getMass).thenComparing((AlignedFeatures::getRetentionTime)));
        final List<AlignedFeatures[]> toAlign = new ArrayList<>();
        final ArrayList<AlignedFeatures> dummy = new ArrayList<>();
        final BitSet done = new BitSet(fs.size());
        for (int i=0; i < fs.size(); ++i) {
            final AlignedFeatures f = fs.get(i);
            dummy.clear();
            dummy.add(f);
            for (int j=i+1; j < fs.size(); ++j) {
                AlignedFeatures g = fs.get(j);
                if (!done.get(j) && dev.inErrorWindow(f.getMass(),g.getMass()) && Math.abs(f.rt-g.rt) < 2*maxRetentionError()) {
                    dummy.add(g);
                    done.set(j);
                }
            }
            if (dummy.size()>1) {
                toAlign.add(dummy.toArray(new AlignedFeatures[0]));
                done.set(i);
            }
        }
        for (int k=0; k < fs.size(); ++k) {
            if (done.get(k)) fs.set(k,null);
        }

        final List<BasicJJob<List<AlignedFeatures>>> rejoins = new ArrayList<>();

        for (AlignedFeatures[] xs : toAlign) {
            rejoins.add(basicMasterJJob.submitSubJob(new BasicJJob<List<AlignedFeatures>>() {
                @Override
                protected List<AlignedFeatures> compute() throws Exception {
                    return tryAlign(xs);
                }
            }));
        }
        fs.removeIf(x->x==null);
        rejoins.forEach(e->fs.addAll(e.takeResult()));
        return fs.toArray(new AlignedFeatures[fs.size()]);

    }

    private List<AlignedFeatures> tryAlign(AlignedFeatures[] xs) {
        final ArrayList<AlignedFeatures> features = new ArrayList<>();
        //final double gamma = 1d/(2*retentionTimeError*retentionTimeError);

        final ArrayList<Map.Entry<ProcessedSample, FragmentedIon>> candidates = new ArrayList<>();
        for (AlignedFeatures f : xs) {
            for (Map.Entry<ProcessedSample, FragmentedIon> e : f.features.entrySet()) {
                candidates.add(e);
            }
        }
        double maxIntensity = 0d;
        for (AlignedFeatures f : xs) {
            for (FragmentedIon g : f.features.values()) {
                maxIntensity = Math.max(g.getIntensity(),maxIntensity);
            }
        }

        while (candidates.size()>0) {
            Map.Entry<ProcessedSample,FragmentedIon> bestCandidate = null; double bestScore = Double.NEGATIVE_INFINITY; AlignedFeatures bestAlign = xs[0];

                for (Map.Entry<ProcessedSample, FragmentedIon> e : candidates) {
                    double rt = (e.getKey().getRecalibratedRT(e.getValue().getRetentionTime()));
                    final double rtScore = getRtScore(rt-bestAlign.rt);//Math.exp(-gamma * (rt-bestAlign.rt)*(rt-bestAlign.rt));
                    final double intensityScore = e.getValue().getIntensity()/maxIntensity;
                    double score = rtScore*intensityScore;
                    if (score > bestScore) {
                        bestScore=score;
                        bestCandidate = e;
                    }
                }

            AlignedFeatures initial = new AlignedFeatures(bestCandidate.getKey(),bestCandidate.getValue(), bestCandidate.getKey().getRecalibratedRT(bestCandidate.getValue().getRetentionTime()));
                candidates.remove(bestCandidate);

            while (candidates.size()>0) {
                final TreeSet<Alignment> alignments = new TreeSet<Alignment>();
                for (int i=0; i < candidates.size(); ++i) {
                    if (initial.features.containsKey(candidates.get(i).getKey()))
                        continue;
                    final float score = getScore(initial, candidates.get(i).getKey(), candidates.get(i).getValue());
                    if (score > 0) {
                        alignments.add(new Alignment(0,i,score));
                    }
                }
                if (alignments.isEmpty()) break;
                final Alignment bestCand = alignments.descendingIterator().next();
                initial = initial.merge(candidates.get(bestCand.j).getKey(), candidates.get(bestCand.j).getValue());
                candidates.remove(bestCand.j);
            }
            features.add(initial);
        }
        return features;
    }

    private static class Alignment implements Comparable<Alignment>{
        int i, j;
        float score;

        public Alignment(int i, int j, float score) {
            this.i = i;
            this.j = j;
            this.score = score;
        }

        @Override
        public int compareTo(@NotNull Aligner2.Alignment o) {
            int x=Double.compare(score,o.score);
            if (x==0) x = Integer.compare(i,o.i);
            if (x==0) x = Integer.compare(j,o.j);
            return x;
        }
    }

    private static class AlignmentResult implements Comparable<AlignmentResult> {
        private ArrayList<FragmentedIon> ions;
        private ProcessedSample mergedSample;
        int index;
        private int[] assignments;
        private double score;

        public AlignmentResult(int index, ProcessedSample sample, ArrayList<FragmentedIon> ions, int[] assignments, double score) {
            this.assignments = assignments;
            this.ions = ions;
            this.score = score;
            this.mergedSample = sample;
            this.index = index;
        }

        @Override
        public int compareTo(@NotNull Aligner2.AlignmentResult o) {
            return Double.compare(score,o.score);
        }
    }

    private AlignedFeatures[] init(ProcessedSample sample) {
        final AlignedFeatures[] features = new AlignedFeatures[sample.ions.size()+sample.gapFilledIons.size()];
        for (int k=0; k < sample.ions.size(); ++k) {
            features[k] = new AlignedFeatures(sample,sample.ions.get(k),sample.getRecalibratedRT(sample.ions.get(k).getRetentionTime()));
        }
        for (int k=0; k < sample.gapFilledIons.size(); ++k) {
            features[k+sample.ions.size()] = new AlignedFeatures(sample,sample.gapFilledIons.get(k),sample.getRecalibratedRT(sample.gapFilledIons.get(k).getRetentionTime()));
        }
        return features;
    }

}
