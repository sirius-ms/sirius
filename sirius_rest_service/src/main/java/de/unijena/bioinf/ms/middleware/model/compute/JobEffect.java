package de.unijena.bioinf.ms.middleware.model.compute;

public enum JobEffect {
    /**
     * Jobs that import features and/or compounds into projects from various sources.
     */
    IMPORT,
    /**
     * Jobs that compute annotations on features and/or compounds (formulaId, CSI:FingerID, CANOPUS, MsNovelist)
     */
    COMPUTATION,
    /**
     * Jobs that remove features and/or compounds from projects
     */
    DELETION
}
