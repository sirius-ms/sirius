/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 * Chair of Bioinformatics, Friedrich-Schilller University.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.elgordo;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static de.unijena.bioinf.ChemistryBase.chem.MolecularFormula.parseOrNull;
import static de.unijena.bioinf.elgordo.FragmentLib.def;
import static de.unijena.bioinf.elgordo.HeadGroup.*;

public enum LipidClass {
    //https://lipidmaps.org/databases/lmsd/
    MG(1, Glycerol, def("+").losses("", "OH", "H2O").acyl("", "H2O").acylFragments("", "-H2O", "C3H5O", "C3H3").def("[M+NH3+H]+").def("[M+Na]+").adductSwitch().done(), "OCC(O)COR1", "Monoradylglycerol", "LMGL01010000"),
    DG(2, Glycerol, def("+").losses("", "OH", "H2O").acyl("", "H2O").acylFragments("", "-H2O", "C3H5O", "C3H3").def("[M+NH3+H]+").def("[M+Na]+").adductSwitch().done(), "OCC(OR2)COR1", "Diradylglycerol", null), //https://lipidmaps.org/databases/lmsd/LMGL02010000 //https://lipidmaps.org/databases/lmsd/LMGL02020000
    TG(3, Glycerol, def("+").losses("", "OH", "H2O").acyl("", "H2O").acylFragments("", "-H2O", "C3H5O", "C3H3").def("[M+NH3+H]+").def("[M+Na]+").adductSwitch().done(), "C(COR1)(OR2)COR3", "Triradylglycerol", "LMGL03010000"),
    DGTS(2, Glyceroltrimethylhomoserin, def("+").fragments("C10H21NO5", "C7H13NO2").acyl("", "H2O").def("[M+H]+").done(), "N(C)(C)(C)C(C(=O)O)CCOCC(OR2)COR1", "Betaine diradylglycerol", null),
    LDGTS(1, Glyceroltrimethylhomoserin, def("+").fragments("C10H21NO5", "C7H13NO2").losses("H2O").acyl("").def("[M+H]+").done(), "C(COCCC(C(=O)O)N(C)(C)C)(O)COR1", "MGTS", "Betaine monoradylglycerol", null),
    MGDG(2, Galactosylglycerol, def("+").losses("C6H11O6", "C6H13O7").acyl("", "C6H11O6", "C6H13O7", "C3H5O").def("[M+NH3+H]+").def("[M+Na]+").done(), "O(C1C(O)C(O)C(O)C(CO)O1)CC(OR2)(COR1)", "Glycosyldiradylglycerol", "LMGL0501AA00"),
    DGDG(2, Digalactosyldiacylglycerol, def("+").losses("C12H21O11", "C12H23O12").acyl("", "H2O", "C3H5O").def("[M+NH3+H]+").def("[M+Na]+").done(), "C(OC1C(O)C(O)C(O)C(COC2C(O)C(O)C(O)C(CO)O2)O1)C(OR1)(COR2)", "Glycosyldiradylglycerol", "LMGL0501AD00"),

    SQDG(2, Sulfoquinovosylglycerols, def("+").losses("C6H10O7S", "C6H12O8S").acyl("", "H2O", "C3H5O").acylFragments("").def("[M+NH3+H]+").def("-").fragments("H2SO3","HSO3", "CH4SO3","CH4O2S"). losses("C6H10O7S", "C6H12O8S").acyl("").acylFragments("", "H2O").done(), "C(COC1C(O)C(O)C(O)C(CS(O)(=O)=O)O1)(OR2)COR1", "Sulfoquinovosyldiacylglycerol", null),
    SQMG(1, Sulfoquinovosylglycerols, def("-").fragments("H2SO3","HSO3", "CH4SO3","CH4O2S").acyl("").acylFragments("", "H2O").done(), "C(COC1C(O)C(O)C(O)C(CS(O)(=O)=O)O1)(O)COR1", "Sulfoquinovosylmonoacylglycerol", null),

    PC(2, Glycerophosphocholines, def("+").fragments("C2H5O4P", "C5H14NO4P", "C5H13NO").losses("C3H9N", "C5H14NO4P").alkyl("", "H", "H2O").acyl("", "H2O").plasmalogenFragment("C21H39O").
            def("[M+H]+").def("[M+Na]+").adductSwitch().acyl(parseOrNull("C3H9N").negate(), MolecularFormula.parseOrThrow("C3H9N").negate().add(parseOrNull("H2O")))
            .def("[M + H2CO2 - H]-").fragments("C4H12NO4P", "C7H16O5NP", "C7H18O6NP", "C3H7O5P").losses("CH2").acyl("", "H2O").acylFragments("", "-H2O")
            .done(), "C(COP(=O)(O)OCCN(C)(C)C)(OR2)COR1", "Diacylglycerophosphocholine", "LMGP01010000"),

    LPC(1, Glycerophosphocholines, def("[M+H]+").fragments("C5H13NO", "C5H14NO4P", "C2H5O4P").losses("H2O", "C5H14NO4P").acyl("", "H2O").def("[M+Na]+").fragments("C5H13NO", "C5H14NO4P", "C2H5O4P").losses("C3H9N", "C5H14NO4P").acyl("")
            .def("[M + H2CO2 - H]-").fragments("C4H12NO4P", "C7H16O5NP", "C7H18O6NP", "C3H7O5P").losses("CH2").acyl("", "H2O").acylFragments("", "-H2O")
            .done(), "C(OP(OCCN(C)(C)C)(=O)O)C(O)COR1", "Monoacylglycerophosphocholine", "LMGP01050000"),

    PE(2, Glycerophosphoethanolamines, def("+").losses("C2H5N", "C2H6NO3P", "C2H8NO4P", "C2H6NO4P").acylFragments("", "-OH", "-H3O2", "-C2H5N").acyl("", "H2O", "-C2H5N").fragments("H3PO4", "C2H6NO3P", "C2H8NO4P").def("[M+Na]+").def("[M+H]+")
            .def("[M-H]-").fragments("C2H6NO3P", "C2H8NO4P", "C5H12O5PN", "C5H14O6PN", "C3H7O5P", "C3H9O6P")
            .acyl("", "H2O").acylFragments("", "-H2O")
            .done(), "C(COP(=O)(O)OCCN)(OR2)COR1", "Diacylglycerophosphoethanolamine", "LMGP02050000"),

    LPE(1, Glycerophosphoethanolamines, def("[M+H]+").fragments("C3H7O5P").losses("H2O", "C2H7NO", "C2H8NO4P", "C3H9O6P").acyl("", "-C2H6NO3P").acylFragments("").def("[M+Na]+").fragments("H3PO4", "C2H8NO4P", "C3H7O5P", "C3H9O6P").losses("C2H5N", "C2H7NO", "C2H6NO3P", "C2H8NO4P", "C2H6NO4P").acylFragments("").acyl("", "H2O").done(), "NCCOP(O)(=O)OCC(O)COR1", "Monoacylglycerophosphoethanolamine", "LMGP02050000"),

    PS(2, Glycerophosphoserines, def("[M+H]+").fragments("C3H7NO3", "C3H7O5P").losses("C3H8NO6P", "C3H6NO6P").acyl("", "H2O", "-C3H6NO5P").acylFragments("")
            .def("[M-H]-").fragments("C3H7O5P", "C3H9O6P").losses("C3H5NO2").acyl("", "H2O", "C3H5NO2", "C3H7NO3").acylFragments("", "-H2O")
            .done(), "C(O)(=O)C(N)COP(=O)(O)OCC(OR2)COR1", "Diacylglycerophosphoserine", "LMGP03010000"),

    LPS(1, Glycerophosphoserines, def("[M+H]+").fragments("C3H8NO3").losses("H2O", "C3H8NO6P").acyl("", "H2O").done(), "C(O)(=O)C(N)COP(=O)(O)OCC(O)COR1", "Monoacylglycerophosphoserine", "LMGP03050000"),

    PG(2, Glycerophosphoglycerols, def("+").losses("H2O", "C3H9O6P", "C3H7O5P", "C3H9O6P").fragments("C3H9O6P").acyl("", "H2O").acylFragments("", "-H2O").def("[M+Na]+").losses("C3H8O6P", "C3H7O5P").acyl("-C3H6O2").adductSwitch().def("[M+H]+").def("[M+NH3+H]+")
            .def("[M-H]-").fragments("C3H7O5P", "C3H9O6P", "C6H13O7P", "C6H11O6P").losses("C3H6O2").acyl("", "H2O", "C3H6O2", "C3H8O3").acylFragments("", "-H2O")
            .done(), "C(COR1)(COP(=O)(O)OCC(CO)O)OR2", "Diacylglycerophosphoglycerol", "LMGP04010000"),

    LPG(1, Glycerophosphoglycerols,
            def("+").losses("H2O", "C3H9O6P", "C3H7O5P", "C3H9O6P").fragments("C3H9O6P").acyl("", "H2O").acylFragments("", "-H2O").def("[M+Na]+").losses("C3H8O6P", "C3H7O5P").acyl("-C3H6O2").adductSwitch().def("[M+H]+").def("[M+NH3+H]+")
                    .def("[M-H]-").fragments("C3H7O5P", "C3H9O6P", "C6H13O7P", "C6H11O6P").losses("C3H6O2").acyl("", "H2O", "C3H6O2", "C3H8O3").acylFragments("", "-H2O")
                    .done(),
            "C(O)(CO)COP(=O)(O)OCC(O)COR1", "Monoacylglycerophosphoglycerol", "LMGP04050000"),

    PI(2, Glycerophosphoinositols, def("+").losses("C6H10O5", "C6H12O6", "C6H11O8P", "C6H13O9P").fragments("C6H12O6", "C6H11O8P", "C6H13O9P").acyl("", "H2O", "C6H10O5").def("[M+H]+").def("[M+Na]+").def("[M+NH3+H]+").done(), "P(OC1C(O)C(O)C(O)C(O)C1O)(=O)(O)OCC(OR2)COR1", "Diacylglycerophosphoinositol", "LMGP06010000"),
    LPI(1, Glycerophosphoinositols, null, "C(O)(COP(=O)(O)OC1C(O)C(O)C(O)C(O)C1O)COR1", "Monoacylglycerophosphoinositol", "LMGP06050000"),
    PA(2, Glycerophosphates, def("[M+Na]+").losses("HO3P", "H3O4P").acyl("", "-H3O4P").acylFragments("")
            .def("-").fragments("C3H9O6P", "C3H7O5P", "H3PO4", "HPO3").acyl("", "H2O").acylFragments("", "-H2O").def("[M-H]-")
            .done(), "C(COP(=O)(O)O)(OR2)COR1", "Diacylglycerophosphate", "LMGP10010000"),

    LPA(1, Glycerophosphates, null, "C(COP(=O)(O)O)(O)COR1", "Monoacylglycerophosphate", "LMGP10050000"),
    CL(4, Glycerophosphoglycerophosphoglycerols, def("[M+NH3+H]+").acyl("").acylFragments("", "C3H5").done(), "P(=O)(O)(OCC(OR2)COR1)OCC(O)COP(=O)(O)OCC(OR4)COR3", "Diacylglycerophosphoglycerophosphodiradylglycerol", "LMGP12010000"),

    Cer(2, NoHeadGroup, def("+").losses("H2O", "H4O2").sphingosinFragments("", "H2O", "H4O2", "CH4O2").sphingosinLosses("", "H2O", "H4O2", "CH2O", "NH3", "NH5O").def("[M+O+H]+").def("[M+H]+")/* .losses("H2O","H4O2").sphingosinFragments("", "H2O", "H4O","CH4O2").sphingosinLosses("") */.done(), "X", "Ceramide", "LMSP02010000"),
    HexCer(2, Hexose, def("+").losses("H2O", "H4O2", "C6H10O5", "C6H12O6", "C6H14O7").sphingosinFragments("", "H2O", "H4O2", "CH4O2").sphingosinLosses("", "H2O", "H4O2", "CH2O", "NH3", "NH5O").def("[M+O+H]+").def("[M+H]+")/* .losses("H2O","H4O2").sphingosinFragments("", "H2O", "H4O","CH4O2").sphingosinLosses("") */.done(), "O1C(CO)C(O)C(O)C(O)C1X", "Hexose Ceramide", null),
    Sm(2, Phosphocholin, def("+").fragments("C5H13NO", "C2H5O4P", "C5H14NO4P").losses("H2O", "C3H11NO").sphingosinFragments("").sphingosinLosses("").def("[M+H]+").def("[M+Na]+").fragments("C5H15NO5P").losses("C3H9N").done(), "N(C)(C)(C)CCOP(=O)(O)X", "Sphingomyeline", null);


    protected static HashMap<HeadGroup, List<LipidClass>> group2classes;

    static {
        group2classes = new HashMap<>();
        for (LipidClass c : values()) {
            group2classes.computeIfAbsent(c.headgroup, (x) -> new ArrayList<>()).add(c);
        }
    }

    public static Set<HeadGroup> getHeadGroups() {
        return group2classes.keySet();
    }

    public static LipidClass[] getClassesFor(HeadGroup group) {
        return group2classes.get(group).toArray(LipidClass[]::new);
    }

    private LipidClass(int chains, HeadGroup headgroup, FragmentLib lib, String smiles, String longName, String lipidMapsId) {
        this(chains, headgroup, lib, smiles, null, longName, lipidMapsId);
    }

    private LipidClass(int chains, HeadGroup headgroup, FragmentLib lib, String smiles, String abbrev, String longName, String lipidMapsId) {
        this.headgroup = headgroup;
        this.chains = chains;
        this.fragmentLib = lib;
        this.sphingolipid = lib != null && lib.hasSphingosin();
        this.smiles = smiles;
        this.abbreviation = abbrev;
        this.longName = longName;
        this.lipidMapsId = lipidMapsId;
    }


    public final HeadGroup headgroup;
    public final int chains;
    public final FragmentLib fragmentLib;
    protected final boolean sphingolipid;
    private final String smiles, abbreviation, longName, lipidMapsId;

    public static final String fuzzSearchBaseURL = "https://www.lipidmaps.org/data/structure/LMSDFuzzySearch.php?Name=%s&s=%s&SortResultsBy=Name";

    public Optional<String> getSmiles() {
        return Optional.ofNullable(smiles);
    }

    public String abbr() {
        return abbreviation == null ? name() : abbreviation;
    }

    public String longName() {
        return longName;
    }

    public boolean isSphingolipid() {
        return sphingolipid;
    }

    public String getLipidMapsId() {
        return lipidMapsId;
    }

    public URI lipidMapsClassLink() {
        if (lipidMapsId != null) {
            return URI.create("https://lipidmaps.org/databases/lmsd/" + lipidMapsId);
        } else {
            return makeLipidMapsFuzzySearchLink(abbr());
        }
    }

    public static URI makeLipidMapsFuzzySearchLink(String abbrev) {
        String encAbb = URLEncoder.encode(abbrev, StandardCharsets.UTF_8);
        return URI.create(String.format(Locale.US, fuzzSearchBaseURL, encAbb, encAbb));
    }
}
