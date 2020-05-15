package de.unijena.bioinf.projectspace.canopus;

import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.Location;

public interface CanopusLocations {

    Location
            CANOPUS = new Location("canopus/", "fpt", FormulaResultId::fileName);


    String
            CANOPUS_CLIENT_DATA = "canopus.tsv",
            CANOPUS_CLIENT_DATA_NEG = "canopus_neg.tsv";
}
