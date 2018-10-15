package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.inputValidators.Warning;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilderFactory;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.DNNRegressionPredictor;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.ElementPredictor;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.BufferedJJobSubmitter;
import de.unijena.bioinf.jjobs.exceptions.TimeoutException;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.projectspace.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class SiriusInstanceProcessor implements  InstanceProcessor<ExperimentResult> {

    Sirius sirius;
    SiriusOptions options;
    protected Logger logger = LoggerFactory.getLogger(SiriusInstanceProcessor.class);

    public SiriusInstanceProcessor(SiriusOptions options) {
        this.options = options;
    }

    public Sirius getSirius() {
        return sirius;
    }

    @Override
    public boolean setup() {
        try {
            this.sirius = new Sirius(options.getProfile());
            Sirius.USE_FAST_MODE = !options.isDisableFastMode();
//            if (options.isDisableFastMode()) LoggerFactory.getLogger(CLI.class).info("Use experimental fast mode. Results might differ from default mode.");
            final FragmentationPatternAnalysis ms2 = sirius.getMs2Analyzer();
            final IsotopePatternAnalysis ms1 = sirius.getMs1Analyzer();
            final MutableMeasurementProfile ms1Prof = new MutableMeasurementProfile(ms1.getDefaultProfile());
            final MutableMeasurementProfile ms2Prof = new MutableMeasurementProfile(ms2.getDefaultProfile());
            final String outerClassName = getClass().getName();
            ms2.setValidatorWarning(new Warning() {
                @Override
                public void warn(String message) {
                    //todo changed from java util Logger
                    LoggerFactory.getLogger(outerClassName).warn(message);
                }
//                public void warn(String message) {
//                    LoggerFactory.getLogger(outerClassName).warn(message);
//                }
            });
//            if (options.getElements() == null) {
//                // autodetect and use default set
//                ms1Prof.setFormulaConstraints(getDefaultElementSet(options));
//                ms2Prof.setFormulaConstraints(getDefaultElementSet(options));
//            } else {
//                ms2Prof.setFormulaConstraints(options.getElements());
//                ms1Prof.setFormulaConstraints(options.getElements());
//            }

            if (options.getMedianNoise() != null) {
                ms2Prof.setMedianNoiseIntensity(options.getMedianNoise());
            }
            if (options.getPPMMax() != null) {
                ms2Prof.setAllowedMassDeviation(new Deviation(options.getPPMMax()));
                ms1Prof.setAllowedMassDeviation(new Deviation(options.getPPMMax()));
            }
            if (options.getPPMMaxMs2() != null) {
                ms2Prof.setAllowedMassDeviation(new Deviation(options.getPPMMax()));
            }

            final TreeBuilder builder = sirius.getMs2Analyzer().getTreeBuilder();
            if (builder == null) {
                String noILPSolver = "Could not load a valid ILP solver (TreeBuilder) " + Arrays.toString(TreeBuilderFactory.getBuilderPriorities()) + ". Please read the installation instructions.";
                logger.error(noILPSolver);
                System.exit(1);
            }
            logger.info("Compute trees using " + builder);


            sirius.getMs2Analyzer().setDefaultProfile(ms2Prof);
            sirius.getMs1Analyzer().setDefaultProfile(ms1Prof);

            if (options.isEnableSiliconDetection()) {
                ElementPredictor elementPredictor = sirius.getElementPrediction();
                if (elementPredictor instanceof DNNRegressionPredictor) {
                    ((DNNRegressionPredictor) elementPredictor).enableSilicon();
                }
            }

            /*
            if (options.getPossibleIonizations() != null) {
                List<String> ionList = options.getPossibleIonizations();
                if (ionList.size() == 1) {
                    ionList = Arrays.asList(ionList.get(0).split(","));
                }
                if (ionList.size() == 1) {
                    logger.error("Cannot guess ionization when only one ionization/adduct is provided");
                }
                ionTypes = new PrecursorIonType[ionList.size()];
                Set<PrecursorIonType> set = new HashSet<>();
                for (int i = 0; i < ionTypes.length; i++) {
                    String ion = ionList.get(i);
                    ionTypes[i] = PrecursorIonType.getPrecursorIonType(ion);
                    set.add(ionTypes[i].withoutAdduct());
                }
                ionTypesWithoutAdducts = set.toArray(new PrecursorIonType[0]);

            }
            */
        } catch (IOException e) {
            logger.error("Cannot load profile '" + options.getProfile() + "':\n", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean validate() {
        return true;
    }

    @Override
    public void output(ExperimentResult experimentResult) {
        //sirius output
        List<IdentificationResult> results = experimentResult.getResults();
        if (results!=null){
            int rank = 1;
            int n = Math.max(1, (int) Math.ceil(Math.log10(results.size())));
            for (IdentificationResult result : results) {
                final IsotopePattern pat = result.getRawTree().getAnnotationOrNull(IsotopePattern.class);
                final int isoPeaks = pat == null ? 0 : pat.getPattern().size();
                printf("%" + n + "d.) %s\t%s\tscore: %.2f\ttree: %+.2f\tiso: %.2f\tpeaks: %d\texplained intensity: %.2f %%\tisotope peaks: %d\n", rank++, result.getMolecularFormula().toString(), String.valueOf(result.getResolvedTree().getAnnotationOrNull(PrecursorIonType.class)), result.getScore(), result.getTreeScore(), result.getIsotopeScore(), result.getResolvedTree().numberOfVertices(), getSirius().getMs2Analyzer().getIntensityRatioOfExplainedPeaks(result.getResolvedTree()) * 100, isoPeaks);
            }



        } else {
            ExperimentResult.ErrorCause error = experimentResult.getError();
            if (error.equals(ExperimentResult.ErrorCause.NORESULTS)){
                logger.warn("Cannot find valid tree that supports the data. You can try to increase the allowed mass deviation with parameter --ppm-max");
            } else if (error.equals(ExperimentResult.ErrorCause.TIMEOUT)) {
                println("Ignore " + experimentResult.getExperiment().getName() + " due to timeout!");

//            } else if (errorString.equals("ERROR")){
            } else {
                //todo save and output error
//                e.printStackTrace();
                println("Error during computation of " + experimentResult.getExperiment().getName() + ": " + experimentResult.getErrorMessage());
                logger.debug("Error during computation of " + experimentResult.getExperiment().getName(), experimentResult.getErrorMessage());
            }
        }
    }


    protected ExperimentResultForSiriusJJob makeSiriusJob(final Instance i) {
        Sirius.SiriusIdentificationJob job = (sirius.makeIdentificationJob(i.experiment, getNumberOfCandidates()));
        return new ExperimentResultForSiriusJJob(job);
    }

    private Integer getNumberOfCandidates() {
        return options.getNumberOfCandidates() != null ? options.getNumberOfCandidates() : 5;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public class ExperimentResultForSiriusJJob extends BasicMasterJJob<ExperimentResult> implements ExperimentResultJJob {

        private Sirius.SiriusIdentificationJob siriusIdentificationJob;

        public ExperimentResultForSiriusJJob(Sirius.SiriusIdentificationJob siriusIdentificationJob) {
            super(JobType.CPU);
            this.siriusIdentificationJob = siriusIdentificationJob;
        }

        @Override
        protected ExperimentResult compute() throws Exception {
            try {
//                 = submitSubJob(siriusIdentificationJob).takeResult();
                final List<IdentificationResult> results = siriusIdentificationJob.call();
                if (!results.isEmpty()) {

                    return new ExperimentResult(siriusIdentificationJob.getExperiment(), results);
                } else {

                    return new ExperimentResult(siriusIdentificationJob.getExperiment(), null, ExperimentResult.ErrorCause.NORESULTS);
                }
            } catch (TimeoutException e) {

                return new ExperimentResult(siriusIdentificationJob.getExperiment(), null, ExperimentResult.ErrorCause.TIMEOUT);
            }
        }

        @Override
        public Ms2Experiment getExperiment() {
            return siriusIdentificationJob.getExperiment();
        }

    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void print(String s) {
        if (!CombinedCLI.shellOutputSurpressed) System.out.print(s);
    }

    public void println(String s) {
        if (!CombinedCLI.shellOutputSurpressed) System.out.println(s);
    }

    protected void printf(String msg, Object... args) {
        if (!CombinedCLI.shellOutputSurpressed)
            System.out.printf(Locale.US, msg, args);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
