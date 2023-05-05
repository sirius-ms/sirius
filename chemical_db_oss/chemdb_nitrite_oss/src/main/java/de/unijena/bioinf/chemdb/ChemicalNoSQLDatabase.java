package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.Index;
import de.unijena.bioinf.storage.db.nosql.IndexType;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class ChemicalNoSQLDatabase<DocType> implements AbstractChemicalDatabase {

    protected static final String FORMULA_COLLECTION = "formula";
    protected static final String COMPOUND_COLLECTION = "compound";

    protected static final Map<String, Index[]> INDEX = Map.of(
            FORMULA_COLLECTION, new Index[]{
                    new Index("formula", IndexType.UNIQUE),
                    new Index("mass", IndexType.NON_UNIQUE),
                    new Index("bitset", IndexType.NON_UNIQUE)
            },
            COMPOUND_COLLECTION, new Index[]{
                    new Index("formula", IndexType.NON_UNIQUE),
                    new Index("mass", IndexType.NON_UNIQUE),
                    new Index("bitset", IndexType.NON_UNIQUE),
                    new Index("name", IndexType.NON_UNIQUE),
                    new Index("inchikey", IndexType.NON_UNIQUE)
            }
            // TODO what about compund/fingerprint being essentially the same!?
    );

    final protected Database<DocType> database;

    final protected NoSQLSerializer<DocType> serializer;
    
    final protected FingerprintVersion version;

    public ChemicalNoSQLDatabase(Database<DocType> database, NoSQLSerializer<DocType> serializer) {
        this.database = database;
        this.serializer = serializer;
        this.version = CdkFingerprintVersion.getDefault();
    }

    public ChemicalNoSQLDatabase(Database<DocType> database, NoSQLSerializer<DocType> serializer, FingerprintVersion version) {
        this.database = database;
        this.serializer = serializer;
        this.version = version;
    }

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(double ionMass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        try {
            final double mass = ionType.precursorMassToNeutralMass(ionMass);
            final double from = mass - deviation.absoluteFor(mass);
            final double to = mass + deviation.absoluteFor(mass);
            return StreamSupport.stream(this.database.find(FORMULA_COLLECTION, new Filter().and().gte("mass", from).lte("mass", to)).spliterator(), false).map((d) -> serializer.deserializeFormula(d, ionType)).collect(Collectors.toList());
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public boolean containsFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        try {
            return this.database.count(FORMULA_COLLECTION, new Filter().eq("formula", formula.toString())) > 0;
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        try {
            return StreamSupport.stream(database.find(COMPOUND_COLLECTION, new Filter().eq("formula", formula.toString())).spliterator(), false).map(serializer::deserializeCompound).collect(Collectors.toList());
        } catch (RuntimeException | IOException e) {
            throw new ChemicalDatabaseException(e);
        }

    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        try {
            Object[] keys = StreamSupport.stream(inchi_keys.spliterator(), false).toArray();
            return StreamSupport.stream(database.find(COMPOUND_COLLECTION, new Filter().in("inchikey", keys)).spliterator(), false).map((d) -> serializer.deserializeFingerprint(d, version)).collect(Collectors.toList());
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        try {
            Object[] keys = StreamSupport.stream(inchi_keys.spliterator(), false).toArray();
            return StreamSupport.stream(database.find(COMPOUND_COLLECTION, new Filter().in("inchikey", keys)).spliterator(), false).map(serializer::deserializeInchi).collect(Collectors.toList());
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<FingerprintCandidate> lookupManyFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        return this.lookupFingerprintsByInchis(inchi_keys);
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> compounds) throws ChemicalDatabaseException {
        try {
            Object[] keys = StreamSupport.stream(compounds.spliterator(), false).map(c -> c.inchi.key).toArray();
            return StreamSupport.stream(database.find(COMPOUND_COLLECTION, new Filter().in("inchikey", keys)).spliterator(), false).map((d) -> serializer.deserializeFingerprint(d, version)).collect(Collectors.toList());
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<InChI> findInchiByNames(List<String> names) throws ChemicalDatabaseException {
        try {
            Object[] keys = names.toArray();
            return StreamSupport.stream(database.find(COMPOUND_COLLECTION, new Filter().in("name", keys)).spliterator(), false).map(serializer::deserializeInchi).collect(Collectors.toList());
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public String getChemDbDate() throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void annotateCompounds(List<? extends CompoundCandidate> sublist) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        try {
            StreamSupport.stream(database.find(COMPOUND_COLLECTION, new Filter().eq("formula", formula)).spliterator(), false).forEach((d) -> fingerprintCandidates.add(serializer.deserializeFingerprint(d, version)));
            return fingerprintCandidates;
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public void close() throws IOException {
        this.database.close();
    }

    public abstract <C extends CompoundCandidate> void importCompoundsAndFingerprints(MolecularFormula key, Iterable<C> candidates) throws ChemicalDatabaseException;

}
