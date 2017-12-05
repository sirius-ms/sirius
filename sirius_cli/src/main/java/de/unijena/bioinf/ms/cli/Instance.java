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
package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;

import java.io.File;

public class Instance {

    final MutableMs2Experiment experiment;
    final File file;
    boolean isMultipleInstancesPerFile;

    FTree optTree;

    public Instance(Ms2Experiment experiment, File file) {
        this.experiment = new MutableMs2Experiment(experiment);
        this.file = file;
    }

    public String fileNameWithoutExtension() {
        final String name = file.getName();
        final int i = name.lastIndexOf('.');
        if (i>=0) return name.substring(0, i);
        else return name;
    }
}
