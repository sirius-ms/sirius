package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import gnu.trove.set.hash.TIntHashSet;

import java.util.BitSet;

/**
 * spectral comparison. dot product like.
 */
public abstract class AbstractSpectralAlignment {

    protected Deviation deviation;

    /**
     *
     * @param deviation should be higher than usual expected mass deviation to not punish mz errors too much. this results in low cosine scores even for the same compounds
     */
    public AbstractSpectralAlignment(Deviation deviation) {
        this.deviation = deviation;
    }


    public abstract SpectralSimilarity score(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right);

    /**
     * one peak can only match one peak in the other spectrum
     * @param left
     * @param right
     * @return
     */
    public SpectralSimilarity score1To1(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right) {
        if (left.size()==0 || right.size()==0) return new SpectralSimilarity(0d, 0);
        MatchesMatrix backtrace = new MatchesMatrix(left.size(), right.size());
        double[] scoreRowBefore = new double[left.size()];
        double[] scoreRow = new double[left.size()];
        int i = 0, j = 0, i_lower;

        final int nl=left.size(), nr=right.size();
        while (i < nl && left.getMzAt(i) < 0.5d) ++i; //skip negative peaks of inversed spectra
        while (j < nr && right.getMzAt(j) < 0.5d) ++j;
        i_lower=i;
        while (i < nl && j < nr) {
            Peak lp = left.getPeakAt(i);
            Peak rp = right.getPeakAt(j);
            final double difference = lp.getMass()- rp.getMass();
            final double allowedDifference = maxAllowedDifference(Math.min(lp.getMass(), rp.getMass()));

            double matchScore;
            if (Math.abs(difference) <= allowedDifference) {
                matchScore = scorePeaks(lp,rp);
            } else {
                matchScore = 0d;
            }
            if (i==0) {
                scoreRow[i] = Math.max(scoreRowBefore[i], matchScore);
                if (matchScore==scoreRow[i]){
                    backtrace.setMatch(i,j);
                }
            } else {
                //might not have been updated because of this whole window moving but it must hold that scoreRowBefore[i]>=scoreRowBefore[i-1] and scoreRow[i]>=scoreRow[i-1]
                if (scoreRowBefore[i] <= scoreRowBefore[i-1]) {
                    scoreRowBefore[i] = scoreRowBefore[i-1];
                }
//                scoreRowBefore[i] = Math.max(scoreRowBefore[i], scoreRowBefore[i-1]); //might not have been updated because of this whole window moving but it must hold that scoreRowBefore[i]>=scoreRowBefore[i-1] and scoreRow[i]>=scoreRow[i-1]
                scoreRow[i] = Math.max(Math.max(scoreRow[i-1], scoreRowBefore[i]), matchScore+scoreRowBefore[i-1]); //todo max(scoreRow[i]...?
                if (matchScore+scoreRowBefore[i-1]==scoreRow[i]){
                    backtrace.setMatch(i,j);
                }
            }


            if (Math.abs(difference) <= allowedDifference) {
                ++i;
            } else if (difference > 0) {
                ++j;

                double[] tmp = scoreRowBefore;
                scoreRowBefore = scoreRow;
                scoreRow = tmp; //could be an empty array, but in this way we save creation time
                //update horizontal gaps???
                for (int k = i_lower; k <= i; k++) {
                    scoreRow[k] = scoreRowBefore[k]; //todo alway greater equal? Math.max(scoreRow[k], scoreRowBefore[k]);

                }
                i = i_lower;
            } else {
                ++i;
                i_lower = i; //todo or i-1?
            }
            if (i>=nl){
                if (j>=nr-1) break;
                ++j;
                double[] tmp = scoreRowBefore;
                scoreRowBefore = scoreRow;
                scoreRow = tmp; //could be an empty array, but in this way we save creation time
                //update horizontal gaps???
                for (int k = i_lower; k <= nl-1; k++) {
                    scoreRow[k] = scoreRowBefore[k]; //todo alway greater equal? Math.max(scoreRow[k], scoreRowBefore[k]);
                }
                i = i_lower;
            }
        }

        //find best score
        double maxScore = Double.NEGATIVE_INFINITY;
        int maxScorePos = -1;
        double maxMzRight = right.getMzAt(right.size()-1);
        for (int k = Math.min(i_lower,scoreRow.length-1); k < scoreRow.length; k++) {
            double s = scoreRow[k];
            double mz = left.getMzAt(k);
            if (s>=maxScore){
                maxScorePos = k;
                maxScore = s;
            }

            if (maxMzRight<mz && maxAllowedDifference(maxMzRight)<(mz-maxMzRight)){
                break;
            }
        }

        int matchedPeaks = backtraceAndCountMatchedPeaks(left, right, backtrace, maxScorePos, (j==nr?nr-1:j), maxScore);
        return  new SpectralSimilarity(maxScore, matchedPeaks);

    }


    public SpectralSimilarity scoreAllAgainstAll(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right) {
        final BitSet usedIndicesLeft = new BitSet();
        final BitSet usedIndicesRight = new BitSet();

        int i = 0, j = 0;
        double score = 0d;

        final int nl=left.size(), nr=right.size();
        while (i < nl && left.getMzAt(i) < 0.5d) ++i; //skip negative peaks of inversed spectra
        while (j < nr && right.getMzAt(j) < 0.5d) ++j;
        while (i < nl && j < nr) {
            Peak lp = left.getPeakAt(i);
            Peak rp = right.getPeakAt(j);
            final double difference = lp.getMass()- rp.getMass();
            final double allowedDifference = maxAllowedDifference(Math.min(lp.getMass(), rp.getMass()));

            if (Math.abs(difference) <= allowedDifference) {
                double matchScore = scorePeaks(lp,rp);
                score += matchScore;
                usedIndicesLeft.set(i);
                usedIndicesRight.set(j);
                for (int k=i+1; k < nl; ++k) {
                    Peak lp2 = left.getPeakAt(k);
                    final double difference2 = lp2.getMass()- rp.getMass();
                    if (Math.abs(difference2) <= allowedDifference) {
                        matchScore = scorePeaks(lp2,rp);
                        score += matchScore;
                        usedIndicesLeft.set(k);
                    } else break;
                }
                for (int l=j+1; l < nr; ++l) {
                    Peak rp2 = right.getPeakAt(l);
                    final double difference2 = lp.getMass()- rp2.getMass();
                    if (Math.abs(difference2) <= allowedDifference) {
                        matchScore = scorePeaks(lp,rp2);
                        score += matchScore;
                        usedIndicesRight.set(l);
                    } else break;
                }
                ++i; ++j;
            } else if (difference > 0) {
                ++j;

            } else {
                ++i;
            }
        }
        int matchedPeaks = Math.min(usedIndicesLeft.cardinality(), usedIndicesRight.cardinality());
        return  new SpectralSimilarity(score, matchedPeaks);

    }

    private int backtraceAndCountMatchedPeaks(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right, MatchesMatrix backtrace, int imax, int jmax, double maxScore){
        //todo take only one best match. should result in same number of peaks!?!?
        int i = imax;
        int j = jmax;

        final BitSet usedIndicesLeft = new BitSet();
        final BitSet usedIndicesRight = new BitSet();

        while (i>=0 && j>=0) {
            Peak lp = left.getPeakAt(i);
            Peak rp = right.getPeakAt(j);
            final double difference = lp.getMass()- rp.getMass();
            final double allowedDifference = maxAllowedDifference(Math.min(lp.getMass(), rp.getMass()));

            double matchScore;
            if (Math.abs(difference) <= allowedDifference) {
                matchScore = scorePeaks(lp,rp);
            } else {
                matchScore = 0d;
            }

            if (backtrace.hasMatch(i,j)){
                if (matchScore>0) {
                    usedIndicesLeft.set(i);
                    usedIndicesRight.set(j);
                }
                --i;
                --j;
            } else if (lp.getMass()>=rp.getMass()){
                --i;
            } else {
                --j;
            }

        }
        return Math.min(usedIndicesLeft.cardinality(), usedIndicesRight.cardinality());
    }


    protected abstract double scorePeaks(Peak lp, Peak rp);

    protected abstract double maxAllowedDifference(double mz);


    private class MatchesMatrix {
        private int leftN, rightN;
        TIntHashSet pairedIndexSet;

        public MatchesMatrix(int leftN, int rightN) {
            this.leftN = leftN;
            this.rightN = rightN;
            this.pairedIndexSet = new TIntHashSet();
        }

        public void setMatch(int leftIdx, int rightIdx){
            //cantor pairing
            final int sum = leftIdx+rightIdx;
            final int pairedIdx = ((sum)*(sum+1))/2+rightIdx;
            if (pairedIdx>Integer.MAX_VALUE) throw new RuntimeException("cannot map peak indices. paired index in bigger than largest Integer value");
            pairedIndexSet.add(pairedIdx);
        }

        public boolean hasMatch(int leftIdx, int rightIdx){
            //cantor pairing
            final int sum = leftIdx+rightIdx;
            final int pairedIdx = ((sum)*(sum+1))/2+rightIdx;
            return pairedIndexSet.contains(pairedIdx);
        }

    }
}
