package de.unijena.bioinf.lcms.isotopes;

import org.apache.commons.lang3.Range;
import de.unijena.bioinf.ChemistryBase.math.ExponentialDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.align.MoI;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegment;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static de.unijena.bioinf.ChemistryBase.utils.RangeUtils.isEmpty;

public class IsotopeDetectionByCorrelation implements IsotopeDetectionStrategy{


    @Override
    public Result detectIsotopesFor(ProcessedSample sample, MoI moi, ContiguousTrace trace, TraceSegment traceSegment) {
        final Deviation smallDev = sample.getStorage().getStatistics().getMs1MassDeviationWithinTraces();
        final Deviation largeDev = smallDev.multiply(3);
        final SimpleSpectrum spectrum = sample.getStorage().getSpectrumStorage().getSpectrum(moi.getScanId());
        int peakIdx = Spectrums.mostIntensivePeakWithin(spectrum, trace.mz(moi.getScanId()), smallDev);
        if (peakIdx<0) {
            LoggerFactory.getLogger(IsotopeDetectionStrategy.class).warn("Cannot find peak for " + moi);
            return Result.nothingFound();
        }
        final List<IsotopePattern> monoisotopicPatterns = IsotopePattern.extractPatterns(spectrum, peakIdx);
        final List<IsotopePattern> nonMonoisotopicPatterns = IsotopePattern.getPatternsForNonMonoisotopicPeak(spectrum, peakIdx);
        final List<IsotopePattern> allPatterns = new ArrayList<>();
        allPatterns.addAll(monoisotopicPatterns);
        allPatterns.addAll(nonMonoisotopicPatterns);
        // we now calculate scores based on correlation to find the best matching isotope pattern
        double bestScore=0;
        IsotopePattern bestPattern = null; int[] bestTraceIds=null;
        for (IsotopePattern p : allPatterns) {
            double scoresum=0;
            boolean[] iso = new boolean[p.size()];
            int[] traceIdsAll = new int[p.size()];
            for (int i=0; i < p.size(); ++i) {
                final double mz = p.getMzAt(i);
                if (Math.abs(mz-spectrum.getMzAt(peakIdx))<1e-3) {
                    iso[i]=true;
                    traceIdsAll[i] = trace.getUid();
                    continue;
                }
                int isotopePeakIdx = Spectrums.mostIntensivePeakWithin(spectrum, mz, smallDev);
                if (isotopePeakIdx<0) isotopePeakIdx = Spectrums.mostIntensivePeakWithin(spectrum, mz, largeDev);
                if (isotopePeakIdx<0) continue;
                Optional<ContiguousTrace> isotope = sample.getStorage().getTraceStorage().getContigousTrace(mz-largeDev.absoluteFor(mz), mz+largeDev.absoluteFor(mz), moi.getScanId());
                double score = isotope.isPresent() ? correlationScore(sample, moi, trace, traceSegment, spectrum, isotope.get()) : 0d;
                if (score >= 0.5) {
                    scoresum+=score;
                    traceIdsAll[i] =isotope.get().getUid();
                    iso[i] = true;
                }
            }
            if (scoresum<=bestScore) continue;
            int usedPeaks = 0;
            for (boolean x : iso) {
                if (x) {
                    usedPeaks+=1;
                }
            }
            int[] traceIds = traceIdsAll;
            if (usedPeaks < p.size()) {
                double[] masses = new double[usedPeaks];
                double[] intens = new double[usedPeaks];
                traceIds = new int[usedPeaks];
                int j=0;
                for (int k=0; k < p.size(); ++k) {
                    if (iso[k]) {
                        masses[j]=p.getMzAt(k);
                        intens[j]=p.getIntensityAt(k);
                        traceIds[j] = traceIdsAll[k];
                        ++j;
                    }
                }
                p = new IsotopePattern(masses,intens,p.chargeState);
            }

            bestScore = scoresum;
            bestPattern = p;
            bestTraceIds=traceIds;
        }
        if (bestPattern==null) return Result.nothingFound();
        if (Math.abs(bestPattern.getMzAt(0)-spectrum.getMzAt(peakIdx)) < 1e-3)
            return Result.monoisotopicPeak(new IsotopeResult(bestPattern.chargeState, bestTraceIds, bestPattern.floatMzArray(), bestPattern.floatIntensityArray()));
        return Result.isIsotopicPeak();
    }

    public double correlationScore(ProcessedSample sample, MoI moi, ContiguousTrace trace, TraceSegment traceSegment, SimpleSpectrum spectrum,
                                   ContiguousTrace iso) {
        // find matching segment
        List<TraceSegment> matching = new ArrayList<>();
        for (TraceSegment s : iso.getSegments()) {
            if (s.apex >= traceSegment.leftEdge && s.apex <= traceSegment.rightEdge) {
                matching.add(s);
            }
        }
        matching.sort(Comparator.comparingInt(x->Math.abs(x.apex- trace.apex())));
        TraceSegment isoSegment;
        if (!matching.isEmpty()) {
            isoSegment = matching.get(0);
        } else {
            isoSegment = TraceSegment.createSegmentFor(iso, Math.max(iso.startId(), traceSegment.leftEdge), Math.min(
                    iso.endId(), traceSegment.rightEdge));
        }


        // compute correlation between both traces
        return correlate(trace, iso, traceSegment, isoSegment);
    }

    private double correlate(ContiguousTrace trace, ContiguousTrace isotope, TraceSegment traceSegment, TraceSegment isoSegment) {
        return correlateSmallerToBigger(trace, isotope, traceSegment, isoSegment);
    }

    private double correlateSmallerToBigger(ContiguousTrace trace, ContiguousTrace isotope, TraceSegment traceSegment, TraceSegment isoSegment) {
        if (isotope.intensity(isoSegment.apex) < trace.intensity(traceSegment.apex)) {
            return predictProbability(trace, traceSegment, isotope, isoSegment);
        } else {
            return predictProbability(isotope, isoSegment, trace, traceSegment);
        }
    }

    /////////////////////////////////////

    public double predictProbability(ContiguousTrace leftIon, TraceSegment leftSegment, ContiguousTrace rightIon, TraceSegment rightSegment) {
        if (rightIon.intensity(rightSegment.apex) > leftIon.intensity(leftSegment.apex)) {
            return predictProbability(rightIon, rightSegment, leftIon, leftSegment);
        }
        // first align both vectors
        final TraceSegment mainSegment = leftSegment;
        final int mainBegin = mainSegment.leftEdge;
        final int mainEnd = mainSegment.rightEdge;
        final float[] intensitiesLeft = new float[mainEnd-mainBegin+1];
        final float[] intensitiesRight = intensitiesLeft.clone();
        for (int i=0; i < intensitiesLeft.length; ++i) {
            intensitiesLeft[i] = leftIon.intensity(mainBegin+i);
            intensitiesRight[i] = rightIon.intensityOrZero(mainBegin+i);
        }
        Range<Integer> t25 = calculateFWHMMinPeaks(leftIon, leftSegment, 0.25f, 3);
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

    private Range<Integer> calculateFWHMMinPeaks(ContiguousTrace trace, TraceSegment segment, float percentile, int minPeaks) {
        int apex = segment.apex;
        float intensityThreshold = trace.intensity(apex)*percentile;
        int left=apex, right=apex;
        while (left > segment.leftEdge && trace.intensityOrZero(left)>intensityThreshold) {
            --left;
        }
        while (right > segment.rightEdge && trace.intensityOrZero(right)>intensityThreshold) {
            ++right;
        }
        int missing = minPeaks - (right-left+1);
        if (missing > 0 && 1+segment.rightEdge-segment.leftEdge >= missing) {
            while (true) {
                if (left > segment.leftEdge) {
                    --left;
                    --missing;
                    if (missing <= 0) break;
                    else continue;
                }
                if (right < segment.rightEdge) {
                    ++right;
                    --missing;
                    if (missing <= 0) break;
                    else continue;
                }
                break;
            }
        }
        return Range.of(left,right);
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
        final DoubleArrayList xs2 = new DoubleArrayList();
        for (float x : xs) xs2.add(x);
        final DoubleArrayList ys2 = new DoubleArrayList();
        for (float y : ys) ys2.add(y);
        return (float) maximumLikelihoodIsotopeScore2(xs2, ys2);

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


    private final static double LAMBDA2 = 200d, SIGMA_ABS2 = 0.1, SIGMA_REL2 = 0.01;
    public static double maximumLikelihoodIsotopeScore2(DoubleArrayList xslist, DoubleArrayList yslist) {
        double currentScore = 0d;
        double maxScore = 0d;
        int maxLenLeft, maxLenRight = 0;
        int apex = findApex(xslist);
        double penalty = 0d;
        final double[] xs = normalizedToMax(xslist);
        final double[] ys = normalizedToMax(yslist);
        final int n = xs.length;
        // we start at position apex and then extend pattern to both sides
        for (int i=apex-1; i >= 0; --i) {
            final double intensityScore = LAMBDA2*xs[i];
            penalty += intensityScore;
            final double delta = xs[i]-ys[i];
            final double matchScore = Math.exp(-(delta*delta)/(2*(SIGMA_ABS2*SIGMA_ABS2 + xs[i]*xs[i]*SIGMA_REL2*SIGMA_REL2)))/(2*Math.PI*xs[i]*SIGMA_REL2*SIGMA_REL2);
            final double score = intensityScore+Math.log(matchScore);
            currentScore += score;
            if (currentScore>=maxScore) {
                maxScore = currentScore;
                maxLenLeft = apex-i;
            }
        }
        currentScore = maxScore;
        // we start at position apex and then extend pattern to both sides
        for (int i=apex+1; i < xs.length; ++i) {
            final double intensityScore = LAMBDA2*xs[i];
            penalty += intensityScore;
            final double delta = xs[i]-ys[i];
            final double matchScore = Math.exp(-(delta*delta)/(2*(SIGMA_ABS2*SIGMA_ABS2 + xs[i]*xs[i]*SIGMA_REL2*SIGMA_REL2)))/(2*Math.PI*xs[i]*SIGMA_REL2*SIGMA_REL2);
            final double score = intensityScore+Math.log(matchScore);
            currentScore += score;
            if (currentScore>=maxScore) {
                maxScore = currentScore;
                maxLenRight = i-apex;
            }
        }
        return maxScore-penalty;
    }

    private static int findApex(DoubleArrayList xs) {
        int apex=0;
        double apexInt = Double.NEGATIVE_INFINITY;
        for (int i=0; i < xs.size(); ++i) {
            if (xs.getDouble(i)>apexInt) {
                apex = i; apexInt = xs.getDouble(i);
            }
        }
        return apex;
    }

    public static double[] normalizedToMax(DoubleArrayList a) {
        final double[] b = a.toDoubleArray();
        if (b.length<1) return b;
        double mx = a.doubleStream().max().orElse(0d);
        for (int k=0; k < b.length; ++k) {
            b[k] /= mx;
        }
        return b;
    }





}
