package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.fingerid.blast.FingerblastResult;
import de.unijena.bioinf.projectspace.fingerid.*;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;

/**
 * has to be called on startup to setup the project space
 */
public class FingerIdProjectSpaceConfiguration {

    public static void configure(ProjectSpaceConfiguration configuration) {

        configuration.registerComponent(FormulaResult.class, FingerprintResult.class, new FingerprintSerializer());
        configuration.registerComponent(FormulaResult.class, CanopusResult.class , new CanopusSerializer());
        configuration.registerComponent(FormulaResult.class, FingerblastResult.class, new FingerblastResultSerializer());

        configuration.defineProjectSpaceProperty(CSIClientData.class, new CsiClientSerializer());
        configuration.defineProjectSpaceProperty(CanopusClientData.class, new CanopusClientSerializer());
    }

}
