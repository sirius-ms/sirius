package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;

import java.util.BitSet;

public class SpectralAlignment {

    private Deviation deviation;

    public SpectralAlignment(Deviation deviation) {
        this.deviation = deviation;
    }


    public SpectralSimilarity score(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right) {
        SpectralSimilarity s1 = score1(left, right);
        return s1;
    }

    public SpectralSimilarity score1(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right) {
        if (left.size()==0 || right.size()==0) return new SpectralSimilarity(0d, 0);
//        byte[][] backtrace = new byte[left.size()][right.size()];
        boolean[][] backtrace = new boolean[left.size()][right.size()];
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
//                if (scoreRowBefore[i]==scoreRow[i]){
//                    backtrace[i][j] = (byte) (backtrace[i][j]|1);
//                }
                if (matchScore==scoreRow[i]){
//                    backtrace[i][j] = (byte) (backtrace[i][j]|4);
                    backtrace[i][j] = true;
                }
            } else {
                //might not have been updated because of this whole window moving but it must hold that scoreRowBefore[i]>=scoreRowBefore[i-1] and scoreRow[i]>=scoreRow[i-1]
                if (scoreRowBefore[i] <= scoreRowBefore[i-1]) {
                    scoreRowBefore[i] = scoreRowBefore[i-1];
//                    if (j>0){
//                        backtrace[i][j-1] = (byte) (backtrace[i][j-1]|2);
//                    }

                }
//                scoreRowBefore[i] = Math.max(scoreRowBefore[i], scoreRowBefore[i-1]); //might not have been updated because of this whole window moving but it must hold that scoreRowBefore[i]>=scoreRowBefore[i-1] and scoreRow[i]>=scoreRow[i-1]
                scoreRow[i] = Math.max(Math.max(scoreRow[i-1], scoreRowBefore[i]), matchScore+scoreRowBefore[i-1]); //todo max(scoreRow[i]...?
//                if (scoreRowBefore[i]==scoreRow[i]){
//                    backtrace[i][j] = (byte) (backtrace[i][j]|1); //from top
//                }
//                if (scoreRow[i-1]==scoreRow[i]){
//                    backtrace[i][j] = (byte) (backtrace[i][j]|2); //from left
//                }
                if (matchScore+scoreRowBefore[i-1]==scoreRow[i]){
//                    backtrace[i][j] = (byte) (backtrace[i][j]|4); //from matching
                    backtrace[i][j] = true; //from matching
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
//                    scoreRow[k] = scoreRowBefore[k];
                    scoreRow[k] = Math.max(scoreRow[k], scoreRowBefore[k]);
                }
                i = i_lower;
            } else {
                i_lower = i; //todo or i-1?
                ++i;
            }
            if (i>=nl){
                if (j>=nr-1) break;
                ++j;
                double[] tmp = scoreRowBefore;
                scoreRowBefore = scoreRow;
                scoreRow = tmp; //could be an empty array, but in this way we save creation time
                //update horizontal gaps???
                for (int k = i_lower; k <= nl-1; k++) {
                    if (scoreRowBefore[k]==scoreRow[k]){
//                        backtrace[k][j] = (byte) (backtrace[k][j]+1); //from top
                    } else {
//                        scoreRow[k] = scoreRowBefore[k];
                        scoreRow[k] = Math.max(scoreRow[k], scoreRowBefore[k]);
                    }

//                    scoreRow[k] = scoreRowBefore[k];

//                    scoreRow[k] = Math.max(scoreRow[k], scoreRowBefore[k]);
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

            if (maxMzRight<mz && maxAllowedDifference(maxMzRight)>(mz-maxMzRight)){
                break;
            }
        }

        int matchedPeaks = backtraceAndCountMatchedPeaks(left, right, backtrace, maxScorePos, (j==nr?nr-1:j), maxScore);
        return  new SpectralSimilarity(maxScore, matchedPeaks);

    }

    private int backtraceAndCountMatchedPeaks(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right, boolean[][] backtrace, int imax, int jmax, double maxScore){
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



//            if (((backtrace[i][j] & 4 ) != 0)){
//                if (matchScore>0) {
//                    usedIndicesLeft.set(i);
//                    usedIndicesRight.set(j);
//                }
//                --i;
//                --j;
//
////            } else if (i>0 && ((backtrace[i][j] & 2 ) != 0)){
//            } else if (((backtrace[i][j] & 2 ) != 0)){
//                --i;
//                //            } else if (j>0 && ((backtrace[i][j] & 1 ) != 0)){
//            } else if (((backtrace[i][j] & 1 ) != 0)){
//                --j;
//            } else if (lp.getMass()>=rp.getMass()){
//                --j;
////                --i;
//            } else {
//                --i;
////                --j;
//            }




//
//            if (((backtrace[i][j] & 4 ) != 0)){
            if (backtrace[i][j]){
                if (matchScore>0) {
                    usedIndicesLeft.set(i);
                    usedIndicesRight.set(j);
                }
                --i;
                --j;

//            } else if (i>0 && ((backtrace[i][j] & 2 ) != 0)){
            } else if (lp.getMass()>=rp.getMass()){
//                --j;
                --i;
            } else {
//                --i;
                --j;
            }



        }
        return Math.min(usedIndicesLeft.cardinality(), usedIndicesRight.cardinality());
    }


    private double scorePeaks(Peak lp, Peak rp) {
        return lp.getIntensity()*rp.getIntensity();
        //        return Math.max(0.0000001,lp.getIntensity()*rp.getIntensity());
//        return 1d;
//        double min = Math.min(lp.getMass(), rp.getMass());
//        double max = Math.max(lp.getMass(), rp.getMass());
//
//        return deviation.inErrorWindow(min, max)?1d:0d;
    }

    private double scorePeaksOld(Peak lp, Peak rp) {
        double min = Math.min(lp.getMass(), rp.getMass());
        double max = Math.max(lp.getMass(), rp.getMass());

//        return deviation.inErrorWindow(min, max)?1d:0d;
        return deviation.inErrorWindow(min, max)?lp.getIntensity()*rp.getIntensity():0d;
    }


    private double maxAllowedDifference(double mz) {
        //change to, say 3*dev, when using gaussians
        return deviation.absoluteFor(mz);
    }

}
