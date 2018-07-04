package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractChemicalDatabase implements Closeable, Cloneable,SearchStructureByFormula, AnnotateStructures {

    /**
     * Search for molecular formulas in the database
     * @param mass exact mass of the ion
     * @param deviation allowed mass deviation
     * @param ionType adduct of the ion
     * @return list of formula candidates which theoretical mass (+ adduct mass) is within the given mass window
     */
    public abstract List<FormulaCandidate> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType ionType) throws DatabaseException;

    /**
     * Search for molecular formulas in the database
     * @param mass exact mass of the ion
     * @param deviation allowed mass deviation
     * @param ionTypes allowed adducts of the ion
     * @return list of formula candidates which theoretical mass (+ adduct mass) is within the given mass window
     */
    public List<List<FormulaCandidate>> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType[] ionTypes)  throws DatabaseException {
        ArrayList<List<FormulaCandidate>> candidates = new ArrayList<>(ionTypes.length);
        for (PrecursorIonType type : ionTypes)
            candidates.add(lookupMolecularFormulas(mass, deviation, type));
        return candidates;
    }

    /**
     * Lookup structures by the given molecular formula. This method will NOT add database links to these structures
     * @param formula
     * @return
     */
    public abstract List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws DatabaseException;

    /**
     * Lookup structures and corresponding fingerprints
     * by the given molecular formula. This method will NOT add database links to these structures
     * @param formula
     * @return
     */
    public List<FingerprintCandidate> lookupStructuresAndFingerprintsByFormula(MolecularFormula formula) throws DatabaseException {
        return lookupStructuresAndFingerprintsByFormula(formula, new ArrayList<FingerprintCandidate>());
    }

    public abstract List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws DatabaseException;

    public abstract List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws DatabaseException;

    public abstract List<FingerprintCandidate> lookupManyFingerprintsByInchis(Iterable<String> inchi_keys) throws DatabaseException;

    public abstract List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> compounds) throws DatabaseException;

    public final Fingerprint lookupFingerprintByInChI(InChI inchi) throws DatabaseException {
        final List<FingerprintCandidate> xs = lookupFingerprintsByInchis(Collections.singleton(inchi.key2D()));
        if (xs.size()>0) return xs.get(0).getFingerprint();
        else return null;
    }

    public abstract List<InChI> findInchiByNames(List<String> names) throws DatabaseException;

}
