package de.unijena.bioinf.sirius.cli;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator.InvalidException;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MassDeviationScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MassDifferenceDeviationScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MissingPeakScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.NormalDistributedIntensityScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.util.PiecewiseLinearFunctionIntensityDependency;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.sirius.Sirius;
import gnu.trove.list.array.TDoubleArrayList;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Test {

    public static void main(String[] args) {
        try {
            final Sirius s = new Sirius("qtof");

            final MsExperimentParser parser = new MsExperimentParser();
            final PiecewiseLinearFunctionIntensityDependency dep = new PiecewiseLinearFunctionIntensityDependency(new double[]{0.2, 0.1, 0.01}, new double[]{1, 2, 3});
            final MassDifferenceDeviationScorer diff = new MassDifferenceDeviationScorer(dep);
            final MassDeviationScorer dev = new MassDeviationScorer(dep);
            final NormalDistributedIntensityScorer intens = new NormalDistributedIntensityScorer(0.1, 0.01);
            final MissingPeakScorer missing = new MissingPeakScorer();

            s.getMs1Analyzer().getIsotopePatternScorers().clear();
            s.getMs1Analyzer().getIsotopePatternScorers().add(diff);
            s.getMs1Analyzer().getIsotopePatternScorers().add(dev);
            s.getMs1Analyzer().getIsotopePatternScorers().add(intens);
            s.getMs1Analyzer().getIsotopePatternScorers().add(missing);

            final TDoubleArrayList[] sdiff = new TDoubleArrayList[10], sdev = new TDoubleArrayList[10], sinten = new TDoubleArrayList[10], smissing = new TDoubleArrayList[10];
            for (int i=0; i < 10; ++i) {
                sdiff[i] = new TDoubleArrayList();
                sdev[i] = new TDoubleArrayList();
                smissing[i] = new TDoubleArrayList();
                sinten[i] = new TDoubleArrayList();
            }
            TDoubleArrayList allScores = new TDoubleArrayList();
            int count=0, rank1=0;
            for (File f : new File("/home/kaidu/data/ms/ms1/unf").listFiles()) {
                if (parser.getParser(f)!=null) {
                    for (Ms2Experiment exp : parser.getParser(f).parseFromFile(f)) {
                        if (exp.getMolecularFormula()==null) {
                            System.out.println("Miss formula in " + f.getName());
                            continue;
                        }
                        if (exp.getMs2Spectra().isEmpty()) {
                            final SimpleMutableSpectrum spec = new SimpleMutableSpectrum();
                            spec.addPeak(exp.getIonMass(), 1);
                            exp.getMs2Spectra().add(new MutableMs2Spectrum(spec, exp.getIonMass(), CollisionEnergy.none(), 2));
                        }
                        final ProcessedInput pinput;
                        try {
                            pinput = s.getMs2Analyzer().performValidation(exp);
                            exp = pinput.getExperimentInformation();
                        } catch (InvalidException e) {
                            System.err.println(f);
                            e.printStackTrace();
                            continue;
                        }
                        if (exp.getMs1Spectra().size()==0) {
                            System.err.println("Miss spectrum in " + f);
                            continue;
                        }
                        SimpleSpectrum spec = s.getMs1Analyzer().extractPattern(exp, exp.getIonMass());
                        if (spec==null) {
                            System.err.println("Don't find spectrum in " + f);
                            System.err.println(exp.getIonMass());
                            System.err.println(exp.<Spectrum<Peak>>getMs1Spectra().get(0));
                        }
                        final MolecularFormula neutral = exp.getPrecursorIonType().getInSourceFragmentation()==null ? exp.getMolecularFormula() : exp.getMolecularFormula().subtract(exp.getPrecursorIonType().getInSourceFragmentation());

                        SimpleSpectrum theoretical = s.getMs1Analyzer().getPatternGenerator().simulatePattern(neutral,exp.getPrecursorIonType().getIonization());
                        if (theoretical.size() < spec.size()) {
                            spec = Spectrums.subspectrum(spec, 0, theoretical.size());
                        }
                        spec = Spectrums.getNormalizedSpectrum(spec, Normalization.Max(1));
                        theoretical = Spectrums.getNormalizedSpectrum(theoretical, Normalization.Max(1));
                        final double[] scores = new double[10];

                        diff.score(scores, spec, theoretical, Normalization.Max(1), exp, s.getMs1Analyzer().getDefaultProfile());
                        for (int k=0; k < Math.min(theoretical.size(), 10); ++k) {
                            sdiff[k].add(scores[k]);
                        }
                        Arrays.fill(scores, 0d);

                        dev.score(scores, spec, theoretical, Normalization.Max(1), exp, s.getMs1Analyzer().getDefaultProfile());
                        for (int k=0; k < Math.min(theoretical.size(), 10); ++k) {
                            sdev[k].add(scores[k]);
                        }
                        Arrays.fill(scores, 0d);

                        intens.score(scores, spec, theoretical, Normalization.Max(1), exp, s.getMs1Analyzer().getDefaultProfile());
                        for (int k=0; k < Math.min(theoretical.size(), 10); ++k) {
                            sinten[k].add(scores[k]);
                        }
                        Arrays.fill(scores, 0d);

                        missing.score(scores, spec, theoretical, Normalization.Max(1), exp, s.getMs1Analyzer().getDefaultProfile());
                        for (int k=0; k < Math.min(theoretical.size(), 10); ++k) {
                            smissing[k].add(scores[k]);
                        }
                        Arrays.fill(scores, 0d);


                        diff.score(scores, spec, theoretical, Normalization.Max(1), exp, s.getMs1Analyzer().getDefaultProfile());
                        dev.score(scores, spec, theoretical, Normalization.Max(1), exp, s.getMs1Analyzer().getDefaultProfile());
                        intens.score(scores, spec, theoretical, Normalization.Max(1), exp, s.getMs1Analyzer().getDefaultProfile());
                        missing.score(scores, spec, theoretical, Normalization.Max(1), exp, s.getMs1Analyzer().getDefaultProfile());
                        allScores.add(new TDoubleArrayList(scores).max());

                        if (allScores.get(allScores.size()-1)==0) {

                            System.out.println("Empty Score for " + f);
                            System.out.println(spec);
                            System.out.println(theoretical);
                            Arrays.fill(scores, 0d);
                            diff.score(scores, spec, theoretical, Normalization.Max(1), exp, s.getMs1Analyzer().getDefaultProfile());
                            System.out.println("diff: " + Arrays.toString(scores));
                            Arrays.fill(scores, 0d);
                            dev.score(scores, spec, theoretical, Normalization.Max(1), exp, s.getMs1Analyzer().getDefaultProfile());
                            System.out.println("dev: " + Arrays.toString(scores));
                            Arrays.fill(scores, 0d);
                            intens.score(scores, spec, theoretical, Normalization.Max(1), exp, s.getMs1Analyzer().getDefaultProfile());
                            System.out.println("int: " + Arrays.toString(scores));
                            Arrays.fill(scores, 0d);
                            missing.score(scores, spec, theoretical, Normalization.Max(1), exp, s.getMs1Analyzer().getDefaultProfile());
                            System.out.println("miss: " + Arrays.toString(scores));
                            Arrays.fill(scores, 0d);
                        }

                        System.out.println(f.getName() + " processed");
                        final List<IsotopePattern> patterns = s.getMs1Analyzer().deisotope(exp, pinput.getMeasurementProfile());
                        boolean found=false;
                        for (int k=0; k < patterns.size(); ++k) {
                            if (patterns.get(k).getCandidate().equals(exp.getMolecularFormula())) {
                                System.out.println("Found " + exp.getMolecularFormula() + " at rank " + (k+1) + " with score " + patterns.get(k).getScore() + " with opt score is " + patterns.get(0).getScore());
                                found=true;
                                if (k==0) ++rank1;
                                break;
                            }
                        }
                        if (!found) {
                            System.out.println("Do not find " + exp.getMolecularFormula() + " with opt score is " + patterns.get(0).getScore()); } else {
                            ++count;
                        }
                    }
                }
            }

            int max = Math.max(lastOne(smissing),Math.max(lastOne(sinten), Math.max(lastOne(sdev), lastOne(sdev))));
            for (int i=0; i < max; ++i) {
                System.out.println("Peak #" + i);
                System.out.println("massdev: median=" + median(sdev[i]) + ", avg=" + avg(sdev[i]) + ", values=" +  sdev[i]);
                System.out.println("massdiff: median=" + median(sdiff[i]) + ", avg=" + avg(sdiff[i]) + ", values=" +  sdiff[i]);
                System.out.println("intensity: median=" + median(sinten[i]) + ", avg=" + avg(sinten[i]) + ", values=" +  sinten[i]);
                System.out.println("missing: median=" + median(smissing[i]) + ", avg=" + avg(smissing[i]) + ", values=" +  smissing[i]);
            }

            System.out.println("--------------");
            allScores.sort();
            System.out.println(allScores);

            System.out.println("Found " + rank1 + " / " + count + " on top 1");

        } catch (IOException e) {

        }
    }

    private static double avg (TDoubleArrayList entries) {
        return entries.sum()/entries.size();
    }

    private static double median (TDoubleArrayList entries) {
        double[] vals = entries.toArray();
        Arrays.sort(vals);
        return vals[vals.length/2];
    }

    private static int lastOne(TDoubleArrayList[] entries) {
        for (int k=0; k < entries.length; ++k)
            if (entries[k].isEmpty()) return k;
        return entries.length;
    }

}
