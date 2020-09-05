/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.function.Predicate;

class InstanceImportIteratorMS2Exp implements Iterator<Instance> {

    private final ProjectSpaceManager spaceManager;
    private final Iterator<Ms2Experiment> ms2ExperimentIterator;
    @NotNull
    private final Predicate<CompoundContainer> filter;

    private Instance next = null;


    public InstanceImportIteratorMS2Exp(@NotNull Iterator<Ms2Experiment> ms2ExperimentIterator, @NotNull ProjectSpaceManager spaceManager) {
       this(ms2ExperimentIterator,spaceManager, (c) -> true);
    }

    public InstanceImportIteratorMS2Exp(@NotNull Iterator<Ms2Experiment> ms2ExperimentIterator, @NotNull ProjectSpaceManager spaceManager, @NotNull Predicate<CompoundContainer> compoundFilter) {
        this.ms2ExperimentIterator = ms2ExperimentIterator;
        this.spaceManager = spaceManager;
        this.filter = compoundFilter;
    }



    @Override
    public boolean hasNext() {
        if (next != null)
            return true;

        if (ms2ExperimentIterator.hasNext()) {
            final Ms2Experiment input = ms2ExperimentIterator.next();
            @NotNull Instance inst = spaceManager.newCompoundWithUniqueId(input); //this writers

            if (input == null || !filter.test(inst.loadCompoundContainer(Ms2Experiment.class))) {
                LoggerFactory.getLogger(getClass()).info("Skipping instance " + inst.getID().getDirectoryName() + " because it does not match the Filter criterion.");
                return hasNext();
            } else {
                next = inst;
                return true;
            }
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
