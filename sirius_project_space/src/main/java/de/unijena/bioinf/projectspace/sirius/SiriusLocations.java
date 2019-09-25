package de.unijena.bioinf.projectspace.sirius;

import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.Location;

public interface SiriusLocations {

    String
            MS2_EXPERIMENT = "spectrum.ms",
            COMPOUND_INFO = "compound.info",
            COMPOUND_CONFIG = "compound.config",
            SIRIUS_SUMMARY = "sirius_summary.csv";

    Location
            SPECTRA = new Location("spectra/", "ms", FormulaResultId::fileName),
            TREES = new Location("trees/", "json", FormulaResultId::fileName),
            SCORES = new Location("scores/", "info", FormulaResultId::fileName),
            DECOYS = new Location("decoys/", "tsv", FormulaResultId::fileName);
}
