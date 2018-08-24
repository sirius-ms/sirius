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

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.fingerid.fingerprints.*;
import de.unijena.bioinf.fingerid.fingerprints.ShortestPathFingerprinter;
import net.sf.jniinchi.INCHI_RET;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.*;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Fingerprinter {

    private final InChIGeneratorFactory factory;
    private final List<IFingerprinter> fingerprinters;

    public Fingerprinter() throws CDKException {
        factory = InChIGeneratorFactory.getInstance();
        this.fingerprinters = createListOfFingerprints();
    }

    public static Fingerprinter getForVersion(CdkFingerprintVersion version) throws CDKException {
        final List<IFingerprinter> fingerprinters = new ArrayList<>();
        for (int k=0; k < version.numberOfFingerprintTypesInUse(); ++k) {
            fingerprinters.add(getFingerprinter(version.getFingerprintTypeAt(k)));
        }
        return new Fingerprinter(fingerprinters);
    }

    public Fingerprinter(List<IFingerprinter> fingerprinters) throws CDKException {
        factory = InChIGeneratorFactory.getInstance();
        this.fingerprinters = fingerprinters;
    }

    public static IFingerprinter getFingerprinter(CdkFingerprintVersion.USED_FINGERPRINTS fp) {
        switch (fp) {
            case OPENBABEL: return new OpenBabelFingerprinter();
            case SUBSTRUCTURE: return new SubstructureFingerprinter();
            case MACCS: return new MACCSFingerprinter();
            case PUBCHEM: return new PubchemFingerprinter(DefaultChemObjectBuilder.getInstance());
            case KLEKOTA_ROTH: return new KlekotaRothFingerprinter();
            case ECFP: return new ECFPFingerprinter();
            case CLASSYFIRE_SMARTS: return new ClassyFireSmartsFingerprint();
            case SHORTEST_PATH: return new ShortestPathFingerprinter();
            case BIOSMARTS: return new BiosmartsFingerprinter();
            case RINGSYSTEMS: return new RingsystemFingerprinter();
            default: throw new IllegalArgumentException();
        }
    }

    public static IFingerprinter getFingerprinterByName(String name) {
        switch (name.toLowerCase()) {
            case "openbabel": return new OpenBabelFingerprinter();
            case "substructure": return new SubstructureFingerprinter();
            case "maccs": return new MACCSFingerprinter();
            case "pubchem": return new PubchemFingerprinter(DefaultChemObjectBuilder.getInstance());
            case "klekota":
            case "klekota_roth": return new KlekotaRothFingerprinter();
            case "path": return new MarcusPathFingerprinter();
            case "neighbours": return new NeighbourhoodFingerprinter();
            case "spheres": return new SphericalFingerprint();
            case "ecfp": return new ECFPFingerprinter();
            default: throw new IllegalArgumentException("Unknown fingerprinter: " + name);
        }
    }

    public static List<IFingerprinter> createListOfAllFingerprints() {
        return Arrays.asList(
                (IFingerprinter) new OpenBabelFingerprinter(), // 55 (0..54)
                (IFingerprinter) new SubstructureFingerprinter(), // 307 (55..361)
                (IFingerprinter) new MACCSFingerprinter(),// 166 (362..527)
                (IFingerprinter) new PubchemFingerprinter(
                        DefaultChemObjectBuilder.getInstance()), // 881 (528..1408)
                (IFingerprinter) new KlekotaRothFingerprinter(), // 4860 (1409..6269)
                (IFingerprinter) new SphericalFingerprint(),
                (IFingerprinter) new ECFPFingerprinter()
        );
    }

    public static List<IFingerprinter> createListOfFingerprints() {
        return Arrays.asList(
                (IFingerprinter) new OpenBabelFingerprinter(),
                (IFingerprinter) new SubstructureFingerprinter(),
                (IFingerprinter) new MACCSFingerprinter(),
                (IFingerprinter) new PubchemFingerprinter(DefaultChemObjectBuilder.getInstance()),
                (IFingerprinter) new KlekotaRothFingerprinter(),
                (IFingerprinter) new ECFPFingerprinter()
                );
    }

    public static List<IFingerprinter> createExtendedListOfFingerprints() {
        return Arrays.asList(
                (IFingerprinter) new OpenBabelFingerprinter(),
                (IFingerprinter) new SubstructureFingerprinter(),
                (IFingerprinter) new MACCSFingerprinter(),
                (IFingerprinter) new PubchemFingerprinter(DefaultChemObjectBuilder.getInstance()),
                (IFingerprinter) new KlekotaRothFingerprinter(),
                (IFingerprinter) new SphericalFingerprint()
        );
    }

    public String fingerprintsToString(BitSet[] bitSets) {
        int n=0;
        for (BitSet b : bitSets) n += b.size();
        final StringBuilder buffer = new StringBuilder(n);
        for (int i=0; i < bitSets.length; ++i) {
            final BitSet b = bitSets[i];
            final int N = fingerprinters.get(i).getSize();
            for (int k = 0; k < N; ++k) {
                if (b.get(k)) buffer.append('1');
                else buffer.append('0');
            }
        }
        return buffer.toString();
    }

    public boolean[] fingerprintsToBooleans(BitSet[] bitSets) {
        int n=0;
        for (BitSet b : bitSets) n += b.size();
        int fpn = 0;
        for (IFingerprinter f : fingerprinters) fpn += f.getSize();
        final boolean[] bits = new boolean[fpn];
        int j=0;
        for (int i=0; i < bitSets.length; ++i) {
            final BitSet b = bitSets[i];
            final int N = fingerprinters.get(i).getSize();
            for (int k = 0; k < N; ++k) {
                if (b.get(k)) bits[j] = true;
                ++j;
            }
        }
        return bits;
    }

    public static String booleanToString(boolean[] fingerprint) {
        final char[] values = new char[fingerprint.length];
        Arrays.fill(values, '0');
        for (int k=0; k < fingerprint.length; ++k)
            if (fingerprint[k])
                values[k] = '1';
        return new String(values);
    }

    public static boolean[] stringToBoolean(String fingerprint) {
        final boolean[] values = new boolean[fingerprint.length()];
        for (int k=0; k < fingerprint.length(); ++k)
            if (fingerprint.charAt(k)=='1')
                values[k] = true;
        return values;
    }

    public BitSet[] computeFingerprints(IAtomContainer mol) throws CDKException {
        final BitSet[] fingerprints = new BitSet[fingerprinters.size()];
        int k=0;
        for (IFingerprinter fp : fingerprinters) {
            fingerprints[k] = fp.getBitFingerprint(mol).asBitSet();
            assert (fingerprints[k].size() >= fp.getSize() && fingerprints[k].length() <= fp.getSize());
            ++k;
        }
        return fingerprints;
    }

    public String convert3Dto2DInchi(String inchi) {
        for (int k=0; k < inchi.length(); ++k) {
            if (inchi.charAt(k)=='/') {
                if (k+1 < inchi.length()) {
                    final char type = inchi.charAt(k+1);
                    if (type=='b' || type=='t' || type=='r' || type=='s' || type=='m' || type=='i' || type=='f') {
                        return inchi.substring(0, k);
                    }
                }
            }
        }
        return inchi;
    }

    public IAtomContainer convertInchi2Mol(String inchi) throws CDKException {
        if (inchi == null) throw new NullPointerException("Given InChI is null");
        if (inchi.isEmpty()) throw new IllegalArgumentException("Empty string given as InChI");
        final InChIToStructure converter = factory.getInChIToStructure(inchi, DefaultChemObjectBuilder.getInstance());
        if (converter.getReturnStatus() == INCHI_RET.OKAY) return converter.getAtomContainer();       else if (converter.getReturnStatus()==INCHI_RET.WARNING) {
            LoggerFactory.getLogger(Fingerprinter.class).warn(converter.getMessage());
            return converter.getAtomContainer();
        }
        else {
            LoggerFactory.getLogger(Fingerprinter.class).error("Error while parsing InChI:\n'" + inchi +"'\n-> " + converter.getMessage());
            final IAtomContainer a = converter.getAtomContainer();
            if (a!=null) return a;
            else throw new CDKException(converter.getMessage());
        }
    }

    public List<IFingerprinter> getFingerprinters() {
        return Collections.unmodifiableList(fingerprinters);
    }

    public IAtomContainer convertInchi2Mol(String inchi, boolean is3D) throws CDKException {
        if (!is3D) inchi = convert3Dto2DInchi(inchi);
        return convertInchi2Mol(inchi);
    }

    public int numberOfFingerprints() {
        int s=0;
        for (IFingerprinter f : fingerprinters) s += f.getSize();
        return s;
    }


    public static Fingerprinter getFor(FingerprintVersion fingerprintVersion) throws CDKException {
        if (fingerprintVersion instanceof CdkFingerprintVersion) {
            final CdkFingerprintVersion cdk = (CdkFingerprintVersion) fingerprintVersion;
            if (cdk == CdkFingerprintVersion.getDefault()) return new Fingerprinter();
            final List<IFingerprinter> fps = new ArrayList<>();
            for (int k=0, n = cdk.numberOfFingerprintTypesInUse(); k < n ; ++k) {
                fps.add(getFingerprinter(cdk.getFingerprintTypeAt(k)));
            }
            return new Fingerprinter(fps);
        } else throw new IllegalArgumentException();
    }
}
