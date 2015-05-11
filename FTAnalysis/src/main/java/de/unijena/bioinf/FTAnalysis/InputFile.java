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
package de.unijena.bioinf.FTAnalysis;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

import java.io.File;

/**
 * Created by kaidu on 04.02.14.
 */
public class InputFile {

    private Ms2Experiment experiment;
    private File fileName;

    public InputFile(Ms2Experiment experiment, File fileName) {
        this.experiment = experiment;
        this.fileName = fileName;
    }

    public Ms2Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Ms2Experiment experiment) {
        this.experiment = experiment;
    }

    public File getFileName() {
        return fileName;
    }

    public void setFileName(File fileName) {
        this.fileName = fileName;
    }
}
