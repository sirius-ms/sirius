package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.Location;

public interface FingerIdLocations {
    Location
            FINGERBLAST = new Location("fingerid/", "tsv", FormulaResultId::fileName),
            FINGERBLAST_FPs = new Location("fingerid/", "fps", FormulaResultId::fileName),
            FINGERPRINTS = new Location("fingerprints/", "fpt", FormulaResultId::fileName);

    String
            FINGERID_CLIENT_DATA = "csi_fingerid.tsv",
            FINGERID_CLIENT_DATA_NEG = "csi_fingerid_neg.tsv";
}
