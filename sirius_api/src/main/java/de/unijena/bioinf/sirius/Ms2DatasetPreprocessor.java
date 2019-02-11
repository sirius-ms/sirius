package de.unijena.bioinf.sirius;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.SimpleRectangularIsolationWindow;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.exceptions.InsufficientDataException;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.FormulaSettings;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.PeaklistSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.DNNRegressionPredictor;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.ElementPredictor;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.NotMonoisotopicAnnotatorUsingIPA;
import gnu.trove.list.array.TDoubleArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by ge28quv on 01/07/17.
 * Do statistics and validation on Ms2Datasets.
 * Not everything is threadsafe
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
    public static final Deviation FIND_MS1_PEAK_DEVIATION = new Deviation(100, 0.1);
    private static Deviation findMs1PeakDeviation = FIND_MS1_PEAK_DEVIATION;

    //minimum number of peaks for a good quality spectrum
    public static final int MIN_NUMBER_OF_PEAKS = 5;

    private Sirius sirius;
    private PrecursorIonType[] precursorIonTypes;


    List<Ms2ExperimentValidator> ms2ExperimentValidators;
    private Warning validatorWarning;
    private boolean repairInput;

    private List<QualityAnnotator> qualityAnnotators;

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

        //todo add test to Annotator order
        NoMs1PeakAnnotator noMs1PeakAnnotator = new NoMs1PeakAnnotator(findMs1PeakDeviation);
        FewPeaksAnnotator fewPeaksAnnotator = new FewPeaksAnnotator(MIN_NUMBER_OF_PEAKS);
        LowIntensityAnnotator lowIntensityAnnotator = new LowIntensityAnnotator(findMs1PeakDeviation, 0.01, 0d); //todo ... there is no abs intensity
//        NotMonoisotopicAnnotator notMonoisotopicAnnotator = new NotMonoisotopicAnnotator(findMs1PeakDeviation);
        //todo if you knew isolation window you could allways take teh most intense peak as precursor peak not just guessing the window with findMs1PeakDeviation;
        NotMonoisotopicAnnotatorUsingIPA notMonoisotopicAnnotator = new NotMonoisotopicAnnotatorUsingIPA(findMs1PeakDeviation);

        qualityAnnotators = new ArrayList<>();
        qualityAnnotators.add(noMs1PeakAnnotator);
        qualityAnnotators.add(fewPeaksAnnotator);
        qualityAnnotators.add(lowIntensityAnnotator);
        qualityAnnotators.add(notMonoisotopicAnnotator);

    }

    public void setQualityAnnotators(List<QualityAnnotator> qualityAnnotators){
        this.qualityAnnotators = qualityAnnotators;
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
//        double max2ndMostIntenseRatio = 0.33;
//        double maxSummedIntensitiesRatio = 1.0;
//        ChimericAnnotator chimericAnnotator = new ChimericAnnotator(findMs1PeakDeviation, max2ndMostIntenseRatio, maxSummedIntensitiesRatio);
//        chimericAnnotator.prepare(ms2Dataset.getDatasetStatistics());
//        chimericAnnotator.annotate(ms2Dataset);

        for (QualityAnnotator qualityAnnotator : qualityAnnotators) {
            if (qualityAnnotator instanceof ChimericAnnotator){
                qualityAnnotator.prepare(ms2Dataset.getDatasetStatistics());
                qualityAnnotator.annotate(ms2Dataset);
            }
        }

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
        DatasetStatistics datasetStatistics;
        if (mutableMs2Dataset.getDatasetStatistics()==null){
            datasetStatistics = makeStatistics(mutableMs2Dataset); //mainly noise, min max intensities
            mutableMs2Dataset.setDatasetStatistics(datasetStatistics);
        } else {
            datasetStatistics = mutableMs2Dataset.getDatasetStatistics();
        }

        //todo this assumes relative noise intensity. we learned absolute. adjust at some point?
//        ((MutableMeasurementProfile)mutableMs2Dataset.getMeasurementProfile()).setMedianNoiseIntensity(datasetStatistics.getMedianMs2NoiseIntensity());




        for (QualityAnnotator qualityAnnotator : qualityAnnotators) {
            if (qualityAnnotator instanceof ChimericAnnotator) continue;
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
    public MutableDatasetStatistics makeStatistics(Ms2Dataset ms2Dataset){
        init(ms2Dataset);

        //guess elements
        for (Ms2Experiment experiment : ms2Dataset.getExperiments()) {
            FormulaConstraints constraints = predictElements(experiment, ms2Dataset);
            sirius.setFormulaConstraints(experiment, constraints);
        }

        MutableDatasetStatistics mutableDatasetStatistics = new MutableDatasetStatistics();


        //in these experiments, noise peaks can be annotated
        List<ExperimentWithAnnotatedSpectra> experiments = extractSpectra(ms2Dataset);

        //get min and max intensities
        for (ExperimentWithAnnotatedSpectra experiment : experiments) {
            for (Spectrum<PeakWithAnnotation> spectrum : experiment.getMs1spectra()) {
                mutableDatasetStatistics.addMaxMs1Intensity(Spectrums.getMaximalIntensity(spectrum));
                mutableDatasetStatistics.addMinMs1Intensity(Spectrums.getMinimalIntensity(spectrum));
            }
            for (Spectrum<PeakWithAnnotation> spectrum : experiment.getMs2spectra()) {
                mutableDatasetStatistics.addMaxMs2Intensity(Spectrums.getMaximalIntensity(spectrum));
                mutableDatasetStatistics.addMinMs2Intensity(Spectrums.getMinimalIntensity(spectrum));
            }


        }

        if (DEBUG) {
            try {
                BufferedWriter writer = Files.newBufferedWriter(Paths.get("noise_intensities.csv"));
                //find (very high probability) noise
                for (ExperimentWithAnnotatedSpectra experiment : experiments) {
                    annotateNoise(experiment);
                    for (Spectrum<PeakWithAnnotation> spectrum : experiment.getMs2spectra()) {
                        for (PeakWithAnnotation peakWithAnnotation : spectrum) {
                            if (peakWithAnnotation.isNoise()){
                                mutableDatasetStatistics.addMs2NoiseIntensity(peakWithAnnotation.getIntensity());
                                writer.write(peakWithAnnotation.getMass()+"\t"+peakWithAnnotation.getIntensity());
                                writer.newLine();
//                        System.out.println("noise "+peakWithAnnotation.getMass());
                            }
                        }
                    }
                }
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            //find (very high probability) noise
            for (ExperimentWithAnnotatedSpectra experiment : experiments) {
                annotateNoise(experiment);
                for (Spectrum<PeakWithAnnotation> spectrum : experiment.getMs2spectra()) {
                    for (PeakWithAnnotation peakWithAnnotation : spectrum) {
                        if (peakWithAnnotation.isNoise()){
                            mutableDatasetStatistics.addMs2NoiseIntensity(peakWithAnnotation.getIntensity());
//                        System.out.println("noise "+peakWithAnnotation.getMass());
                        }
                    }
                }
            }
        }



        if (DEBUG) {
            System.out.println("number of noise peaks "+ mutableDatasetStatistics.getNoiseIntensities().size());
            System.out.println("mean noise intensity "+ mutableDatasetStatistics.getMeanMs2NoiseIntensity());
            System.out.println("median noise intensity "+ mutableDatasetStatistics.getMedianMs2NoiseIntensity());
            System.out.println("80% quantile noise intensity "+ mutableDatasetStatistics.getPrecentileMs2NoiseIntensity(80));

            System.out.println("min intensity ms1 "+ mutableDatasetStatistics.getMinMs1Intensity());
            System.out.println("max intensity ms1 "+ mutableDatasetStatistics.getMaxMs1Intensity());

            System.out.println("min intensity ms2 "+ mutableDatasetStatistics.getMinMs2Intensity());
            System.out.println("max intensity ms2 "+ mutableDatasetStatistics.getMaxMs2Intensity());

            System.out.println(Arrays.toString(mutableDatasetStatistics.getNoiseIntensities().toArray()));
        }



        return mutableDatasetStatistics;
    }


    private boolean isNotMonoisotopicPeak(Ms2Experiment experiment, MeasurementProfile profile) {
        final double precursorMass = experiment.getIonMass();


        //todo what about experiment.getMergedMs1Spectrum()? probably already some cutoff applied by mzMine an co?
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

    public void writeExperimentInfos(Ms2Dataset ms2Dataset, Path outputFile) throws IOException {
        SpectrumProperty[] allProperties = SpectrumProperty.values();
        writeExperimentInfos(ms2Dataset, outputFile, allProperties);
    }

    private final static String SEP = "\t";
    public void writeExperimentInfos(Ms2Dataset ms2Dataset, Path outputFile, SpectrumProperty[] properties) throws IOException {
        init(ms2Dataset);
        BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset());

        writer.write("name"+SEP+"mass");
        for (SpectrumProperty property : properties) {
            writer.write(SEP+property.toString());
        }
        writer.write(SEP+"numberOfIsotopePeaks");
        for (Ms2Experiment experiment : ms2Dataset.getExperiments()) {
            writer.newLine();
            writer.write(experiment.getName()+SEP+experiment.getIonMass());
            for (SpectrumProperty property : properties) {
                if (hasProperty(experiment, property)) writer.write(SEP+"1");
                else writer.write(SEP+"0");
            }

            SimpleSpectrum isotopePattern = extractIsotopePattern(experiment);
            writer.write(SEP+String.valueOf(isotopePattern==null?0:isotopePattern.size()));
        }
        writer.close();
    }

    private SimpleSpectrum extractIsotopePattern(Ms2Experiment experiment) {

        Ms2Experiment experiment2;
        if (experiment.getMergedMs1Spectrum()!=null) experiment2 = experiment;
        else {
            experiment2 = new MutableMs2Experiment(experiment);
            if (experiment2.getMs1Spectra().size() > 0) {
                ((MutableMs2Experiment)experiment2).setMergedMs1Spectrum(Spectrums.mergeSpectra(experiment2.<Spectrum<Peak>>getMs1Spectra()));
            } else {
                return new SimpleSpectrum(new double[0], new double[0]);
            }

        }

        //todo again the problem with the measurementprofile?!
        return sirius.getMs1Analyzer().extractPattern(experiment2, experiment2.getIonMass());
    }

    public void writeDatasetSummary(Ms2Dataset ms2Dataset, Path outputFile) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(outputFile);
        DatasetStatistics ds = ms2Dataset.getDatasetStatistics();

        double ms1MinInt, ms1MaxInt, ms2MinInt, ms2MaxInt, medianNoise, maxNoise, noise80, noise95;
        if (ds instanceof MutableDatasetStatistics){
            MutableDatasetStatistics mds = (MutableDatasetStatistics)ds;
            ms1MinInt = mds.getMinMs1Intensities().size()>0?mds.getMinMs1Intensity():Double.NaN;
            ms1MaxInt = mds.getMaxMs1Intensities().size()>0?mds.getMaxMs1Intensity():Double.NaN;
            ms2MinInt = mds.getMinMs2Intensities().size()>0?mds.getMinMs2Intensity():Double.NaN;
            ms2MaxInt = mds.getMaxMs2Intensities().size()>0?mds.getMaxMs2Intensity():Double.NaN;

            medianNoise = mds.getNoiseIntensities().size()>0?mds.getMedianMs2NoiseIntensity():Double.NaN;
            maxNoise = mds.getNoiseIntensities().size()>0?mds.getMaxMs2NoiseIntensity():Double.NaN;
            noise80 = mds.getNoiseIntensities().size()>0?mds.getPrecentileMs2NoiseIntensity(80):Double.NaN;
            noise95 = mds.getNoiseIntensities().size()>0?mds.getPrecentileMs2NoiseIntensity(95):Double.NaN;

        } else {
            ms1MinInt = ds.getMinMs1Intensity();
            ms1MaxInt = ds.getMaxMs1Intensity();
            ms2MinInt = ds.getMinMs2Intensity();
            ms2MaxInt = ds.getMaxMs2Intensity();
            medianNoise = ds.getMedianMs2NoiseIntensity();
            maxNoise = ds.getMaxMs2NoiseIntensity();
            noise80 = Double.NaN;
            noise95 = Double.NaN;
        }

        writer.write("min intensity MS1\t"+ms1MinInt);
        writer.newLine();
        writer.write("max intensity MS1\t"+ms1MaxInt);
        writer.newLine();
        writer.write("min intensity MS2\t"+ms2MinInt);
        writer.newLine();
        writer.write("max intensity MS2\t"+ms2MaxInt);
        writer.newLine();


        writer.write("median noise intensity MS2\t"+medianNoise);
        writer.newLine();
        writer.write("max noise intensity MS2\t"+maxNoise);
        writer.newLine();
        writer.write("80% percentile noise intensity MS2\t"+noise80);
        writer.newLine();
        writer.write("95% percentile noise intensity MS2\t"+noise95);
        writer.newLine();

        IsolationWindow isolationWindow = ms2Dataset.getIsolationWindow();

        double width = isolationWindow.getEstimatedWindowSize();
        writer.write("isolation window width\t"+width);
        writer.newLine();
        double shift = isolationWindow.getEstimatedMassShift();
        writer.write("isolation window shift\t"+shift);
        writer.newLine();
        double left = shift-width/2d;
        double right = shift+width/2d;

        TDoubleArrayList masses = new TDoubleArrayList();
        TDoubleArrayList intensities = new TDoubleArrayList();
        for (double m = left; m <= right-0.5; m+=0.5) {
            masses.add(m);
            intensities.add(isolationWindow.getIntensityRatio(0d, m));
        }
        masses.add(right);
        intensities.add(isolationWindow.getIntensityRatio(0d, right));

        writer.write("isolation window relative masses example\t"+Arrays.stream(masses.toArray()).mapToObj(d->String.valueOf(d)).collect(Collectors.joining(" ")));
        writer.newLine();
        writer.write("isolation window relative intensities example\t"+Arrays.stream(intensities.toArray()).mapToObj(d->String.valueOf(d)).collect(Collectors.joining(" ")));
        writer.newLine();

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
        if (experiment.hasAnnotation(FormulaSettings.class)){
            constraints = experiment.getAnnotation(FormulaSettings.class).getConstraints();
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
                ms2Dataset.setIsolationWindow(new EstimatedIsolationWindow(width, 0, true, findMs1PeakDeviation));
                try {
                    ms2Dataset.getIsolationWindow().estimate(ms2Dataset);
                    width = ms2Dataset.getIsolationWindow().getEstimatedWindowSize();
                    ms2Dataset.setIsolationWindowWidth(width);
                } catch (InsufficientDataException e) {
                    LOG.warn("Cannot estimate isolation window. Fallback to rectangular isolation window with width 1Da.");
                    ms2Dataset.setIsolationWindow(new SimpleRectangularIsolationWindow(-0.5, 0.5));
                    ms2Dataset.setIsolationWindowWidth(1.0);
                }

            } else {
                ms2Dataset.setIsolationWindow(new EstimatedIsolationWindow(width, 0, false, findMs1PeakDeviation));
                try {
                    ms2Dataset.getIsolationWindow().estimate(ms2Dataset);
                    //todo also set new isolationWindowWidth?
                } catch (InsufficientDataException e) {
                    LOG.warn("Cannot estimate isolation window. Fallback to rectangular isolation window with width "+width+" Da.");
                    double right = width/2d;
                    double left = -width/2d;
                    ms2Dataset.setIsolationWindow(new SimpleRectangularIsolationWindow(left, right));
                    ms2Dataset.setIsolationWindowWidth(width);
                }
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
