package de.unijena.bioinf.sirius;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.MissingMergedSpectrumValidator;
import de.unijena.bioinf.ChemistryBase.ms.utils.PeaklistSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Ms2ExperimentValidator;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.InvalidException;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Warning;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.DNNRegressionPredictor;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.ElementPredictor;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;

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

    private static String[] STANDARD_IONIZATIONS = new String[]{"[M]+", "[M+H]+", "[M+Na]+", "[M+K]+", "[M+NH4]+"};

    private IsolationWindow standardIsolationWindow;
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


    private void setInitials(){
        ms2ExperimentValidators = new ArrayList<>();
        ms2ExperimentValidators.add(new MissingMergedSpectrumValidator());
        //todo MissingValueValidator???
        this.validatorWarning = new Warning.Noop();
        this.repairInput = true;
    }

    public List<Ms2Experiment> preprocess(List<Ms2Experiment> experiments) {
        Ms2Dataset dataset = new MutableMs2Dataset(experiments, "default", Double.NaN, new Sirius().getMs2Analyzer().getDefaultProfile());
        Ms2DatasetPreprocessor preprocessor = new Ms2DatasetPreprocessor(true);
        dataset = preprocessor.preprocess(dataset);
        return dataset.getExperiments();
    }

    public Ms2Dataset preprocess(Ms2Dataset ms2Dataset) {
        //todo inplace?
        ms2Dataset = validate(ms2Dataset);
        ms2Dataset = flagBadQualitySpectra(ms2Dataset);
        estimateIsolationWindow((MutableMs2Dataset) ms2Dataset);
        ms2Dataset = flagChimericSpectra(ms2Dataset);
        for (Ms2Experiment experiment : ms2Dataset.getExperiments()) {
            experiment.setAnnotation(IsolationWindow.class,  ms2Dataset.getIsolationWindow());
        }
        return ms2Dataset;
    }


    public MutableMs2Dataset validate(Ms2Dataset ms2Dataset) {
        MutableMs2Dataset mutableMs2Dataset = new MutableMs2Dataset(ms2Dataset);
        List<Ms2Experiment> validatedExperiments = new ArrayList<>();

        for (Ms2Experiment experiment : ms2Dataset.getExperiments()) {
            for (Ms2ExperimentValidator ms2ExperimentValidator : ms2ExperimentValidators) {
                try {
                    Ms2Experiment validatedExperiment = ms2ExperimentValidator.validate(experiment, validatorWarning, repairInput);
                    validatedExperiments.add(validatedExperiment);
                } catch (InvalidException exception) {
                    //todo currently throw away all bad stuff
                }

            }
        }
        mutableMs2Dataset.setExperiments(validatedExperiments);

        return mutableMs2Dataset;
    }


    private void init(Ms2Dataset ms2Dataset) {
        try {
            sirius = new Sirius(ms2Dataset.getProfile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (precursorIonTypes==null){
            precursorIonTypes = new PrecursorIonType[STANDARD_IONIZATIONS.length];
            for (int i = 0; i < STANDARD_IONIZATIONS.length; i++) {
                precursorIonTypes[i] = PrecursorIonType.getPrecursorIonType(STANDARD_IONIZATIONS[i]);

            }
        }

        MeasurementProfile measurementProfile = ms2Dataset.getMeasurementProfile();
        sirius.getMs2Analyzer().setDefaultProfile(measurementProfile);

        //use silicon
        DNNRegressionPredictor defaultPredictor = new DNNRegressionPredictor();
//        defaultPredictor.disableSilicon();
        sirius.setElementPrediction(defaultPredictor);
    }

    public MutableMs2Dataset flagBadQualitySpectra(Ms2Dataset ms2Dataset){
        MutableMs2Dataset mutableMs2Dataset = new MutableMs2Dataset(ms2Dataset);

        init(mutableMs2Dataset);
        Deviation maxDeviation = mutableMs2Dataset.getMeasurementProfile().getAllowedMassDeviation();


        //guess elements
        for (Ms2Experiment experiment : mutableMs2Dataset.getExperiments()) {
            FormulaConstraints constraints = predictElements(experiment, ms2Dataset);
            experiment.setAnnotation(FormulaConstraints.class, constraints);
        }


        datasetStatistics = new DatasetStatistics();




        List<ExperimentWithAnnotatedSpectra> experiments = extractSpectra(mutableMs2Dataset);
//        List<Spectrum<PeakWithAnnotation>> ms1Spectra = extractAllMs1(ms2Dataset);
//        List<Spectrum<PeakWithAnnotation>> ms2Spectra = extractAllMs1(ms2Dataset);

        //.... do some statistics
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

        for (ExperimentWithAnnotatedSpectra experiment : experiments) {
            annotateNoise(experiment);
//            for (Spectrum<PeakWithAnnotation> spectrum : experiment.getMs1spectra()) {
//                for (PeakWithAnnotation peakWithAnnotation : spectrum) {
//                    if (peakWithAnnotation.isNoise()) datasetStatistics.addMs2NoiseIntensity(peakWithAnnotation.getIntensity());
//                }
//            }
            for (Spectrum<PeakWithAnnotation> spectrum : experiment.getMs2spectra()) {
                for (PeakWithAnnotation peakWithAnnotation : spectrum) {
                    if (peakWithAnnotation.isNoise()){
                        datasetStatistics.addMs2NoiseIntensity(peakWithAnnotation.getIntensity());
//                        System.out.println("noise "+peakWithAnnotation.getMass());
                    }
                }
            }
        }


        //todo abosulte vs relative!!!!!
        ((MutableMeasurementProfile)mutableMs2Dataset.getMeasurementProfile()).setMedianNoiseIntensity(datasetStatistics.getMedianMs2NoiseIntensity());


        System.out.println("number of noise peaks "+datasetStatistics.getNoiseIntensities().size());
        System.out.println("mean noise intensity "+datasetStatistics.getMeanMs2NoiseIntensity());
        System.out.println("median noise intensity "+datasetStatistics.getMedianMs2NoiseIntensity());
        System.out.println("80% quantile noise intensity "+datasetStatistics.getQuantileMs2NoiseIntensity(80));

        System.out.println("min intensity ms1 "+datasetStatistics.getMinMs1Intensity());
        System.out.println("max intensity ms1 "+datasetStatistics.getMaxMs1Intensity());

        System.out.println("min intensity ms2 "+datasetStatistics.getMinMs2Intensity());
        System.out.println("max intensity ms2 "+datasetStatistics.getMaxMs2Intensity());


        System.out.println(Arrays.toString(datasetStatistics.getNoiseIntensities().toArray()));


        //no MS1 peak
        Deviation hasPeakDeviation = new Deviation(100, 0.1);
        for (Ms2Experiment experiment : mutableMs2Dataset.getExperiments()) {
            Spectrum<Peak> ms1 = experiment.getMergedMs1Spectrum();

            if (Spectrums.binarySearch(ms1, experiment.getIonMass(), hasPeakDeviation)<0){

                setSpectrumProperty(experiment, SpectrumProperty.NoMS1Peak);
            }
        }

        //to few peaks
        for (Ms2Experiment experiment : mutableMs2Dataset.getExperiments()) {
            Spectrum<Peak> ms2Spec = getMergedMs2(experiment, mutableMs2Dataset.getMeasurementProfile().getAllowedMassDeviation());
            if (Double.isNaN(mutableMs2Dataset.getIsolationWindowWidth()) || mutableMs2Dataset.getIsolationWindowWidth()>1){
                SimpleMutableSpectrum mutableSpectrum = new SimpleMutableSpectrum(ms2Spec);
                Spectrums.filterIsotpePeaks(mutableSpectrum, mutableMs2Dataset.getMeasurementProfile().getAllowedMassDeviation());
                ms2Spec = mutableSpectrum;
            }

            int numberOfPeaks = 0;
            for (Peak peak : ms2Spec) {
                if (peak.getIntensity()>2*datasetStatistics.getMedianMs2NoiseIntensity()) numberOfPeaks++;
            }

            if (numberOfPeaks<5) setSpectrumProperty(experiment, SpectrumProperty.FewPeaks);
        }

        //estimate noise


        //detect saturation

        //detect contamination

        //detect grass peaks

        //estimate isolation window!!!!!

        //too low MS1 peak intensity
        double maxMs1Intensity = datasetStatistics.getMaxMs1Intensity();
        for (Ms2Experiment experiment : mutableMs2Dataset.getExperiments()) {
//            if (!isNotBadQuality(experiment)) continue; //todo fast or better statistics?
            if (hasProperty(experiment, SpectrumProperty.NoMS1Peak)) continue;
            Spectrum<Peak> ms1 = experiment.getMergedMs1Spectrum();
            double highestInCurrentMs1 = Spectrums.getMaximalIntensity(ms1);
            double ionIntensity = ms1.getIntensityAt(Spectrums.mostIntensivePeakWithin(ms1, experiment.getIonMass(), hasPeakDeviation));
            if (ionIntensity/highestInCurrentMs1<0.01) setSpectrumProperty(experiment, SpectrumProperty.LowIntensity);
            //todo another way with absolute intensities
//            else if (ionIntensity<10*datasetStatistics.getMedianMs2NoiseIntensity()) setSpectrumProperty(experiment, SpectrumProperty.LowIntensity); //todo ???

        }

        //detect too-low-intensity spectra (absolute and relative, (just precursor, no fragments

        //exclude chimerics

        return mutableMs2Dataset;

    }

    public MutableMs2Dataset flagChimericSpectra(Ms2Dataset ms2Dataset){
        MutableMs2Dataset mutableMs2Dataset = new MutableMs2Dataset(ms2Dataset);

        init(mutableMs2Dataset);
        Deviation maxDeviation = mutableMs2Dataset.getMeasurementProfile().getAllowedMassDeviation();
        IsolationWindow isolationWindow = ms2Dataset.getIsolationWindow();


        for (Ms2Experiment experiment : mutableMs2Dataset.getExperiments()) {
            Spectrum<Peak> ms1 = experiment.getMergedMs1Spectrum();

            int ms1PrecursorIdx = Spectrums.mostIntensivePeakWithin(ms1, experiment.getIonMass(), maxDeviation);
            if (ms1PrecursorIdx<0){
                if (!hasProperty(experiment, SpectrumProperty.NoMS1Peak)){
                    setSpectrumProperty(experiment, SpectrumProperty.NoMS1Peak);
                }
                continue;
            }
            Peak precursorPeak = ms1.getPeakAt(ms1PrecursorIdx);
            double precursorMz = precursorPeak.getMass();
            double filteredIntensity = isolationWindow.getIntensity(precursorPeak.getIntensity(), precursorMz, precursorMz);

            double center = isolationWindow.getMassShift()+precursorPeak.getMass();
            double left = center-isolationWindow.getMaxWindowSize()/2;
            double right = center+isolationWindow.getMaxWindowSize()/2;

            SimpleMutableSpectrum ms1IsotopesRemoved = new SimpleMutableSpectrum(ms1);
            //todo which deviation to use? rather remove too much other peaks?
            ChemicalAlphabet alphabet;
            if (experiment.hasAnnotation(FormulaConstraints.class)){
                alphabet = experiment.getAnnotation(FormulaConstraints.class).getChemicalAlphabet();
            }else {
                alphabet = mutableMs2Dataset.getMeasurementProfile().getFormulaConstraints().getChemicalAlphabet();
            }
            Spectrums.filterIsotpePeaks(ms1IsotopesRemoved, maxDeviation.multiply(2), 0.3, 0.7, 5, alphabet); //todo or add up isotope intensities

            Spectrum<Peak> massSorted = Spectrums.getMassOrderedSpectrum(ms1IsotopesRemoved);
            int precursorIdx = Spectrums.binarySearch(massSorted, precursorPeak.getMass());

            if (precursorIdx<0) {
                if (isNotMonoisotopicPeak(experiment, mutableMs2Dataset.getMeasurementProfile())){
                    setSpectrumProperty(experiment, SpectrumProperty.NotMonoisotopicPeak);
                    continue;
                } else {
                    ms1IsotopesRemoved.addPeak(precursorPeak);
                    massSorted = Spectrums.getMassOrderedSpectrum(ms1IsotopesRemoved);
                    precursorIdx = Spectrums.binarySearch(massSorted, precursorPeak.getMass());
                }
            }

            int idx = precursorIdx;
            double summedIntensity = 0d;
            double maxIntensity = 0d;
            while ((++idx<massSorted.size()) && massSorted.getMzAt(idx)<=right) {
                Peak p = massSorted.getPeakAt(idx);
                double intensity = isolationWindow.getIntensity(p.getIntensity(), precursorMz, p.getMass());
                maxIntensity = Math.max(maxIntensity, intensity);
                summedIntensity += intensity;
            }
            idx = precursorIdx;
            while ((--idx>=0) && massSorted.getMzAt(idx)>=left) {
                Peak p = massSorted.getPeakAt(idx);
                double intensity = isolationWindow.getIntensity(p.getIntensity(), precursorMz, p.getMass());
                maxIntensity = Math.max(maxIntensity, intensity);
                summedIntensity += intensity;
            }


            //todo best would be to look how much is fragmented in MS2. If nothing, it's not a problem
            if (maxIntensity>=0.33*filteredIntensity || summedIntensity>=filteredIntensity){
                setSpectrumProperty(experiment, SpectrumProperty.Chimeric);
            }
        }
        return mutableMs2Dataset;
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
    }

    private void setSpectrumProperty(Ms2Experiment experiment, SpectrumProperty property){
        SpectrumQuality quality = experiment.getAnnotation(SpectrumQuality.class);
        if (quality==null){
            quality = new SpectrumQuality(property);
            experiment.setAnnotation(SpectrumQuality.class, quality);
        } else {
            quality.addProperty(property);
        }
    }

    private boolean hasProperty(Ms2Experiment experiment, SpectrumProperty property) {
        SpectrumQuality quality = experiment.getAnnotation(SpectrumQuality.class);
        if (quality==null) return false;
        else return quality.hasProperty(property);
    }

    private boolean isNotBadQuality(Ms2Experiment experiment) {
        return experiment.getAnnotation(SpectrumQuality.class, new SpectrumQuality(SpectrumProperty.Good)).isGoodQuality();
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
     *
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


        double decomposableIntensityAboveIonMass = 0d;
        double nonDecomposableIntensityAboveIonMass = 0d;

        int idx = 0;
        for (PeakWithAnnotation peakWithAnnotation : spectrum) {
            if (peakWithAnnotation.getMass()<= experiment.getIonMass()+5){ //all above isolation ion mass + some window
                for (PrecursorIonType precursorIonType : precursorIonTypes) {
                    double mass = precursorIonType.getIonization().subtractFromMass(peakWithAnnotation.getMass());
                    if (isDecomposable(mass, decomposer, deviation, constraints)){
                        annotateNonNoisePattern(spectrum, idx, sirius.getMs2Analyzer().getDefaultProfile(), constraints.getChemicalAlphabet());
                        break;
                    }
                }
            } else {
                //multiple charged or all noise
//                e.g. at least 50% of intensity decomposable?
                for (PrecursorIonType precursorIonType : precursorIonTypes) {
                    double mass = precursorIonType.getIonization().subtractFromMass(peakWithAnnotation.getMass());
                    if (isDecomposable(mass, decomposer, deviation, constraints)){
                        decomposableIntensityAboveIonMass += spectrum.getIntensityAt(idx);
                        annotateNonNoisePattern(spectrum, idx, sirius.getMs2Analyzer().getDefaultProfile(), constraints.getChemicalAlphabet());
                        break;
                    } else {
                        nonDecomposableIntensityAboveIonMass += spectrum.getIntensityAt(idx);
                    }
                }
            }

            ++idx;
        }


//        if  e.g. at least 50% of intensity decomposable? .....
        if (nonDecomposableIntensityAboveIonMass>decomposableIntensityAboveIonMass) {
            int ionIdx = Spectrums.binarySearch(spectrum, experiment.getIonMass()+5);
            if (ionIdx<0) ionIdx = -(ionIdx+1);
            for (int i = ionIdx; i < spectrum.size(); i++) {
                spectrum.getPeakAt(i).setNoise(true);

            }
        }

    }

    private boolean isDecomposable(double mass, MassToFormulaDecomposer decomposer, Deviation deviation, FormulaConstraints constraints){
        //todo changed to bigger mass dev
        final double abs = deviation.absoluteFor(mass);
        return decomposer.maybeDecomposable(mass-abs, mass+abs);
//        return decomposer.formulaIterator(mass, deviation, constraints).hasNext();
    }


    private void annotateNonNoisePattern(Spectrum<PeakWithAnnotation> spectrum, int monoIdx, MeasurementProfile profile, ChemicalAlphabet alphabet) {

        spectrum.getPeakAt(monoIdx).setNoise(false);
        double monoMz = spectrum.getMzAt(monoIdx);

        // add additional peaks
        for (int k=1; k <= 5; ++k) {
            final Range<Double> nextMz = PeriodicTable.getInstance().getIsotopicMassWindow(alphabet, profile.getAllowedMassDeviation(), monoMz, k);
            final double a = nextMz.lowerEndpoint();
            final double b = nextMz.upperEndpoint();
            final double startPoint = a - profile.getStandardMassDifferenceDeviation().absoluteFor(a);
            final double endPoint = b + profile.getStandardMassDifferenceDeviation().absoluteFor(b);
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
                ms2Dataset.setIsolationWindow(new SimpleIsolationWindow(width));
                ms2Dataset.getIsolationWindow().estimate(ms2Dataset);
                width = ms2Dataset.getIsolationWindow().getEstimatedWindowSize();
                ms2Dataset.setIsolationWindowWidth(width);
            } else {
                ms2Dataset.setIsolationWindow(new SimpleIsolationWindow(width));
                ms2Dataset.getIsolationWindow().estimate(ms2Dataset);
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

}
