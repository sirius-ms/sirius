package de.unijena.bioinf.FragmentationTreeConstruction.computation;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.ScoredMolecularFormula;
import de.unijena.bioinf.ChemistryBase.math.MathUtils;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering.PostProcessor;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Marcus
 * Date: 20.06.13
 * Time: 15:49
 * To change this template use File | Settings | File Templates.
 */
public class GCMSFragmentationPatternAnalysis extends FragmentationPatternAnalysis {

    private static final boolean VERBOSE = true;

    private boolean moleculePeakKnown;
    private boolean moleculePeakPresent;

    private boolean useHalogens;
    private boolean useChlorine;

    //derivates
    private boolean useDerivates;
    private boolean usePFB;
    private boolean useTMS;
    private boolean isPFBcompound;
    private boolean isTMScompound;

    private boolean removeIsotopePeaks;
    private static final double NEUTRON_MASS = 1.00866491;
    private static final double ELECTRON_MASS = 0.00054857990946;
    private Ms2ExperimentImpl input;

    //todo introduce tms elements stuff

    //ask : in GCMS still a LossSizeScorer in comparison to parent (FractionOfParent)   so LossSizeScorer -> learn, other Distribution of losses than MS2?
    //ask : in GCMS rdbeScore<0 strict filter --> change to scoring?

    //ask: whats about combined losses and scoring?

    @Override
    public void setInitial() {
        super.setInitial();
    }

    @Override
    public void setToDefault() {
        setInitial();
        this.useChlorine = false;
        this.useHalogens = false;
        this.removeIsotopePeaks = true;
    }

    @Override
    public ProcessedInput preprocessing(Ms2Experiment experiment) {
        List<ProcessedPeak> processedPeaks = preprocessingPeaks(experiment);
        // decompose and score all peaks
        //todo split decomposeAndScore method to differentiate in scoring whether molecule peak present or not
        if (moleculePeakPresent){
            //todo use input not experiment?
            return decomposeAndScore(input, processedPeaks);
        } else {
            return null;
        }

    }


    protected List<ProcessedPeak> preprocessingPeaks(Ms2Experiment experiment) {
        //is molecule peak known or unknown?
        if (experiment.getIonMass()==0) moleculePeakKnown = false;
        else moleculePeakKnown = true;
        // first of all: insert default profile if no profile is given
        input = wrapInput(experiment);
        if (input.getMeasurementProfile()==null) input.setMeasurementProfile(getDefaultProfile());
        // use a mutable experiment, such that we can easily modify it. Validate and preprocess input
        input = wrapInput(preProcess(validate(experiment)));
        System.out.println("ms1"+input.getMeasurementProfile().getStandardMs1MassDeviation());
        System.out.println("allowed"+input.getMeasurementProfile().getAllowedMassDeviation());
        List<ProcessedPeak> peaks = normalize(input);
        peaks = postProcess(PostProcessor.Stage.AFTER_NORMALIZING, new ProcessedInput(input, peaks, null, null)).getMergedPeaks();
        if (removeIsotopePeaks){
            peaks = removeIsotopePeaks(input, peaks);
            if (VERBOSE){
                if (moleculePeakKnown) {
                    System.out.println("ionMass: "+input.getIonMass()+", largest peak mass: "+peaks.get(peaks.size()-1).getMz());
                }
            }
        }
        //todo after isotopeStuff

        final ProcessedPeak parentPeak = selectMoleculePeakAndCleanSpectrum(input, peaks);
        //todo after parentPeak selection the molecule peak has to be last in list (assumed in decomposeAndScore)
        if (moleculePeakPresent) assert parentPeak.equals(peaks.get(peaks.size()-1)) : "parent is last in peak list";

        //todo für manche PostProcess angenommen, dass parent present
        //PostProcessor.Stage.AFTER_MERGING ist z.B. NoiseThreshold
        peaks = postProcess(PostProcessor.Stage.AFTER_MERGING, new ProcessedInput(input, peaks, parentPeak, null)).getMergedPeaks();

        return peaks;
    }

    @Override
    ArrayList<ProcessedPeak> normalize(Ms2Experiment experiment) {
        //todo delete peaks near high intensity peaks like in MS2 version?
        //normalize relative intensities in merged MS1 spectrum
        final ArrayList<ProcessedPeak> peakList = new ArrayList<ProcessedPeak>();
        final Spectrum<Peak> ms1spectrum = experiment.getMergedMs1Spectrum();
        final Ionization ion = experiment.getIonization();
        MutableSpectrum<Peak> mutableSpectrum = new SimpleMutableSpectrum(ms1spectrum);
        Spectrums.normalize(mutableSpectrum, Normalization.Max(100d));

        for (int i = 0; i < mutableSpectrum.size(); i++) {
            ProcessedPeak processedPeak = new ProcessedPeak();
            processedPeak.setIntensity(ms1spectrum.getIntensityAt(i));
            processedPeak.setMz(ms1spectrum.getMzAt(i));
            processedPeak.setRelativeIntensity(mutableSpectrum.getIntensityAt(i));
            processedPeak.setIon(ion);
            //insert a dummy as original Peak, because some scorings decide if it is a synthetic peak on whether he has a original MS2Peak! peak in list.
            processedPeak.setOriginalPeaks(Collections.singletonList(new MS2Peak(new Ms2SpectrumImpl(new CollisionEnergy(70, 70), 0), ms1spectrum.getMzAt(i), ms1spectrum.getIntensityAt(i))));
            peakList.add(processedPeak);
        }
        return peakList;
    }

    @Override
    public FragmentationGraph buildGraph(ProcessedInput input, ScoredMolecularFormula candidate) {
        assert moleculePeakPresent; //only works with present molecule peak;
        if (VERBOSE) System.out.println("buildGraph");
        if (VERBOSE) System.out.println("mergedPeaks:"+input.getMergedPeaks().size());
        int withDecomp = 0;
        int totalDecomp = 0;
        for (ProcessedPeak processedPeak : input.getMergedPeaks()) {
            if (processedPeak.getDecompositions().size()>0){
                withDecomp++;
                totalDecomp += processedPeak.getDecompositions().size();
            }
        }
        if (VERBOSE) System.out.println("withDecomp:"+withDecomp+" total:"+totalDecomp);
        return super.buildGraph(input, candidate);
    }

    public FragmentationGraph buildGraph(ProcessedInput input){
        //todo introduce dummy

        return null;
    }



    /**
     * under construction
     * always return true
     * predict whether molecule peak is present. And even predict mass ...
     * @param experiment
     * @param processedPeaks
     * @return
     */
    boolean isMoleculePeakPresent(Ms2Experiment experiment, List<ProcessedPeak> processedPeaks) {
        //todo implement
        return true;
    }



    ProcessedPeak selectMoleculePeakAndCleanSpectrum(Ms2Experiment experiment, List<ProcessedPeak> processedPeaks){
        if (moleculePeakKnown){
            //todo broaden errorWindow?
            //todo split method?
            moleculePeakPresent = true; //synthetic peak introduced if peak not present
            return selectParentPeakAndCleanSpectrum(experiment, processedPeaks);
        } else {
            //molecule peak not known
            //maybe later guessing whether it's in there and which peak it is -->
            moleculePeakPresent = isMoleculePeakPresent(experiment, processedPeaks);
            //but until now: just take heaviest
            //hopefully all isotopes are removed
            double currentMax = Double.NEGATIVE_INFINITY;
            ProcessedPeak moleculePeak = null;
            for (ProcessedPeak processedPeak : processedPeaks) {
                if (processedPeak.getMz()>currentMax){
                    moleculePeak = processedPeak;
                    currentMax = processedPeak.getMz();
                }
            }
            return moleculePeak;
        }
    }


    private void testForDerivatization(Ms2Experiment experiment, List<ProcessedPeak> processedPeaks){
        double massDeviationPenalty = 3.0; //todo add setter?
        //test for derivatization
        if (useDerivates){
            if (VERBOSE) System.out.println("Derivatization: ");

            for (ProcessedPeak p : processedPeaks){
                if (p.getMass()>181 && p.getMass()<182){
                    double score = MathUtils.erfc(Math.abs(p.getMass() - (181.007665 - ELECTRON_MASS)) * massDeviationPenalty / getErrorForMass(experiment, p.getMass(), p.getRelativeIntensity()) / Math.sqrt(2));
                    if (score>0.3){
                        usePFB=true;
                        isPFBcompound =true;
                        System.out.println("increase PFB score");
                    }
                } else if (p.getMass()>73 && p.getMass()<74){
                    double score = MathUtils.erfc(Math.abs(p.getMass()-(73.047352-ELECTRON_MASS))*massDeviationPenalty/ getErrorForMass(experiment, p.getMass(), p.getRelativeIntensity())/Math.sqrt(2));
                    //System.out.println(score);
                    if (score>0.3){
                        useTMS=true;
                        isTMScompound =true;
                        System.out.println("increase TMS score");
                    }
                }
            }
            if(!usePFB && !useTMS){
                usePFB=true;
                useTMS=true;
            }
        }


        boolean tmsAlreadyKnown = false;
        boolean pfbAlreadyKnown = false;
        List<Element> usedElements = experiment.getMeasurementProfile().getFormulaConstraints().getChemicalAlphabet().getElements();
        for (Element usedElement : usedElements) {
            if (usePFB){
                if (usedElement.equals(PeriodicTable.getInstance().getByName("Tms"))) tmsAlreadyKnown = true;
            }
            if (useTMS){
                if (usedElement.equals(PeriodicTable.getInstance().getByName("Pfb"))) pfbAlreadyKnown = true;
            }
        }

        Ms2ExperimentImpl ms2ExperimetImpl = new Ms2ExperimentImpl(experiment);
        List<Element> newElementsList = new ArrayList<Element>(usedElements);
        if (!tmsAlreadyKnown) newElementsList.add(PeriodicTable.getInstance().getByName("Tms"));
        if (!pfbAlreadyKnown) newElementsList.add(PeriodicTable.getInstance().getByName("Pfb"));
        if (!tmsAlreadyKnown || !pfbAlreadyKnown) {
            List<FormulaFilter> filters = ms2ExperimetImpl.getMeasurementProfile().getFormulaConstraints().getFilters();
            ChemicalAlphabet alphabet = new ChemicalAlphabet(newElementsList.toArray(new Element[0]));
            FormulaConstraints constraints = new FormulaConstraints(alphabet);
            for (FormulaFilter filter : filters) {
                constraints.addFilter(filter);
            }
            MutableMeasurementProfile measurementProfile = new MutableMeasurementProfile(ms2ExperimetImpl.getMeasurementProfile());
            measurementProfile.setFormulaConstraints(constraints);
            ms2ExperimetImpl.setMeasurementProfile(measurementProfile);
        }
    }


    private List<ProcessedPeak> removeIsotopePeaks(Ms2Experiment experiment, List<ProcessedPeak> processedPeaks){
        List<ProcessedPeak> peaks = new ArrayList<ProcessedPeak>(processedPeaks);

        //todo already sorted?
        Collections.sort(peaks, new ProcessedPeak.MassComparator());

        double cl =0;
        if (useHalogens || useChlorine){
            cl = 36.96590259 - 34.96885268;
        }

        int isotopePeaks=0;
        ProcessedPeak mono, iso;
        int monoCounter =0;
        int isoCounter =1;

        //List<ProcessedPeak> allIsotopes = new ArrayList<ProcessedPeak>();

        Map<ProcessedPeak, List<ProcessedPeak>> isotopesMap = new HashMap<ProcessedPeak, List<ProcessedPeak>>();

        while(monoCounter<peaks.size() && isoCounter<peaks.size()){
            mono = peaks.get(monoCounter);
            isotopesMap.put(mono, new ArrayList<ProcessedPeak>());
            isoCounter=monoCounter+1;
            //ProcessedPeak lastFoundIso =null;
            while (isoCounter<peaks.size()){
                iso = peaks.get(isoCounter);
                double diffToMono=0;
                diffToMono =  iso.getMass() - mono.getMass();

                double error = getErrorForMass(experiment, iso.getMz(), iso.getRelativeIntensity());
                if (diffToMono< NEUTRON_MASS-error){
                    isoCounter++;
                }else if (diffToMono >= NEUTRON_MASS-error && diffToMono <= NEUTRON_MASS+error && (0.3*mono.getRelativeIntensity()>iso.getRelativeIntensity())){      //todo 0.3 well-founded or just tested???
                    //isotope found
                    isotopesMap.get(mono).add(iso);
                    peaks.remove(iso);
                    //lastFoundIso=iso;
                    ++isotopePeaks;
                }else if ((useHalogens || useChlorine) && diffToMono>= cl-error && diffToMono<= cl+error && mono.getRelativeIntensity()> iso.getRelativeIntensity()){  // cl isotope
                    isotopesMap.get(mono).add(iso);
                    peaks.remove(iso);
                    //lastFoundIso=iso;
                    ++isotopePeaks;
                }else if (diffToMono > NEUTRON_MASS+error && diffToMono > cl+error){
                    ++monoCounter;
                    break;
                }else isoCounter++;
            }
        }

        if (VERBOSE) System.out.println("Found "+isotopePeaks+" isotope peaks out of "+processedPeaks.size());
        return peaks;
    }



    private Deviation intensityDeviation(double intensity, Deviation deviation){
        final EIIntensityDeviation intensityDeviation = (EIIntensityDeviation)deviation;
        intensityDeviation.setRelIntensity(intensity);
        return intensityDeviation;
    }

    private double getErrorForMass(Ms2Experiment experiment, double center, double intensity){
        return intensityDeviation(intensity, experiment.getMeasurementProfile().getAllowedMassDeviation()).absoluteFor(center);
    }


    public boolean isUseHalogens() {
        return useHalogens;
    }

    public void setUseHalogens(boolean useHalogens) {
        this.useHalogens = useHalogens;
    }

    public boolean isUseChlorine() {
        return useChlorine;
    }

    public void setUseChlorine(boolean useChlorine) {
        this.useChlorine = useChlorine;
    }

    public boolean isRemoveIsotopePeaks() {
        return removeIsotopePeaks;
    }

    public void setRemoveIsotopePeaks(boolean removeIsotopePeaks) {
        this.removeIsotopePeaks = removeIsotopePeaks;
    }

    public boolean isUseDerivates() {
        return useDerivates;
    }

    public void setUseDerivates(boolean useDerivates) {
        this.useDerivates = useDerivates;
    }


    class GCMSProcessedInput extends ProcessedInput{

        private boolean isTmsCompound;
        private boolean isPfbCompound;

        public GCMSProcessedInput(Ms2Experiment experiment, List<ProcessedPeak> mergedPeaks, ProcessedPeak parentPeak, List<ScoredMolecularFormula> parentMassDecompositions, double[] peakScores, double[][] peakPairScores) {
            super(experiment, mergedPeaks, parentPeak, parentMassDecompositions, peakScores, peakPairScores);
        }

        boolean isTmsCompound() {
            return isTmsCompound;
        }

        void setTmsCompound(boolean tmsCompound) {
            this.isTmsCompound = tmsCompound;
        }

        boolean isPfbCompound() {
            return isPfbCompound;
        }

        void setPfbCompound(boolean pfbCompound) {
            this.isPfbCompound = pfbCompound;
        }
    }
}
