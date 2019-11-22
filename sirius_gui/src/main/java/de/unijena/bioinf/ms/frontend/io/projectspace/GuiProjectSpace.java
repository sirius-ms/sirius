package de.unijena.bioinf.ms.frontend.io.projectspace;

import ca.odell.glazedlists.BasicEventList;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ErrorListDialog;
import de.unijena.bioinf.ms.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.BatchImportDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.FileImportDialog;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.ContainerListener;
import de.unijena.bioinf.projectspace.ProjectSpaceIO;
import de.unijena.bioinf.projectspace.SiriusProjectSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

public class GuiProjectSpace {
    //    todo ringbuffer???
    protected static final Logger LOG = LoggerFactory.getLogger(GuiProjectSpace.class);
    public final BasicEventList<InstanceBean> COMPOUNT_LIST = new BasicEventList<>();

    private ContainerListener.Defined addListener, deleteListener;
    private ProjectSpaceManager projectSpace;

    public ProjectSpaceManager getProjectSpace() {
        return projectSpace;
    }

    public GuiProjectSpace(@NotNull ProjectSpaceManager projectSpaceManager) {
        changeProjectSpace(projectSpaceManager);
    }


    private void clearListener(@Nullable ContainerListener.Defined listener) {
        if (listener != null)
            listener.unregister();
    }

    public synchronized void changeProjectSpace(ProjectSpaceManager projectSpaceManager) {
        // clean old if available
        inEDTAndWait(COMPOUNT_LIST::clear);
        clearListener(addListener);
        clearListener(deleteListener);

        // add new project & listeners
        projectSpace = projectSpaceManager;

        // listen to add events
        addListener = projectSpace.projectSpace().defineCompoundListener().onCreate().thenDo((event) -> {
            inEDTAndWait(() -> COMPOUNT_LIST.add((InstanceBean) projectSpace.newInstanceFromCompound(event.getAffectedID(), Ms2Experiment.class)));
        }).register();

        // listen to delete events
        deleteListener = projectSpace.projectSpace().defineCompoundListener().onDelete().thenDo((event) -> {
            inEDTAndWait(() -> COMPOUNT_LIST.removeIf(inst -> event.getAffectedID().equals(inst.getID())));
        }).register();

        // add already existing compounds to reactive list
        inEDTAndWait(() -> projectSpace.forEach(intBean -> COMPOUNT_LIST.add((InstanceBean) intBean)));

        inEDTAndWait(() -> MF.setTitlePath(projectSpace.projectSpace().getRootPath().toString()));
    }

    private void inEDTAndWait(@NotNull final Runnable run) {
        if (SwingUtilities.isEventDispatchThread()) {
            run.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(run);
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void deleteCompound(@Nullable final InstanceBean inst) {
        if (inst == null)
            return;
        deleteCompound(inst.getID());
    }

    public void deleteCompound(@NotNull final CompoundContainerId id) {
        try {
            projectSpace.projectSpace().deleteCompound(id);
        } catch (IOException e) {
            LOG.error("Could not delete Compound: " + id, e);
        }
    }

    public synchronized void deleteAll() {
        COMPOUNT_LIST.iterator().forEachRemaining(this::deleteCompound);
    }


    public enum ImportMode {REPLACE, MERGE}

    public void importCompound(Ms2Experiment ex) {
        projectSpace.newCompoundWithUniqueId(ex);
    }

    public void importOneExperimentPerLocation(File... files) {
        importOneExperimentPerLocation(Arrays.asList(files));
    }

    public void importOneExperimentPerLocation(List<File> files) {
        FileImportDialog imp = new FileImportDialog(MF, files);
        importOneExperimentPerLocation(imp.getMSFiles(), imp.getMGFFiles(), null, null); //todo add PS support
    }

    public void importOneExperimentPerLocation(List<File> msFiles, List<File> mgfFiles, List<File> psFiles, List<File> psDirs) {
        BatchImportDialog batchDiag = new BatchImportDialog(MF);
        batchDiag.start(msFiles, mgfFiles);

        List<Ms2Experiment> ecs = batchDiag.getResults();
        List<String> errors = batchDiag.getErrors();

        ecs.forEach(this::importCompound);

        if (errors != null) {
            if (errors.size() > 1) {
                ErrorListDialog elDiag = new ErrorListDialog(MF, errors);
            } else if (errors.size() == 1) {
                ErrorReportDialog eDiag = new ErrorReportDialog(MF, errors.get(0));
            }

        }
    }

    public void importFromProjectSpace(@NotNull final ImportMode importMode, @NotNull final Path... selFile) {
        importFromProjectSpace(importMode, Arrays.asList(selFile));
    }

    public void importFromProjectSpace(@NotNull final List<Path> selFile) {
        importFromProjectSpace(ImportMode.REPLACE, selFile);
    }

    public void importFromProjectSpace(@NotNull final ImportMode importMode, @NotNull final List<Path> selFile) {
        Jobs.runInBackgroundAndLoad(MF, "Importing into Project-Space", new TinyBackgroundJJob<Boolean>() {
            @Override
            protected Boolean compute() {

                System.out.println("##### NOT IMPLEMENTED");
                //todo do import
                return true;
            }
        });
    }


    public void openProjectSpace(Path selFile) {
        Jobs.runInBackgroundAndLoad(MF, "Opening new Project...", () -> {
            SiriusProjectSpace ps = new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).openExistingProjectSpace(selFile);
            final ProjectSpaceManager psm = new ProjectSpaceManager(ps, new InstanceBeanFactory(), null, null);
            //todo we need to cancel all running computations here.
            System.out.println("todo we need to cancel all running computations here!");
            MF.getPS().changeProjectSpace(psm);
            return psm;
        });

    }

    public void moveProjectSpace(File newlocation) {
        //todo implement save as
        System.out.println("TODO implement save as");
        /*final IOException ex = Jobs.runInBackgroundAndLoad(MF, "Moving Project to '" + newlocation.getAbsolutePath() + "'", () -> {
            try {
                projectSpace.projectSpace().move(newlocation);
                inEDTAndWait(() -> MF.setTitlePath(projectSpace.projectSpace().getRootPath().toString()));
                return null;
            } catch (IOException e) {
                return e;
            }
        }).getResult();

        if (ex != null)
            new ExceptionDialog(MF, ex.getMessage());*/
    }

    /**
     * Exports the project space as compressed archive to the given files
     *
     * @param zipFile The path where the archive will be saved
     * @throws IOException Thrown if writing of archive fails
     */
    public void exportAsProjectArchive(Path zipFile) throws IOException {
        final IOException ex = Jobs.runInBackgroundAndLoad(MF, "Exporting Project to '" + zipFile.toString() + "'", () -> {
            try {
                ProjectSpaceIO.toZipProjectSpace(projectSpace.projectSpace(), zipFile, ProjectSpaceManager.defaultSummarizer());
                return null;
            } catch (IOException e) {
                return e;
            }
        }).getResult();

        if (ex != null)
            new ExceptionDialog(MF, ex.getMessage());
    }



}
