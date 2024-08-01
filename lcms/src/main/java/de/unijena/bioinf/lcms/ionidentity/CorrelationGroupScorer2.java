package de.unijena.bioinf.lcms.ionidentity;

import de.unijena.bioinf.ChemistryBase.math.ExponentialDistribution;
import de.unijena.bioinf.model.lcms.ChromatographicPeak;
import gnu.trove.list.array.TDoubleArrayList;
import org.apache.commons.lang3.Range;

import static de.unijena.bioinf.ChemistryBase.utils.RangeUtils.isEmpty;

public class CorrelationGroupScorer2 {

    public CorrelationGroupScorer2() {

    }

    public double predictProbability(ChromatographicPeak.Segment leftIon, ChromatographicPeak.Segment rightIon) {
        if (rightIon.getApexIntensity() > leftIon.getApexIntensity()) {
            return predictProbability(rightIon, leftIon);
        }
        final ChromatographicPeak left = leftIon.getPeak();
        final ChromatographicPeak right = rightIon.getPeak();
        // first align both vectors
        final ChromatographicPeak.Segment mainSegment = leftIon;
        final int mainBegin = mainSegment.getStartIndex();
        final int mainEnd = mainSegment.getEndIndex();
        final float[] intensitiesLeft = new float[mainEnd-mainBegin+1];
        final float[] intensitiesRight = intensitiesLeft.clone();
        for (int i=0; i < intensitiesLeft.length; ++i) {
            intensitiesLeft[i] = (float)left.getIntensityAt(mainBegin+i);
            int rscan = right.findScanNumber(left.getScanNumberAt(mainBegin+i));
            if (rscan<0) {
                intensitiesRight[i] = 0;
            } else {
                intensitiesRight[i] = (float)right.getIntensityAt(rscan);
            }
        }
        Range<Integer> t25 = mainSegment.calculateFWHMMinPeaks(0.25, 3);
        t25 = Range.of(t25.getMinimum()-mainBegin, t25.getMaximum()-mainBegin);

        //////////////////////////////////////////////
        float[] completeX = intensitiesLeft, completeY = intensitiesRight;
        float[][] xy25 = extrArray(intensitiesLeft,intensitiesRight, t25);
        /////////////////////////////////////////////

        double correlationComplete = pearson(completeX, completeY);
        double correlation25 = pearson(xy25[0], xy25[1]);
        double ml25 = Math.min(150, Math.max(ml(xy25[0],xy25[1]), -150));
        double mlPerPeak = ml(completeX,completeY) / completeX.length;
        double cosine = cosine(completeX,completeY);

        ExponentialDistribution correlationDistribution = new ExponentialDistribution(28.244636243022576);
        ExponentialDistribution correlation25Distribution = new ExponentialDistribution(20.599845979349915);
        ExponentialDistribution cosineDistribution = new ExponentialDistribution(32.87442414088085);

        double correlationDist = 1d-correlationDistribution.getCumulativeProbability(1-correlationComplete);
        double correlation25Dist = 1d-correlation25Distribution.getCumulativeProbability(1-correlation25);
        double cosineDist = 1d-cosineDistribution.getCumulativeProbability(1-cosine);

        double max = Math.max(correlationComplete, correlation25);

        // standardizing the data
        correlationComplete = (correlationComplete - 0.77300947d)/0.24259546d;
        correlation25 = (correlation25 - 0.7421673d) / 0.29368569d;
        ml25 = (ml25 - 23.85311168d) / 36.55617401;
        mlPerPeak = (mlPerPeak - 5.97456173d) / 4.74243129d;
        cosine = (cosine - 0.85369755d)/0.14702799d;
        correlationDist = (correlationDist - 0.22150261d)/0.32509649d;
        correlation25Dist = (correlation25Dist - 0.2494303)/0.33362106d;
        cosineDist = (cosineDist - 0.2324338d)/0.32800417d;
        max = (max - 0.83123974d)/0.19354648d;
        if (completeX.length > 10) {
            double score = -2.26430239;
            score += correlation25 * 0.41599358;
            score += ml25 * 0.51549164;
            score += mlPerPeak * 0.33843087;
            score += correlationDist* 0.81630293;
            score += correlation25Dist * 0.65308529;
            score += cosineDist * 1.2133807;
            score += max*0.7089628;
            return 1d / (1 + Math.exp(-score));
        } else {
            double score = -1.60906428d;
            score += correlationComplete * 0.68589627;
            score += correlation25 * 1.03476268;
            score += ml25 * 0.99107672;
            score += mlPerPeak * 0.1295519;
            score += correlationDist * 1.41527795;
            score += cosineDist * 0.31796007;
            return 1d / (1 + Math.exp(-score));
        }
    }


    private float cosine(float[] xs, float[] ys) {
        if (xs.length<=1) return Float.NaN;
        double X=0d, Y=0d, XY=0d;
        for (int i=0; i < xs.length; ++i) {
            X += xs[i]*xs[i];
            Y += ys[i]*ys[i];
            XY += xs[i]*ys[i];
        }
        return (float)(XY/Math.sqrt(X*Y));
    }


    private static float ml(float[] xs, float[] ys) {
        if (xs.length<=0) return Float.NaN;
        final TDoubleArrayList xs2 = new TDoubleArrayList();
        for (float x : xs) xs2.add(x);
        final TDoubleArrayList ys2 = new TDoubleArrayList();
        for (float y : ys) ys2.add(y);
        return (float)CorrelationGroupScorer.maximumLikelihoodIsotopeScore2(xs2, ys2);

    }

    private static float pearson(float[] a, float[] b) {
        final int n = b.length;
        if (n<=1) return Float.NaN;
        double meanA=0d;
        double meanB=0d;
        for (int i=0; i < n; ++i) {
            meanA += a[i];
            meanB += b[i];
        }
        meanA /= n;
        meanB /= n;
        double va=0d, vb=0d, vab=0d;
        for (int i=0; i < n; ++i) {
            final double x = a[i]-meanA;
            final double y = b[i]-meanB;
            va += x*x;
            vb += y*y;
            vab += x*y;
        }
        if (va*vb==0) return 0f;
        return (float)(vab / Math.sqrt(va*vb));
    }

    private static float[][] extrArray(float[] xs, float[] ys, Range<Integer> range) {
        if (isEmpty(range)) return new float[2][0];
        int n = range.getMaximum()-range.getMinimum()+1;
        float[] xs2 = new float[n], ys2= new float[n];
        for (int i=range.getMinimum(); i <= range.getMaximum(); ++i) {
            xs2[i-range.getMinimum()] = xs[i];
            ys2[i-range.getMinimum()] = ys[i];
        }
        return new float[][]{xs2,ys2};
    }



}
