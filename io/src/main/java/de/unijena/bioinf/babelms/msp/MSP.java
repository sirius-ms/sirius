/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms.msp;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*

MONA *.msp format III (also Massbank):
----------------
http://mona.fiehnlab.ucdavis.edu/downloads
Name: CLC_301.1468_14.3
Synon: Chlorcyclizine
Synon: 1-[(4-chlorophenyl)-phenylmethyl]-4-methylpiperazine
SYNON: $:00in-source
DB#: ET010001
InChIKey: WFNAKBGANONZEQ-UHFFFAOYSA-N
Precursor_type: [M+H]+
Spectrum_type: MS2
PrecursorMZ: 301.1466
Instrument_type: LC-ESI-QFT
Instrument: Q Exactive Orbitrap Thermo Scientific
Ion_mode: P
Collision_energy: 15, 30, 45, 60, 70 or 90 (nominal)
InChIKey: WFNAKBGANONZEQ-UHFFFAOYSA-N
Formula: C18H21ClN2
MW: 300
ExactMass: 300.13932635199996
Comments: "accession=ET010001" "author=R. Gulde, E. Schymanski, K. Fenner, Department of Environmental Chemistry, Eawag" "license=CC BY" "copyright=Copyright (C) 2016 Eawag, Duebendorf, Switzerland" "publication=Gulde, Meier, Schymanski, Kohler, Helbling, Derrer, Rentsch & Fenner; ES&T 2016 50(6):2908-2920. DOI: 10.1021/acs.est.5b05186. Systematic Exploration of Biotransformation Reactions of Amine-containing Micropollutants in Activated Sludge" "comment=CONFIDENCE Parent Substance with Reference Standard (Level 1)" "comment=INTERNAL_ID 100" "exact mass=300.1393" "instrument=Q Exactive Orbitrap Thermo Scientific" "instrument type=LC-ESI-QFT" "ms level=MS2" "ionization=ESI" "fragmentation mode=HCD" "collision energy=15, 30, 45, 60, 70 or 90 (nominal)" "resolution=17500" "column=Atlantis T3 3um, 3x150mm, Waters with guard column" "flow gradient=95/5 at 0 min, 5/95 at 15 min, 5/95 at 20 min, 95/5 at 20.1 min, 95/5 at 25 min" "flow rate=300 uL/min" "retention time=14.6 min" "solvent a=water with 0.1% formic acid" "solvent b=methanol with 0.1% formic acid" "precursor m/z=301.1466" "precursor type=[M+H]+" "ionization mode=positive" "mass accuracy=0.007810149499385606" "mass error=-2.351999967231677E-6" "SMILES=CN1CCN(CC1)C(C1=CC=CC=C1)C1=CC=C(Cl)C=C1" "cas=82-93-9" "pubchem cid=2710" "chemspider=2609" "InChI=InChI=1S/C18H21ClN2/c1-20-11-13-21(14-12-20)18(15-5-3-2-4-6-15)16-7-9-17(19)10-8-16/h2-10,18H,11-14H2,1H3" "InChIKey=WFNAKBGANONZEQ-UHFFFAOYSA-N" "molecular formula=C18H21ClN2" "total exact mass=300.13932635199996" "SMILES=CN1CCN(CC1)C(C2=CC=CC=C2)C3=CC=C(C=C3)Cl"
Num Peaks: 5
99.0915 0.440287
165.0694 1.883217
166.0777 3.318937
201.0466 100.000000
Name: CLC_301.1468_14.3
Synon: Chlorcyclizine
Synon: 1-[(4-chlorophenyl)-phenylmethyl]-4-methylpiperazine
SYNON: $:00in-source
DB#: ET010002
InChIKey: WFNAKBGANONZEQ-UHFFFAOYSA-N
Precursor_type: [M+H]+
Spectrum_type: MS2
PrecursorMZ: 301.1466
Instrument_type: LC-ESI-QFT
Instrument: Q Exactive Orbitrap Thermo Scientific
Ion_mode: P
Collision_energy: 15, 30, 45, 60, 70 or 90 (nominal)
InChIKey: WFNAKBGANONZEQ-UHFFFAOYSA-N
Formula: C18H21ClN2
MW: 300
ExactMass: 300.13932635199996
Comments: "accession=ET010002" "author=R. Gulde, E. Schymanski, K. Fenner, Department of Environmental Chemistry, Eawag" "license=CC BY" "copyright=Copyright (C) 2016 Eawag, Duebendorf, Switzerland" "publication=Gulde, Meier, Schymanski, Kohler, Helbling, Derrer, Rentsch & Fenner; ES&T 2016 50(6):2908-2920. DOI: 10.1021/acs.est.5b05186. Systematic Exploration of Biotransformation Reactions of Amine-containing Micropollutants in Activated Sludge" "comment=CONFIDENCE Parent Substance with Reference Standard (Level 1)" "comment=INTERNAL_ID 100" "exact mass=300.1393" "instrument=Q Exactive Orbitrap Thermo Scientific" "instrument type=LC-ESI-QFT" "ms level=MS2" "ionization=ESI" "fragmentation mode=HCD" "collision energy=15, 30, 45, 60, 70 or 90 (nominal)" "resolution=17500" "column=Atlantis T3 3um, 3x150mm, Waters with guard column" "flow gradient=95/5 at 0 min, 5/95 at 15 min, 5/95 at 20 min, 95/5 at 20.1 min, 95/5 at 25 min" "flow rate=300 uL/min" "retention time=14.6 min" "solvent a=water with 0.1% formic acid" "solvent b=methanol with 0.1% formic acid" "precursor m/z=301.1466" "precursor type=[M+H]+" "ionization mode=positive" "mass accuracy=0.007810149499385606" "mass error=-2.351999967231677E-6" "SMILES=CN1CCN(CC1)C(C1=CC=CC=C1)C1=CC=C(Cl)C=C1" "cas=82-93-9" "pubchem cid=2710" "chemspider=2609" "InChI=InChI=1S/C18H21ClN2/c1-20-11-13-21(14-12-20)18(15-5-3-2-4-6-15)16-7-9-17(19)10-8-16/h2-10,18H,11-14H2,1H3" "InChIKey=WFNAKBGANONZEQ-UHFFFAOYSA-N" "molecular formula=C18H21ClN2" "total exact mass=300.13932635199996" "SMILES=CN1CCN(CC1)C(C2=CC=CC=C2)C3=CC=C(C=C3)Cl"
Num Peaks: 7
99.0916 0.527554
165.07 14.
183.0805 3.771867
201.0466 100.000000
*/
/*
* NIST
Name: TKPR/-1
Synon: p-Aminophenylacetyl Tuftsin
Synon: $:04632.3525
Synon: $:03[M-H]-
Synon: $:0515
Synon: $:16150
Synon: $:07Agilent QTOF 6530
Synon: $:06Q-TOF
Synon: $:09direct flow injection
Synon: $:10ESI
Synon: $:12N2
Synon: $:17micromol/L in water/methanol/formic acid (50/50/0.1) Peptide_VID=A_80-0-26 Peptide_QTOF_id_2012=7979 Spec=Consensus Nreps=14/16 Mz_diff=-0.0081
Synon: $:11N
Formula: C29H47N9O7
MW: 633.359844
PrecursorMZ: 632.3525
CASNO: 0
Comment: Mods=1/0,T,Aminophenylacetyl NIST Mass Spectrometry Data Center
Num peaks: 11
228.124 18.68 "y2-42/-0.0112 4/14"
270.153 31.97 "y2/-0.0041 9/14"
546.316 15.48 "p-C2H4O-42/0.0114 5/14"
588.302 39.36 "p-C2H4O/-0.0249;p-44/-0.0612 4/14"
588.320 178.72 "p-C2H4O/-0.0063;p-44/-0.0426 11/14"
588.353 58.14 "p-44/-0.0093;p-C2H4O/0.0270 9/14"
631.976 11.49 "? 4/14"
632.343 999.00 "p/-0.0099 14/14"
633.343 18.68 "pi/0.9900 5/14"
633.375 12.69 "?i 4/14"
633.579 10.09 "?i 4/14"
*
*/

/*
Also supports MS-Finder .mat extensions
 */

public class MSP {
    //todo maybe add TXONOMY field from MS-Dial mat file.

    public final static String FEATURE_ID = "PEAKID"; //feature id extension of ms-dial .mat format
    public final static String SYNONYME = "Synon"; //multiple times possible
    public final static String SYNONYME_KEY = "Synon: $:"; //multiple times possible
    public final static String[] PRECURSOR_MZ = {"PrecursorMZ", "PRECURSORMZ"};
    public final static String SYN_PRECURSOR_MZ = SYNONYME_KEY + "04";

    public final static String[] SPEC_TYPE = {"Spectrum_type", "MSTYPE"};
    public final static String[] COL_ENERGY = {"Collision_energy", "COLLISIONENERGY"};
    public final static String SYN_COL_ENERGY = SYNONYME_KEY + "05";
    public final static String[] PRECURSOR_ION_TYPE = {"Precursor_type", "PRECURSORTYPE"};
    public final static String SYN_PRECURSOR_ION_TYPE = SYNONYME_KEY + "03";
    public final static String NAME = "Name";
    public final static String DB_ID = "DB#";
    public final static String[] INCHI_KEY = {"InChIKey", "INCHIKEY"};
    public final static String INCHI = "InChI";
    public final static String SMILES = "SMILES";
    public final static String[] INSTRUMENT_TYPE = {"Instrument_type", "INSTRUMENTTYPE"};
    public final static String INSTRUMENT = "Instrument";
    public final static String FORMULA = "Formula";
    public final static String COMMENTS[] = {"Comments", "COMMENT"}; //multiple times possible
    public final static String SPLASH = "Splash";
    public final static String EXACT_MASS = "ExactMass";
    public final static String NOMINAL_MASS = "MW"; // seems to be unit mass? Molecular Weight?
    public final static String[] CHARGE = {"Ion_mode", "IONMODE"};

    public final static String NUM_PEAKS = "Num Peaks";

    public final static String RT = "RETENTIONTIME";

    public static String COMMENT_SEPARATOR = ";:|:;";

    public static Optional<PrecursorIonType> parsePrecursorIonType(Map<String, String> metaInfo) {
        String value = getWithSynonyms(metaInfo, PRECURSOR_ION_TYPE).orElse(null);
        if (value != null && !value.isBlank())
            return Optional.of(PrecursorIonType.fromString(value));

        value = metaInfo.get(SYN_PRECURSOR_ION_TYPE);
        if (value != null && !value.isBlank())
            return Optional.of(PrecursorIonType.fromString(value));

        value = getWithSynonyms(metaInfo, CHARGE).orElse(null);
        if (value != null && !value.isBlank()) {
            return Optional.of(value.toLowerCase().charAt(0) == 'n' ? PrecursorIonType.unknownNegative() : PrecursorIonType.unknownPositive());
        }

        return Optional.empty();
    }

    public static Optional<Double> parsePrecursorMZ(Map<String, String> metaInfo) {
        String value = getWithSynonyms(metaInfo, PRECURSOR_MZ).orElse(null);
        if (value != null && !value.isBlank()) {
            String[] arr = value.split("/");
            return Optional.of(Utils.parseDoubleWithUnknownDezSep(arr[arr.length - 1]));
        }

        value = metaInfo.get(SYN_PRECURSOR_MZ);
        if (value != null && !value.isBlank()) {
            String[] arr = value.split("/");
            return Optional.of(Utils.parseDoubleWithUnknownDezSep(arr[arr.length - 1]));
        }

        value = metaInfo.get(EXACT_MASS);
        if (value != null && !value.isBlank())
            return Optional.of(Utils.parseDoubleWithUnknownDezSep(value));

        return Optional.empty();
    }

    public static Optional<CollisionEnergy> parseCollisionEnergy(Map<String, String> metaInfo) {
        String value = getWithSynonyms(metaInfo, COL_ENERGY).orElse(null);
        CollisionEnergy e = null;
        if (value != null && !value.isBlank()) {
            e = CollisionEnergy.fromStringOrNull(value);
            if (e != null) {
                value = metaInfo.get(SYN_COL_ENERGY);
                if (value != null)
                    e = CollisionEnergy.fromStringOrNull(value);
            }
        }
        return Optional.ofNullable(e);
    }

    public static Optional<List<String>> extractComments(Map<String, String> metaInfo) {
        String value = getWithSynonyms(metaInfo, COMMENTS).orElse(null);
        if (value != null && !value.isBlank())
            return Optional.of(List.of(value.split(COMMENT_SEPARATOR)));
        return Optional.empty();
    }

    public static Optional<String> parseFeatureId(Map<String, String> metaInfo) {
        return parseFeatureId(metaInfo, extractComments(metaInfo).orElse(null));
    }
    public static Optional<String> parseFeatureId(Map<String, String> metaInfo, @Nullable List<String> comments) {
        String value = metaInfo.get(MSP.FEATURE_ID);
        if (value != null && !value.isBlank())
            return Optional.of(value);

        if (comments != null && !comments.isEmpty()){
            return comments.stream().filter(Objects::nonNull).flatMap(s -> Arrays.stream(s.split("\\s*\\|\\s*")))
                    .filter(s -> s.startsWith(MSP.FEATURE_ID)).map(s -> s.split("\\s*=\\s*")[1]).findFirst();
        }
        return Optional.empty();
    }

    public static Optional<String> parseName(Map<String, String> metaInfo) {
        return parseName(metaInfo, extractComments(metaInfo).orElse(null));
    }
    public static Optional<String> parseName(Map<String, String> metaInfo, @Nullable List<String> comments) {
        //if feature id is available, use this as name
        String value = parseFeatureId(metaInfo, comments).orElse(null);
        if (value != null && !value.isBlank())
            return Optional.of(value);

        value = metaInfo.get(MSP.NAME);
        if (value != null && !value.isBlank() && !"Unknown".equalsIgnoreCase(value) && !"null".equalsIgnoreCase(value)) {
            return Optional.of(value);
        }

        if (comments != null && !comments.isEmpty())
            value = comments.stream().min(Comparator.comparing(String::length)).orElse(null);

        if (value != null && !value.isBlank())
            return Optional.of(value);

        return Optional.empty();
    }

    public static Optional<RetentionTime> parseRetentionTime(Map<String, String> metaInfo) {
        if (metaInfo.containsKey(RT)) {
            double rt = Utils.parseDoubleWithUnknownDezSep(metaInfo.get(RT));
            if (rt > 0) {
                return Optional.of(new RetentionTime(rt * 60));
            }
        } else {
            String comments = getWithSynonyms(metaInfo, COMMENTS).orElse(null);
            if (comments != null) {
                Matcher m = Pattern.compile("\"retention\\s?time=([^\"]+)").matcher(comments);
                if (m.find()) {
                    String rtValue = m.group(1);
                    return RetentionTime.tryParse(rtValue);
                }
                m = Pattern.compile("\"rtinseconds=([^\"]+)").matcher(comments);
                if (m.find()) {
                    String rtValue = m.group(1);
                    return Optional.of(new RetentionTime(Double.parseDouble(rtValue)));
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<String> getWithSynonyms(@NotNull final Map<String, String> metaInfo, @NotNull final String... keys) {
        return Arrays.stream(keys).map(metaInfo::get).filter(Objects::nonNull).filter(s -> !s.isBlank()).findFirst();
    }
}
