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
import de.unijena.bioinf.babelms.utils.SmilesUCdk;
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

    public static String inchi2inchiKey(String inchi) {
        final InChI in = getInchiWithKeyOrThrow(inchi);
        return in == null ? null : in.key;
    }

    public static InChI getInchiWithKeyOrThrow(String inchi) {
        return getInchiWithKeyOrThrow(inchi, e -> new RuntimeException("Error when creating CDK Objects from InChI String.", e));
    }

    public static <X extends Throwable> InChI getInchiWithKeyOrThrow(String inchi, Function<CDKException, ? extends X> exceptionSupplier) throws X {
        try {
            return getInchiWithKey(inchi);
        } catch (CDKException e) {
            throw exceptionSupplier.apply(e);
        }
    }

    public static InChI getInchiWithKey(String inchi) throws CDKException {
        return getInchi(getAtomContainerFromInchi(inchi));
    }

    //    NEWPSOFF/DoNotAddH/SNon
    public static InChI getInchi(IAtomContainer atomContainer) throws CDKException {
        // this will create a standard inchi, see: https://egonw.github.io/cdkbook/inchi.html
        InChIGenerator inChIGenerator = InChIGeneratorFactory.getInstance().getInChIGenerator(atomContainer, InchiFlag.SNon); //suppress Omitted undefined stereo
        InchiStatus state = inChIGenerator.getStatus();
        if (state != InchiStatus.ERROR) {
            if (state == InchiStatus.WARNING)
                LoggerFactory.getLogger(InChISMILESUtils.class).debug("Warning while reading AtomContainer with title '" + atomContainer.getTitle() + "' -> " + inChIGenerator.getMessage());
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
                LoggerFactory.getLogger(InChISMILESUtils.class).debug("Warning while parsing InChI:\n'" + inchi + "'\n-> " + structureGenerator.getMessage());
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

    public static InChI getInchiFromSmiles(String smiles) throws CDKException {
        return getInchi(getAtomContainerFromSmiles(smiles));
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
        return SmilesUCdk.formulaFromSmiles(smiles);
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
    }

}
