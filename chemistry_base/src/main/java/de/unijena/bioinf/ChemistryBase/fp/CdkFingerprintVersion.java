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

package de.unijena.bioinf.ChemistryBase.fp;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import gnu.trove.list.array.TIntArrayList;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fingerprints based on CDK.
 *
 * Fingerprints are always stored in same order!
 */
public class CdkFingerprintVersion extends FingerprintVersion {


    public static void main(String[] args) {
        System.out.println(CdkFingerprintVersion.getDefault().getOffsetFor(USED_FINGERPRINTS.MACCS));
        System.out.println(CdkFingerprintVersion.getDefault().size());
    }

    private final long fastCompareFlag;
    private final MolecularProperty[] properties;
    private final USED_FINGERPRINTS[] usedFingerprints;

    private static int[] topIndizes;

    public USED_FINGERPRINTS[] getUsedFingerprints() {
        return usedFingerprints;
    }

    protected CdkFingerprintVersion() {
        this(new USED_FINGERPRINTS[0]);
    }

    public CdkFingerprintVersion(USED_FINGERPRINTS... fingerprints) {
        final ArrayList<MolecularProperty> properties = new ArrayList<>();
        long fastCompareFlag = 0L;
        Arrays.sort(fingerprints);
        for (USED_FINGERPRINTS uf : fingerprints) {
            properties.addAll(Arrays.asList(getDefaultPropertiesFor(uf)));
            fastCompareFlag |= (1L << uf.defaultPosition);
        }
        this.fastCompareFlag = fastCompareFlag;
        this.usedFingerprints = fingerprints;
        this.properties = properties.toArray(new MolecularProperty[properties.size()]);
    }

    public MaskedFingerprintVersion getMaskFor(USED_FINGERPRINTS... fingerprints) {
        Arrays.sort(fingerprints);
        final MaskedFingerprintVersion.Builder b = de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion.buildMaskFor(this);
        b.disableAll();
        for (USED_FINGERPRINTS f : fingerprints) {
            final int i = getOffsetFor(f);
            final int j = i + f.length;
            b.enable(i,j);
        }
        return b.toMask();
    }

    public int getOffsetFor(USED_FINGERPRINTS fingerprint) {
        int offset=0;
        for (USED_FINGERPRINTS f : usedFingerprints) {
            if (f == fingerprint) return offset;
            else offset += f.length;
        }
        throw new IllegalArgumentException(fingerprint.name() + " is not part of this fingerprint version");
    }

    public long getBitsetIdentifier() {
        return fastCompareFlag;
    }

    @Override
    public String toString() {
        return "Cdk fingerprint version: " + properties.length + " bits in use (type = " + fastCompareFlag + ").";
    }

    @Override
    public MolecularProperty getMolecularProperty(int index) {
        return properties[index];
    }

    @Override
    public int size() {
        return properties.length;
    }

    @Override
    public boolean compatible(FingerprintVersion fingerprintVersion) {
        if (this == fingerprintVersion) return true;
        if (fingerprintVersion.getClass().equals(this.getClass())) {
            return fastCompareFlag == ((CdkFingerprintVersion)fingerprintVersion).fastCompareFlag;
        }
        if (fingerprintVersion instanceof MaskedFingerprintVersion) {
            final MaskedFingerprintVersion m = (MaskedFingerprintVersion)fingerprintVersion;
            return m.isNotFiltering() && compatible(m.getMaskedFingerprintVersion());
        }
        return false;
    }

    @Override
    public boolean identical(FingerprintVersion fingerprintVersion) {
        if (this == fingerprintVersion) return true;
        if (fingerprintVersion.getClass().equals(this.getClass())) {
            return fastCompareFlag == ((CdkFingerprintVersion)fingerprintVersion).fastCompareFlag;
        }
        if (fingerprintVersion instanceof MaskedFingerprintVersion) {
            final MaskedFingerprintVersion m = (MaskedFingerprintVersion)fingerprintVersion;
            return m.isNotFiltering() && identical(m.getMaskedFingerprintVersion());
        }
        return false;
    }

    private static final USED_FINGERPRINTS[] WITHOUT_ECFP_SETUP = new USED_FINGERPRINTS[]{
            USED_FINGERPRINTS.OPENBABEL, USED_FINGERPRINTS.SUBSTRUCTURE, USED_FINGERPRINTS.MACCS, USED_FINGERPRINTS.PUBCHEM, USED_FINGERPRINTS.KLEKOTA_ROTH
    };

    /**
     * will be replaced by complete setup as soon as fingerprints are imported into the database.
     */
    private static final USED_FINGERPRINTS[] EXTENDED_SETUP = new USED_FINGERPRINTS[]{
            USED_FINGERPRINTS.OPENBABEL, USED_FINGERPRINTS.SUBSTRUCTURE, USED_FINGERPRINTS.MACCS, USED_FINGERPRINTS.PUBCHEM, USED_FINGERPRINTS.KLEKOTA_ROTH, USED_FINGERPRINTS.ECFP, USED_FINGERPRINTS.BIOSMARTS, USED_FINGERPRINTS.RINGSYSTEMS, USED_FINGERPRINTS.INSILICO};

    /**
     * version string to reference the exact fingerprint computation code in the database (fp_version table)
     * TODO UPDATE everytime anything is changed on the code base of the fingerprint computation
     */
    public static final String DEFAULT_SETUP_VERSION = "2023-08-04";
    private static final USED_FINGERPRINTS[] DEFAULT_SETUP = new USED_FINGERPRINTS[]{
        USED_FINGERPRINTS.OPENBABEL, USED_FINGERPRINTS.SUBSTRUCTURE, USED_FINGERPRINTS.MACCS, USED_FINGERPRINTS.PUBCHEM, USED_FINGERPRINTS.KLEKOTA_ROTH, USED_FINGERPRINTS.ECFP, USED_FINGERPRINTS.BIOSMARTS, USED_FINGERPRINTS.RINGSYSTEMS, USED_FINGERPRINTS.INSILICO};


    public enum USED_FINGERPRINTS {
        OPENBABEL(0, 55,true,false), SUBSTRUCTURE(1, 307,false,false), MACCS(2, 166,false,true), PUBCHEM(3, 881,true,true), KLEKOTA_ROTH(4, 4860,false,false),
        ECFP(5, ExtendedConnectivityProperty.getFingerprintLength(),false,false),

        BIOSMARTS(6, 283, false, true),
        RINGSYSTEMS(7, 463, false, true),
        INSILICO(8, 9104, false, true);

        public final int defaultPosition, length;
        /*
        if true, the fingerprint type expects that all hydrogens in the molecule are explicit. If false, the fingerprint
        either expects implicit hydrogens or is invariant.
         */
        public final boolean requiresExplicitHydrogens;

        /*
        If true, the fingerprint expects that Aromaticity.cdkLegacy() is performed before computing the fingerprint. If false,
        the fingerprint ensures itself that the correct aromaticity model is performed.
         */
        public final boolean requiresAromaticityPerception;

        USED_FINGERPRINTS(int defaultPosition, int length, boolean requiresExplicitHydrogens, boolean requiresAromaticityPerception) {
            this.defaultPosition = defaultPosition;
            this.length = length;
            this.requiresExplicitHydrogens = requiresExplicitHydrogens;
            this.requiresAromaticityPerception = requiresAromaticityPerception;
        }
    }

    public int numberOfFingerprintTypesInUse() {
        return usedFingerprints.length;
    }

    public boolean isFingerprintTypeInUse(USED_FINGERPRINTS type) {
        return (fastCompareFlag & (1L<<type.defaultPosition)) != 0;
    }

    @Deprecated
    public USED_FINGERPRINTS getFingerprintTypeAt(int index) {
        return usedFingerprints[index];
    }

    public USED_FINGERPRINTS getFingerprintTypeFor(final int fingerprintIndex_) {
        int fingerprintIndex = fingerprintIndex_;
        for (int i=0; i < usedFingerprints.length; ++i) {

            if (fingerprintIndex < usedFingerprints[i].length)
                return usedFingerprints[i];
            fingerprintIndex -= usedFingerprints[i].length;
        }
        throw new IndexOutOfBoundsException(fingerprintIndex_ + " is not the index of a molecular property for " + this + " with size " + size());
    }

    private static Pattern COUNT_PATTERN = Pattern.compile(" at least (\\d+) times");
    private static Pattern ELEMENT_PATTERN = Pattern.compile(" >= (\\d+) ([A-Z][a-z]?)");
    private static Pattern SINGLE_ELEMENT_PATTERN = Pattern.compile("^\\[#(\\d+)\\]$");

    private static final MolecularProperty[][] DEFAULT_PROPERTIES = new MolecularProperty[EXTENDED_SETUP.length][];
    private static final ExtendedConnectivityProperty[] ECFP_PROPS = new ExtendedConnectivityProperty[ExtendedConnectivityProperty.getFingerprintLength()];

    static {
        try {
            loadFingerprintDescriptors();
        } catch (IOException e) {
            LoggerFactory.getLogger(CdkFingerprintVersion.class).error(e.getMessage(),e);
        }
    }
/*
    private static void loadFingerprintDescriptors() throws IOException {
        final PeriodicTable T = PeriodicTable.getInstance();
        final BufferedReader r = FileUtils.ensureBuffering(new InputStreamReader(CdkFingerprintVersion.class.getResourceAsStream("/index_fingerprints.txt")));
        String line = null;
        final ArrayList<MolecularProperty> properties = new ArrayList<>();
        while ((line = r.readLine()) != null) {
            final String[] parts = line.split("\t#");
            final String smartsPattern = parts[0];
            final String comment = (parts.length > 1) ? parts[1] : "";
            final Matcher SINGLE_ELEM = SINGLE_ELEMENT_PATTERN.matcher(smartsPattern);
            final Matcher M = comment.length() > 0 ? COUNT_PATTERN.matcher(comment) : null;
            if (SINGLE_ELEM.find()) {
                final Element elem = T.get(Integer.parseInt(SINGLE_ELEM.group(1)));
                final int count;
                if (M!=null && M.find()) {
                    count = Integer.parseInt(M.group(1));
                } else count = 1;
                properties.add(new FormulaProperty(elem, count));
            } else {
                if (M != null && M.find()) {
                    final int count = Integer.parseInt(M.group(1));
                    properties.add(new SubstructureCountProperty(parts[0], count));
                } else if (smartsPattern.length() > 0 && !smartsPattern.equals("?")) {
                    properties.add(new SubstructureProperty(smartsPattern, comment.isEmpty() ? null : parts[1]));
                } else {
                    final Matcher M2 = ELEMENT_PATTERN.matcher(comment);
                    if (M2.find()) {
                        properties.add(new FormulaProperty(T.getByName(M2.group(2)), Integer.parseInt(M2.group(1))));
                    } else {
                        properties.add(new SpecialMolecularProperty(comment));
                    }

                }
            }
        }
        int offset=0;
        for (int k=0; k < WITHOUT_ECFP_SETUP.length; ++k) {
            DEFAULT_PROPERTIES[k] = new MolecularProperty[WITHOUT_ECFP_SETUP[k].length];
            for (int i=0; i < WITHOUT_ECFP_SETUP[k].length; ++i) {
                DEFAULT_PROPERTIES[k][i] = properties.get(offset++);
            }
        }
        for (int k=0; k < ECFP_PROPS.length; ++k) {
            ECFP_PROPS[k] = new ExtendedConnectivityProperty(ExtendedConnectivityProperty.getHashValue(k));
        }
    }
    */

    private static void loadFingerprintDescriptors() throws IOException {
        final PeriodicTable T = PeriodicTable.getInstance();
        final ArrayList<MolecularProperty> properties = new ArrayList<>();
        try (final BufferedReader r = FileUtils.ensureBuffering(new InputStreamReader(CdkFingerprintVersion.class.getResourceAsStream("/index_fingerprints.txt")))) {
            String line = null;
            while ((line = r.readLine()) != null) {
                final String[] parts = line.split("\t#");
                final String smartsPattern = parts[0];
                final String comment = (parts.length > 1) ? parts[1] : "";
                final Matcher SINGLE_ELEM = SINGLE_ELEMENT_PATTERN.matcher(smartsPattern);
                final Matcher M = comment.length() > 0 ? COUNT_PATTERN.matcher(comment) : null;
                if (SINGLE_ELEM.find()) {
                    final Element elem = T.get(Integer.parseInt(SINGLE_ELEM.group(1)));
                    final int count;
                    if (M != null && M.find()) {
                        count = Integer.parseInt(M.group(1));
                    } else count = 1;
                    properties.add(new FormulaProperty(elem, count));
                } else {
                    if (M != null && M.find()) {
                        final int count = Integer.parseInt(M.group(1));
                        properties.add(new SubstructureCountProperty(parts[0], count));
                    } else if (smartsPattern.length() > 0 && !smartsPattern.equals("?")) {
                        properties.add(new SubstructureProperty(smartsPattern, comment.isEmpty() ? null : parts[1]));
                    } else {
                        final Matcher M2 = ELEMENT_PATTERN.matcher(comment);
                        if (M2.find()) {
                            properties.add(new FormulaProperty(T.getByName(M2.group(2)), Integer.parseInt(M2.group(1))));
                        } else {
                            properties.add(new SpecialMolecularProperty(comment));
                        }

                    }
                }
            }
        }

        // ECFP

        for (int k = 0; k < ECFP_PROPS.length; ++k) {
            properties.add(new ExtendedConnectivityProperty(ExtendedConnectivityProperty.getHashValue(k)));
        }

        // MINED FINGERPRINTS
        try (final BufferedReader r = FileUtils.ensureBuffering(new InputStreamReader(CdkFingerprintVersion.class.getResourceAsStream("/fingerprints/biosmarts.txt")))) {
            String line = null;
            while ((line = r.readLine()) != null) {
                properties.add(new SubstructureProperty(line));
            }
        }
        try (final BufferedReader r = FileUtils.ensureBuffering(new InputStreamReader(CdkFingerprintVersion.class.getResourceAsStream("/fingerprints/ringsystems.txt")))) {
            String line = null;
            while ((line = r.readLine()) != null) {
                properties.add(new SubstructureProperty(line));
            }
        }

        try (final BufferedReader r = FileUtils.ensureBuffering(new InputStreamReader(CdkFingerprintVersion.class.getResourceAsStream("/fingerprints/insilico.txt")))) {
            String line = null;
            while ((line = r.readLine()) != null) {
                properties.add(new SubstructureProperty(line));
            }
        }

        final TIntArrayList topIndizesList = new TIntArrayList();
        try (final BufferedReader r = FileUtils.ensureBuffering(new InputStreamReader(CdkFingerprintVersion.class.getResourceAsStream("/fingerprints/fingerprint_selection_indizes.txt")))) {
            String line = null;
            while ((line = r.readLine()) != null) {
                topIndizesList.add(Integer.parseInt(line));
            }
        }
        topIndizes = topIndizesList.toArray();

        int offset=0;
        for (int k=0; k < DEFAULT_PROPERTIES.length; ++k) {
            int length = EXTENDED_SETUP[k].length;
            DEFAULT_PROPERTIES[k] = properties.subList(offset, offset+length).toArray(new MolecularProperty[length]);
            offset += length;
        }
    }

    public static MolecularProperty[] getDefaultPropertiesFor(USED_FINGERPRINTS uf) {
        return DEFAULT_PROPERTIES[uf.defaultPosition];
    }

    public static MaskedFingerprintVersion getTopProperties(int desiredNumberOfProperties) {
        MaskedFingerprintVersion.Builder builder = MaskedFingerprintVersion.buildMaskFor(getDefault());
        builder.disableAll();
        for (int i=0; i < Math.min(topIndizes.length, desiredNumberOfProperties); ++i) {
            builder.enable(topIndizes[i]);
        }
        return builder.toMask();
    }


    /**
     * Returns the fingerprint version that is default in our database
     */
    public static CdkFingerprintVersion getDefault() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Returns the fingerprint version that contains all (future) fingerprints
     */
    public static CdkFingerprintVersion getComplete() {
        return DEFAULT_INSTANCE;
    }

    public static CdkFingerprintVersion withECFP() {
        return DEFAULT_INSTANCE;
    }

    public static CdkFingerprintVersion getWithoutEcfp() {
        return WITHOUT_ECFP;
    }

    public static CdkFingerprintVersion getExtended() {
        return EXTENDED_INSTANCE;
    }


    public static CdkFingerprintVersion getFromBitsetIdentifier(long identifier) {
        final ArrayList<USED_FINGERPRINTS> list = new ArrayList<>();
        final USED_FINGERPRINTS[] all = USED_FINGERPRINTS.values();
        for (int k=0; k < all.length; ++k) {
            if ((identifier & (1L<<all[k].defaultPosition))!=0) {
                list.add(all[k]);
                identifier &= ~(1L<<all[k].defaultPosition);
            }
        }
        if (identifier==0L) {
            return new CdkFingerprintVersion(list.toArray(new USED_FINGERPRINTS[list.size()]));
        } else {
            throw new IllegalArgumentException("Unknown fingerprint types with bit set " + Long.toBinaryString(identifier));
        }
    }

    private static final CdkFingerprintVersion WITHOUT_ECFP = new CdkFingerprintVersion(WITHOUT_ECFP_SETUP);
    private final static CdkFingerprintVersion DEFAULT_INSTANCE = new CdkFingerprintVersion(DEFAULT_SETUP);

    private static final CdkFingerprintVersion EXTENDED_INSTANCE = new CdkFingerprintVersion(EXTENDED_SETUP);

}
