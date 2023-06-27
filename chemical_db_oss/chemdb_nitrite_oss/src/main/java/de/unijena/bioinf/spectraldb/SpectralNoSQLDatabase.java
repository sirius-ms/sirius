/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

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

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.Iterables;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.storage.db.nosql.*;
import de.unijena.bionf.spectral_alignment.AbstractSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public class SpectralNoSQLDatabase<Doctype> implements SpectralLibrary {

    final protected Database<Doctype> storage;

    public SpectralNoSQLDatabase(Database<Doctype> storage) throws IOException {
        this.storage = storage;
    }

    protected static <T> JsonSerializer<T> serializer() {
        return new JsonSerializer<T>() {
            @Override
            public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(toString());
            }
        };
    }

    protected static <T> JsonDeserializer<T> deserializer(Function<String, T> callable) {
        return new JsonDeserializer<T>() {
            @Override
            public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
                while (p.currentToken() != JsonToken.VALUE_STRING)
                    p.nextToken();
                return callable.apply(p.getText());
            }
        };
    }

    protected static Metadata initMetadata() throws IOException {
        return Metadata.build()
                .addRepository(Tag.class, new Index("key",IndexType.UNIQUE))
                .addRepository(
                        Ms2ReferenceSpectrum.class,
                        "id",
                        new Index("ionMass", IndexType.NON_UNIQUE),
                        new Index("formula", IndexType.NON_UNIQUE),
                        new Index("name", IndexType.NON_UNIQUE),
                        new Index("candidateInChiKey", IndexType.NON_UNIQUE)
                ).addSerialization(
                        PrecursorIonType.class,
                        serializer(),
                        deserializer(PrecursorIonType::fromString)
                ).addSerialization(
                        MolecularFormula.class,
                        serializer(),
                        deserializer(MolecularFormula::parseOrThrow)
                ).addSerialization(
                        CollisionEnergy.class,
                        serializer(),
                        deserializer(CollisionEnergy::fromString)
                ).addDeserializer(
                        MsInstrumentation.class,
                        deserializer(MsInstrumentation.Instrument::valueOf)
                ).setOptionalFields(Ms2ReferenceSpectrum.class, "spectrum");
    }

    @Override
    public <P extends Peak, A extends AbstractSpectralAlignment> Iterable<Pair<SpectralSimilarity, Ms2ReferenceSpectrum>> matchingSpectra(
            Ms2Spectrum<P> spectrum,
            Deviation precursorMzDeviation,
            Deviation maxPeakDeviation,
            Class<A> alignmentType
    ) throws ChemicalDatabaseException {
        return matchingSpectra(spectrum, precursorMzDeviation, maxPeakDeviation, alignmentType, false);
    }

    @Override
    public <P extends Peak, A extends AbstractSpectralAlignment> Iterable<Pair<SpectralSimilarity, Ms2ReferenceSpectrum>> matchingSpectra(
            Ms2Spectrum<P> spectrum,
            Deviation precursorMzDeviation,
            Deviation maxPeakDeviation,
            Class<A> alignmentType,
            boolean parallel
    ) throws ChemicalDatabaseException {
        try {
            PriorityBlockingQueue<Pair<SpectralSimilarity, Ms2ReferenceSpectrum>> heap = new PriorityBlockingQueue<>(100, (o1, o2) -> - Double.compare(o1.getLeft().similarity, o2.getLeft().similarity));
            A alignment = alignmentType.getConstructor(Deviation.class).newInstance(maxPeakDeviation);
            OrderedSpectrum<Peak> query = new SimpleSpectrum(spectrum);

            // TODO it might be a better idea replace iterables.partition with pagination
            Iterable<Ms2ReferenceSpectrum> spectra = lookupSpectra(spectrum.getPrecursorMz(), precursorMzDeviation, true);
            StreamSupport.stream(Iterables.partition(spectra, 100).spliterator(), parallel).forEach(chunk -> {
                for (Ms2ReferenceSpectrum reference : chunk) {
                    SpectralSimilarity similarity = alignment.score(query, reference.getSpectrum());
                    Ms2ReferenceSpectrum withoutData = new Ms2ReferenceSpectrum(
                            reference.getId(), reference.getCandidateInChiKey(), reference.getPrecursorIonType(),
                            reference.getPrecursorMz(), reference.getIonMass(), reference.getMsLevel(), reference.getCollisionEnergy(),
                            reference.getInstrumentation(), reference.getFormula(), reference.getName(), reference.getSmiles(),
                            reference.getLibraryName(), reference.getLibraryId(), reference.getSplash(), null);
                    if (similarity.shardPeaks > 0) {
                        heap.add(Pair.of(similarity, withoutData));
                    }
                }
            });

            List<Pair<SpectralSimilarity, Ms2ReferenceSpectrum>> result = new ArrayList<>(heap.size());
            heap.drainTo(result);
            return result;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 RuntimeException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public Iterable<Ms2ReferenceSpectrum> lookupSpectra(double precursorMz, Deviation deviation, boolean withData) throws ChemicalDatabaseException {
        try {
            double abs = deviation.absoluteFor(precursorMz);
            Filter filter = new Filter().and().gte("precursorMz", precursorMz - abs).lte("precursorMz", precursorMz + abs);
            if (withData) {
                return this.storage.find(filter, Ms2ReferenceSpectrum.class, "spectrum");
            } else {
                return this.storage.find(filter, Ms2ReferenceSpectrum.class);
            }
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public Iterable<Ms2ReferenceSpectrum> lookupSpectra(String inchiKey2d, boolean withData) throws ChemicalDatabaseException {
        try {
            Filter filter = new Filter().eq("candidateInChiKey", inchiKey2d);
            if (withData) {
                return this.storage.find(filter, Ms2ReferenceSpectrum.class, "spectrum");
            } else {
                return this.storage.find(filter, Ms2ReferenceSpectrum.class);
            }
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tag {
        private String key;
        private String value;

        public static Tag of(String key, String value) {
            return new Tag(key, value);
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
            if (!(o instanceof SpectralNoSQLDatabase.Tag tag)) return false;
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
