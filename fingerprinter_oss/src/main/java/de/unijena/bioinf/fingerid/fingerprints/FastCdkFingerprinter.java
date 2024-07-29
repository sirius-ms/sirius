package de.unijena.bioinf.fingerid.fingerprints;

import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.fingerid.Mask;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.*;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smarts.SmartsPattern;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class FastCdkFingerprinter {

    private final static int[] fingerprintIndizes;
    private final static CdkFingerprintVersion CDK_FINGERPRINT_VERSION = CdkFingerprintVersion.getDefault();
    private final SmartsPattern[] patterns;
    private final int[] substructureIndizes;
    private final Int2IntOpenHashMap countMap;

    private final int ringCountIndex;
    private final int[] ecfpIndizes;
    private final int ecfpBreak;

    static {
        IntArrayList indizes = new IntArrayList();
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(FastCdkFingerprinter.class.getResourceAsStream("/fingerprints/fingerprint_indizes_v6.txt")))) {
            String line;
            while ((line=br.readLine())!=null) {
                indizes.add(Integer.parseInt(line));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        fingerprintIndizes = indizes.toIntArray();
    }

    protected MaskedFingerprintVersion mask;

    public static void main(String[] args) {
        MaskedFingerprintVersion.Builder builder = MaskedFingerprintVersion.buildMaskFor(CdkFingerprintVersion.getDefault());
        builder.disableAll();
        for (int index : fingerprintIndizes) builder.enable(index);
        MaskedFingerprintVersion maskedFingerprintVersion = builder.toMask();
        try {
            for (String line : FileUtils.readLines(new File("/home/kaidu/analysis/newfp/all_train_structures.txt"))) {
                String[] cols = line.split("\t");
                //if (!cols[0].equals("AABZZWPMCAZHFC")) continue;
                IAtomContainer molecule = FixedFingerprinter.parseStructureFromStandardizedSMILES(cols[1]);
                final long _T_ = System.currentTimeMillis();
                String a = new FastCdkFingerprinter().getArrayFingerprint(molecule).toCommaSeparatedString();
                final long _T_2 = System.currentTimeMillis();
                ArrayFingerprint fp = new FixedFingerprinter(CdkFingerprintVersion.getDefault()).computeFingerprint(molecule);
                ArrayFingerprint fp2 = maskedFingerprintVersion.mask(fp);
                String b = fp2.toCommaSeparatedString();
                final long _T_3 = System.currentTimeMillis();
                System.out.println((_T_2 - _T_) + " vs " + (_T_3 - _T_2));
                BooleanFingerprint fp3 = new FastCdkFingerprinter().getBooleanFingerprint(molecule);

                String c = fp3.toCommaSeparatedString();
                if (!a.equals(b)) {
                    System.out.println(line);
                    System.out.println(a);
                    System.out.println(b);
                    System.out.println("\n");
                }
                if (!c.equals(a)) {
                    System.out.println("BOOLEAN ARRAY MISSMATCH!!!!!!!!!11111");
                    System.out.println(line);
                    System.out.println(a);
                    System.out.println(c);
                    System.out.println("\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public MaskedFingerprintVersion getMask() {
        return mask;
    }

    public FastCdkFingerprinter() {
        MaskedFingerprintVersion.Builder b = MaskedFingerprintVersion.buildMaskFor(CDK_FINGERPRINT_VERSION);
        b.disableAll();
        for (int i : fingerprintIndizes) b.enable(i);
        mask = b.toMask();
        final List<String> pats = new ArrayList<>();
        final IntArrayList patsI = new IntArrayList();
        this.countMap = new Int2IntOpenHashMap(12, 0.75f);
        countMap.defaultReturnValue(1);
        final IntArrayList ecfpInds = new IntArrayList();
        int ecfpbreak = Integer.MAX_VALUE;
        for (int k : mask.allowedIndizes()) {
            MolecularProperty prop = mask.getMolecularProperty(k);
            if (prop instanceof SubstructureProperty) {
                pats.add(((SubstructureProperty) prop).getSmarts());
                patsI.add(k);
                if (prop instanceof SubstructureCountProperty) {
                    this.countMap.put(k, ((SubstructureCountProperty) prop).getMinimalCount());
                }
            } else if (prop instanceof ExtendedConnectivityProperty) {

                ecfpInds.add(k);
                ecfpbreak = Math.min(ecfpbreak, pats.size());
            }
        }
        this.ecfpBreak = ecfpbreak;
        this.ecfpIndizes = ecfpInds.toIntArray();
        this.patterns = pats.stream().map(x->SmartsPattern.create(x, SilentChemObjectBuilder.getInstance())).toArray(SmartsPattern[]::new);
        this.substructureIndizes = patsI.toIntArray();
        int breakForPubChem = 0;
        final int pubchemOffset = CDK_FINGERPRINT_VERSION.getOffsetFor(CdkFingerprintVersion.USED_FINGERPRINTS.PUBCHEM) + 147;
        for (int k=0; k < substructureIndizes.length; ++k) {
            if (substructureIndizes[k] >= pubchemOffset) {
                breakForPubChem = k;
                break;
            }
        }
        this.ringCountIndex = breakForPubChem;

    }

    public ArrayFingerprint getFingerprint(IAtomContainer molecule) {
        return getArrayFingerprint(molecule);
    }

    public ArrayFingerprint getArrayFingerprint(IAtomContainer molecule) {
        return new ArrayFingerprint(mask, getShortArray(molecule));
    }

    public BooleanFingerprint getBooleanFingerprint(IAtomContainer molecule) {
        return new BooleanFingerprint(mask, getBooleanArray(molecule));
    }

    public boolean[] getBooleanArray(IAtomContainer molecule) {
        final boolean[] vec = new boolean[fingerprintIndizes.length];
        computeGenericFingerprint(molecule, (i,j)->vec[j]=true);
        return vec;
    }

    public short[] getShortArray(IAtomContainer molecule) {
        ShortArrayList list = new ShortArrayList(64);
        computeGenericFingerprint(molecule, (i,j)->list.add((short)i));
        return list.toShortArray();
    }

    public void computeGenericFingerprint(IAtomContainer molecule, SetFp callback) {
        SmartsPattern.prepare(molecule);
        countSubstructures(molecule, 0, ringCountIndex, callback);
        countRings(molecule, callback);
        countSubstructures(molecule, ringCountIndex, this.ecfpBreak, callback);
        countEcfp(molecule, callback);
        countSubstructures(molecule, this.ecfpBreak, patterns.length, callback);
    }

    private void countEcfp(IAtomContainer molecule, SetFp callback) {
        ECFPFingerprinter ecfpFingerprinter = new ECFPFingerprinter();
        IBitFingerprint bitFingerprint = null;
        try {
            int j=mask.getRelativeIndexOf(ecfpIndizes[0]);
            bitFingerprint = ecfpFingerprinter.getBitFingerprint(molecule);
            final int ecfpOffset = CDK_FINGERPRINT_VERSION.getOffsetFor(CdkFingerprintVersion.USED_FINGERPRINTS.ECFP);
            for (int i : ecfpIndizes) {
                if (bitFingerprint.get(i-ecfpOffset)) {
                    callback.set(i, j);
                }
                ++j;
            }
        } catch (CDKException e) {
            throw new RuntimeException(e);
        }
    }

    private void countSubstructures(IAtomContainer molecule, int from, int to, SetFp callback) {
        int j = mask.getRelativeIndexOf(substructureIndizes[from]);
        for (int k=from; k < to; ++k) {
            final int index = substructureIndizes[k];
            final int minc = countMap.get(index);
            if (minc <= 1) {
                if (patterns[k].matches(molecule)) callback.set(index, j);
            } else {
                if (patterns[k].matchAll(molecule).uniqueAtoms().atLeast(minc)) callback.set(index, j);
            }
            ++j;
        }
    }

    public interface SetFp {
        public void set(int absoluteIndex, int relativeIndex);
    }

    /*
     * Section 2: Rings in a canonic ESSR ring set-These bs test for the
     * presence or count of the described chemical ring system. An ESSR ring is
     * any ring which does not share three consecutive atoms with any other ring
     * in the chemical structure. For example, naphthalene has three ESSR rings
     * (two phenyl fragments and the 10-membered envelope), while biphenyl will
     * yield a count of only two ESSR rings.
     */
    private void countRings(IAtomContainer mol, SetFp callback) {
        CountRings cr = new CountRings(mol);
        final int offset = CDK_FINGERPRINT_VERSION.getOffsetFor(CdkFingerprintVersion.USED_FINGERPRINTS.PUBCHEM);
        int b;
        b = 147;
        int j = mask.getRelativeIndexOf(offset+b);
        if (cr.countUnsaturatedCarbonOnlyRing(5) >= 1) callback.set(b+offset, j);
        b = 149; ++j;
        if (cr.countUnsaturatedHeteroContainingRing(5) >= 1) callback.set(b+offset, j);
        b = 153; ++j;
        if (cr.countSaturatedOrAromaticHeteroContainingRing(5) >= 2) callback.set(b+offset, j);
        b = 157; ++j;
        if (cr.countAnyRing(5) >= 3) callback.set(b+offset, j);
        b = 184; ++j;
        if (cr.countUnsaturatedHeteroContainingRing(6) >= 1) callback.set(b+offset, j);
        b = 186; ++j;
        if (cr.countSaturatedOrAromaticCarbonOnlyRing(6) >= 2) callback.set(b+offset, j);
        b = 187; ++j;
        if (cr.countSaturatedOrAromaticNitrogenContainingRing(6) >= 2) callback.set(b+offset, j);
        b = 188; ++j;
        if (cr.countSaturatedOrAromaticHeteroContainingRing(6) >= 2) callback.set(b+offset, j);
        b = 191; ++j;
        if (cr.countUnsaturatedHeteroContainingRing(6) >= 2) callback.set(b+offset, j);
        b = 192; ++j;
        if (cr.countAnyRing(6) >= 3) callback.set(b+offset, j);
        b = 193; ++j;
        if (cr.countSaturatedOrAromaticCarbonOnlyRing(6) >= 3) callback.set(b+offset, j);
        b = 194; ++j;
        if (cr.countSaturatedOrAromaticNitrogenContainingRing(6) >= 3) callback.set(b+offset, j);
        b = 195; ++j;
        if (cr.countSaturatedOrAromaticHeteroContainingRing(6) >= 3) callback.set(b+offset, j);
        b = 199; ++j;
        if (cr.countAnyRing(6) >= 4) callback.set(b+offset, j);
        b = 200; ++j;
        if (cr.countSaturatedOrAromaticCarbonOnlyRing(6) >= 4) callback.set(b+offset, j);
        b = 206; ++j;
        if (cr.countAnyRing(6) >= 5) callback.set(b+offset, j);
        b = 207; ++j;
        if (cr.countSaturatedOrAromaticCarbonOnlyRing(6) >= 5) callback.set(b+offset, j);
        b = 218; ++j;
        if (cr.countUnsaturatedNitrogenContainingRing(7) >= 1) callback.set(b+offset, j);
        b = 257;  ++j; ///// ???? 256
        if (cr.countAromaticRing() >= 2) callback.set(b+offset, j);
        b = 258; ++j;
        if (cr.countHeteroAromaticRing() >= 2) callback.set(b+offset, j);
        b = 259; ++j;
        if (cr.countAromaticRing() >= 3) callback.set(b+offset, j);
        b = 260; ++j;
        if (cr.countHeteroAromaticRing() >= 3) callback.set(b+offset, j);
        b = 261; ++j;
        if (cr.countAromaticRing() >= 4) callback.set(b+offset, j);
    }


    private static class CountRings {

        int[][]  sssr = {};
        final IRingSet ringSet;

        public CountRings(IAtomContainer m) {
            ringSet = Cycles.sssr(m).toRingSet();
        }

        public int countAnyRing(int size) {
            int c = 0;
            for (IAtomContainer ring : ringSet.atomContainers()) {
                if (ring.getAtomCount() == size) c++;
            }
            return c;
        }

        private boolean isCarbonOnlyRing(IAtomContainer ring) {
            for (IAtom ringAtom : ring.atoms()) {
                if (ringAtom.getAtomicNumber() != IElement.C) return false;
            }
            return true;
        }

        private boolean isRingSaturated(IAtomContainer ring) {
            for (IBond ringBond : ring.bonds()) {
                if (ringBond.getOrder() != IBond.Order.SINGLE || ringBond.getFlag(CDKConstants.ISAROMATIC)
                        || ringBond.getFlag(CDKConstants.SINGLE_OR_DOUBLE)) return false;
            }
            return true;
        }

        private boolean isRingUnsaturated(IAtomContainer ring) {
            return !isRingSaturated(ring);
        }

        private int countNitrogenInRing(IAtomContainer ring) {
            int c = 0;
            for (IAtom ringAtom : ring.atoms()) {
                if (ringAtom.getAtomicNumber() == IElement.N) c++;
            }
            return c;
        }

        private int countHeteroInRing(IAtomContainer ring) {
            int c = 0;
            for (IAtom ringAtom : ring.atoms()) {
                if (ringAtom.getAtomicNumber() != IElement.C && ringAtom.getAtomicNumber() != IElement.H) c++;
            }
            return c;
        }

        private boolean isAromaticRing(IAtomContainer ring) {
            for (IBond bond : ring.bonds())
                if (!bond.getFlag(CDKConstants.ISAROMATIC)) return false;
            return true;
        }

        public int countAromaticRing() {
            int c = 0;
            for (IAtomContainer ring : ringSet.atomContainers()) {
                if (isAromaticRing(ring)) c++;
            }
            return c;
        }

        public int countHeteroAromaticRing() {
            int c = 0;
            for (IAtomContainer ring : ringSet.atomContainers()) {
                if (!isCarbonOnlyRing(ring) && isAromaticRing(ring)) c++;
            }
            return c;
        }

        public int countSaturatedOrAromaticCarbonOnlyRing(int size) {
            int c = 0;
            for (IAtomContainer ring : ringSet.atomContainers()) {
                if (ring.getAtomCount() == size && isCarbonOnlyRing(ring)
                        && (isRingSaturated(ring) || isAromaticRing(ring))) c++;
            }
            return c;
        }

        public int countSaturatedOrAromaticNitrogenContainingRing(int size) {
            int c = 0;
            for (IAtomContainer ring : ringSet.atomContainers()) {
                if (ring.getAtomCount() == size && (isRingSaturated(ring) || isAromaticRing(ring))
                        && countNitrogenInRing(ring) > 0) ++c;
            }
            return c;
        }

        public int countSaturatedOrAromaticHeteroContainingRing(int size) {
            int c = 0;
            for (IAtomContainer ring : ringSet.atomContainers()) {
                if (ring.getAtomCount() == size && (isRingSaturated(ring) || isAromaticRing(ring))
                        && countHeteroInRing(ring) > 0) ++c;
            }
            return c;
        }

        public int countUnsaturatedCarbonOnlyRing(int size) {
            int c = 0;
            for (IAtomContainer ring : ringSet.atomContainers()) {
                if (ring.getAtomCount() == size && isRingUnsaturated(ring) && !isAromaticRing(ring)
                        && isCarbonOnlyRing(ring)) ++c;
            }
            return c;
        }

        public int countUnsaturatedNitrogenContainingRing(int size) {
            int c = 0;
            for (IAtomContainer ring : ringSet.atomContainers()) {
                if (ring.getAtomCount() == size && isRingUnsaturated(ring) && !isAromaticRing(ring)
                        && countNitrogenInRing(ring) > 0) ++c;
            }
            return c;
        }

        public int countUnsaturatedHeteroContainingRing(int size) {
            int c = 0;
            for (IAtomContainer ring : ringSet.atomContainers()) {
                if (ring.getAtomCount() == size && isRingUnsaturated(ring) && !isAromaticRing(ring)
                        && countHeteroInRing(ring) > 0) ++c;
            }
            return c;
        }
    }


}
