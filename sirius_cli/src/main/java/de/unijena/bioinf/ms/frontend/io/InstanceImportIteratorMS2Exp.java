package de.unijena.bioinf.ms.frontend.io;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ms.frontend.io.projectspace.Instance;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
