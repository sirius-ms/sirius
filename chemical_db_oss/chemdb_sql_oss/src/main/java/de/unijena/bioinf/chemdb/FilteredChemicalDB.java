package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

// this class is just a workaround to prevent old api for the internal csi Fingerid tools and should not
// be used for new code
@Deprecated
public class FilteredChemicalDB extends AbstractChemicalDatabase implements Cloneable {


    private long filter = DataSource.ALL.flag();
    private final ChemicalDatabase wrappedDB;

    public FilteredChemicalDB() throws ChemicalDatabaseException {
        super();
        wrappedDB = new ChemicalDatabase();
    }

    public FilteredChemicalDB(ChemicalDatabase db) {
        wrappedDB = db;
    }

    public long getBioFilter() {
        return filter;
    }

    public void setBioFilter(long filter) {
        this.filter = filter;
    }

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(double mass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        return wrappedDB.lookupMolecularFormulas(filter, mass, deviation, ionType);
    }

    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        return wrappedDB.lookupStructuresByFormula(filter, formula);
    }


    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        return wrappedDB.lookupStructuresAndFingerprintsByFormula(filter, formula, fingerprintCandidates);
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        return wrappedDB.lookupFingerprintsByInchis(inchi_keys);
    }

    @Override
    public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        return wrappedDB.lookupManyInchisByInchiKeys(inchi_keys);
    }

    @Override
    public List<FingerprintCandidate> lookupManyFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        return wrappedDB.lookupManyFingerprintsByInchis(inchi_keys);
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> compounds) throws ChemicalDatabaseException {
        return wrappedDB.lookupFingerprintsByInchi(compounds);
    }

    @Override
    public List<InChI> findInchiByNames(List<String> names) throws ChemicalDatabaseException {
        return wrappedDB.findInchiByNames(names);
    }

    @Override
    public void annotateCompounds(List<? extends CompoundCandidate> sublist) throws ChemicalDatabaseException {
        wrappedDB.annotateCompounds(sublist);
    }

    @Override
    public void close() throws IOException {
        wrappedDB.close();
    }

    public FilteredChemicalDB clone(){
        FilteredChemicalDB clone = new FilteredChemicalDB(wrappedDB.clone()); //todo maybe we should not clone wrapped db here
        clone.setBioFilter(filter);
        return clone;
    }

    public ChemicalDatabase getWrappedDB() {
        return wrappedDB;
    }
}
