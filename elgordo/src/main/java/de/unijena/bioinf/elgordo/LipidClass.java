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

import java.util.*;

import static de.unijena.bioinf.ChemistryBase.chem.MolecularFormula.parseOrNull;
import static de.unijena.bioinf.elgordo.FragmentLib.def;
import static de.unijena.bioinf.elgordo.HeadGroup.*;

public enum LipidClass {

    MG(1,	Glycerol, def("[M+NH3+H]+").losses("","OH", "H2O","H3O2","C3H6O2","C3H8O3").acyl("", "H2O").acylFragments("","-H2O","C3H5O","C3H8O").done(), "OCC(O)COR1"),
    DG(2,	Glycerol, def("[M+NH3+H]+").losses("","OH", "H2O","H3O2","C3H6O2","C3H8O3").acyl("","H2O").acylFragments("","C3H5O","-H2O","C3H8O").
                              def("[M+Na]+").losses("H2O").adductSwitch().acyl("").acylFragments("","-O").done(), "OCC(OR2)COR1"),
    TG(3,	Glycerol, def("[M+NH3+H]+").losses("","OH", "H2O","H3O2","C3H6O2","C3H8O3").acyl("","H2O").acylFragments("","-OH","-H3O2","H2O","C3H8O3").def("[M+Na]+").adductSwitch().acyl("","H2O").acylFragments("-OH","-H3O2").done(), "C(COR1)(OR2)COR3"),
    DGTS(	2,	Glyceroltrimethylhomoserin, def("+").fragments("C10H21NO5","C7H13NO2").acyl("","H2O").def("[M+H]+").done(), "N(C)(C)(C)C(C(=O)[O-])CCOCC(OR2)COR1"),
    LDGTS(	1,	Glyceroltrimethylhomoserin, def("+").fragments("C10H21NO5","C7H13NO2").losses("H2O").acyl("").def("[M+H]+").done(), "[C@](COCCC(C(=O)[O-])[N+](C)(C)C)([H])(O)COR1", "MGTS"),
    MGDG(	2,	Galactosylglycerol, def("+").losses("C6H11O6","C6H13O7").acyl("","C6H11O6","C6H13O7","C3H5O").def("[M+NH3+H]+").def("[M+Na]+").done(),"O([C@H]1[C@H](O)[C@@H](O)[C@@H](O)[C@@H](CO)O1)C[C@]([H])(OR2)(COR1)"),
    DGDG(	2,	Digalactosyldiacylglycerol,def("+").losses("C12H21O11","C12H23O12").acyl("","H2O","C3H5O").def("[M+NH3+H]+").def("[M+Na]+").done(), "C(O[C@H]1[C@H](O)[C@@H](O)[C@@H](O)[C@@H](CO[C@@H]2[C@H](O)[C@@H](O)[C@@H](O)[C@@H](CO)O2)O1)[C@]([H])(OR1)(COR2)"),
    SQDG(	2,	Sulfoquinovosylglycerols,def("+").losses("C6H10O7S","C6H12O8S").acyl("","H2O","C3H5O").acylFragments("").def("[M+NH3+H]+").done(), "[C@](CO[C@@H]1[C@H](O)[C@@H](O)[C@H](O)[C@@H](CS(O)(=O)=O)O1)([H])(OR2)COR1"),
    SQMG(	1,	Sulfoquinovosylglycerols,null, "[C@](CO[C@@H]1[C@H](O)[C@@H](O)[C@H](O)[C@@H](CS(O)(=O)=O)O1)([H])(O)COR1"),
    PC(	2,	Glycerophosphocholines, def("+").fragments("C2H5O4P","C5H14NO4P","C5H13NO").losses("C3H9N","C5H14NO4P").alkyl("","H","H2O").acyl("","H2O").plasmalogenFragment("C21H39O").
                                            def("[M+H]+").def("[M+Na]+").adductSwitch().acyl(parseOrNull("C3H9N").negate(),MolecularFormula.parseOrThrow("C3H9N").negate().add(parseOrNull("H2O"))).done(), "[C@](COP(=O)([O-])OCC[N+](C)(C)C)([H])(OR2)COR1"),
    LPC(	1,	Glycerophosphocholines, def("[M+H]+").fragments("C5H13NO","C5H14NO4P","C2H5O4P").losses("H2O","C5H14NO4P").acyl("","H2O").def("[M+Na]+").fragments("C5H13NO","C5H14NO4P","C2H5O4P").losses("C3H9N","C5H14NO4P").acyl("").done(), "C(OP(OCC[N+](C)(C)C)(=O)[O-])[C@@](O)([H])COR1"),
    //PE(	2,	Glycerophosphoethanolamines, def("[M+H]+").losses("C2H8NO4P","C2H5N").acylFragments("", "-OH","-H3O2", "-C2H5N").acyl("","H2O").def("[M+Na]+").fragments("H3PO4","C2H6NO3P","C2H8NO4P").losses("C2H5N","C2H6NO3P","C2H8NO4P","C2H6NO4P").acyl("","-C2H5N").acylFragments("").done()),
    PE(	2,	Glycerophosphoethanolamines, def("+").losses("C2H5N","C2H6NO3P","C2H8NO4P","C2H6NO4P").acylFragments("", "-OH","-H3O2", "-C2H5N").acyl("","H2O","-C2H5N").fragments("H3PO4","C2H6NO3P","C2H8NO4P").def("[M+Na]+").def("[M+H]+").done(),"[C@](COP(=O)(O)OCCN)([H])(OR2)COR1"),
    LPE(	1,	Glycerophosphoethanolamines, def("[M+H]+").fragments("C3H7O5P").losses("H2O","C2H7NO","C2H8NO4P","C3H9O6P").acyl("","-C2H6NO3P").acylFragments("").def("[M+Na]+").fragments("H3PO4","C2H8NO4P","C3H7O5P","C3H9O6P").losses("C2H5N","C2H7NO","C2H6NO3P","C2H8NO4P","C2H6NO4P").acylFragments("").acyl("","H2O").done(), "[C@](COP(=O)(O)OCCN)([H])(O)COR1"),
    PS(	2,	Glycerophosphoserines, def("[M+H]+").fragments("C3H7NO3","C3H7O5P").losses("C3H8NO6P","C3H6NO6P").acyl("","H2O", "-C3H6NO5P").acylFragments("").done(), "C(O)(=O)[C@@]([H])(N)COP(=O)(O)OC[C@]([H])(OR2)COR1"),
    LPS(	1,	Glycerophosphoserines, def("[M+H]+").fragments("C3H8NO3").losses("H2O","C3H8NO6P").acyl("","H2O").done(), "C(O)(=O)[C@@]([H])(N)COP(=O)(O)OC[C@]([H])(O)COR1"),
    PG(	2,	Glycerophosphoglycerols, def("+").losses("H2O","C3H9O6P").fragments("C3H9O6P").acyl("","H2O").acylFragments("","-H2O").def("[M+Na]+").losses("C3H8O6P","C3H7O5P").acyl("-C3H6O2").adductSwitch().def("[M+H]+").def("[M+NH3+H]+").done(), "[H][C@](O)(CO)COP(=O)(O)OC[C@]([H])(OR2)COR1"),
    LPG(	1,	Glycerophosphoglycerols, null, "[H][C@](O)(CO)COP(=O)(O)OC[C@]([H])(O)COR1"),
    PI(	2,	Glycerophosphoinositols, def("[M+H]+").losses("C6H10O5","C6H12O6","C6H11O8P","C6H13O9P").acyl("","H2O").done()),
    LPI(	1,	Glycerophosphoinositols),
    PA(	2,	Glycerophosphates, def("[M+Na]+").losses("HO3P","H3O4P").acyl("","-H3O4P").acylFragments("").done(), "[C@](COP(=O)(O)O)([H])(OR2)COR1"),
    LPA(	1,	Glycerophosphates,null,"[C@](COP(=O)(O)O)([H])(O)COR1"),
    CL(	4,	Glycerophosphoglycerophosphoglycerols, def("[M+NH3+H]+").acyl("").acylFragments("", "C3H5").done()),

    Cer(2, NoHeadGroup, def("+").losses("H2O","H4O2").sphingosinFragments("","H2O","H4O2","CH4O2").sphingosinLosses("","H2O","H4O2","CH2O","NH3", "NH5O").def("[M+O+H]+").def("[M+H]+")/* .losses("H2O","H4O2").sphingosinFragments("", "H2O", "H4O","CH4O2").sphingosinLosses("") */.done(),"X"),
    HexCer(2, Hexose, def("+").losses("H2O","H4O2", "C6H10O5", "C6H12O6", "C6H14O7").sphingosinFragments("","H2O","H4O2","CH4O2").sphingosinLosses("","H2O","H4O2","CH2O","NH3", "NH5O").def("[M+O+H]+").def("[M+H]+")/* .losses("H2O","H4O2").sphingosinFragments("", "H2O", "H4O","CH4O2").sphingosinLosses("") */.done(),"O1C(CO)C(O)C(O)C(O)C1X"),
    Sm(2, Phosphocholin, def("+").fragments("C5H13NO","C2H5O4P","C5H14NO4P").losses("H2O","C3H11NO").sphingosinFragments("").sphingosinLosses("").def("[M+H]+").def("[M+Na]+").fragments("C5H15NO5P").losses("C3H9N").done());


    protected static HashMap<HeadGroup, List<LipidClass>> group2classes;

    static {
        group2classes = new HashMap<>();
        for (LipidClass c : values()) {
            group2classes.computeIfAbsent(c.headgroup, (x)->new ArrayList<>()).add(c);
        }
    }

    public static Set<HeadGroup> getHeadGroups() {
        return group2classes.keySet();
    }

    public static LipidClass[] getClassesFor(HeadGroup group) {
        return group2classes.get(group).toArray(LipidClass[]::new);
    }

    private LipidClass(int chains, HeadGroup headgroup) {
        this.headgroup = headgroup;
        this.chains = chains;
        this.fragmentLib = null;
        this.sphingolipid = false;
        this.smiles = null;
        this.abbreviation = null;
    }
    private LipidClass(int chains, HeadGroup headgroup, FragmentLib lib) {
        this(chains, headgroup, lib, null, null);
    }
    private LipidClass(int chains, HeadGroup headgroup, FragmentLib lib, String smiles) {
        this(chains, headgroup, lib, smiles, null);
    }
    private LipidClass(int chains, HeadGroup headgroup, FragmentLib lib, String smiles, String abbrev) {
        this.headgroup = headgroup;
        this.chains = chains;
        this.fragmentLib = lib;
        this.sphingolipid = lib==null ? false : lib.hasSphingosin();
        this.smiles = smiles;
        this.abbreviation = abbrev;
    }


    public final HeadGroup headgroup;
    public final int chains;
    public final FragmentLib fragmentLib;
    protected final boolean sphingolipid;
    private final String smiles, abbreviation;

    public Optional<String> getSmiles() {
        return Optional.ofNullable(smiles);
    }

    public String abbr() {
        return abbreviation==null ? name() : abbreviation;
    }

    public boolean isSphingolipid() {
        return sphingolipid;
    }
}
