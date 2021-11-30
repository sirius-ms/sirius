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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.fingerid.fingerprints.FixedFingerprinter;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.storage.blob.file.FileBlobStorage;
import de.unijena.bioinf.webapi.WebAPI;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
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

import javax.json.JsonException;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CustomDatabaseImporter {
    final CustomDatabase<?> database;
    Queue<Listener> listeners = new ConcurrentLinkedQueue<>();

    // fingerprint buffer
    private final List<FingerprintCandidate> buffer;
    private final int bufferSize;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    // molecule buffer
    private final List<Molecule> moleculeBuffer;
    private static final int molBufferSize = 1000;

    final protected ConcurrentLinkedQueue<FingerprintCalculator> freeFingerprinter = new ConcurrentLinkedQueue<>();
    protected InChIGeneratorFactory inChIGeneratorFactory;
    protected SmilesGenerator smilesGen;
    protected SmilesParser smilesParser;
    protected CdkFingerprintVersion fingerprintVersion;
    protected final WebAPI<?> api;

    protected CustomDatabaseImporter(@NotNull CustomDatabase<?> database, CdkFingerprintVersion version, WebAPI<?> api, int bufferSize) {
        this.api = api;
        this.database = database;
        this.fingerprintVersion = version;
        this.bufferSize = bufferSize;
        this.buffer = new ArrayList<>((int) (this.bufferSize * 1.25));
        this.moleculeBuffer = new ArrayList<>((int) (molBufferSize * 1.25));
        try {
            inChIGeneratorFactory = InChIGeneratorFactory.getInstance();
            smilesGen = SmilesGenerator.generic().aromatic();
            smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            smilesParser.kekulise(true);
        } catch (CDKException e) {
            throw new RuntimeException(e);
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

    public void importFromString(String str) throws IOException, CDKException {
        importFromString(str, null, null);
    }

    public void importFromString(String str, String id, String name) throws IOException, CDKException {
        final Molecule molecule;
        if (InChIs.isInchi(str)) {
            if (!InChIs.isConnected(str)) {
                LoggerFactory.getLogger(getClass()).warn(
                        String.format("Compound '%s' is Not connected! Only connected structures are supported! Skipping.", str));
                return;
            }

            if (InChIs.isMultipleCharged(str)) {
                LoggerFactory.getLogger(getClass()).warn(
                        String.format("Compound '%s' is multiple charged! Only neutral or single charged compounds are supported! Skipping.", str));
                return;
            }


            molecule = new Molecule(inChIGeneratorFactory.getInChIToStructure(str, SilentChemObjectBuilder.getInstance()).getAtomContainer());
        } else {
            if (!SmilesU.isConnected(str)) {
                LoggerFactory.getLogger(getClass()).warn(
                        String.format("Compound '%s' is Not connected! Only connected structures are supported! Skipping.", str));
                return;
            }

            if (SmilesU.isMultipleCharged(str)) {
                LoggerFactory.getLogger(getClass()).warn(
                        String.format("Compound '%s' is multiple charged! Only neutral or single charged compounds are supported! Skipping.", str));
                return;
            }

            molecule = new Molecule(smilesParser.parseSmiles(str));
            molecule.smiles = new Smiles(str);
        }
        molecule.id = id;
        molecule.name = name;
        addMolecule(molecule);
    }

    public void importFrom(File file) throws IOException {
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
                                addMolecule(new Molecule(c));
                            }
                        }
                    }
                } catch (CDKException e) {
                    throw new IOException(e);
                }
            }
        } else {
            // checkConnectionToUrl for SMILES and InChI formats
            try (final BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    checkCancellation();
                    //skip empty lines
                    if (!line.isBlank()) {
                        String[] parts = line.split("\t");
                        final String structure = parts[0].trim();

                        final String id = parts.length > 1 ? parts[1] : null;
                        final String name = parts.length > 2 ? parts[2] : null;

                        try {
                            importFromString(structure, id, name);
                        } catch (CDKException e) {
                            CustomDatabase.logger.error(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }


    protected void addMolecule(Molecule mol) throws IOException {
        synchronized (moleculeBuffer) {
            moleculeBuffer.add(mol);
            for (Listener l : listeners) l.newMoleculeBufferSize(moleculeBuffer.size());
            if (moleculeBuffer.size() > molBufferSize) {
                flushMoleculeBuffer();
            }
        }
    }

    private void flushMoleculeBuffer() throws IOException {
        // start downloading
        if (moleculeBuffer.size() > 0) {
            final ConcurrentHashMap<String, Comp> dict = new ConcurrentHashMap<>(moleculeBuffer.size());
            try {
                final InChIGeneratorFactory icf = InChIGeneratorFactory.getInstance();
                for (Molecule c : moleculeBuffer) {
                    checkCancellation();
                    final String inchi2d;
                    try {
                        inchi2d = InChIs.inchi2d(icf.getInChIGenerator(c.container).getInchi());
                        Comp comp = new Comp(inchi2d);
                        comp.molecule = c;
                        dict.put(inchi2d, comp);
                    } catch (CDKException | IllegalArgumentException e) {
                        CustomDatabase.logger.error(e.getMessage(), e);
                    }
                }
            } catch (CDKException | IllegalArgumentException e) {
                CustomDatabase.logger.error(e.getMessage(), e);
            }
            moleculeBuffer.clear();
            CustomDatabase.logger.info("Try downloading compounds");
            try {
                lookupAndAnnotateFingerprints(dict);
            } catch (Exception e) {
                CustomDatabase.logger.error(e.getMessage(), e);
            }

            List<BasicJJob<FingerprintCandidate>> jobs = dict.values().stream().map(c -> new BasicJJob<FingerprintCandidate>() {
                @Override
                protected FingerprintCandidate compute() throws Exception {
                    FingerprintCalculator fcalc = null;
                    try {
                        fcalc = getFingerprintCalculator();
                        FingerprintCandidate fc = fcalc.computeCompound(c.molecule, c.candidate);
                        addToBuffer(fc);
                        return fc;
                    } finally {
                        if (fcalc != null)
                            freeFingerprinter.offer(fcalc);
                    }
                }
            }).collect(Collectors.toList());

            List<BasicJJob<FingerprintCandidate>> batches = SiriusJobs.getGlobalJobManager().submitJobsInBatches(jobs);

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

    private void lookupAndAnnotateFingerprints(final ConcurrentHashMap<String, Comp> dict) throws IOException {
        Set<MolecularFormula> formulasToSearch = new HashSet<>();
        checkCancellation();
        try {
            for (String in : dict.keySet())
                formulasToSearch.add(InChIs.extractFormula(in));
        } catch (UnknownElementException e) {
            throw new IOException(e);
        }

        checkCancellation();
        List<JJob<Boolean>> jobs = formulasToSearch.stream().map(formula -> new BasicJJob<Boolean>() {
            @Override
            protected Boolean compute() throws Exception {
                api.consumeStructureDB(DataSource.ALL.flag(), new FileBlobStorage(SearchableDatabases.getWebDatabaseCacheDirectory()), db -> {
                    List<FingerprintCandidate> cans = db.lookupStructuresAndFingerprintsByFormula(formula);
                    for (FingerprintCandidate can : cans) {
                        Comp toAdd = dict.get(can.getInchi().in2D);
                        if (toAdd != null) {
                            toAdd.candidate = can;
                            CustomDatabase.logger.info(toAdd.candidate.getInchi().in2D + " downloaded");
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
            calc = new FingerprintCalculator(database.name(), fingerprintVersion);
        return calc;
    }

    private void addToBuffer(FingerprintCandidate fingerprintCandidate) throws IOException {
        synchronized (buffer) {
            buffer.add(fingerprintCandidate);
            for (Listener l : listeners) {
                l.newFingerprintBufferSize(buffer.size());
                l.newInChI(fingerprintCandidate.getInchi());
            }
            if (buffer.size() > bufferSize)
                flushBuffer();
        }
    }

    public void flushBuffer() throws IOException {
        flushMoleculeBuffer();
        final ArrayList<FingerprintCandidate> candidates;
        synchronized (buffer) {
            candidates = new ArrayList<>(buffer);
            buffer.clear();
        }
        synchronized (database) {
            final Multimap<MolecularFormula, FingerprintCandidate> candidatePerFormula = ArrayListMultimap.create();
            for (FingerprintCandidate fc : candidates) {
                candidatePerFormula.put(fc.getInchi().extractFormulaOrThrow(), fc);
            }
            for (Map.Entry<MolecularFormula, Collection<FingerprintCandidate>> entry : candidatePerFormula.asMap().entrySet()) {
                mergeCompounds(entry.getKey(), entry.getValue());
            }
            for (Listener l : listeners) l.newFingerprintBufferSize(buffer.size());
            database.writeSettings();
        }

    }

    private void mergeCompounds(MolecularFormula key, Collection<FingerprintCandidate> value) throws IOException {
        Path path = Path.of(key.toString() + ".json)");
        try {
            synchronized (database) {
                List<FingerprintCandidate> candidates = new ArrayList<>(value);
                try (InputStream in = database.storage.reader(path)) {
                    candidates.addAll(JSONReader.fromJSONList(fingerprintVersion, in));
                }
                candidates = WebWithCustomDatabase.mergeCompounds(candidates);
                database.getStatistics().compounds().addAndGet(candidates.size() - value.size());
            }
        } catch (IOException | JsonException e) {
            throw new IOException("Error while merging into: " + path, e);
        }
    }

    private void checkCancellation() {
        if (shutdown.get())
            throw new CancellationException("Import Cancelled");
    }


    // INNER CLASSES
    static class Molecule {
        Smiles smiles = null;
        String id = null;
        String name = null;
        @NotNull IAtomContainer container;

        Molecule(Smiles smiles, @NotNull AtomContainer container) {
            this.smiles = smiles;
            this.container = container;
        }

        Molecule(@NotNull IAtomContainer container) {
            this.container = container;
        }
    }

    static class Comp {
        String inchikey;
        Molecule molecule;
        FingerprintCandidate candidate;

        Comp(String inchikey) {
            this.inchikey = inchikey;
        }
    }

    private class FingerprintCalculator {
        private final String dbname;
        private final FixedFingerprinter fingerprinter;
        private final LogPEstimator logPEstimator;

        public FingerprintCalculator(String dbname, CdkFingerprintVersion version) {
            this.dbname = dbname;
            this.fingerprinter = new FixedFingerprinter(version);
            this.logPEstimator = new LogPEstimator();
        }

        protected FingerprintCandidate computeCompound(Molecule molecule, FingerprintCandidate fc) throws CDKException, IOException {
            if (fc == null)
                return computeCompound(molecule);

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
            return fc;
        }

        protected FingerprintCandidate computeCompound(Molecule molecule) throws CDKException, IllegalArgumentException {
            InChIGenerator gen = inChIGeneratorFactory.getInChIGenerator(molecule.container);
            final InChI inchi = InChIs.newInChI(gen.getInchiKey(), gen.getInchi());


            if (molecule.smiles == null) {
                LoggerFactory.getLogger(getClass()).warn("Computing fingerprint from non smiles input. NO standardization has happened!");
                //eliminate 3d info to have a minial amount of standardization.
                molecule.container = inChIGeneratorFactory.getInChIToStructure(inchi.in2D, SilentChemObjectBuilder.getInstance()).getAtomContainer();
                molecule.smiles = new Smiles(smilesGen.create(molecule.container));
            }

            CustomDatabase.logger.info("Compute fingerprint for " + inchi.in2D);
            final ArrayFingerprint fps = fingerprinter.computeFingerprintFromSMILES(molecule.smiles.smiles);

            final FingerprintCandidate fc = new FingerprintCandidate(inchi, fps);
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
            return fc;
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

        // informs about imported molecule
        void newInChI(InChI inchi);
    }
}
