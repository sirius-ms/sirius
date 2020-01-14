package de.unijena.bioinf.ms.frontend.io.projectspace;

import ca.odell.glazedlists.BasicEventList;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.io.InstanceImporter;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.ProjectSpaceIO;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;
import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.inEDTAndWait;

public class GuiProjectSpaceManager extends ProjectSpaceManager {
    //    todo ringbuffer???
    protected static final Logger LOG = LoggerFactory.getLogger(GuiProjectSpaceManager.class);
    public final BasicEventList<InstanceBean> COMPOUNT_LIST;

//    private ContainerListener.Defined addListener, deleteListener;

    public GuiProjectSpaceManager(@NotNull SiriusProjectSpace space) {
        this(space, new BasicEventList<>());
    }

    public GuiProjectSpaceManager(@NotNull SiriusProjectSpace space, BasicEventList<InstanceBean> compoundList) {
        this(space, compoundList, null, null);
    }

    public GuiProjectSpaceManager(@NotNull SiriusProjectSpace space, BasicEventList<InstanceBean> compoundList, @Nullable Function<Ms2Experiment, String> formatter, @Nullable Predicate<CompoundContainerId> compoundFilter) {
        super(space, new InstanceBeanFactory(), formatter, compoundFilter);
        COMPOUNT_LIST = compoundList;
        inEDTAndWait(() -> {
            COMPOUNT_LIST.clear();
            forEach(ins -> COMPOUNT_LIST.add((InstanceBean) ins));
        });


    }

//    private ProjectSpaceManager projectSpace;

//    public ProjectSpaceManager getProjectSpace() {
//        return projectSpace;
//    }

    /*public GuiProjectSpace(@NotNull ProjectSpaceManager projectSpaceManager) {
        changeProjectSpace(projectSpaceManager);
    }*/



/*
    private void clearListener(@Nullable ContainerListener.Defined listener) {
        if (listener != null)
            listener.unregister();
    }*/


    /*public synchronized void changeProjectSpace(ProjectSpaceManager projectSpaceManager) {
        // clean old if available
        inEDTAndWait(COMPOUNT_LIST::clear);
        *//*clearListener(addListener);
        clearListener(deleteListener);

        // add new project & listeners
        projectSpace = projectSpaceManager;

        //todo why are imported compounds do not contain results

        // listen to add events
        addListener = projectSpace.projectSpace().defineCompoundListener().onCreate().thenDo((event) -> {
            inEDTAndWait(() -> COMPOUNT_LIST.add((InstanceBean) projectSpace.newInstanceFromCompound(event.getAffectedID(), Ms2Experiment.class)));
        }).register();

        // listen to delete events
        deleteListener = projectSpace.projectSpace().defineCompoundListener().onDelete().thenDo((event) -> {
            inEDTAndWait(() -> COMPOUNT_LIST.removeIf(inst -> event.getAffectedID().equals(inst.getID())));
        }).register();

        // add already existing compounds to reactive list
        inEDTAndWait(() -> projectSpace.forEach(intBean -> COMPOUNT_LIST.add((InstanceBean) intBean)));*//*


    }*/

    public void deleteCompounds(@Nullable final List<InstanceBean> insts) {
        if (insts == null || insts.isEmpty())
            return;
        Jobs.runInBackgroundAndLoad(MF, "Deleting Compounds...", false, new TinyBackgroundJJob<Boolean>() {
            @Override
            protected Boolean compute() throws Exception {
                final AtomicInteger pro = new AtomicInteger(0);
                updateProgress(0, insts.size(), pro.get(), "Deleting...");
                insts.iterator().forEachRemaining(inst -> {
                    try {
                        inEDTAndWait(() -> COMPOUNT_LIST.remove(inst));
                        projectSpace().deleteCompound(inst.getID());
                    } catch (IOException e) {
                        LOG.error("Could not delete Compound: " + inst.getID(), e);
                    } finally {
                        updateProgress(0, insts.size(), pro.incrementAndGet(), "Deleting...");
                    }
                });
                return true;
            }
        });

    }

    /*public void deleteCompound(@NotNull final CompoundContainerId id) {
        try {
            projectSpace().deleteCompound(id);

        } catch (IOException e) {
            LOG.error("Could not delete Compound: " + id, e);
        }
    }*/

    public synchronized void deleteAll() {
        deleteCompounds(COMPOUNT_LIST);
    }

//    public enum ImportMode {REPLACE, MERGE}


    @Override
    public @NotNull Instance newCompoundWithUniqueId(Ms2Experiment inputExperiment) {
        final InstanceBean inst = (InstanceBean) super.newCompoundWithUniqueId(inputExperiment);
        inEDTAndWait(() -> COMPOUNT_LIST.add(inst));
        return inst;
    }

    public InputFilesOptions importOneExperimentPerLocation(@NotNull final List<File> rawFiles) {
        final InputFilesOptions.MsInput inputFiles = Jobs.runInBackgroundAndLoad(MF, "Analyzing Files...", false, InstanceImporter.makeExpandFilesJJob(rawFiles)).getResult();
        final InputFilesOptions inputF = new InputFilesOptions();
        inputF.msInput = inputFiles;
        importOneExperimentPerLocation(inputF);
        return inputF;
    }

    public void importOneExperimentPerLocation(@NotNull final InputFilesOptions input) {
        InstanceImporter importer = new InstanceImporter(this, Double.MAX_VALUE);
        Jobs.runInBackgroundAndLoad(MF, "Auto-Importing supported Files...", true, importer.makeImportJJob(input));
    }

    protected void copy(Path newlocation, boolean switchLocation) {
        String header = switchLocation ? "Saving Project to" : "Saving a Copy to";
        final IOException ex = Jobs.runInBackgroundAndLoad(MF, header + " '" + newlocation.toString() + "'...", () -> {
            try {
                ProjectSpaceIO.copyProject(projectSpace(), newlocation, switchLocation);
                inEDTAndWait(() -> MF.setTitlePath(projectSpace().getLocation().toString()));
                return null;
            } catch (IOException e) {
                return e;
            }
        }).getResult();

        if (ex != null)
            new ExceptionDialog(MF, ex.getMessage());
    }

    /**
     * Saves (copy) the project to a new location. New location is active
     *
     * @param newlocation The path where the project will be saved
     * @throws IOException Thrown if writing of archive fails
     */
    public void saveAs(Path newlocation) {
        copy(newlocation, true);
    }

    /**
     * Saves a copy of the project to a new location. Original location will be the active project.
     *
     * @param copyLocation The path where the project will be saved
     * @throws IOException Thrown if writing of archive fails
     */
    public void saveCopy(Path copyLocation) throws IOException {
        copy(copyLocation, false);
    }


}
