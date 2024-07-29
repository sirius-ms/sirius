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

package de.unijena.bioinf.spectraldb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.AdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.ms.annotations.SpectrumAnnotation;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.Index;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
//todo check when data/spectra should be included an when not

@Getter
public abstract class SpectralNoSQLDatabase<Doctype> implements SpectralLibrary, WriteableSpectralLibrary, Closeable {

    final protected Database<Doctype> storage;

    public Database<Doctype> getStorage() {
        return storage;
    }

    public SpectralNoSQLDatabase(Database<Doctype> storage) {
        this.storage = storage;
    }

    protected static Metadata initMetadata() throws IOException {
        return Metadata.build()
                .addRepository(
                        Ms2ReferenceSpectrum.class,
                        Index.nonUnique("exactMass"),
                        Index.nonUnique("precursorMz"),
                        Index.nonUnique("formula"),
                        Index.nonUnique("candidateInChiKey")
                ).addSerializer(
                        AdditionalFields.class,
                        new SimpleSerializers.AnnotationSerializer()
                ).addDeserializer(
                        SpectrumAnnotation.class,
                        new SimpleSerializers.AnnotationDeserializer()
                ).setOptionalFields(Ms2ReferenceSpectrum.class, "spectrum");
    }

    public abstract <O> Doctype asDocument(O object);

    public abstract <O> O asObject(Doctype document, Class<O> objectClass);

    public String name() {
        return this.storage.location().getFileName().toString();
    }

    public String location() {
        return this.storage.location().toString();
    }

    @Override
    public long countAllSpectra() throws IOException {
        return this.storage.countAll(Ms2ReferenceSpectrum.class);
    }

    @Override
    public Iterable<Ms2ReferenceSpectrum> lookupSpectra(double precursorMz, Deviation deviation, boolean withData) throws ChemicalDatabaseException {
        try {
            double abs = deviation.absoluteFor(precursorMz);
            Filter filter = Filter.where("precursorMz").beetweenBothInclusive(precursorMz - abs, precursorMz + abs);
            if (withData) {
                return withLibrary(this.storage.find(filter, Ms2ReferenceSpectrum.class, "spectrum"));
            } else {
                return withLibrary(this.storage.find(filter, Ms2ReferenceSpectrum.class));
            }
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    public Iterable<Ms2ReferenceSpectrum> lookupSpectraBy(@NotNull String field, @NotNull Object value) throws ChemicalDatabaseException {
        return lookupSpectraBy(field, value, false);
    }

    public Iterable<Ms2ReferenceSpectrum> lookupSpectraBy(@NotNull String field, @NotNull Object value, boolean withData) throws ChemicalDatabaseException {
        try {
            Filter filter = Filter.where(field).eq(value);
            if (withData) {
                return withLibrary(this.storage.find(filter, Ms2ReferenceSpectrum.class, "spectrum"));
            } else {
                return withLibrary(this.storage.find(filter, Ms2ReferenceSpectrum.class));
            }
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public Iterable<Ms2ReferenceSpectrum> lookupSpectra(String inchiKey2d, boolean withData) throws ChemicalDatabaseException {
        return lookupSpectraBy("candidateInChiKey", inchiKey2d, withData);
    }

    @Override
    public Iterable<Ms2ReferenceSpectrum> lookupSpectra(MolecularFormula formula, boolean withData) throws ChemicalDatabaseException {
        return lookupSpectraBy("formula", formula, withData);
    }

    @Override
    public Ms2ReferenceSpectrum getReferenceSpectrum(long uuid) throws ChemicalDatabaseException {
        try {
            Iterator<Ms2ReferenceSpectrum> specs = this.storage.find(Filter.where("uuid").eq(uuid), Ms2ReferenceSpectrum.class).iterator();
            if (specs.hasNext()) return fillLibrary(specs.next());
            else throw new ChemicalDatabaseException("No spectrum with uuid " + uuid + " found.");
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public Iterable<Ms2ReferenceSpectrum> getSpectralData(Iterable<Ms2ReferenceSpectrum> references) throws ChemicalDatabaseException {
        try {
            return withLibrary(this.storage.injectOptionalFields(Ms2ReferenceSpectrum.class, references, "spectrum"));
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public Ms2ReferenceSpectrum getSpectralData(Ms2ReferenceSpectrum reference) throws ChemicalDatabaseException {
        try {
            return fillLibrary(this.storage.injectOptionalFields(reference, "spectrum"));
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public int upsertSpectra(List<Ms2ReferenceSpectrum> data) throws IOException {
        return storage.insertAll(data);
    }

    @Override
    public void forEachSpectrum(Consumer<Ms2ReferenceSpectrum> consumer) throws IOException {
        this.storage.findAllStr(Ms2ReferenceSpectrum.class).forEach(consumer);
    }

    @Override
    public void updateSpectraMatchingSmiles(Consumer<Ms2ReferenceSpectrum> updater, String smiles) throws IOException {
        List<Ms2ReferenceSpectrum> spectra = this.storage.findStr(Filter.where("smiles").eq(smiles), Ms2ReferenceSpectrum.class, "spectrum").peek(updater).toList();
        this.storage.upsertAll(spectra);
    }

    @Override
    public void close() throws IOException {
        this.storage.close();
    }

    private Ms2ReferenceSpectrum fillLibrary(Ms2ReferenceSpectrum spectrum) {
        spectrum.setLibraryName(name());
        return spectrum;
    }

    /**
     * Wraps the passed iterable and fills the library name before returning reference spectra
     */
    private Iterable<Ms2ReferenceSpectrum> withLibrary(Iterable<Ms2ReferenceSpectrum> iterable) {
        return new Iterable<>() {
            @NotNull
            @Override
            public Iterator<Ms2ReferenceSpectrum> iterator() {
                Iterator<Ms2ReferenceSpectrum> delegate = iterable.iterator();
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return delegate.hasNext();
                    }

                    @Override
                    public Ms2ReferenceSpectrum next() {
                        return fillLibrary(delegate.next());
                    }
                };
            }
        };
    }
}
