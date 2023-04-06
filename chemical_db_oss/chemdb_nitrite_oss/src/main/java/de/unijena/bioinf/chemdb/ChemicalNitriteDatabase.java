package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.Index;
import de.unijena.bioinf.storage.db.nosql.IndexType;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ChemicalNitriteDatabase implements AbstractChemicalDatabase {

    protected NitriteDatabase database;
    
    protected FingerprintVersion version;

    public ChemicalNitriteDatabase(Path file) {
        this.database = new NitriteDatabase(file, Map.of(
                "FormulaCandidate", new Index[]{new Index("mass", IndexType.NON_UNIQUE)}
        ));
        this.version = CdkFingerprintVersion.getDefault();
    }

    public ChemicalNitriteDatabase(Path file, FingerprintVersion version) {
        this.database = new NitriteDatabase(file, Map.of(
                "FormulaCandidate", new Index[]{new Index("mass", IndexType.NON_UNIQUE)}
        ));
        this.version = version;
    }

    // additional fields
    // FormulaCandidate: String formula, double mass, long flags
    // CompoundCandidate, FingerprintCandidate: String formula
    // TODO remove distinction between compoundcandidate and fingerprint candidate collection!

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(double ionMass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        try {
            final double mass = ionType.precursorMassToNeutralMass(ionMass);
            final double from = mass - deviation.absoluteFor(mass);
            final double to = mass + deviation.absoluteFor(mass);
            return StreamSupport.stream(this.database.find("FormulaCandidate", new Filter().and().gte("mass", from).lte("mass", to)).spliterator(), false).map((d) -> Deserializer.deserializeFormula(d, ionType)).collect(Collectors.toList());
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public boolean containsFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        try {
            return this.database.count("FormulaCandidate", new Filter().eq("formula", formula.toString())) > 0;
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        try {
            return StreamSupport.stream(database.find("CompoundCandidate", new Filter().eq("formula", formula.toString())).spliterator(), false).map(Deserializer::deserializeCompound).collect(Collectors.toList());
        } catch (RuntimeException | IOException e) {
            throw new ChemicalDatabaseException(e);
        }

    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        try {
            Object[] keys = StreamSupport.stream(inchi_keys.spliterator(), false).toArray();
            return StreamSupport.stream(database.find("FingerprintCandidate", new Filter().in("inchikey", keys)).spliterator(), false).map((d) -> Deserializer.deserializeFingerprint(d, version)).collect(Collectors.toList());
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        try {
            Object[] keys = StreamSupport.stream(inchi_keys.spliterator(), false).toArray();
            return StreamSupport.stream(database.find("FingerprintCandidate", new Filter().in("inchikey", keys)).spliterator(), false).map(Deserializer::deserializeInchi).collect(Collectors.toList());
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
            return StreamSupport.stream(database.find("FingerprintCandidate", new Filter().in("inchikey", keys)).spliterator(), false).map((d) -> Deserializer.deserializeFingerprint(d, version)).collect(Collectors.toList());
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<InChI> findInchiByNames(List<String> names) throws ChemicalDatabaseException {
        try {
            Object[] keys = names.toArray();
            return StreamSupport.stream(database.find("FingerprintCandidate", new Filter().in("name", keys)).spliterator(), false).map(Deserializer::deserializeInchi).collect(Collectors.toList());
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
            StreamSupport.stream(database.find("FingerprintCandidate", new Filter().eq("formula", formula)).spliterator(), false).forEach((d) -> {
                fingerprintCandidates.add(Deserializer.deserializeFingerprint(d, version));
            });
            return fingerprintCandidates;
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public void close() throws IOException {
        this.database.close();
    }
}
