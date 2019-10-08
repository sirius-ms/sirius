package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.annotations.SpectrumAnnotation;

import java.util.HashMap;

/**
 * additional fields and comments which are parsed from the input file and are no parameters for SIRIUS computations.
 */
public class AdditionalFields extends HashMap<String, String> implements Ms2ExperimentAnnotation, SpectrumAnnotation {


}
