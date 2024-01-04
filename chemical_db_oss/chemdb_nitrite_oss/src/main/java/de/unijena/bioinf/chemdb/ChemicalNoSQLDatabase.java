/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb;

import com.google.common.collect.Iterables;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.chemdb.nitrite.serializers.FingerprintCandidateWrapperDeserializer;
import de.unijena.bioinf.chemdb.nitrite.serializers.FingerprintCandidateWrapperSerializer;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintCandidateWrapper;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDatabase;
import de.unijena.bioinf.storage.db.nosql.*;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static de.unijena.bioinf.chemdb.ChemDbTags.TAG_DATE;

public abstract class ChemicalNoSQLDatabase<Doctype> extends SpectralNoSQLDatabase<Doctype> implements AbstractChemicalDatabase, WriteableChemicalDatabase {

    public static final String SETTINGS_COLLECTION = "CUSTOM-DB-SETTINGS";

    public ChemicalNoSQLDatabase(Database<Doctype> database) throws IOException {
        super(database);
    }

    protected static Metadata initMetadata(FingerprintVersion version) throws IOException {
        Metadata metadata = SpectralNoSQLDatabase.initMetadata();
        return metadata
                .addRepository(ChemicalNoSQLDatabase.Tag.class, new Index("key",IndexType.UNIQUE))
                .addRepository(
                        FingerprintCandidateWrapper.class,
                        new Index("formula", IndexType.NON_UNIQUE),
                        new Index("mass", IndexType.NON_UNIQUE)
                ).addCollection(
                        SETTINGS_COLLECTION
                ).setOptionalFields(
                        FingerprintCandidateWrapper.class, "fingerprint"
                ).addSerialization(
                        FingerprintCandidateWrapper.class,
                        new FingerprintCandidateWrapperSerializer(),
                        new FingerprintCandidateWrapperDeserializer(version)
                );
    }

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(double ionMass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        try {
            final double mass = ionType.precursorMassToNeutralMass(ionMass);
            final double from = mass - deviation.absoluteFor(mass);
            final double to = mass + deviation.absoluteFor(mass);
            return this.storage.findStr(Filter.build().and().gte("mass", from).lte("mass", to), FingerprintCandidateWrapper.class)
                    .map(c -> c.getCandidate().toFormulaCandidate(ionType)).toList();
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public boolean containsFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        try {
            return this.storage.count(new Filter().eq("formula", formula.toString()), FingerprintCandidateWrapper.class, 0, 1) > 0;
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        try {
            return storage.findStr(new Filter().eq("formula", formula.toString()), FingerprintCandidateWrapper.class)
                    .map(FingerprintCandidateWrapper::getCandidate).toList();
        } catch (RuntimeException | IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        try {
            storage.findStr(Filter.build().eq("formula", formula.toString()), FingerprintCandidateWrapper.class, "fingerprint")
                    .map(FingerprintCandidateWrapper::getFingerprintCandidate)
                    .forEach(fingerprintCandidates::add);
            return fingerprintCandidates;
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    public Stream<FingerprintCandidate> lookupFingerprintsByInchisStr(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        try {
            Object[] keys = StreamSupport.stream(inchi_keys.spliterator(), false).toArray();
            return storage.findStr(Filter.build().in("candidate.inchikey", keys), FingerprintCandidateWrapper.class, "fingerprint")
                    .map(FingerprintCandidateWrapper::getFingerprintCandidate);
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchis(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        return lookupFingerprintsByInchisStr(inchi_keys).toList();
    }

    @Override
    public List<InChI> lookupManyInchisByInchiKeys(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        return lookupFingerprintsByInchisStr(inchi_keys).map(FingerprintCandidate::getInchi).toList();
    }

    @Override
    public List<FingerprintCandidate> lookupFingerprintsByInchi(Iterable<CompoundCandidate> compounds) throws ChemicalDatabaseException {
        Iterable<String> inchiIt = () -> {
            Iterator<CompoundCandidate> it = compounds.iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public String next() {
                    return it.next().getInchiKey2D();
                }
            };
        };
        return lookupFingerprintsByInchis(inchiIt);
    }

    @Override
    public List<InChI> findInchiByNames(List<String> names) throws ChemicalDatabaseException {
        try {
            return storage.findStr(new Filter().in("candidate.name", names.toArray()), FingerprintCandidateWrapper.class)
                    .map(fc -> fc.getCandidate().getInchi()).toList();
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public String getChemDbDate() throws ChemicalDatabaseException {
        try {
            return storage.findStr(Filter.build().eq("key", TAG_DATE), Tag.class, 0, 1)
                    .map(Tag::getValue).findFirst()
                    .orElseThrow(() -> new MissingResourceException("Could not find Database Date tag", "Tag", TAG_DATE));
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public void annotateCompounds(List<? extends CompoundCandidate> sublist) {
        throw new UnsupportedOperationException("Compounds of this database are already Annotated! So annotation is not supported!");
    }

    @Override
    public long countAllFingerprints() throws ChemicalDatabaseException {
        try {
            return this.storage.countAll(FingerprintCandidateWrapper.class);
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public long countAllFormulas() throws ChemicalDatabaseException {
        try {
            return this.storage.findAllStr(FingerprintCandidateWrapper.class).map(FingerprintCandidateWrapper::getFormula).distinct().count();
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public void updateTags(@Nullable String dbFlavor, int fpId) throws IOException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String dbDate = df.format(new Date());
        upsertTag(ChemDbTags.TAG_DATE, dbDate);
        if (dbFlavor != null && !dbFlavor.isBlank()) {
            upsertTag(ChemDbTags.TAG_FLAVOR, dbFlavor);
        }
        if (fpId >= 0) {
            upsertTag(ChemDbTags.TAG_FP_ID, String.valueOf(fpId));
        }
    }

    private void upsertTag(String key, String value) throws IOException {
        if (this.storage.count(Filter.build().eq("key", key), ChemicalNoSQLDatabase.Tag.class) > 0) {
            ChemicalNoSQLDatabase.Tag tag = this.storage.find(Filter.build().eq("key", key), ChemicalNoSQLDatabase.Tag.class).iterator().next();
            tag.setValue(value);
            this.storage.upsert(tag);
        } else {
            this.storage.insert(ChemicalNoSQLDatabase.Tag.of(key, value));
        }
    }

    @Override
    public void updateAllFingerprints(Consumer<FingerprintCandidate> updater) throws ChemicalDatabaseException {
        try {
            Iterables.partition(this.storage.findAll(FingerprintCandidateWrapper.class, "fingerprint"), 50).forEach(chunk -> {
                List<FingerprintCandidateWrapper> updated = chunk.stream().peek(wrapper -> updater.accept(wrapper.getFingerprintCandidate())).toList();
                try {
                    this.storage.upsertAll(updated);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException | IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tag {

        @Id
        private long id;
        private String key;
        private String value;

        public static Tag of(String key, String value) {
            return new Tag(-1L, key, value);
        }

        public static Tag of(Map.Entry<String, String> source) {
            return of(source.getKey(), source.getValue());
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public String setValue(String value) {
            String old = this.value;
            this.value = value;
            return old;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChemicalNoSQLDatabase.Tag tag)) return false;
            return Objects.equals(key, tag.key) && Objects.equals(value, tag.value);
        }

        @Override
        public String toString() {
            return "Tag{" +
                    "key='" + key + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }
    }

}
