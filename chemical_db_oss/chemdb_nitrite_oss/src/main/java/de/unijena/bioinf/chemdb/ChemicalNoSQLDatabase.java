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

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.chemdb.nitrite.serializers.FingerprintCandidateWrapperDeserializer;
import de.unijena.bioinf.chemdb.nitrite.serializers.FingerprintCandidateWrapperSerializer;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintCandidateWrapper;
import de.unijena.bioinf.jjobs.Partition;
import de.unijena.bioinf.spectraldb.SpectralNoSQLDatabase;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.Index;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import jakarta.persistence.Id;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

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

    @Getter
    @Setter
    private String name = null;

    @Getter
    @Setter
    private Long dbFlag = null;

    public ChemicalNoSQLDatabase(Database<Doctype> database) throws IOException {
        super(database);
    }

    protected static Metadata initMetadata(FingerprintVersion version) throws IOException {
        Metadata metadata = SpectralNoSQLDatabase.initMetadata();
        return metadata
                .addRepository(ChemicalNoSQLDatabase.Tag.class, Index.unique("key"))
                .addRepository(
                        FingerprintCandidateWrapper.class,
                        Index.nonUnique("formula"),
                        Index.nonUnique("mass"))
                .addCollection(SETTINGS_COLLECTION)
                .setOptionalFields(FingerprintCandidateWrapper.class, "fingerprint")
                .addSerialization(
                        FingerprintCandidateWrapper.class,
                        new FingerprintCandidateWrapperSerializer(),
                        new FingerprintCandidateWrapperDeserializer(version));
    }

    @Override
    public String name() { //this is ugly should we add metadata from custom dbs completely?
        if (getName() != null)
            return getName();
        return super.name();
    }

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(double ionMass, Deviation deviation, PrecursorIonType ionType) throws ChemicalDatabaseException {
        try {
            final double mass = ionType.precursorMassToNeutralMass(ionMass);
            final double from = mass - deviation.absoluteFor(mass);
            final double to = mass + deviation.absoluteFor(mass);
            return this.storage.findStr(Filter.where("mass").beetweenBothInclusive(from, to), FingerprintCandidateWrapper.class)
                    .map(c -> c.getFormulaCandidate(dbFlag, ionType)).toList();
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public boolean containsFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        try {
            return this.storage.count(Filter.where("formula").eq(formula.toString()), FingerprintCandidateWrapper.class, 0, 1) > 0;
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public List<CompoundCandidate> lookupStructuresByFormula(MolecularFormula formula) throws ChemicalDatabaseException {
        try {
            return storage.findStr(Filter.where("formula").eq(formula.toString()), FingerprintCandidateWrapper.class)
                    .map(fpw -> fpw.getCandidate(name(), dbFlag)).toList();
        } catch (RuntimeException | IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public <T extends Collection<FingerprintCandidate>> T lookupStructuresAndFingerprintsByFormula(MolecularFormula formula, T fingerprintCandidates) throws ChemicalDatabaseException {
        try {
            storage.findStr(Filter.where("formula").eq(formula.toString()), FingerprintCandidateWrapper.class, "fingerprint")
                    .map(fpw -> fpw.getFingerprintCandidate(name(), dbFlag))
                    .forEach(fingerprintCandidates::add);
            return fingerprintCandidates;
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    public Stream<FingerprintCandidate> lookupFingerprintsByInchisStr(Iterable<String> inchi_keys) throws ChemicalDatabaseException {
        try {
            String[] keys = StreamSupport.stream(inchi_keys.spliterator(), false).toArray(String[]::new);
            return storage.findStr(Filter.where("candidate.inchikey").in(keys), FingerprintCandidateWrapper.class, "fingerprint")
                    .map(fpw -> fpw.getFingerprintCandidate(name(), dbFlag));
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
            return storage.findStr(Filter.where("candidate.name").in(names.toArray(String[]::new)), FingerprintCandidateWrapper.class)
                    .map(fc -> fc.getCandidate(name(), dbFlag).getInchi()).toList();
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    public Optional<String> getTag(@NotNull String key) {
        try {
            return storage.findStr(Filter.where("key").eq(key), Tag.class, 0, 1)
                    .map(Tag::getValue).findFirst();
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Error when loading Tag: " + key + ".", e);
            return Optional.empty();
        }
    }

    @Override
    public String getChemDbDate() {
            return getTag(TAG_DATE)
                    .orElseThrow(() -> new MissingResourceException("Could not find Database Date tag", "Tag", TAG_DATE));
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
        if (this.storage.count(Filter.where("key").eq(key), ChemicalNoSQLDatabase.Tag.class) > 0) {
            ChemicalNoSQLDatabase.Tag tag = this.storage.find(Filter.where("key").eq(key), ChemicalNoSQLDatabase.Tag.class).iterator().next();
            tag.setValue(value);
            this.storage.upsert(tag);
        } else {
            this.storage.insert(ChemicalNoSQLDatabase.Tag.of(key, value));
        }
    }

    @Override
    public void updateAllFingerprints(Consumer<FingerprintCandidate> updater) throws ChemicalDatabaseException {
        try {
           Partition.ofSize(this.storage.findAll(FingerprintCandidateWrapper.class, "fingerprint"), 50)
                   .forEach(chunk -> doUpdate(chunk, updater));
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @SneakyThrows
    private void doUpdate(List<FingerprintCandidateWrapper> chunk, Consumer<FingerprintCandidate> updater) {
        List<FingerprintCandidateWrapper> updated = new ArrayList<>();
        for (FingerprintCandidateWrapper wrapper : chunk) {
            FingerprintCandidate fingerprintCandidate = wrapper.getFingerprintCandidate(name(), dbFlag);
            updater.accept(fingerprintCandidate);
            updated.add(FingerprintCandidateWrapper.of(wrapper.getFormula(), wrapper.getMass(), fingerprintCandidate));
        }
        this.storage.upsertAll(updated);
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tag {

        @Setter
        @Id
        private String key;
        private String value;

        public static Tag of(String key, String value) {
            return new Tag(key, value);
        }

        public static Tag of(Map.Entry<String, String> source) {
            return of(source.getKey(), source.getValue());
        }

        public String setValue(String value) {
            String old = this.value;
            this.value = value;
            return old;
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
