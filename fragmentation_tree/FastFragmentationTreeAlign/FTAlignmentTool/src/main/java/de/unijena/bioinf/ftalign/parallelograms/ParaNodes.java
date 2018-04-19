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
package de.unijena.bioinf.ftalign.parallelograms;

import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;

public class ParaNodes {

    private Fragment x;
    private Fragment y;
    private Fragment u;
    private Fragment v;

    public ParaNodes(Fragment x, Fragment y, Fragment u, Fragment v) {
        this.x = x;
        this.y = y;
        this.u = u;
        this.v = v;
    }

    public Fragment getX() {
        return x;
    }

    public Fragment getY() {
        return y;
    }

    public Fragment getU() {
        return u;
    }

    public Fragment getV() {
        return v;
    }

}
