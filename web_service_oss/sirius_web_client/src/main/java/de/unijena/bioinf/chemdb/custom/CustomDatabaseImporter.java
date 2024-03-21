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
import de.unijena.bioinf.babelms.annotations.CompoundMetaData;
import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.babelms.inputresource.InputResourceParsingIterator;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.chemdb.nitrite.wrappers.FingerprintCandidateWrapper;
import de.unijena.bioinf.fingerid.fingerprints.FixedFingerprinter;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.spectraldb.WriteableSpectralLibrary;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;
import de.unijena.bioinf.spectraldb.io.SpectralDbMsExperimentParser;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.webapi.WebAPI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.interfaces.IChemSequence;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CustomDatabaseImporter {
    private final NoSQLCustomDatabase<?, ?> database;
    private WriteableSpectralLibrary databaseAsSpecLib;
    private final String dbname;

    Queue<Listener> listeners = new ConcurrentLinkedQueue<>();

    // fingerprint buffer: used to compute and import multiple fps at once but keep not all in memory
    private final List<FingerprintCandidateWrapper> buffer;
    private final int bufferSize;

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
    protected CdkFingerprintVersion fingerprintVersion;
    protected final WebAPI<?> api;

    // todo make abstract and implement different versions for blob and document storage
    protected CustomDatabaseImporter(@NotNull NoSQLCustomDatabase<?, ?> database, CdkFingerprintVersion version, WebAPI<?> api, int bufferSize) {
        this.api = api;
        this.database = database;
        this.dbname = database.name();
        this.fingerprintVersion = version;

        this.bufferSize = bufferSize;
        this.molBufferSize = bufferSize;
        this.specBufferSize = bufferSize;

        this.buffer = new ArrayList<>((int) (this.bufferSize * 1.25));
        this.moleculeBuffer = new ArrayList<>((int) (molBufferSize * 1.25));
        this.spectraBuffer = new ArrayList<>((int) (specBufferSize * 1.25));

        smilesGen = SmilesGenerator.generic().aromatic();
        smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        smilesParser.kekulise(true);
    }

    private void throwIfShutdown() {
        if (shutdown.get())
            throw new IllegalStateException("Importer has already been shutdown or cancelled!");
    }

    public synchronized void flushAll() throws IOException {
        flushBuffer();
        flushSpectraBuffer();
    }

    public synchronized void updateStatistics() throws IOException {
        // update tags & statistics
        database.toSpectralLibrary()
                .ifPresent(sl -> {
                    try {
                        database.getStatistics().spectra().set(sl.countAllSpectra());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        database.database.updateTags(null, -1);
        database.getStatistics().compounds().set(database.database.countAllFingerprints());
        database.getStatistics().formulas().set(database.database.countAllFormulas());
        database.writeSettings();
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
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void importSpectraFromResources(List<InputResource<?>> spectrumFiles) throws IOException {
        throwIfShutdown();
        Iterator<Ms2Experiment> iterator = new InputResourceParsingIterator(spectrumFiles, new SpectralDbMsExperimentParser());
        while (iterator.hasNext()) {
            Ms2Experiment experiment = iterator.next();
            List<Ms2ReferenceSpectrum> specs = SpectralUtils.ms2ExpToMs2Ref((MutableMs2Experiment) experiment);

            String smiles = experiment.getAnnotation(Smiles.class).map(Smiles::toString)
                    .orElseThrow(() -> new IllegalArgumentException("Spectrum file does not contain SMILES: " + experiment.getSource()));
            CompoundMetaData metaData = experiment.getAnnotation(CompoundMetaData.class).orElseGet(() ->
                    CompoundMetaData.builder().compoundName(experiment.getName()).build());

            //todo speclib: add support for custom structure ids to spectra formats -> important to import in house ref-libs without needing the structure tsv
            importStructureFromString(smiles, metaData.getCompoundId(), metaData.getCompoundName())
                    .map(CustomDatabaseImporter.Molecule::getInchi)
                    .map(InChI::key2D)
                    .ifPresent(key -> specs.forEach(s -> s.setCandidateInChiKey(key)));

            addToSpectraBuffer(specs);
        }
    }

    public void importStructureFromString(String smilesOrInChI) throws IOException {
        throwIfShutdown();
        importStructureFromString(smilesOrInChI, null, null);
    }

    public Optional<Molecule> importStructureFromString(@Nullable String smilesOrInChI, @Nullable String id, @Nullable String name) throws IOException {
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
        molecule.id = id;
        molecule.name = name;
        addMolecule(molecule);
        return Optional.of(molecule);
    }

    public void importStructureFromStream(InputStream stream) throws IOException {
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
                importStructureFromString(structure, id, name);
            }
        }
    }

    public void importStructuresFromResources(List<InputResource<?>> structureFiles) throws IOException {
        throwIfShutdown();
        for (InputResource<?> f : structureFiles) {
            try (InputStream s = f.getInputStream()) {
                importStructureFromStream(s);
            }
        }
    }

    public void importStructureFromFile(File file) throws IOException {
        throwIfShutdown();
        ReaderFactory factory = new ReaderFactory();
        ISimpleChemObjectReader reader;
        try (InputStream stream = new FileInputStream(file)) {
            reader = factory.createReader(stream);
        }
        if (reader != null) {
            try (InputStream stream = new FileInputStream(file)) {
                try {
                    reader.setReader(stream);
                    IChemFile chemFile = SilentChemObjectBuilder.getInstance().newInstance(IChemFile.class);
                    chemFile = reader.read(chemFile);
                    for (IChemSequence s : chemFile.chemSequences()) {
                        for (IChemModel m : s.chemModels()) {
                            for (IAtomContainer c : m.getMoleculeSet().atomContainers()) {
                                checkCancellation();
                                InChI inchi = InChISMILESUtils.getInchi(c, false);
                                Smiles smiles = new Smiles(smilesGen.create(c));
                                Molecule molecule = new Molecule(c, smiles, inchi);
                                addMolecule(molecule);
                            }
                        }
                    }
                } catch (CDKException e) {
                    throw new IOException(e);
                }
            }
        } else {
            try (FileInputStream s = new FileInputStream(file)) {
                importStructureFromStream(s);
            }
        }
    }


    protected void addToSpectraBuffer(List<Ms2ReferenceSpectrum> spectra) throws ChemicalDatabaseException {
        synchronized (spectraBuffer) {
            spectraBuffer.addAll(spectra);
            for (Listener l : listeners) l.newSpectraBufferSize(spectraBuffer.size());
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
            for (Listener l : listeners) l.newMoleculeBufferSize(moleculeBuffer.size());
            if (moleculeBuffer.size() > molBufferSize) {
                flushMoleculeBuffer();
            }
        }
    }

    private void flushMoleculeBuffer() {
        // start downloading
        if (!moleculeBuffer.isEmpty()) {
            final ConcurrentHashMap<String, Comp> key2DToComp = new ConcurrentHashMap<>(moleculeBuffer.size());
            try {
                for (Molecule c : moleculeBuffer) {
                    checkCancellation();
                    try {
                        final InChI inchi = c.inchi;
                        final String key2d = inchi.key2D();
                        if (key2DToComp.containsKey(key2d)) {
                            Comp comp = key2DToComp.get(key2d);
                            if (comp.molecule.id == null && c.id != null)
                                comp.molecule.id = c.id;
                            if (comp.molecule.name == null && c.name != null)
                                comp.molecule.name = c.name;
                        } else {
                            Comp comp = new Comp(c);
                            key2DToComp.put(key2d, comp);
                        }
                    } catch (IllegalArgumentException e) {
                        CustomDatabase.logger.error(e.getMessage(), e);
                    }
                }
            } catch (IllegalArgumentException e) {
                CustomDatabase.logger.error(e.getMessage(), e);
            }

            moleculeBuffer.clear();
            CustomDatabase.logger.info("Try downloading compounds");
            try { //try to download fps for compound
                lookupAndAnnotateFingerprints(key2DToComp);
            } catch (Exception e) {
                CustomDatabase.logger.error(e.getMessage(), e);
            }

            // compound fps locally if not already downloaded or loaded from local db
            List<BasicJJob<Void>> jobs = key2DToComp.values().stream()
                    .map(c -> new BasicJJob<Void>() {
                        @Override
                        protected Void compute() throws Exception {
                            if (c.candidate == null) {
                                FingerprintCalculator fcalc = null;
                                try {
                                    fcalc = getFingerprintCalculator();
                                    addToBuffer(fcalc.computeCompound(c.molecule));
                                } finally {
                                    if (fcalc != null)
                                        freeFingerprinter.offer(fcalc);
                                }
                            } else {
                                annotateCandidate(c.molecule, c.candidate);
                                addToBuffer(c.candidate);
                            }
                            return null;
                        }
                    }).collect(Collectors.toList());

            List<BasicJJob<Void>> batches = SiriusJobs.getGlobalJobManager().submitJobsInBatches(jobs);

            jobs.forEach(j -> {
                try {
                    if (shutdown.get()) {
                        batches.forEach(JJob::cancel);
                        checkCancellation();
                    }
                    j.awaitResult();
                } catch (ExecutionException e) {
                    CustomDatabase.logger.error(e.getMessage(), e);
                }
            });

            for (Listener l : listeners) l.newMoleculeBufferSize(0);
        }
    }

    private void lookupAndAnnotateFingerprints(final ConcurrentHashMap<String, Comp> key2DToComp) throws IOException {
        Set<MolecularFormula> formulasToSearch = new HashSet<>();
        checkCancellation();
        try {
            for (Comp comp : key2DToComp.values()) {
                comp.candidate = database.database.getStorage() //todo do we need the fp here?
                        .findStr(Filter.where("inchiKey").eq(comp.key2D()), FingerprintCandidateWrapper.class, "fingerprint")
                        .findFirst()
                        .orElse(null);

                if (comp.candidate == null) //group by formula to reduce unnecessary downloads
                    formulasToSearch.add(InChIs.extractNeutralFormulaByAdjustingHsOrThrow(comp.inChI2D()));
                else
                    LoggerFactory.getLogger(getClass()).info(comp.inChI2D() + " already exists in db. Merging!");
            }
        } catch (UnknownElementException e) {
            throw new IOException(e);
        }

        checkCancellation();
        List<JJob<Boolean>> jobs = formulasToSearch.stream().map(formula -> new BasicJJob<Boolean>() {
            @Override
            protected Boolean compute() throws Exception {
                api.consumeStructureDB(DataSource.ALL.flag(), db -> {
                    List<FingerprintCandidate> cans = db.lookupStructuresAndFingerprintsByFormula(formula);
                    for (FingerprintCandidate can : cans) {
                        Comp toAdd = key2DToComp.get(can.getInchi().key2D());
                        if (toAdd != null) {
                            toAdd.candidate = FingerprintCandidateWrapper.of(formula, can);
                            CustomDatabase.logger.info(toAdd.candidate.getCandidate().getInchi().in2D + " downloaded");
                        }
                    }
                });
                return true;
            }
        }.asCPU()).collect(Collectors.toList());

        List<JJob<Boolean>> batches = SiriusJobs.getGlobalJobManager().submitJobsInBatches(jobs);

        jobs.forEach(j -> {
            try {
                if (shutdown.get()) {
                    batches.forEach(JJob::cancel);
                    checkCancellation();
                }
                j.awaitResult();
            } catch (ExecutionException e) {
                CustomDatabase.logger.error("Error during Download", e);
            }
        });
    }

    private FingerprintCalculator getFingerprintCalculator() {
        FingerprintCalculator calc = freeFingerprinter.poll();
        if (calc == null)
            calc = new FingerprintCalculator(dbname, fingerprintVersion);
        return calc;
    }

    private void addToBuffer(FingerprintCandidateWrapper fingerprintCandidate) throws IOException {
        synchronized (buffer) {
            buffer.add(fingerprintCandidate);
            for (Listener l : listeners) {
                l.newFingerprintBufferSize(buffer.size());
                l.newInChI(fingerprintCandidate.getCandidate().getInchi());
            }
            if (buffer.size() > bufferSize)
                flushBuffer();
        }
    }

    protected void flushBuffer() throws IOException {
        //todo flush buffer in background?
        flushMoleculeBuffer();
        final ArrayList<FingerprintCandidateWrapper> candidates;
        synchronized (buffer) {
            candidates = new ArrayList<>(buffer);
            buffer.clear();
        }
        database.database.getStorage().upsertAll(candidates);
        for (Listener l : listeners) l.newFingerprintBufferSize(buffer.size());
    }

    protected void annotateCandidate(@NotNull Molecule molecule, @NotNull FingerprintCandidateWrapper fcw) {
        CompoundCandidate fc = fcw.getCandidate();
        if (fc.getLinks() == null)
            fc.setLinks(new ArrayList<>(0));

        if (fc.getName() == null || fc.getName().isEmpty()) {
            if (molecule.name != null)
                fc.setName(molecule.name);
        }

        if (molecule.id != null) {
            if (fc.getName() == null || fc.getName().isEmpty())
                fc.setName(molecule.id);
            fc.getMutableLinks().add(new DBLink(dbname, molecule.id));
        } else {
            fc.getMutableLinks().add(new DBLink(dbname, ""));
        }
        fc.setBitset(fc.getBitset() | CustomDataSources.getSourceFromName(dbname).flag());
    }

    private void checkCancellation() {
        if (shutdown.get())
            throw new CancellationException("Import Cancelled");
    }


    // INNER CLASSES
    @Getter
    public static class Molecule {
        @NotNull
        private final InChI inchi;
        @NotNull
        private final Smiles smiles;
        private String id = null;
        private String name = null;
        @NotNull
        private IAtomContainer container;

        private Molecule(@NotNull IAtomContainer container, @NotNull Smiles smiles, @NotNull InChI inchi) {
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
        private final String dbname;
        private final FixedFingerprinter fingerprinter;
        private final LogPEstimator logPEstimator;

        public FingerprintCalculator(String dbname, CdkFingerprintVersion version) {
            this.dbname = dbname;
            this.fingerprinter = new FixedFingerprinter(version);
            this.logPEstimator = new LogPEstimator();
        }

        protected FingerprintCandidateWrapper computeCompound(Molecule molecule) throws CDKException, IllegalArgumentException, UnknownElementException {
            CustomDatabase.logger.info("Compute fingerprint for " + molecule.getInchi().in2D);
            final ArrayFingerprint fps = fingerprinter.computeFingerprintFromSMILES(molecule.smiles.smiles);

            final FingerprintCandidate fc = new FingerprintCandidate(molecule.getInchi(), fps);
            fc.setSmiles(molecule.smiles.smiles);

            if (molecule.name != null)
                fc.setName(molecule.name);

            if (molecule.id != null) {
                fc.setLinks(List.of(new DBLink(dbname, molecule.id)));
                if (fc.getName() == null || fc.getName().isEmpty())
                    fc.setName(molecule.id);//set id as name if no name was set
            } else {
                fc.setLinks(new ArrayList<>());
            }
            // compute XLOGP
            fc.setXlogp(logPEstimator.prepareMolAndComputeLogP(molecule.container));
            return FingerprintCandidateWrapper.of(fc);
        }
    }

    @FunctionalInterface
    public interface Listener {
        // informs about fingerprints that have to be computed
        default void newFingerprintBufferSize(int size) {
        }

        // informs about molecules that have to be parsed
        default void newMoleculeBufferSize(int size) {
        }

        default void newSpectraBufferSize(int size) {
        }

        // informs about imported molecule
        void newInChI(InChI inchi);
    }
}
