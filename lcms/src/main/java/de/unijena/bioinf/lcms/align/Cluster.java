package de.unijena.bioinf.lcms.align;

import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import de.unijena.bioinf.model.lcms.GapFilledIon;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.*;
import java.util.stream.Collectors;

public class Cluster {

    protected AlignedFeatures[] features;
    protected double score;
    protected Cluster left, right;
    protected HashSet<ProcessedSample> mergedSamples;

    public Cluster(AlignedFeatures[] features, double score, Cluster left, Cluster right, boolean keepIntermediates) {
        this.features = features.clone();
        Arrays.sort(this.features, Comparator.comparingDouble(AlignedFeatures::getRetentionTime));
        this.score = score;
        this.left = keepIntermediates ? left : left.header();
        this.right = keepIntermediates ? right : right.header();
        this.mergedSamples = new HashSet<>();
        mergedSamples.addAll(left.mergedSamples);
        mergedSamples.addAll(right.mergedSamples);
    }

    private Cluster header() {
        return new Cluster(new AlignedFeatures[0], score, left, right, mergedSamples);
    }

    public boolean isLeaf() {
        return left==null && right==null;
    }

    public Cluster(AlignedFeatures[] features, double score, Cluster left, Cluster right, HashSet<ProcessedSample> mergedSamples) {
        this.features = features;
        this.score = score;
        this.left = left;
        this.right = right;
        this.mergedSamples = mergedSamples;
    }

    public AlignedFeatures[] getFeatures() {
        return features;
    }

    public Cluster(ProcessedSample sample, boolean useAll) {
        this.left = null;
        this.right = null;
        this.score = 0d;
        this.mergedSamples = new HashSet<>();
        mergedSamples.add(sample);
        {
            final ArrayList<AlignedFeatures> feats = new ArrayList<>();
            for (FragmentedIon ion : sample.ions) {
                if (ion.getPeakShape().getPeakShapeQuality().betterThan(Quality.BAD) && (useAll || ion.getMsMsQuality().betterThan(Quality.BAD)))
                    feats.add(new AlignedFeatures(sample, ion, sample.getRecalibratedRT(ion.getRetentionTime())));
            }
            if (useAll) {
                for (int i = 0; i < sample.gapFilledIons.size(); ++i) {
                    feats.add(new AlignedFeatures(sample, sample.gapFilledIons.get(i), sample.getRecalibratedRT(sample.gapFilledIons.get(i).getRetentionTime())));
                }
                for (int i = 0; i < sample.otherIons.size(); ++i) {
                    feats.add(new AlignedFeatures(sample, sample.otherIons.get(i), sample.getRecalibratedRT(sample.otherIons.get(i).getRetentionTime())));
                }
            }
            this.features = feats.toArray(new AlignedFeatures[feats.size()]);
        }
        Arrays.sort(this.features, Comparator.comparingDouble(AlignedFeatures::getRetentionTime));
    }

    public HashSet<ProcessedSample> getMergedSamples() {
        return mergedSamples;
    }

    public Cluster getLeft() {
        return left;
    }

    public Cluster getRight() {
        return right;
    }

    public String toString() {
        return mergedSamples.stream().map(x->x.run.getIdentifier()).collect(Collectors.joining(",")) + " :: " + score;
    }

    public Cluster deleteRowsWithNoIsotopes() {
        final ArrayList<AlignedFeatures> alf = new ArrayList<>();
        outerloop:
        for (AlignedFeatures f : this.features) {
            for (FragmentedIon ion : f.features.values()) {
                if (!(ion.getIsotopes().isEmpty())) {
                    alf.add(f);
                    continue outerloop;
                }
            }
        }
        Set<FragmentedIon> alignedFeatures = new HashSet<>();
        for (AlignedFeatures f : alf)
            for (FragmentedIon i : f.getFeatures().values())
                alignedFeatures.add(i);
        for (ProcessedSample s : getMergedSamples()) {
            s.gapFilledIons.removeIf(x->!alignedFeatures.contains(x));
            s.ions.removeIf(x->!alignedFeatures.contains(x));
        }
        return new Cluster(alf.toArray(new AlignedFeatures[0]), score, left, right,mergedSamples);
    }

    public Cluster deleteRowsWithNoMsMs() {
        final ArrayList<AlignedFeatures> alf = new ArrayList<>();
        outerloop:
        for (AlignedFeatures f : this.features) {
            for (FragmentedIon ion : f.features.values()) {
                if (!(ion instanceof GapFilledIon)) {
                    alf.add(f);
                    continue outerloop;
                }
            }
        }
        Set<FragmentedIon> alignedFeatures = new HashSet<>();
        for (AlignedFeatures f : alf)
            for (FragmentedIon i : f.getFeatures().values())
                alignedFeatures.add(i);
        for (ProcessedSample s : getMergedSamples()) {
            s.gapFilledIons.removeIf(x->!alignedFeatures.contains(x));
            s.ions.removeIf(x->!alignedFeatures.contains(x));
        }
        return new Cluster(alf.toArray(new AlignedFeatures[0]), score, left, right,mergedSamples);
    }

    public double estimateError() {
        final TDoubleArrayList values = new TDoubleArrayList();
        final int thr = Math.max(2,Math.min(10,(int)Math.ceil(this.mergedSamples.size()*0.2)));
        for (AlignedFeatures f : features) {
            if (f.getFeatures().size() < thr)
                continue;
            final ArrayList<ProcessedSample> xs = new ArrayList<>(f.features.keySet());
            for (int i=0; i < xs.size(); ++i) {
                for (int j=0; j < i; ++j) {
                    values.add(Math.pow(xs.get(i).getRecalibratedRT(f.features.get(xs.get(i)).getRetentionTime()) - xs.get(j).getRecalibratedRT(f.features.get(xs.get(j)).getRetentionTime()), 2));
                }
            }
        }
        values.sort();
        double mean = 0d;
        int k=(int)(values.size()*0.25), n =(int)(values.size()*0.75);
        for (int i=0; i < n; ++i) {
            mean += values.getQuick(i);
        }
        return Math.sqrt(mean/(n-k));
    }
    public double estimatePeakShapeError() {
        final TDoubleArrayList values = new TDoubleArrayList();

        for (AlignedFeatures f : features) {
            final ArrayList<FragmentedIon> xs = new ArrayList<>(f.features.values());
            for (int i=0; i < xs.size(); ++i) {
                for (int j=0; j < i; ++j) {
                    values.add(xs.get(i).comparePeakWidthSmallToLarge(xs.get(j)));
                }
            }
        }
        return Statistics.robustAverage(values.toArray());
    }

    public Cluster deleteRowsWithTooFewEntries(int threshold) {
        final ArrayList<AlignedFeatures> alf = new ArrayList<>();
        outerloop:
        for (AlignedFeatures f : this.features) {
            if (f.features.size() >= threshold)
                alf.add(f);
        }
        Set<FragmentedIon> alignedFeatures = new HashSet<>();
        for (AlignedFeatures f : alf)
            for (FragmentedIon i : f.getFeatures().values())
                alignedFeatures.add(i);
        for (ProcessedSample s : getMergedSamples()) {
            s.gapFilledIons.removeIf(x->!alignedFeatures.contains(x));
            s.ions.removeIf(x->!alignedFeatures.contains(x));
        }
        return new Cluster(alf.toArray(new AlignedFeatures[0]), score, left, right,mergedSamples);
    }
}
