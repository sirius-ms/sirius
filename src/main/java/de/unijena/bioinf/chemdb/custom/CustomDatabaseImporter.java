package de.unijena.bioinf.chemdb.custom;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.stream.JsonWriter;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.InChIs;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.WebAPI;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.fingerid.fingerprints.FixedFingerprinter;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonReader;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class CustomDatabaseImporter {
    final CustomDatabase database;
    File currentPath;
    List<Listener> listeners = new ArrayList<>();

    // fingerprint buffer
    private final List<FingerprintCandidate> buffer;
    private final int bufferSize;


    // molecule buffer
    private final List<CustomDatabase.Molecule> moleculeBuffer;
    private static final int molBufferSize = 1000;

    final protected ConcurrentLinkedQueue<FingerprintCalculator> freeFingerprinter = new ConcurrentLinkedQueue<>();
    protected InChIGeneratorFactory inChIGeneratorFactory;
    protected SmilesGenerator smilesGen;
    protected SmilesParser smilesParser;
    protected CdkFingerprintVersion fingerprintVersion;
    protected final WebAPI api;

    protected CustomDatabaseImporter(CustomDatabase database, CdkFingerprintVersion version, WebAPI api, int bufferSize) {
        this.api = api;
        this.database = database;
        this.fingerprintVersion = version;
        this.bufferSize = bufferSize;
        this.buffer = new ArrayList<>((int) (this.bufferSize * 1.25));
        this.moleculeBuffer = new ArrayList<>((int) (molBufferSize * 1.25));
        this.currentPath = database.path;
        if (currentPath == null) throw new NullPointerException();
        try {
            inChIGeneratorFactory = InChIGeneratorFactory.getInstance();
            smilesGen = SmilesGenerator.generic().aromatic();
            smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            smilesParser.kekulise(true);
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    @Deprecated
    public void collect(Listener listener) {
        for (File f : currentPath.listFiles()) {
            if (!f.getName().endsWith("json.gz")) continue;
            synchronized (this) {
                try {
                    try (final JsonReader parser = Json.createReader(new GZIPInputStream(new FileInputStream(f)))) {
                        final JsonArray ary = parser.readObject().getJsonArray("compounds");
                        for (int k = 0; k < ary.size(); ++k) {
                            listener.newInChI(CompoundCandidate.fromJSON(ary.getJsonObject(k)).getInchi());
                        }
                    }
                } catch (IOException e) {
                    LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
                }
            }
        }
    }

    public void init() {
        if (!currentPath.exists()) {
            currentPath.mkdirs();
            try {
                writeSettings();
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            }
        } else {
            try {
                database.readSettings();
            } catch (IOException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            }
        }
    }

    public void importFromString(String str) throws IOException, CDKException {
        importFromString(str, null, null);
    }

    public void importFromString(String str, String id, String name) throws IOException, CDKException {
        final CustomDatabase.Molecule molecule;
        if (str.startsWith("InChI")) {
            molecule = new CustomDatabase.Molecule(inChIGeneratorFactory.getInChIToStructure(str, SilentChemObjectBuilder.getInstance()).getAtomContainer());
        } else {
            molecule = new CustomDatabase.Molecule(smilesParser.parseSmiles(str));
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
                                addMolecule(new CustomDatabase.Molecule(c));
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


    protected void addMolecule(CustomDatabase.Molecule mol) throws IOException {
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
            final HashMap<String, CustomDatabase.Comp> dict = new HashMap<>(moleculeBuffer.size());
            try {
                final InChIGeneratorFactory icf = InChIGeneratorFactory.getInstance();
                for (CustomDatabase.Molecule c : moleculeBuffer) {
                    final String key;
                    try {
                        key = icf.getInChIGenerator(c.container).getInchiKey().substring(0, 14);
                        CustomDatabase.Comp comp = new CustomDatabase.Comp(key);
                        comp.molecule = c;
                        dict.put(key, comp);
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
                try (final RESTDatabase db = api.getRESTDb(BioFilter.ALL, new File("."))) {
                    try {
                        for (FingerprintCandidate fc : db.lookupManyFingerprintsByInchis(dict.keySet())) {
                            CustomDatabase.logger.info(fc.getInchiKey2D() + " downloaded");
                            dict.get(fc.getInchiKey2D()).candidate = fc;
                        }
                    } catch (ChemicalDatabaseException e) {
                        CustomDatabase.logger.error(e.getMessage(), e);
                    }
                }
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

            SiriusJobs.getGlobalJobManager().submitJobsInBatches(jobs).forEach(j -> {
                try {
                    j.awaitResult();
                } catch (ExecutionException e) {
                    CustomDatabase.logger.error(e.getMessage(), e);
                }
            });
            for (Listener l : listeners) l.newMoleculeBufferSize(0);
        }
    }

    private FingerprintCalculator getFingerprintCalculator() {
        FingerprintCalculator calc = freeFingerprinter.poll();
        if (calc == null)
            calc = new FingerprintCalculator(database.name, fingerprintVersion);
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


    private class FingerprintCalculator {
        private final String dbname;
        private final FixedFingerprinter fingerprinter;
        private final LogPEstimator logPEstimator;

        public FingerprintCalculator(String dbname, CdkFingerprintVersion version) {
            this.dbname = dbname;
            this.fingerprinter = new FixedFingerprinter(version);
            this.logPEstimator = new LogPEstimator();
        }

        protected FingerprintCandidate computeCompound(CustomDatabase.Molecule molecule, FingerprintCandidate fc) throws CDKException, IOException {
            if (fc == null)
                return computeCompound(molecule);

            if (fc.getLinks() == null)
                fc.setLinks(new DBLink[0]);

            if (fc.getName() == null || fc.getName().isEmpty()) {
                if (molecule.name != null)
                    fc.setName(molecule.name);
            }

            if (molecule.id != null) {
                if (fc.getName() == null || fc.getName().isEmpty())
                    fc.setName(molecule.id);
                DBLink[] ls = Arrays.copyOf(fc.getLinks(), fc.getLinks().length + 1);
                ls[ls.length - 1] = new DBLink(dbname, molecule.id);
                fc.setLinks(ls);
            } else {
                DBLink[] ls = Arrays.copyOf(fc.getLinks(), fc.getLinks().length + 1);
                ls[ls.length - 1] = new DBLink(dbname, "");
                fc.setLinks(ls);
            }
            fc.setBitset(fc.getBitset() | CustomDataSourceService.getSourceFromName(dbname).flag());
            return fc;
        }

        protected FingerprintCandidate computeCompound(CustomDatabase.Molecule molecule) throws CDKException, IllegalArgumentException {
            InChIGenerator gen = inChIGeneratorFactory.getInChIGenerator(molecule.container);
            final InChI inchi = InChIs.newInChI(gen.getInchiKey(), gen.getInchi());


            if (molecule.smiles == null) {
                LoggerFactory.getLogger(getClass()).warn("Computing fingerprint from non smiles input. NO standardization has happened!");
                //eliminate 3d info to have a minial amount of standardization.
                molecule.container = inChIGeneratorFactory.getInChIToStructure(inchi.in2D, SilentChemObjectBuilder.getInstance()).getAtomContainer();
                molecule.smiles = new Smiles(smilesGen.create(molecule.container));
            }

            CustomDatabase.logger.info("Compute fingerprint for " + inchi.key2D());
            final ArrayFingerprint fps = fingerprinter.computeFingerprintFromSMILES(molecule.smiles.smiles);

            final FingerprintCandidate fc = new FingerprintCandidate(inchi, fps);
            fc.setSmiles(molecule.smiles.smiles);

            if (molecule.name != null)
                fc.setName(molecule.name);

            if (molecule.id != null) {
                fc.setLinks(new DBLink[]{new DBLink(dbname, molecule.id)});
                if (fc.getName() == null || fc.getName().isEmpty())
                    fc.setName(molecule.id);//set id as name if no name was set
            } else {
                fc.setLinks(new DBLink[0]);
            }
            fc.setBitset(CustomDataSourceService.getSourceFromName(dbname).flag());
            // compute XLOGP
            fc.setXlogp(logPEstimator.prepareMolAndComputeLogP(molecule.container));
            return fc;
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
            writeSettings();
        }

    }

    private void mergeCompounds(MolecularFormula key, Collection<FingerprintCandidate> value) throws IOException {
        final File file = new File(database.path, key.toString() + ".json.gz");
        try {
            List<FingerprintCandidate> candidates = new ArrayList<>();
            candidates.addAll(value);
            synchronized (database) {
                database.numberOfCompounds += FingerprintCandidate.mergeFromJsonToJson(fingerprintVersion, candidates, file);
            }
        } catch (IOException | JsonException e) {
            throw new IOException("Error while merging into " + file, e);
        }
    }


    public void writeSettings() throws IOException {
        synchronized (database) {
            try (final JsonWriter writer = new JsonWriter(new FileWriter(database.settingsFile()))) {
                writer.beginObject();
                writer.name("inheritance");
                writer.beginArray();
                if (database.deriveFromBioDb) writer.value(DataSource.BIO.realName);
                if (database.deriveFromPubchem) writer.value(DataSource.PUBCHEM.realName);
                writer.endArray();
                writer.name("fingerprintVersion");
                writer.beginArray();
                for (int t = 0; t < fingerprintVersion.numberOfFingerprintTypesInUse(); ++t) {
                    writer.value(fingerprintVersion.getFingerprintTypeAt(t).name());
                }
                writer.endArray();

                writer.name("schemaVersion");
                writer.value(VersionsInfo.CUSTOM_DATABASE_SCHEMA);
                writer.name("statistics");
                writer.beginObject();
                writer.name("compounds");
                writer.value(database.numberOfCompounds);
                writer.endObject();
                writer.endObject();
            }
        }
    }

    public static void importDatabaseFromStrings(String dbPath, List<String> files, WebAPI api, int bufferSize) {
        importDatabase(dbPath, files.stream().map(File::new).collect(Collectors.toList()), api, bufferSize);
    }

    public static void importDatabase(String dbPath, List<File> files, WebAPI api, int bufferSize) {
        importDatabase(new File(dbPath), files, api, bufferSize);
    }

    public static void importDatabase(File dbPath, List<File> files, WebAPI api, int bufferSize) {
        importDatabase(dbPath, files, false, false, api, bufferSize);
    }

    public static void importDatabase(File dbPath, List<File> files, boolean derivePubChem, boolean deriveBio, WebAPI api, int bufferSize) {
        final Logger log = LoggerFactory.getLogger(CustomDatabaseImporter.class);
        try {
            final CustomDatabase db = CustomDatabase.createNewDatabase(dbPath.getName(), dbPath, api.getCDKChemDBFingerprintVersion());
            db.setDeriveFromPubchem(derivePubChem);
            db.setDeriveFromBioDb(deriveBio);
            db.buildDatabase(files, inchi -> log.debug(inchi.in2D + " imported"), api, bufferSize);
        } catch (IOException | CDKException e) {
            LoggerFactory.getLogger(CustomDatabaseImporter.class).error("Error during database import!", e);
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
