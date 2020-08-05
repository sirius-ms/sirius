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

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import java.io.File;
import java.util.List;

public interface ComputeOpts {

    @Option(longName = "3d", description = "compute 3D fingerprints instead of 2D (if 3D InChIs are provided")
    boolean is3D();

    @Option(shortName = "o", description = "write output to stdout. If input comes from STDIN, it is written to STDOUT by default.")
    boolean isStdout();

    @Unparsed(description = "either files with InChi strings or tab separated tables containing an InChI column")
    List<File> getInput();

    @Option(longName = "all")
    boolean isAll();

    @Option(shortName = "C")
    boolean isCfm();

    @Option(shortName = "M")
    boolean isMaccs();

    @Option(shortName = "P")
    boolean isPubchem();

    @Option(shortName = "K")
    boolean isKlekotha();

    @Option(shortName = "O")
    boolean isOpenbabel();

    @Option(shortName = "S")
    boolean isSpherical();

    @Option(shortName = "A")
    boolean isPath();

    @Option(shortName = "N")
    boolean isNeighbourhood();

    @Option(shortName = "X")
    boolean isExtended();
}
