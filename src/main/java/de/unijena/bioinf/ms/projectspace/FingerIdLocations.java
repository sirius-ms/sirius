package de.unijena.bioinf.ms.projectspace;

public interface FingerIdLocations extends SiriusLocations {
    Location FINGERID_FINGERPRINT = new Location("fingerprints", null, ".fpt");
    Location FINGERID_FINGERPRINT_INDEX = new Location(null, "fingerprints", ".csv");
    Location FINGERID_CANDIDATES = new Location("csi_fingerid", null, ".csv");
    Location FINGERID_FINGERPRINT_INFO = new Location("fingerprints", null, ".info");
    Location FINGERID_SUMMARY = new Location(null, "summary_csi_fingerid", ".csv");

    Location CANOPUS_FINGERPRINT = new Location("canopus", null, ".fpt");
    Location CANOPUS_FINGERPRINT_INDEX = new Location(null, "canopus", ".csv");

    Location WORKSPACE_SUMMARY = new Location(null, "analysis_report", ".mztab");
}
