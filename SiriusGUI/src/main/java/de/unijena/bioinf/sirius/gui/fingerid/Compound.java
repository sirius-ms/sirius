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
import gnu.trove.list.array.TIntArrayList;
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

public class Compound {

    private static Compound PrototypeCompound;

    protected static Compound getPrototypeCompound() {
        if (PrototypeCompound!=null) return PrototypeCompound;
        PrototypeCompound = new Compound();
        PrototypeCompound.inchi = new InChI("WQZGKKKJIJFFOK-GASJEMHNSA-N", "InChI=1S/C6H12O6/c7-1-2-3(8)4(9)5(10)6(11)12-2/h2-11H,1H2/t2-,3-,4+,5-,6?/m1/s1");
        PrototypeCompound.smiles = new Smiles("OC[C@H]1OC(O)[C@H](O)[C@@H](O)[C@@H]1O");
        PrototypeCompound.name = "Glucose";
        PrototypeCompound.pubchemIds = new int[]{5793};
        PrototypeCompound.fingerprint = new boolean[]{true};
        return PrototypeCompound;
    }

    protected InChI inchi;
    protected Smiles smiles;
    protected String name;
    protected IAtomContainer molecule;

    protected boolean[] fingerprint;
    protected Multimap<String, String> databases;
    protected int[] pubchemIds; // special case for the lots of pubchem ids a structure might have

    protected Compound() {

    }

    protected static List<Compound> parseCompounds(int[] fingerprintIndizes, List<Compound> compounds, JsonParser parser) {
        if (parser.next()!= JsonParser.Event.START_OBJECT) throw new JsonException("Expect json object");
        if (!findTopLevelKey(parser, "compounds")) throw new JsonException("Do not find any compounds for given molecular formula");
        if (parser.next()!=JsonParser.Event.START_ARRAY) throw new JsonException("Expect array of compounds");
        outer:
        while (parser.hasNext()) {
            final JsonParser.Event event = parser.next();
            switch (event) {
                case START_OBJECT:
                    compounds.add(Compound.parseFromJSON(parser, fingerprintIndizes));
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

    public static Compound parseFromJSON(JsonParser parser, int[] fingerprintIndizes) {
        final Compound compound = new Compound();
        String inchi = null, inchikey = null;
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
                            compound.fingerprint = stringToBoolean(expectString(parser), fingerprintIndizes); break;
                        case "links":
                            parseLinks(compound, parser); break;
                    }; break;
                case END_OBJECT:
                    compound.inchi = new InChI(inchikey, inchi);
                    return compound;
            }
        }
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
