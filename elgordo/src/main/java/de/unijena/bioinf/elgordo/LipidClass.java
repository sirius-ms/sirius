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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static de.unijena.bioinf.elgordo.FragmentLib.def;
import static de.unijena.bioinf.elgordo.HeadGroup.*;

public enum LipidClass {

    MG(1,	Glycerol, def("[M+NH3+H]+").losses("","OH", "H2O","H3O2").acyl("H2O").done()),
    DG(2,	Glycerol, def("[M+NH3+H]+").losses("","OH", "H2O","H3O2").acyl("","H2O","C3H5O").acylFragments("","C3H5O","C3H7O2").
                              def("[M+Na]+").losses("H2O").adductSwitch().acyl("").acylFragments("","-O").done()),
    TG(3,	Glycerol, def("[M+NH3+H]+").losses("","OH", "H2O","H3O2").acyl("","H2O","C3H5O").acylFragments("-OH","-H3O2","C3H5O").def("[M+Na]+").adductSwitch().acyl("","H2O").acylFragments("-OH","-H3O2").done()),
    DGTS(	2,	Glyceroltrimethylhomoserin, def("+").fragments("C10H21NO5","C7H13NO2").acyl("","H2O").def("[M+H]+").done()),
    LDGTS(	1,	Glyceroltrimethylhomoserin, def("+").fragments("C10H21NO5","C7H13NO2").losses("H2O").acyl("").def("[M+H]+").done()),
    MGDG(	2,	Galactosylglycerol, def("+").losses("C6H11O6","C6H13O7").acyl("","-C6H11O6","C3H5O").def("[M+NH3+H]+").def("[M+Na]+").done()),
    DGDG(	2,	Digalactosyldiacylglycerol,def("+").losses("C12H21O11","C12H23O12").acyl("","H2O","C3H5O").def("[M+NH3+H]+").def("[M+Na]+").done()),
    SQDG(	2,	Sulfoquinovosylglycerols,def("+").losses("C6H10O7S","C6H12O8S").acyl("","H2O","C3H5O").acylFragments("").def("[M+NH3+H]+").done()),
    SQMG(	1,	Sulfoquinovosylglycerols),
    PC(	2,	Glycerophosphocholines, def("+").fragments("C2H5O4P").losses("C3H9N","C5H14NO4P").alkyl("","H","H2O").acyl("","H2O").plasmalogenFragment("C21H39O").
                                            def("[M+H]+").fragments("C5H13NO","C5H14NO4P").def("[M+Na]+").adductSwitch().acyl("-C3H9N","C-3H-7N-O").done()),
    LPC(	1,	Glycerophosphocholines, def("[M+H]+").fragments("C5H13NO","C5H14NO4P","C2H5O4P").losses("H2O","C5H14NO4P").acyl("","H2O").def("[M+Na]+").fragments("C5H13NO","C5H14NO4P","C2H5O4P").losses("C3H9N","C5H14NO4P").acyl("").done()),
    PE(	2,	Glycerophosphoethanolamines, def("[M+H]+").losses("C2H8NO4P","C2H5N").acylFragments("", "-OH","-H3O2").acyl("","H2O").def("[M+Na]+").fragments("H3PO4","C2H6NO3P","C2H8NO4P").losses("C2H5N","C2H6NO3P","C2H8NO4P","C2H6NO4P").acyl("","-C2H5N").acylFragments("").done()),
    LPE(	1,	Glycerophosphoethanolamines, def("[M+H]+").fragments("C3H7O5P").losses("H2O","C2H7NO","C2H8NO4P","C3H9O6P").acyl("","-C2H6NO3P").acylFragments("").def("[M+Na]+").fragments("H3PO4","C2H8NO4P","C3H7O5P","C3H9O6P").losses("C2H5N","C2H7NO","C2H6NO3P","C2H8NO4P","C2H6NO4P").acylFragments("").acyl("","H2O").done()),
    PS(	2,	Glycerophosphoserines, def("[M+H]+").losses("C3H8NO6P").acyl("","H2O", "-C3H6NO5P").done()),
    LPS(	1,	Glycerophosphoserines, def("[M+H]+").fragments("C3H8NO3").losses("H2O","C3H8NO6P").acyl("","H2O").done()),
    PG(	2,	Glycerophosphoglycerols, def("+").losses("H2O","C3H9O6P").acyl("","H2O").acylFragments("","-H2O").def("[M+Na]+").losses("C3H8O6P","C3H7O5P").acyl("-C3H6O2").def("[M+H]+").def("[M+NH3+H]+").done()),
    LPG(	1,	Glycerophosphoglycerols),
    PI(	2,	Glycerophosphoinositols, def("[M+H]+").losses("C6H10O5","C6H12O6","C6H11O8P","C6H13O9P").acyl("","H2O").done()),
    LPI(	1,	Glycerophosphoinositols),
    PA(	2,	Glycerophosphates),
    LPA(	1,	Glycerophosphates),
    CL(	4,	Glycerophosphoglycerophosphoglycerols, def("[M+NH3+H]+").acyl("").acylFragments("", "C3H5").done()),

    Cer(2, NoHeadGroup, def("[M+H]+").losses("H2O","H4O2").sphingosinFragments("","H2O","H4O2","CH4O2").sphingosinLosses("","H2O","H4O2","CH2O","-NH3").done()),
    Sm(2, Phosphocholin, def("+").fragments("C5H13NO","C2H5O4P","C5H14NO4P").losses("H2O","C3H11NO").sphingosinFragments("").sphingosinLosses("").def("[M+H]+").def("[M+Na]+").fragments("C5H15NO5P").losses("C3H9N").done());


    protected static HashMap<HeadGroup, List<LipidClass>> group2classes;
    protected final boolean sphingolipid;

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
        this.sphingolipid=false;
    }
    private LipidClass(int chains, HeadGroup headgroup, FragmentLib lib) {
        this.headgroup = headgroup;
        this.chains = chains;
        this.fragmentLib = lib;
        this.sphingolipid = lib.hasSphingosin();
    }

    public final HeadGroup headgroup;
    public final int chains;
    public final FragmentLib fragmentLib;

    public String abbr() {
        return name();
    }

    public boolean isSphingolipid() {
        return sphingolipid;
    }
}
