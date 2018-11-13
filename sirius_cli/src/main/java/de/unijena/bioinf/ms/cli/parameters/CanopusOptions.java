package de.unijena.bioinf.ms.cli.parameters;

import picocli.CommandLine.Option;

import java.io.File;
/**
 * This is for Canopus specific parameters.
 *
 * It will be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 * */
public class CanopusOptions implements Options {
    @Option(names = {"-C","--canopus"})
    public boolean canopus;


    @Option(names = {"--experimental-canopus"}, hidden = true) //experimental/temporary/internal
    public File experimentalCanopus;
}
