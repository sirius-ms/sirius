/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.fingerid;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.fingerid.db.CustomDataSourceService;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;
import net.sf.jniinchi.INCHI_RET;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.descriptors.molecular.XLogPDescriptor;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Compound {

    private static Compound PrototypeCompound;

    private static Logger logger = LoggerFactory.getLogger(Compound.class);

    protected static Compound getPrototypeCompound() {
        if (PrototypeCompound != null) return PrototypeCompound;
        PrototypeCompound = new Compound();
        PrototypeCompound.inchi = new InChI("WQZGKKKJIJFFOK-GASJEMHNSA-N", "InChI=1S/C6H12O6/c7-1-2-3(8)4(9)5(10)6(11)12-2/h2-11H,1H2/t2-,3-,4+,5-,6?/m1/s1");
        PrototypeCompound.smiles = new Smiles("OC[C@H]1OC(O)[C@H](O)[C@@H](O)[C@@H]1O");
        PrototypeCompound.name = "Glucose";
        PrototypeCompound.pubchemIds = new int[]{5793};
        PrototypeCompound.fingerprint = new ArrayFingerprint(CdkFingerprintVersion.getDefault(), new short[]{
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 34, 35, 38, 80, 120
        });
        return PrototypeCompound;
    }

    protected InChI inchi;
    protected Smiles smiles;
    protected String name;
    protected IAtomContainer molecule;
    protected double xlogP = Double.NaN;
    protected long bitset;

    protected Fingerprint fingerprint;
    protected Multimap<String, String> databases;
    protected int[] pubchemIds; // special case for the lots of pubchem ids a structure might have

    protected int pLayer;
    protected int qLayer;

    protected Compound(FingerprintCandidate candidate) {
        this.inchi = candidate.getInchi();
        this.smiles = new Smiles(candidate.getSmiles());
        this.name = candidate.getName();
        this.bitset = candidate.getBitset();
        this.fingerprint = candidate.getFingerprint();
        final Set<String> names = DatasourceService.getDataSourcesFromBitFlags(candidate.getBitset());
        this.databases = ArrayListMultimap.create(names.size(), 1);
        if (candidate.getLinks()!=null) {
            for (DBLink link : candidate.getLinks()) {
                this.databases.put(link.name, link.id);
            }
        }

        for (String aname : names)
            if (!this.databases.containsKey(aname))
                this.databases.put(aname, null);
        this.pLayer = candidate.getpLayer();
        this.qLayer = candidate.getqLayer();
        this.xlogP = candidate.getXlogp();
    }

    public long getBitset() {
        return bitset;
    }

    protected Compound() {

    }

    public CompoundWithAbstractFP<ProbabilityFingerprint> asQuery(ProbabilityFingerprint probFp) {
        return new CompoundWithAbstractFP<ProbabilityFingerprint>(inchi, probFp);
    }

    public CompoundWithAbstractFP<Fingerprint> asCandidate() {
        return new CompoundWithAbstractFP<Fingerprint>(inchi, this.fingerprint);
    }

    public FingerprintCandidate asFingerprintCandidate() {
        final FingerprintCandidate fc = new FingerprintCandidate(inchi, fingerprint);
        fc.setBitset(bitset);
        final ArrayList<DBLink> links = new ArrayList<>();
        if (pubchemIds != null)
            for (int i : pubchemIds)
                links.add(new DBLink(DatasourceService.Sources.PUBCHEM.name(), String.valueOf(i)));
        if (databases != null) {
            for (Map.Entry<String, String> entry : databases.entries()) {
                links.add(new DBLink(entry.getKey(), entry.getValue()));
            }
        }
        fc.setLinks(links.toArray(new DBLink[links.size()]));
        if (name != null) fc.setName(name);
        if (smiles != null) fc.setSmiles(smiles.smiles);
        fc.setpLayer(pLayer);
        fc.setqLayer(qLayer);
        fc.setXlogp(xlogP);
        return fc;
    }


    public static List<Compound> parseCompounds(MaskedFingerprintVersion version, List<Compound> compounds, JsonParser parser) {
        if (parser.next() != JsonParser.Event.START_OBJECT) throw new JsonException("Expect json object");
        if (!findTopLevelKey(parser, "compounds"))
            throw new JsonException("Do not find any compounds for given molecular formula");
        if (parser.next() != JsonParser.Event.START_ARRAY) throw new JsonException("Expect array of compounds");
        outer:
        while (parser.hasNext()) {
            final JsonParser.Event event = parser.next();
            switch (event) {
                case START_OBJECT:
                    compounds.add(Compound.parseFromJSON(parser, version));
                    break;
                case END_ARRAY:
                    break outer;
            }
        }
        return compounds;
    }


    private static boolean findTopLevelKey(JsonParser parser, String keyname) {
        int intendation = 0;
        while (true) {
            final JsonParser.Event event = parser.next();
            switch (event) {
                case START_ARRAY:
                case START_OBJECT:
                    ++intendation;
                    break;
                case END_ARRAY:
                case END_OBJECT:
                    if (--intendation < 0) return false;
                    break;
                case KEY_NAME:
                    if (intendation == 0 && parser.getString().equals(keyname)) return true;
            }
        }
    }

    public static Compound parseFromJSON(JsonParser parser, MaskedFingerprintVersion version) {
        final Compound compound = new Compound();
        String inchi = null, inchikey = null;
        long flags = 0;
        int pLayer = 0;
        int qLayer = 0;
        double xlogp = Double.NaN;
        while (true) {
            final JsonParser.Event event = parser.next();
            switch (event) {
                case KEY_NAME:
                    final String name = parser.getString();
                    switch (name) {
                        case "inchi":
                            inchi = expectString(parser);
                            break;
                        case "inchikey":
                            inchikey = expectString(parser);
                            break;
                        case "name":
                            compound.name = expectString(parser);
                            break;
                        case "smiles":
                            compound.smiles = new Smiles(expectString(parser));
                            break;
                        case "fingerprint":
                            expectArray(parser);
                            final TShortArrayList values = new TShortArrayList(100);
                            while (consumeShorts(parser, values)) {
                            }
                            compound.fingerprint = version == null ? new ArrayFingerprint(CdkFingerprintVersion.getDefault(), values.toArray()) : version.mask(values.toArray());
                            break;
                        case "bitset":
                            flags = expectLong(parser);
                            break;
                        case "links":
                            parseLinks(compound, parser);
                            break;
                        case "pLayer":
                            pLayer = expectInt(parser);
                            break;
                        case "qLayer":
                            qLayer = expectInt(parser);
                            break;
                        case "xlogp":
                            xlogp = expectDouble(parser);
                            break;
                    }
                    break;
                case END_OBJECT:
                    compound.inchi = new InChI(inchikey, inchi);
                    // add databases without links
                    compound.bitset = flags;
                    final Set<String> names = DatasourceService.getDataSourcesFromBitFlags(flags);
                    if (compound.databases != null) {
                        for (String aname : names) {
                            if (!compound.databases.containsKey(aname))
                                compound.addDatabase(aname, null);
//                                compound.databases.put(aname, null);
                        }
                    } else {
                        compound.databases = ArrayListMultimap.create(names.size(), 1);
                        for (String aname : names)
                            compound.addDatabase(aname, null);//compound.databases.put(aname, null);
                    }
                    compound.pLayer = pLayer;
                    compound.qLayer = qLayer;
                    compound.xlogP = xlogp;
                    return compound;
            }
        }
    }

    private static void expectArray(JsonParser parser) {
        final JsonParser.Event event = parser.next();
        if (event != JsonParser.Event.START_ARRAY)
            throw new JsonException("expected array value but '" + event.name() + "' is given.");
    }

    private static boolean consumeShorts(JsonParser parser, TShortArrayList values) {
        final JsonParser.Event event = parser.next();
        if (event == JsonParser.Event.END_ARRAY) return false;
        if (event != JsonParser.Event.VALUE_NUMBER)
            throw new JsonException("expected number value but '" + event.name() + "' is given.");
        values.add((short) parser.getInt());
        return true;
    }

    private static int expectInt(JsonParser parser) {
        final JsonParser.Event event = parser.next();
        if (event != JsonParser.Event.VALUE_NUMBER)
            throw new JsonException("expected number value but '" + event.name() + "' is given.");
        return parser.getInt();
    }

    private static long expectLong(JsonParser parser) {
        final JsonParser.Event event = parser.next();
        if (event != JsonParser.Event.VALUE_NUMBER)
            throw new JsonException("expected number value but '" + event.name() + "' is given.");
        return parser.getLong();
    }

    private static double expectDouble(JsonParser parser) {
        final JsonParser.Event event = parser.next();
        if (event != JsonParser.Event.VALUE_NUMBER)
            throw new JsonException("expected number value but '" + event.name() + "' is given.");
        return parser.getBigDecimal().doubleValue();
    }

    private static void parseLinks(Compound compound, JsonParser parser) {
        if (parser.next() != JsonParser.Event.START_OBJECT) throw new JsonException("expected start of dictionary");
        final TIntArrayList pubchemIds = new TIntArrayList(10);
        while (true) {
            final JsonParser.Event event = parser.next();
            switch (event) {
                case KEY_NAME:
                    final String dbname = parser.getString();
                    final boolean pubchem = dbname.equals(DatasourceService.Sources.PUBCHEM.name);
                    if (compound.databases == null) compound.databases = ArrayListMultimap.create(5, 1);
                    if (parser.next() != JsonParser.Event.START_ARRAY)
                        throw new JsonException("expected start of array");

                    while (true) {
                        final JsonParser.Event anevent = parser.next();
                        if (anevent == JsonParser.Event.END_ARRAY) {
                            break;
                        } else if (anevent == JsonParser.Event.VALUE_STRING) {
                            final String id = parser.getString();
                            if (pubchem) {
                                pubchemIds.add(Integer.parseInt(id));
                            }
                            compound.databases.put(dbname, id);
                        }
                    }
                    break;
                case END_OBJECT:
                    compound.pubchemIds = pubchemIds.toArray();
                    return;
            }
        }
    }

    private static String expectString(JsonParser parser) {
        final JsonParser.Event event = parser.next();
        if (event != JsonParser.Event.VALUE_STRING)
            throw new JsonException("expected string value but '" + event.name() + "' is given.");
        return parser.getString();
    }

    public IAtomContainer getMolecule() {
        if (molecule == null) molecule = parseMoleculeFromInChi();
        return molecule;
    }

    private IAtomContainer parseMoleculeFromInChi() {
        try {
            final InChIGeneratorFactory f = InChIGeneratorFactory.getInstance();
            final InChIToStructure s = f.getInChIToStructure(inchi.in2D, SilentChemObjectBuilder.getInstance());
            if (s.getReturnStatus() == INCHI_RET.OKAY && (s.getReturnStatus() == INCHI_RET.OKAY || s.getReturnStatus() == INCHI_RET.WARNING)) {
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(s.getAtomContainer());

                return s.getAtomContainer();
            } else {
                logger.warn("Cannot parse InChI: " + String.valueOf(inchi.in2D) + " due to the following error: " + String.valueOf(s.getMessage() + " Return code: " + s.getReturnStatus() + ", Return status: " + s.getReturnStatus().toString()));
                // try to parse smiles instead
                return parseMoleculeFromSmiles();
            }
            // calculate xlogP
        } catch (CDKException e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            return parseMoleculeFromSmiles();
        }
    }

    public void calculateXlogP() {
        if (Double.isNaN(xlogP)) {
            try {
                XLogPDescriptor logPDescriptor = new XLogPDescriptor();
                logPDescriptor.setParameters(new Object[]{true, true});
                this.xlogP = ((DoubleResult) logPDescriptor.calculate(getMolecule()).getValue()).doubleValue();
            } catch (CDKException e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            }
        }
    }

    public void generateInchiIfNull() {
        try {
            if (inchi == null) {
                final InChIGenerator gen = InChIGeneratorFactory.getInstance().getInChIGenerator(getMolecule());
                this.inchi = new InChI(gen.getInchiKey(), gen.getInchi());
            }
        } catch (CDKException e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
        }
    }

    private IAtomContainer parseMoleculeFromSmiles() {
        try {
            final IAtomContainer c =  new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(smiles.smiles);
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(c);
            return c;
        } catch (CDKException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static boolean[] stringToBoolean(String fingerprint, int[] fingerprintIndizes) {
        final boolean[] values = new boolean[fingerprintIndizes.length];
        for (int k = 0; k < fingerprintIndizes.length; ++k)
            if (fingerprint.charAt(fingerprintIndizes[k]) == '1')
                values[k] = true;
        return values;
    }

    public static void merge(List<FingerprintCandidate> candidates, File file) throws IOException {
        MaskedFingerprintVersion mv = null;
        if (candidates.size() > 0) {
            FingerprintVersion v = candidates.get(0).getFingerprint().getFingerprintVersion();
            if (v instanceof MaskedFingerprintVersion) mv = (MaskedFingerprintVersion) v;
            else {
                mv = MaskedFingerprintVersion.buildMaskFor(v).enableAll().toMask();
            }
        }
        merge(mv, candidates, file);
    }

    /**
     * merges a given list of fingerprint candidates into the given file. Ignore duplicates
     *
     * @return number of newly added candidates
     */
    public static int merge(FingerprintVersion version, List<FingerprintCandidate> candidates, File file) throws IOException {
        int sizeDiff = 0;
        final MaskedFingerprintVersion mv = (version instanceof MaskedFingerprintVersion) ? (MaskedFingerprintVersion) version : MaskedFingerprintVersion.buildMaskFor(version).enableAll().toMask();
        final HashMap<String, FingerprintCandidate> compoundPerInchiKey = new HashMap<>();
        for (FingerprintCandidate fc : candidates) compoundPerInchiKey.put(fc.getInchiKey2D(), fc);
        sizeDiff = compoundPerInchiKey.size();
        if (file.exists()) {
            final List<Compound> compounds = new ArrayList<>();
            try (final JsonParser parser = Json.createParser(new GZIPInputStream(new FileInputStream(file)))) {
                parseCompounds(mv, compounds, parser);
            }
            for (Compound c : compounds) {
                if (compoundPerInchiKey.containsKey(c.inchi.key2D()))
                    --sizeDiff;
                else
                    compoundPerInchiKey.put(c.inchi.key2D(), c.asFingerprintCandidate());
            }
        }
        try (final JsonGenerator writer = Json.createGenerator(new GZIPOutputStream(new FileOutputStream(file)))) {
            writer.writeStartObject();
            writer.writeStartArray("compounds");
            for (FingerprintCandidate fc : compoundPerInchiKey.values()) {
                fc.writeToJSON(writer, true);
            }
            writer.writeEnd();
            writer.writeEnd();
        }
        return sizeDiff;
    }

    public InChI getInchi() {
        return inchi;
    }

    public Smiles getSmiles() {
        return smiles;
    }

    public String getName() {
        return name;
    }

    public double getXlogP() {
        return xlogP;
    }

    public Fingerprint getFingerprint() {
        return fingerprint;
    }

    /*public void mergeMetaData(Compound meta) {
        if (name==null) name = meta.name;
        if (smiles==null) smiles = meta.smiles;
        if (inchi==null) inchi = meta.inchi;
        if (pubchemIds==null) pubchemIds = meta.pubchemIds;
        else if (meta.pubchemIds!=null) {
            final TIntHashSet ids = new TIntHashSet(pubchemIds);
            ids.addAll(meta.pubchemIds);
            pubchemIds = ids.toArray();
            Arrays.sort(pubchemIds);
        }
        bitset = bitset|meta.bitset;
        if (databases==null) databases=ArrayListMultimap.create(meta.databases);
        else {
            databases = HashMultimap.create(databases);
            databases.putAll(meta.databases);
            databases = ArrayListMultimap.create(databases);
        }
    }*/

    public void addDatabase(String name, String id) {
        CustomDataSourceService.Source c = CustomDataSourceService.getSourceFromName(name);
        if (c == null) {
            System.out.println("SCHOULD NOT BE ADDED");
        }
        long bit = c.flag();
        databases.put(name, id);
        bitset |= bit;
    }


    public boolean canBeNeutralCharged() {
        return hasChargeState(CompoundCandidateChargeState.NEUTRAL_CHARGE);
    }

    public boolean canBePositivelyCharged() {
        return hasChargeState(CompoundCandidateChargeState.POSITIVE_CHARGE);
    }

    public boolean canBeNegativelyCharged() {
        return hasChargeState(CompoundCandidateChargeState.NEGATIVE_CHARGE);
    }

    public boolean hasChargeState(CompoundCandidateChargeState chargeState) {
        return (hasChargeState(pLayer, chargeState.getValue()) || hasChargeState(qLayer, chargeState.getValue()));
    }

    public boolean hasChargeState(CompoundCandidateChargeLayer chargeLayer, CompoundCandidateChargeState chargeState) {
        return (chargeLayer == CompoundCandidateChargeLayer.P_LAYER ?
                hasChargeState(pLayer, chargeState.getValue()) :
                hasChargeState(qLayer, chargeState.getValue())
        );
    }

    private boolean hasChargeState(int chargeLayer, int chargeState) {
        return ((chargeLayer & chargeState) == chargeState);
    }

}
