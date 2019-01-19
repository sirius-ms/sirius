package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.HasParameters;
import de.unijena.bioinf.ChemistryBase.algorithm.Parameter;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2IsotopePatternMatch;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FastIsotopePatternGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FragmentIsotopeGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.IsotopePatternGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MassDeviationScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MassDifferenceDeviationScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MissingPeakScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.NormalDistributedIntensityScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.util.PiecewiseLinearFunctionIntensityDependency;
import de.unijena.bioinf.sirius.PeakAnnotation;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.SpectralRecalibration;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Identity;
import org.apache.commons.math3.special.Erf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@HasParameters
public class IsotopePatternInMs2Scorer {


    private static final double MULTIPLIER = 1d/10d;

    private static final boolean USE_FRAGMENT_ISOGEN = false;


    @Parameter
    protected double baselineAbsoluteIntensity = 500;

    public double getBaselineAbsoluteIntensity() {
        return baselineAbsoluteIntensity;
    }

    public void setBaselineAbsoluteIntensity(double baselineAbsoluteIntensity) {
        this.baselineAbsoluteIntensity = baselineAbsoluteIntensity;
    }

    /**
     *
     * @param input
     * @param graph
     */
    public void score(ProcessedInput input, FGraph graph) {
        final SpectralRecalibration recalibration = graph.getAnnotation(SpectralRecalibration.class, SpectralRecalibration.none());
        final Ms2Experiment experiment = input.getExperimentInformation();
        final List<MutableMs2Spectrum> ms2Spectra = new ArrayList<>(experiment.getMs2Spectra());
        for (int k=0; k < ms2Spectra.size(); ++k) {
            ms2Spectra.set(k, new MutableMs2Spectrum(ms2Spectra.get(k)));
            Spectrums.sortSpectrumByMass(ms2Spectra.get(k));
        }
        final Deviation peakDev = experiment.getAnnotationOrDefault(MS2MassDeviation.class).allowedMassDeviation;
        final Deviation shiftDev = peakDev.divide(2);
        // 1. for each fragment compute Isotope Pattern and match them against raw spectra
        final FastIsotopePatternGenerator generator = new FastIsotopePatternGenerator(Normalization.Sum(1d));
        final TIntArrayList ids = new TIntArrayList(5);
        final TDoubleArrayList scores = new TDoubleArrayList(5);
        final ArrayList<SimpleSpectrum> patterns = new ArrayList<SimpleSpectrum>(5);
        final FragmentAnnotation<ProcessedPeak> peakAno = graph.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        final FragmentAnnotation<Ms2IsotopePatternMatch> isoAno =  graph.addFragmentAnnotation(Ms2IsotopePatternMatch.class);
        final FragmentAnnotation<IsotopicMarker> pseudoAno = graph.getOrCreateFragmentAnnotation(IsotopicMarker.class);
        final FragmentAnnotation<Ionization> ionizationAno = graph.getFragmentAnnotationOrThrow(Ionization.class);
        // find patterns and score

        //////////
        final double sigmaAbs; // absolute error depending relatively on the base peak
        {
            double abs = 0d;
            double sig = 0.01;
            for (int k=0; k < input.getMergedPeaks().size(); ++k) {
                final ProcessedPeak p = input.getMergedPeaks().get(k);
                if (p.isSynthetic()) continue;
                for (Peak ap : p.getOriginalPeaks()) abs = Math.max(ap.getIntensity(),abs);
            }
            sigmaAbs = abs*sig;
        }

        final MolecularFormula ms1Formula = graph.getRoot().getChildren(0).getFormula();
        final FragmentIsotopeGenerator fisogen = new FragmentIsotopeGenerator();
        IsolationWindow isolationWindow = input.getExperimentInformation().getAnnotation(IsolationWindow.class);
        final SimpleSpectrum ms1Pattern;
        assert graph.getRoot().getChildren().size()==1;
        if (isolationWindow!=null){
            Ionization ion = ionizationAno.get(graph.getRoot().getChildren(0));
            ms1Pattern = isolationWindow.transform(generator.simulatePattern(ms1Formula, ion), input.getExperimentInformation().getIonMass());
        } else {
            Ionization ion = ionizationAno.get(graph.getRoot().getChildren(0));
            ms1Pattern = findMs1PatternInMs2(input, graph, generator, ms2Spectra, ion);
        }


        ////////////

        final ArrayList<Fragment> isoFrags = new ArrayList<Fragment>();
        for (Fragment f : graph) {
            if (f.getFormula()!=null && !f.getFormula().isEmpty()) {
                final SimpleSpectrum simulated;
                Ionization ion = ionizationAno.get(f);
                if (ms1Pattern!=null) {
                    if (f.getFormula().equals(ms1Formula)) {
                        simulated = ms1Pattern;
                    } else {
                        simulated = normalizeByFirstPeak(fisogen.simulatePattern(ms1Pattern, ms1Formula, ms1Formula.subtract(f.getFormula()), ion,true));
                    }
                    /////// DEBUG
                    /*
                    System.out.println(f.getCandidate());
                    System.out.println(simulated);
                    System.out.println(normalizeByFirstPeak(generator.simulatePattern(f.getCandidate(), ion)));
                    */
                    /////////////

                } else {
                    simulated = normalizeByFirstPeak(generator.simulatePattern(f.getFormula(), ion));
                }

                // match simulated spectrum against MS/MS spectra
                ids.resetQuick();
                scores.resetQuick();
                patterns.clear();
                // TODO: ensure that MS/MS spectra are ordered by mass
                // TODO: maybe use original MS/MS spectra to avoid prefiltering?
                int msmsId=-1;
                eachSpec:
                for (MutableMs2Spectrum msms : input.getExperimentInformation().getMs2Spectra()) {
                    final UnivariateFunction F = recalibration.getRecalibrationFunctionFor(msms);
                    ++msmsId;
                    final double maxIntensity = Spectrums.getMaximalIntensity(msms);
                    final int index = Spectrums.mostIntensivePeakWithin(msms, simulated.getMzAt(0), peakDev);
                    if (index < 0) {
                        continue;
                    }
                    final SimpleSpectrum foundPattern = extractPattern(peakDev, shiftDev, simulated, msms, maxIntensity, index, F);
                    if (foundPattern.size() <= 1) continue ;
                    final double[] pkscores = new double[foundPattern.size()];
                    final double baselineAbs = Math.max(baselineAbsoluteIntensity/msms.getIntensityAt(index), sigmaAbs/msms.getIntensityAt(index));
                    double score = scorePatternPeakByPeak(simulated, foundPattern, pkscores, peakAno.get(f).getRelativeIntensity(), baselineAbs);
                    if (score <= 0) continue ;
                    ids.add(msmsId);
                    scores.add(score);
                    patterns.add(foundPattern);
                }

                if (patterns.size()==0) continue;

                int argmax = 0;
                for (int k=0; k < scores.size(); ++k) {
                    if (scores.get(k) > scores.get(argmax)) {
                        argmax=k;
                    }
                }
                isoAno.set(f, new Ms2IsotopePatternMatch(simulated, patterns.get(argmax), scores.get(argmax)));

                isoFrags.add(f);
            }
        }
        final ArrayList<ProcessedPeak> peaklist = new ArrayList<ProcessedPeak>(input.getMergedPeaks());
        Collections.sort(peaklist, new ProcessedPeak.MassComparator());
        final FragmentAnnotation<Ms2IsotopePattern> patternAno = graph.getOrCreateFragmentAnnotation(Ms2IsotopePattern.class);
        int maxColor = graph.maxColor();
        for (Fragment f : isoFrags) {
            Fragment currenFrag = f;
            final Ms2IsotopePatternMatch iso = isoAno.get(f);
            final double[] perPeakScores = new double[iso.getMatched().size()];
            scorePatternPeakByPeak(iso.getSimulated(), iso.getMatched(), perPeakScores, peakAno.get(f).getRelativeIntensity(), sigmaAbs);
            for (int k=1; k < perPeakScores.length; ++k) {

                // find peak with the same color
                final double mz = iso.getMatched().getMzAt(k);
                final double shift = peakDev.absoluteFor(mz);

                int index = indexOfFirstPeakWithin(peaklist, mz-shift, mz+shift);
                // TODO: check if there could be multiple peaks
                final int color = index < 0 ? (++maxColor) : peaklist.get(index).getIndex();
                // introduce new isotope node
                final Fragment pseudoFragment = graph.addFragment(MolecularFormula.emptyFormula(), PrecursorIonType.unknown().getIonization());
                pseudoAno.set(pseudoFragment, new IsotopicMarker());
                pseudoFragment.setColor(color);
                if (index >= 0) {
                    peakAno.set(pseudoFragment, peaklist.get(index));
                } else {
                    final ProcessedPeak syntheticPeak = new ProcessedPeak();
                    syntheticPeak.setMass(mz);
                    peakAno.set(pseudoFragment, syntheticPeak);
                }
                final Loss l = graph.addLoss(currenFrag, pseudoFragment);
                currenFrag=pseudoFragment;
                l.setWeight(perPeakScores[k]);
            }
            patternAno.set(f, new Ms2IsotopePattern(Spectrums.extractPeakList(iso.getMatched()).toArray(new Peak[iso.getMatched().size()]), 0d));
        }
    }

    private SimpleSpectrum extractPattern(Deviation peakDev, Deviation shiftDev, SimpleSpectrum simulated, Ms2Spectrum msms, double maxIntensity, int index, UnivariateFunction recalibrationFunction) {
        SimpleMutableSpectrum buf = new SimpleMutableSpectrum(simulated.size());
        buf.addPeak(msms.getMzAt(index), 1d);

        int isoIndex = 1;
        int lastFound=0;
        final double monoMz = recalibrationFunction.value(msms.getMzAt(0));
        // find isotope peaks
        findPeaks:
        for (int k=index+1; isoIndex < simulated.size() && k < msms.size(); ++k) {

            final double mz = recalibrationFunction.value(msms.getMzAt(k));
            final double intens = msms.getIntensityAt(k)/msms.getIntensityAt(index);

            final double isoMz = simulated.getMzAt(isoIndex);
            final double isoInt = simulated.getIntensityAt(isoIndex);

            if (peakDev.inErrorWindow(mz, isoMz) || shiftDev.inErrorWindow(mz-monoMz, isoMz-simulated.getMzAt(0)))  {
                buf.addPeak(mz, intens);
                lastFound = isoIndex+1;
            } else if (mz > (isoMz+0.25)) {
                if (lastFound>isoIndex) {
                    ++isoIndex; // we already found a peak
                    --k;
                } else if (((msms.getIntensityAt(k)/maxIntensity)*isoInt) < 0.01) {
                    // if intensity of isotope peak is too low, we allow to skip it
                    ++isoIndex;
                    --k;
                } else {
                    break findPeaks;
                }
            }
        }

        // add found pattern
        return merge(buf);
    }

    private SimpleSpectrum findMs1Pattern(ProcessedInput input, FGraph graph, FastIsotopePatternGenerator generator, Ionization ion) {
        SimpleSpectrum ms1Pattern;// find MS1 spectrum
        if (USE_FRAGMENT_ISOGEN && input.getExperimentInformation().getMergedMs1Spectrum()!=null) {
            // search for isotope pattern in MS1
            final List<IsotopePattern> patterns = new IsotopePatternAnalysis().deisotope(input.getExperimentInformation());
            return patterns.get(0).getPattern();

        } else {
            ms1Pattern=null;
        }
        return ms1Pattern;
    }


    private SimpleSpectrum findMs1PatternInMs2(ProcessedInput input, FGraph graph, FastIsotopePatternGenerator generator, List<MutableMs2Spectrum> ms2Spectra, Ionization ion) {
        SimpleSpectrum ms1Pattern;// find MS1 spectrum
        final Deviation dev = input.getExperimentInformation().getAnnotationOrDefault(MS1MassDeviation.class).allowedMassDeviation;
        if (USE_FRAGMENT_ISOGEN) {
            final SimpleSpectrum simulated = new FastIsotopePatternGenerator().simulatePattern(graph.getRoot().getChildren(0).getFormula(),ion);
            int mostIntens = -1;
            double intens = 0d;

            int k=-1;
            for (Ms2Spectrum spec : ms2Spectra) {
                ++k;
                final int parent = Spectrums.mostIntensivePeakWithin(spec, input.getExperimentInformation().getIonMass(), dev);
                if (parent<0) continue;
                double i = spec.getIntensityAt(parent);
                if (i > intens) {
                    intens = i;
                    mostIntens = k;
                }
            }
            if (mostIntens<0) return null;
            final Ms2Spectrum msms = input.getExperimentInformation().getMs2Spectra().get(mostIntens);
            final double maxInt = Spectrums.getMaximalIntensity(msms);
            final SimpleSpectrum extr = extractPattern(dev, dev, simulated, msms, maxInt, Spectrums.mostIntensivePeakWithin(msms, input.getExperimentInformation().getIonMass(), dev), new Identity());
            if (msms.getIntensityAt(Spectrums.mostIntensivePeakWithin(msms, input.getExperimentInformation().getIonMass(), dev))/maxInt < 0.1) return findMs1Pattern(input,graph,generator,ion);
            final double[] pkscores = new double[extr.size()];
            double score = scorePatternPeakByPeak(normalizeByFirstPeak(simulated), normalizeByFirstPeak(extr), pkscores, 1, 0.05);
            double maxScore = 0;
            int maxIndex = 0;
            final SimpleMutableSpectrum buf = new SimpleMutableSpectrum();
            for (int i=0; i < pkscores.length; ++i) {
                if (pkscores[i] >= /*maxScore*/ 0) { // just take everything as long as it has a reasonable score
                    maxScore = pkscores[i];
                    maxIndex = i;
                }
            }
            for (int i=0; i <= maxIndex; ++i) buf.addPeak(extr.getPeakAt(i));
            return new SimpleSpectrum(buf);
        } else {
            ms1Pattern=null;
        }
        return ms1Pattern;
    }

    private double scorePatternPeakByPeak(SimpleSpectrum simulated, SimpleSpectrum foundPattern, double[] scores, double relativeIntensityOfMono, double sigmaAbs) {
        double intensityLeft = 0d;
        for (int k=1; k < simulated.size(); ++k) intensityLeft += simulated.getIntensityAt(k);
        double score = 0d;
        double bestScore = 0d;
        // adjust sigmaAbs to relative intensity
        sigmaAbs = Math.min(0.05, sigmaAbs);// * (1d/relativeIntensityOfMono));
        final double absDiff = new Deviation(20).absoluteFor(simulated.getMzAt(0));
        double lastPenalty = 0d;

        final double normMz = Math.log(Erf.erfc((1.5*absDiff)/(Math.sqrt(2)*absDiff)));
        int foundIndex = 1;
        for (int k=1, n = simulated.size(); k < n; ++k) {
            if (foundIndex >= foundPattern.size()) break;
            if (k < simulated.size() && foundPattern.getMzAt(foundIndex) > (0.25+simulated.getMzAt(k))) {
                final double p = simulated.getIntensityAt(k);
                final double intensScore = Math.exp((-p*p)/(2*sigmaAbs*sigmaAbs))/Math.sqrt(2d*Math.PI * sigmaAbs*sigmaAbs)*MULTIPLIER;
                final double penalty = MULTIPLIER*Math.log(Erf.erfc((intensityLeft)/(Math.sqrt(2)*sigmaAbs)));
                score += intensScore ;//+ 3*Math.min(1,relativeIntensityOfMono)*(foundPattern.getIntensityAt(k)/foundPattern.getIntensityAt(0));
                if (score+penalty > bestScore) bestScore = (score+penalty);
                scores[foundIndex] += score + (penalty-lastPenalty);
                lastPenalty = penalty;
                continue;
            }
            if (foundIndex>=foundPattern.size()) break;
            intensityLeft -= simulated.getIntensityAt(k);
            // mass dev score
            final double mz1Score = Math.log(Erf.erfc(Math.abs(simulated.getMzAt(k) - foundPattern.getMzAt(foundIndex))/(Math.sqrt(2)*absDiff)))*MULTIPLIER;
            final double mz2Score = Math.log(Erf.erfc(Math.abs((simulated.getMzAt(k)-simulated.getMzAt(0)) - (foundPattern.getMzAt(foundIndex)-foundPattern.getMzAt(0)))/(Math.sqrt(2)*absDiff)))*MULTIPLIER;
            final double intensScore = scoreLogOddIntensity(foundPattern.getIntensityAt(foundIndex), simulated.getIntensityAt(k), 0.15, sigmaAbs)*MULTIPLIER;
            final double penalty = MULTIPLIER*Math.log(Erf.erfc((intensityLeft)/(Math.sqrt(2)*sigmaAbs)));
            score += (Math.max(mz1Score,mz2Score)-normMz) + intensScore ;//+ 3*Math.min(1,relativeIntensityOfMono)*(foundPattern.getIntensityAt(k)/foundPattern.getIntensityAt(0));
            if (score+penalty > bestScore) bestScore = (score+penalty);
            scores[foundIndex] += score + (penalty-lastPenalty);
            lastPenalty = penalty;
            ++foundIndex;
        }
        return bestScore;
    }

    private SimpleSpectrum merge(SimpleMutableSpectrum buffer) {
        final SimpleMutableSpectrum buf2 = new SimpleMutableSpectrum(buffer.size());
        int i=0,k=0;
        double mzMerge=0;
        double intSum=0;
        while (k < buffer.size()) {
            if (Math.abs(buffer.getMzAt(i)-buffer.getMzAt(k))<0.25) {
                mzMerge += buffer.getMzAt(k)*buffer.getIntensityAt(k);
                intSum += buffer.getIntensityAt(k);
                ++k;
            } else {
                buf2.addPeak(mzMerge/intSum, intSum);
                mzMerge=0; intSum=0;
                i=k;
            }
        }
        if (mzMerge>0) buf2.addPeak(mzMerge/intSum, intSum);
        return new SimpleSpectrum(buf2);
    }

    private SimpleSpectrum normalizeByFirstPeak(SimpleSpectrum spec) {
        final double[] mz = new double[spec.size()];
        final double[] intensity = new double[spec.size()];
        for (int k=0; k < spec.size(); ++k) {
            mz[k] = spec.getMzAt(k);
            intensity[k] = spec.getIntensityAt(k)/spec.getIntensityAt(0);
        }
        return new SimpleSpectrum(mz, intensity);
    }

    private static int indexOfFirstPeakWithin(List<ProcessedPeak> peaklist, double begin, double end) {
        int pos = Spectrums.binarySearch(Spectrums.wrap(peaklist), begin);
        if (pos < 0) {
            pos = (-pos) - 1;
        }
        if (pos < peaklist.size() && peaklist.get(pos).getMass() >= begin && peaklist.get(pos).getMass() <= end) {
            return pos;
        } else return -1;
    }

    private static final double SQRT2PI = Math.sqrt(2 * Math.PI);
    private double scoreIntensity(double measuredIntensity, double theoreticalIntensity, double sigmaR, double sigmaA) {
        final double delta = measuredIntensity-theoreticalIntensity;
        final double probability = Math.exp(-(delta*delta)/(2*(sigmaA*sigmaA + measuredIntensity*measuredIntensity*sigmaR*sigmaR)))/(2*Math.PI*measuredIntensity*sigmaR*sigmaA);
        return Math.log(probability);
    }

    private double scoreLogOddIntensity(double measuredIntensity, double theoreticalIntensity, double sigmaR, double sigmaA) {
        return scoreIntensity(measuredIntensity, theoreticalIntensity, sigmaR, sigmaA)/* - scoreIntensity(theoreticalIntensity+1.5*sigmaA + 1.5*theoreticalIntensity*sigmaR, theoreticalIntensity, sigmaR, sigmaA)*/;
    }

    public void scoreFromMs1(ProcessedInput input, FGraph graph) {
        final Ms2Experiment exp = input.getExperimentInformation();
        final MS1MassDeviation dev = exp.getAnnotationOrDefault(MS1MassDeviation.class);
        final SimpleSpectrum mergedMs1 = input.getExperimentInformation().getMergedMs1Spectrum();
        if (mergedMs1 == null) return;
        final IsotopePatternAnalysis analyzer = new IsotopePatternAnalysis();
        final MassDeviationScorer scorer1 = new MassDeviationScorer(new PiecewiseLinearFunctionIntensityDependency(
                new double[]{0.2,0.1,0.01},
                new double[]{1,2,3}
        ));
        final MassDifferenceDeviationScorer scorer2 = new MassDifferenceDeviationScorer(new PiecewiseLinearFunctionIntensityDependency(
                new double[]{0.2,0.1,0.01},
                new double[]{1,2,3}
        ));
        final NormalDistributedIntensityScorer scorer3 = new NormalDistributedIntensityScorer(0.1, 0.005);
        final MissingPeakScorer scorer4 = new MissingPeakScorer();
        if (!input.getPeakAnnotations().containsKey(IsotopePatternAssignment.class)) {
            final PeakAnnotation<IsotopePatternAssignment> ano = input.getOrCreatePeakAnnotation(IsotopePatternAssignment.class);
            for (ProcessedPeak peak : input.getMergedPeaks()) {

                if (peak == input.getParentPeak())
                    continue;

                final double ionMass = peak.getMz();
                final int index = Spectrums.mostIntensivePeakWithin(mergedMs1, ionMass, dev.allowedMassDeviation);
                if (index >= 0) {
                    final SimpleSpectrum spec = Spectrums.getNormalizedSpectrum(analyzer.extractPattern(mergedMs1, dev, exp.getAnnotationOrDefault(FormulaConstraints.class).getChemicalAlphabet(), ionMass), Normalization.Max(1d));
                    if (spec.size() > 1) {
                        // use pattern!
                        //peak.setMass(spec.getMzAt(0));
                        ano.set(peak, new IsotopePatternAssignment(spec));
                    }
                }
            }
        }
        final IsotopePatternGenerator gen = new FastIsotopePatternGenerator(Normalization.Max(1d));

        final FragmentAnnotation<Ionization> ionizationAno = graph.getFragmentAnnotationOrThrow(Ionization.class);
        final PeakAnnotation<IsotopePatternAssignment> ano = input.getOrCreatePeakAnnotation(IsotopePatternAssignment.class);
        final FragmentAnnotation<Ms1IsotopePattern> isoPat = graph.getOrCreateFragmentAnnotation(Ms1IsotopePattern.class);
        for (Fragment f : graph.getFragmentsWithoutRoot()) {
            final ProcessedPeak peak = input.getMergedPeaks().get(f.getColor());
            final IsotopePatternAssignment assignment = ano.get(peak);
            if (assignment != null) {
                SimpleSpectrum spec = assignment.pattern;
                gen.setMaximalNumberOfPeaks(spec.size());
                final SimpleSpectrum simulated = Spectrums.subspectrum(gen.simulatePattern(f.getFormula(), ionizationAno.get(f)), 0, assignment.pattern.size());
                spec = Spectrums.subspectrum(spec, 0, simulated.size());
                // shorten pattern


                if (spec.size()==0) continue; //might happen for strange elements, since we set max number of peaks and a min intensity threshold

                final double[] scores = new double[spec.size()];
                scorer1.score(scores, spec, simulated, Normalization.Max(1d), input.getExperimentInformation());
                scorer2.score(scores, spec, simulated, Normalization.Max(1d), input.getExperimentInformation());
                scorer3.score(scores, spec, simulated, Normalization.Max(1d), input.getExperimentInformation());
                scorer4.score(scores, spec, simulated, Normalization.Max(1d), input.getExperimentInformation());
                double maxScore = 0d;
                for (int i=0; i < scores.length; ++i) {
                    maxScore = Math.max(scores[i], maxScore);
                }
                if (maxScore>0) {
                    isoPat.set(f, new Ms1IsotopePattern(spec, maxScore));
                    for (int i=0, n=f.getInDegree(); i < n; ++i) {
                        final Loss l = f.getIncomingEdge(i);
                        l.setWeight(l.getWeight() + maxScore);
                    }
                }
            }

        }



    }

    protected static class IsotopePatternAssignment {
        private final SimpleSpectrum pattern;

        public IsotopePatternAssignment(SimpleSpectrum pattern) {
            this.pattern = pattern;
        }
    }
}
