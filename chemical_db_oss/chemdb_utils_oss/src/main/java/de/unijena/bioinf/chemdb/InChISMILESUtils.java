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

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import io.github.dan2097.jnainchi.InchiFlag;
import io.github.dan2097.jnainchi.InchiStatus;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import static de.unijena.bioinf.ChemistryBase.chem.InChIs.*;
import static de.unijena.bioinf.ChemistryBase.chem.SmilesU.*;


public class InChISMILESUtils {

    public static <X extends Throwable> InChIGeneratorFactory getInChIGeneratorFactoryOrThrow() throws RuntimeException {
        return getInChIGeneratorFactoryOrThrow(c -> new RuntimeException("Error when loading CDK InChIGenerator instance.", c));
    }

    public static <X extends Throwable> InChIGeneratorFactory getInChIGeneratorFactoryOrThrow(Function<CDKException, ? extends X> exceptionSupplier) throws X {
        try {
            return InChIGeneratorFactory.getInstance();
        } catch (CDKException e) {
            throw exceptionSupplier.apply(e);
        }
    }

    public static String inchi2inchiKey(String inchi, boolean keepStereoInformation) {
        //todo by converting first to atom container and then retrieving the key, the inchi may be altered and so the inchi key. Should only concern 3D information. May happens for undefined stereo centers.
        final InChI in = getInchiWithKeyOrThrow(inchi, keepStereoInformation);
        return in == null ? null : in.key;
    }

    public static InChI getInchiWithKeyOrThrow(String inchi, boolean keepStereoInformation) {
        return getInchiWithKeyOrThrow(inchi, keepStereoInformation, e -> new RuntimeException("Error when creating CDK Objects from InChI String.", e));
    }

    public static <X extends Throwable> InChI getInchiWithKeyOrThrow(String inchi, boolean keepStereoInformation, Function<CDKException, ? extends X> exceptionSupplier) throws X {
        try {
            return getInchiWithKey(inchi, keepStereoInformation);
        } catch (CDKException e) {
            throw exceptionSupplier.apply(e);
        }
    }

    public static InChI getInchiWithKey(String inchi, boolean keepStereoInformation) throws CDKException {
        return getInchi(getAtomContainerFromInchi(inchi), keepStereoInformation);
    }

    //    NEWPSOFF/DoNotAddH/SNon
    public static InChI getInchi(IAtomContainer atomContainer, boolean keepStereoInformation) throws CDKException {
        // this will create a standard inchi, see: https://egonw.github.io/cdkbook/inchi.html
        InChIGenerator inChIGenerator = InChIGeneratorFactory.getInstance().getInChIGenerator(atomContainer, keepStereoInformation ? new InchiFlag[0] : new InchiFlag[]{InchiFlag.SNon}); //removing stereoInformation produces much less warnings, including 'Omitted undefined stereo'
        InchiStatus state = inChIGenerator.getStatus();
        if (state != InchiStatus.ERROR) {
            if (state == InchiStatus.WARNING)
                LoggerFactory.getLogger(InChISMILESUtils.class).warn("Warning while reading AtomContainer: '" + atomContainer.getTitle() + "'\n-> " + inChIGenerator.getMessage());
            String inchi = inChIGenerator.getInchi();
            if (inchi == null) return null;
            if (!isStandardInchi(inchi))
                throw new IllegalStateException("Non standard Inchi was created ('" + inchi + "'), which is not expected behaviour. Please submit a bug report!");
            String key = inChIGenerator.getInchiKey();
            return newInChI(key, inchi);
        } else {
            throw new CDKException("Error while creating InChI. State: '" + state + "'. Message: '" + inChIGenerator.getMessage() + "'.");
        }
    }

    public static String get2DSmiles(IAtomContainer atomContainer) throws CDKException {
        return SmilesGenerator.unique().create(atomContainer); //Unique - canonical SMILES string, different atom ordering produces the same* SMILES. No isotope or stereochemistry encoded.
    }

    public static String getSmiles(IAtomContainer atomContainer) throws CDKException {
        return SmilesGenerator.unique().create(atomContainer); //Absolute - canonical SMILES string, different atom ordering produces the same SMILES. Isotope and stereochemistry is encoded.
    }

    public static IAtomContainer getAtomContainerFromInchi(String inchi) throws CDKException {
        return getAtomContainerFromInchi(inchi, false);
    }

    public static IAtomContainer getAtomContainerFromInchi(String inchi, boolean lazyErrorHandling) throws CDKException {
        if (inchi == null) throw new NullPointerException("Given InChI is null");
        if (inchi.isEmpty()) throw new IllegalArgumentException("Empty string given as InChI");
        final InChIToStructure structureGenerator = InChIGeneratorFactory.getInstance().
                getInChIToStructure(inchi, SilentChemObjectBuilder.getInstance());
        InchiStatus state = structureGenerator.getStatus();
        if (state != InchiStatus.ERROR) {
            if (state == InchiStatus.WARNING)
                LoggerFactory.getLogger(InChISMILESUtils.class).error("Warning while parsing InChI:\n'" + inchi + "'\n-> " + structureGenerator.getMessage());
            return structureGenerator.getAtomContainer();
        } else {
            if (lazyErrorHandling) {
                LoggerFactory.getLogger(InChISMILESUtils.class).error("Error while parsing InChI:\n'" + inchi + "'\n-> " + structureGenerator.getMessage());
                final IAtomContainer a = structureGenerator.getAtomContainer();
                if (a != null) return a;
            }
            throw new CDKException("Error while creating AtomContainer. State: '" + state + "'. Message: '" + structureGenerator.getMessage() + "'.");
        }
    }

    public static InChI getInchiFromSmiles(String smiles, boolean keepStereoInformation) throws CDKException {
        return getInchi(getAtomContainerFromSmiles(smiles), keepStereoInformation);
    }


    public static IAtomContainer getAtomContainerFromSmiles(String smiles) throws CDKException {
        if (smiles == null) throw new NullPointerException("Given Smiles is null");
        if (smiles.isEmpty()) throw new IllegalArgumentException("Empty string given as Smiles");
        //todo do we need to do any processing?!?
        SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer iAtomContainer = smilesParser.parseSmiles(smiles);
        return iAtomContainer;
    }


    /**
     * IMPORTANT: CDK is very picky with new-lines. for multi-line formats it seems to be important to have a new-line character after last line (and maybe one at first?)
     *
     * @param someStructureFormat input can be SMILES or Inchi or String contained in a .mol-file
     * @return
     */
    public static IAtomContainer getAtomContainer(String someStructureFormat) throws CDKException, IOException {
        if (isInchi(someStructureFormat)) {
            return getAtomContainerFromInchi(someStructureFormat);
        } else if (someStructureFormat.contains("\n")) {
            //it is a structure format from some file
            ReaderFactory readerFactory = new ReaderFactory();
            BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(someStructureFormat.getBytes(StandardCharsets.UTF_8)));
            ISimpleChemObjectReader reader = readerFactory.createReader(in);
//            MDLV2000Reader reader = new MDLV2000Reader();
//            reader.setReader(in);
            if (reader == null) {
                in.close();
                //try with another new-line
                someStructureFormat += "\n";
                in = new BufferedInputStream(new ByteArrayInputStream(someStructureFormat.getBytes(StandardCharsets.UTF_8)));
                reader = readerFactory.createReader(in);
            }
            if (reader == null) {
                in.close();
                throw new IOException("No reader found for given format");
            } else if (reader.accepts(ChemFile.class)) {
                ChemFile cfile = new ChemFile();
                cfile = reader.read(cfile);
                List<IAtomContainer> atomContainerList = ChemFileManipulator.getAllAtomContainers(cfile);

                if (atomContainerList.size() > 1) {
                    throw new IOException("Multiple structures in input");
                } else if (atomContainerList.size() == 0) {
                    throw new IOException("Could not parse any structure");
                }
                return atomContainerList.get(0);

            } else {
                throw new IOException("Unknown format");
            }
        } else {
            //assume SMILES
            return getAtomContainerFromSmiles(someStructureFormat);
        }
    }

    public static IAtomContainer getAtomContainer(@NotNull Smiles smiles) throws CDKException {

        return getAtomContainerFromSmiles(smiles.smiles);
    }


    /**
     * IMPORTANT: CDK is very picky with new-lines. for multi-line formats it seems to be important to have a new-line character after last line (and maybe one at first?)
     *
     * @param someStructureFormat input can be SMILES or Inchi or String contained in a .mol-file
     * @return
     */
    public static String get2DSmiles(String someStructureFormat) throws CDKException, IOException {
        return get2DSmiles(getAtomContainer(someStructureFormat));
    }

    public static String get2DSmiles(Smiles smiles) throws CDKException {
        return get2DSmiles(getAtomContainer(smiles));
    }

    public static MolecularFormula formulaFromSmiles(String smiles) throws InvalidSmilesException, UnknownElementException {
        SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer iAtomContainer = smilesParser.parseSmiles(smiles);
        if (iAtomContainer == null) return null;
        String s = MolecularFormulaManipulator.getString(MolecularFormulaManipulator.getMolecularFormula(iAtomContainer));
        if (s == null) return null;
        int formalCharge = getFormalChargeFromSmiles(smiles);
        MolecularFormula formula = MolecularFormula.parse(s);
        if (formalCharge == 0) return formula;
        else if (formalCharge < 0) {
            return formula.add(MolecularFormula.parse(String.valueOf(Math.abs(formalCharge) + "H")));
        } else {
            return formula.subtract(MolecularFormula.parse(String.valueOf(formalCharge + "H")));
        }
    }


    public static void main(String... args) throws CDKException, IOException {
        //todo remove after testing

//    System.out.println(formulaFromSmiles("CCCCCCCCCCCCCCCCCC(=O)OC[C@H](COP(=O)(O)OCC[N+](C)(C)C)O").formatByHill());
//    System.out.println(InChIs.newInChI(null,"InChI=1S/C26H55NO7P/c1-5-6-7-8-9-10-11-12-13-14-15-16-17-18-19-20-26(29)32-23-25(28)24-34-35(30,31)33-22-21-27(2,3)4/h25,28H,5-24H2,1-4H3,(H,30,31)/t25-/m1/s1").extractFormula().formatByHill());
//    System.out.println(inchi2inchiKey("InChI=1S/C26H55NO7P/c1-5-6-7-8-9-10-11-12-13-14-15-16-17-18-19-20-26(29)32-23-25(28)24-34-35(30,31)33-22-21-27(2,3)4/h25,28H,5-24H2,1-4H3,(H,30,31)/t25-/m1/s1"));
//    InChI i = getInchiAndInchiKey("CCCCCCCCCCCCCCCCCC(=O)OC[C@H](COP(=O)(O)OCC[N+](C)(C)C)O");
//    System.out.println(i.in3D);
//    System.out.println(i.extractFormula().formatByHill());
//    System.out.println(i.key);

        String s = "C(C(/O)=C/C=C1(CC2(/C(\\C(=O)1)=C/C=CC=2)))([O-])=O";
        s = stripStereoCentres(s);
        s = stripDoubleBondGeometry(s);
        Smiles smiles = new Smiles(s);
        System.out.println(get2DSmiles(smiles));
        System.out.println(get2DSmilesByTextReplace("C(C(/O)=C/C=C1(CC2(/C(\\C(=O)1)=C/C=CC=2)))([O-])=O"));

//        System.out.println(getInchiWithKeyOrThrow("InChI=1S/C20H24O9/c1-7-4-10(21)13(23)17(3)9(7)5-11-18-6-28-20(27,16(17)18)12(22)8(2)19(18,26)14(24)15(25)29-11/h4,9,11-14,16,22-24,26-27H,2,5-6H2,1,3H3/t9-,11+,12+,13+,14-,16+,17+,18+,19-,20-/m0/s1", true).in3D);
//        System.out.println(getInchiWithKeyOrThrow("InChI=1S/C20H24O9/c1-7-4-10(21)13(23)17(3)9(7)5-11-18-6-28-20(27,16(17)18)12(22)8(2)19(18,26)14(24)15(25)29-11/h4,9,11-14,16,22-24,26-27H,2,5-6H2,1,3H3/t9-,11+,12+,13+,14-,16+,17+,18+,19-,20-/m0/s1", false).in3D);
//
//
//        System.out.println("InChI=1S/2C2H4O2.Mg/c2*1-2(3)4;/h2*1H3,(H,3,4);/q;;+2/p-2");
//        System.out.println(getInchiWithKeyOrThrow("InChI=1S/2C2H4O2.Mg/c2*1-2(3)4;/h2*1H3,(H,3,4);/q;;+2/p-2", false).in3D);
//        System.out.println(getInchiWithKeyOrThrow("InChI=1S/2C2H4O2.Mg/c2*1-2(3)4;/h2*1H3,(H,3,4);/q;;+2/p-2", true).in3D);
//
//
//        String inchi = "InChI=1S/C5H9/c1-4-5(2)3/h4H,1H2,2-3H3";
//        System.out.println(inchi);
//        System.out.println(getInchiWithKeyOrThrow(inchi, false).in3D);
//        System.out.println(getInchiWithKeyOrThrow(inchi, true).in3D);
//
//        inchi = "InChI=1S/C18H32N8O4/c19-10-14(27)25-8-3-6-13(25)16(29)24-11(4-1-7-23-18(21)22)17(30)26-9-2-5-12(26)15(20)28/h11-13H,1-10,19H2,(H2,20,28)(H,24,29)(H4,21,22,23)";
//        System.out.println(inchi);
//        System.out.println(getInchiWithKeyOrThrow(inchi, false).in3D);
//        System.out.println(getInchiWithKeyOrThrow(inchi, true).in3D);
//
//        inchi = "InChI=1S/C4H8N2S4.Zn/c7-3(8)5-1-2-6-4(9)10;/h1-2H2,(H2,5,7,8)(H2,6,9,10);/q;+2/p-2";
//        System.out.println(inchi);
//        System.out.println(getInchiWithKeyOrThrow(inchi, false).in3D);
//        System.out.println(getInchiWithKeyOrThrow(inchi, true).in3D);
//
//        inchi = "InChI=1S/C30H46O4.Na/c1-25(2)21-8-11-30(7)23(28(21,5)10-9-22(25)32)20(31)16-18-19-17-27(4,24(33)34)13-12-26(19,3)14-15-29(18,30)6;/h16,19,21-23,32H,8-15,17H2,1-7H3,(H,33,34);/q;+1/p-1/t19-,21-,22-,23+,26+,27-,28-,29+,30+;/m0./s1";
//        System.out.println(inchi);
//        System.out.println(getInchiWithKeyOrThrow(inchi, false).in3D);
//        System.out.println(getInchiWithKeyOrThrow(inchi, true).in3D);

        String inchi = "InChI=1S/C32H47N3O10S/c1-2-3-9-14-23(36)15-10-6-4-5-7-12-17-27(26(37)16-11-8-13-18-29(39)40)46-22-25(31(43)34-21-30(41)42)35-28(38)20-19-24(33)32(44)45/h3-12,15,17,23-27,36-37H,2,13-14,16,18-22,33H2,1H3,(H,34,43)(H,35,38)(H,39,40)(H,41,42)(H,44,45)/b6-4-,7-5?,9-3-,11-8-,15-10+,17-12?/t23-,24-,25-,26+,27-/m0/s1";
        System.out.println(inchi);
        System.out.println(getInchiWithKeyOrThrow(inchi, false).in3D);
        System.out.println(getInchiWithKeyOrThrow(inchi, false).key);
        System.out.println(getInchiWithKeyOrThrow(inchi, true).in3D);
        System.out.println(getInchiWithKeyOrThrow(inchi, true).key);

        LoggerFactory.getLogger(InChISMILESUtils.class).error("Column '%s' is missing", new String[0]);
    }

}
