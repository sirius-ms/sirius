package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by Marcus Ludwig on 19.07.16.
 */
public class NewBioChemicalDatabase extends ChemicalDatabase {
    public NewBioChemicalDatabase() throws DatabaseException {
        super();
    }

    public NewBioChemicalDatabase(String host, String username, String password) throws DatabaseException {
        super(host, username, password);
    }

    public NewBioChemicalDatabase(ChemicalDatabase db) throws DatabaseException {
        super(db);
    }
    @Override
    public NewBioChemicalDatabase clone() throws CloneNotSupportedException {
        try {
            NewBioChemicalDatabase db =  new NewBioChemicalDatabase(this);
            db.setBioFilter(this.getBioFilter());
            return db;
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected PreparedStatement sqlLookupStructureBio(MolecularFormula f) throws SQLException {
        if (_sqlLookupStructureBio == null)
            _sqlLookupStructureBio = connection.prepareStatement(
                    "SELECT inchi_key_1, inchi, '', '', flags FROM biodb16_06_13 WHERE formula = ?"
            );
        _sqlLookupStructureBio.setString(1, f.toString());
        return _sqlLookupStructureBio;
    }

    @Override
    protected PreparedStatement sqlLookupStructureAndFingerprintsBio(MolecularFormula f) throws SQLException {
        if (_sqlLookupStructureAndFingerprintsBio == null)
            _sqlLookupStructureAndFingerprintsBio = connection.prepareStatement(
                    "SELECT s.inchi_key_1, s.inchi, '', '', s.flags, f.fingerprint FROM biodb16_06_13 as s, fingerprints as f WHERE s.formula = ? AND f.fp_id = 1 AND f.inchi_key_1 = s.inchi_key_1"
            );
        _sqlLookupStructureAndFingerprintsBio.setString(1, f.toString());
        return _sqlLookupStructureAndFingerprintsBio;
    }
}
