package de.unijena.bioinf.ms.frontend.io.projectspace;

import ca.odell.glazedlists.BasicEventList;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ErrorListDialog;
import de.unijena.bioinf.ms.gui.dialogs.ErrorReportDialog;
import de.unijena.bioinf.ms.gui.mainframe.BatchImportDialog;
import de.unijena.bioinf.ms.gui.mainframe.FileImportDialog;
import de.unijena.bioinf.projectspace.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

public class GuiProjectSpace {
    protected static final Logger LOG = LoggerFactory.getLogger(GuiProjectSpace.class);
    public final BasicEventList<InstanceBean> COMPOUNT_LIST = new BasicEventList<>();
    private final ContainerListener.Defined addListener, deleteListener;

//    todo ringbuffer???
//    private final Map<CompoundContainerId, InstanceBean> idToWrapperBean = new ConcurrentHashMap<>();

    public final ProjectSpaceManager projectSpace;

    public GuiProjectSpace(@NotNull ProjectSpaceManager projectSpaceManager) {
        projectSpace = projectSpaceManager;

        // add already existing compounds to reactive list
        projectSpace.forEach(intBean -> COMPOUNT_LIST.add((InstanceBean) intBean));

        // listen to add events
        addListener = projectSpace.projectSpace().defineCompoundListener().onCreate().thenDo((event) -> {
            COMPOUNT_LIST.add((InstanceBean) projectSpace.newInstanceFromCompound(event.getAffectedID()));
        }).register();

        // listen to delete events
        deleteListener = projectSpace.projectSpace().defineCompoundListener().onDelete().thenDo((event) -> {
            COMPOUNT_LIST.removeIf(inst -> event.getAffectedID().equals(inst.getID()));
        }).register();
    }


    public void importCompound(Ms2Experiment ex) {
        projectSpace.newCompoundWithUniqueId(ex);
    }

    public void deleteCompound(final InstanceBean inst) {
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

    public synchronized void clear() {
        COMPOUNT_LIST.iterator().forEachRemaining(this::deleteCompound);
    }


    public enum ImportMode {REPLACE, MERGE}

    public void importFromProjectSpace(@NotNull final ImportMode importMode, @NotNull final File... selFile) {
        importFromProjectSpace(importMode, Arrays.asList(selFile));
    }

    public void importFromProjectSpace(@NotNull final Collection<File> selFile) {
        importFromProjectSpace(ImportMode.REPLACE, selFile);
    }

    public void importFromProjectSpace(@NotNull final ImportMode importMode, @NotNull final Collection<File> selFile) {
        Jobs.runInBackroundAndLoad(MF, "Importing into Project-Space", (TinyBackgroundJJob) new TinyBackgroundJJob<Boolean>() {
            @Override
            protected Boolean compute() {
                System.out.println("##### NOT IMPLEMENTED");
                //todo do import
                return true;
            }
        }.asIO());
    }

    public void exportAsProjectSpace(File file) throws IOException {
        System.out.println("##### NOT IMPLEMENTED");
        //todo save as and compress (archive)
    }


    public void importOneExperimentPerFile(File... files) {
        importOneExperimentPerFile(Arrays.asList(files));
    }

    public void importOneExperimentPerFile(List<File> files) {
        FileImportDialog imp = new FileImportDialog(MF, files);
        importOneExperimentPerFile(imp.getMSFiles(), imp.getMGFFiles());
    }

    public void importOneExperimentPerFile(List<File> msFiles, List<File> mgfFiles) {
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
