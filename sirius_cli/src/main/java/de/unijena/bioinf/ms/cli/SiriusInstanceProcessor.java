package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMeasurementProfile;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilder;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilderFactory;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.DNNRegressionPredictor;
import de.unijena.bioinf.IsotopePatternAnalysis.prediction.ElementPredictor;
import de.unijena.bioinf.jjobs.BasicMasterJJob;
import de.unijena.bioinf.jjobs.exceptions.TimeoutException;
import de.unijena.bioinf.ms.cli.parameters.SiriusOptions;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.projectspace.ExperimentResult;
import de.unijena.bioinf.sirius.projectspace.ExperimentResultJJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Class to process Instances with Sirius algorithms
 * <p>
 * The Instances should be already configured (all parameters set)
 * the InstanceProcessor should now start and configure computation (j)jobs
 * based on this parameters
 */
public class SiriusInstanceProcessor implements InstanceProcessor<ExperimentResult> {

    Sirius sirius;
    SiriusOptions options;
    protected Logger logger = LoggerFactory.getLogger(SiriusInstanceProcessor.class);

    public Sirius getSirius() {
        return sirius;
    }

    @Override
    public boolean setup() {
        try {
            //todo combing profile with argument default values
            sirius = new Sirius(options.profile);
            sirius.setFastMode(!options.disableFastMode);
            final FragmentationPatternAnalysis ms2 = sirius.getMs2Analyzer();
            final IsotopePatternAnalysis ms1 = sirius.getMs1Analyzer();
            final MutableMeasurementProfile ms1Prof = new MutableMeasurementProfile(ms1.getDefaultProfile());
            final MutableMeasurementProfile ms2Prof = new MutableMeasurementProfile(ms2.getDefaultProfile());
            final String outerClassName = getClass().getName();

            //Validator warning
            ms2.setValidatorWarning(message -> LoggerFactory.getLogger(outerClassName).warn(message));

            //setting up the profile (not Instance but Sirius dependent)
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
        if (results != null) {
            int rank = 1;
            int n = Math.max(1, (int) Math.ceil(Math.log10(results.size())));
            for (IdentificationResult result : results) {
                final IsotopePattern pat = result.getRawTree().getAnnotationOrNull(IsotopePattern.class);
                final int isoPeaks = pat == null ? 0 : pat.getPattern().size();
                printf("%" + n + "d.) %s\t%s\tscore: %.2f\ttree: %+.2f\tiso: %.2f\tpeaks: %d\texplained intensity: %.2f %%\tisotope peaks: %d\n", rank++, result.getMolecularFormula().toString(), String.valueOf(result.getResolvedTree().getAnnotationOrNull(PrecursorIonType.class)), result.getScore(), result.getTreeScore(), result.getIsotopeScore(), result.getResolvedTree().numberOfVertices(), getSirius().getMs2Analyzer().getIntensityRatioOfExplainedPeaks(result.getResolvedTree()) * 100, isoPeaks);
            }


        } else {
            ExperimentResult.ErrorCause error = experimentResult.getError();
            if (error.equals(ExperimentResult.ErrorCause.NORESULTS)) {
                logger.warn("Cannot find valid tree that supports the data. You can try to increase the allowed mass deviation with parameter --ppm-max");
            } else if (error.equals(ExperimentResult.ErrorCause.TIMEOUT)) {
                println("Ignore " + experimentResult.getExperiment().getName() + " due to timeout!");
            } else {
                //todo save and output error
//                e.printStackTrace();
                println("Error during computation of " + experimentResult.getExperiment().getName() + ": " + experimentResult.getErrorMessage());
                logger.debug("Error during computation of " + experimentResult.getExperiment().getName(), experimentResult.getErrorMessage());
            }
        }
    }


    protected ExperimentResultForSiriusJJob makeSiriusJob(final Instance i) {
        Sirius.SiriusIdentificationJob job = (sirius.makeIdentificationJob(i.experiment));
        return new ExperimentResultForSiriusJJob(job);
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
