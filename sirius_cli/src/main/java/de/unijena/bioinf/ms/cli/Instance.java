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
import de.unijena.bioinf.sirius.projectspace.Index;

import java.io.File;

public class Instance {
    public static final Instance NULL_INSTANCE = new Instance();

    final MutableMs2Experiment experiment;
    final File file;
    final int index;

    private Instance() {
        experiment = null;
        file = null;
        index = 0;
    }

    public Instance(Ms2Experiment experiment, File file, int index) {
        this.experiment = new MutableMs2Experiment(experiment);
        this.file = file;
        this.index = index;
        this.experiment.setAnnotation(Index.class, new Index(index));
    }

    public String fileNameWithoutExtension() {
        final String name = file.getName();
        final int i = name.lastIndexOf('.');
        if (i >= 0) return name.substring(0, i);
        else return name;
    }
}
