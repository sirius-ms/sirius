package de.unijena.bioinf.lcms.align;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.MassMap;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.TraceChain;
import de.unijena.bioinf.lcms.trace.TraceNode;
import de.unijena.bioinf.recal.MzRecalibration;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatIterator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

public class TraceAligner {

    private final static float CONF_THRESHOLD = 2;

    private final static float RT_WEIGHT = 4, MZ_WEIGHT=1;

    private final IntensityNormalization normalizer;
    private final Deviation deviation;

    private final ProcessedSample[] samples;

    private final UnivariateFunction[] recalibrationFunctions;


    private final UnivariateFunction[] mzRecalibrationFunctionsAbs, mzRecalibrationFunctionsRel;

    private final double[] rtErrors;

    private final Range<Double> totalRtSpan;

    private MoiSet merged;

    public TraceAligner(IntensityNormalization normalizer, ProcessedSample[] samples) {
        this.normalizer = normalizer;
        this.deviation = new Deviation(10);
        this.samples = samples;
        this.recalibrationFunctions = new UnivariateFunction[samples.length];
        this.mzRecalibrationFunctionsAbs = new UnivariateFunction[samples.length];
        this.mzRecalibrationFunctionsRel = new UnivariateFunction[samples.length];
        this.merged = new MoiSet(samples[0]);
        this.rtErrors = new double[samples.length];
        List<Range<Double>> rtSpans = Arrays.stream(samples).map(ProcessedSample::getRtSpan).toList();
        this.totalRtSpan  = Range.closed(rtSpans.stream().mapToDouble(Range::lowerEndpoint).min().orElse(0),
                rtSpans.stream().mapToDouble(Range::upperEndpoint).min().orElse(1));
    }

    // 1.) sort samples by number of confident traces
    // 2.) merge two samples
        // for each

    public MassOfInterest[] align() {
        // first: sort samples by confidence
        Arrays.sort(samples, Comparator.comparingInt(ProcessedSample::getNumberOfHighQualityTraces).reversed());
        // first sample is the guide, the other samples are attached to
        ProcessedSample start = samples[0];
        start.active();
        merged = extractMoIs(0);
        start.inactive();

        // die Frage ist, ob man zuerst die Rekalibrierung berechnet und die Masses of Interest bestimmt und
        // danach nochmal alle Samples durchgeht... ich glaube aber, das macht Sinn
        // in dem Fall berechnet man das Alignment zweier Samples durch:
        // alle Traces alignieren
        // die alignierten Traces gruppieren. Uns interessieren eigentlich nur die Massen am Ende
        // hat man alle Traces aligniert, mittelt man die Massen der alignierten Samples und nimmt die als
        // Mass of Interest = Bin (Mittelwert, Minimal Mz, Maximal Mz)
        // wir müssen nur Alignierte Cluster, die nah bei einander liegen, in verschiedene Mass of Interest splitten

        // TODO: wenn wir ganze Trace Chains alignieren, riskieren wir natürlich, dass sich Fehler die wir dort
        // gemacht haben fortsetzen.
        for (int k=1; k < samples.length; ++k) {
            samples[k].active();
            MoiSet mois = extractMoIs(k);
            samples[k].inactive();
            merged = preAlign(merged, mois, k);
        }
        int scanIdSpan = 0;
        for (ProcessedSample sample : samples) scanIdSpan = Math.max(scanIdSpan, sample.getMapping().length());
        double rtSpan = this.totalRtSpan.upperEndpoint()-totalRtSpan.lowerEndpoint();
        final double rtPerBin = rtSpan / scanIdSpan;
        final double[] retentionTimeBins = new double[scanIdSpan+1];
        retentionTimeBins[0] = totalRtSpan.lowerEndpoint();;
        for (int k=1; k < retentionTimeBins.length; ++k) {
            retentionTimeBins[k] = retentionTimeBins[k-1]+rtPerBin;
        }

        int j=0;

        MoiSet merged2 = fullAlign();

        // recalibrate masses
        for (int k=0; k < samples.length; ++k) {
            samples[k].active();
            MoiSet mois = extractMoIs(k);
            samples[k].inactive();
            recalibrateMasses(merged2.all, mois.all, k, rtErrors[k], 20 );
        }

        {
            final DoubleArrayList allMasses = new DoubleArrayList();
            for (MassOfInterest m : merged2.all) {
                allMasses.add(m.mz);
            }
            allMasses.sort(null);
            final DoubleArrayList mergedMasses = new DoubleArrayList();
            // merge masses
            int c=1; double d = allMasses.getDouble(0);
            double m=d;
            for (int q = 1; q < allMasses.size(); ++q) {
                final double f = allMasses.getDouble(q);
                if (Math.abs(d-f) < 1e-4) {
                    m += f;
                    c+=1;
                } else {
                    mergedMasses.add(m/c);
                    m=f;d=f;c=1;
                }
            }
            mergedMasses.add(m/c);
            try (final PrintStream out = new PrintStream("/home/kaidu/analysis/test/_mois_.txt")) {
                for (double v : mergedMasses) out.println(v);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        // delete all mois that do not align
        merged2.all.removeIf(x->!(x instanceof MergedMassOfInterest));

        if(false) {
            for (MassOfInterest moi : merged2.all) {
                if (moi instanceof MergedMassOfInterest mmoi) {
                    if (mmoi.mergedRts.length >= (samples.length / 2)) {
                        try (final PrintStream out = new PrintStream("/home/kaidu/analysis/test/" + (j++) + ".txt")) {
                            int count = 1;
                            for (int k = 0; k < samples.length; ++k) {
                                samples[k].active();
                                Deviation dev = new Deviation(5);
                                double delta = dev.absoluteFor(mmoi.mz);
                                List<ContiguousTrace> contigousTracesByMass = samples[k].getTraceStorage().getContigousTracesByMass(mmoi.mz - delta, mmoi.mz + delta);
                                if (contigousTracesByMass.isEmpty()) continue;
                                contigousTracesByMass.sort(Comparator.comparingDouble(x -> Math.abs(x.averagedMz() - mmoi.mz)));
                                ContiguousTrace tr = contigousTracesByMass.get(0);
                                Optional<TraceChain> chainFor = samples[k].getTraceStorage().getChainFor(tr);
                                if (chainFor.isPresent()) {
                                    ++count;
                                    out.print(k);
                                    out.print("\t");
                                    TraceChain C = chainFor.get();
                                    out.print(C.averagedMz());

                                    final double[] intensities = new double[retentionTimeBins.length];
                                    for (int d = C.startId() + 1; d < C.endId(); d += 2) {
                                        UnivariateFunction R = recalibrationFunctions[k];
                                        if (R == null) R = new Identity();
                                        final double recalibratedRetentionTimeA = R.value(C.retentionTime(d - 1));
                                        final double recalibratedRetentionTimeB = R.value(C.retentionTime(d));
                                        final double recalibratedRetentionTimeC = R.value(C.retentionTime(d + 1));
                                        int newIndexA = search(retentionTimeBins, recalibratedRetentionTimeA);
                                        int newIndexB = search(retentionTimeBins, recalibratedRetentionTimeB);
                                        int newIndexC = search(retentionTimeBins, recalibratedRetentionTimeC);
                                        final double intensityA = C.intensity(d - 1), intensityB = C.intensity(d), intensityC = C.intensity(d + 1);
                                        intensities[newIndexA] = intensityA;
                                        intensities[newIndexB] = intensityB;
                                        intensities[newIndexC] = intensityC;
                                        // interpolate
                                        if (newIndexB - newIndexA > 1) {
                                            final double bt = 1d / (newIndexB - newIndexA);
                                            for (int c = newIndexA + 1; c < newIndexB; ++c) {
                                                intensities[c] = intensityA + (intensityB - intensityA) * bt;
                                            }
                                        }
                                        if (newIndexC - newIndexB > 1) {
                                            final double bt = 1d / (newIndexC - newIndexB);
                                            for (int c = newIndexB + 1; c < newIndexC; ++c) {
                                                intensities[c] = intensityB + (intensityC - intensityB) * bt;
                                            }
                                        }
                                    }

                                    for (double intens : intensities) {
                                        out.print("\t");
                                        out.print(intens);
                                    }
                                    out.println();
                                }
                                samples[k].inactive();
                            }
                            System.out.println((j - 1) + ".txt\tcount: " + count);
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        this.merged = merged2;
        return merged.all.toArray(MassOfInterest[]::new);
    }

    public ScanPointMapping getMergedScanPointMapping() {
        int maxLen = 0;
        for (ProcessedSample sample : samples) {
            maxLen = Math.max(sample.getMapping().length(), maxLen);
        }
        final int[] scanids = new int[maxLen];
        final double[] retentionTimes = new double[maxLen];
        final double rtStep = (totalRtSpan.upperEndpoint()-totalRtSpan.lowerEndpoint())/maxLen;
        retentionTimes[0] = totalRtSpan.lowerEndpoint();
        for (int i=1; i < retentionTimes.length; ++i) {
            retentionTimes[i] = retentionTimes[i-1]+rtStep;
            scanids[i] = i;
        }
        return new ScanPointMapping(retentionTimes, scanids);
    }

    public ProcessedSample[] getSamples() {
        return samples;
    }

    public UnivariateFunction[] getRecalibrationFunctions() {
        return recalibrationFunctions;
    }

    public MoiSet getMerged() {
        return merged;
    }

    private static int search(double[] array, double value) {
        int i = Arrays.binarySearch(array, value);
        if (i < 0) {
            int j = -(i+1);
            if (j >= array.length) return j-1;
            else return j;
        }
        return i;
    }

    private MoiSet preAlign(MoiSet init, MoiSet mois, int index) {
        double rtError = alignMostIntensiveTraces(init, mois, index);
        rtErrors[index] = rtError;
        List<PossibleAlignment> alignments = alignGoodTraces(init, mois, rtError, index);
        return mergeMoISets(init.sample, init.good, mois.good, alignments);
    }
    private MoiSet fullAlign() {
        // 1.) delete all mois that could not be aligned to anything
        merged.all.removeIf(x->!(x instanceof MergedMassOfInterest));
        merged.good.removeIf(x->!(x instanceof MergedMassOfInterest));
        merged.high.removeIf(x->!(x instanceof MergedMassOfInterest));
        System.out.println("guidance has " + merged.all.size() + " mois.");
        // use this one as guidance and align everything against it
        rtErrors[0] = Statistics.robustAverage(rtErrors);
        for (int k=0; k < samples.length; ++k) {
            samples[k].active();
            MoiSet mois = extractMoIs(k);
            samples[k].inactive();
            List<PossibleAlignment> alignments = alignTraces(merged, mois, rtErrors[k], k);
            merged = mergeMoISets(samples[0], merged.all, mois.all, alignments);
        }
        return merged;
    }


    private List<PossibleAlignment> alignTraces(MoiSet left, MoiSet right, double rtstd, int sampleIndex) {
        final ArrayList<MassOfInterest> L = left.all, R = right.all;
        // get retentiontime span
        double rtspanRight = retentionTimeSpan(right);
        List<PossibleAlignment> chosenAlignment = alignSets(L, R, rtstd, 2d);
        MassOfInterest[] alignments = getFromAlignment(L, R, chosenAlignment);
        double mzError = 0d, intError = 0d, rtError = 0d;

        PossibleAlignment[] byRtError = chosenAlignment.toArray(PossibleAlignment[]::new);
        List<PossibleAlignment> chosenForRecalibration = new ArrayList<>();
        Arrays.sort(byRtError, Comparator.comparingDouble(x->x.retentionTimeError));
        int startFrom = (int)Math.ceil(byRtError.length*0.05);
        int endAt = (int)Math.floor(byRtError.length*0.95);
        for (int k=startFrom; k < endAt; ++k) {
            PossibleAlignment al = byRtError[k];
            mzError += Math.abs(L.get(al.left).getMz() - R.get(al.right).getMz());
            intError += Math.abs(Math.log((L.get(al.left).getRelativeIntensity()+0.1)/(R.get(al.right).getRelativeIntensity()+0.1)));
            rtError += Math.abs(al.retentionTimeError);
            chosenForRecalibration.add(al);
        }
        final int n = alignments.length/2;
        System.out.println("mzError = " + mzError/n);
        System.out.println("intError = " + Math.exp(intError/n));
        System.out.println("rtError = " + rtError/n);
        System.out.println("Number of Alignments: " + chosenAlignment.size() + " / " + Math.min(L.size(),R.size()));
        System.out.println("----------------------------------------");
        /*{
            PossibleAlignment[] byMz = chosenAlignment.toArray(PossibleAlignment[]::new);
            Arrays.sort(byRtError, Comparator.comparingDouble(x->L.get(x.left).mz));
            for (PossibleAlignment al : byMz) {
                System.out.println("align " + L.get(al.left) + " with " + R.get(al.right).mz);
            }
        }
         */
        // learn a linear recalibration function if at least 4 data points are available per quantile
        chosenForRecalibration.sort(Comparator.comparingDouble(x->R.get(x.right).getUncalibratedRetentionTime()));
        MassOfInterest[] rec = getFromAlignment(L, R, chosenForRecalibration);
        recalibrateByAlignment(right, sampleIndex, rec, rtspanRight, rtError);
        return chosenAlignment;
    }

    private void recalibrateMasses(List<MassOfInterest> fromMerged, List<MassOfInterest> mois, int sampleIndex, double rtTolerance, double intTolerance) {
        // we have to do a second alignment pass :/
        // maybe we should store the aligned mois in db to avoid that
        final List<MassOfInterest>mergedSet = fromMerged.stream().filter(x->x instanceof MergedMassOfInterest).toList();
        List<PossibleAlignment> aligned = alignSets(mergedSet, mois, rtTolerance, intTolerance);
        aligned.sort(Comparator.comparingDouble(x->mergedSet.get(x.left).getRt()));
        // absolute
        {
            DoubleArrayList xs = new DoubleArrayList(), ys = new DoubleArrayList();

            for (int k=0; k < aligned.size(); ++k) {
                double l = mois.get(aligned.get(k).right).mz;
                double r = mergedSet.get(aligned.get(k).left).mz;
                double rt = mergedSet.get(aligned.get(k).left).rt;
                if (l <= 200) {
                    xs.add(rt);
                    ys.add(r-l);
                }
            }
            mzRecalibrationFunctionsAbs[sampleIndex] = xs.size() >= 50 ? MzRecalibration.getMedianLinearRecalibration(xs.toDoubleArray(), ys.toDoubleArray()) : new Identity();
        }
        // relative
        {
            DoubleArrayList xs = new DoubleArrayList(), ys = new DoubleArrayList();
            for (int k=0; k < aligned.size(); ++k) {
                double l = mois.get(aligned.get(k).right).mz;
                double r = mergedSet.get(aligned.get(k).left).mz;
                double rt = mergedSet.get(aligned.get(k).left).rt;
                if (l > 200) {
                    xs.add(rt);
                    ys.add(r/l);
                }
            }
            mzRecalibrationFunctionsRel[sampleIndex] = xs.size() >= 50 ? MzRecalibration.getMedianLinearRecalibration(xs.toDoubleArray(), ys.toDoubleArray()) : new Identity();
        }
    }

    private void recalibrateByAlignment(MoiSet right, int sampleIndex, MassOfInterest[] rec, double rtspanRight, double rtError) {
        int minimumBuckSize = getMinimumNumberOfSamplePointsPerRegion(rec, right.sample.getMapping(), rtspanRight);
        if (minimumBuckSize > 1 && minimumBuckSize < 25) {
            System.out.println("Recalibrate with linear recalibration");
            double[] x = new double[rec.length / 2];
            double[] y = x.clone();
            for (int k = 0; k < rec.length; k += 2) {
                y[k / 2] = rec[k].getRt();
                x[k / 2] = rec[k + 1].getUncalibratedRetentionTime();
            }
            {
                System.out.println("Recalibrate " + x.length + " values in total!");
                try (final PrintStream p = new PrintStream("/home/kaidu/testx_" + sampleIndex + ".out")) {
                    p.println("x\ty");
                    for (int k=0; k < x.length; ++k) {
                        p.println(x[k] + "\t" + y[k]);
                    }
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                PolynomialFunction medianLinearRecalibration;
                if (x.length < 1000) {
                    medianLinearRecalibration = MzRecalibration.getMedianLinearRecalibration(x, y);
                } else {
                    medianLinearRecalibration = MzRecalibration.getLinearRecalibration(x, y);
                }
                System.out.println(medianLinearRecalibration);
                {
                    double rtError2 = 0d;
                    for (int k = 0; k < rec.length; k += 2) {
                        rtError2 += Math.abs(rec[k].getRt() - medianLinearRecalibration.value(rec[k + 1].getUncalibratedRetentionTime()));
                    }
                    final int n2 = rec.length / 2;
                    System.out.println("rtError after linear recalibration = " + rtError2 / n2);
                    if (rtError2 < rtError *0.85) {
                        System.out.println("Apply recalibration");
                        right.applyRecalibration(medianLinearRecalibration);
                        recalibrationFunctions[sampleIndex] = medianLinearRecalibration;
                    }
                }
            }
        } else if (minimumBuckSize>=25) {
            double linearRtError, NoCalRtError, LoessRtError;
            UnivariateFunction linearRecalibration;
            double[] x,y;
            {
                System.out.println("Recalibrate with LOESS recalibration");
                x = new double[rec.length / 2];
                y = x.clone();
                for (int k = 0; k < rec.length; k += 2) {
                    y[k / 2] = rec[k].getRt();
                    x[k / 2] = rec[k + 1].getUncalibratedRetentionTime();
                }
                PolynomialFunction f;
                if (x.length < 1000) {
                    f = MzRecalibration.getMedianLinearRecalibration(x, y);
                } else {
                    f = MzRecalibration.getLinearRecalibration(x, y);
                }
                linearRecalibration = f;
                System.out.println("Linear recalibration: " + f);
                {
                    double rtError1 = 0d, rtError2 = 0d;
                    for (int k = 0; k < rec.length; k += 2) {
                        rtError1 += Math.abs(rec[k].getUncalibratedRetentionTime() - rec[k+1].getUncalibratedRetentionTime());
                        rtError2 += Math.abs(rec[k].getUncalibratedRetentionTime() - f.value(rec[k + 1].getUncalibratedRetentionTime()));
                    }
                    final int n2 = rec.length / 2;
                    System.out.println("rtError before linear recalibration = " + rtError1 / n2);
                    System.out.println("rtError after linear recalibration = " + rtError2 / n2);
                    linearRtError = rtError2;
                    NoCalRtError = rtError1;

                }
                {
                    try (final PrintStream p = new PrintStream("/home/kaidu/test.out")) {
                        p.println("x\ty");
                        for (int k=0; k < x.length; ++k) {
                            p.println(x[k] + "\t" + y[k]);
                        }
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }

                // get start values for x and y
                double xstart = Math.min(0, totalRtSpan.lowerEndpoint()-1);
                double xend = totalRtSpan.upperEndpoint()+1;
                double[][] vals = strictMonotonic(x,y);//, xstart, linearRecalibration.value(xstart), xend, linearRecalibration.value(xend));
                x = vals[0];
                y = vals[1];
                {
                    try (final PrintStream p = new PrintStream("/home/kaidu/test_" + sampleIndex + ".out")) {
                        p.println("x\ty");
                        for (int k=0; k < x.length; ++k) {
                            p.println(x[k] + "\t" + y[k]);
                        }
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            {
                double bandwidth = Math.min(0.3, Math.max(0.1, (200d / x.length)));
                System.out.println("Recalibrate " + x.length + " values in total! (bandwidth = " + bandwidth + ")");
                PolynomialSplineFunction loess = new LoessInterpolator(bandwidth, 2).interpolate(x, y);
                System.out.println("Loess with " + loess.getN() + " knots.");
                UnivariateFunction loessRecalibration = RecalibrationFunction.fromLoess(
                        loess, linearRecalibration) ;
                {
                    double rtError2 = 0d;
                    for (int k = 0; k < rec.length; k += 2) {
                        rtError2 += Math.abs(rec[k].getUncalibratedRetentionTime() - loessRecalibration.value(rec[k + 1].getUncalibratedRetentionTime()));
                    }
                    final int n2 = rec.length / 2;
                    System.out.println("rtError after LOESS recalibration = " + rtError2 / n2);
                    LoessRtError = rtError2;

                    if (LoessRtError < linearRtError) {
                        System.out.println("Apply LOESS recalibration");
                        right.applyRecalibration(loessRecalibration);
                        recalibrationFunctions[sampleIndex] = loessRecalibration;
                    } else if (linearRtError < NoCalRtError) {
                        System.out.println("Apply linear recalibration");
                        right.applyRecalibration(linearRecalibration);
                        recalibrationFunctions[sampleIndex] = loessRecalibration;
                    } else {
                        System.out.println("Apply NO recalibration");
                        right.applyRecalibration(new Identity());
                        recalibrationFunctions[sampleIndex] = new Identity();
                    }
                }
            }
        }
    }

    private List<PossibleAlignment> alignGoodTraces(MoiSet left, MoiSet right, double rtstd, int sampleIndex) {
        final ArrayList<MassOfInterest> L = left.good, R = right.good;
        // get retentiontime span
        double rtspanRight = retentionTimeSpan(right);
        List<PossibleAlignment> chosenAlignment = alignSets(L, R, rtstd, 2d);
        MassOfInterest[] alignments = getFromAlignment(L, R, chosenAlignment);
        double mzError = 0d, intError = 0d, rtError = 0d;

        PossibleAlignment[] byRtError = chosenAlignment.toArray(PossibleAlignment[]::new);
        List<PossibleAlignment> chosenForRecalibration = new ArrayList<>();
        Arrays.sort(byRtError, Comparator.comparingDouble(x->x.retentionTimeError));
        final double medianRtError = medianRtError(chosenAlignment);
        int startFrom = (int)Math.ceil(byRtError.length*0.05);
        int endAt = (int)Math.floor(byRtError.length*0.95);
        for (int k=startFrom; k < endAt; ++k) {
            PossibleAlignment al = byRtError[k];
            mzError += Math.abs(L.get(al.left).getMz() - R.get(al.right).getMz());
            intError += Math.abs(Math.log((L.get(al.left).getRelativeIntensity()+0.1)/(R.get(al.right).getRelativeIntensity()+0.1)));
            rtError += Math.abs(al.retentionTimeError);
            chosenForRecalibration.add(al);
        }
        final int n = alignments.length/2;
        System.out.println("mzError = " + mzError/n);
        System.out.println("intError = " + Math.exp(intError/n));
        System.out.println("rtError = " + rtError/n);
        System.out.println("median rtError = " + medianRtError);
        System.out.println("----------------------------------------");
        {
            PossibleAlignment[] byMz = chosenAlignment.toArray(PossibleAlignment[]::new);
            Arrays.sort(byRtError, Comparator.comparingDouble(x->L.get(x.left).mz));
            for (PossibleAlignment al : byMz) {
                System.out.println("align " + L.get(al.left) + " with " + R.get(al.right));
            }
        }
        // learn a linear recalibration function if at least 4 data points are available per quantile
        chosenForRecalibration.sort(Comparator.comparingDouble(x->R.get(x.right).getUncalibratedRetentionTime()));
        MassOfInterest[] rec = getFromAlignment(L, R, chosenForRecalibration);
        recalibrateByAlignment(right, sampleIndex, rec, rtspanRight, rtError);
        rtErrors[sampleIndex] = medianRtError;
        return chosenAlignment;
    }

    private double medianRtError(List<PossibleAlignment> chosenAlignment) {
        return median(chosenAlignment.stream().mapToDouble(x->Math.abs(x.retentionTimeError)).toArray());
    }
    private static double median(double[] xs) {
        if (xs.length==1) return xs[0];
        Arrays.sort(xs);
        if (xs.length%2==0) {
            double a = xs[xs.length/2 - 1], b = xs[xs.length/2];
            return (a+b)/2d;
        } else {
            return xs[xs.length/2];
        }
    }


    private double alignMostIntensiveTraces(MoiSet left, MoiSet right, int index) {
        final ArrayList<MassOfInterest> L = left.high, R = right.high;
        // get retentiontime span
        double rtspanRight = retentionTimeSpan(right);
        double allowedRt = (retentionTimeSpan(left)+ rtspanRight)/16d;
        List<PossibleAlignment> chosenAlignment = alignSets(L, R, allowedRt, 0.5d);
        MassOfInterest[] alignments = getFromAlignment(L, R, chosenAlignment);
        double mzError = 0d, intError = 0d, rtError = 0d;

        PossibleAlignment[] byRtError = chosenAlignment.toArray(PossibleAlignment[]::new);
        List<PossibleAlignment> chosenForRecalibration = new ArrayList<>();
        Arrays.sort(byRtError, Comparator.comparingDouble(x->x.retentionTimeError));
        int startFrom = (int)Math.ceil(byRtError.length*0.05);
        int endAt = (int)Math.floor(byRtError.length*0.95);
        for (int k=startFrom; k < endAt; ++k) {
            PossibleAlignment al = byRtError[k];
            mzError += Math.abs(L.get(al.left).getMz() - R.get(al.right).getMz());
            intError += Math.abs(Math.log((L.get(al.left).getRelativeIntensity()+0.1)/(R.get(al.right).getRelativeIntensity()+0.1)));
            rtError += Math.abs(al.retentionTimeError);
            chosenForRecalibration.add(al);
        }
        if (chosenAlignment.isEmpty()) {
            System.out.println("Alignment is empty.");
            return allowedRt;
        }
        final double medianRtError = medianRtError(chosenAlignment);
        final int n = alignments.length/2;
        System.out.println("mzError = " + mzError/n);
        System.out.println("intError = " + Math.exp(intError/n));
        System.out.println("rtError = " + rtError/n);
        System.out.println("median rtError = " + medianRtError);
        System.out.println("----------------------------------------");

        // learn a linear recalibration function if at least 4 data points are available per quantile
        chosenForRecalibration.sort(Comparator.comparingDouble(x->R.get(x.right).getUncalibratedRetentionTime()));
        MassOfInterest[] rec = getFromAlignment(L, R, chosenForRecalibration);
        recalibrateByAlignment(right, index, rec, rtspanRight, rtError);
        return medianRtError;
    }

    private int getMinimumNumberOfSamplePointsPerRegion(MassOfInterest[] alignments, ScanPointMapping mp, double rtspanRight) {
        int[] counts = new int[]{0,0,0,0};
        for (int k=0; k < alignments.length; k += 2) {
            int rt = (int)Math.min(3, Math.floor((alignments[k+1].getRt()-mp.getRetentionTimeAt(0)) * 4)/rtspanRight);
            counts[rt]++;
        }
        System.out.println(Arrays.toString(counts));
        int minc = Integer.MAX_VALUE;
        for (int c : counts) {
            minc = Math.min(minc, c);
        }
        return minc;
    }

    private double retentionTimeSpan(MoiSet left) {
        ScanPointMapping m = left.sample.getMapping();
        return m.getRetentionTimeAt(m.length()-1) - m.getRetentionTimeAt(0);
    }

    private MassOfInterest[] getFromAlignment(ArrayList<MassOfInterest> left, ArrayList<MassOfInterest> right, List<PossibleAlignment> chosenAlignments) {
        List<MassOfInterest> alignments = new ArrayList<>();
        for (PossibleAlignment a : chosenAlignments) {
            alignments.add(left.get(a.left));
            alignments.add(right.get(a.right));
        }
        return alignments.toArray(MassOfInterest[]::new);
    }

    private List<PossibleAlignment> alignSetsWrong(ArrayList<MassOfInterest> left, ArrayList<MassOfInterest> right, double retentionTimeTolerance,
                                       double intensityTolerance

                                       ) {
        final double retentionTimeWindow = retentionTimeTolerance*8;
        final double rtVar = 2*retentionTimeTolerance;
        final double intensityVar = 2*intensityTolerance*intensityTolerance;
        final ArrayList<PossibleAlignment> possibleAlignments = new ArrayList<>();
        int l=0, r=0;
        while (l < left.size() && r < right.size()) {
            final MassOfInterest ml = left.get(l), mr = right.get(r);
            final double delta = ml.getMz()-mr.getMz();
            final double allowedDelta = deviation.absoluteFor((ml.getMz()+mr.getMz())/2);
            if (delta < -allowedDelta) { // ml is smaller than mr
                ++l;
            } else if (delta > allowedDelta) { // ml is bigger than mr
                ++r;
            } else {
                final double retentionTimeDelta = ml.getRt()-mr.getRt();
                final double intensityDelta = Math.log((ml.getRelativeIntensity()+0.1)/(mr.getRelativeIntensity()+0.1));
                if (Math.abs(retentionTimeDelta) < retentionTimeWindow) {
                    final float loglikelihood = (float)((-Math.abs(retentionTimeDelta)/rtVar)*RT_WEIGHT +
                            (-(delta*delta)/(2*allowedDelta*allowedDelta)) + (-(intensityDelta*intensityDelta)/(intensityVar)));
                    if (loglikelihood > -(20)) {
                        possibleAlignments.add(new PossibleAlignment(l, r, loglikelihood, retentionTimeDelta));
                    }
                }
                ++l; ++r;
            }
        }
        possibleAlignments.sort(null);
        final BitSet alignedL = new BitSet(left.size()), alignedR = new BitSet(right.size());
        final ArrayList<PossibleAlignment> chosenAlignments = new ArrayList<>();
        for (PossibleAlignment a : possibleAlignments) {
            if (!alignedL.get(a.left) && !alignedR.get(a.right)) {
                chosenAlignments.add(a);
                alignedL.set(a.left);
                alignedR.set(a.right);
            }
        }
        chosenAlignments.sort(Comparator.comparingDouble(x->right.get(x.right).getRt()));
        return chosenAlignments;
        /*
        for (PossibleAlignment a : chosenAlignments) {
            alignments.add(left.get(a.left));
            alignments.add(right.get(a.right));
        }
        return alignments.toArray(MassOfInterest[]::new);
         */
    }


    private List<PossibleAlignment> alignSets(List<MassOfInterest> left, List<MassOfInterest> right, double retentionTimeTolerance,
                                                   double intensityTolerance

    ) {
        final double retentionTimeWindow = retentionTimeTolerance * 8;
        final double rtVar = 2 * retentionTimeTolerance;
        final double intensityVar = 2 * intensityTolerance * intensityTolerance;
        final ArrayList<PossibleAlignment> possibleAlignments = new ArrayList<>();
        final IntArrayList[] rightLookup = new IntArrayList[1 + right.stream().mapToInt(x -> (int) (x.mz)).max().orElse(0)];
        for (int j = 0; j < right.size(); ++j) {
            int i = (int) right.get(j).mz;
            if (rightLookup[i] == null) rightLookup[i] = new IntArrayList();
            rightLookup[i].add(j);
        }

        for (int l = 0; l < left.size(); ++l) {
            final MassOfInterest ml = left.get(l);
            IntIterator intIterator = potentialMatches(ml.mz, rightLookup);
            while (intIterator.hasNext()) {
                final int r = intIterator.nextInt();
                final MassOfInterest mr = right.get(r);
                final double delta = ml.getMz() - mr.getMz();
                final double allowedDelta = deviation.absoluteFor((ml.getMz() + mr.getMz()) / 2);
                if (Math.abs(delta) < allowedDelta) {
                    final double retentionTimeDelta = ml.getRt() - mr.getRt();
                    final double intensityDelta = Math.log((ml.getRelativeIntensity() + 0.1) / (mr.getRelativeIntensity() + 0.1));
                    if (Math.abs(retentionTimeDelta) < retentionTimeWindow) {
                        final float loglikelihood = (float) ((-Math.abs(retentionTimeDelta) / rtVar) * RT_WEIGHT +
                                (-(delta * delta) / (2 * allowedDelta * allowedDelta)) + (-(intensityDelta * intensityDelta) / (intensityVar)));
                        if (loglikelihood > -(20)) {
                            possibleAlignments.add(new PossibleAlignment(l, r, loglikelihood, retentionTimeDelta));
                        }
                    }
                }
            }
        }
        possibleAlignments.sort(null);
        final BitSet alignedL = new BitSet(left.size()), alignedR = new BitSet(right.size());
        final ArrayList<PossibleAlignment> chosenAlignments = new ArrayList<>();
        for (PossibleAlignment a : possibleAlignments) {
            if (!alignedL.get(a.left) && !alignedR.get(a.right)) {
                chosenAlignments.add(a);
                alignedL.set(a.left);
                alignedR.set(a.right);
            }
        }
        chosenAlignments.sort(Comparator.comparingDouble(x -> right.get(x.right).getRt()));
        return chosenAlignments;
    }

    private IntIterator potentialMatches(double mz, IntArrayList[] lookupTable) {
        final double delta = deviation.absoluteFor(mz+1);
        final int mzLeftKey = (int)(mz - delta), mzRightKey = (int)(mz+delta);
        if (mzLeftKey >= lookupTable.length) return IntIterators.EMPTY_ITERATOR;
        IntArrayList L = lookupTable[mzLeftKey];
        if (mzLeftKey==mzRightKey) {
            if (L ==null) return IntIterators.EMPTY_ITERATOR;
            else return L.iterator();
        } else {
            IntArrayList R = mzRightKey>=lookupTable.length ? null : lookupTable[mzRightKey];
            if (L ==null){
                if (R ==null) return IntIterators.EMPTY_ITERATOR;
                else return R.iterator();
            } else if (R ==null) {
                return L.iterator();
            } else {
                return IntIterators.concat(L.iterator(), R.iterator());
            }
        }
    }

    private double[][] strictMonotonic(double[] x, double[] y, double xstart, double ystart, double xend, double yend){
        if (xstart > x[0]) {
            xstart = x[0]; ystart = y[0];
        }
        if (xend < x[x.length-1]) {
            xend = x[x.length-1];
            yend = y[y.length-1];
        }
        DoubleArrayList xs = new DoubleArrayList(x.length), ys = new DoubleArrayList(y.length);
        double ymean=ystart; int c=1;
        for (int i=0; i < y.length; ++i) {
            if (x[i] > xstart) {
                xs.add(xstart);
                ys.add(ymean/c);
                xstart = x[i];
                ymean = y[i];
                c = 1;
            } else {
                ymean += y[i];
                c += 1;
            }
        }
        if (xend > xstart) {
            xs.add(xstart);
            ys.add(ymean/c);
            xs.add(xend);
            ys.add(yend);
        } else {
            xs.add(xstart);
            ys.add((ymean+yend)/(c+1));
        }


        return new double[][]{xs.toDoubleArray(), ys.toDoubleArray()};
    }
    private double[][] strictMonotonic(double[] x, double[] y){
        DoubleArrayList xs = new DoubleArrayList(x.length), ys = new DoubleArrayList(y.length);
        double xstart=x[0];
        double ymean=y[0]; int c=1;
        for (int i=1; i < y.length; ++i) {
            if (x[i] > xstart) {
                xs.add(xstart);
                ys.add(ymean/c);
                xstart = x[i];
                ymean = y[i];
                c = 1;
            } else {
                ymean += y[i];
                c += 1;
            }
        }
        xs.add(xstart);
        ys.add(ymean/c);
        return new double[][]{xs.toDoubleArray(), ys.toDoubleArray()};
    }

    private MoiSet extractMoIs(int index) {
        final ProcessedSample sample = samples[index];
        final MoiSet set = new MoiSet(sample);
        for (Iterator<TraceChain> it = sample.getTraceStorage().chains(); it.hasNext(); ) {
            TraceChain chain = it.next();
            for (ContiguousTrace trace : chain.getTraces()) {
                final TraceNode node = sample.getTraceStorage().getTraceNode(trace.getUid());
                if (node.getApexes()==null) continue; // might happen for isotope peaks
                for (int apex : node.getApexes()) {
                    final float intensity = trace.intensity(apex);
                    final double retentionTime = trace.retentionTime(apex);
                    MassOfInterest moi = new MassOfInterest(chain.averagedMz(), retentionTime,
                            retentionTime - chain.retentionTime(chain.startId()),
                            chain.retentionTime(chain.endId()) - retentionTime,
                            chain.minMz(),
                            chain.maxMz(),
                            intensity, trace.getUid(), apex, index, 0);
                    if (recalibrationFunctions[index]!=null) moi.recalibrate(recalibrationFunctions[index]);
                    if (node.getConfidence()>=CONF_THRESHOLD) {
                        set.good.add(moi);
                        moi.setQualityLevel(1);
                    }
                    set.all.add(moi);
                }
            }
        }
        // normalize
        IntensityNormalization.Normalizer normalization = normalizer.getNormalizer(() -> new FloatIterator() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < set.all.size();
            }

            @Override
            public float nextFloat() {
                return set.all.get(i++).getRelativeIntensity();
            }
        });
        FloatArrayList fls = new FloatArrayList();
        for (MassOfInterest moi : set.all) {
            moi.normalize(normalization);
            fls.add(moi.getRelativeIntensity());
        }
        fls.sort(null);
        final float threshold = fls.getFloat((int)Math.floor(fls.size()*0.9));
        // add high intensity mois
        for (MassOfInterest moi : set.good) {
            if (moi.getRelativeIntensity() >= threshold) {
                set.high.add(moi);
                moi.setQualityLevel(2);
            }
        }
        set.all.sort(Comparator.comparingDouble(MassOfInterest::getMz));
        set.high.sort(Comparator.comparingDouble(MassOfInterest::getMz));
        set.good.sort(Comparator.comparingDouble(MassOfInterest::getMz));
        System.out.println(set.all.size() + " mois in total ");
        System.out.println(set.good.size() + " mois with good quality");
        System.out.println(set.high.size() + " mois with high intensity and hood quality ");
        return set;
    }

    protected static class MoiSet {
        private final ArrayList<MassOfInterest> all, good, high;
        private final ProcessedSample sample;

        public MoiSet(ProcessedSample sample) {
            this.sample = sample;
            all = new ArrayList<>();
            good = new ArrayList<>();
            high = new ArrayList<>();
        }

        public void applyRecalibration(UnivariateFunction f) {
            for (MassOfInterest moi : all) moi.recalibrate(f);
        }
    }

    public MoiSet mergeMoISets(ProcessedSample initSample, List<MassOfInterest> left, List<MassOfInterest> right, List<PossibleAlignment> alignments) {
        MoiSet merged = new MoiSet(initSample);
        final HashSet<MassOfInterest> used = new HashSet<>();
        for (PossibleAlignment al : alignments) {
            used.add(left.get(al.left));
            used.add(right.get(al.right));
            merged.all.add(new MergedMassOfInterest(left.get(al.left), right.get(al.right)));
        }
        for (MassOfInterest a : left) {
            if (!used.contains(a)) {
                merged.all.add(a);
            }
        }
        for (MassOfInterest a : right) {
            if (!used.contains(a)) {
                merged.all.add(a);
            }
        }
        merged.all.sort(Comparator.comparingDouble(x->x.mz));
        for (MassOfInterest m : merged.all) {
            if (m.qualityLevel>=1) merged.good.add(m);
            if (m.qualityLevel>=2) merged.high.add(m);
        }
        return merged;
    }

    public static class MergedMassOfInterest extends MassOfInterest {
        public double[] mergedRts;

        public MassOfInterest[] mergedForDebug;

        public MergedMassOfInterest(MassOfInterest left, MassOfInterest right) {
            super(left.getMz(), left.getUncalibratedRetentionTime(),
                    Math.max(left.widthLeft, right.widthLeft),
                    Math.max(left.widthRight, right.widthRight),
                    Math.min(left.getMinMz(), right.getMinMz()),
                    Math.max(left.getMaxMz(), right.getMaxMz()),
                    left.relativeIntensity, left.getTraceId(), left.getScanId(), -1, left.qualityLevel);
            if (left instanceof MergedMassOfInterest) {
                MergedMassOfInterest m = (MergedMassOfInterest)left;
                this.mergedRts = Arrays.copyOf(m.mergedRts, m.mergedRts.length+1);
                this.mergedRts[mergedRts.length-1]= right.getRt();

                this.mergedForDebug = Arrays.copyOf(m.mergedForDebug, m.mergedForDebug.length+1);
                this.mergedForDebug[this.mergedForDebug.length-1] = right;
            } else {
                this.mergedRts = new double[]{left.getRt(), right.getRt()};
                this.mergedForDebug = new MassOfInterest[]{left, right};
            }
            this.rt = 0;
            for (double  r : mergedRts) this.rt += r;
            this.rt /= mergedRts.length;
        }

        @Override
        protected void recalibrate(UnivariateFunction f) {
            throw new RuntimeException("Never recalibrate the merged moi!");
        }
    }

    protected static class PossibleAlignment implements Comparable<PossibleAlignment>{
        private final int left, right;
        private final float score;
        private final double retentionTimeError;

        public PossibleAlignment(int left, int right, float score, double retentionTimeError) {
            this.left = left;
            this.right = right;
            this.score = score;
            this.retentionTimeError = retentionTimeError;
        }

        @Override
        public int compareTo(@NotNull TraceAligner.PossibleAlignment o) {
            return Float.compare(o.score, score);
        }
    }

}
