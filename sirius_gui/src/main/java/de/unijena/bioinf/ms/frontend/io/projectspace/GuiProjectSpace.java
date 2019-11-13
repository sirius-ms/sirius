package de.unijena.bioinf.ms.frontend.io.projectspace;

import ca.odell.glazedlists.BasicEventList;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ErrorListDialog;
import de.unijena.bioinf.ms.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.BatchImportDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.FileImportDialog;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.ContainerListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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

    /*// close and cleanup PS
    // ask user etc
    public void destroy() {
        try {

            projectSpace.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/


    /**
     * Exports the project space as compressed archive to the given files
     *
     * @param file The path where the archive will be saved
     * @throws IOException Thrown if writing of archive fails
     */
    public void exportAsProjectArchive(File file) throws IOException {
        System.out.println("##### NOT IMPLEMENTED");
        //todo save as and compress (archive)
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

    public void importFromProjectSpace(@NotNull final ImportMode importMode, @NotNull final File... selFile) {
        importFromProjectSpace(importMode, Arrays.asList(selFile));
    }

    public void importFromProjectSpace(@NotNull final List<File> selFile) {
        importFromProjectSpace(ImportMode.REPLACE, selFile);
    }

    public void importFromProjectSpace(@NotNull final ImportMode importMode, @NotNull final List<File> selFile) {
        Jobs.runInBackgroundAndLoad(MF, "Importing into Project-Space", new TinyBackgroundJJob<Boolean>() {
            @Override
            protected Boolean compute() {

                System.out.println("##### NOT IMPLEMENTED");
                //todo do import
                return true;
            }
        });
    }





    /*


private static String escapeFileName(String name) {
        final String n = name.replaceAll("[:\\\\/*\"?|<>']", "");
        if (n.length() > 128) {
            return n.substring(0, 128);
        } else return n;
    }



    }*/


























    /*public void importCompound(@NotNull final ExperimentResultBean ec) {
        SwingUtilities.invokeLater(() -> {
            cleanExperiment(ec.getMs2Experiment());

            //adding experiment to gui
            addToCompoundList(ec);

            //listen to changes to decide if a compound has to be rewritten
            addListener(ec);

            //write experiment to project-space
            writeToProjectSpace(ec.getExperimentResult());

        });
    }

    //todo make thread safe and check property changes
    private void addListener(final ExperimentResultBean ec) {
        ec.addPropertyChangeListener(evt -> {
            changed.add(ec.getExperimentResult());
        });
    }

    public void importCompound(@NotNull final MutableMs2Experiment ex) {
        importCompound(new ExperimentResultBean(ex));
    }

    private void addToCompoundList(@NotNull final ExperimentResultBean ec) {
        addName(ec);
        COMPOUNT_LIST.add(ec);
    }

    public void writeToProjectSpace(@NotNull final ExperimentResult... exResults) {
        writeToProjectSpace(Arrays.asList(exResults).iterator());
    }

    public void writeToProjectSpace(@NotNull final Iterator<ExperimentResult> exResults) {
        SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<ExperimentDirectory>(JJob.JobType.IO) {
            @Override
            protected ExperimentDirectory compute() throws Exception {
                while (exResults.hasNext()) {
                    final ExperimentResult exResult = exResults.next();
                    projectSpace.writeExperiment(exResult);
                    changed.remove(exResult); //todo check locking
                }
                return null;
            }
        });
    }

    public void writeChangedCompounds() {
        writeToProjectSpace(changed.iterator());
    }

    //todo check
    public void writeSummary() {
        writeChangedCompounds();
        projectSpace.writeSummaries(COMPOUNT_LIST.stream().map(ExperimentResultBean::getExperimentResult).collect(Collectors.toList()));
    }


    private void cleanExperiment(@NotNull final MutableMs2Experiment ex) {
        //search for a missing molecular formula before cleaning the annotations
        if (ex.getMolecularFormula() == null) {
            String f = extractMolecularFormulaString(ex);
            if (f != null && !f.isEmpty())
                ex.setMolecularFormula(MolecularFormula.parseOrThrow(f));
        }

        if (ex.getPrecursorIonType() == null) {
            LoggerFactory.getLogger(getClass()).warn("Input experiment with name '" + ex.getName() + "' does not have a charge nor an ion type annotation.");
            ex.setPrecursorIonType(PeriodicTable.getInstance().getUnknownPrecursorIonType(1));
        }
        clearExperimentAnotations(ex);
        addIonToPeriodicTable(ex.getPrecursorIonType());
    }

    public void remove(ExperimentResultBean... containers) {
        remove(Arrays.asList(containers));
    }

    public void remove(List<ExperimentResultBean> containers) {
        for (ExperimentResultBean ec : containers) {
            NAMES.remove(ec.getGUIName());
            try {
                projectSpace.deleteExperiment(ec.getProjectSpaceID());
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).warn("Could not delete Compound: " + ec.getGUIName(), e);
            }
        }
        COMPOUNT_LIST.removeAll(containers);
    }



    // region static import helper methods
    public void changeName(ExperimentResultBean ec, String old) {
        if (NAMES.containsKey(old)) {
            TIntSet indeces = NAMES.get(old);
            indeces.remove(ec.getNameIndex());

            if (indeces.isEmpty())
                NAMES.remove(old);
        }
        addName(ec);
    }

    public void addName(ExperimentResultBean ec) {
        if (ec.getName() == null || ec.getName().isEmpty()) {
            ec.setName("Unknown");
        } else {
            final TIntSet indeces = NAMES.putIfAbsent(ec.getName(), new TIntHashSet());
            assert indeces != null;

            int counter = 1;
            while (indeces.contains(counter))
                counter++;
            indeces.add(counter);

            ec.setNameIndex(counter);
        }
    }





    //endregion

    public static SiriusProjectSpace createGuiProjectSpace(File location) throws IOException {
        FilenameFormatter formatter = null;
        String formatterString = SiriusProperties.getProperty("de.unijena.bioinf.sirius.projectspace.formatter");
        if (formatterString != null) {
            try {
                formatter = new StandardMSFilenameFormatter(formatterString);
            } catch (ParseException e) {
                LoggerFactory.getLogger(GuiProjectSpace.class).error("Could not parse Formatter String." + formatterString + " Using default Formatter instead!", e);
            }
        }
        final @NotNull SiriusProjectSpace space = SiriusProjectSpaceIO.create(formatter, location,
                (cur, max, mess) -> {//todo progress listener
                },
                new IdentificationResultSerializer(), new FingerIdResultSerializer(ApplicationCore.WEB_API), new CanopusResultSerializer(ApplicationCore.CANOPUS));
        space.registerSummaryWriter(new MztabSummaryWriter());
        return space;
    }*/
}
