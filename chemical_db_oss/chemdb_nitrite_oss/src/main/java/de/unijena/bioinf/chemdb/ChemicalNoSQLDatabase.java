package de.unijena.bioinf.chemdb;

import com.google.api.client.util.Lists;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.chemdb.nitrite.wrappers.CompoundCandidateWrapper;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintCandidateWrapper;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FormulaCandidateDeserializer;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FormulaCandidateSerializer;
import de.unijena.bioinf.storage.db.nosql.*;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class ChemicalNoSQLDatabase<DocType> implements AbstractChemicalDatabase {

    final protected Database<DocType> database;

    public ChemicalNoSQLDatabase(Database<DocType> database) throws IOException {
        this.database = database;
    }

    protected static Metadata initMetadata(FingerprintVersion version) throws IOException {
        return Metadata.build()
                .addRepository(
                        FormulaCandidate.class,
                        new FormulaCandidateSerializer(),
                        new FormulaCandidateDeserializer(),
                        new Index("formula", IndexType.UNIQUE),
                        new Index("mass", IndexType.NON_UNIQUE),
                        new Index("bitset", IndexType.NON_UNIQUE)
                ).addRepository(
                        CompoundCandidateWrapper.class,
                        "id",
                        new CompoundCandidateWrapper.WrapperSerializer(),
                        new CompoundCandidateWrapper.WrapperDeserializer(version),
                        new Index("formula", IndexType.NON_UNIQUE),
                        new Index("mass", IndexType.NON_UNIQUE),
                        new Index("candidate.bitset", IndexType.NON_UNIQUE),
                        new Index("candidate.name", IndexType.NON_UNIQUE),
                        new Index("candidate.inchikey", IndexType.NON_UNIQUE)
                ).addRepository(
                        FingerprintCandidateWrapper.class,
                        "id",
                        new FingerprintCandidateWrapper.WrapperSerializer(),
                        new FingerprintCandidateWrapper.WrapperDeserializer(version),
                        new Index("formula", IndexType.NON_UNIQUE),
                        new Index("mass", IndexType.NON_UNIQUE),
                        new Index("candidate.bitset", IndexType.NON_UNIQUE),
                        new Index("candidate.name", IndexType.NON_UNIQUE),
                        new Index("candidate.inchikey", IndexType.NON_UNIQUE)
                );
    }

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(double ionMass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        try {
            final double mass = ionType.precursorMassToNeutralMass(ionMass);
            final double from = mass - deviation.absoluteFor(mass);
            final double to = mass + deviation.absoluteFor(mass);
            return Lists.newArrayList(this.database.find(new Filter().and().gte("mass", from).lte("mass", to), FormulaCandidate.class));
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public boolean containsFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        try {
            return this.database.count(new Filter().eq("formula", formula.toString()), FormulaCandidate.class) > 0;
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        try {
            return StreamSupport.stream(database.find(new Filter().eq("formula", formula.toString()), CompoundCandidateWrapper.class).spliterator(), false).map(c -> c.candidate).collect(Collectors.toList());
        } catch (RuntimeException | IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        try {
            Object[] keys = StreamSupport.stream(inchi_keys.spliterator(), false).toArray();
            return StreamSupport.stream(database.find(new Filter().in("inchikey", keys), FingerprintCandidateWrapper.class).spliterator(), false).map(c -> c.candidate).collect(Collectors.toList());
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        try {
            Object[] keys = StreamSupport.stream(inchi_keys.spliterator(), false).toArray();
            return StreamSupport.stream(database.find(new Filter().in("inchikey", keys), FingerprintCandidateWrapper.class).spliterator(), false).map(c -> c.candidate.inchi).collect(Collectors.toList());
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
            return StreamSupport.stream(database.find(new Filter().in("inchikey", keys), FingerprintCandidateWrapper.class).spliterator(), false).map(c -> c.candidate).collect(Collectors.toList());
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<InChI> findInchiByNames(List<String> names) throws ChemicalDatabaseException {
        try {
            Object[] keys = names.toArray();
            return StreamSupport.stream(database.find(new Filter().in("name", keys), CompoundCandidateWrapper.class).spliterator(), false).map(c -> c.candidate.inchi).collect(Collectors.toList());
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
            StreamSupport.stream(database.find(new Filter().eq("formula", formula), FingerprintCandidateWrapper.class).spliterator(), false).forEach(c -> fingerprintCandidates.add(c.candidate));
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
