package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.projectspace.FormulaResultId;

import java.util.Locale;
import java.util.function.Function;

public class FingerIdLocations {

    public static final Function<FormulaResultId,String> FingerBlastResults = (f)->"fingerid/%s" + f.fileName("csv");
    public static final Function<FormulaResultId,String> CanopusResults = (f)->"canopus/%s" + f.fileName("fpt");
    public static final String CanopusDir = "canopus", FingerprintDir = "fingerprints";

}
