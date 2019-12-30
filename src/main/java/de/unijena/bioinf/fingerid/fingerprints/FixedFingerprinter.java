package de.unijena.bioinf.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.BooleanFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.fingerid.Fingerprinter;
import gnu.trove.set.hash.TIntHashSet;
import net.sf.jniinchi.INCHI_RET;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
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

    protected CdkFingerprintVersion cdkFingerprintVersion;

    public FixedFingerprinter(CdkFingerprintVersion cdkFingerprintVersion) {
        this.cdkFingerprintVersion = cdkFingerprintVersion;
    }

    public ArrayFingerprint computeFingerprintFromSMILES(String smiles) {
        return new BooleanFingerprint(cdkFingerprintVersion, new FixedFingerprinterInstance(FixedFingerprinter.parseStructureFromStandardizedSMILES(smiles),false).getAsBooleanArray()).asArray();
    }

    public ArrayFingerprint computeFingerprint(String inchi) {
        return new BooleanFingerprint(cdkFingerprintVersion, new FixedFingerprinterInstance(inchi).getAsBooleanArray()).asArray();
    }
    public ArrayFingerprint computeFingerprint(InChI inchi) {
        return computeFingerprint(inchi.in2D);
    }
    public ArrayFingerprint computeFingerprint(IAtomContainer molecule) {
        try {
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
                this.fingerprints[k] = Fingerprinter.getFingerprinter(fingerprintType).getFingerprint(molecule);
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
        String inchi = InChI.inchi2d(__inchi);
        if (standardizeInChI && inchi.startsWith("InChI=1/")) {
            try {
                LoggerFactory.getLogger(FixedFingerprinter.class).warn("Fix non-standard InChI '" + inchi + "'");
                final InChIToStructure structureGenerator = InChIGeneratorFactory.getInstance().getInChIToStructure(inchi, SilentChemObjectBuilder.getInstance());
                IAtomContainer m = structureGenerator.getAtomContainer();
                inchi = InChI.inchi2d(InChIGeneratorFactory.getInstance().getInChIGenerator(m).getInchi());
            } catch (CDKException e) {
                LoggerFactory.getLogger(FixedFingerprinter.class).warn(e.getMessage());
            }
        }
        if (inchi.isEmpty() || !inchi.startsWith("InChI=1S/")) {
            throw new RuntimeException("Cannot parse InChI: " + __inchi);
        }
        if (!inchi.startsWith("InChI=1S/")) {
            LoggerFactory.getLogger(FixedFingerprinter.class).warn("Non-standard InChI detected. This might result into inaccurate fingerprint computation! " + __inchi);
        }
        try {
            final InChIToStructure structureGenerator = InChIGeneratorFactory.getInstance().getInChIToStructure(inchi, SilentChemObjectBuilder.getInstance());
            final INCHI_RET returnStatus = structureGenerator.getReturnStatus();
            if (returnStatus != INCHI_RET.OKAY) {
                if (returnStatus==INCHI_RET.ERROR || returnStatus==INCHI_RET.FATAL || structureGenerator.getAtomContainer()==null)
                    throw new RuntimeException("Cannot parse InChI: " + __inchi);
                LoggerFactory.getLogger(FixedFingerprinter.class).warn("InChI parser returns a warning while parsing '" + __inchi + "': " + structureGenerator.getMessage() );
            }
            return structureGenerator.getAtomContainer();
        } catch (CDKException e) {
            throw new RuntimeException("Cannot parse InChI: " + __inchi);
        }
    }

}
