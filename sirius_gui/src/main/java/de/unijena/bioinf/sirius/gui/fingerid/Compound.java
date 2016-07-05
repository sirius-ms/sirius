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

package de.unijena.bioinf.sirius.gui.fingerid;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;
import net.sf.jniinchi.INCHI_RET;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import javax.json.JsonException;
import javax.json.stream.JsonParser;
import java.util.List;
import java.util.Set;

public class Compound {

    private static Compound PrototypeCompound;

    protected static Compound getPrototypeCompound() {
        if (PrototypeCompound!=null) return PrototypeCompound;
        PrototypeCompound = new Compound();
        PrototypeCompound.inchi = new InChI("WQZGKKKJIJFFOK-GASJEMHNSA-N", "InChI=1S/C6H12O6/c7-1-2-3(8)4(9)5(10)6(11)12-2/h2-11H,1H2/t2-,3-,4+,5-,6?/m1/s1");
        PrototypeCompound.smiles = new Smiles("OC[C@H]1OC(O)[C@H](O)[C@@H](O)[C@@H]1O");
        PrototypeCompound.name = "Glucose";
        PrototypeCompound.pubchemIds = new int[]{5793};
        PrototypeCompound.fingerprint = new ArrayFingerprint(CdkFingerprintVersion.getDefault(), new short[]{
            1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,34,35,38,80,120
        });
        return PrototypeCompound;
    }

    protected InChI inchi;
    protected Smiles smiles;
    protected String name;
    protected IAtomContainer molecule;

    protected Fingerprint fingerprint;
    protected Multimap<String, String> databases;
    protected int[] pubchemIds; // special case for the lots of pubchem ids a structure might have

    protected Compound(FingerprintCandidate candidate) {
        this.inchi = candidate.getInchi();
        this.smiles = new Smiles(candidate.getSmiles());
        this.name = candidate.getName();
        this.fingerprint = candidate.getFingerprint();
        final Set<String> names = DatasourceService2.getDataSourcesFromBitFlags(candidate.getBitset());
        names.remove(DatasourceService2.Sources.PUBCHEM.name);
        this.databases = ArrayListMultimap.create(names.size(), 1);
        for (String aname : names) this.databases.put(aname,null);
    }

    protected Compound() {

    }

    protected static List<Compound> parseCompounds(MaskedFingerprintVersion version, List<Compound> compounds, JsonParser parser) {
        if (parser.next()!= JsonParser.Event.START_OBJECT) throw new JsonException("Expect json object");
        if (!findTopLevelKey(parser, "compounds")) throw new JsonException("Do not find any compounds for given molecular formula");
        if (parser.next()!=JsonParser.Event.START_ARRAY) throw new JsonException("Expect array of compounds");
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
                    ++intendation; break;
                case END_ARRAY:
                case END_OBJECT:
                    if (--intendation < 0) return false;
                    break;
                case KEY_NAME:
                    if (intendation==0 && parser.getString().equals(keyname)) return true;
            }
        }
    }

    public static Compound parseFromJSON(JsonParser parser, MaskedFingerprintVersion version) {
        final Compound compound = new Compound();
        String inchi = null, inchikey = null;
        int flags=0;
        while (true) {
            final JsonParser.Event event = parser.next();
            switch (event) {
                case KEY_NAME:
                    final String name = parser.getString();
                    switch (name) {
                        case "inchi":
                            inchi = expectString(parser); break;
                        case "inchikey":
                            inchikey = expectString(parser); break;
                        case "name":
                            compound.name = expectString(parser); break;
                        case "smiles":
                            compound.smiles = new Smiles(expectString(parser)); break;
                        case "fingerprint":
                            expectArray(parser);
                            final TShortArrayList values = new TShortArrayList(100);
                            while (consumeShorts(parser, values)) {}
                            compound.fingerprint = version.mask(values.toArray());
                            break;
                        case "bitset":
                            flags = expectInt(parser); break;
                        case "links":
                            parseLinks(compound, parser); break;
                    }; break;
                case END_OBJECT:
                    compound.inchi = new InChI(inchikey, inchi);
                    // add databases without links
                    final Set<String> names = DatasourceService2.getDataSourcesFromBitFlags(flags);
                    names.remove(DatasourceService2.Sources.PUBCHEM.name);
                    if (compound.databases!=null) {
                        for (String aname : names) {
                            if (!compound.databases.containsKey(aname))
                                compound.databases.put(aname, null);
                        }
                    } else {
                        compound.databases = ArrayListMultimap.create(names.size(), 1);
                        for (String aname : names) compound.databases.put(aname,null);
                    }
                    return compound;
            }
        }
    }

    private static void expectArray(JsonParser parser) {
        final JsonParser.Event event = parser.next();
        if (event != JsonParser.Event.START_ARRAY) throw new JsonException("expected array value but '" + event.name() + "' is given." );
    }
    private static boolean consumeShorts(JsonParser parser, TShortArrayList values) {
        final JsonParser.Event event = parser.next();
        if (event == JsonParser.Event.END_ARRAY) return false;
        if (event != JsonParser.Event.VALUE_NUMBER) throw new JsonException("expected number value but '" + event.name() + "' is given." );
        values.add((short)parser.getInt());
        return true;
    }

    private static int expectInt(JsonParser parser) {
        final JsonParser.Event event = parser.next();
        if (event != JsonParser.Event.VALUE_NUMBER) throw new JsonException("expected number value but '" + event.name() + "' is given." );
        return parser.getInt();
    }

    private static void parseLinks(Compound compound, JsonParser parser) {
        if (parser.next() != JsonParser.Event.START_OBJECT) throw new JsonException("expected start of dictionary");
        final TIntArrayList pubchemIds = new TIntArrayList(10);
        while (true) {
            final JsonParser.Event event = parser.next();
            switch (event) {
                case KEY_NAME:
                    final String dbname = parser.getString();
                    final boolean pubchem = dbname.equals("PubChem");
                    if (!pubchem && compound.databases==null) compound.databases = ArrayListMultimap.create(5, 1);
                    if (parser.next() != JsonParser.Event.START_ARRAY) throw new JsonException("expected start of array");

                    while (true) {
                        final JsonParser.Event anevent = parser.next();
                        if (anevent == JsonParser.Event.END_ARRAY) {
                            break;
                        } else if (anevent == JsonParser.Event.VALUE_STRING) {
                            final String id = parser.getString();
                            if (pubchem) {
                                pubchemIds.add(Integer.parseInt(id));
                            } else {
                                compound.databases.put(dbname, id);
                            }
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
        if (event != JsonParser.Event.VALUE_STRING) throw new JsonException("expected string value but '" + event.name() + "' is given." );
        return parser.getString();
    }

    public IAtomContainer getMolecule() {
        if (molecule==null) molecule = parseMoleculeFromInChi();
        return molecule;
    }

    private IAtomContainer parseMoleculeFromInChi() {
        try {
            final InChIGeneratorFactory f = InChIGeneratorFactory.getInstance();
            final InChIToStructure s = f.getInChIToStructure(inchi.in2D, SilentChemObjectBuilder.getInstance());
            if (s.getReturnStatus() != INCHI_RET.OKAY && s.getReturnStatus() != INCHI_RET.WARNING) {
                return s.getAtomContainer();
            } else {
                // try to parse smiles instead
                return parseMoleculeFromSmiles();
            }
        } catch (CDKException e) {
            e.printStackTrace();
            return parseMoleculeFromSmiles();
        }
    }

    private IAtomContainer parseMoleculeFromSmiles() {
        try {
            return new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(smiles.smiles);
        } catch (InvalidSmilesException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    public static boolean[] stringToBoolean(String fingerprint, int[] fingerprintIndizes) {
        try {
        if (fingerprint.length() < 3000)  {
            System.err.println("TOO SHORT FINGERPRINT:");
            System.err.println(fingerprint);
        }
        final boolean[] values = new boolean[fingerprintIndizes.length];
        for (int k=0; k < fingerprintIndizes.length; ++k)
            if (fingerprint.charAt(fingerprintIndizes[k])=='1')
                values[k] = true;
        return values;
    }catch (Exception e) {
            e.printStackTrace();
            System.err.println(fingerprint);
            System.exit(1);
            return null;
        }
    }
}
