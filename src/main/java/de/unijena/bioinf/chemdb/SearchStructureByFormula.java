package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface SearchStructureByFormula {
    /**
     * Lookup structures and corresponding fingerprints
     * by the given molecular formula. This method will NOT add database links to these structures
     * The method pushs the compounds into the given collection (usually a
     * ConcurrentLinkedQueue), allowing the caller to process
     * asynchronously.
     */
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T candidates) throws ChemicalDatabaseException;

    default public List<FingerprintCandidate> lookupStructuresAndFingerprintsByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        return lookupStructuresAndFingerprintsByFormula(formula, new ArrayList<FingerprintCandidate>());
    }
}
