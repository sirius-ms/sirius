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

import com.google.common.collect.Streams;
import de.unijena.bioinf.ChemistryBase.ms.AdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.ChemicalNoSQLDatabase;
import de.unijena.bioinf.ms.annotations.SpectrumAnnotation;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.SimpleSerializers;
import de.unijena.bioinf.storage.db.nosql.*;
import de.unijena.bionf.spectral_alignment.AbstractSpectralAlignment;
import de.unijena.bionf.spectral_alignment.CosineQuerySpectrum;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import org.apache.commons.lang3.tuple.Triple;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SpectralNoSQLDatabase<Doctype> implements SpectralLibrary, WriteableSpectralLibrary, Closeable {

    final protected Database<Doctype> storage;

    public SpectralNoSQLDatabase(Database<Doctype> storage) throws IOException {
        this.storage = storage;
    }

    protected static Metadata initMetadata() throws IOException {
        return Metadata.build()
                .addRepository(ChemicalNoSQLDatabase.Tag.class, "id", new Index("key",IndexType.UNIQUE))
                .addRepository(
                        Ms2ReferenceSpectrum.class,
                        "id",
                        new Index("uuid", IndexType.UNIQUE),
                        new Index("splash", IndexType.NON_UNIQUE),
                        new Index("ionMass", IndexType.NON_UNIQUE),
                        new Index("formula", IndexType.NON_UNIQUE),
                        new Index("name", IndexType.NON_UNIQUE),
                        new Index("candidateInChiKey", IndexType.NON_UNIQUE)
                ).addSerializer(
                        AdditionalFields.class,
                        new SimpleSerializers.AnnotationSerializer()
                ).addDeserializer(
                        SpectrumAnnotation.class,
                        new SimpleSerializers.AnnotationDeserializer()
                ).setOptionalFields(Ms2ReferenceSpectrum.class, "spectrum");
    }

    @Override
    public <P extends Peak> SpectralSearchResult matchingSpectra(
            Iterable<Ms2Spectrum<P>> queries,
            Deviation precursorMzDeviation,
            Deviation maxPeakDeviation,
            SpectralAlignmentType alignmentType,
            BiConsumer<Integer, Integer> progressConsumer
    ) throws ChemicalDatabaseException {
        // TODO move matching away from persistence layer
        // TODO when combined databases (or not nitrite with possible equal splashes) -> query only unique splashes? -> too slow? maybe give spectra with same splash same rank when sorting the results
        // TODO modified cosine
        try {
            final List<SpectralSearchResult.SearchResult> results = new ArrayList<>();
            AbstractSpectralAlignment alignment = alignmentType.type.getConstructor(Deviation.class).newInstance(maxPeakDeviation);
            CosineQueryUtils cosineQueryUtils = new CosineQueryUtils(alignment);

            int maxProgress = 0;
            List<Triple<Ms2Spectrum<P>, Integer, Filter>> params = new ArrayList<>();
            for (Ms2Spectrum<P> query : queries) {
                double abs = precursorMzDeviation.absoluteFor(query.getPrecursorMz());
                Filter filter = new Filter().and().gte("precursorMz", query.getPrecursorMz() - abs).lte("precursorMz", query.getPrecursorMz() + abs);
                int count = this.storage.count(filter, Ms2ReferenceSpectrum.class);
                maxProgress += count;
                params.add(Triple.of(query, count, filter));
            }

            int progress = 0;
            for (int i = 0; i < params.size(); i++) {
                Triple<Ms2Spectrum<P>, Integer, Filter> param = params.get(i);
                CosineQuerySpectrum cosineQuery = cosineQueryUtils.createQuery(new SimpleSpectrum(param.getLeft()), param.getLeft().getPrecursorMz());

                int pageSize = 100;
                for (int offset = 0; offset < param.getMiddle(); offset += pageSize) {
                    Iterable<Ms2ReferenceSpectrum> references = this.storage.find(param.getRight(), Ms2ReferenceSpectrum.class, offset, pageSize,"spectrum");
                    for (Ms2ReferenceSpectrum reference : references) {
                        CosineQuerySpectrum cosineReference = cosineQueryUtils.createQuery(reference.getSpectrum(), reference.getPrecursorMz());
                        SpectralSimilarity similarity = cosineQueryUtils.cosineProduct(cosineQuery, cosineReference);

                        if (similarity.shardPeaks > 0) {
                            SpectralSearchResult.SearchResult res = SpectralSearchResult.SearchResult.builder()
                                    .dbName(this.name())
                                    .querySpectrumIndex(i)
                                    .similarity(similarity)
                                    .referenceUUID(reference.getUuid())
                                    .referenceSplash(reference.getSplash())
                                    .build();
                            results.add(res);
                        }
                        if (progressConsumer != null) {
                            progressConsumer.accept(++progress, maxProgress);
                        }
                    }
                }
            }

            results.sort((a, b) -> Double.compare(b.getSimilarity().similarity, a.getSimilarity().similarity));

            return SpectralSearchResult.builder()
                    .precursorDeviation(precursorMzDeviation)
                    .peakDeviation(maxPeakDeviation)
                    .alignmentType(alignmentType)
                    .results(Streams.mapWithIndex(results.stream(), (r, index) -> {
                        r.setRank((int) index + 1);
                        return r;
                    }).toList())
                    .build();

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 RuntimeException | IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    public Database<Doctype> getStorage() {
        return storage;
    }

    @Override
    public String name() {
        return this.storage.location().getFileName().toString();
    }

    @Override
    public String location() {
        return this.storage.location().toString();
    }

    @Override
    public int countAllSpectra() throws IOException {
        return this.storage.countAll(Ms2ReferenceSpectrum.class);
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
    public Ms2ReferenceSpectrum getReferenceSpectrum(String uuid) throws ChemicalDatabaseException {
        try {
            return this.storage.find(Filter.build().eq("uuid", uuid), Ms2ReferenceSpectrum.class).iterator().next();
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
        List<Ms2ReferenceSpectrum> spectra = this.storage.findStr(Filter.build().eq("smiles", smiles), Ms2ReferenceSpectrum.class, "spectrum").peek(updater).toList();
        this.storage.upsertAll(spectra);
    }

    @Override
    public void close() throws IOException {
        this.storage.close();
    }

}
