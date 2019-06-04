package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.model.lcms.AlignedIon;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import de.unijena.bionf.spectral_alignment.CosineQuerySpectrum;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FeatureAligner {

    public AlignedIon[] align(ProcessedSample left, ProcessedSample right) {
        // first make a pool of m/z values we want to align
        final TLongHashSet mzLeft = new TLongHashSet(), mzRight = new TLongHashSet();
        for (FragmentedIon l : left.ions) {
            final double mass = l.getMass();
            final long roundedDown = (long)Math.floor(mass*20);
            final long roundedUp = (long)Math.ceil(mass*20);
            mzLeft.add(roundedDown); mzLeft.add(roundedUp);
        }

        for (FragmentedIon r : right.ions) {
            final double mass = r.getMass();
            final long roundedDown = (long)Math.floor(mass*20);
            final long roundedUp = (long)Math.ceil(mass*20);
            mzRight.add(roundedDown); mzRight.add(roundedUp);
        }

        mzLeft.retainAll(mzRight);

        final List<FragmentedIon> ionsLeft = new ArrayList<>();
        for (FragmentedIon l : left.ions.stream().sorted(Comparator.comparingInt(u -> u.getSegment().getApexScanNumber())).toArray(FragmentedIon[]::new)) {
            final double mass = l.getMass();
            final long roundedDown = (long)Math.floor(mass*20);
            final long roundedUp = (long)Math.ceil(mass*20);
            if (mzLeft.contains(roundedDown) || mzLeft.contains(roundedUp))
                ionsLeft.add(l);
        }

        final List<FragmentedIon> ionsRight = new ArrayList<>();
        for (FragmentedIon l : right.ions.stream().sorted(Comparator.comparingInt(u -> u.getSegment().getApexScanNumber())).toArray(FragmentedIon[]::new)) {
            final double mass = l.getMass();
            final long roundedDown = (long)Math.floor(mass*20);
            final long roundedUp = (long)Math.ceil(mass*20);
            if (mzLeft.contains(roundedDown) || mzLeft.contains(roundedUp))
                ionsRight.add(l);
        }

        return align(ionsLeft, ionsRight);

    }

    protected AlignedIon[] align(List<FragmentedIon> left, List<FragmentedIon> right) {
        final double[][] scores = new double[left.size()][right.size()];
        computePairwiseCosine(scores, left, right);
        final double[][] D = new double[left.size()][right.size()];
        for (int i=0; i < D.length; ++i) {
            for (int j=0; j < D[0].length; ++j) {
                double gapLeft = (i<=0) ? 0 : D[i-1][j];
                double gapRight = (j<=0) ? 0 : D[i][j-1];
                D[i][j] = Math.max(
                        Math.max(gapLeft,gapRight), // gapleft or gapRight
                        Math.max(0, (i>0&&j>0 ? D[i-1][j-1] : 0) + scores[i][j])// align
                );
            }
        }
        return backtrack(D,left,right);
    }

    protected AlignedIon[] backtrack(double[][] scores, List<FragmentedIon> left, List<FragmentedIon> right) {
        // backtrack
        int maxI=0,maxJ=0;
        double max=0d;
        for (int i=0; i < left.size(); ++i) {
            for (int j=0; j < right.size(); ++j) {
                if (scores[i][j] > max) {
                    max=scores[i][j];
                    maxI=i;maxJ=j;
                }
            }
        }
        if (max <= 0) return new AlignedIon[0];
        final ArrayList<AlignedIon> ions = new ArrayList<>();
        backtrackFrom(scores, maxI, maxJ, left, right, ions);
        return ions.toArray(new AlignedIon[ions.size()]);
    }

    private void backtrackFrom(double[][] D, int i, int j, List<FragmentedIon> left, List<FragmentedIon> right, ArrayList<AlignedIon> ions) {
        while (i > 0 && j > 0) {
            double gapLeft = D[i-1][j];
            double gapRight = D[i][j-1];
            double score = D[i][j];
            if (score > gapLeft && score > gapRight) {
                ions.add(new AlignedIon(left.get(i),right.get(j), score-D[i-1][j-1]));
                i = i-1;
                j = j-1;
            } else if (score == gapLeft) {
                --i;
            } else --j;
        }
        Collections.reverse(ions);
    }

    private void computePairwiseCosine(double[][] scores, List<FragmentedIon> left, List<FragmentedIon> right) {
        System.out.println("-------------------");
        final Deviation dev = new Deviation(15);
        final CosineQueryUtils utils = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(dev));
        final List<CosineQuerySpectrum> ll = new ArrayList<>(), rr = new ArrayList<>();
        for (FragmentedIon l : left) {
            final SimpleMutableSpectrum buf = new SimpleMutableSpectrum(l.getMsMs());
            Spectrums.cutByMassThreshold(buf, l.getMass()-20d);
            Spectrums.applyBaseline(buf, l.getMsMs().getNoiseLevel());
            final SimpleSpectrum spectrum = Spectrums.extractMostIntensivePeaks(buf, 8, 100);
            ll.add(utils.createQuery(spectrum, l.getMass()));
        }
        for (FragmentedIon r : right) {
            final SimpleMutableSpectrum buf = new SimpleMutableSpectrum(r.getMsMs());
            Spectrums.cutByMassThreshold(buf, r.getMass()-20d);
            Spectrums.applyBaseline(buf, r.getMsMs().getNoiseLevel());
            final SimpleSpectrum spectrum = Spectrums.extractMostIntensivePeaks(buf, 8, 100);
            rr.add(utils.createQuery(spectrum, r.getMass()));
        }
        for (int i=0; i < left.size(); ++i) {
            final FragmentedIon l = left.get(i);
            for (int j=0; j < right.size(); ++j) {
                final FragmentedIon r = right.get(j);
                if (dev.inErrorWindow(l.getMass(), r.getMass())) {
                    SpectralSimilarity spectralSimilarity = utils.cosineProduct(ll.get(i), rr.get(j));
                    if (spectralSimilarity.similarity < 0.5 || spectralSimilarity.shardPeaks <= 3) {
                        scores[i][j] = Double.NEGATIVE_INFINITY;
                    } else {
                        scores[i][j] = spectralSimilarity.similarity * 100d + spectralSimilarity.shardPeaks;
                        System.out.println(scores[i][j] + " at "+ i  +", " + j);
                    }
                } else scores[i][j] = Double.NEGATIVE_INFINITY;
            }
        }
    }

}
