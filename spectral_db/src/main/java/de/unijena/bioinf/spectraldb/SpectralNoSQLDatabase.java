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

import com.google.common.collect.Iterables;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.ChemicalNoSQLDatabase;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralData;
import de.unijena.bioinf.spectraldb.entities.Ms2SpectralMetadata;
import de.unijena.bioinf.spectraldb.ser.Ms2SpectralDataDeserializer;
import de.unijena.bioinf.spectraldb.ser.Ms2SpectralDataSerializer;
import de.unijena.bioinf.spectraldb.ser.Ms2SpectralMetadataDeserializer;
import de.unijena.bioinf.spectraldb.ser.Ms2SpectralMetadataSerializer;
import de.unijena.bioinf.storage.db.nosql.*;
import de.unijena.bionf.spectral_alignment.AbstractSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


public abstract class SpectralNoSQLDatabase<Doctype> extends ChemicalNoSQLDatabase<Doctype> implements SpectralLibrary {

    public SpectralNoSQLDatabase(Database<Doctype> database) throws IOException {
        super(database);
    }

    protected static Metadata initMetadata(FingerprintVersion version) throws IOException {
        Metadata metadata = ChemicalNoSQLDatabase.initMetadata(version);
        return metadata.addRepository(
                Ms2SpectralMetadata.class,
                "id",
                new Ms2SpectralMetadataSerializer(),
                new Ms2SpectralMetadataDeserializer(),
                new Index("ionMass", IndexType.NON_UNIQUE),
                new Index("formula", IndexType.NON_UNIQUE),
                new Index("name", IndexType.NON_UNIQUE),
                new Index("smiles", IndexType.NON_UNIQUE)
        ).addRepository(
                Ms2SpectralData.class,
                "id",
                new Ms2SpectralDataSerializer(),
                new Ms2SpectralDataDeserializer(),
                new Index("ionMass", IndexType.NON_UNIQUE),
                new Index("precursorMz", IndexType.NON_UNIQUE)
        );
    }

    public abstract void importSpectra(Iterable<Ms2Experiment> experiments) throws ChemicalDatabaseException;

    @Override
    public <P extends Peak, A extends AbstractSpectralAlignment> Iterable<Pair<SpectralSimilarity, Ms2SpectralMetadata>> matchingSpectra(
            Ms2Spectrum<P> spectrum,
            Deviation precursorMzDeviation,
            Deviation maxPeakDeviation,
            Class<A> alignmentType
    )  throws ChemicalDatabaseException {
        return matchingSpectra(spectrum, precursorMzDeviation, maxPeakDeviation, alignmentType, false);
    }

    @Override
    public <P extends Peak, A extends AbstractSpectralAlignment> Iterable<Pair<SpectralSimilarity, Ms2SpectralMetadata>> matchingSpectra(
            Ms2Spectrum<P> spectrum,
            Deviation precursorMzDeviation,
            Deviation maxPeakDeviation,
            Class<A> alignmentType,
            boolean parallel
    ) throws ChemicalDatabaseException {
        try {
            Collection<Pair<SpectralSimilarity, Ms2SpectralMetadata>> heap = Collections.synchronizedCollection(new PriorityQueue<>((o1, o2) -> - Double.compare(o1.getLeft().similarity, o2.getLeft().similarity)));
            A alignment  = alignmentType.getConstructor(Deviation.class).newInstance(maxPeakDeviation);
            OrderedSpectrum<Peak> query = new SimpleSpectrum(spectrum);

            Iterable<Ms2SpectralData> spectra = lookupSpectra(spectrum.getPrecursorMz(), precursorMzDeviation);
            StreamSupport.stream(Iterables.partition(spectra, 100).spliterator(), parallel).forEach(chunk -> {
                for (Ms2SpectralData data : chunk) {
                    SpectralSimilarity similarity = alignment.score(query, data);
                    if (similarity.shardPeaks > 0) {
                        try {
                            heap.add(Pair.of(similarity, database.getById(data.getMetaId(), Ms2SpectralMetadata.class)));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
            return heap;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | RuntimeException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public Iterable<Ms2SpectralData> lookupSpectra(double precursorMz, Deviation deviation) throws ChemicalDatabaseException {
        try {
            double abs = deviation.absoluteFor(precursorMz);
            return this.database.find(new Filter().and().gte("precursorMz", precursorMz - abs).lte("precursorMz", precursorMz + abs), Ms2SpectralData.class);
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public Iterable<Ms2SpectralMetadata> getMetaData(Iterable<Ms2SpectralData> data) throws ChemicalDatabaseException {
        try {
            return StreamSupport.stream(data.spliterator(), false).map(d -> {
                try {
                    return database.getById(d.getMetaId(), Ms2SpectralMetadata.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
        } catch (RuntimeException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public Iterable<Ms2SpectralData> getSpectralData(Iterable<Ms2SpectralMetadata> metadata) throws ChemicalDatabaseException {
        try {
            return StreamSupport.stream(metadata.spliterator(), false).map(d -> {
                try {
                    return database.getById(d.getPeaksId(), Ms2SpectralData.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
        } catch (RuntimeException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

}
