package de.unijena.bioinf.ms.cli.parameters;

import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;
/**
 * This is for Canopus specific parameters.
 *
 * It will be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 * */
@CommandLine.Command(name = "canopus", aliases = {"C"}, description = "Predict compound categories for the whole dataset using CANOPUS.")
public class CanopusOptions extends AbstractMs2ExperimentOptions {
//    @Option(names = {"-C","--canopus"})
//    public boolean canopus;


    @Option(names = {"--experimental-canopus"}, hidden = true) //experimental/temporary/internal
    public File experimentalCanopus;

    @Override
    public void setParamatersToExperiment(MutableMs2Experiment experiment) {
        //todo fill me
    }
}
