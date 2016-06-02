package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2IsotopePatternMatch;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedPeak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.IsotopicMarker;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.extraction.ExtractAll;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FastIsotopePatternGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FragmentIsotopeGenerator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.math3.special.Erf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IsotopePatternInMs2Scorer {


    private static final String LABEL = "ms2Isotope";
    private static final double MULTIPLIER = 1d/10d;

    private static final boolean USE_FRAGMENT_ISOGEN = false;

    public void score(ProcessedInput input, FGraph graph) {

        final Deviation peakDev = input.getMeasurementProfile().getAllowedMassDeviation();
        final Deviation shiftDev = peakDev.divide(2);
        // 1. for each fragment compute Isotope Pattern and match them against raw spectra
        final FastIsotopePatternGenerator generator = new FastIsotopePatternGenerator(Normalization.Sum(1d));
        final Ionization ion = input.getExperimentInformation().getPrecursorIonType().getIonization();
        final TIntArrayList ids = new TIntArrayList(5);
        final TDoubleArrayList scores = new TDoubleArrayList(5);
        final ArrayList<SimpleSpectrum> patterns = new ArrayList<SimpleSpectrum>(5);
        final FragmentAnnotation<ProcessedPeak> peakAno = graph.getFragmentAnnotationOrThrow(ProcessedPeak.class);
        final FragmentAnnotation<Ms2IsotopePatternMatch> isoAno =  graph.addFragmentAnnotation(Ms2IsotopePatternMatch.class);
        final FragmentAnnotation<IsotopicMarker> pseudoAno = graph.getOrCreateFragmentAnnotation(IsotopicMarker.class);
        // find patterns and score

        //////////
        // TODO: ONLY FOR QTOF!!!!
        final double sigmaAbs;
        {
            double sig = 0.05;
            for (int k=0; k < input.getMergedPeaks().size(); ++k) {
                final ProcessedPeak p = input.getMergedPeaks().get(0);
                if (p.isSynthetic()) continue;
                double abs = 0d;
                for (Peak ap : p.getOriginalPeaks()) abs = Math.max(ap.getIntensity(),abs);
                final double scale = p.getRelativeIntensity()/abs;
                sig = 300*scale;
                break;
            }
            sigmaAbs = sig;
        }

        final MolecularFormula ms1Formula = graph.getRoot().getChildren(0).getFormula();
        final FragmentIsotopeGenerator fisogen = new FragmentIsotopeGenerator();
        final SimpleSpectrum ms1Pattern = findMs1PatternInMs2(input, graph, generator, ion);


        ////////////

        final ArrayList<Fragment> isoFrags = new ArrayList<Fragment>();
        for (Fragment f : graph) {
            if (f.getFormula()!=null && !f.getFormula().isEmpty()) {
                final SimpleSpectrum simulated;

                if (ms1Pattern!=null) {
                    if (f.getFormula().equals(ms1Formula)) {
                        simulated = ms1Pattern;
                    } else {
                        simulated = normalizeByFirstPeak(fisogen.simulatePattern(ms1Pattern, ms1Formula, ms1Formula.subtract(f.getFormula()), ion,true));
                    }
                    /////// DEBUG
                    /*
                    System.out.println(f.getFormula());
                    System.out.println(simulated);
                    System.out.println(normalizeByFirstPeak(generator.simulatePattern(f.getFormula(), ion)));
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
                for (Ms2Spectrum msms : input.getExperimentInformation().getMs2Spectra()) {
                    ++msmsId;
                    final double maxIntensity = Spectrums.getMaximalIntensity(msms);
                    final int index = Spectrums.mostIntensivePeakWithin(msms, simulated.getMzAt(0), peakDev);
                    if (index < 0) {
                        continue;
                    }
                    final SimpleSpectrum foundPattern = extractPattern(peakDev, shiftDev, simulated, msms, maxIntensity, index);
                    final double[] pkscores = new double[foundPattern.size()];
                    double score = scorePatternPeakByPeak(simulated, foundPattern, pkscores, peakAno.get(f).getRelativeIntensity(), sigmaAbs);
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
                final Fragment pseudoFragment = graph.addFragment(MolecularFormula.emptyFormula());
                pseudoAno.set(pseudoFragment, new IsotopicMarker());
                pseudoFragment.setColor(color);
                if (index >= 0) {
                    peakAno.set(pseudoFragment, peaklist.get(index));
                } else {
                    final ProcessedPeak syntheticPeak = new ProcessedPeak();
                    syntheticPeak.setMz(mz);
                    peakAno.set(pseudoFragment, syntheticPeak);
                }
                final Loss l = graph.addLoss(currenFrag, pseudoFragment);
                currenFrag=pseudoFragment;
                l.setWeight(perPeakScores[k]);
            }
            patternAno.set(f, new Ms2IsotopePattern(Spectrums.extractPeakList(iso.getMatched()).toArray(new Peak[iso.getMatched().size()]), 0d));
        }
    }

    private SimpleSpectrum extractPattern(Deviation peakDev, Deviation shiftDev, SimpleSpectrum simulated, Ms2Spectrum msms, double maxIntensity, int index) {
        SimpleMutableSpectrum buf = new SimpleMutableSpectrum(simulated.size());
        buf.addPeak(msms.getMzAt(index), 1d);

        int isoIndex = 1;
        int lastFound=0;

        // find isotope peaks
        findPeaks:
        for (int k=index+1; isoIndex < simulated.size() && k < msms.size(); ++k) {

            final double mz = msms.getMzAt(k);
            final double intens = msms.getIntensityAt(k)/msms.getIntensityAt(index);

            final double isoMz = simulated.getMzAt(isoIndex);
            final double isoInt = simulated.getIntensityAt(isoIndex);

            if (peakDev.inErrorWindow(mz, isoMz) || shiftDev.inErrorWindow(mz-msms.getMzAt(0), isoMz-simulated.getMzAt(0)))  {
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
            final List<IsotopePattern> isoPatterns = new ExtractAll().extractPattern(input.getMeasurementProfile(), input.getExperimentInformation().getMergedMs1Spectrum(), input.getExperimentInformation().getIonMass(), false);
            double maxScore = Double.NEGATIVE_INFINITY;
            SimpleSpectrum bestPattern = null;
            for (IsotopePattern pat : isoPatterns) {
                final SimpleSpectrum spec = normalizeByFirstPeak(pat.getPattern());
                // TODO: implicitely assume that graph has only one ion formula
                final SimpleSpectrum sim = normalizeByFirstPeak(generator.simulatePattern(graph.getRoot().getChildren(0).getFormula(), ion));

                final double[] isoScores = new double[spec.size()];
                double sc = scorePatternPeakByPeak(sim, spec, isoScores, 1, 0.05);
                final SimpleMutableSpectrum buf = new SimpleMutableSpectrum();
                for (int k=0; k < isoScores.length; ++k) {
                    buf.addPeak(spec.getPeakAt(k));
                    if (isoScores[k] > maxScore) {
                        maxScore = isoScores[k];
                        bestPattern = new SimpleSpectrum(buf);
                    }
                }
            }
            if (bestPattern!=null && bestPattern.size()>1) {
                ms1Pattern=bestPattern;
            } else {
                ms1Pattern=null;
            }

        } else {
            ms1Pattern=null;
        }
        return ms1Pattern;
    }


    private SimpleSpectrum findMs1PatternInMs2(ProcessedInput input, FGraph graph, FastIsotopePatternGenerator generator, Ionization ion) {
        SimpleSpectrum ms1Pattern;// find MS1 spectrum
        final Deviation dev = input.getMeasurementProfile().getAllowedMassDeviation();
        if (USE_FRAGMENT_ISOGEN) {
            final SimpleSpectrum simulated = new FastIsotopePatternGenerator().simulatePattern(graph.getRoot().getChildren(0).getFormula(),ion);
            int mostIntens = -1;
            double intens = 0d;

            int k=-1;
            for (Ms2Spectrum spec : input.getExperimentInformation().getMs2Spectra()) {
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
            final SimpleSpectrum extr = extractPattern(dev, dev, simulated, msms, maxInt, Spectrums.mostIntensivePeakWithin(msms, input.getExperimentInformation().getIonMass(), dev));
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
        sigmaAbs = Math.min(0.5, sigmaAbs * (1d/relativeIntensityOfMono));
        final double absDiff = new Deviation(20).absoluteFor(simulated.getMzAt(0));
        double lastPenalty = 0d;

        final double normMz = Math.log(Erf.erfc((1.5*absDiff)/(Math.sqrt(2)*absDiff)));

        for (int k=1, n = Math.min(foundPattern.size(), simulated.size()); k < n; ++k) {
            intensityLeft -= simulated.getIntensityAt(k);
            // mass dev score
            final double mz1Score = Math.log(Erf.erfc(Math.abs(simulated.getMzAt(k) - foundPattern.getMzAt(k))/(Math.sqrt(2)*absDiff)))*MULTIPLIER;
            final double mz2Score = Math.log(Erf.erfc(Math.abs((simulated.getMzAt(k)-simulated.getMzAt(0)) - (foundPattern.getMzAt(k)-foundPattern.getMzAt(0)))/(Math.sqrt(2)*absDiff)))*MULTIPLIER;
            final double intensScore = scoreLogOddIntensity(foundPattern.getIntensityAt(k), simulated.getIntensityAt(k), 0.1, sigmaAbs)*MULTIPLIER;
            final double penalty = MULTIPLIER*Math.log(Erf.erfc((intensityLeft)/(Math.sqrt(2)*Math.max(sigmaAbs, 0.05))));
            score += (Math.max(mz1Score,mz2Score)-normMz) + intensScore ;//+ 3*Math.min(1,relativeIntensityOfMono)*(foundPattern.getIntensityAt(k)/foundPattern.getIntensityAt(0));
            if (score+penalty > bestScore) bestScore = (score+penalty);
            scores[k] = score + (penalty-lastPenalty);
            lastPenalty = penalty;
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
        final double delta = measuredIntensity - theoreticalIntensity;
        final double deltaZero = (sigmaR * sigmaR * delta * theoreticalIntensity) / (sigmaA * sigmaA +
                sigmaR * sigmaR * theoreticalIntensity * theoreticalIntensity);
        final double epsilon = delta - deltaZero * theoreticalIntensity;
        final double probDelta = (1 / (SQRT2PI * sigmaR)) * (Math.pow(Math.E, ((deltaZero) * (deltaZero) / (-2 * sigmaR *
                sigmaR))));
        final double probEpsilon = (1 / (SQRT2PI * sigmaA)) * (Math.pow(Math.E, (epsilon * epsilon / (-2 * sigmaA * sigmaA))));
        final double probability = probDelta * probEpsilon;
        return Math.log(probability);
    }

    private double scoreLogOddIntensity(double measuredIntensity, double theoreticalIntensity, double sigmaR, double sigmaA) {
        return scoreIntensity(measuredIntensity, theoreticalIntensity, sigmaR, sigmaA) - scoreIntensity(theoreticalIntensity+1.5*sigmaA + 1.5*theoreticalIntensity*sigmaR, theoreticalIntensity, sigmaR, sigmaA);
    }

}
