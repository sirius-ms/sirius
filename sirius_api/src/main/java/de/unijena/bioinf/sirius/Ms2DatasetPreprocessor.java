package de.unijena.bioinf.sirius;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.PeaklistSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.DNNRegressionPredictor;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.ElementPredictor;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.NotMonoisotopicAnnotatorUsingIPA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by ge28quv on 01/07/17.
 * Do statistics and validation on Ms2Datasets
 */
public class Ms2DatasetPreprocessor {
    /*
    BIG TODOS:
        validators:
            - create merged MS1 still experimental.
        annotateNoise:
            use only precursorIonType if set?
            properly deal with peaks > ion mass (multiple charges?)
        QualityAnnotators:
            NotMonoisotopicAnnotatorUsingIPA: when is a peak considered not monoisotopic? just better score? 50% better score?
            NoMs1PeakAnnotator:    - only if no MS1 has precursor?! or if any?!
                                   - do we need skipPeak and trimToPossiblePattern and setUpperBounds?
        estimateIsolationWindow:
            - has to be tested thoroughly. Does it makes sense in this way?!
            - estimated width if not estimated!?!!?
        ChimericAnnotator:
            - might give different results if estimateIsolationWindow changes
            - do or don't remove isotopes from MS1 before gauging chimeric state //TODO test this first!?
     */


    private static final boolean DEBUG = false;

    protected final static Logger LOG = LoggerFactory.getLogger(Ms2DatasetPreprocessor.class);

    //these ionizations do not include adducts as gauging adduct mass differences without peak correlation data is probably to risky
    private static String[] STANDARD_IONIZATIONS_POSITIVE = new String[]{"[M]+", "[M+H]+", "[M+Na]+", "[M+K]+"};
    private static String[] STANDARD_IONIZATIONS_NEGATIVE = new String[]{"[M]-", "[M-H]-", "[M+Cl]-"};

    // intended precursor peak should be seen within this window.
    private static Deviation findMs1PeakDeviation = new Deviation(100, 0.1);

    //minimum number of peaks for a good quality spectrum
    private int MIN_NUMBER_OF_PEAKS = 5;

    private Sirius sirius;
    private PrecursorIonType[] precursorIonTypes;
    private DatasetStatistics datasetStatistics;



    List<Ms2ExperimentValidator> ms2ExperimentValidators;
    private Warning validatorWarning;
    private boolean repairInput;

    public Ms2DatasetPreprocessor() {
        this(true);
    }

    public Ms2DatasetPreprocessor(boolean repairInput) {
        this.repairInput = repairInput;
        setInitials();
    }

    /*
    preprocess
        -> validate
        -> flagBadQualitySpectra
            -> init
        -> estimateIsolationWindow
        -> flag chimerics

     */

    private void setInitials(){
        ms2ExperimentValidators = new ArrayList<>();
        ms2ExperimentValidators.add(new EmptySpectraValidator());
        ms2ExperimentValidators.add(new MissingMergedSpectrumValidator());
        //todo MissingValueValidator???
        this.validatorWarning = new WarningLog();
    }

    /**
     * estimate isolation window and annotate {@link CompoundQuality}.
     * ASSUMES NO BASELINE WAS APPLIED !!
     * @return
     */
    public List<Ms2Experiment> preprocess(List<Ms2Experiment> experiments) {
        Ms2Dataset dataset = new MutableMs2Dataset(experiments, "default", Double.NaN, new Sirius().getMs2Analyzer().getDefaultProfile());
        Ms2DatasetPreprocessor preprocessor = new Ms2DatasetPreprocessor(true);
        dataset = preprocessor.preprocess(dataset);
        return dataset.getExperiments();
    }

    /**
     * estimate isolation window and annotate {@link CompoundQuality}.
     * ASSUMES NO BASELINE WAS APPLIED !!
     * @param ms2Dataset
     * @return
     */
    public Ms2Dataset preprocess(Ms2Dataset ms2Dataset) {
        //todo inplace?
        ms2Dataset = validate(ms2Dataset);
        ms2Dataset = flagBadQualitySpectra(ms2Dataset);
        //todo this is this very alpha version. Has to be tested.
        estimateIsolationWindow((MutableMs2Dataset) ms2Dataset);

        //todo as estimateIsolationWindow is in alpha version, chimeric annotation might also not perform perfect
        double max2ndMostIntenseRatio = 0.33;
        double maxSummedIntensitiesRatio = 1.0;
        ChimericAnnotator chimericAnnotator = new ChimericAnnotator(findMs1PeakDeviation, max2ndMostIntenseRatio, maxSummedIntensitiesRatio);
        chimericAnnotator.prepare(ms2Dataset.getDatasetStatistics());
        chimericAnnotator.annotate(ms2Dataset);

        for (Ms2Experiment experiment : ms2Dataset.getExperiments()) {
            experiment.setAnnotation(IsolationWindow.class,  ms2Dataset.getIsolationWindow());
            CompoundQuality quality = experiment.getAnnotation(CompoundQuality.class);
            if (quality==null) {
                experiment.setAnnotation(CompoundQuality.class, new CompoundQuality(SpectrumProperty.Good));
            } else if (quality.isNotBadQuality() && !quality.isGoodQuality()) quality.addProperty(SpectrumProperty.Good);
        }

        return ms2Dataset;
    }


    /**
     * validate input experiments. Repair or remove errorneous experiments
     * @param ms2Dataset
     * @return
     */
    public MutableMs2Dataset validate(Ms2Dataset ms2Dataset) {
        MutableMs2Dataset mutableMs2Dataset = new MutableMs2Dataset(ms2Dataset);
        List<Ms2Experiment> validatedExperiments = new ArrayList<>();



        for (Ms2Experiment experiment : ms2Dataset.getExperiments()) {
            Ms2Experiment validatedExperiment = experiment;
            for (Ms2ExperimentValidator ms2ExperimentValidator : ms2ExperimentValidators) {
                try {
                    validatedExperiment = ms2ExperimentValidator.validate(validatedExperiment, validatorWarning, repairInput);

                } catch (InvalidException exception) {
                    LOG.warn("validation error: remove compound "+experiment.getName());
                    validatedExperiment = null;
                    break;
                }
            }
            if (validatedExperiment!=null) validatedExperiments.add(validatedExperiment);
        }
        mutableMs2Dataset.setExperiments(validatedExperiments);

        return mutableMs2Dataset;
    }


    /**
     * initialized Sirus, ionizations, element predictors etc.
     * @param ms2Dataset
     */
    private void init(Ms2Dataset ms2Dataset) {
        try {
            sirius = new Sirius(ms2Dataset.getProfile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        int chargeSign;
        try {
          chargeSign  = testCharge(ms2Dataset);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("currently do not support preprocessing datasets with positive and negative charges.");
        }

        String[] STANDARD_IONIZATIONS;
        if (chargeSign<0){
            STANDARD_IONIZATIONS = STANDARD_IONIZATIONS_NEGATIVE;
        } else {
            STANDARD_IONIZATIONS = STANDARD_IONIZATIONS_POSITIVE;
        }
        precursorIonTypes = new PrecursorIonType[STANDARD_IONIZATIONS.length];
        for (int i = 0; i < STANDARD_IONIZATIONS.length; i++) {
            precursorIonTypes[i] = PrecursorIonType.getPrecursorIonType(STANDARD_IONIZATIONS[i]);

        }



        MeasurementProfile measurementProfile = ms2Dataset.getMeasurementProfile();
        sirius.getMs2Analyzer().setDefaultProfile(measurementProfile);

        //use silicon for our purpose
        DNNRegressionPredictor defaultPredictor = new DNNRegressionPredictor();
//        defaultPredictor.disableSilicon();
        sirius.setElementPrediction(defaultPredictor);
    }

    /**
     * test for positive or negative charge
     * @param ms2Dataset +1 if all ionizations either positive or 0; -1 if all ionizations either negative or zero; 0 if all 0 (unknown)
     * @param ms2Dataset
     * @return
     * @throws IllegalArgumentException dataset contains positive and negative charges
     */
    private int testCharge(Ms2Dataset ms2Dataset) throws IllegalArgumentException{
        int charge = 0;
        for (Ms2Experiment experiment : ms2Dataset) {
            PrecursorIonType precursorIonType = experiment.getPrecursorIonType();
            int currentCharge = precursorIonType.getCharge();
            if (charge==0){
                charge = (int)Math.signum(currentCharge);
            } else if (charge*currentCharge<0){ //different signs
                throw new IllegalArgumentException("charge differs between experiments");
            }
        }
        return charge;
    }



    public MutableMs2Dataset flagBadQualitySpectra(Ms2Dataset ms2Dataset){
        MutableMs2Dataset mutableMs2Dataset = new MutableMs2Dataset(ms2Dataset);
        init(ms2Dataset);
        datasetStatistics = makeStatistics(mutableMs2Dataset); //mainly noise, min max intensities
        mutableMs2Dataset.setDatasetStatistics(datasetStatistics);
        //todo this assumes relative noise intensity. we learned absolute. adjust at some point?
//        ((MutableMeasurementProfile)mutableMs2Dataset.getMeasurementProfile()).setMedianNoiseIntensity(datasetStatistics.getMedianMs2NoiseIntensity());

        //todo add test to Annotator order
        NoMs1PeakAnnotator noMs1PeakAnnotator = new NoMs1PeakAnnotator(findMs1PeakDeviation);
        FewPeaksAnnotator fewPeaksAnnotator = new FewPeaksAnnotator(MIN_NUMBER_OF_PEAKS);
        LowIntensityAnnotator lowIntensityAnnotator = new LowIntensityAnnotator(findMs1PeakDeviation, 0.01, 0d); //todo ... there is no abs intensity
//        NotMonoisotopicAnnotator notMonoisotopicAnnotator = new NotMonoisotopicAnnotator(findMs1PeakDeviation);
        //todo if you knew isolation window you could allways take teh most intense peak as precursor peak not just guessing the window with findMs1PeakDeviation;
        NotMonoisotopicAnnotatorUsingIPA notMonoisotopicAnnotator = new NotMonoisotopicAnnotatorUsingIPA(findMs1PeakDeviation);

        List<QualityAnnotator> qualityAnnotators = new ArrayList<>();
        qualityAnnotators.add(noMs1PeakAnnotator);
        qualityAnnotators.add(fewPeaksAnnotator);
        qualityAnnotators.add(lowIntensityAnnotator);
        qualityAnnotators.add(notMonoisotopicAnnotator);


        for (QualityAnnotator qualityAnnotator : qualityAnnotators) {
            qualityAnnotator.prepare(datasetStatistics);
            qualityAnnotator.annotate(mutableMs2Dataset);
        }

        //todo saturation, brumm peaks, contamination ...

        return mutableMs2Dataset;
    }

    /**
     * statistics about noise intensity and stuff.
     * assumes NO BASELINE was applied
     * @param ms2Dataset
     * @return
     */
    private DatasetStatistics makeStatistics(Ms2Dataset ms2Dataset){
        //guess elements
        for (Ms2Experiment experiment : ms2Dataset.getExperiments()) {
            FormulaConstraints constraints = predictElements(experiment, ms2Dataset);
            experiment.setAnnotation(FormulaConstraints.class, constraints);
        }

        DatasetStatistics datasetStatistics = new DatasetStatistics();


        //in these experiments, noise peaks can be annotated
        List<ExperimentWithAnnotatedSpectra> experiments = extractSpectra(ms2Dataset);

        //get min and max intensities
        for (ExperimentWithAnnotatedSpectra experiment : experiments) {
            for (Spectrum<PeakWithAnnotation> spectrum : experiment.getMs1spectra()) {
                datasetStatistics.addMaxMs1Intensity(Spectrums.getMaximalIntensity(spectrum));
                datasetStatistics.addMinMs1Intensity(Spectrums.getMinimalIntensity(spectrum));
            }
            for (Spectrum<PeakWithAnnotation> spectrum : experiment.getMs2spectra()) {
                datasetStatistics.addMaxMs2Intensity(Spectrums.getMaximalIntensity(spectrum));
                datasetStatistics.addMinMs2Intensity(Spectrums.getMinimalIntensity(spectrum));
            }


        }

        //find (very high probability) noise
        for (ExperimentWithAnnotatedSpectra experiment : experiments) {
            annotateNoise(experiment);
            for (Spectrum<PeakWithAnnotation> spectrum : experiment.getMs2spectra()) {
                for (PeakWithAnnotation peakWithAnnotation : spectrum) {
                    if (peakWithAnnotation.isNoise()){
                        datasetStatistics.addMs2NoiseIntensity(peakWithAnnotation.getIntensity());
//                        System.out.println("noise "+peakWithAnnotation.getMass());
                    }
                }
            }
        }

        if (DEBUG) {
            System.out.println("number of noise peaks "+datasetStatistics.getNoiseIntensities().size());
            System.out.println("mean noise intensity "+datasetStatistics.getMeanMs2NoiseIntensity());
            System.out.println("median noise intensity "+datasetStatistics.getMedianMs2NoiseIntensity());
            System.out.println("80% quantile noise intensity "+datasetStatistics.getQuantileMs2NoiseIntensity(80));

            System.out.println("min intensity ms1 "+datasetStatistics.getMinMs1Intensity());
            System.out.println("max intensity ms1 "+datasetStatistics.getMaxMs1Intensity());

            System.out.println("min intensity ms2 "+datasetStatistics.getMinMs2Intensity());
            System.out.println("max intensity ms2 "+datasetStatistics.getMaxMs2Intensity());

            System.out.println(Arrays.toString(datasetStatistics.getNoiseIntensities().toArray()));
        }



        return datasetStatistics;
    }


    private boolean isNotMonoisotopicPeak(Ms2Experiment experiment, MeasurementProfile profile) {
        final double precursorMass = experiment.getIonMass();

        MutableSpectrum<Peak> merged = new SimpleMutableSpectrum(getMergedMs2(experiment, profile.getAllowedMassDeviation()));
        int idx = Spectrums.mostIntensivePeakWithin(merged, precursorMass, profile.getAllowedMassDeviation());
        if (idx<0){
            merged.addPeak(precursorMass, experiment.getIonMass());
//            idx = -(idx+1);
        }
        Spectrums.filterIsotpePeaks(merged, profile.getAllowedMassDeviation());

        return (Spectrums.search(merged, precursorMass, profile.getAllowedMassDeviation())<0); //not contained after filtering
    }

    private Spectrum<Peak> getMergedMs2(Ms2Experiment experiment, Deviation deviation){
        if (experiment.getMs2Spectra().size()==1) return experiment.getMs2Spectra().get(0);
        else {
            return Spectrums.mergeSpectra(deviation, true, true, experiment.getMs2Spectra());
        }
    }

    private final static String SEP = "\t";
    public void writeExperimentInfos(Ms2Dataset ms2Dataset, Path outputFile) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset());
        SpectrumProperty[] allProperties = SpectrumProperty.values();
        writer.write("name"+SEP+"mass");
        for (SpectrumProperty property : allProperties) {
            writer.write(SEP+property.toString());
        }
        for (Ms2Experiment experiment : ms2Dataset.getExperiments()) {
            writer.write("\n"+experiment.getName()+SEP+experiment.getIonMass());
            for (SpectrumProperty property : allProperties) {
                if (hasProperty(experiment, property)) writer.write(SEP+"1");
                else writer.write(SEP+"0");
            }

        }
        writer.close();
    }

    private void setSpectrumProperty(Ms2Experiment experiment, SpectrumProperty property){
        CompoundQuality quality = experiment.getAnnotation(CompoundQuality.class);
        if (quality==null){
            quality = new CompoundQuality(property);
            experiment.setAnnotation(CompoundQuality.class, quality);
        } else {
            quality.addProperty(property);
        }
    }

    private boolean hasProperty(Ms2Experiment experiment, SpectrumProperty property) {
        CompoundQuality quality = experiment.getAnnotation(CompoundQuality.class);
        if (quality==null) return false;
        else return quality.hasProperty(property);
    }

    private boolean isNotBadQuality(Ms2Experiment experiment) {
        return experiment.getAnnotation(CompoundQuality.class, new CompoundQuality(SpectrumProperty.Good)).isGoodQuality();
    }

    private FormulaConstraints predictElements(Ms2Experiment experiment, Ms2Dataset ms2Dataset) {
        FormulaConstraints constraints = sirius.predictElementsFromMs1(experiment);
        FormulaConstraints globalConstraints = ms2Dataset.getMeasurementProfile().getFormulaConstraints();
        if (constraints==null) return globalConstraints;

        ElementPredictor elementPredictor = sirius.getElementPrediction();
        for (Element element : globalConstraints.getChemicalAlphabet()) {
            if (!elementPredictor.isPredictable(element)){
                if (globalConstraints.getUpperbound(element)>constraints.getUpperbound(element)){
                    constraints.setUpperbound(element, globalConstraints.getUpperbound(element));
                }
            }
        }
        return constraints;
    }


    private void annotateNoise(ExperimentWithAnnotatedSpectra experiment){
        for (Spectrum<PeakWithAnnotation> spectrum : experiment.getMs1spectra()) {
            annotateNoise(spectrum, experiment.getExperiment());
        }
        for (Spectrum<PeakWithAnnotation> spectrum : experiment.getMs2spectra()) {
            annotateNoise(spectrum, experiment.getExperiment());
        }
    }


    /**
     * only annote peaks with very high possibility being noise (use large mass deviation window)
     * @param spectrum mass ordered spectrum
     */
    private void annotateNoise(Spectrum<PeakWithAnnotation> spectrum, Ms2Experiment experiment){
        for (PeakWithAnnotation peakWithAnnotation : spectrum) {
            peakWithAnnotation.setNoise(true);
        }

        //estimate elements
//        FormulaConstraints constraints = sirius.predictElementsFromMs1(experiment);

        FormulaConstraints constraints;
        if (experiment.hasAnnotation(FormulaConstraints.class)){
            constraints = experiment.getAnnotation(FormulaConstraints.class);
        } else {
            constraints = new FormulaConstraints(ChemicalAlphabet.getExtendedAlphabet());
        }
        Deviation deviation = sirius.getMs2Analyzer().getDefaultProfile().getAllowedMassDeviation().multiply(4); //todo bigger window for noise estimation?

////todo changed!!!!!!!!!!!!!
//        constraints = new FormulaConstraints("CHNOPS");

        MassToFormulaDecomposer decomposer = sirius.getMs2Analyzer().getDecomposerFor(constraints.getChemicalAlphabet());

        Ionization[] ionizations = new Ionization[precursorIonTypes.length];
        for (int i = 0; i < precursorIonTypes.length; i++) {
            ionizations[i] = precursorIonTypes[i].getIonization();

        }


        int idx = 0;
        for (PeakWithAnnotation peakWithAnnotation : spectrum) {
            if (peakWithAnnotation.getMass()<= experiment.getIonMass()+5){ //all above isolation ion mass + some window
                //todo use only precursorIonType of experiment if set?!
                if (!experiment.getPrecursorIonType().isIonizationUnknown() && !contains(ionizations, experiment.getPrecursorIonType().getIonization())){
                    double mass = experiment.getPrecursorIonType().getIonization().subtractFromMass(peakWithAnnotation.getMass());
                    if (isDecomposable(mass, decomposer, deviation, constraints)){
                        annotateNonNoisePattern(spectrum, idx, sirius.getMs2Analyzer().getDefaultProfile(), constraints.getChemicalAlphabet());
                    }
                }
                for (Ionization ionization : ionizations) {
                    double mass = ionization.subtractFromMass(peakWithAnnotation.getMass());
                    if (isDecomposable(mass, decomposer, deviation, constraints)){
                        annotateNonNoisePattern(spectrum, idx, sirius.getMs2Analyzer().getDefaultProfile(), constraints.getChemicalAlphabet());
//                        break; don't break. Might be wrong
                    }
                }
            } else {
                //multiple charged or all noise
                //todo currently not differently processed (multi charge)
                for (Ionization ionization : ionizations) {
                    double mass = ionization.subtractFromMass(peakWithAnnotation.getMass());
                    if (isDecomposable(mass, decomposer, deviation, constraints)){
                        annotateNonNoisePattern(spectrum, idx, sirius.getMs2Analyzer().getDefaultProfile(), constraints.getChemicalAlphabet());
//                        break; don't break. Might be wrong
                    }
                }
            }

            ++idx;
        }

        //might be risky
//        // if e.g. test at least 50% of intensity decomposable? else everything is noise !? what about chemical noise?
//        double decomposableIntensityAboveIonMass = 0d;
//        double nonDecomposableIntensityAboveIonMass = 0d;
//        int ionIdx = Spectrums.binarySearch(spectrum, experiment.getIonMass()+5);
//        if (ionIdx<0) ionIdx = -(ionIdx+1);
//        for (int i = ionIdx; i < spectrum.size(); i++) {
//            if (spectrum.getPeakAt(i).isNoise()) nonDecomposableIntensityAboveIonMass += spectrum.getIntensityAt(i);
//            else decomposableIntensityAboveIonMass += spectrum.getIntensityAt(i);
//        }
//
//        if (nonDecomposableIntensityAboveIonMass>decomposableIntensityAboveIonMass) {
//            ionIdx = Spectrums.binarySearch(spectrum, experiment.getIonMass()+5);
//            if (ionIdx<0) ionIdx = -(ionIdx+1);
//            for (int i = ionIdx; i < spectrum.size(); i++) {
//                spectrum.getPeakAt(i).setNoise(true);
//
//            }
//        }

    }

    /**
     * TODO currently {@link FormulaConstraints} not used. This makes is so much slower.
     * @param mass
     * @param decomposer
     * @param deviation
     * @param constraints
     * @return
     */
    private boolean isDecomposable(double mass, MassToFormulaDecomposer decomposer, Deviation deviation, FormulaConstraints constraints){
        final double abs = deviation.absoluteFor(mass);
        return decomposer.maybeDecomposable(mass-abs, mass+abs);
//        return decomposer.formulaIterator(mass, deviation, constraints).hasNext(); this makes it must slower
    }


    /**
     * again, allow bigger deviation to annotate non-noise
     * @param spectrum
     * @param monoIdx
     * @param profile
     * @param alphabet
     */
    private void annotateNonNoisePattern(Spectrum<PeakWithAnnotation> spectrum, int monoIdx, MeasurementProfile profile, ChemicalAlphabet alphabet) {
        Deviation massDifferenceDeviation = profile.getStandardMassDifferenceDeviation().multiply(2d);

        spectrum.getPeakAt(monoIdx).setNoise(false);
        double monoMz = spectrum.getMzAt(monoIdx);

        // add additional peaks
        for (int k=1; k <= 5; ++k) {
            final Range<Double> nextMz = PeriodicTable.getInstance().getIsotopicMassWindow(alphabet, profile.getAllowedMassDeviation(), monoMz, k);
            final double a = nextMz.lowerEndpoint();
            final double b = nextMz.upperEndpoint();
            final double startPoint = a - massDifferenceDeviation.absoluteFor(a);
            final double endPoint = b + massDifferenceDeviation.absoluteFor(b);
            final int nextIndex = Spectrums.indexOfFirstPeakWithin(spectrum, startPoint, endPoint);
            if (nextIndex < 0) break;

            for (int i=nextIndex; i < spectrum.size(); ++i) {
                final double mz = spectrum.getMzAt(i);
                if (mz > endPoint) break;
                spectrum.getPeakAt(i).setNoise(false);
            }
        }
    }

    public void estimateIsolationWindow(MutableMs2Dataset ms2Dataset){
        if (ms2Dataset.getIsolationWindow()==null){
            double width = ms2Dataset.getIsolationWindowWidth();
            if (Double.isNaN(width) || width<=0){
                width = 10;
                ms2Dataset.setIsolationWindow(new SimpleIsolationWindow(width, 0, true, findMs1PeakDeviation));
                ms2Dataset.getIsolationWindow().estimate(ms2Dataset);
                width = ms2Dataset.getIsolationWindow().getEstimatedWindowSize();
                ms2Dataset.setIsolationWindowWidth(width);
            } else {
                ms2Dataset.setIsolationWindow(new SimpleIsolationWindow(width, 0, false, findMs1PeakDeviation));
                ms2Dataset.getIsolationWindow().estimate(ms2Dataset);
                //todo also set new isolationWindowWidth?
            }

        }
    }

    private List<ExperimentWithAnnotatedSpectra> extractSpectra(Ms2Dataset ms2Dataset){
        List<ExperimentWithAnnotatedSpectra> experimentsWithAnnotatedSpectra = new ArrayList<>();
        final List<Ms2Experiment> experiments = ms2Dataset.getExperiments();
        for (Ms2Experiment experiment : experiments) {
            List<Spectrum<PeakWithAnnotation>> ms1 = convert(experiment.getMs1Spectra());
            List<Spectrum<PeakWithAnnotation>> ms2 = convert(experiment.getMs2Spectra());
            experimentsWithAnnotatedSpectra.add(new ExperimentWithAnnotatedSpectra(new MutableMs2Experiment(experiment), ms1, ms2));
        }
        return experimentsWithAnnotatedSpectra;
    }

    private List<Spectrum<PeakWithAnnotation>> extractAllMs1(Ms2Dataset ms2Dataset){
        List<Spectrum<PeakWithAnnotation>> ms1Spectra = new ArrayList<>();
        final List<Ms2Experiment> experiments = ms2Dataset.getExperiments();
        for (Ms2Experiment experiment : experiments) {
            for (Spectrum<Peak> spectrum : experiment.getMs1Spectra()) {
                ms1Spectra.add(convert(spectrum));
            }
        }
        return ms1Spectra;
    }

    private List<Spectrum<PeakWithAnnotation>> extractAllMs2(Ms2Dataset ms2Dataset){
        List<Spectrum<PeakWithAnnotation>> ms2Spectra = new ArrayList<>();
        final List<Ms2Experiment> experiments = ms2Dataset.getExperiments();
        for (Ms2Experiment experiment : experiments) {
            for (Spectrum<Peak> spectrum : experiment.getMs2Spectra()) {
                ms2Spectra.add(convert(spectrum));
            }
        }
        return ms2Spectra;
    }

    private List<Spectrum<PeakWithAnnotation>> convert(List<? extends Spectrum<Peak>> spectra){
        List<Spectrum<PeakWithAnnotation>> list = new ArrayList<>();
        for (Spectrum<Peak> spectrum : spectra) {
            list.add(convert(spectrum));
        }
        return list;
    }

    private Spectrum<PeakWithAnnotation> convert(Spectrum<Peak> spectrum){
        List<PeakWithAnnotation> pList = new ArrayList<>(spectrum.size());
        for (Peak peak : spectrum) {
            pList.add(new PeakWithAnnotation(peak));
        }
        Collections.sort(pList);
        return new PeaklistSpectrum<PeakWithAnnotation>(pList);
    }

    private <T> boolean contains(T[] array, T object) {
        for (T o : array) {
            if (o.equals(object)){
                return true;
            }
        }
        return false;
    }

    private class ExperimentWithAnnotatedSpectra {

        private MutableMs2Experiment experiment;
        private List<Spectrum<PeakWithAnnotation>> ms1spectra;
        private List<Spectrum<PeakWithAnnotation>> ms2spectra;

        public ExperimentWithAnnotatedSpectra(MutableMs2Experiment experiment, List<Spectrum<PeakWithAnnotation>> ms1spectra, List<Spectrum<PeakWithAnnotation>> ms2spectra) {
            this.experiment = experiment;
            this.ms1spectra = ms1spectra;
            this.ms2spectra = ms2spectra;
        }

        public MutableMs2Experiment getExperiment() {
            return experiment;
        }

        public List<Spectrum<PeakWithAnnotation>> getMs1spectra() {
            return ms1spectra;
        }

        public List<Spectrum<PeakWithAnnotation>> getMs2spectra() {
            return ms2spectra;
        }
    }

    private class PeakWithAnnotation extends Peak implements Comparable<Peak>{

        private boolean isNoise;

        public PeakWithAnnotation(Peak x) {
            super(x);
        }

        public PeakWithAnnotation(double mass, double intensity) {
            super(mass, intensity);
        }

        public boolean isNoise() {
            return isNoise;
        }

        public void setNoise(boolean noise) {
            isNoise = noise;
        }
    }


    private class WarningLog implements Warning {

        @Override
        public void warn(String message) {
            LOG.warn(message);
        }
    }
}
