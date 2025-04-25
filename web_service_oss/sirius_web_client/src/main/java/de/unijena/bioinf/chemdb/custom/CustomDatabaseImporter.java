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

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.babelms.ReportingInputStream;
import de.unijena.bioinf.babelms.annotations.CompoundMetaData;
import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.babelms.inputresource.InputResourceParsingIterator;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintCandidateWrapper;
import de.unijena.bioinf.fingerid.fingerprints.FixedFingerprinter;
import de.unijena.bioinf.fingerid.fingerprints.cache.IFingerprinterCache;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.biotransformer.BioTransformerJJob;
import de.unijena.bioinf.ms.biotransformer.BioTransformerResult;
import de.unijena.bioinf.ms.biotransformer.BioTransformerSettings;
import de.unijena.bioinf.spectraldb.WriteableSpectralLibrary;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.spectraldb.io.SpectralDbMsExperimentParser;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.webapi.WebAPI;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class CustomDatabaseImporter {
    private final NoSQLCustomDatabase<?, ?> database;
    private WriteableSpectralLibrary databaseAsSpecLib;

    private final Queue<Listener> listeners = new LinkedList<>();

    // molecule buffer:  used to bundle molecular formula requests
    private final List<Molecule> moleculeBuffer;
    private final int molBufferSize;

    // spectra buffer: used to import multiple spectra at once into db but do not keep all in memory
    private final List<Ms2ReferenceSpectrum> spectraBuffer;
    private final int specBufferSize;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    final protected ConcurrentLinkedQueue<FingerprintCalculator> freeFingerprinter = new ConcurrentLinkedQueue<>();
    protected SmilesGenerator smilesGen;
    protected SmilesParser smilesParser;
    protected InChIGeneratorFactory inchiGenFactory;
    protected InChIGenerator inchiGen;
    protected CdkFingerprintVersion fingerprintVersion;
    protected final WebAPI<?> api;
    protected final IFingerprinterCache ifpCache;
    // a magic number of bytes that represent the number of bytes in the input that correspond to on compound.
    //todo we should estimate this based on the file format instead.
    private static final int BYTE_EQUIVALENTS = 52428;

    private final BioTransformerSettings bioTransformerSettings;

    // todo make abstract and implement different versions for blob and document storage
    private CustomDatabaseImporter(@NotNull NoSQLCustomDatabase<?, ?> database, CdkFingerprintVersion version, WebAPI<?> api, @Nullable IFingerprinterCache ifpCache, int bufferSize, BioTransformerSettings settings) {
        this.api = api;
        this.database = database;
        this.fingerprintVersion = version;
        this.ifpCache = ifpCache == null ? IFingerprinterCache.NOOP_CACHE : ifpCache;

        this.molBufferSize = bufferSize;
        this.specBufferSize = bufferSize;

        this.moleculeBuffer = new ArrayList<>((int) (molBufferSize * 1.25));
        this.spectraBuffer = new ArrayList<>((int) (specBufferSize * 1.25));

        smilesGen = SmilesGenerator.generic().aromatic();
        smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        smilesParser.kekulise(true);

        this.bioTransformerSettings = settings;

    }

    public InChIGenerator generateInChI(IAtomContainer container) throws CDKException {
        InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
        InChIGenerator generator = factory.getInChIGenerator(container);

        return generator;

    }

    private void throwIfShutdown() {
        if (shutdown.get())
            throw new IllegalStateException("Importer has already been shutdown or cancelled!");
    }

    public synchronized void flushAll() throws IOException {
        flushSpectraBuffer();
        flushMoleculeBuffer();
    }

    public synchronized void updateStatistics() throws IOException {
        synchronized (database) {
            CustomDatabase.logger.info("Updating database statistics...");

            // update tags & statistics
            database.toSpectralLibrary()
                    .ifPresent(sl -> {
                        try {
                            long spectraCount = sl.countAllSpectra();
                            CustomDatabase.logger.info("Found " + spectraCount + " spectra in database");
                            database.getStatistics().spectra().set(spectraCount);
                        } catch (IOException e) {
                            CustomDatabase.logger.error("Error counting spectra", e);
                            throw new RuntimeException(e);
                        }
                    });

            try {
                database.database.updateTags(null, -1);
                long compoundCount = database.database.countAllFingerprints();
                long formulaCount = database.database.countAllFormulas();

                CustomDatabase.logger.info("Found " + compoundCount + " compounds and " + formulaCount + " formulas in database");

                database.getStatistics().compounds().set(compoundCount);
                database.getStatistics().formulas().set(formulaCount);
                database.writeSettings();

                CustomDatabase.logger.info("Database statistics updated successfully");
            } catch (Exception e) {
                CustomDatabase.logger.error("Error updating database statistics", e);
                throw e;
            }
        }
    }

    public synchronized void flushAllAndUpdateStatistics() throws IOException {
        try {
            flushAll();
        } finally {
            updateStatistics();
        }
    }

    public void cancel() {
        shutdown.set(true);
    }

    public void addListener(Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }


    public void importSpectraFromResources(List<InputResource<?>> spectrumFiles) throws IOException {
        throwIfShutdown();
        InputResourceParsingIterator iterator = new InputResourceParsingIterator(spectrumFiles, new SpectralDbMsExperimentParser());
        iterator.addBytesRaiseListener((read, readTotal) -> {
            synchronized (listeners) {
                listeners.forEach(l -> l.bytesRead(read));
            }
        });
        while (iterator.hasNext()) {
            Ms2Experiment experiment = iterator.next();
            List<Ms2ReferenceSpectrum> specs = SpectralUtils.ms2ExpToMs2Ref((MutableMs2Experiment) experiment);

            Optional<String> maybeSmiles = experiment.getAnnotation(Smiles.class).map(Smiles::toString);

            if (maybeSmiles.isEmpty()) {
                log.warn("Record {} from {} does not contain SMILES. Skipping.", experiment.getName(), experiment.getSource());
                continue;
            }

            String smiles = maybeSmiles.get();

            CompoundMetaData metaData = experiment.getAnnotation(CompoundMetaData.class)
                    .orElseGet(() -> CompoundMetaData.builder()
                            .compoundName(experiment.getName())
                            .build());

            //todo speclib: add support for custom structure ids to spectra formats -> important to import in house ref-libs without needing the structure tsv
            Optional<Molecule> molecule = importStructuresFromSmileAndInChis(smiles, metaData.getCompoundId(), metaData.getCompoundName());
            if (molecule.isEmpty()) {
                log.warn("Record {} from {} could not be mapped to a known structure. Skipping.", experiment.getName(), experiment.getSource());
                continue;
            }
            specs.forEach(s -> s.setCandidateInChiKey(molecule.get().getInchi().key2D()));
            addToSpectraBuffer(specs);
        }
        if (!iterator.getParsingErrors().isEmpty()) {
            String files = "'" + String.join("', '", iterator.getParsingErrors().keySet()) + "'";
            throw new RuntimeException("Following files could not be imported: " + files);
        }
    }

    public Optional<Molecule> importStructuresFromSmileAndInChis(@Nullable String smilesOrInChI, @Nullable String id, @Nullable String name) {
        throwIfShutdown();
        if (smilesOrInChI == null || smilesOrInChI.isBlank()) {
            LoggerFactory.getLogger(getClass()).warn("No structure information given in Line ' " + smilesOrInChI + "\t" + id + "\t" + name + "'. Skipping!");
            return Optional.empty();
        }

        InChI inchi;
        Smiles smiles;
        IAtomContainer container;
        try {
            if (InChIs.isInchi(smilesOrInChI)) {
                if (!InChIs.isConnected(smilesOrInChI)) {
                    LoggerFactory.getLogger(getClass()).warn(
                            String.format("Compound '%s' is Not connected! Only connected structures are supported! Skipping.", smilesOrInChI));
                    return Optional.empty();
                }

                if (InChIs.isMultipleCharged(smilesOrInChI)) {
                    LoggerFactory.getLogger(getClass()).warn(
                            String.format("Compound '%s' is multiple charged! Only neutral or single charged compounds are supported! Skipping.", smilesOrInChI));
                    return Optional.empty();
                }

                container = InChISMILESUtils.getAtomContainerFromInchi(smilesOrInChI);
                inchi = InChIs.newInChI(smilesOrInChI);
                smiles = new Smiles(smilesGen.create(container));
            } else {
                if (!SmilesU.isConnected(smilesOrInChI)) {
                    LoggerFactory.getLogger(getClass()).warn(
                            String.format("Compound '%s' is Not connected! Only connected structures are supported! Skipping.", smilesOrInChI));
                    return Optional.empty();
                }

                if (SmilesU.isMultipleCharged(smilesOrInChI)) {
                    LoggerFactory.getLogger(getClass()).warn(
                            String.format("Compound '%s' is multiple charged! Only neutral or single charged compounds are supported! Skipping.", smilesOrInChI));
                    return Optional.empty();
                }

                container = smilesParser.parseSmiles(smilesOrInChI);
                smiles = new Smiles(smilesOrInChI);
                inchi = InChISMILESUtils.getInchi(container, false);
            }


        } catch (CDKException e) {
            LoggerFactory.getLogger(getClass()).warn(String.format("Error when parsing molecule: '%s'! Skipping.", smilesOrInChI));
            return Optional.empty();
        }

        final Molecule molecule = new Molecule(container, smiles, inchi);
        molecule.ids.add(id);
        molecule.name = name;
        addMolecule(molecule);
        return Optional.of(molecule);
    }

    public void importStructuresFromSmileAndInChis(InputStream stream) throws IOException {
        throwIfShutdown();
        // checkConnectionToUrl for SMILES and InChI formats
        final BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = br.readLine()) != null) {
            checkCancellation();
            //skip empty lines
            if (!line.isBlank()) {
                String[] parts = line.split("\t");
                final String structure = parts[0].trim();

                final String id = parts.length > 1 ? parts[1] : null;
                final String name = parts.length > 2 ? parts[2] : null;
                importStructuresFromSmileAndInChis(structure, id, name);
            }
        }
    }

    public void importStructuresFromResources(List<InputResource<?>> structureFiles) throws IOException {
        throwIfShutdown();
        for (InputResource<?> f : structureFiles) {
            try (ReportingInputStream s = f.getReportingInputStream()) {
                s.addBytesRaiseListener((rb, rbTotal) -> {
                    synchronized (listeners) {
                        listeners.forEach(l -> l.bytesRead(rb));
                    }
                });
                importStructuresFromSmileAndInChis(s);
            }
        }
    }


    protected void addToSpectraBuffer(List<Ms2ReferenceSpectrum> spectra) throws ChemicalDatabaseException {
        synchronized (spectraBuffer) {
            spectraBuffer.addAll(spectra);
//            for (Listener l : listeners) l.newSpectra(spectraBuffer.size());
            if (spectraBuffer.size() > specBufferSize)
                flushSpectraBuffer();
        }
    }

    protected void flushSpectraBuffer() throws ChemicalDatabaseException {
        if (databaseAsSpecLib == null)
            try {
                databaseAsSpecLib = database.toWriteableSpectralLibraryOrThrow();
            } catch (IOException e) {
                throw new IllegalArgumentException("Structure db cannot be converted to spectral library", e);
            }

        //todo do flush in background?
        final ArrayList<Ms2ReferenceSpectrum> spectra;
        synchronized (spectraBuffer) {
            spectra = new ArrayList<>(spectraBuffer);
            spectraBuffer.clear();
        }
        if (!spectra.isEmpty())
            SpectralUtils.importSpectra(databaseAsSpecLib, spectra, spectra.size());

    }

    protected void addMolecule(Molecule mol) {
        synchronized (moleculeBuffer) {
            moleculeBuffer.add(mol);
//            for (Listener l : listeners) l.newMolecules(moleculeBuffer.size());
        }
        if (moleculeBuffer.size() > molBufferSize)
            flushMoleculeBuffer();
    }



    private void flushMoleculeBuffer() {
        // start downloading
        if (!moleculeBuffer.isEmpty()) {
            synchronized (moleculeBuffer) {
                checkCancellation();
                try {
                    final ConcurrentHashMap<String, Comp> key2DToComp = new ConcurrentHashMap<>(moleculeBuffer.size());

                    for (Molecule c : moleculeBuffer) {
                        checkCancellation();
                        try {
                            final InChI inchi = c.inchi;
                            final String key2d = inchi.key2D();
                            if (key2DToComp.containsKey(key2d)) {
                                Comp comp = key2DToComp.get(key2d);
                                comp.molecule.ids.addAll(c.ids);
                                if ((c.name != null && !c.name.isBlank()) && (comp.molecule.name == null || comp.molecule.name.isBlank() || comp.molecule.name.length() > c.name.length()))
                                    comp.molecule.name = c.name;
                            } else {
                                Comp comp = new Comp(c);
                                key2DToComp.put(key2d, comp);
                            }
                        } catch (IllegalArgumentException e) {
                            CustomDatabase.logger.error("Error when flushing molecule. Skipping: " + c.ids + " - " + c.name, e);
                        }
                    }
                    checkCancellation();

/*
                    // todo Jonas: this could be the right place to create and run tranformer jobs if needed
                    // run transformer as configured on all molecule in key2DToComp (valueset)
                    // add tranformation tesult molecules to key2DToComp (2d deduplication as above)

                    BioTransformerJJob job = new BioTransformerJJob();
                    job.setSubstrates(key2DToComp.values().stream()
                            .map(comp -> comp.molecule.container)
                            .toList());

                    List<BioTransformerResult> result = SiriusJobs.getGlobalJobManager().submitJob(job).awaitResult();
                    //todo convert AtomContains in results into Molecules and perform  deduplication as done above.
                    result.stream().map(res -> res.getBiotranformations().stream().map(t -> t.))
*/
                    if (bioTransformerSettings != null) {
                        CustomDatabase.logger.info("BioTransformer settings detected: " + bioTransformerSettings.getMetabolicTransformation() +
                                ", CYP450 Mode: " + bioTransformerSettings.getCyp450Mode() +
                                ", P2 Mode: " + bioTransformerSettings.getP2Mode() +
                                ", Iterations: " + bioTransformerSettings.getIterations());
                        CustomDatabase.logger.info("Preparing to transform " + key2DToComp.size() + " molecules");
                        BioTransformerJJob job = new BioTransformerJJob(bioTransformerSettings);
                        job.setSubstrates(
                                key2DToComp.values().stream()
                                        .map(comp -> comp.molecule.container) // Aus Molecule -> IAtomContainer
                                        .toList()
                        );
                        CustomDatabase.logger.info("Created BioTransformerJJob with " + job.getSubstrates().size() + " substrates");
                        for (IAtomContainer container : job.getSubstrates()) {
                            try {
                                CustomDatabase.logger.info("Substrate SMILES: " + smilesGen.create(container));
                            } catch (Exception e) {
                                CustomDatabase.logger.error("Error generating SMILES for substrate", e);
                            }
                        }
                        CustomDatabase.logger.info("Submitting BioTransformerJJob to job manager");
                        List<BioTransformerResult> transformationResults = SiriusJobs.getGlobalJobManager().submitJob(job).awaitResult();
                        CustomDatabase.logger.info("BioTransformerJJob completed with " + transformationResults.size() + " results");
                        for (BioTransformerResult result : transformationResults) {
                            CustomDatabase.logger.info("Result contains " + result.getBiotranformations().size() + " biotransformations");
                            CustomDatabase.logger.info("Result contains " + result.getAllProductContainers().size() + " product containers");
                        }

                        // 2. Transformations in Molecule konvertieren

                        List<Molecule> transformedMolecules = transformationResults.stream()
                                // Iteriere über alle Ergebnisse und hole direkt alle Produkt-Container pro Ergebnis
                                .flatMap(result -> result.getAllProductContainers().stream()) // Verwende die neue Methode
                                // Konvertiere jeden IAtomContainer in ein Molecule
                                .map(container -> {
                                    try {
                                        // Die Logik zur Erstellung von Molecule bleibt gleich
                                        InChIGenerator inchiGenerator = generateInChI(container); // Annahme: generateInChI gibt InChIGenerator zurück
                                        String inchiValue = inchiGenerator.getInchi();
                                        String inchiKey = inchiGenerator.getInchiKey();
                                        String smilesValue = smilesGen.create(container);

                                        return new Molecule(container, new Smiles(smilesValue), new InChI(inchiValue, inchiKey));
                                    } catch (CDKException e) {
                                        // Passende Fehlerbehandlung, hier RuntimeException wie im Original
                                        throw new RuntimeException("Fehler bei der Konvertierung von IAtomContainer zu Molecule", e);
                                    }
                                })
                                .toList(); // oder .collect(Collectors.toList()); je nach Java-Version/Präferenz

                        CustomDatabase.logger.info("Created " + transformedMolecules.size() + " transformed molecules");
                        for (Molecule mol : transformedMolecules) {
                            CustomDatabase.logger.info("Transformed molecule: " + mol.smiles + ", InChI Key: " + mol.inchi.key2D());
                        }


                        //TODO: welche ? subrstrates oder Products? beide? und wie einfügen??


                        // 3. Deduplikation basierend auf InChIKey-2D
                        for (Molecule newMolecule : transformedMolecules) {
                            try {
                                final InChI inchi = newMolecule.inchi;
                                final String key2d = inchi.key2D(); // InChIKey-2D als Schlüssel
                                if (key2DToComp.containsKey(key2d)) {
                                    // Wenn Molekül bereits existiert, IDs zusammenfügen und Namen vergleichen
                                    Comp existingComp = key2DToComp.get(key2d);
                                    existingComp.molecule.ids.addAll(newMolecule.ids);
                                    if ((newMolecule.name != null && !newMolecule.name.isBlank()) &&
                                            (existingComp.molecule.name == null || existingComp.molecule.name.isBlank() ||
                                                    existingComp.molecule.name.length() > newMolecule.name.length())) {
                                        existingComp.molecule.name = newMolecule.name; // Kürzeren oder besseren Namen übernehmen
                                    }
                                } else {
                                    // Neues Molekül hinzufügen
                                    Comp newComp = new Comp(newMolecule);
                                    key2DToComp.put(key2d, newComp);
                                }
                            } catch (IllegalArgumentException e) {
                                // Fehlerhafte Moleküle ignorieren, aber loggen
                                CustomDatabase.logger.error("Error deduplicating molecule. Skipping: " + newMolecule.ids + " - " + newMolecule.name, e);
                            }
                        }
                        CustomDatabase.logger.info("After transformation, key2DToComp contains " + key2DToComp.size() + " unique molecules");

                    }


                    CustomDatabase.logger.info("Looking up compounds to find existing fps");
                    try {
                        lookupAndAnnotateMissingCandidates(key2DToComp);
                    } catch (Exception e) {
                        // if lookup fails, we can still download or compute locally and override
                        CustomDatabase.logger.error(e.getMessage(), e);
                    }
                    checkCancellation();

                    CustomDatabase.logger.info("Try downloading missing fps");
                    try { //try to download fps for compound
                        downloadAndAnnotateMissingCandidates(key2DToComp);
                    } catch (Exception e) {
                        // if download fails, we can still compute locally
                        CustomDatabase.logger.error(e.getMessage(), e);
                    }
                    checkCancellation();

                    CustomDatabase.logger.info("Computing missing fps that are still missing.");
                    computeAndAnnotateMissingCandidates(key2DToComp);
                    checkCancellation();

                    storeCandidates(key2DToComp.values().stream().map(c -> c.candidate).filter(Objects::nonNull).toList());
                    checkCancellation();
                } catch (Exception e) {
                    //now we might have inconsistent data -> fail/stop import.
                    CustomDatabase.logger.error(e.getMessage(), e);
                    cancel();
                    throw new RuntimeException("Databse import failed!", e);
                } finally {
                    moleculeBuffer.clear();
                }
            }
        }
        // Check database writability
        try {
            CustomDatabase.logger.info("Checking database writability...");
            boolean canWrite = database.toWriteableChemDB().isPresent();
            CustomDatabase.logger.info("Database is " + (canWrite ? "writable" : "NOT writable"));

            if (!canWrite) {
                CustomDatabase.logger.error("Database is not writable! This may explain why no transformations are being saved.");
            } else {
                // Try to get statistics to verify database access
                CustomDatabase.logger.info("Current database statistics: Compounds=" +
                        database.getStatistics().getCompounds() +
                        ", Formulas=" + database.getStatistics().getFormulas() +
                        ", Spectra=" + database.getStatistics().getSpectra());
            }
        } catch (Exception e) {
            CustomDatabase.logger.error("Error checking database writability", e);
        }
    }


    private void lookupAndAnnotateMissingCandidates(final ConcurrentHashMap<String, Comp> key2DToComp) throws IOException {
        synchronized (database) {
            for (Comp comp : key2DToComp.values()) {
                checkCancellation();
                if (comp.candidate == null) {
                    comp.candidate = database.database.getStorage() //todo do we need the fp here?
                            .findStr(Filter.where("inchiKey").eq(comp.key2D()), FingerprintCandidateWrapper.class, "fingerprint")
                            .findFirst()
                            .orElse(null);
                    mergeLinksAndNames(comp);
                    notifyFingerprintCreation(comp);
                }
            }
        }
    }

    private void downloadAndAnnotateMissingCandidates(final ConcurrentHashMap<String, Comp> key2DToComp) throws IOException {
        Set<MolecularFormula> formulasToSearch = new HashSet<>();
        checkCancellation();
        try {
            for (Comp comp : key2DToComp.values())
                if (comp.candidate == null) //group by formula to reduce unnecessary downloads
                    formulasToSearch.add(InChIs.extractNeutralFormulaByAdjustingHsOrThrow(comp.inChI2D()));
        } catch (UnknownElementException e) {
            throw new IOException(e);
        }

        checkCancellation();
        List<JJob<Boolean>> jobs = formulasToSearch.stream().map(formula -> new TinyBackgroundJJob<Boolean>() {
            @Override
            protected Boolean compute() throws Exception {
                checkCancellation();
                api.consumeStructureDB(0, db -> {
                    List<FingerprintCandidate> cans = db.lookupStructuresAndFingerprintsByFormula(formula).stream().filter(s -> DataSource.isInAll(s.getBitset())).toList();
                    for (FingerprintCandidate can : cans) {
                        checkCancellation();
                        Comp toAdd = key2DToComp.get(can.getInchi().key2D());
                        if (toAdd != null) {
                            toAdd.candidate = FingerprintCandidateWrapper.of(formula, can);
                            clearAndCreateLinksAndName(toAdd);
                            CustomDatabase.logger.info("{} downloaded", toAdd.candidate.getCandidate(null, null).getInchi().in2D);
                            notifyFingerprintCreation(toAdd);
                        }
                    }
                });
                return true;
            }
        }).collect(Collectors.toList());

        checkCancellation();

        SiriusJobs.getGlobalJobManager().submitJobsInBatches(jobs).forEach(JJob::getResult);
    }

    private void computeAndAnnotateMissingCandidates(final ConcurrentHashMap<String, Comp> key2DToComp) {
        // compound fps locally if not already downloaded or loaded from local db
        List<BasicJJob<Void>> jobs = key2DToComp.values().stream()
                .filter(c -> c.candidate == null)
                .map(c -> new BasicJJob<Void>() {
                    @Override
                    protected Void compute() throws Exception {
                        FingerprintCalculator fcalc = null;
                        try {
                            fcalc = getFingerprintCalculator();
                            c.candidate = fcalc.computeNewCandidate(c.molecule); //adding links and name info is done here.
                            notifyFingerprintCreation(c);
                        } finally {
                            if (fcalc != null)
                                freeFingerprinter.offer(fcalc);
                        }
                        return null;
                    }
                }).collect(Collectors.toList());

        checkCancellation();

        List<BasicJJob<Void>> batches = SiriusJobs.getGlobalJobManager().submitJobsInBatches(jobs);

        batches.forEach(j -> {
            try {
                if (shutdown.get())
                    j.cancel(true);
                j.awaitResult();
            } catch (ExecutionException e) {
                CustomDatabase.logger.error(e.getMessage(), e);
            }
        });
    }


    private FingerprintCalculator getFingerprintCalculator() {
        FingerprintCalculator calc = freeFingerprinter.poll();
        if (calc == null)
            calc = new FingerprintCalculator(fingerprintVersion, ifpCache);
        return calc;
    }

    private void storeCandidates(Collection<FingerprintCandidateWrapper> candidates) throws IOException {
        synchronized (database) {
            database.database.getStorage().upsertAll(candidates);
            List<InChI> inchis = candidates.stream().map(candidate -> candidate.getCandidate(null, null).getInchi()).toList();

            synchronized (listeners) {
                for (Listener l : listeners)
                    l.newInChI(inchis);
            }
        }
    }

    //used to merge information from existing entries in this custom db.
    private void mergeLinksAndNames(@NotNull Comp comp) {
        if (comp.molecule == null || comp.candidate == null)
            return;

        Molecule molecule = comp.molecule;
        CompoundCandidate fc = comp.candidate.getCandidate(null, null);

        fc.setBitset(0);//bit sets of custom dbs are non-persistent, so every custom db entry stores a zero.

        if ((molecule.name != null && !molecule.name.isBlank()) && (fc.getName() == null || fc.getName().isBlank() || fc.getName().length() > molecule.name.length()))
            fc.setName(molecule.name);

        final HashSet<DBLink> links = new HashSet<>(fc.getMutableLinks());

        if (!molecule.ids.isEmpty()) {
            molecule.ids.stream().filter(Objects::nonNull).map(id -> new DBLink(null, id)).forEach(links::add);
            if (fc.getName() == null || fc.getName().isBlank())
                fc.setName(molecule.ids.iterator().next());
        }

        fc.setLinks(new ArrayList<>(links));
    }

    //used to clear link data from remote db and add links of this custom db
    private void clearAndCreateLinksAndName(@NotNull Comp comp) {
        if (comp.molecule == null || comp.candidate == null)
            return;

        Molecule molecule = comp.molecule;
        CompoundCandidate fc = comp.candidate.getCandidate(null, null);

        fc.setBitset(0);//bit sets of custom dbs are non-persistent, so every custom db entry stores a zero.
        fc.setLinks(List.of());

        //set custom db name or id if name is null. otherwise keep the downloaded name from remote db.
        if (molecule.name != null)
            fc.setName(molecule.name);

        //override remote db links.
        if (!molecule.ids.isEmpty()) {
            fc.setLinks(molecule.ids.stream().filter(Objects::nonNull).map(id -> new DBLink(null, id)).toList()); //we add just id so that names can be added during db retrieval
            if (fc.getName() == null || fc.getName().isBlank())
                fc.setName(molecule.ids.iterator().next());
        }
    }

    private void checkCancellation() {
        if (shutdown.get())
            throw new CancellationException("Import Cancelled");
    }


    // INNER CLASSES
    @Getter
    public static class Molecule {
        @NotNull
         final InChI inchi;
        @NotNull
         final Smiles smiles;
         final Set<String> ids = new HashSet<>();
         String name = null;
        @NotNull
         final IAtomContainer container;

          Molecule(@NotNull IAtomContainer container, @NotNull Smiles smiles, @NotNull InChI inchi) {
            this.container = container;
            this.smiles = smiles;
            this.inchi = inchi;
        }
    }

    static class Comp {
        Molecule molecule;
        FingerprintCandidateWrapper candidate;

        String inChI2D() {
            return molecule.inchi.in2D;
        }

        String key2D() {
            return molecule.inchi.key2D();
        }

        Comp(Molecule molecule) {
            this.molecule = molecule;
        }
    }

    private static class FingerprintCalculator {
        private final FixedFingerprinter fingerprinter;
        private final LogPEstimator logPEstimator;

        public FingerprintCalculator(CdkFingerprintVersion version, IFingerprinterCache cache) {
            this.fingerprinter = new FixedFingerprinter(version, cache);
            this.logPEstimator = new LogPEstimator();
        }

        private FingerprintCandidateWrapper computeNewCandidate(Molecule molecule) throws CDKException, IllegalArgumentException, UnknownElementException {
            CustomDatabase.logger.info("Compute fingerprint for {}", molecule.getInchi().in2D);
            final ArrayFingerprint fps = fingerprinter.computeFingerprintFromSMILES(molecule.smiles.smiles);

            final FingerprintCandidate fc = new FingerprintCandidate(molecule.getInchi(), fps);
            fc.setSmiles(molecule.smiles.smiles);
            fc.setBitset(0);//bit sets of custom dbs are non-persistent, so every custom db entry stores a zero.

            if (molecule.name != null)
                fc.setName(molecule.name);

            if (!molecule.ids.isEmpty()) {
                fc.setLinks(molecule.ids.stream().filter(Objects::nonNull).map(id -> new DBLink(null, id)).toList());
                if (fc.getName() == null || fc.getName().isEmpty())
                    fc.setName(molecule.ids.iterator().next()); //set id as name if no name was set
            }
            // compute XLOGP
            fc.setXlogp(logPEstimator.prepareMolAndComputeLogP(molecule.container));
            return FingerprintCandidateWrapper.of(fc);
        }
    }


    private void notifyFingerprintCreation(Comp comp) {
        synchronized (listeners) {
            listeners.forEach(l -> l.newFingerprint(comp.molecule.getInchi(), BYTE_EQUIVALENTS));
        }
    }


    @FunctionalInterface
    public interface Listener {
        default void newFingerprint(InChI inChI, int byteEquivalent) {
        }

        // informs about imported molecule
        void newInChI(List<InChI> inchis);

        default void bytesRead(int numOfBytes) {

        }
    }


    public static void importToDatabase(
            List<InputResource<?>> spectrumFiles,
            List<InputResource<?>> structureFiles,
            CustomDatabaseImporter importer
    ) throws IOException {

        try {
            if (structureFiles != null && !structureFiles.isEmpty())
                importer.importStructuresFromResources(structureFiles);

            if (spectrumFiles != null && !spectrumFiles.isEmpty())
                importer.importSpectraFromResources(spectrumFiles);

        } finally {
            // update tags & statistics
            importer.flushAllAndUpdateStatistics();
        }
    }


    public static JJob<Boolean> makeImportToDatabaseJob(
            List<InputResource<?>> spectrumFiles,
            List<InputResource<?>> structureFiles,
            @Nullable CustomDatabaseImporter.Listener listener,
            @NotNull NoSQLCustomDatabase<?, ?> database, WebAPI<?> api,
            @Nullable IFingerprinterCache ifpCache,
            int bufferSize,
            BioTransformerSettings settings
    ) {
        return new BasicJJob<Boolean>() {
            CustomDatabaseImporter importer;
            final CustomDatabaseImporter.Listener l = listener;

            @Override
            protected Boolean compute() throws Exception {
                importer = new CustomDatabaseImporter(database, api.getCDKChemDBFingerprintVersion(), api, ifpCache, bufferSize, settings);
                if (listener != null)
                    importer.addListener(listener);
                importToDatabase(spectrumFiles, structureFiles, importer);
                return true;
            }

            @Override
            public void cancel(boolean mayInterruptIfRunning) {
                super.cancel(mayInterruptIfRunning);
                if (importer != null)
                    importer.cancel();
            }

            @Override
            protected void cleanup() {
                super.cleanup();
                if (l != null)
                    importer.removeListener(l);
            }
        }.asCPU();
    }
}
