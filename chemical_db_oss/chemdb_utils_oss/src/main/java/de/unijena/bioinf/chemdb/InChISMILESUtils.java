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

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.babelms.utils.SmilesUCdk;
import io.github.dan2097.jnainchi.InchiFlag;
import io.github.dan2097.jnainchi.InchiStatus;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.isomorphism.Pattern;
import org.openscience.cdk.isomorphism.matchers.QueryAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smarts.Smarts;
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
import java.util.stream.StreamSupport;

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

    public static InChI getStandardInchiIfNonStandard(InChI inChI, boolean keepStereoInformation) {
        String inchi = inChI.in3D;
        if (isStandardInchi(inchi)) return inChI;
        return getInchiWithKeyOrThrow(inchi, keepStereoInformation);
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
        //todo this sometimes seems to produce a different InChI key than simultaneously generating InChI plus InChIKey from a SMILES. Does this only happen with q-charges? E.g. 'CC1CCC2C(=CCCC2(C)C)C1(C)CCC(C)=CCN3[CH][NH+](C)[C]4[N][CH][N][C](N)[C]34'
        return getInchi(getAtomContainerFromInchi(inchi), keepStereoInformation);
    }

    //    NEWPSOFF/DoNotAddH/SNon
    public static InChI getInchi(IAtomContainer atomContainer, boolean keepStereoInformation) throws CDKException {
        // this will create a standard inchi, see: https://egonw.github.io/cdkbook/inchi.html
        InChIGenerator inChIGenerator = InChIGeneratorFactory.getInstance().getInChIGenerator(atomContainer, keepStereoInformation ? new InchiFlag[0] : new InchiFlag[]{InchiFlag.SNon}); //removing stereoInformation produces much less warnings, including 'Omitted undefined stereo'
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

    public static InChI getInchiFromSmilesOrThrow(String smiles, boolean keepStereoInformation) {
        try {
            return getInchiFromSmiles(smiles,keepStereoInformation);
        } catch (CDKException e) {
            throw new RuntimeException(e);
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
        return SmilesUCdk.formulaFromSmiles(smiles);
    }


    public static Smiles getMainConnectedComponentOrNull(Smiles smiles) throws CDKException {
        if (SmilesU.isConnected(smiles.smiles)) return smiles;
        IAtomContainer atomContainer = InChISMILESUtils.getAtomContainerFromSmiles(smiles.smiles);
        IAtomContainer mainStructure = getMainConnectedComponentOrNull(atomContainer, true);
        return new Smiles(InChISMILESUtils.getSmiles(mainStructure));
    }

    public static InChI getMainConnectedComponentOrNull(InChI inChI) throws CDKException {
        if (InChIs.isConnected(inChI.in3D)) return inChI;
        IAtomContainer atomContainer = InChISMILESUtils.getAtomContainerFromInchi(inChI.in3D);
        IAtomContainer mainStructure = getMainConnectedComponentOrNull(atomContainer, true);
        return InChISMILESUtils.getInchi(mainStructure, false); //standardization uses 2D inchi anyway //I don't want see these warning "Proton(s) added/removed" anymore
    }

    /**
     * strips some salts and solvent. If only one connected component remains, it is returned. Else null.
     * @param atomContainer
     * @return
     */
    public static IAtomContainer getMainConnectedComponentOrNull(IAtomContainer atomContainer, boolean adjustForRemovedChargesIfPossible) {
        if (atomContainer == null || atomContainer.getAtomCount()==0) return atomContainer;
//        Cl,Br,I]
//[Li,Na,K,Ca,Mg]
//[O,N]


        String smarts = "[Cl,Na,I,Br,K,Ca,Mg,Li,O,N]";
        QueryAtomContainer query = new QueryAtomContainer(DefaultChemObjectBuilder.getInstance());
            if (!Smarts.parse(query, smarts))
                throw new IllegalArgumentException("Could not parse SMARTS: " +
                        smarts + "\n" +
                        Smarts.getLastErrorMesg() + "\n" +
                        Smarts.getLastErrorLocation());
        Pattern pattern = Pattern.findIdentical(query);

        //no need to prepare target molecule with rings and  aromaticity

        if (ConnectivityChecker.isConnected(atomContainer)) return atomContainer;

        IAtomContainerSet fragments = ConnectivityChecker.partitionIntoMolecules(atomContainer);
        int fragmentCount = fragments.getAtomContainerCount();

        int mainComponentIdx = -1;
        int removedCharge = 0;
        for (int i = 0; i < fragmentCount; i++) {
             IAtomContainer f = fragments.getAtomContainer(i);
            if (!pattern.matches(f)) {
                if (mainComponentIdx>=0) {
                    LoggerFactory.getLogger(InChISMILESUtils.class).warn("Molecule has more than one main connected component.");
                    return null; //more than one component after removal of salts
                }
                mainComponentIdx = i;
            } else {
                removedCharge += StreamSupport.stream(f.atoms().spliterator(), false).mapToInt(IAtom::getFormalCharge).sum();
            }
        }

        if (mainComponentIdx == -1 ) {
            try {
                LoggerFactory.getLogger(InChISMILESUtils.class).warn("No main component for: "+InChISMILESUtils.getSmiles(atomContainer));
            } catch (CDKException e) {
                LoggerFactory.getLogger(InChISMILESUtils.class).warn("No main component atomcontainer "+atomContainer.getID()+" | "+atomContainer.getTitle());
            }
            return null;
        }

        IAtomContainer main = fragments.getAtomContainer(mainComponentIdx);

        if (!adjustForRemovedChargesIfPossible || removedCharge == 0) return main;

        int posCharges = 0;
        int negCharges =0;
        for (IAtom atom : main.atoms()) {
            int charge = atom.getFormalCharge();
                if (charge < 0) negCharges += charge;
                if (charge > 0) posCharges += charge;
        }

        if (removedCharge < 0 && posCharges == -removedCharge) {
            removeAllPositiveChargesIfPossible(main);
        } else if (negCharges == -removedCharge) {
            removeAllNegativeChargesIfPossible(main);
        } else {
            //don't know which charges to remove.
            LoggerFactory.getLogger(InChISMILESUtils.class).warn("Cannot remove charges from main connected component");
            return null;
        }

        return main;
    }

    private static void removeAllPositiveChargesIfPossible(IAtomContainer atomContainer) {
        removeChargesIfPossible(atomContainer, true, false);
    }

    private static void removeAllNegativeChargesIfPossible(IAtomContainer atomContainer) {
        removeChargesIfPossible(atomContainer, false, true);
    }

    private static void removeChargesIfPossible(IAtomContainer atomContainer, boolean removePositiveCharges, boolean removeNegativeCharges) {
        for (IAtom atom : atomContainer.atoms()) {
            if (atom.getFormalCharge()<0 && removeNegativeCharges){
                atom.setImplicitHydrogenCount(atom.getImplicitHydrogenCount()-atom.getFormalCharge());
                atom.setFormalCharge(0);
            }
            else if (atom.getFormalCharge()>0 && removePositiveCharges) {
                int adjustment = atom.getImplicitHydrogenCount()-atom.getFormalCharge();
                atom.setImplicitHydrogenCount(Math.max(0, adjustment));
                atom.setFormalCharge(Math.max(0, -adjustment));
            }
        }
    }

    public static void main(String... args) throws CDKException, IOException {
        //todo remove after testing

        String s = "C(C(/O)=C/C=C1(CC2(/C(\\C(=O)1)=C/C=CC=2)))([O-])=O";
        s = stripStereoCentres(s);
        s = stripDoubleBondGeometry(s);
        Smiles smiles = new Smiles(s);
        System.out.println(get2DSmiles(smiles));
        System.out.println(get2DSmilesByTextReplace("C(C(/O)=C/C=C1(CC2(/C(\\C(=O)1)=C/C=CC=2)))([O-])=O"));
    }

}
