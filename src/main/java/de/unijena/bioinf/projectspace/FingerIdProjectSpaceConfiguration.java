package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.fingerid.blast.FBCandidateFingerprints;
import de.unijena.bioinf.fingerid.blast.FBCandidates;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusData;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.fingerid.*;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;

/**
 * has to be called on startup to setup the project space
 */
public class FingerIdProjectSpaceConfiguration {

    public static void configure(ProjectSpaceConfiguration configuration) {
        configuration.registerComponent(FormulaResult.class, FingerprintResult.class, new FingerprintSerializer());
        configuration.registerComponent(FormulaResult.class, CanopusResult.class , new CanopusSerializer());
        configuration.registerComponent(FormulaResult.class, FBCandidates.class, new FBCandidatesSerializer());
        configuration.registerComponent(FormulaResult.class, FBCandidateFingerprints.class, new FBCandidateFingerprintSerializer());

        configuration.defineProjectSpaceProperty(FingerIdData.class, new CsiClientSerializer());
        configuration.defineProjectSpaceProperty(CanopusData.class, new CanopusClientSerializer());
    }

}
