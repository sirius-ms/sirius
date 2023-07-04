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
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.ms.annotations.SpectrumAnnotation;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.storage.db.nosql.*;
import de.unijena.bionf.spectral_alignment.AbstractSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import it.unimi.dsi.fastutil.doubles.Double2ObjectMaps;
import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectSortedMap;
import it.unimi.dsi.fastutil.doubles.DoubleComparators;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
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
                gen.writeString(value.toString());
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

    protected static JsonSerializer<AdditionalFields> annotationSerializer() {
        return new JsonSerializer<>() {
            @Override
            public void serialize(AdditionalFields value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                try {
                    gen.writeStartObject();
                    value.forEach((key, val) -> {
                        try {
                            gen.writeStringField(key, val);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    gen.writeEndObject();
                } catch (RuntimeException e) {
                    throw new IOException(e);
                }
            }
        };
    }

    protected static JsonDeserializer<SpectrumAnnotation> annotationDeserializer() {
        return new JsonDeserializer<>() {
            @Override
            public SpectrumAnnotation deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                JsonToken token = p.currentToken();
                if (token != JsonToken.START_OBJECT)
                    return null;

                Map<String, String> map = new HashMap<>();
                for (token = p.nextToken(); token == JsonToken.FIELD_NAME; token = p.nextToken()) {
                    String key = p.getCurrentName();
                    String value = p.nextTextValue();
                    map.put(key, value);
                }
                AdditionalFields af = new AdditionalFields();
                af.putAll(map);
                return af;
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
                ).addSerializer(
                        AdditionalFields.class,
                        annotationSerializer()
                ).addDeserializer(
                        SpectrumAnnotation.class,
                        annotationDeserializer()
                ).setOptionalFields(Ms2ReferenceSpectrum.class, "spectrum");
    }

    @Override
    public <P extends Peak, A extends AbstractSpectralAlignment> Iterable<SearchResult> matchingSpectra(
            Iterable<Ms2Spectrum<P>> queries,
            Deviation precursorMzDeviation,
            Deviation maxPeakDeviation,
            Class<A> alignmentType
    ) throws ChemicalDatabaseException {
        try {
            Double2ObjectSortedMap<List<SearchResult>> map = new Double2ObjectRBTreeMap<>(DoubleComparators.OPPOSITE_COMPARATOR);

            A alignment = alignmentType.getConstructor(Deviation.class).newInstance(maxPeakDeviation);
            for (Ms2Spectrum<P> query : queries) {
                OrderedSpectrum<Peak> orderedQuery = new SimpleSpectrum(query);

                double abs = precursorMzDeviation.absoluteFor(query.getPrecursorMz());
                Filter filter = new Filter().and().gte("precursorMz", query.getPrecursorMz() - abs).lte("precursorMz", query.getPrecursorMz() + abs);

                int total = this.storage.count(filter, Ms2ReferenceSpectrum.class);
                int pageSize = 100;
                for (int offset = 0; offset < total; offset += pageSize) {
                    Iterable<Ms2ReferenceSpectrum> references = this.storage.find(filter, Ms2ReferenceSpectrum.class, offset, pageSize,"spectrum");

                    StreamSupport.stream(references.spliterator(), false).forEach(reference -> {
                        SpectralSimilarity similarity = alignment.score(orderedQuery, reference.getSpectrum());
                        if (similarity.shardPeaks > 0) {
                            Ms2ReferenceSpectrum withoutData = new Ms2ReferenceSpectrum(
                                    reference.getId(), reference.getCandidateInChiKey(), reference.getPrecursorIonType(),
                                    reference.getPrecursorMz(), reference.getIonMass(), reference.getMsLevel(), reference.getCollisionEnergy(),
                                    reference.getInstrumentation(), reference.getFormula(), reference.getName(), reference.getSmiles(),
                                    reference.getLibraryName(), reference.getLibraryId(), reference.getSplash(), null);
                            SearchResult res = SearchResult.builder().query(query).similarity(similarity).reference(withoutData).build();
                            if (map.containsKey(similarity.similarity)) {
                                map.get(similarity.similarity).add(res);
                            } else {
                                map.put(similarity.similarity, List.of(res));
                            }
                        }
                    });
                }
            }

            List<SearchResult> result = new ArrayList<>();
            final int[] rank = new int[]{0};
            Double2ObjectMaps.fastForEach(map, entry -> {
                for (SearchResult res : entry.getValue()) {
                    res.setRank(++rank[0]);
                    result.add(res);
                }
            });

            return result;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 RuntimeException | IOException e) {
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

    @Override
    public Iterable<Ms2ReferenceSpectrum> getSpectralData(Iterable<Ms2ReferenceSpectrum> references) throws ChemicalDatabaseException {
        try {
            return this.storage.injectOptionalFields(Ms2ReferenceSpectrum.class, references, "spectrum");
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public Ms2ReferenceSpectrum getSpectralData(Ms2ReferenceSpectrum reference) throws ChemicalDatabaseException {
        try {
            return this.storage.injectOptionalFields(reference, "spectrum");
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
