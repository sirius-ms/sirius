package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.Location;

import java.util.Locale;
import java.util.function.Function;

public interface FingerIdLocations {

    Location
            FINGERBLAST = new Location("fingerid/", "csv", FormulaResultId::fileName),
            FINGERBLAST_FPs = new Location("fingerid/", "fps", FormulaResultId::fileName),
            FINGERPRINTS = new Location("fingerprints/", "fpt", FormulaResultId::fileName),
            CANOPUS = new Location("canopus/", "fpt", FormulaResultId::fileName);


    String
            FINGERID_CLIENT_DATA = "csi_fingerid.csv",
            CANOPUS_CLIENT_DATA = "canopus.csv";
}
