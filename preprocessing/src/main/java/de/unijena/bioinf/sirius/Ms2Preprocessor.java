package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.sirius.merging.HighIntensityMsMsMerger;
import de.unijena.bioinf.sirius.merging.Ms2Merger;
import de.unijena.bioinf.sirius.peakprocessor.MergedSpectrumProcessor;
import de.unijena.bioinf.sirius.peakprocessor.NoiseIntensityThresholdFilter;
import de.unijena.bioinf.sirius.peakprocessor.NormalizeToSumPreprocessor;
import de.unijena.bioinf.sirius.peakprocessor.UnmergedSpectrumProcessor;
import de.unijena.bioinf.sirius.validation.Ms2Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Ms2Preprocessor extends Ms1Preprocessor {

    protected Ms2Merger ms2Merger = new HighIntensityMsMsMerger();
    protected List<MergedSpectrumProcessor> postProcessors;
    protected List<UnmergedSpectrumProcessor> preprocessors;

    public Ms2Preprocessor() {
        super();
        this.validator = new Ms2Validator();
        this.preprocessors = new ArrayList<>(Arrays.asList(
                new NormalizeToSumPreprocessor()
        ));
        this.postProcessors = new ArrayList<>(Arrays.asList(
           new NoiseIntensityThresholdFilter()
        ));
    }

    @Override
    public ProcessedInput preprocess(Ms2Experiment experiment) {
        final ProcessedInput processedInput = super.preprocess(experiment);
        preProcessMsMs(processedInput);
        mergePeaks(processedInput);
        renormalizeSpectrum(processedInput);
        postProcessMsMs(processedInput);
        return processedInput;
    }

    private void renormalizeSpectrum(ProcessedInput processedInput) {
        List<ProcessedPeak> mergedPeaks = processedInput.getMergedPeaks();
        double maxIntensity = 0d;
        for (ProcessedPeak peak : mergedPeaks) {
            maxIntensity = Math.max(peak.getRelativeIntensity(), maxIntensity);
        }
        for (int k = 0; k < mergedPeaks.size(); ++k) {
            final ProcessedPeak peak = mergedPeaks.get(k);
            peak.setRelativeIntensity(peak.getRelativeIntensity()/maxIntensity);
            peak.setIndex(k);
        }

    }

    private void preProcessMsMs(ProcessedInput processedInput) {
        for (UnmergedSpectrumProcessor p : preprocessors) {
            p.process(processedInput.getExperimentInformation());
        }
    }

    private void postProcessMsMs(ProcessedInput processedInput) {
        for (MergedSpectrumProcessor p : postProcessors) {
            p.process(processedInput);
        }
    }

    protected void mergePeaks(ProcessedInput processedInput) {
        ms2Merger.merge(processedInput);
    }

}
