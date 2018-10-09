package de.unijena.bioinf.ChemistryBase.ms;

import java.util.HashMap;
/**
 * additional parameters which are parsed from the input file and are parameters for SIRIUS computations
 * but have not an explicit field in the Experiment or Spectrum.
 */
public class AdditionalParameters extends HashMap<String, String> implements Ms2ExperimentAnnotation, SpectrumAnnotation  {
}
