package de.unijena.bioinf.fingerid.db;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.fingerid.net.WebAPI;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class FingerblastSearchEngine implements SearchStructureByFormula, AnnotateStructures{

    protected final CachedRESTDB underlyingDatabase;
    protected final SearchableDatabase queryDB;

    public FingerblastSearchEngine(CachedRESTDB underlyingDatabase, SearchableDatabase queryDB) {
        this.underlyingDatabase = underlyingDatabase;
        this.queryDB = queryDB;
    }

    @Override
    public void annotateCompounds(List<? extends CompoundCandidate> list) throws DatabaseException {
        // compounds from this database are already annotated
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula molecularFormula, T fingerprintCandidates) throws DatabaseException {
        try (final WebAPI webAPI = WebAPI.newInstance()) {
            fingerprintCandidates.addAll(underlyingDatabase.loadCompoundsByFormula(webAPI, molecularFormula, queryDB));
            return fingerprintCandidates;
        } catch (IOException e) {
            throw new DatabaseException(e);
        }
    }
}
