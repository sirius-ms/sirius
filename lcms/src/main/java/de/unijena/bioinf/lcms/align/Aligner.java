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

import de.unijena.bioinf.ChemistryBase.algorithm.HierarchicalClustering;
import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.BasicSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.Ms2CosineSegmenter;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.lcms.quality.AlignmentQuality;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.model.lcms.*;
import de.unijena.bioinf.recal.MzRecalibration;
import de.unijena.bionf.spectral_alignment.CosineQuerySpectrum;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import gnu.trove.iterator.TLongFloatIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongFloatHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TLongHashSet;
import org.apache.commons.math3.analysis.function.Identity;
import org.apache.commons.math3.distribution.LaplaceDistribution;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Aligner {

    private boolean dynamicTimeWarping;
    // if we have aligned more than X samples, we remove rows which do not align well
    private int cutoffForFilterHeuristic;

    public Aligner(boolean dynamicTimeWarping) {
        this.dynamicTimeWarping = dynamicTimeWarping;
        this.cutoffForFilterHeuristic = 50;
    }

    public ConsensusFeature[] makeFeatureTable(LCMSProccessingInstance instance, Cluster cluster) {
        final List<ConsensusFeature> consensusFeatures = new ArrayList<>();
        final AlignedFeatures[] allFeatures = cluster.features.clone();
        Arrays.sort(allFeatures, Comparator.comparingDouble(u -> u.rt));
        // assign feature ID
        final HashMap<AlignedFeatures, ConsensusFeature> mapper = new HashMap<>();
        int featureID = 0;
        for (AlignedFeatures f : allFeatures) {
            if (Math.abs(f.chargeState)>1)
                continue; // ignore multiple charged compounds
            final double mass = f.getMass();
            final TLongArrayList retentionTimes = new TLongArrayList();
            final ArrayList<Feature> features = new ArrayList<>();
            MergedSpectrumWithCollisionEnergies mergedSpectra = new MergedSpectrumWithCollisionEnergies();
            final TIntObjectHashMap<List<SimpleSpectrum>> coeluted = new TIntObjectHashMap<>();
            Feature best = null;
            List<ProcessedSample> samples = new ArrayList<>(f.features.keySet());
            samples.sort((u,v)->{
                final FragmentedIon left = f.features.get(u);
                final FragmentedIon right = f.features.get(v);
                return Double.compare(right.getIntensity(), left.getIntensity());

            });

            // reject chimeric spectra
            double lowestChimeric = Double.POSITIVE_INFINITY;
            double highestScore = 0d;
            double[] tics = new double[samples.size()];
            for (int i=0; i < samples.size(); ++i) {
                final FragmentedIon ion = f.getFeatures().get(samples.get(i));
                if (ion.getMsMs()!=null) {
                    lowestChimeric = Math.min(lowestChimeric, ion.getChimericPollution());
                    tics[i] = ion.getIntensity();
                    highestScore = Math.max(highestScore, tics[i]*Math.sqrt(tics[i])/ion.getChimericPollution());
                }
            }
            final double chimericThreshold = lowestChimeric+0.2d;
            final double scoreThreshold = highestScore*0.75d;
            int tot=0;
            final HashSet<ProcessedSample> rejectedSamples = new HashSet<>();
            for (int i=0; i < samples.size(); ++i) {
                final FragmentedIon ion = f.getFeatures().get(samples.get(i));
                if (ion.getMsMs()!=null) {
                    ++tot;
                    if (ion.getChimericPollution() > chimericThreshold && tics[i]*Math.sqrt(tics[i])/ion.getChimericPollution() < scoreThreshold) {
                        rejectedSamples.add(samples.get(i));
                    }
                }
            }

            if (rejectedSamples.size()>0) {
                LoggerFactory.getLogger(Aligner.class).info("Reject " +rejectedSamples.size() + " of " + tot + " samples for feature " + featureID);
            }

            double totalInt = 0d;
            MergedSpectrumWithCollisionEnergies merged = null;
            final Set<PrecursorIonType> ionTypes = new HashSet<>();
            PrecursorIonType ionType=null;
            double chimericPollution = 0d;

            ProcessedSample representativeSample = null;
            double representativeTIC = Double.NEGATIVE_INFINITY;

            for (ProcessedSample sample : samples) {
                final FragmentedIon ion = f.features.get(sample);
                if (Math.abs(ion.getChargeState())>1)
                    continue; // multiple charged ions are not allowed
                retentionTimes.add(ion.getRetentionTime());

                final Feature e = instance.makeFeature(sample, ion, !ion.isCompound());
                features.add(e);
                totalInt += e.getIntensity();

                final MergedSpectrumWithCollisionEnergies msms;
                if (ion.getMsMsScans()==null || rejectedSamples.contains(sample)) msms=null;
                else msms = new MergedSpectrumWithCollisionEnergies(Arrays.stream(ion.getMsMsScans()).map(x-> new MergedSpectrum(
                        x, instance.getMs2(x), x.getPrecursor(),sample.ms2NoiseModel.getNoiseLevel(x.getIndex(),ion.getMass())
                )).toArray(MergedSpectrum[]::new));

                if (msms!=null) {
                    final double tic = msms.totalTic();
                    if (tic > representativeTIC) {
                        representativeSample = sample;
                    }
                }


                if (merged==null) merged = msms;
                else if (msms!=null) {
                    MergedSpectrumWithCollisionEnergies spec = Ms2CosineSegmenter.merge(merged,msms);
                    boolean goodMatch = false;
                    for (MergedSpectrum s : spec.getSpectra()) {
                        if (s.getMergedCosine() >= 0.66) {
                            goodMatch=true;
                        }
                    }
                    if (goodMatch) {
                        merged = spec;
                    }
                }

                for (SimpleSpectrum coel : e.getCorrelatedFeatures()) {
                    final int mz0 = (int)Math.round(coel.getMzAt(0));
                    if (coeluted.get(mz0)==null) coeluted.put(mz0,new ArrayList<>());
                    coeluted.get(mz0).add(coel);
                }

                if (!e.getIonType().isIonizationUnknown())
                    ionTypes.add(e.getIonType());
                ionType = PrecursorIonType.unknown(e.getIonType().getCharge());

                if (msms!=null) chimericPollution = Math.max(chimericPollution, ion.getChimericPollution());
            }

            final ArrayList<SimpleSpectrum> allMs1Spectra = new ArrayList<>();
            for (int key : coeluted.keys()) {
                final List<SimpleSpectrum> cs = coeluted.get(key);
                int allowedSize = cs.stream().mapToInt(BasicSpectrum::size).max().orElse(1)-1;
                cs.removeIf(x->x.size()<allowedSize);
                if (cs.size()==1) {
                    allMs1Spectra.add(cs.get(0));
                    continue;
                }
                cs.sort(Comparator.comparingInt(BasicSpectrum::size));
                cs.remove(cs.size()-1);
                SimpleSpectrum lastOne = cs.get(cs.size()-1);
                final SimpleMutableSpectrum buf = new SimpleMutableSpectrum(allowedSize+1);
                final TDoubleArrayList intensities = new TDoubleArrayList(allowedSize+1), masses = new TDoubleArrayList(allowedSize+1);
                for (int k=0; k < lastOne.size(); ++k) {
                    masses.clear();
                    intensities.clear();
                    final double mz = lastOne.getMzAt(k);
                    masses.add(mz); intensities.add(lastOne.getIntensityAt(k));
                    for (SimpleSpectrum c : cs) {
                        for (int i=0; i < c.size(); ++i) {
                            if (Math.abs(c.getMzAt(i)-mz)<0.1) {
                                masses.add(c.getMzAt(i));
                                intensities.add(c.getIntensityAt(i));
                            }
                        }
                    }
                    buf.addPeak(Statistics.robustAverage(masses.toArray()), Statistics.robustGeometricAverage(intensities.toArray(),false));
                }
                allMs1Spectra.add(new SimpleSpectrum(buf));
            }


            if (ionTypes.size()==1) {
                ionType = ionTypes.iterator().next();
            }

            if (merged==null)
                continue; // no MS/MS spectrum in row

            retentionTimes.sort();
            final long medianRet = retentionTimes.get(retentionTimes.size()/2);
            merged.getSpectra().sort(Comparator.comparingDouble(x->x.getCollisionEnergy().getMaxEnergy()));
            SimpleMutableSpectrum[] ms2merged = merged.getSpectra().stream().map(x->new SimpleMutableSpectrum(x.finishMerging())).toArray(SimpleMutableSpectrum[]::new);
            // remove isotope peaks from the MS/MS
            for (SimpleMutableSpectrum s : ms2merged) {
                Spectrums.filterIsotopePeaks(s, new Deviation(10), 0.2, 0.55, 3, new ChemicalAlphabet(MolecularFormula.parseOrNull("CHNOPS").elementArray()), true);
            }
            final ConsensusFeature F = new ConsensusFeature(++featureID, features.toArray(new Feature[0]),representativeSample==null ? -1 : samples.indexOf(representativeSample), allMs1Spectra.toArray(SimpleSpectrum[]::new), Arrays.stream(ms2merged).map(x->new SimpleSpectrum(x)).toArray(SimpleSpectrum[]::new), ionType, medianRet, merged.getSpectra().stream().map(x->x.getCollisionEnergy()).toArray(CollisionEnergy[]::new),mass, totalInt,chimericPollution);
            consensusFeatures.add(F);
            mapper.put(f, F);
        }

        // reinsert connections
        for (AlignedFeatures a : allFeatures) {
            ConsensusFeature f = mapper.get(a);
            if (f!=null) {
                for (IonConnection<AlignedFeatures> c : a.connections) {
                    final ConsensusFeature o = mapper.get(c.getRight());
                    if (o!=null) f.addConnection(o, c.getType(), c.getWeight());
                }
            }
        }

        return consensusFeatures.toArray(new ConsensusFeature[0]);
    }

    public LaplaceDistribution estimateErrorLaplace(List<ProcessedSample> samples) {
        // start with the 15% percentile distance of consecutive features with same mz as initial error term
        TDoubleArrayList distances = new TDoubleArrayList();
        final TIntObjectHashMap<ArrayList<FragmentedIon>> greedyAlignment = new TIntObjectHashMap<>();
        final TIntLongHashMap greedyAlignmentAvg = new TIntLongHashMap();
        for (ProcessedSample s : samples) {
            // binned ions
            final TIntObjectHashMap<List<FragmentedIon>> binnedIons = new TIntObjectHashMap<>();
            for (FragmentedIon f : s.ions) {
                int m = (int)Math.round(f.getMass()*100);
                if (!binnedIons.containsKey(m)) binnedIons.put(m, new ArrayList<>());
                binnedIons.get(m).add(f);
            }
            binnedIons.forEachValue(x->{x.sort(Comparator.comparingDouble(FragmentedIon::getRetentionTime)); return true;});
            for (List<FragmentedIon> ionList : binnedIons.valueCollection()) {
                for (int j=1; j < ionList.size(); ++j) {
                    distances.add(ionList.get(j).getRetentionTime()-ionList.get(j-1).getRetentionTime());
                }
            }
            binnedIons.forEachEntry((y,x)->{
                if (!greedyAlignment.containsKey(y)) {
                    greedyAlignment.put(y,new ArrayList<>());
                    final FragmentedIon i = Collections.max(x, Comparator.comparingDouble(FragmentedIon::getIntensity));
                    greedyAlignment.get(y).add(i);
                    greedyAlignmentAvg.put(y, i.getRetentionTime());
                } else {
                    final double avg = greedyAlignmentAvg.get(y) / (double)greedyAlignment.get(y).size();
                    final FragmentedIon i = Collections.min(x, Comparator.comparingDouble(z->Math.abs(avg-z.getRetentionTime())));
                    greedyAlignment.get(y).add(i);
                    greedyAlignmentAvg.put(y, greedyAlignmentAvg.get(y) + i.getRetentionTime());
                }
                return true;
            });
        }
        distances.sort();
        double sigma = 0d;
        distances.transformValues(Math::abs);
        final double std = distances.sum()/distances.size();
        System.err.println("CONSECUTIVE DISTANCES: " + distances.sum()/distances.size());
        distances.clearQuick();
        greedyAlignment.forEachValue(ilist->{
            for (int i=0; i < ilist.size(); ++i) {
                for (int j=i+1; j < ilist.size(); ++j) {
                    double dist = Math.abs(ilist.get(i).getRetentionTime()-ilist.get(j).getRetentionTime());
                    if (dist <= std) {
                        distances.add(dist);
                    }
                }
            }
            return true;
        });
        return new LaplaceDistribution(0d, distances.sum()/distances.size());
    }

    public double estimateErrorTerm(List<ProcessedSample> samples) {
        // start with the 15% percentile distance of consecutive features with same mz as initial error term
        TDoubleArrayList distances = new TDoubleArrayList();
        for (ProcessedSample s : samples) {
            // binned ions
            final TIntObjectHashMap<List<FragmentedIon>> binnedIons = new TIntObjectHashMap<>();
            for (FragmentedIon f : s.ions) {
                int m = (int)Math.round(f.getMass()*100);
                if (!binnedIons.containsKey(m)) binnedIons.put(m, new ArrayList<>());
                binnedIons.get(m).add(f);
            }
            binnedIons.forEachValue(x->{x.sort(Comparator.comparingDouble(y->y.getRetentionTime())); return true;});
            for (List<FragmentedIon> ionList : binnedIons.valueCollection()) {
                for (int j=1; j < ionList.size(); ++j) {
                    distances.add(ionList.get(j).getRetentionTime()-ionList.get(j-1).getRetentionTime());
                }
            }
        }
        distances.sort();
        if (distances.size() <= 10) {
            // just take 10 seconds as error. Dunno anything better yet
            return 10000;
        }
        final double error = distances.getQuick((int)Math.floor(distances.size() * 0.15));
        if (error <= 0) {
            return 10000;
        }
        return error;
    }

    public BasicJJob<Cluster> recalibrateRetentionTimes(List<ProcessedSample> samples, Cluster cluster, double errorTerm) {
        return new BasicMasterJJob<Cluster>(JJob.JobType.CPU){

            @Override
            protected Cluster compute() throws Exception {
                double avgError = 0d;
                TObjectIntHashMap<ProcessedSample> alignmentSize = new TObjectIntHashMap<>();
                int naligns = 0;
                for (AlignedFeatures f : cluster.features) {
                    if (f.features.size()>1  ) {
                        for (Map.Entry<ProcessedSample, FragmentedIon> g : f.features.entrySet()) {
                            alignmentSize.adjustOrPutValue(g.getKey(),1,1);
                            ++naligns;
                            avgError += Math.abs(g.getKey().getRecalibratedRT(g.getValue().getRetentionTime())-f.rt);
                        }
                    }
                }
                final int[] medianAlignedFeatures = alignmentSize.values();
                Arrays.sort(medianAlignedFeatures);
                int medianAlignedFeature = medianAlignedFeatures.length==0 ? 0 : medianAlignedFeatures[medianAlignedFeatures.length/2];
                avgError /= naligns;
                final double averageError = avgError;

                double minimumTime = 0d, maximumTime = Double.NEGATIVE_INFINITY;
                for (ProcessedSample s : samples) {
                    maximumTime = Math.max(maximumTime, s.ions.stream().mapToLong(x->x.getRetentionTime()).max().orElse(1l));
                }
                maximumTime += 10*errorTerm;

                final double MinimumTime = minimumTime; final double MaximumTime = maximumTime;

                for (ProcessedSample s : samples) {
                    submitSubJob(new BasicJJob() {
                        @Override
                        protected Object compute() throws Exception {
                            final SimpleMutableSpectrum buff = new SimpleMutableSpectrum();
                            buff.addPeak(MinimumTime, MinimumTime);
                            buff.addPeak(MaximumTime,MaximumTime);
                            // add anchors

                            for (AlignedFeatures f : cluster.features) {
                                if (f.features.size()>1 && f.features.containsKey(s) && f.features.get(s).getPeakShape().getPeakShapeQuality().betterThan(Quality.DECENT)) {
                                    buff.addPeak(f.features.get(s).getRetentionTime(), f.rt);
                                }
                            }
                            //buff.addPeak(1d,1d);
                            if (buff.size() >= 22) {
                                Spectrums.sortSpectrumByMass(buff);
                                for (int k=1; k < buff.size(); ++k) {
                                    if (buff.getMzAt(k)-buff.getMzAt(k-1) <= 0) {
                                        double intens = buff.getIntensityAt(k);
                                        buff.removePeakAt(k);
                                        --k;
                                        buff.setIntensityAt(k, (buff.getIntensityAt(k)+intens)/2d);

                                    }
                                }
                                final double[] X = Spectrums.copyMasses(buff);
                                final double[] Y = Spectrums.copyIntensities(buff);
                                s.setRecalibrationFunction(MzRecalibration.getMedianLinearRecalibration(X, Y));
                                //s.setRecalibrationFunction(new LoessFunction(new LoessInterpolator().interpolate(X, Y)));
                                System.out.println(s.run.getSource() + " :: " + s.getRecalibrationFunction());
                            } else {
                                System.out.println("Not enough aligned features to recalibrate " + s.run.getSource());
                                s.setRecalibrationFunction(new Identity());
                                s.setAnnotation(AlignmentQuality.class, new AlignmentQuality(buff.size()-2, medianAlignedFeature));
                            }
                            return "";
                        }
                    });
                }
                awaitAllSubJobs();
                // get average error
                avgError = 0d;
                naligns = 0;
                for (AlignedFeatures f : cluster.features) {
                    if (f.features.size()>1) {
                        for (Map.Entry<ProcessedSample, FragmentedIon> g : f.features.entrySet()) {
                            ++naligns;
                            avgError += Math.abs(g.getKey().getRecalibratedRT(g.getValue().getRetentionTime())-f.rt);
                        }
                    }
                }
                avgError /= naligns;
                return cluster;
            }
        };

    }

    public Cluster upgma(List<ProcessedSample> samples, double errorTerm, boolean useAllFeatures) {
        return new UPGMA(errorTerm, useAllFeatures).cluster(samples);
    }

    public BasicJJob<Cluster> upgmaInParallel(List<ProcessedSample> samples, double errorTerm, boolean useAllFeatures) {
        return new UPGMA(errorTerm, useAllFeatures).makeParallelClusterJobs(samples);
    }


    private class UPGMA extends HierarchicalClustering<ProcessedSample, Cluster, Cluster> {
        private final double errorTerm;
        private final boolean useAllFeatures;
        private boolean firstTime = true;
        public UPGMA(double errorTerm, boolean useAllFeatures) {
            this.errorTerm = errorTerm;
            this.useAllFeatures = useAllFeatures;
        }

        @Override
        public Cluster createLeaf(ProcessedSample entry) {
            return new Cluster(entry, useAllFeatures);
        }

        @Override
        public Cluster merge(Cluster cluster, Cluster left, Cluster right, double score) {
            return cluster;
        }

        @Override
        public Cluster preMerge(Cluster left, Cluster right) {
            return align(left,right, errorTerm, useAllFeatures, true);
        }

        @Override
        public double getScore(Cluster preMerged, Cluster left, Cluster right) {
            return preMerged.score;
        }
    }

    public static boolean IS_REALIGN = false;
    public BasicJJob<Cluster> makeRealignJob(Cluster cluster, double errorTerm) {
        return new BasicMasterJJob<Cluster>(JJob.JobType.CPU) {
            @Override
            protected Cluster compute() throws Exception {
                Cluster left = cluster.left;
                Cluster right = cluster.right;
                if (left == null || right == null)  {
                    // cluster is a leaf
                    return new Cluster(cluster.mergedSamples.iterator().next(), true);
                } else {
                    final BasicJJob<Cluster> L = makeRealignJob(left,errorTerm);
                    final BasicJJob<Cluster> R = makeRealignJob(right,errorTerm);
                    submitSubJob(L);
                    submitSubJob(R);
                    // cluster is an inner node
                    return align(L.takeResult(), R.takeResult(), errorTerm, true, true);
                }
            }
        };
    }

    public Cluster realign(Cluster cluster, double errorTerm) {
        Cluster left = cluster.left;
        Cluster right = cluster.right;
        if (left == null || right == null)  {
            // cluster is a leaf
            return new Cluster(cluster.mergedSamples.iterator().next(), true);
        } else {
            final Cluster L = realign(left,errorTerm);
            final Cluster R = realign(right,errorTerm);
            if (IS_REALIGN) {
                System.out.println("##############################");
                System.out.println("ALIGN " + left.mergedSamples + " WITH " + right.mergedSamples);
                System.out.println("##############################");
            }
            // cluster is an inner node
            return align(L, R, errorTerm, true, true);
        }
    }

    public Cluster align(Cluster leftNode, Cluster rightNode, double errorTerm, boolean useAll, boolean addUnaligned) {
        final HashSet<AlignedFeatures> allFeatures = new HashSet<>();
        if (addUnaligned) {
            allFeatures.addAll(Arrays.asList(leftNode.features));
            allFeatures.addAll(Arrays.asList(rightNode.features));
        }
        // first make a pool of m/z values we want to align
        final TLongHashSet mzLeft = new TLongHashSet(), mzRight = new TLongHashSet();
        for (AlignedFeatures l : leftNode.features) {
            final double mass = l.getMass();
            final long roundedDown = (long)Math.floor(mass*20);
            final long roundedUp = (long)Math.ceil(mass*20);
            mzLeft.add(roundedDown); mzLeft.add(roundedUp);
        }

        for (AlignedFeatures r : rightNode.features) {
            final double mass = r.getMass();
            final long roundedDown = (long)Math.floor(mass*20);
            final long roundedUp = (long)Math.ceil(mass*20);
            mzRight.add(roundedDown); mzRight.add(roundedUp);
        }

        mzLeft.retainAll(mzRight);

        final List<AlignedFeatures> ionsLeft = new ArrayList<>();
        for (AlignedFeatures l : leftNode.features) {
            final double mass = l.mass;
            final long roundedDown = (long)Math.floor(mass*20);
            final long roundedUp = (long)Math.ceil(mass*20);
            if (mzLeft.contains(roundedDown) || mzLeft.contains(roundedUp))
                ionsLeft.add(l);
        }

        final List<AlignedFeatures> ionsRight = new ArrayList<>();
        for (AlignedFeatures l : rightNode.features) {
            final double mass = l.getMass();
            final long roundedDown = (long)Math.floor(mass*20);
            final long roundedUp = (long)Math.ceil(mass*20);
            if (mzLeft.contains(roundedDown) || mzLeft.contains(roundedUp))
                ionsRight.add(l);
        }
        if (dynamicTimeWarping) {
            return alignMatchingListDynamicTimeWarping(leftNode,rightNode,ionsLeft,ionsRight, allFeatures, (float)errorTerm, useAll, !addUnaligned);
        } else {
            return alignMatchingListBipartite(leftNode,rightNode,ionsLeft,ionsRight, allFeatures, (float)errorTerm, useAll, !addUnaligned);
        }
    }

    protected Cluster alignMatchingListBipartite(Cluster left, Cluster right, List<AlignedFeatures> leftFeatures, List<AlignedFeatures> rightFeatures, Set<AlignedFeatures> unaligned, float errorTerm, boolean useAll, boolean keepIntermediates) {
        final SparseScoreMatrix scores = new SparseScoreMatrix(Float.NEGATIVE_INFINITY);
        computePairwiseCosine(scores, leftFeatures, rightFeatures, errorTerm, useAll);
        final TreeSet<ScoredAligned> set = new TreeSet<>();
        for (int i=0; i  < leftFeatures.size(); ++i) {
            for (int j=0; j < rightFeatures.size(); ++j) {
                final float score = scores.lookup(i,j);
                if (score > 0) set.add(new ScoredAligned(i,j,score));
            }
        }
        double totalScore = 0d;
        final BitSet alignedLeft = new BitSet(leftFeatures.size()), alignedRight = new BitSet(rightFeatures.size());
        final List<AlignedFeatures> alignedFeatures =new ArrayList<>(leftFeatures.size());
        for (ScoredAligned a : set.descendingSet()) {
            if (!alignedLeft.get(a.i) && !alignedRight.get(a.j)) {
                final AlignedFeatures l = leftFeatures.get(a.i);
                final AlignedFeatures r = rightFeatures.get(a.j);
                unaligned.remove(l); unaligned.remove(r);
                alignedFeatures.add(l.merge(r));
                totalScore += a.score;
                alignedLeft.set(a.i);
                alignedRight.set(a.j);
            }
        }
        //System.out.println("Average score = " + (alignedFeatures.isEmpty() ? 0.0 : (totalScore / alignedFeatures.size())) + " for " + left.mergedSamples.toString() + " WITH " + right.mergedSamples.toString());
        alignedFeatures.addAll(unaligned);
        return new Cluster(alignedFeatures.toArray(new AlignedFeatures[0]), totalScore, left,right, keepIntermediates);

    }

    private static class ScoredAligned implements Comparable<ScoredAligned>{
        private int i,j; private float score;

        public ScoredAligned(int i, int j, float score) {
            this.i = i;
            this.j = j;
            this.score = score;
        }

        @Override
        public int compareTo(@NotNull Aligner.ScoredAligned o) {
            int c = Float.compare(score,o.score);
            if (c!=0) return c;
            c = Integer.compare(i,o.i);
            if (c!=0) return c;
            return Integer.compare(j,o.j);
        }
    }

    protected Cluster alignMatchingListDynamicTimeWarping(Cluster left, Cluster right, List<AlignedFeatures> leftFeatures, List<AlignedFeatures> rightFeatures, Set<AlignedFeatures> unaligned, float errorTerm, boolean useAll, boolean keepIntermediates) {
        final double gamma = 1/(2d*errorTerm*errorTerm);
        final SparseScoreMatrix scores = new SparseScoreMatrix(Float.NEGATIVE_INFINITY);
        computePairwiseCosine(scores, leftFeatures, rightFeatures, errorTerm, useAll);
        final float[][] D = new float[leftFeatures.size()][rightFeatures.size()];
        //final SparseScoreMatrix D = new SparseScoreMatrix(0f);
        for (int i=0; i < leftFeatures.size(); ++i) {
            for (int j=0; j < rightFeatures.size(); ++j) {
                float gapLeft = (i<=0) ? 0 : D[i-1][j];
                float gapRight = (j<=0) ? 0 : D[i][j-1];
                final double diffrt = (leftFeatures.get(i).rt - (i>0 ? leftFeatures.get(i-1).rt : 0)) - (rightFeatures.get(j).rt - (j>0 ? rightFeatures.get(j-1).rt : 0));
                //double retentionTimeScore = Math.exp(-gamma * (diffrt*diffrt));
                D[i][j] = Math.max(
                        Math.max(gapLeft,gapRight), // gapleft or gapRight
                        (i>0&&j>0 ? D[i-1][j-1] : 0) + Math.max(0f,scores.lookup(i,j))// * (float)retentionTimeScore// align
                );
            }
        }
        final ArrayList<AlignedFeatures> backtracked = new ArrayList<>();
        float score = backtrack(D, leftFeatures, rightFeatures, backtracked, unaligned);
        backtracked.addAll(unaligned);
        return new Cluster(backtracked.toArray(new AlignedFeatures[0]), score, left, right,  keepIntermediates);
    }

    protected float backtrack(float[][] scores, List<AlignedFeatures> left, List<AlignedFeatures> right, List<AlignedFeatures> backtracked, Set<AlignedFeatures> unaligned) {
        int maxI=0,maxJ=0;
        float max=0f;
        for (int i=0; i  <scores.length; ++i) {
            for (int j=0; j < scores[0].length; ++j){
                if (scores[i][j] > max) {
                    max = scores[i][j];
                    maxI =i;
                    maxJ = j;
                }
            }
        }
        if (max <= 0) {
            return 0;
        }
        backtrackFrom(scores, maxI, maxJ, left, right, backtracked, unaligned);
        return max;
    }

    private void backtrackFrom(float[][] D, int i, int j, List<AlignedFeatures> left, List<AlignedFeatures> right, List<AlignedFeatures> aligned, Set<AlignedFeatures> unaligned) {
        while (i > 0 && j > 0) {
            float gapLeft = D[i-1][j];
            float gapRight = D[i][j-1];
            float score = D[i][j];
            if (score > gapLeft && score > gapRight) {
                aligned.add(left.get(i).merge(right.get(j)));
                boolean done = unaligned.remove(left.get(i));
                assert done;
                done = unaligned.remove(right.get(j));
                i = i-1;
                j = j-1;
                assert done;
            } else if (score == gapLeft) {
                --i;
            } else {
                --j;
            }
        }
        Collections.reverse(aligned);
    }

    private void computePairwiseCosine2(SparseScoreMatrix scores, List<AlignedFeatures> left, List<AlignedFeatures> right, float errorTerm, boolean useAll ) {
        final Deviation dev = new Deviation(30);
        final double gamma = 1d / (2d * errorTerm * errorTerm);
        for (int i = 0; i < left.size(); ++i) {
            final AlignedFeatures l = left.get(i);
            for (int j = 0; j < right.size(); ++j) {
                final AlignedFeatures r = right.get(j);
                if (dev.inErrorWindow(l.getMass(), r.getMass()) && Math.abs(l.rt - r.rt) < 5 * errorTerm) {
                    final float value = (float)Math.exp(-gamma * (l.rt-r.rt)*(l.rt-r.rt));
                    scores.add(i,j, value);
                    assert Double.isFinite(scores.lookup(i,j)) && Math.abs(scores.lookup(i,j)-value) < 1e-3;
                }
            }
        }
    }

    private void computePairwiseCosine(SparseScoreMatrix scores, List<AlignedFeatures> left, List<AlignedFeatures> right, float errorTerm, boolean useAll) {
        final double SCORE_THRESHOLD = 1e-8;
        final Deviation dev = new Deviation(20);

        final CosineQueryUtils utils = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(dev));
        final List<CosineQuerySpectrum> ll = new ArrayList<>(), rr = new ArrayList<>();
        for (AlignedFeatures l : left) {
            if (l.representativeFeature==null || l.getRepresentativeIon().getMsMsQuality().notBetterThan(Quality.BAD) || l.getRepresentativeIon().getMsMs().size() < 5) {
                ll.add(null);
            } else ll.add(l.getRepresentativeIon().getMsMs());
        }
        for (AlignedFeatures r : right) {
            if (r.representativeFeature==null || r.getRepresentativeIon().getMsMsQuality().notBetterThan(Quality.BAD) || r.getRepresentativeIon().getMsMs().size() < 5) {
                rr.add(null);
            } else rr.add(r.getRepresentativeIon().getMsMs());
        }
        for (int i=0; i < left.size(); ++i) {
            final AlignedFeatures l = left.get(i);
            for (int j=0; j < right.size(); ++j) {
                final AlignedFeatures r = right.get(j);
                if (dev.inErrorWindow(l.getMass(), r.getMass()) && Math.abs(l.rt-r.rt) < 4*errorTerm && l.chargeStateIsNotDifferent(r)) {
                    double error = errorTerm*0.66;
                    final double gamma = 1d / (2d * (error*error + (l.rtVariance+r.rtVariance)/2d));
                    float peakShapeScore = 0f;
                    int n = 0;
                    for (FragmentedIon ia : l.features.values()) {
                        for (FragmentedIon ib : r.features.values()) {
                            peakShapeScore += ia.comparePeakWidthSmallToLarge(ib);
                            ++n;
                        }
                    }
                    peakShapeScore /= n;
                    if (peakShapeScore >= 1) {
                        peakShapeScore = (float)new de.unijena.bioinf.ChemistryBase.math.NormalDistribution(1d, 0.25).getErrorProbability(peakShapeScore);
                    } else peakShapeScore = 1f;

                    // peak height score
                    double peakHeightScore = 0d;
                    {
                        double h = Math.log(l.peakHeight / r.peakHeight);
                        h*=h;
                        double w = Math.log(l.peakWidth / r.peakWidth);
                        w *= w;
                        peakHeightScore = Math.max(0.05, Math.exp(-1.5*h*w));
                        //System.out.println(peakHeightScore + " <- " + l.peakHeight + " vs " + r.peakHeight + ",\t " + l.peakWidth +" vs" + r.peakWidth);
                    }
                    peakShapeScore *= peakHeightScore;

                    if (ll.get(i)!=null &&  rr.get(j)!=null) {
                        SpectralSimilarity spectralSimilarity = utils.cosineProduct(ll.get(i), rr.get(j));
                        if ((spectralSimilarity.similarity < 0.5 || spectralSimilarity.sharedPeaks < 3)) {
                            // prefer to not align features with low cosine
                            if (l.getRepresentativeIon().getMsMsQuality().betterThan(Quality.DECENT) || r.getRepresentativeIon().getMsMsQuality().betterThan(Quality.DECENT)) {
                                //System.out.println(l + " with " + r + " are rejected due to COSINE of " + spectralSimilarity);
                                // do not align both scans if they are good quality
                            } else {
                                float value = peakShapeScore * (float)( Math.exp(-2*gamma*((l.rt-r.rt)*(l.rt-r.rt))) * 0.25d );
                                if (value >= SCORE_THRESHOLD)
                                    scores.add(i,j,value);
                            }
                        } else {
                            float value = peakShapeScore *  (float)((spectralSimilarity.similarity + spectralSimilarity.sharedPeaks /10d) * Math.exp(-gamma*((l.rt-r.rt)*(l.rt-r.rt))));
                            //System.err.println(spectralSimilarity.similarity + " cosine, " + spectralSimilarity.sharedPeaks + " peaks for " + (l.rt/60000d) + " vs " + (r.rt/60000d) + ", and " + l.mass + " vs " + r.mass + ", rt score = " +  Math.exp(-gamma*((l.rt-r.rt)*(l.rt-r.rt))) + ", final score = " + value);

                            if (value >= SCORE_THRESHOLD) {
                                scores.add(i,j,value);
                            }
                        }
                    } else if (useAll) {
                        float value = peakShapeScore * (float)( Math.exp(-gamma*((l.rt-r.rt)*(l.rt-r.rt))) * 0.25d );
                        if (value >= SCORE_THRESHOLD) {
                            scores.add(i,j,value);
                        }
                    }
                }
            }
        }
    }

    private static class SparseScoreMatrix {

        private final TLongFloatHashMap map;

        private SparseScoreMatrix(float defaultValue) {
            this.map = new TLongFloatHashMap(100, 0.75f, -1, defaultValue);
        }

        private long maxKey() {
            TLongFloatIterator iter = map.iterator();
            long maxkey = -1;
            float maxValue = Float.NEGATIVE_INFINITY;
            while (iter.hasNext()) {
                iter.advance();
                if (iter.value() > maxValue) {
                    maxValue = iter.value();
                    maxkey = iter.key();
                }
            }
            return maxkey;
        }

        private void addIfPositive(int i, int j, float value) {
            if (value <= 0) return;
            long key = i;
            key <<= 32;
            key |= j;
            map.put(key, value);
        }

        private void add(int i, int j, float value) {
            long key = i;
            key <<= 32;
            key |= j;
            map.put(key, value);
        }

        private float lookup(int i, int j) {
            long key = i;
            key <<= 32;
            key |= j;
            return map.get(key);
        }

    }

    public int prealignAndFeatureCutoff(List<ProcessedSample> samples, double errorTerm, int threshold) {
        final BasicMasterJJob<Cluster> upgma2BasicJJob = new UPGMA2(errorTerm,false).makeParallelClusterJobs(samples);
        final Cluster cluster = SiriusJobs.getGlobalJobManager().submitJob(upgma2BasicJJob).takeResult();
        final TObjectIntHashMap<FragmentedIon> counter = new TObjectIntHashMap<>();
        count(cluster,counter);
        // now remove all features that belong to less than threshold samples
        int before=0,after=0;
        for (ProcessedSample sample : samples) {
            before += sample.ions.size();
            sample.ions.removeIf(ion->counter.get(ion) < threshold);
            after += sample.ions.size();
        }
        return before-after;
    }
    public BasicMasterJJob<Integer> prealignAndFeatureCutoff2(List<ProcessedSample> samples,double rtCutoff, int threshold) {

        final TIntObjectHashMap<List<FragmentedIon>>[] mass2msms = new TIntObjectHashMap[samples.size()];
        for (int k=0; k < samples.size(); ++k) {
            TIntObjectHashMap<List<FragmentedIon>> mass2msm = new TIntObjectHashMap<List<FragmentedIon>>();
            mass2msms[k] = mass2msm;
            for (FragmentedIon f : samples.get(k).ions) {
                final int low = (int)Math.floor(f.getMass()*10), high = (int)Math.ceil(f.getMass()*10);
                if (!mass2msm.containsKey(low)) mass2msm.put(low,new ArrayList<>());
                mass2msm.get(low).add(f);
                if (high!=low) {
                    if (!mass2msm.containsKey(high)) mass2msm.put(high, new ArrayList<>());
                    mass2msm.get(high).add(f);
                }
            }
        }

        return new BasicMasterJJob<Integer>(JJob.JobType.SCHEDULER) {
            @Override
            protected Integer compute() throws Exception {
                for (int i=0; i < samples.size(); ++i) {
                    final int I = i;
                    final ProcessedSample S = samples.get(i);
                    submitSubJob(new BasicJJob<Object>() {
                        @Override
                        protected Object compute() throws Exception {
                            final CosineQueryUtils utils = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(new Deviation(15)));
                            final HashSet<FragmentedIon> set = new HashSet<>();
                            for (int j=0; j < S.ions.size(); ++j) {
                                FragmentedIon f = S.ions.get(j);
                                final int low = (int)Math.floor(f.getMass()*10);
                                final int high = (int)Math.ceil(f.getMass()*10);
                                for (int k=I+1; k < samples.size(); ++k) {
                                    set.clear();
                                    List<FragmentedIon> c = mass2msms[k].get(low);
                                    if (c!=null) set.addAll(c);
                                    c = mass2msms[k].get(high);
                                    if (c!=null) set.addAll(c);
                                    for (FragmentedIon g : set) {
                                        if (new Deviation(15).inErrorWindow(f.getMass(), g.getMass())) {
                                            final SpectralSimilarity cosineScore = utils.cosineProduct(f.getMsMs(), g.getMsMs());
                                            final double rtDiff = Math.abs(f.getRetentionTime() - g.getRetentionTime());
                                            if (rtDiff <= rtCutoff && cosineScore.similarity >= 0.5 && cosineScore.sharedPeaks >= 3) {
                                                f.incrementAlignments();
                                                g.incrementAlignments();
                                            }
                                        }

                                    }
                                }
                            }
                            return "";
                        }
                    });
                }
                awaitAllSubJobs();
                // now remove all features that belong to less than threshold samples
                int before=0,after=0;
                for (ProcessedSample sample : samples) {
                    before += sample.ions.size();
                    sample.ions.removeIf(ion->ion.alignmentCount() < threshold && ion.getMsMsQuality().notBetterThan(Quality.DECENT));
                    after += sample.ions.size();
                }
                return before-after;
            }
        };
    }

    private void count(Cluster cluster, TObjectIntHashMap<FragmentedIon> counter) {
        for (AlignedFeatures f : cluster.getFeatures()) {
            for (FragmentedIon i : f.getFeatures().values())
                counter.adjustOrPutValue(i,1,1);
        }
        if (!cluster.left.isLeaf()) count(cluster.left,counter);
        if (!cluster.right.isLeaf()) count(cluster.right,counter);
    }


    private class UPGMA2 extends HierarchicalClustering<ProcessedSample, Cluster, Cluster> {
        private final double errorTerm;
        private final boolean useAllFeatures;
        private boolean firstTime = true;
        public UPGMA2(double errorTerm, boolean useAllFeatures) {
            this.errorTerm = errorTerm;
            this.useAllFeatures = useAllFeatures;
        }

        @Override
        public Cluster createLeaf(ProcessedSample entry) {
            return new Cluster(entry, useAllFeatures);
        }

        @Override
        public Cluster merge(Cluster cluster, Cluster left, Cluster right, double score) {
            return cluster;
        }

        @Override
        public Cluster preMerge(Cluster left, Cluster right) {
            return align(left,right, errorTerm, useAllFeatures, false);
        }

        @Override
        public double getScore(Cluster preMerged, Cluster left, Cluster right) {
            return preMerged.score;
        }
    }


}
