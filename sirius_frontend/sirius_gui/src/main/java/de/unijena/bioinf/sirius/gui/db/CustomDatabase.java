package de.unijena.bioinf.sirius.gui.db;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.stream.JsonWriter;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.BooleanFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.fingerid.Fingerprinter;
import de.unijena.bioinf.sirius.gui.fingerid.Compound;
import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import de.unijena.bioinf.sirius.gui.mainframe.Workspace;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class CustomDatabase implements SearchableDatabase {

    protected static Logger logger = LoggerFactory.getLogger(CustomDatabase.class);

    protected String name;
    protected File path;

    public static List<CustomDatabase> customDatabases() {
        final List<CustomDatabase> databases = new ArrayList<>();
        final File root = Workspace.CONFIG_STORAGE.getDatabaseDirectory();
        final File custom = new File(root, "custom");
        if (!custom.exists()) {
            return databases;
        }
        for (File subDir : custom.listFiles()) {
            if (subDir.isDirectory()) {
                databases.add(new CustomDatabase(subDir.getName(), subDir));
            }
        }
        return databases;
    }

    protected boolean deriveFromPubchem, deriveFromBioDb;
    protected CdkFingerprintVersion version = CdkFingerprintVersion.getDefault();

    public CustomDatabase(String name, File path) {
        this.name = name;
        this.path = path;
    }

    public void buildDatabase(List<File> files, ImporterListener listener) throws IOException, CDKException {
        final Importer importer = getImporter();
        final HashMap<String, Comp> dict = new HashMap<>(100);
        final InChIGeneratorFactory icf = InChIGeneratorFactory.getInstance();
        importer.init();
        importer.addListener(listener);
        for (File f : files) {
            importer.importFrom(f);
        }
        importer.flushBuffer();
    }

    private static class Comp {
        private String inchikey;
        private IAtomContainer molecule;
        private FingerprintCandidate candidate;

        public Comp(String inchikey) {
            this.inchikey = inchikey;
        }
    }

    public void inheritMetadata(File otherDb) throws IOException {
        // should be done automatically
    }

    public boolean isDeriveFromPubchem() {
        return deriveFromPubchem;
    }

    public void setDeriveFromPubchem(boolean deriveFromPubchem) {
        this.deriveFromPubchem = deriveFromPubchem;
    }

    public boolean isDeriveFromBioDb() {
        return deriveFromBioDb;
    }

    public void setDeriveFromBioDb(boolean deriveFromBioDb) {
        this.deriveFromBioDb = deriveFromBioDb;
    }

    protected void readSettings() throws IOException {
        if (settingsFile().exists()) {
            deriveFromPubchem = false;
            deriveFromBioDb = false;
            try (FileReader r = new FileReader(settingsFile())) {
                JsonObject o = Json.createReader(r).readObject();
                JsonArray ary = o.getJsonArray("inheritance");
                if (ary != null) {
                    for (JsonValue v : ary) {
                        if (v instanceof JsonString) {
                            final String s = ((JsonString)v).getString();
                            if (s.equals(DatasourceService.Sources.PUBCHEM.name))
                                deriveFromPubchem = true;
                            if (s.equals(DatasourceService.Sources.BIO.name))
                                deriveFromBioDb = true;
                        }
                    }
                }
                JsonArray fpAry = o.getJsonArray("fingerprintVersion");
                if (fpAry == null) {
                    this.version = CdkFingerprintVersion.getDefault();
                } else {
                    final List<CdkFingerprintVersion.USED_FINGERPRINTS> usedFingerprints = new ArrayList<>();
                    for (JsonValue v : fpAry) {
                        if (v instanceof JsonString) {
                            try {
                                usedFingerprints.add(CdkFingerprintVersion.USED_FINGERPRINTS.valueOf(((JsonString) v).getString().toUpperCase()));
                            } catch (IllegalArgumentException e) {
                                throw new RuntimeException("Unknown fingerprint type '" + ((JsonString) v).getString() + "'");
                            }
                        }
                    }
                    this.version = new CdkFingerprintVersion(usedFingerprints.toArray(new CdkFingerprintVersion.USED_FINGERPRINTS[usedFingerprints.size()]));
                }
            }
        }
    }

    public Importer getImporter() {
        return new Importer(this, version);
    }

    protected File settingsFile() {
        return new File(path, "settings.json");
    }

    public void setFingerprintVersion(CdkFingerprintVersion version) {
        this.version = version;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean searchInPubchem() {
        return deriveFromPubchem;
    }

    @Override
    public boolean searchInBio() {
        return deriveFromBioDb || deriveFromPubchem;
    }

    @Override
    public boolean isCustomDb() {
        return true;
    }

    @Override
    public File getDatabasePath() {
        return path;
    }

    public interface ImporterListener {
        // informs about fingerprints that have to be computed
        public void newFingerprintBufferSize(int size);
        // informs about molecules that have to be parsed
        public void newMoleculeBufferSize(int size);
        // informs about imported molecule
        public void newInChI(InChI inchi);
    }

    public static abstract class AbstractImporterListener implements ImporterListener {
        // informs about fingerprints that have to be computed
        public void newFingerprintBufferSize(int size) {

        }
        // informs about molecules that have to be parsed
        public void newMoleculeBufferSize(int size) {

        }
        // informs about imported molecule
        public void newInChI(InChI inchi) {

        }
    }

    public static class Importer {

        CustomDatabase database;
        File currentPath;
        List<ImporterListener> listeners = new ArrayList<>();

        // fingerprint buffer
        private List<FingerprintCandidate> buffer;

        // molecule buffer
        private List<IAtomContainer> moleculeBuffer;

        protected Fingerprinter fingerprinter;
        protected InChIGeneratorFactory inChIGeneratorFactory;
        protected SmilesGenerator smilesGen;
        protected SmilesParser smilesParser;
        protected CdkFingerprintVersion fingerprintVersion;

        protected Importer(CustomDatabase database, CdkFingerprintVersion version) {
            this.database = database;
            fingerprintVersion = version;
            this.buffer = new ArrayList<>();
            this.moleculeBuffer = new ArrayList<>();
            currentPath = database.path;
            if (currentPath==null) throw new NullPointerException();
            try {
                inChIGeneratorFactory = InChIGeneratorFactory.getInstance();
                smilesGen = SmilesGenerator.generic().aromatic();
                fingerprinter = Fingerprinter.getFor(version);
                        /*
                        // TODO: find a better solution
                        new Fingerprinter(Arrays.asList(new OpenBabelFingerprinter(), new SubstructureFingerprinter(), new FixedMACCSFingerprinter(), new PubchemFingerprinter(DefaultChemObjectBuilder.getInstance()), new KlekotaRothFingerprinter(), new ECFPFingerprinter()));
                        //new Fingerprinter();
                */
                smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
                smilesParser.kekulise(true);
            } catch (CDKException e) {
                throw new RuntimeException(e);
            }
        }

        public void addListener(ImporterListener listener) {
            listeners.add(listener);
        }
        public void removeListener(ImporterListener listener) {
            listeners.remove(listener);
        }

        @Deprecated
        public void collect(ImporterListener listener) {
            for (File f : currentPath.listFiles()) {
                if (!f.getName().endsWith("json.gz")) continue;
                synchronized(this) {
                    try {
                        try (final JsonReader parser = Json.createReader(new GZIPInputStream(new FileInputStream(f)))) {
                            final JsonArray ary = parser.readObject().getJsonArray("compounds");
                            for (int k=0; k < ary.size(); ++k) {
                                listener.newInChI(CompoundCandidate.fromJSON(ary.getJsonObject(k)).getInchi());
                            }
                        }
                    } catch (IOException e) {
                        LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
                    }
                }
            }
        }

        protected void init() {
            if (!currentPath.exists()) {
                currentPath.mkdirs();
                try {
                    writeSettings();
                } catch (IOException e) {
                    LoggerFactory.getLogger(this.getClass()).error(e.getMessage(),e);
                }
            }
        }

        public void importFromString(String str) throws IOException {
            importFromString(str, null);
        }

        public void importFromString(String str, String id) throws IOException {
            if (str.startsWith("InChI")) {
                // try InChI parser
                try {
                    final IAtomContainer molecule = inChIGeneratorFactory.getInChIToStructure(str, SilentChemObjectBuilder.getInstance()).getAtomContainer();
                    if (id!=null) molecule.setID(id);
                    addMolecule(molecule);
                } catch (CDKException e) {
                    throw new IOException(e);
                }
            } else {
                // try SMILES parser
                try {
                    final IAtomContainer molecule = smilesParser.parseSmiles(str);
                    if (id!=null) molecule.setID(id);
                    addMolecule(molecule);
                } catch (CDKException e) {
                    throw new IOException(e);
                }
            }
        }

        protected void importFrom(File file) throws IOException {
            ReaderFactory factory = new ReaderFactory();
            ISimpleChemObjectReader reader = null;
            try (InputStream stream = new FileInputStream(file)) {
                reader = factory.createReader(stream);
            }
            if (reader != null) {
                try (InputStream stream = new FileInputStream(file)) {
                    try {
                        reader.setReader(stream);
                        IChemModel model = SilentChemObjectBuilder.getInstance().newInstance(IChemModel.class);
                        model = reader.read(model);
                        for (IAtomContainer molecule : model.getMoleculeSet().atomContainers()) {
                            addMolecule(molecule);
                        }
                    } catch (CDKException e) {
                        throw new IOException(e);
                    }
                }
            } else {
                // check for SMILES and InChI formats
                final BufferedReader br = new BufferedReader(new FileReader(file));
                String line = br.readLine();
                br.close();
                final List<IAtomContainer> mols = new ArrayList<>();
                if (line==null) {
                    throw new IOException("Unknown file format: " + file.getName());
                } else if (line.contains("InChI")) {
                    for (String aline : Files.readAllLines(file.toPath(), Charset.forName("UTF-8"))) {
                        aline = aline.trim();
                        if (aline.isEmpty()) continue;
                        String id = null;
                        final int index = aline.indexOf('\t');
                        if (index >= 0) {
                            String[] parts = aline.split("\t");
                            aline = parts[0];
                            id = parts[1];
                        }
                        try {
                            final IAtomContainer mol = (inChIGeneratorFactory.getInChIToStructure(aline, SilentChemObjectBuilder.getInstance()).getAtomContainer());
                            if (id!=null) mol.setID(id);
                            addMolecule(mol);
                        } catch (CDKException e) {
                            logger.error(e.getMessage(),e);
                        }


                    }
                } else {
                    try {
                        smilesParser.parseSmiles(line);
                        for (String aline : Files.readAllLines(file.toPath(), Charset.forName("UTF-8"))) {aline = aline.trim();
                            if (aline.isEmpty()) continue;
                            String id = null;
                            final int index = aline.indexOf('\t');
                            if (index >= 0) {
                                String[] parts = aline.split("\t");
                                aline = parts[0];
                                id = parts[1];
                            }
                            try {final IAtomContainer mol = smilesParser.parseSmiles(aline);
                                if (id!=null) mol.setID(id);
                                addMolecule(mol);} catch (CDKException e) {
                                logger.error(e.getMessage(),e);
                            }
                        }
                    } catch (InvalidSmilesException e) {
                        throw new IOException("Unknown file format: " + file.getName());
                    }
                }
            }
        }

        protected void addMolecule(IAtomContainer mol) throws IOException {
            moleculeBuffer.add(mol);
            for (ImporterListener l : listeners) l.newMoleculeBufferSize(moleculeBuffer.size());
            if (moleculeBuffer.size()> 1000) {
                flushMoleculeBuffer();
            }
        }

        private void flushMoleculeBuffer() throws IOException {
            // start downloading
            final HashMap<String, Comp> dict = new HashMap<>(moleculeBuffer.size());
            try {
                final InChIGeneratorFactory icf = InChIGeneratorFactory.getInstance();
                for (IAtomContainer c : moleculeBuffer) {
                    System.err.println(SmilesGenerator.unique().create(c));
                    final String key;
                    try {
                        key = icf.getInChIGenerator(c).getInchiKey().substring(0, 14);
                        Comp comp = new Comp(key);
                        comp.molecule = c;
                        dict.put(key, comp);
                    } catch (CDKException | IllegalArgumentException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            } catch (CDKException | IllegalArgumentException e) {
                logger.error(e.getMessage(), e);
            }
            logger.info("Try downloading compounds");
            try (final WebAPI webAPI = new WebAPI()){
                try (final RESTDatabase db = webAPI.getRESTDb(BioFilter.ALL, new File("."))) {
                    try {
                        for (FingerprintCandidate fc : db.lookupManyFingerprintsByInchis(dict.keySet())) {
                            logger.info(fc.getInchiKey2D() + " downloaded");
                            dict.get(fc.getInchiKey2D()).candidate = fc;
                        }
                    } catch (DatabaseException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            for (Comp c : dict.values()) {
                try {
                    addToBuffer(computeCompound(c.molecule, c.molecule.getID(), c.candidate));
                } catch (CDKException | IllegalArgumentException e) {
                    logger.error(e.getMessage(),e);
                }
            }
            moleculeBuffer.clear();
            for (ImporterListener l : listeners) l.newMoleculeBufferSize(0);
        }

        private void addToBuffer(FingerprintCandidate fingerprintCandidate) throws IOException {
            buffer.add(fingerprintCandidate);
            for (ImporterListener l : listeners) l.newFingerprintBufferSize(buffer.size());
            if (buffer.size() > 10000)
                flushBuffer();
        }

        protected FingerprintCandidate computeCompound(IAtomContainer molecule, String optionalName, FingerprintCandidate fc) throws CDKException {
            if (fc==null) return computeCompound(molecule,optionalName);
            logger.info("download fingerprint " + fc.getInchiKey2D());
            if (fc.getLinks()==null) fc.setLinks(new DBLink[0]);
            if (optionalName!=null) {
                fc.setName(optionalName);
                DBLink[] ls = Arrays.copyOf(fc.getLinks(), fc.getLinks().length+1);
                ls[ls.length-1] = new DBLink(database.name, optionalName);
                fc.setLinks(ls);
            } else {
                DBLink[] ls = Arrays.copyOf(fc.getLinks(), fc.getLinks().length+1);
                ls[ls.length-1] = new DBLink(database.name, "");
                fc.setLinks(ls);
            }
            fc.setBitset(fc.getBitset() | DatasourceService.Sources.CUSTOM.flag);
            synchronized (buffer){
                buffer.add(fc);
            }
            return fc;
        }

        protected FingerprintCandidate computeCompound(IAtomContainer molecule, String optionalName) throws CDKException, IllegalArgumentException {
            InChIGenerator gen = inChIGeneratorFactory.getInChIGenerator(molecule);
            final InChI inchi = new InChI(gen.getInchiKey(), gen.getInchi());
            molecule = inChIGeneratorFactory.getInChIToStructure(inchi.in2D, SilentChemObjectBuilder.getInstance()).getAtomContainer();
            final boolean[] fps = fingerprinter.fingerprintsToBooleans(fingerprinter.computeFingerprints(molecule));
            logger.info("compute fingerprint " + inchi.key2D());
            final Fingerprint fp = new BooleanFingerprint(fingerprintVersion, fps).asArray();
            final String smiles = smilesGen.create(molecule);

            final FingerprintCandidate fc = new FingerprintCandidate(inchi, fp);
            fc.setSmiles(smiles);
            if (optionalName!=null) {
                fc.setName(optionalName);
                fc.setLinks(new DBLink[]{new DBLink(database.name, optionalName)});
            } else {
                fc.setLinks(new DBLink[0]);
            }
            fc.setBitset(DatasourceService.Sources.CUSTOM.flag);
            synchronized (buffer){
                buffer.add(fc);
            }
            return fc;
        }

        protected synchronized void flushBuffer() throws IOException {
            final ArrayList<FingerprintCandidate> candidates;
            synchronized (buffer) {
                candidates = new ArrayList<>(buffer);
                buffer.clear();
            }
            final Multimap<MolecularFormula, FingerprintCandidate> candidatePerFormula = ArrayListMultimap.create();
            for (FingerprintCandidate fc : candidates) {
                candidatePerFormula.put(fc.getInchi().extractFormula(), fc);
            }
            for (Map.Entry<MolecularFormula, Collection<FingerprintCandidate>> entry : candidatePerFormula.asMap().entrySet()) {
                mergeCompounds(entry.getKey(), entry.getValue());
            }
            for (ImporterListener l : listeners) l.newFingerprintBufferSize(buffer.size());

        }

        private void mergeCompounds(MolecularFormula key, Collection<FingerprintCandidate> value) throws IOException {
            final File file = new File(database.path, key.toString() + ".json.gz");
            try {
            List<FingerprintCandidate> candidates = new ArrayList<>();
            candidates.addAll(value);
            Compound.merge(fingerprintVersion, candidates, file);
            } catch (IOException | JsonException e) {
                throw new IOException("Error while merging into " + file, e);
            }
        }


        public void writeSettings() throws IOException {
            try (final JsonWriter writer = new JsonWriter(new FileWriter(database.settingsFile()))) {
                writer.beginObject();
                writer.name("inheritance");
                writer.beginArray();
                if (database.deriveFromBioDb) writer.value(DatasourceService.Sources.BIO.name);
                if (database.deriveFromPubchem) writer.value(DatasourceService.Sources.PUBCHEM.name);
                writer.endArray();
                writer.name("fingerprintVersion");
                writer.beginArray();
                for (int t=0; t < fingerprintVersion.numberOfFingerprintTypesInUse(); ++t) {
                    writer.value(fingerprintVersion.getFingerprintTypeAt(t).name());
                }
                writer.endArray();
                writer.endObject();
            }
        }

        public synchronized void deleteDatabase() {
            for (File f : currentPath.listFiles()) {
                f.delete();
            }
            currentPath.delete();
        }
    }

    @Override
    public String toString() {
        return name;
    }

}
