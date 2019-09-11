package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.Locations;

import java.util.function.Function;

public class SiriusLocations extends Locations {

    public final static String
            MS2_EXPERIMENT = "spectrum.ms",
            COMPOUND_INFO = "compound.info",
            COMPOUND_CONFIG = "compound.config";
//            RESULT_RANKING = "ranking.info";

    public final static Function<FormulaResultId, String>
            TREES = (id) -> "trees/" + id.fileName("json"),
            SCORES = (id) -> "scores/" + id.fileName("info");
}
