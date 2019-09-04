package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.projectspace.FormulaResultId;

import java.util.Locale;
import java.util.function.Function;

public class FingerIdLocations {

    public static Function<FormulaResultId,String> FingerBlastResults = (f)->"fingerid/%s" + f.fileName("csv");
    public static Function<FormulaResultId,String> CanopusResults = (f)->"canopus/%s" + f.fileName("fpt");

}
