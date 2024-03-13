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

package de.unijena.bioinf.chemdb.custom;

import de.unijena.bioinf.ChemistryBase.chem.InChIs;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.babelms.annotations.CompoundMetaData;
import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.babelms.inputresource.InputResourceParsingIterator;
import de.unijena.bioinf.chemdb.AbstractChemicalDatabase;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.chemdb.SpectralUtils;
import de.unijena.bioinf.chemdb.WriteableChemicalDatabase;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import de.unijena.bioinf.spectraldb.SpectralLibrary;
import de.unijena.bioinf.spectraldb.WriteableSpectralLibrary;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.spectraldb.io.SpectralDbMsExperimentParser;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.exception.CDKException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public abstract class CustomDatabase implements SearchableDatabase {
    protected static Logger logger = LoggerFactory.getLogger(CustomDatabase.class);

    public abstract void deleteDatabase();

    protected CustomDatabaseSettings settings;

    public int getDatabaseVersion() {
        return settings.getSchemaVersion();
    }

    @Override
    public String name() {
        return Optional.ofNullable(settings).map(CustomDatabaseSettings::getName).orElse(null);
    }

    @Override
    public String displayName() {
        return Optional.ofNullable(settings).map(CustomDatabaseSettings::getDisplayName).orElse(name());
    }

    public boolean needsUpgrade() {
        //todo nightsky: check fingerprint compatibility?
        return settings.getSchemaVersion() != VersionsInfo.CUSTOM_DATABASE_SCHEMA;
    }

    public abstract String storageLocation();

    @Override
    public boolean isRestDb() {
        return false;
    }

    @Override
    public long getFilterFlag() {
        return CustomDataSources.getFlagFromName(name());
    }

    @Override
    public String toString() {
        return name();
    }

    public abstract void readSettings() throws IOException;

    public abstract void writeSettings(CustomDatabaseSettings settings) throws IOException;

    public void writeSettings() throws IOException {
        writeSettings(settings);
    }

    public CustomDatabaseSettings getSettings() {
        return settings;
    }

    protected synchronized void setSettings(CustomDatabaseSettings config) {
        settings = config;
    }

    public CustomDatabaseSettings.Statistics getStatistics() {
        return settings.getStatistics();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomDatabase that)) return false;
        return storageLocation().equals(that.storageLocation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(storageLocation());
    }

    public abstract AbstractChemicalDatabase toChemDBOrThrow() throws IOException;

    public Optional<AbstractChemicalDatabase> toChemDB() {
        return toOptional(this::toChemDBOrThrow, AbstractChemicalDatabase.class);
    }

    public abstract WriteableChemicalDatabase toWriteableChemDBOrThrow() throws IOException;

    public Optional<WriteableChemicalDatabase> toWriteableChemDB() {
        return toOptional(this::toWriteableChemDBOrThrow, WriteableChemicalDatabase.class);
    }

    public abstract SpectralLibrary toSpectralLibraryOrThrow() throws IOException;

    public Optional<SpectralLibrary> toSpectralLibrary() {
        return toOptional(this::toSpectralLibraryOrThrow, SpectralLibrary.class);
    }

    public abstract WriteableSpectralLibrary toWriteableSpectralLibraryOrThrow() throws IOException;

    public Optional<WriteableSpectralLibrary> toWriteableSpectralLibrary() {
        return toOptional(this::toWriteableSpectralLibraryOrThrow, WriteableSpectralLibrary.class);
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {

        T get() throws IOException;

    }

    private <S> Optional<S> toOptional(ThrowingSupplier<S> supplier, Class<S> type) {
        try {
            return Optional.of(supplier.get());
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("Could not create " + type + " from Custom database'" + name() + "'", e);
            return Optional.empty();
        }
    }

    public void importToDatabase(
            List<InputResource<?>> spectrumFiles,
            List<InputResource<?>> structureFiles,
            @Nullable CustomDatabaseImporter.Listener listener,
            CustomDatabaseImporter importer,
            int bufferSize
    ) throws IOException, CDKException {
        if (listener != null)
            importer.addListener(listener);

        AbstractChemicalDatabase chemDb = this.toChemDBOrThrow();
        WriteableChemicalDatabase writeableChemDB = this.toWriteableChemDBOrThrow();

        try {
            importStructuresToDatabase(structureFiles, importer);


            if (!spectrumFiles.isEmpty()) {
                SpectralLibrary spectralLibrary = this.toSpectralLibraryOrThrow();
                try {
                    WriteableSpectralLibrary writeableSpectralLibrary = this.toWriteableSpectralLibraryOrThrow();

                    Iterator<Ms2Experiment> iterator = new InputResourceParsingIterator(spectrumFiles, new SpectralDbMsExperimentParser());
                    // Map of <SMILES, <STRUCTURE_ID, NAME>>
                    // todo this should be buffered and not just read everything into memory
                    // todo check for inchi in db to not download twice and get duplicate exception
                    Map<String, CompoundMetaData> spectrumSmiles = new HashMap<>();
                    Map<String, List<Ms2ReferenceSpectrum>> spectra = new HashMap<>();
                    { // import all spectra files and fill structure map
                        while (iterator.hasNext()) {
                            Ms2Experiment experiment = iterator.next();
                            String smiles = experiment.getAnnotation(Smiles.class).map(Smiles::toString)
                                    .orElseThrow(() -> new IllegalArgumentException("Spectrum file does not contain SMILES: " + experiment.getSource()));
                            spectra.computeIfAbsent(smiles, s -> new ArrayList<>()).addAll(SpectralUtils.ms2ExpToMs2Ref((MutableMs2Experiment) experiment));
                            if (!spectrumSmiles.containsKey(smiles))
                                //todo speclib: add support for custom structure ids to spectra formats -> important to import in house ref-libs without needing the structure tsv
                                experiment.getAnnotation(CompoundMetaData.class).ifPresentOrElse(
                                        cm -> spectrumSmiles.put(smiles, cm),
                                        () -> spectrumSmiles.put(smiles, CompoundMetaData.builder().compoundName(experiment.getName()).build()));
                        }
                    }

                    // import structures from spectra with SMILES, SPLASH (as ID) and NAME
                    for (Map.Entry<String, CompoundMetaData> entry : spectrumSmiles.entrySet())
                        importer.importFromString(entry.getKey(), entry.getValue().getCompoundId(), entry.getValue().getCompoundName());

                    // update spectrum INCHIs to match structure INCHIs
                    spectra.forEach((k, v) -> {
                        String inchiKey = importer.inchiKeyCache.get(k);
                        assert InChIs.isInchiKey(inchiKey);
                        v.forEach(s -> s.setCandidateInChiKey(inchiKey));
                    });

                    // write spectra to db
                    SpectralUtils.importSpectra(writeableSpectralLibrary, spectra.values().stream().flatMap(List::stream).toList(), bufferSize);
                    importer.flushBuffer();
                } finally {
                    // update spectra statistics
                    getStatistics().spectra().set(spectralLibrary.countAllSpectra());
                }

            }
        } finally {
            // update tags & statistics
            writeableChemDB.updateTags(null, -1);
            getStatistics().compounds().set(chemDb.countAllFingerprints());
            getStatistics().formulas().set(chemDb.countAllFormulas());
            writeSettings();
        }
    }

    private void importStructuresToDatabase(List<InputResource<?>> structureFiles, CustomDatabaseImporter importer) throws IOException {
        try {
            for (InputResource<?> f : structureFiles) {
                try (InputStream s = f.getInputStream()) {
                    importer.importFromStream(s);
                }
            }
        } finally {
            importer.flushBuffer();
        }
    }

    public JJob<Boolean> importToDatabaseJob(
            List<InputResource<?>> spectrumFiles,
            List<InputResource<?>> structureFiles,
            @Nullable CustomDatabaseImporter.Listener listener,
            @NotNull WebAPI<?> api,
            int bufferSize
    ) {
        return new BasicJJob<Boolean>() {
            CustomDatabaseImporter importer;

            @Override
            protected Boolean compute() throws Exception {
                importer = new CustomDatabaseImporter(CustomDatabase.this, api.getCDKChemDBFingerprintVersion(), api, bufferSize);
                importToDatabase(spectrumFiles, structureFiles, listener, importer, bufferSize);
                return true;
            }

            @Override
            public void cancel(boolean mayInterruptIfRunning) {
                if (importer != null)
                    importer.cancel();
                super.cancel(mayInterruptIfRunning);
            }
        }.asCPU();
    }
}
