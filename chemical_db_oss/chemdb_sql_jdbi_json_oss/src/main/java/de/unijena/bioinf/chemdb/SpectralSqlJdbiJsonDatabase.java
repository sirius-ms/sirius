/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb;


import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.spectraldb.LibraryHit;
import de.unijena.bioinf.spectraldb.SpectralLibrary;
import de.unijena.bioinf.spectraldb.SpectralLibrarySearchSettings;
import de.unijena.bioinf.spectraldb.SpectrumType;
import de.unijena.bioinf.spectraldb.entities.MergedReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.ReferenceFragmentationTree;
import de.unijena.bioinf.spectraldb.entities.ReferenceSpectrum;
import de.unijena.bionf.fastcosine.ReferenceLibrarySpectrum;
import org.jdbi.v3.core.ConnectionFactory;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.json.Json;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
//todo check when data/spectra should be included an when not
//TODO THIS IS WIP

public class SpectralSqlJdbiJsonDatabase implements SpectralLibrary {

    protected Jdbi jdbi;

    public SpectralSqlJdbiJsonDatabase(ConnectionFactory sqlDb) {
        this.jdbi = Jdbi.create(sqlDb).installPlugin(new SqlObjectPlugin());
        this.jdbi.registerArrayType(Long.class, "BIGINT");
    }

    public SpectralSqlJdbiJsonDatabase(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public long countAllSpectra() {
        return jdbi.withExtension(Dao.class, Dao::countAll);
    }

    @Override
    public Iterable<Ms2ReferenceSpectrum> lookupSpectra(final double precursorMz, @NotNull final Deviation deviation, final boolean withData) {
        double abs = deviation.absoluteFor(precursorMz);
        return withData
                ? jdbi.withExtension(Dao.class, db -> db.findByPrecursorMz(precursorMz - abs, precursorMz + abs))
                : jdbi.withExtension(Dao.class, db -> db.findByPrecursorMzNoData(precursorMz - abs, precursorMz + abs));

    }

    @Override
    public Iterable<Ms2ReferenceSpectrum> lookupSpectra(String inchiKey2d, boolean withData) {
        return withData
                ? jdbi.withExtension(Dao.class, db -> db.findBy("candidateInChiKey", inchiKey2d))
                : jdbi.withExtension(Dao.class, db -> db.findByNoData("candidateInChiKey", inchiKey2d));
    }

    @Override
    public Iterable<Ms2ReferenceSpectrum> lookupSpectra(MolecularFormula formula, boolean withData) {
        return withData
                ? jdbi.withExtension(Dao.class, db -> db.findBy("formula", formula.toString()))
                : jdbi.withExtension(Dao.class, db -> db.findByNoData("formula", formula.toString()));
    }

    @Override
    public Stream<LibraryHit> queryAgainstLibraryWithPrecursorMass(double precursorMz, int chargeAndPolarity, SpectralLibrarySearchSettings settings, List<ReferenceLibrarySpectrum> query) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<LibraryHit> queryAgainstLibrary(int chargeAndPolarity, SpectralLibrarySearchSettings settings, List<ReferenceLibrarySpectrum> query) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<LibraryHit> queryAgainstLibraryByMergedReference(List<MergedReferenceSpectrum> mergedRefQueries, SpectralLibrarySearchSettings settings, @NotNull List<ReferenceLibrarySpectrum> query, @Nullable ReferenceLibrarySpectrum mergedQuery) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<LibraryHit> queryAgainstLibraryByMergedReference(MergedReferenceSpectrum mergedRefQuery, SpectralLibrarySearchSettings settings, @NotNull List<ReferenceLibrarySpectrum> query, @Nullable ReferenceLibrarySpectrum mergedQuery) throws IOException {
        throw new UnsupportedOperationException();
    }


    @Override
    public Stream<MergedReferenceSpectrum> getMergedReferenceSpectra(double precursorMz, int chargeAndPolarity, Deviation precursorDeviation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<MergedReferenceSpectrum> getMergedReferenceSpectra(double precursorMz, int chargeAndPolarity, Deviation precursorDeviation, boolean withSpectrum) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public MergedReferenceSpectrum getMergedReferenceSpectrum(String candidateInChiKey, PrecursorIonType precursorIonType, boolean withSpectrum) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ReferenceFragmentationTree getReferenceTree(long uuid) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Ms2ReferenceSpectrum getReferenceSpectrum(final long uuid) {
        return jdbi.withExtension(Dao.class, db -> db.findByUUID(uuid));
    }

    @Override
    public ReferenceSpectrum getReferenceSpectrum(long uuid, SpectrumType spectrumType) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Ms2ReferenceSpectrum queryAgainstIndividualSpectrum(long uuid) throws ChemicalDatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Ms2ReferenceSpectrum> fetchSpectralData(Iterable<Ms2ReferenceSpectrum> references) {
        return StreamSupport.stream(references.spliterator(), false)
                .peek(this::fetchSpectralData)
                .toList(); //todo use single db request instead
    }

    @Override
    public Ms2ReferenceSpectrum fetchSpectralData(Ms2ReferenceSpectrum reference) {
        reference.setSpectrum(jdbi.withExtension(Dao.class, db -> db.findSpectrumByUUID(reference.getUuid())));
        return reference;
    }

    @Override
    public void forEachSpectrum(@NotNull final Consumer<Ms2ReferenceSpectrum> consumer) {
        jdbi.useExtension(Dao.class, db -> db.findAllLazy().forEach(consumer));
    }

    @Override
    public void forEachSpectrum(Consumer<Ms2ReferenceSpectrum> consumer, boolean withData) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forEachMergedSpectrum(Consumer<MergedReferenceSpectrum> consumer) throws IOException {
        throw new UnsupportedOperationException();
    }


    interface Dao extends SqlObject {

        @SqlQuery("SELECT count(uuid) FROM `SPECTRA`")
        int countAll();

        default List<Ms2ReferenceSpectrum> findAll() {
            return findAllLazy().list();
        }

        default ResultIterable<Ms2ReferenceSpectrum> findAllLazy() {
            String q = "SELECT json FROM `SPECTRA`";
            return withHandle(h -> h.createQuery(q)
                    .mapTo(QualifiedType.of(Ms2ReferenceSpectrum.class).with(Json.class)));
        }

        @SqlQuery("SELECT json FROM `SPECTRA` WHERE uuid = ?")
        @Json
        Ms2ReferenceSpectrum findByUUID(long uuid);

        @SqlQuery("SELECT JSON_EXTRACT(json, '$.\"spectrum\"') as Result FROM `SPECTRA` WHERE uuid = ?")
        @Json
        SimpleSpectrum findSpectrumByUUID(long uuid);

        @SqlQuery("SELECT json FROM `SPECTRA` WHERE exactMass >= ? AND exactMass <= ?")
        @Json
        Iterable<Ms2ReferenceSpectrum> findByExactMass(double fromMass, double toMass);

        @SqlQuery("SELECT JSON_REMOVE(json, '$.\"spectrum\"') as Result FROM `SPECTRA` WHERE exactMass >= ? AND exactMass <= ?")
        @Json
        Iterable<Ms2ReferenceSpectrum> findByExactMassNoData(double fromMass, double toMass);

        @SqlQuery("SELECT json FROM `SPECTRA` WHERE precursorMz >= ? AND precursorMz <= ?")
        @Json
        Iterable<Ms2ReferenceSpectrum> findByPrecursorMz(double fromMass, double toMass);

        @SqlQuery("SELECT JSON_REMOVE(json, '$.\"spectrum\"') as Result FROM `SPECTRA` WHERE precursorMz >= ? AND precursorMz <= ?")
        @Json
        Iterable<Ms2ReferenceSpectrum> findByPrecursorMzNoData(double fromMass, double toMass);

        default <T> Iterable<Ms2ReferenceSpectrum> findBy(@NotNull String fieldName, T value) {
            final String q = "SELECT json from `SPECTRA` WHERE JSON_EXTRACT(json, \"$." + fieldName + "\") = ?";
            return withHandle(h -> h.createQuery(q).bind(0, value)
                    .mapTo(QualifiedType.of(Ms2ReferenceSpectrum.class).with(Json.class)));
        }

        default <T> Iterable<Ms2ReferenceSpectrum> findByNoData(String fieldName, T value) {
            final String q = "SELECT JSON_REMOVE(json, '$.\"spectrum\"') as Result from `SPECTRA` WHERE JSON_EXTRACT(json, \"$." + fieldName + "\") = ?";
            return withHandle(h -> h.createQuery(q).bind(0, value)
                    .mapTo(QualifiedType.of(Ms2ReferenceSpectrum.class).with(Json.class)));
        }
    }
}
