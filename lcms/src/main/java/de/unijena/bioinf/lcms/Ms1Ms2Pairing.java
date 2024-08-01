package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.MassMap;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.model.lcms.*;
import de.unijena.bioinf.recal.MzRecalibration;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class maps MS/MS compounds to an MS1 run.
 * Use this procedure when you have MS1 scans without MS/MS and
 * MS/MS scans without or with low quality MS1
 *
 * TODO: implement prevent merging collision energies!!!
 *
 */
public class Ms1Ms2Pairing {

    public static boolean RECALIBRATE = false;

    private final ProcessedSample ms1;
    private final ProcessedSample[] msmsRuns;

    private IsolationWindow isolationWindow = null;

    public Ms1Ms2Pairing(ProcessedSample ms1, ProcessedSample... msmsRuns) {
        this.ms1 = ms1;
        ms1.setMs2NoiseModel(msmsRuns[0].ms2NoiseModel, msmsRuns[0].ms2NoiseInformation);
        this.msmsRuns = msmsRuns;
    }

    // returns the error distribution of alignments
    public RealDistribution attachRemainingMs1(LCMSProccessingInstance instance, ProcessedSample... ms1runs) {
        TDoubleArrayList errors = new TDoubleArrayList();
        final ArrayList<Target> allTargets = new ArrayList<>();
        final JobManager jm = SiriusJobs.getGlobalJobManager();
        for (FragmentedIon ion : ms1.ions) {
            allTargets.add(new Target(ion));
        }
        final ArrayList<BasicJJob<List<Target>>> jobs = new ArrayList<>();
        for (final ProcessedSample s : ms1runs) {
            jobs.add(jm.submitJob(new BasicJJob<>(){
                @Override
                protected List<Target> compute() throws Exception {
                    List<Target> tgs = allTargets.stream().map(x->new Target(x.ion)).collect(Collectors.toList());
                    searchMs1(s, tgs, instance);
                    tgs.sort(Comparator.comparingLong(x->x.rt));
                    recalibrate(tgs, (values)->MzRecalibration.getMedianLinearRecalibration(values[0], values[1]), s.run.getIdentifier() + "_linear");
                    searchMs1(s, tgs, instance);
                    /*
                    recalibrate(tgs, (values)->new LoessInterpolator(0.3, 2).interpolate(values[0], values[1]), s.run.getIdentifier() + "loess");
                    // and search again
                    searchMs1(s, tgs);
                     */
                    return tgs;
                }
            }));
        }
        for (int k=0; k < jobs.size(); ++k) {
            final List<Target> tgs = jobs.get(k).takeResult();
            // add errors
            final double[] error = tgs.stream().filter(x->x.correspondingFeature!=null).mapToDouble(x->(double)Math.max(Math.abs(x.rt-x.correspondingFeature.getApexRt()), x.correspondingFeature.fwhm())).filter(x->x>0).toArray();
            Arrays.sort(error);
            errors.add(Statistics.robustAverage(error));
            final ProcessedSample s = ms1runs[k];
            for (Target t : tgs) {
                if (t.correspondingFeature!=null) {
                    FragmentedIon i = t.ion;
                    s.gapFilledIons.add(new GapFilledIon(
                            Polarity.fromCharge(i.getPolarity()),
                            t.correspondingFeature.getPeak().mutate(),
                            t.correspondingFeature,
                            i
                    ));
                }
            }
        }
        return new NormalDistribution(0, Statistics.robustAverage(errors.toArray()));
    }

    public void run(LCMSProccessingInstance instance) {
        final MassMap<Target> allTargets = new MassMap<>(100);
        final PriorityQueue<Target> targetQueue = new PriorityQueue<>(Comparator.comparingDouble(x->-x.spectrum.totalTic()));
        // first we extract all m/z retention time pairs from MS/MS
        for (ProcessedSample msms : msmsRuns) {
            if (isolationWindow==null)
                isolationWindow = msms.learnIsolationWindow(instance, msms);
            final List<Target> targets = extractTargets(msms);
            // now search for the targets in the MS1 run
            searchMs1(ms1, targets, instance);
            checkForDuplicates(targets);
            recalibrate(targets, (values)->MzRecalibration.getMedianLinearRecalibration(values[0], values[1]), msms.run.getIdentifier() + "_linear");
            searchMs1(ms1, targets, instance);
            //checkForDuplicates(targets);
            //recalibrate(targets, (values)->new LoessInterpolator(0.3, 2).interpolate(values[0], values[1]),msms.run.getIdentifier() + "_loess");
            // and search again
            //searchMs1(ms1, targets);
            checkForDuplicates(targets);
            //
            for (Target t : targets) {
                if (t.correspondingFeature!=null) {
                    allTargets.put(t.mz, t);
                    targetQueue.add(t);
                }
            }
        }
        final ArrayList<Target> finalList = new ArrayList<>();
        // now merge MS/MS from different MSMS pools
        for (Target t : targetQueue) {
            if (!t.merged) {
                final List<Target> similarTargets = allTargets.retrieveAll(t.mz, new Deviation(10));
                similarTargets.removeIf(x-> x==t || !x.correspondingFeature.equals(t.correspondingFeature));
                if (similarTargets.size()>1) {
                    for (Target s : similarTargets) {
                        t.spectrum = Ms2CosineSegmenter.merge(t.spectrum, s.spectrum);
                        s.merged=true;
                    }
                }
                finalList.add(t);
            }
        }
        update(instance, ms1, finalList);
        instance.getSamples().removeAll(Arrays.asList(msmsRuns));
    }

    private void checkForDuplicates(List<Target> targets) {
        final HashMap<ChromatographicPeak.Segment, Target> mapping = new HashMap<>();
        final Iterator<Target> iter = targets.iterator();
        while (iter.hasNext()) {
            final Target t = iter.next();
            if (t.correspondingFeature!=null) {
                if (mapping.containsKey(t.correspondingFeature)) {
                    final Target s = mapping.get(t.correspondingFeature);
                    double cosine = 0d;
                    int n=0;
                    for (Ms2CosineSegmenter.CosineQuery a : t.scans) {
                        for (Ms2CosineSegmenter.CosineQuery b : s.scans) {
                            cosine += a.cosine(b).similarity;
                            ++n;
                        }
                    }
                    cosine /= n;
                    if (cosine >= 0.5) {
                        // merge
                        iter.remove();
                        final int k = s.scans.length;
                        s.scans = Arrays.copyOf(s.scans, k +t.scans.length);
                        System.arraycopy(t.scans,0, s.scans,k, t.scans.length);

                    } else {
                        long leftRt = Math.min(s.rtOrig, t.rtOrig);
                        long rightRt = Math.max(s.rtOrig, t.rtOrig);
                        int leftIndex = s.correspondingFeature.getPeak().findClosestIndexByRt(leftRt);
                        int rightIndex = s.correspondingFeature.getPeak().findClosestIndexByRt(rightRt);
                        final Optional<ChromatographicPeak.Segment[]> segments = s.correspondingFeature.getPeak().mutate().tryToDivideSegment(s.correspondingFeature, leftIndex, rightIndex);
                        if (segments.isEmpty()) {
                            LoggerFactory.getLogger(Ms1Ms2Pairing.class).warn("Two MS/MS spectra at the same peak with low cosine at " + s );
                            t.correspondingFeature = null;
                        } else {
                            if (s.rtOrig<t.rtOrig) {
                                s.correspondingFeature = segments.get()[0];
                                t.correspondingFeature = segments.get()[1];
                            } else {
                                t.correspondingFeature = segments.get()[0];
                                s.correspondingFeature = segments.get()[1];
                            }
                        }

                    }
                } else {
                    mapping.put(t.correspondingFeature, t);
                }
            }
        }
    }

    private void update(LCMSProccessingInstance instance, ProcessedSample ms1, List<Target> targets) {
        final List<FragmentedIon> ions = new ArrayList<>();
        for (Target t : targets) {
            if (t.correspondingFeature!=null) {
                ions.add(instance.createMs2Ion(ms1, t.spectrum.toCollisionEnergyGroup(), t.correspondingFeature.getPeak().mutate(), t.correspondingFeature));

            }
        }
        instance.detectFeatures(ms1, ions);
    }

    private void recalibrate(List<Target> targets, Function<double[][], UnivariateFunction> recalibrationFunction, String name) {
        final List<Target> tgs = targets.stream().filter(x->x.correspondingFeature!=null).sorted(Comparator.comparingDouble(x->x.rt)).collect(Collectors.toList());
        double[] xs = new double[tgs.size()];
        double[] ys = new double[tgs.size()];
        for (int k=0; k< xs.length; ++k) {
            xs[k] = tgs.get(k).rtOrig;
            if (k>0 && xs[k-1]>=xs[k]) xs[k] = xs[k-1] + 1e-8; // enforce increasing. Is smoothed away anyways
            ys[k] = tgs.get(k).correspondingFeature.getApexRt();
        }
/*
        if (!RECALIBRATE) {
            final File f = new File("/home/kaidu/temp/rec/" + name + ((int) (1024 * Math.random())) + ".csv");
            try (final PrintStream x = new PrintStream(f)) {
                for (int k=0; k < xs.length; ++k) {
                    x.println(xs[k] + "\t" + ys[k]);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            dontRecalibrate(targets);
            return;
        }
*/

        final UnivariateFunction f = recalibrationFunction.apply(new double[][]{xs,ys});
        for (Target t : targets) {
            if (f instanceof PolynomialSplineFunction) {
                if (!(((PolynomialSplineFunction) f).isValidPoint(t.rt)))
                    continue;
            }
            t.rt = Math.round(f.value(t.rtOrig));
        }
    }

    private void dontRecalibrate(List<Target> targets) {

    }


    private void searchMs1(ProcessedSample ms1, List<Target> targets, LCMSProccessingInstance instance) {
        final ArrayList<Scan> scans = new ArrayList<>(ms1.run.getScans());
        Collections.sort(scans, Comparator.comparingLong(Scan::getRetentionTime));
        final long[] rts = scans.stream().mapToLong(Scan::getRetentionTime).toArray();

        final long maxRtDistance = (rts[rts.length-1]-rts[0])/10;
        System.out.println(maxRtDistance);
        final ArrayList<Target> additionalTargets = new ArrayList<>();
        final ArrayList<Target> removeableTargets = new ArrayList<>();
        eachTarget:
        for (Target target : targets) {
            // if an MS/MS feature consists of multiple scans, we check if theese scans might belong to distinct peaks
            if (target.scans.length>1) {
                final List<Target> tgs = target.split();
                List<Optional<ChromatographicPeak>> peaks = tgs.stream().map(x->findTarget(ms1, scans, rts, maxRtDistance, x , instance)).collect(Collectors.toList());
                List<Target> newTargets = mergeMs2AndAssign(tgs, peaks);
                if (newTargets==null) {
                    // no need for merging
                } else {
                    removeableTargets.add(target);
                    additionalTargets.addAll(newTargets);
                }
            } else {
                findTarget(ms1, scans, rts, maxRtDistance, target, instance).ifPresent(target::assignFeature);
            }
        }
        targets.removeAll(removeableTargets);
        targets.addAll(additionalTargets);
    }

    private List<Target> mergeMs2AndAssign(List<Target> tgs, List<Optional<ChromatographicPeak>> peaks) {
        final Ms2CosineSegmenter segmenter = new Ms2CosineSegmenter();
        // first: find all peaks that have an assignment and belong to different peaks. Those are splitted anyways
        final HashMap<ChromatographicPeak.Segment, List<Target>> byPeaks = new HashMap<>();
        final Set<ChromatographicPeak> allPeaks = new HashSet<>();
        for (int k=0; k < tgs.size(); ++k) {
            final Target t = tgs.get(k);
            final Optional<ChromatographicPeak> pk = peaks.get(k);
            if (pk.isPresent()) {
                t.assignFeature(pk.get());
                byPeaks.computeIfAbsent(t.correspondingFeature, (y)->new ArrayList<>()).add(t);
                allPeaks.add(pk.get());
            }
        }
        // check if we can merge two segments
        /*
        for (ChromatographicPeak pk : allPeaks) {
            final List<ChromatographicPeak.Segment> segments = new ArrayList<>(pk.getSegments().values());
            outerLoop:
            while (true) {
                for (int k=0; k < segments.size()-1; ++k) {
                    final ChromatographicPeak.Segment left = segments.get(k);
                    final ChromatographicPeak.Segment right = segments.get(k+1);
                    if (byPeaks.containsKey(left) && byPeaks.containsKey(right) && !areDistinctSegments(left, right)) {
                        ChromatographicPeak.Segment s = pk.mutate().joinSegments(left, right);
                        byPeaks.get(left).forEach(x->x.correspondingFeature = s);
                        byPeaks.get(right).forEach(x->x.correspondingFeature = s);

                        byPeaks.computeIfAbsent(s, y->new ArrayList<>()).addAll(byPeaks.remove(left));
                        byPeaks.computeIfAbsent(s, y->new ArrayList<>()).addAll(byPeaks.remove(right));
                        continue outerLoop;
                    }
                }
                break;
            }
        }
         */
        if (byPeaks.size()==1) {
            return null; // no need for merging!
        }
        // merge MS/MS of merged segments
        final ArrayList<Target> done = new ArrayList<>();
        for (ChromatographicPeak.Segment s : byPeaks.keySet()) {
            final List<Target> xs = byPeaks.get(s);
            if (xs.size()==1) {
                done.add(new Target(s.getApexMass(),s.getApexRt(),xs.get(0).spectrum));
            } else {
                final MergedSpectrum merged = segmenter.mergeViaClustering(null, xs.stream().flatMap(x-> Arrays.stream(x.scans)).toArray(Ms2CosineSegmenter.CosineQuery[]::new));
                done.add(new Target(s.getApexMass(), s.getApexRt(), merged));
            }
        }
        return done;
    }

    private boolean areDistinctSegments(ChromatographicPeak.Segment left, ChromatographicPeak.Segment right) {
        // there has to be a clear minimum between both apexes and at least four data points in between
        if (right.getApexIndex()-left.getApexIndex() <= 5) return false;
        final double li = left.getApexIntensity(), ri = right.getApexIntensity(), minimum = left.getPeak().getIntensityAt(left.getEndIndex());
        if (minimum < li*0.8 && minimum < ri*0.8) return true;
        // the gap in between is larger than the FWHM of both
        final long width = left.fwhm() + right.fwhm();
        return (right.getApexRt()-left.getApexRt()) > width;
    }

    private Optional<ChromatographicPeak> findTarget(ProcessedSample ms1, ArrayList<Scan> scans, long[] rts, long maxRtDistance, Target target, LCMSProccessingInstance instance) {
        int bestMatch = Arrays.binarySearch(rts, target.rt);
        if (bestMatch<0) {
            bestMatch=-bestMatch - 1;
        }
        int forward = bestMatch, backward = bestMatch-1;
        final long rightBorder = target.rt + maxRtDistance, leftBorder = target.rt - maxRtDistance;
        boolean progress=true;
        boolean notFoundDueToIntensityThreshold = false;
        while (progress) {
            progress=false;
            if (forward < rts.length && rts[forward] < rightBorder) {
                final Scan scan = scans.get(forward);
                Optional<ChromatographicPeak> feature = ms1.builder.detect(scan, target.mz, isolationWindow);
                if (feature.isPresent() && feature.get().numberOfScans()>=5) {
                    return feature;
                }
                ++forward;
                progress=true;
            }
            if (backward >= 0 && rts[backward] > leftBorder) {
                final Scan scan = scans.get(backward);
                Optional<ChromatographicPeak> feature = ms1.builder.detect(scan, target.mz, isolationWindow);
                if (feature.isPresent()) {
                    return feature;
                }
                --backward;
                progress=true;
            }
        }
        return Optional.empty();
    }

    private List<Target> extractTargets(ProcessedSample msms) {
        final List<Target> targets = new ArrayList<>();
        final long retentionTimeThreshold = 10000;//(long)Math.ceil(msms.run.retentionTimeRange().getMaximum()*0.01);
        final Ms2CosineSegmenter cosiner = new Ms2CosineSegmenter();
        final MassMap<Scan> map = new MassMap<>(100);
        PriorityQueue<Scan> scans = new PriorityQueue<>(Comparator.comparingDouble(x->-x.getTIC()));
        for (Scan s : msms.run.getScans()) {
            if (s.getPrecursor() != null) {
                map.put(s.getPrecursor().getMass(), s);
                scans.add(s);
            }
        }
        final TIntHashSet done = new TIntHashSet();
        final TIntHashSet merged = new TIntHashSet();
        // merge all scans
        for (Scan s : scans) {
            if (done.add(s.getIndex())) {
                final Ms2CosineSegmenter.CosineQuery[] tomerge = map.retrieveAll(s.getPrecursor().getMass(), new Deviation(10)).stream().filter(x->Math.abs(x.getRetentionTime()-s.getRetentionTime())<retentionTimeThreshold).filter(x->x==s || !done.contains(x.getIndex())).map(x->cosiner.prepareForCosine(msms, x)).filter(Objects::nonNull).toArray(Ms2CosineSegmenter.CosineQuery[]::new);
                // this can happen if the MS/MS spectrum is empty
                if (tomerge.length==0) continue;
                final MergedSpectrum mergedPeaks = tomerge.length>1 ? cosiner.mergeViaClustering(msms, tomerge.clone()) : tomerge[0].getOriginalSpectrum();
                merged.clear();

                for (Ms2CosineSegmenter.CosineQuery q : tomerge) {
                    for (Scan t : q.getOriginalSpectrum().getScans()) {
                        merged.add(t.getIndex());
                    }
                }
                done.addAll(merged);
                final long ret = mergedPeaks.getScans().stream().sorted(Comparator.comparingDouble(x -> -x.getPrecursor().getIntensity())).findFirst().stream().mapToLong(Scan::getRetentionTime).findFirst().orElse(s.getRetentionTime());
                targets.add(new Target(
                        mergedPeaks.getPrecursor().getMass(),
                        ret,
                        mergedPeaks,
                        Arrays.stream(tomerge).filter(x->merged.contains(x.getOriginalSpectrum().getScans().get(0).getIndex())).toArray(Ms2CosineSegmenter.CosineQuery[]::new)
                ));
            }
        }
        return targets;
    }

    private class Target {
        private double mz;
        private long rt, rtOrig;
        private MergedSpectrum spectrum;
        private Ms2CosineSegmenter.CosineQuery[] scans;
        private FragmentedIon ion;
        private ChromatographicPeak.Segment correspondingFeature;
        private boolean merged = false;

        @Override
        public String toString() {
            return String.format(Locale.US, "<m/z = %.4f, rt = %.4f>", mz, rtOrig/60000d);
        }

        public Target(double mz, long rt, MergedSpectrum spectrum, Ms2CosineSegmenter.CosineQuery... scans) {
            this.mz = mz;
            this.rt = rt;
            this.rtOrig = rt;
            this.spectrum = spectrum;
            this.scans = scans;
            assert scansAreDifferent();
        }

        private boolean scansAreDifferent() {
            final TIntHashSet left = new TIntHashSet();
            for (int i=0; i < scans.length; ++i) {
                left.clear();
                left.addAll(scans[i].getOriginalSpectrum().getScans().stream().mapToInt(Scan::getIndex).toArray());
                for (int j=i+1; j < scans.length; ++j) {
                    for (Scan s : scans[j].getOriginalSpectrum().getScans()) {
                        if (left.contains(s.getIndex())) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        public List<Target> split() {
            final List<Target> tgs = new ArrayList<>();
            for (Ms2CosineSegmenter.CosineQuery s : scans) {
                MergedSpectrum m = s.getOriginalSpectrum();
                final Target t = new Target(m.getPrecursor().getMass(), m.getScans().get(0).getRetentionTime(), m, s);
                tgs.add(t);
            }
            tgs.sort(Comparator.comparingLong(x->x.rt));
            return tgs;
        }

        public Target(FragmentedIon spectrum) {
            this.mz = spectrum.getMass();
            this.rt = spectrum.getRetentionTime();
            this.scans = new Ms2CosineSegmenter.CosineQuery[0];
            this.rtOrig = this.rt;
            this.ion = spectrum;
        }

        public void assignFeature(ChromatographicPeak peak) {
            // find best segment
            ChromatographicPeak.Segment best = null;
            long bestDist = Long.MAX_VALUE;
            for (ChromatographicPeak.Segment s : peak.getSegments().values()) {
                final long d = Math.abs(s.getApexRt() - rt);
                if (d < bestDist) {
                    bestDist = d;
                    best = s;
                }
            }
            this.correspondingFeature = best;
            assert best==null || peak.getSegmentWithApexId(correspondingFeature.getApexIndex()).isPresent();
        }
    }


}
