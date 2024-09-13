/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Objects;

class InstanceImportIteratorMS2Exp implements Iterator<Instance> {

    private static final Logger log = LoggerFactory.getLogger(InstanceImportIteratorMS2Exp.class);
    private final ProjectSpaceManager spaceManager;
    private final Iterator<Ms2Experiment> ms2ExperimentIterator;

    private Instance next = null;


    public InstanceImportIteratorMS2Exp(@NotNull Iterator<Ms2Experiment> ms2ExperimentIterator, @NotNull ProjectSpaceManager spaceManager) {
        this.ms2ExperimentIterator = ms2ExperimentIterator;
        this.spaceManager = spaceManager;
    }


    @Override
    public boolean hasNext() {
        if (next != null)
            return true;

        while (ms2ExperimentIterator.hasNext()) {
            final Ms2Experiment input = ms2ExperimentIterator.next();
            try {
                @NotNull Instance inst = spaceManager.importInstanceWithUniqueId(input); //this writers
                next = inst;
            } catch (Exception e) {
                log.warn("Error importing instance rt ={}, mz={}, name={}. Message: {}",
                        input.getAnnotation(RetentionTime.class).map(Objects::toString).orElse("N/A"),
                        Math.round(input.getIonMass()), input.getName(), e.getMessage());
                continue;
            }
            return true;
        }
        return false;
    }

    @Override
    public Instance next() {
        try {
            if (!hasNext())
                return null;
            return next;
        } finally {
            next = null;
        }
    }

    public void importAll() {
        while (hasNext())
            next();
    }
}
