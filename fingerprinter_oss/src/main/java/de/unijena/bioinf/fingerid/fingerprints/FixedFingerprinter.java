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

package de.unijena.bioinf.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.InChIs;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.BooleanFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.chemdb.InChISMILESUtils;
import de.unijena.bioinf.fingerid.fingerprints.cache.IFingerprinterCache;
import gnu.trove.set.hash.TIntHashSet;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.slf4j.LoggerFactory;

import java.util.BitSet;
import java.util.List;
import java.util.Set;

import static org.openscience.cdk.CDKConstants.ISAROMATIC;

public class FixedFingerprinter {

    private final IFingerprinterCache iFpCache;
    protected final CdkFingerprintVersion cdkFingerprintVersion;

    @Getter
    protected boolean useFastMode;
    protected FastCdkFingerprinter fastCdkFingerprinter;

    public FixedFingerprinter(CdkFingerprintVersion cdkFingerprintVersion) {
        this(cdkFingerprintVersion, IFingerprinterCache.NOOP_CACHE);
    }
    public FixedFingerprinter(CdkFingerprintVersion cdkFingerprintVersion, @NotNull IFingerprinterCache cache) {
        this.cdkFingerprintVersion = cdkFingerprintVersion;
        this.iFpCache = cache;
        useFastMode=false;
    }

    public FixedFingerprinter(CdkFingerprintVersion cdkFingerprintVersion, boolean useFastMode) {
        this.cdkFingerprintVersion = cdkFingerprintVersion;
        this.iFpCache = IFingerprinterCache.NOOP_CACHE;
        setFastMode(useFastMode);
    }

    public void setFastMode(boolean useFastMode) {
        this.useFastMode = useFastMode;
        if (useFastMode) {
            this.fastCdkFingerprinter = new FastCdkFingerprinter();
            if (!fastCdkFingerprinter.mask.getMaskedFingerprintVersion().compatible(cdkFingerprintVersion)) {
                throw new RuntimeException("Cdk fingerprint not compatible with fast fingerprint");
            }
        } else {
            this.fastCdkFingerprinter = null;
        }
    }

    public ArrayFingerprint computeFingerprintFromSMILES(String smiles) {
        if (fastCdkFingerprinter!=null) return fastCdkFingerprinter.getArrayFingerprint(FixedFingerprinter.parseStructureFromStandardizedSMILES(smiles));
        return new BooleanFingerprint(cdkFingerprintVersion, new FixedFingerprinterInstance(FixedFingerprinter.parseStructureFromStandardizedSMILES(smiles),false).getAsBooleanArray()).asArray();
    }

    @Deprecated
    public ArrayFingerprint computeFingerprint(String inchi) {
        if (fastCdkFingerprinter!=null) throw new RuntimeException("Never compute fingerprints from InChI!");
        return new BooleanFingerprint(cdkFingerprintVersion, new FixedFingerprinterInstance(inchi).getAsBooleanArray()).asArray();
    }
    @Deprecated
    public ArrayFingerprint computeFingerprint(InChI inchi) {
        if (fastCdkFingerprinter!=null) throw new RuntimeException("Never compute fingerprints from InChI!");
        return computeFingerprint(inchi.in2D);
    }
    public ArrayFingerprint computeFingerprint(IAtomContainer molecule) {
        try {
            if (fastCdkFingerprinter!=null) return fastCdkFingerprinter.getArrayFingerprint(molecule.clone());
            return new BooleanFingerprint(cdkFingerprintVersion, new FixedFingerprinterInstance(molecule.clone(),false).getAsBooleanArray()).asArray();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public class FixedFingerprinterInstance {

        protected final IAtomContainer molecule;
        protected final BitSet[] fingerprints;
        protected boolean implicit;

        protected Set<IBond> cdkAromaticBonds;

        protected FixedFingerprinterInstance(String inchi) {
            this(parseInchi(inchi),true);
        }

        protected FixedFingerprinterInstance(IAtomContainer molecule, boolean hotfix) {
            try {
                this.molecule = molecule;//.clone();
                this.fingerprints = new BitSet[cdkFingerprintVersion.numberOfFingerprintTypesInUse()];
                initialize(hotfix);
            } catch (CDKException e) {
                throw new RuntimeException(e);
            }
        }

        public boolean[] getAsBooleanArray() {
            computeFingerprintsWithImplicitHydrogens();
            computeFingerprintsWithExplicitHydrogens();
            return fingerprintsToBooleans(fingerprints);

        }

        private boolean[] fingerprintsToBooleans(BitSet[] bitSets) {
            final boolean[] bits = new boolean[cdkFingerprintVersion.size()];
            int j=0;
            for (int i=0; i < bitSets.length; ++i) {
                final BitSet b = bitSets[i];
                final int N = cdkFingerprintVersion.getFingerprintTypeAt(i).length;
                for (int k = 0; k < N; ++k) {
                    if (b.get(k)) bits[j] = true;
                    ++j;
                }
            }
            return bits;
        }

        protected void computeFingerprintsWithImplicitHydrogens() {
            if (!implicit) throw new RuntimeException("Wrong order.");
            for (int k=0; k < fingerprints.length; ++k) {
                if (!cdkFingerprintVersion.getFingerprintTypeAt(k).requiresExplicitHydrogens)
                    computeFp(k, cdkFingerprintVersion.getFingerprintTypeAt(k));
            }
        }

        private void computeFp(int k, CdkFingerprintVersion.USED_FINGERPRINTS fingerprintType) {
            try {
                if (fingerprintType.requiresAromaticityPerception)
                    perceiveAromaticity();
                this.fingerprints[k] = iFpCache.applyFingerprinter(fingerprintType, ifp -> ifp.getFingerprint(molecule));
            } catch (CDKException e) {
                throw new RuntimeException(e);
            }
        }

        private void perceiveAromaticity() {
            try {
                if (cdkAromaticBonds==null) {
                    final CycleFinder cycles = Cycles.or(Cycles.all(), Cycles.all(6));
                    final Aromaticity aromaticity = new Aromaticity(ElectronDonation.daylight(), cycles);
                    cdkAromaticBonds = aromaticity.findBonds(molecule);
                }
                // clear existing flags
                molecule.setFlag(ISAROMATIC, false);
                for (IBond bond : molecule.bonds())
                    bond.setIsAromatic(false);
                for (IAtom atom : molecule.atoms())
                    atom.setIsAromatic(false);

                // set the new flags
                for (final IBond bond : cdkAromaticBonds) {
                    bond.setIsAromatic(true);
                    bond.getBegin().setIsAromatic(true);
                    bond.getEnd().setIsAromatic(true);
                }
                molecule.setFlag(ISAROMATIC, !cdkAromaticBonds.isEmpty());
            } catch (CDKException e) {
                throw new RuntimeException(e);
            }

        }

        protected void computeFingerprintsWithExplicitHydrogens() {
            AtomContainerManipulator.convertImplicitToExplicitHydrogens(molecule);
            cdkAromaticBonds = null;
            implicit = false;
            for (int k=0; k < fingerprints.length; ++k) {
                if (cdkFingerprintVersion.getFingerprintTypeAt(k).requiresExplicitHydrogens)
                    computeFp(k, cdkFingerprintVersion.getFingerprintTypeAt(k));
            }
        }



        private void initialize(boolean hotfix) throws CDKException {
            initializeMolecule(molecule,hotfix);
            this.implicit = true;
        }

    }

    public static boolean removeStrangeImidoSubstructure(IAtomContainer molecule) {
        try {
            final SMARTSQueryTool tool = new SMARTSQueryTool("[OH;$([OH]-[#6]=[#7;D2])]",SilentChemObjectBuilder.getInstance());
            if (tool.matches(molecule)) {
                final TIntHashSet indizes = new TIntHashSet(4);
                for (List<Integer> li : tool.getMatchingAtoms()) {
                    indizes.addAll(li);
                }
                boolean changedSomething = false;

                forEachOxygen:
                for (int index : indizes.toArray()) {
                    final IAtom oxygen = molecule.getAtom(index);
                    if (8!=oxygen.getAtomicNumber())
                        throw new RuntimeException("Something goes terribly wrong...");
                    for (IBond oxybond : molecule.getConnectedBondsList(oxygen)) {
                        if (oxybond.getOrder()!= IBond.Order.SINGLE)
                            continue; // something is wrong here
                        final IAtom carbon = oxybond.getOther(oxygen);
                        if (6 != carbon.getAtomicNumber())
                            throw new RuntimeException("Something goes terribly wrong...");
                        for (IBond nitroBond : molecule.getConnectedBondsList(carbon)) {

                            final IAtom nitro = nitroBond.getOther(carbon);
                            if (7  == nitro.getAtomicNumber() && nitroBond.getOrder()== IBond.Order.DOUBLE) {
                                // we have everything we need!
                                oxybond.setOrder(IBond.Order.DOUBLE);
                                oxygen.setImplicitHydrogenCount(0);
                                nitroBond.setOrder(IBond.Order.SINGLE);
                                nitro.setImplicitHydrogenCount(nitro.getImplicitHydrogenCount()+1);
                                changedSomething = true;
                                continue forEachOxygen;
                            }

                        }
                    }
                }
                // everything alright?
                if (changedSomething) {
                    AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
                    return true;
                } else return false;
            } else return false;
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    private static void initializeMolecule(IAtomContainer molecule, boolean hotfix) throws CDKException {
        CDKHydrogenAdder adder = CDKHydrogenAdder.getInstance(SilentChemObjectBuilder.getInstance());
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
        adder.addImplicitHydrogens(molecule);
        if (hotfix) removeStrangeImidoSubstructure(molecule);
    }

    public static IAtomContainer parseNormalizedStructure(String __inchi) {
        IAtomContainer mol = parseInchi(__inchi);
        try {
            initializeMolecule(mol,true);
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
        return mol;
    }

    public static IAtomContainer parseStructureFromStandardizedSMILES(String smiles) {
        try {
            IAtomContainer mol = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(smiles);
            initializeMolecule(mol,false);
            return mol;
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    public static IAtomContainer parseInchi(String __inchi) {
        return parseInchi(__inchi, true);
    }

    public static IAtomContainer parseInchi(String __inchi, boolean standardizeInChI) {
        String inchi = InChIs.inchi2d(__inchi);
        if (standardizeInChI && inchi.startsWith("InChI=1/")) {
            try {
                LoggerFactory.getLogger(FixedFingerprinter.class).warn("Fix non-standard InChI '" + inchi + "'");
                IAtomContainer m = InChISMILESUtils.getAtomContainerFromInchi(inchi);//structureGenerator.getAtomContainer();
                inchi = InChISMILESUtils.getInchi(m, false).in2D;
            } catch (CDKException e) {
                LoggerFactory.getLogger(FixedFingerprinter.class).warn(e.getMessage());
            }
        }

        if (inchi.isEmpty())
            throw new RuntimeException("Cannot parse InChI: " + __inchi);
        if (!inchi.startsWith("InChI=1S/"))
            throw new RuntimeException("Non-standard InChI detected. Cannot parse InChI: " + __inchi);

        try {
            return InChISMILESUtils.getAtomContainerFromInchi(inchi);
        } catch (CDKException e) {
            throw new RuntimeException("Cannot parse InChI: " + __inchi);
        }
    }

}
