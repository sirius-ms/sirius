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

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Quantification;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;

class InstanceImportIteratorMS2Exp implements Iterator<Instance> {

    private final ProjectSpaceManager<?> spaceManager;
    private final Iterator<Ms2Experiment> ms2ExperimentIterator;
    @NotNull
    private final Predicate<CompoundContainer> filter;

    private Instance next = null;


    public InstanceImportIteratorMS2Exp(@NotNull Iterator<Ms2Experiment> ms2ExperimentIterator, @NotNull ProjectSpaceManager<?> spaceManager) {
       this(ms2ExperimentIterator,spaceManager, (c) -> true);
    }

    public InstanceImportIteratorMS2Exp(@NotNull Iterator<Ms2Experiment> ms2ExperimentIterator, @NotNull ProjectSpaceManager<?> spaceManager, @NotNull Predicate<CompoundContainer> compoundFilter) {
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

            // TODO: hacky solution
            // store LC/MS data into project space
            // might change in future. Its important that the trace is written after
            // importing from mzml/mzxml
            if (input!=null){
                LCMSPeakInformation lcmsInfo = input.getAnnotation(LCMSPeakInformation.class, LCMSPeakInformation::empty);
                if (lcmsInfo.isEmpty()) {
                    // check if there are quantification information
                    // grab them and remove them
                    final Quantification quant = input.getAnnotationOrNull(Quantification.class);
                    if (quant!=null) {
                        lcmsInfo = new LCMSPeakInformation(quant.asQuantificationTable());
                        input.removeAnnotation(Quantification.class);
                    }
                }
                if (!lcmsInfo.isEmpty()) {
                    // store this information into the compound container instead
                    final CompoundContainer compoundContainer = inst.loadCompoundContainer(LCMSPeakInformation.class);
                    final Optional<LCMSPeakInformation> annotation = compoundContainer.getAnnotation(LCMSPeakInformation.class);
                    if (annotation.orElseGet(LCMSPeakInformation::empty).isEmpty()) {
                        compoundContainer.setAnnotation(LCMSPeakInformation.class, lcmsInfo);
                        inst.updateCompound(compoundContainer,LCMSPeakInformation.class);
                    }
                }
                // remove annotation from experiment
                {
                    final Ms2Experiment exp = inst.getExperiment();
                    exp.removeAnnotation(LCMSPeakInformation.class);
                    exp.removeAnnotation(Quantification.class);
                    inst.updateExperiment();
                }
            }

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
