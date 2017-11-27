package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Score;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TreeComputationInstance;
import de.unijena.bioinf.FragmentationTreeConstruction.model.DecompositionList;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ExtractedIsotopePattern;
import de.unijena.bioinf.FragmentationTreeConstruction.model.FormulaSettings;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.ElementPredictor;
import de.unijena.bioinf.jjobs.JobManager;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Sirius2 extends Sirius {

    private static final double MINIMAL_SCORE_FOR_APPLY_FILTER = 10d;
    private static final double ISOTOPE_SCORE_FILTER_THRESHOLD = 2.5d;
    protected JobManager jobManager;

    public static void main(String[] args) {
        final TDoubleArrayList correctScore = new TDoubleArrayList(), optScore = new TDoubleArrayList(), quantile = new TDoubleArrayList(), sub=new TDoubleArrayList();
        final TIntArrayList ranks = new TIntArrayList();
        int countFilteredOut = 0, total=0;
        try {
            final Sirius2 sirius2 = new Sirius2("qtof");
            sirius2.jobManager = new JobManager(4);
            for (File f : getExampleFiles()) {
                final MutableMs2Experiment exp = new MutableMs2Experiment(sirius2.parseExperiment(f).next());
                if (exp.getMolecularFormula()==null && exp.getAnnotation(InChI.class,null)!=null)
                    exp.setMolecularFormula(exp.getAnnotation(InChI.class).extractFormula());
                List<IsotopePattern> patterns = sirius2.getMs1Analyzer().deisotope(exp, sirius2.getMs1Analyzer().getDefaultProfile());
                boolean found=false;
                int rank = 0;
                if (patterns.isEmpty()) continue;
                double maxScore = 0d;
                int npeaks = 0;
                for (IsotopePattern p : patterns ) {
                    maxScore = Math.max(p.getScore(), maxScore);
                    npeaks = Math.max(p.getPattern().size(), npeaks);
                }
                for (IsotopePattern p : patterns) {
                    ++rank;
                    if (p.getCandidate().equals(exp.getMolecularFormula())) {
                        correctScore.add(p.getScore());
                        double q = (patterns.size()-rank+1d)/patterns.size();
                        quantile.add(q);
                        ranks.add(rank);
                        System.out.println(npeaks + "\t" + p.getScore());
                        if (maxScore >= 10 && p.getScore() < 2.5*npeaks) ++countFilteredOut;
                        found=true;
                        break;
                    }
                }
                if (found) {
                    ++total;
                    optScore.add(patterns.get(0).getScore());
                    int i = (int)Math.ceil(patterns.size()*0.3);
                    if (i < patterns.size()) {
                        sub.add(patterns.get(i).getScore());
                    }
                }
            }
            System.out.println(countFilteredOut + " / " + total);
            System.out.println("-----------------");
            System.out.println(correctScore);
            System.out.println("-----------------");
            System.out.println(optScore);
            System.out.println("-----------------");
            System.out.println(quantile);
            System.out.println("-----------------");
            System.out.println(ranks);
            System.out.println("-----------------");
            System.out.println(sub);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static File[] getExampleFiles() {
        return new File("/home/kaidu/data/ms/ms1").listFiles();
    }

    public Sirius2(String profileName) throws IOException {
        super(profileName);
    }

    @Override
    public List<IdentificationResult> identify(Ms2Experiment uexperiment, int numberOfCandidates, boolean recalibrating, IsotopePatternHandling deisotope, FormulaConstraints formulaConstraints) {
        final TreeComputationInstance instance = new TreeComputationInstance(jobManager, getMs2Analyzer(), uexperiment, numberOfCandidates);
        final ProcessedInput pinput = instance.validateInput();
        pinput.getMeasurementProfile().setFormulaConstraints(formulaConstraints);
        performMs1Analysis(instance, deisotope);
        // ms2 Analysis
        jobManager.submitSubJob(instance);
        TreeComputationInstance.FinalResult fr = instance.takeResult();
        final List<IdentificationResult> irs = new ArrayList<>();
        int k=0;
        for (FTree tree : fr.getResults()) {
            irs.add(new IdentificationResult(tree, ++k));
        }
        return irs;
    }

    private double getIsotopeScore(FTree tree) {
        return tree.getFragmentAnnotationOrNull(Score.class).get(tree.getRoot()).get("IsotopeMs1Scorer");
    }

    private ExtractedIsotopePattern extractedIsotopePattern(ProcessedInput pinput) {
        ExtractedIsotopePattern pat = pinput.getAnnotation(ExtractedIsotopePattern.class, null);
        if (pat==null){
            final SimpleSpectrum spectrum = getMs1Analyzer().extractPattern(mergeMs1Spec(pinput), pinput.getMeasurementProfile(), pinput.getExperimentInformation().getIonMass());
            pat = new ExtractedIsotopePattern(spectrum);
            pinput.setAnnotation(ExtractedIsotopePattern.class, pat);
        }
        return pat;
    }

    private SimpleSpectrum mergeMs1Spec(ProcessedInput pinput) {
        final MutableMs2Experiment experiment = pinput.getExperimentInformation();
        if (experiment.getMergedMs1Spectrum()!=null) return experiment.getMergedMs1Spectrum();
        else if (experiment.getMs1Spectra().size()>0) {
            experiment.setMergedMs1Spectrum( Spectrums.mergeSpectra(experiment.<Spectrum<Peak>>getMs1Spectra()));
            return experiment.getMergedMs1Spectrum();
        } else return new SimpleSpectrum(new double[0], new double[0]);
    }

    /*
    TODO: We have to move this at some point back into the FragmentationPatternAnalysis pipeline -_-
     */
    protected boolean performMs1Analysis(TreeComputationInstance instance, IsotopePatternHandling handling) {
        if (handling == IsotopePatternHandling.omit) return false;
        final ProcessedInput input = instance.validateInput();
        final ExtractedIsotopePattern pattern = extractedIsotopePattern(input);
        if (!pattern.hasPatternWithAtLeastTwoPeaks())
            return false; // we cannot do any analysis without isotope information
        // step 1: automatic element detection
        performAutomaticElementDetection(input, pattern.getPattern());

        // step 2: Isotope pattern analysis
        final DecompositionList decompositions = instance.precompute().getAnnotationOrThrow(DecompositionList.class);
        final IsotopePatternAnalysis an = getMs1Analyzer();
        for (Map.Entry<Ionization, List<MolecularFormula>> entry : decompositions.getFormulasPerIonMode().entrySet()) {
            for (IsotopePattern pat : an.scoreFormulas(pattern.getPattern(), entry.getValue(), input.getExperimentInformation(), input.getMeasurementProfile(), PrecursorIonType.getPrecursorIonType(entry.getKey()))) {
                pattern.getExplanations().put(pat.getCandidate(), pat);
            }
        }
        int isoPeaks = 0;
        double maxScore = Double.NEGATIVE_INFINITY;
        for (IsotopePattern pat : pattern.getExplanations().values()) {
            maxScore = Math.max(pat.getScore(), maxScore);
            isoPeaks = Math.max(pat.getPattern().size(), isoPeaks);
        }
        // step 3: apply filtering and/or scoring
        if (maxScore >= MINIMAL_SCORE_FOR_APPLY_FILTER) {
            if (handling.isFiltering()) {
                final Iterator<Map.Entry<MolecularFormula, IsotopePattern>> iter = pattern.getExplanations().entrySet().iterator();
                while (iter.hasNext()) {
                    if (iter.next().getValue().getScore() < ((isoPeaks*ISOTOPE_SCORE_FILTER_THRESHOLD))) {
                        iter.remove();
                    }
                }
            }
        }
        final Iterator<Map.Entry<MolecularFormula, IsotopePattern>> iter = pattern.getExplanations().entrySet().iterator();
        while (iter.hasNext())  {
            final Map.Entry<MolecularFormula, IsotopePattern> val = iter.next();
            val.setValue(val.getValue().withScore(handling.isScoring() ? Math.max(val.getValue().getScore(),0d) : 0d));
        }
        return true;
    }

    private void performAutomaticElementDetection(ProcessedInput input, SimpleSpectrum extractedPattern) {
        final FormulaSettings settings = input.getAnnotation(FormulaSettings.class, FormulaSettings.defaultWithMs1());
        if (settings.isElementDetectionEnabled()) {
            final ElementPredictor predictor = getElementPrediction();
            final HashSet<Element> allowedElements = new HashSet<>(input.getMeasurementProfile().getFormulaConstraints().getChemicalAlphabet().getElements());
            final HashSet<Element> auto = settings.getAutomaticDetectionEnabled();
            allowedElements.addAll(auto);
            Iterator<Element> e = allowedElements.iterator();
            final FormulaConstraints constraints = predictor.predictConstraints(extractedPattern);
            while (e.hasNext()) {
                final Element detectable = e.next();
                if (auto.contains(detectable) && getElementPrediction().isPredictable(detectable) && constraints.getUpperbound(detectable) <= 0)
                    e.remove();
            }
            final FormulaConstraints revised = settings.getConstraints().getExtendedConstraints(allowedElements.toArray(new Element[allowedElements.size()]));
            for (Element det : auto) {
                revised.setUpperbound(det, constraints.getUpperbound(det));
            }
            input.getMeasurementProfile().setFormulaConstraints(revised);
        }
    }


}
