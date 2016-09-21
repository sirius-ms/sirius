package de.unijena.bioinf.sirius.gui.db;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.stream.JsonWriter;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.BooleanFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.DBLink;
import de.unijena.bioinf.chemdb.DatasourceService;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.Fingerprinter;
import de.unijena.bioinf.sirius.gui.fingerid.CSIFingerIdComputation;
import de.unijena.bioinf.sirius.gui.fingerid.Compound;
import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.io.FormatFactory;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.io.formats.IChemFormat;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.smiles.smarts.parser.SMARTSParser;
import org.slf4j.LoggerFactory;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CustomDatabase {

    protected String name;
    protected File path;

    protected boolean deriveFromPubchem, deriveFromBioDb;

    public CustomDatabase(String name, File path) {
        this.name = name;
        this.path = path;
    }

    public void buildDatabase(List<File> files, CompoundImportedListener listener) throws IOException, CDKException {
        final Importer importer = getImporter();
        importer.init();
        for (File f : files) {
            final List<IAtomContainer> molecules = importer.importFrom(f);
            for (IAtomContainer mol : molecules) {
                listener.compoundImported(importer.importCompound(mol, mol.getID()));
            }
            if (molecules.size()>1000) {
                importer.flushBuffer();
            }
        }
        importer.flushBuffer();
    }

    public void inheritMetadata(File otherDb) throws IOException {
        getImporter().inheritMetadata(otherDb);
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
            }
        }
    }

    public Importer getImporter() {
        return new Importer(this);
    }

    protected File settingsFile() {
        return new File(path, "settings.json");
    }

    public static class Importer {

        CustomDatabase database;
        File currentPath;

        private List<FingerprintCandidate> buffer;

        protected Fingerprinter fingerprinter;
        protected InChIGeneratorFactory inChIGeneratorFactory;
        protected SmilesGenerator smilesGen;
        protected SmilesParser smilesParser;

        protected Importer(CustomDatabase database) {
            this.database = database;
            this.buffer = new ArrayList<>();
            currentPath = database.path;
            if (currentPath==null) throw new NullPointerException();
            try {
                inChIGeneratorFactory = InChIGeneratorFactory.getInstance();
                smilesGen = SmilesGenerator.generic().aromatic();
                fingerprinter = new Fingerprinter();
                smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
                smilesParser.kekulise(true);
            } catch (CDKException e) {
                throw new RuntimeException(e);
            }
        }

        public void collect(CompoundImportedListener listener) {
            for (File f : currentPath.listFiles()) {
                if (!f.getName().endsWith("json.gz")) continue;
                synchronized(this) {
                    try {
                        try (final JsonReader parser = Json.createReader(new GZIPInputStream(new FileInputStream(f)))) {
                            final JsonArray ary = parser.readObject().getJsonArray("compounds");
                            for (int k=0; k < ary.size(); ++k) {
                                listener.compoundImported(CompoundCandidate.fromJSON(ary.getJsonObject(k)).getInchi());
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

        protected List<IAtomContainer> importFrom(File file) throws IOException {
            System.out.println("Read from " +file.toString());
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
                        final ArrayList<IAtomContainer> mols = new ArrayList<>();
                        for (IAtomContainer molecule : model.getMoleculeSet().atomContainers()) {
                            mols.add(molecule);
                        }
                        return mols;
                    } catch (CDKException e) {
                        throw new IOException(e);
                    }
                }
            } else {
                // check for SMILES and InChI formats
                final BufferedReader br = new BufferedReader(new FileReader(file));
                String line = br.readLine();
                br.close();
                System.out.println(String.valueOf(line));
                final List<IAtomContainer> mols = new ArrayList<>();
                if (line==null) {
                    throw new IOException("Unknown file format: " + file.getName());
                } else if (line.contains("InChI")) {
                    for (String aline : Files.readAllLines(file.toPath(), Charset.forName("UTF-8"))) {
                        try {mols.add(inChIGeneratorFactory.getInChIToStructure(aline, SilentChemObjectBuilder.getInstance()).getAtomContainer());} catch (CDKException e) {

                        }


                    }
                } else {
                    try {
                        smilesParser.parseSmiles(line);
                        for (String aline : Files.readAllLines(file.toPath(), Charset.forName("UTF-8"))) {
                            try {mols.add(smilesParser.parseSmiles(aline));} catch (CDKException e) {

                            }
                        }
                    } catch (InvalidSmilesException e) {
                        throw new IOException("Unknown file format: " + file.getName());
                    }
                }
                return mols;
            }
        }

        public InChI importCompound(String str) throws IOException {
            if (str.startsWith("InChI")) {
                // try InChI parser
                try {
                    final IAtomContainer molecule = inChIGeneratorFactory.getInChIToStructure(str, SilentChemObjectBuilder.getInstance()).getAtomContainer();
                    return importCompound(molecule, null);
                } catch (CDKException e) {
                    throw new IOException(e);
                }
            } else {
                // try SMILES parser
                try {
                    final IAtomContainer molecule = smilesParser.parseSmiles(str);
                    return importCompound(molecule, null);
                } catch (CDKException e) {
                    throw new IOException(e);
                }
            }
        }

        protected void inheritMetadata(File otherDb) throws IOException {
            for (File formulaFile : currentPath.listFiles()) {
                if (formulaFile.getName().endsWith(".json.gz")) {

                    final File otherFormulaFile = new File(otherDb, formulaFile.getName());
                    if (!otherFormulaFile.exists()) continue;

                    final HashMap<String, Compound> compoundPerInchiKey = new HashMap<>();
                    final List<Compound> compounds = new ArrayList<>();
                    try (final JsonParser parser = Json.createParser(new GZIPInputStream(new FileInputStream(formulaFile)))) {
                        Compound.parseCompounds(null, compounds, parser);
                    }
                    for (Compound c : compounds) {
                        compoundPerInchiKey.put(c.getInchi().key2D(), c);
                    }

                    compounds.clear();
                    try (final JsonParser parser = Json.createParser(new GZIPInputStream(new FileInputStream(otherFormulaFile)))) {
                        Compound.parseCompounds(null, compounds, parser);
                    }
                    boolean dirty=false;
                    for (Compound meta : compounds) {
                        final Compound c = compoundPerInchiKey.get(meta.getInchi().key2D());
                        if (c!=null) {
                            c.mergeMetaData(meta);
                            dirty=true;
                        }
                    }

                    if (!dirty) continue;
                    try (final JsonGenerator writer = Json.createGenerator(new GZIPOutputStream(new FileOutputStream(formulaFile)))) {
                        writer.writeStartObject();
                        writer.writeStartArray("compounds");
                        for (Compound fc : compoundPerInchiKey.values()) {
                            fc.asFingerprintCandidate().writeToJSON(writer, true);
                        }
                        writer.writeEnd();
                        writer.writeEnd();
                    }


                }
            }
        }

        protected InChI importCompound(IAtomContainer molecule, String optionalName) throws CDKException {
            InChIGenerator gen = inChIGeneratorFactory.getInChIGenerator(molecule);
            final InChI inchi = new InChI(gen.getInchiKey(), gen.getInchi());
            molecule = inChIGeneratorFactory.getInChIToStructure(inchi.in2D, SilentChemObjectBuilder.getInstance()).getAtomContainer();
            final boolean[] fps = fingerprinter.fingerprintsToBooleans(fingerprinter.computeFingerprints(molecule));
            final Fingerprint fp = new BooleanFingerprint(CdkFingerprintVersion.getDefault(), fps).asArray();
            final String smiles = smilesGen.create(molecule);

            final FingerprintCandidate fc = new FingerprintCandidate(inchi, fp);
            fc.setSmiles(smiles);
            if (optionalName!=null) fc.setName(optionalName);
            fc.setBitset(DatasourceService.Sources.CUSTOM.flag);
            fc.setLinks(new DBLink[0]);
            synchronized (buffer){
                buffer.add(fc);
            }
            return fc.getInchi();
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

        }

        private void mergeCompounds(MolecularFormula key, Collection<FingerprintCandidate> value) throws IOException {
            final File file = new File(database.path, key.toString() + ".json.gz");
            List<FingerprintCandidate> candidates = new ArrayList<>();
            candidates.addAll(value);
            Compound.merge(candidates, file);
        }


        public void writeSettings() throws IOException {
            try (final JsonWriter writer = new JsonWriter(new FileWriter(database.settingsFile()))) {
                writer.beginObject();
                writer.name("inheritance");
                writer.beginArray();
                if (database.deriveFromBioDb) writer.value(DatasourceService.Sources.BIO.name);
                if (database.deriveFromPubchem) writer.value(DatasourceService.Sources.PUBCHEM.name);
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

}
