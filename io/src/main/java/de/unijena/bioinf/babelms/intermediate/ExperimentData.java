/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms.intermediate;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Intermediate format to be parsed into an Ms2Experiment.
 * Does not need to be complete and have valid values.
 */
@Data
@Builder
public class ExperimentData {
    /**
     * Only for error reporting purposes
     */
    private String id;

    private SimpleSpectrum spectrum;
    private String spectrumLevel;
    private String splash;

    private String precursorMz;
    private String precursorIonType;

    private String instrumentation;
    private String collisionEnergy;
    private String retentionTime;

    private String compoundName;
    private String molecularFormula;

    private String inchi;
    private String inchiKey;
    private String smiles;

    private List<String> tags;
}
