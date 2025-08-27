package de.unijena.bioinf.ms.persistence.model.properties;

public enum ProjectType {
    /**
     * Flogs that not data has been imported yet
     */
    UNIMPORTED,
    /**
     * Flags project created via direct import of compounds and features from API
     */
    DIRECT_IMPORT,
    /**
     * Flags project created from Peak-list data (.ms, mgf, .mat, cef.) created by external preprocessing
     */
    PEAKLISTS,
    /**
     * Flags project created from multiple MS runs (.mzml, mzxml) that have been aligned
     */
    ALIGNED_RUNS,
    /**
     * Flags project created from one or multiple MS run(s) (.mzml, mzxml) that have NOT been aligned
     */
    UNALIGNED_RUNS
}
