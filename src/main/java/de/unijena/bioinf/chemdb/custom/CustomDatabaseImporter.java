package de.unijena.bioinf.chemdb.custom;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.stream.JsonWriter;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.BooleanFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.WebAPI;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.fingerid.Fingerprinter;
import de.unijena.bioinf.ms.rest.info.VersionsInfo;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.interfaces.IChemSequence;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.qsar.descriptors.molecular.XLogPDescriptor;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonReader;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class CustomDatabaseImporter {
    private String[] commonNameProps = new String[]{};//notnull;
    private String[] dbIDProps = new String[]{};//notnull;

    final CustomDatabase database;
    File currentPath;
    List<Listener> listeners = new ArrayList<>();

    // fingerprint buffer
    private final List<FingerprintCandidate> buffer;

    // molecule buffer
    private List<IAtomContainer> moleculeBuffer;

    protected Fingerprinter fingerprinter;
    protected InChIGeneratorFactory inChIGeneratorFactory;
    protected SmilesGenerator smilesGen;
    protected SmilesParser smilesParser;
    protected CdkFingerprintVersion fingerprintVersion;
    protected final WebAPI api;

    protected CustomDatabaseImporter(CustomDatabase database, CdkFingerprintVersion version, WebAPI api) {
        this.api = api;
        this.database = database;
        fingerprintVersion = version;
        this.buffer = new ArrayList<>();
        this.moleculeBuffer = new ArrayList<>();
        currentPath = database.path;
        if (currentPath == null) throw new NullPointerException();
        try {
            inChIGeneratorFactory = InChIGeneratorFactory.getInstance();
            smilesGen = SmilesGenerator.generic().aromatic();
            fingerprinter = Fingerprinter.getFor(version);
            smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
            smilesParser.kekulise(true);
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    public void setCommonNameProps(String[] commonNameProps) {
        this.commonNameProps = commonNameProps;
    }

    public void setDbIDProps(String[] dbIDProps) {
        this.dbIDProps = dbIDProps;
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

    public void importFromString(String str) throws IOException {
        importFromString(str, null);
    }

    public void importFromString(String str, String id) throws IOException {
        if (str.startsWith("InChI")) {
            // try InChI parser
            try {
                final IAtomContainer molecule = inChIGeneratorFactory.getInChIToStructure(str, SilentChemObjectBuilder.getInstance()).getAtomContainer();
                if (id != null) molecule.setID(id);
                addMolecule(molecule);
            } catch (CDKException e) {
                throw new IOException(e);
            }
        } else {
            // try SMILES parser
            try {
                final IAtomContainer molecule = smilesParser.parseSmiles(str);
                if (id != null) molecule.setID(id);
                addMolecule(molecule);
            } catch (CDKException e) {
                throw new IOException(e);
            }
        }
    }

    public void importFrom(File file) throws IOException {
        ReaderFactory factory = new ReaderFactory();
        ISimpleChemObjectReader reader = null;
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
                                addMolecule(c);
                            }
                        }
                    }
                } catch (CDKException e) {
                    throw new IOException(e);
                }
            }
        } else {
            // checkConnectionToUrl for SMILES and InChI formats
            final BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            br.close();
            final List<IAtomContainer> mols = new ArrayList<>();
            if (line == null) {
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
                        if (id != null) mol.setID(id);
                        addMolecule(mol);
                    } catch (CDKException e) {
                        CustomDatabase.logger.error(e.getMessage(), e);
                    }


                }
            } else {
                try {
                    smilesParser.parseSmiles(line);
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
                            final IAtomContainer mol = smilesParser.parseSmiles(aline);
                            if (id != null) mol.setID(id);
                            addMolecule(mol);
                        } catch (CDKException e) {
                            CustomDatabase.logger.error(e.getMessage(), e);
                        }
                    }
                } catch (InvalidSmilesException e) {
                    throw new IOException("Unknown file format: " + file.getName());
                }
            }
        }
    }


    private String getFirstMatchingProperty(final IAtomContainer c, final String[] propertyKeys) {
        for (String commonNameProp : propertyKeys) {
            String propValue = (String) c.getProperties().get(commonNameProp);
            if (propValue != null) {
                return propValue;
            }
        }
        return null;
    }

    protected void addMolecule(IAtomContainer mol) throws IOException {
        synchronized (moleculeBuffer) {
            moleculeBuffer.add(mol);
            for (Listener l : listeners) l.newMoleculeBufferSize(moleculeBuffer.size());
            if (moleculeBuffer.size() > 1000) {
                flushMoleculeBuffer();
            }
        }
    }

    private void flushMoleculeBuffer() throws IOException {
        // start downloading
        final HashMap<String, CustomDatabase.Comp> dict = new HashMap<>(moleculeBuffer.size());
        try {
            final InChIGeneratorFactory icf = InChIGeneratorFactory.getInstance();
            for (IAtomContainer c : moleculeBuffer) {
                final String key;
                try {
                    key = icf.getInChIGenerator(c).getInchiKey().substring(0, 14);
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
        for (CustomDatabase.Comp c : dict.values()) {
            try {
                addToBuffer(computeCompound(c.molecule, c.candidate));
            } catch (CDKException | IllegalArgumentException e) {
                CustomDatabase.logger.error(e.getMessage(), e);
            }
        }
        for (Listener l : listeners) l.newMoleculeBufferSize(0);
    }

    private void addToBuffer(FingerprintCandidate fingerprintCandidate) throws IOException {
        synchronized (buffer) {
            buffer.add(fingerprintCandidate);
            for (Listener l : listeners) {
                l.newFingerprintBufferSize(buffer.size());
                l.newInChI(fingerprintCandidate.getInchi());
            }
            if (buffer.size() > 10000)
                flushBuffer();
        }
    }

    protected FingerprintCandidate computeCompound(IAtomContainer molecule, FingerprintCandidate fc) throws CDKException, IOException {
        final String commonName = getFirstMatchingProperty(molecule, commonNameProps);
        final String id = getFirstMatchingProperty(molecule, dbIDProps);

        if (fc == null) return computeCompound(molecule, id, commonName);

        CustomDatabase.logger.info("download fingerprint " + fc.getInchiKey2D());
        if (fc.getLinks() == null) fc.setLinks(new DBLink[0]);

        if (fc.getName() == null || fc.getName().isEmpty()) {
            if (commonName != null)
                fc.setName(commonName);
        }

        if (id != null) {
            if (fc.getName() == null || fc.getName().isEmpty())
                fc.setName(id);
            DBLink[] ls = Arrays.copyOf(fc.getLinks(), fc.getLinks().length + 1);
            ls[ls.length - 1] = new DBLink(database.name, id);
            fc.setLinks(ls);
        } else {
            DBLink[] ls = Arrays.copyOf(fc.getLinks(), fc.getLinks().length + 1);
            ls[ls.length - 1] = new DBLink(database.name, "");
            fc.setLinks(ls);
        }
        fc.setBitset(fc.getBitset() | CustomDataSourceService.getSourceFromName(database.name).flag());

        synchronized (buffer) {
            buffer.add(fc);
            if (buffer.size() > 10000)
                flushBuffer();
        }
        return fc;
    }

    protected FingerprintCandidate computeCompound(IAtomContainer molecule, final String id, final String commonName) throws CDKException, IllegalArgumentException, IOException {
        InChIGenerator gen = inChIGeneratorFactory.getInChIGenerator(molecule);
        final InChI inchi = new InChI(gen.getInchiKey(), gen.getInchi());
        molecule = inChIGeneratorFactory.getInChIToStructure(inchi.in2D, SilentChemObjectBuilder.getInstance()).getAtomContainer();
        final boolean[] fps = fingerprinter.fingerprintsToBooleans(fingerprinter.computeFingerprints(molecule));
        CustomDatabase.logger.info("compute fingerprint " + inchi.key2D());
        final Fingerprint fp = new BooleanFingerprint(fingerprintVersion, fps).asArray();
        final String smiles = smilesGen.create(molecule);

        final FingerprintCandidate fc = new FingerprintCandidate(inchi, fp);
        fc.setSmiles(smiles);

        if (commonName != null)
            fc.setName(commonName);
        if (id != null) {
            fc.setLinks(new DBLink[]{new DBLink(database.name, id)});
            if (fc.getName() == null || fc.getName().isEmpty())
                fc.setName(id);//set id as name if no name was set
        } else {
            fc.setLinks(new DBLink[0]);
        }
        fc.setBitset(CustomDataSourceService.getSourceFromName(database.name).flag());

        // COMPUTE CHARGE STATE
        if (inchi.in3D.contains("/p+")) {
            fc.setpLayer(CompoundCandidateChargeState.POSITIVE_CHARGE.getValue());
        } else if (inchi.in3D.contains("/p-")) {
            fc.setpLayer(CompoundCandidateChargeState.NEGATIVE_CHARGE.getValue());
        } else {
            fc.setpLayer(CompoundCandidateChargeState.NEUTRAL_CHARGE.getValue());
        }
        if (inchi.in3D.contains("/q+")) {
            fc.setqLayer(CompoundCandidateChargeState.POSITIVE_CHARGE.getValue());
        }
        if (inchi.in3D.contains("/q-")) {
            fc.setqLayer(CompoundCandidateChargeState.NEGATIVE_CHARGE.getValue());
        } else {
            fc.setqLayer(CompoundCandidateChargeState.NEUTRAL_CHARGE.getValue());
        }

        {
            // compute XLOGP
            final XLogPDescriptor descriptor = new XLogPDescriptor();
            AtomContainerManipulator.convertImplicitToExplicitHydrogens(molecule);
            descriptor.setParameters(new Object[]{true, true});
            fc.setXlogp(((DoubleResult) descriptor.calculate(molecule).getValue()).doubleValue());
        }


        synchronized (buffer) {
            buffer.add(fc);
            if (buffer.size() > 10000)
                flushBuffer();
        }
        return fc;
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

    /*public void deleteDatabase() {
        synchronized (database) {
            if (currentPath.exists()) {
                for (File f : currentPath.listFiles()) {
                    f.delete();
                }
                currentPath.delete();
            }
        }
    }*/

    public static void importDatabase(String dbPath, List<String> files, WebAPI api) {
        try {
            final CustomDatabase db = CustomDatabase.createNewDatabase(new File(dbPath).getName(), new File(dbPath), api.getFingerprintVersion());
            final List<File> inchiorsmiles = new ArrayList<>();
            for (String f : files) inchiorsmiles.add(new File(f));

            db.buildDatabase(inchiorsmiles, inchi -> System.out.println(inchi.in2D + " imported"), api);
        } catch (IOException | CDKException e) {
            e.printStackTrace();
        }
        System.out.println("\n\nDatabase imported. Use --fingerid_db=\"" + dbPath + "\" to search in this database");
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
