package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class FingerblastSearchEngine implements SearchStructureByFormula, AnnotateStructures{

    protected final RestWithCustomDatabase underlyingDatabase;
    protected final Collection<SearchableDatabase> queryDBs;

    FingerblastSearchEngine(RestWithCustomDatabase underlyingDatabase, Collection<SearchableDatabase> queryDBs) {
        this.underlyingDatabase = underlyingDatabase;
        this.queryDBs = queryDBs;
    }

    @Override
    public void annotateCompounds(List<? extends CompoundCandidate> list) throws ChemicalDatabaseException {
        // compounds from this database are already annotated
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula molecularFormula, T fingerprintCandidates) throws ChemicalDatabaseException {
        try  {
            fingerprintCandidates.addAll(underlyingDatabase.loadCompoundsByFormula(molecularFormula, queryDBs));
            return fingerprintCandidates;
        } catch (IOException e) {
            throw new ChemicalDatabaseException("", e);
        }
    }
}
