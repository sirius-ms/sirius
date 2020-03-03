package de.unijena.bioinf.ms.frontend.subtools.projectspace;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ms.frontend.io.InstanceImporter;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Predicate;

public class ProjectSpaceWorkflow implements Workflow {


    private final RootOptions<?, ?> rootOptions;
    private final ProjecSpaceOptions projecSpaceOptions;

    public ProjectSpaceWorkflow(RootOptions<?, ?> rootOptions, ProjecSpaceOptions projecSpaceOptions) {
        this.rootOptions = rootOptions;
        this.projecSpaceOptions = projecSpaceOptions;
    }

    @Override
    public void run() {
        final ProjectSpaceManager space = rootOptions.getProjectSpace();
        final Predicate<CompoundContainerId> filter = projecSpaceOptions.getCombinedFilter();

        // remove non matching compounds from output space
        if (space.size() > 0){
            space.projectSpace().filteredIterator(c -> !filter.test(c)).forEachRemaining(id -> {
                try {
                    space.projectSpace().deleteCompound(id);
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Could not delete Instance with ID: " + id.getDirectoryName());
                }
            });
        }



        InputFilesOptions input = rootOptions.getInput();
//        if (space != null) {
//            if (input != null)
//                SiriusJobs.getGlobalJobManager().submitJob(new InstanceImporter(space, (exp) -> exp.getIonMass() < maxMz).makeImportJJob(input)).awaitResult();


    }
}
