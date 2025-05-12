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
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.AdditionalFields;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.utils.SimpleSerializers;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.ms.annotations.SpectrumAnnotation;
import de.unijena.bioinf.spectraldb.entities.MergedReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.spectraldb.entities.ReferenceFragmentationTree;
import de.unijena.bioinf.spectraldb.entities.ReferenceSpectrum;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.Index;
import de.unijena.bioinf.storage.db.nosql.Metadata;
import de.unijena.bionf.fastcosine.SearchPreparedSpectrum;
import de.unijena.bionf.spectral_alignment.SpectralMatchingType;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
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
                ).setOptionalFields(Ms2ReferenceSpectrum.class, "spectrum", "querySpectrum")

                .addRepository(ReferenceFragmentationTree.class)
                .addRepository(MergedReferenceSpectrum.class, Index.unique("candidateInChiKey", "precursorIonType"), Index.nonUnique("precursorMz"));
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
            Filter filter = Filter.where("exactMass").betweenBothInclusive(precursorMz - abs, precursorMz + abs);
            if (withData) {
                return withLibrary(this.storage.find(filter, Ms2ReferenceSpectrum.class, "spectrum", "querySpectrum"));
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
                return withLibrary(this.storage.find(filter, Ms2ReferenceSpectrum.class, "spectrum", "querySpectrum"));
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
    public ReferenceSpectrum getReferenceSpectrum(long uuid, SpectrumType type) throws ChemicalDatabaseException {
        try {
            if (type == SpectrumType.SPECTRUM) {
                Iterator<Ms2ReferenceSpectrum> specs = this.storage.find(Filter.where("uuid").eq(uuid), Ms2ReferenceSpectrum.class, "querySpectrum").iterator();
                if (specs.hasNext()) return fillLibrary(specs.next());
                else throw new ChemicalDatabaseException("No spectrum with uuid " + uuid + " found.");
            } else if (type == SpectrumType.MERGED_SPECTRUM) {
                Iterator<MergedReferenceSpectrum> specs = this.storage.find(Filter.where("uuid").eq(uuid), MergedReferenceSpectrum.class).iterator();
                if (specs.hasNext()) return fillLibrary(specs.next());
                else throw new ChemicalDatabaseException("No spectrum with uuid " + uuid + " found.");
            } else throw new ChemicalDatabaseException("Unknown spectrum type: " + type);
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }


    @Override
    public Stream<MergedReferenceSpectrum> getMergedReferenceSpectra(double precursorMz, int chargeAndPolarity, Deviation precursorDeviation, boolean withSpectrum) throws ChemicalDatabaseException {
        double abs = precursorDeviation.absoluteFor(precursorMz);
        try {
            Filter filter = Filter.where("exactMass").betweenBothInclusive(precursorMz - abs, precursorMz + abs);
            return storage.findStr(filter, MergedReferenceSpectrum.class, withSpectrum ? "querySpectrum" : null)
                    .filter(x -> x.getPrecursorIonType().getCharge() == chargeAndPolarity)
                    .peek(this::fillLibrary);
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }


    @Override
    public MergedReferenceSpectrum getMergedReferenceSpectrum(String candidateInChiKey, PrecursorIonType precursorIonType, boolean withSpectrum) throws ChemicalDatabaseException {
        try {
            Filter filter = Filter.and(
                    Filter.where("candidateInChiKey").eq(candidateInChiKey),
                    Filter.where("precursorIonType").eq(precursorIonType.toString())
            );

            return storage.findStr(filter, MergedReferenceSpectrum.class, withSpectrum ? "querySpectrum" : null)
                    .peek(this::fillLibrary).findFirst().orElseThrow(() ->
                            new ChemicalDatabaseException("No spectrum with candidateInChiKey=" + candidateInChiKey + " and precursorIonType=" + precursorIonType + " found."));
        } catch (IOException e) {
            if (e instanceof ChemicalDatabaseException ce)
                throw ce;
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public ReferenceFragmentationTree getReferenceTree(long uuid) throws ChemicalDatabaseException {
        try {
            Iterator<ReferenceFragmentationTree> specs = this.storage.find(Filter.where("uuid").eq(uuid), ReferenceFragmentationTree.class).iterator();
            if (specs.hasNext()) return specs.next();
            else throw new ChemicalDatabaseException("No tree with uuid " + uuid + " found.");
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public Ms2ReferenceSpectrum queryAgainstIndividualSpectrum(long uuid) throws ChemicalDatabaseException {
        try {
            Iterator<Ms2ReferenceSpectrum> specs = this.storage.find(Filter.where("uuid").eq(uuid), Ms2ReferenceSpectrum.class, "querySpectrum").iterator();
            if (specs.hasNext()) return fillLibrary(specs.next());
            else throw new ChemicalDatabaseException("No spectrum with uuid " + uuid + " found.");
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public Iterable<Ms2ReferenceSpectrum> fetchSpectralData(Iterable<Ms2ReferenceSpectrum> references) throws ChemicalDatabaseException {
        try {
            return withLibrary(this.storage.injectOptionalFields(Ms2ReferenceSpectrum.class, references, "spectrum", "querySpectrum"));
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public Ms2ReferenceSpectrum fetchSpectralData(Ms2ReferenceSpectrum reference) throws ChemicalDatabaseException {
        try {
            return fillLibrary(this.storage.injectOptionalFields(reference, "spectrum", "querySpectrum"));
        } catch (IOException e) {
            throw new ChemicalDatabaseException(e);
        }
    }

    @Override
    public int upsertSpectra(List<Ms2ReferenceSpectrum> data) throws IOException {
        return storage.insertAll(data);
    }

    @Override
    public synchronized void insertMergedSpecAndTree(MergedReferenceSpectrum merged, ReferenceFragmentationTree refTree) throws IOException {
        Optional<MergedReferenceSpectrum> first = storage.findStr(
                Filter.and(Filter.where("candidateInChiKey").eq(merged.getCandidateInChiKey()), Filter.where("precursorIonType").eq(merged.getPrecursorIonType().toString())),
                MergedReferenceSpectrum.class).findFirst();

        if (first.isPresent()) {
            // replace tree and merged spectrum!
            merged.setUuid(first.get().getUuid());
            refTree.setUuid(merged.getUuid());
            storage.upsert(merged);
            storage.upsert(refTree);
        } else {
            //  insert new merged spectrum and tree
            storage.insert(merged);
            refTree.setUuid(merged.getUuid());
            storage.insert(refTree);
        }
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

    private MergedReferenceSpectrum fillLibrary(MergedReferenceSpectrum spectrum) {
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

    @Override
    public Stream<LibraryHit> queryAgainstLibraryWithPrecursorMass(double precursorMz, int chargeAndPolarity, SpectralLibrarySearchSettings settings, List<SearchPreparedSpectrum> query) throws IOException {
        return queryAgainstLibraryByMergedReference(getMergedReferenceSpectra(precursorMz, chargeAndPolarity, settings.getPrecursorDeviation()).toList(), settings, query, null);
    }

    @Override
    public Stream<LibraryHit> queryAgainstLibrary(int chargeAndPolarity, SpectralLibrarySearchSettings settings, List<SearchPreparedSpectrum> query) throws IOException {
        return queryAgainstLibraryByMergedReference(storage.findAllStr(MergedReferenceSpectrum.class, "querySpectrum").filter(x -> x.getPrecursorIonType().getCharge() == chargeAndPolarity).toList(), settings, query, null);
    }

    @Override
    public Stream<LibraryHit> queryAgainstLibraryByMergedReference(List<MergedReferenceSpectrum> mergedReferences, SpectralLibrarySearchSettings settings, @NotNull List<SearchPreparedSpectrum> query, @Nullable SearchPreparedSpectrum mergedQuery) throws IOException {
        List<LibraryHit> hits = new ArrayList<>();
        for (MergedReferenceSpectrum mergedRef : mergedReferences)
           queryAgainstLibraryByMergedReference(mergedRef, settings, query,mergedQuery).forEach(hits::add);
        return hits.stream();
    }

    @Override
    public Stream<LibraryHit> queryAgainstLibraryByMergedReference(MergedReferenceSpectrum mergedRef, SpectralLibrarySearchSettings settings, @NotNull List<SearchPreparedSpectrum> query, @Nullable SearchPreparedSpectrum mergedQuery) throws IOException {
        List<LibraryHit> hits = new ArrayList<>();

        boolean doMergedQuery = mergedQuery != null && settings.containsQueryType(SpectrumType.MERGED_SPECTRUM);
        List<SearchPreparedSpectrum> queriesToProcess = !settings.containsQueryType(SpectrumType.SPECTRUM) ? List.of() : query;


        int singleQueries = settings.containsQueryType(SpectrumType.SPECTRUM) ? query.size() : 0;
        int mergedQueries = doMergedQuery ? 1 : 0;

        int numRefs = (settings.containsTargetType(SpectrumType.SPECTRUM) ? mergedRef.getIndividualSpectraUIDs().length : 0 ) + (settings.containsTargetType(SpectrumType.MERGED_SPECTRUM) ? 1 : 0);

        // only perform upper bound computation if there is a change to save time with it.
        if ((singleQueries + mergedQueries) * numRefs > 2 * singleQueries + mergedQueries) {
            SearchPreparedSpectrum mergedRefUpperBoundQuery = mergedRef.getSearchPreparedSpectrum().asUpperboundSearchPreparedSpectrum();
            doMergedQuery = doMergedQuery && settings.exceeded(spectralSimilarity(mergedQuery, mergedRefUpperBoundQuery, settings));
            queriesToProcess = queriesToProcess.stream().filter(q -> settings.exceeded(spectralSimilarity(q, mergedRefUpperBoundQuery, settings))).toList();
        }

        //check if there is any query left to compute before retrieving ref spectra.
        if (doMergedQuery || !queriesToProcess.isEmpty()) {
            // only retrieve single ref spectra and compute cosine if they are a target
            if (settings.containsTargetType(SpectrumType.SPECTRUM)) {
                for (long uid : mergedRef.getIndividualSpectraUIDs()) {
                    for (Ms2ReferenceSpectrum spec : withLibrary(storage.find(Filter.where("uuid").eq(uid), Ms2ReferenceSpectrum.class, "querySpectrum"))) {
                        if (!queriesToProcess.isEmpty()) // compute single query spectra if requested and above bound
                            hits.addAll(getHits(query, spec, settings));
                        if (doMergedQuery) // compute merged query if provided, requested and above bound
                            getHits(List.of(mergedQuery), spec, settings).stream()
                                    .peek(h -> h.setQueryIndex(-1)).forEach(hits::add);
                    }
                }
            }

            // only retrieve compute merged ref spectra if they are a target
            if (settings.containsTargetType(SpectrumType.MERGED_SPECTRUM)) {
                // search in merged library spectra
                fillLibrary(mergedRef);
                if (!queriesToProcess.isEmpty()) // compute single query spectra if requested and above bound
                    hits.addAll(getHits(query, mergedRef, settings));
                if (doMergedQuery) // compute merged query if provided, requested and above bound
                    getHits(List.of(mergedQuery), mergedRef, settings).stream()
                            .peek(h -> h.setQueryIndex(-1)).forEach(hits::add);
            }
        }

        return hits.stream();
    }

    private SpectralSimilarity spectralSimilarity(SearchPreparedSpectrum left, SearchPreparedSpectrum right, SpectralLibrarySearchSettings settings) {
        if (settings.getMatchingType() == SpectralMatchingType.FAST_COSINE)
            return getFastCosine().fastCosine(left, right);
        else if (settings.getMatchingType() == SpectralMatchingType.MODIFIED_COSINE)
            return getFastCosine().fastModifiedCosine(left, right);
        else throw new UnsupportedOperationException();
    }

    private List<LibraryHit> getHits(List<SearchPreparedSpectrum> left, Ms2ReferenceSpectrum right, SpectralLibrarySearchSettings settings) {
        final ArrayList<LibraryHit> hits = new ArrayList<>();
        for (int i=0; i < left.size(); ++i) {
            SpectralSimilarity sim = spectralSimilarity(left.get(i), right.getSearchPreparedSpectrum(), settings);
            if (settings.exceeded(sim)) {
                hits.add(new LibraryHit(i, sim, right, settings.getMatchingType()==SpectralMatchingType.MODIFIED_COSINE));
            }
        }
        return hits;
    }

    private List<LibraryHit> getHits(List<SearchPreparedSpectrum> left, MergedReferenceSpectrum right, SpectralLibrarySearchSettings settings) {
        final ArrayList<LibraryHit> hits = new ArrayList<>();
        for (int i=0; i < left.size(); ++i) {
            SpectralSimilarity sim = spectralSimilarity(left.get(i), right.getSearchPreparedSpectrum(), settings);
            if (settings.exceeded(sim)) {
                hits.add(new LibraryHit(i, sim, right, settings.getMatchingType()==SpectralMatchingType.MODIFIED_COSINE));
            }
        }
        return hits;
    }


    @Override
    public void forEachSpectrum(Consumer<Ms2ReferenceSpectrum> consumer) throws IOException {
        this.storage.findAllStr(Ms2ReferenceSpectrum.class).map(this::fillLibrary).forEach(consumer);
    }

    @Override
    public void forEachSpectrum(Consumer<Ms2ReferenceSpectrum> consumer, boolean withData) throws IOException {
        this.storage.findAllStr(Ms2ReferenceSpectrum.class,"querySpectrum").map(this::fillLibrary).forEach(consumer);
    }

    @Override
    public void forEachMergedSpectrum(Consumer<MergedReferenceSpectrum> consumer) throws IOException {
        this.storage.findAllStr(MergedReferenceSpectrum.class).map(this::fillLibrary).forEach(consumer);
    }
}
