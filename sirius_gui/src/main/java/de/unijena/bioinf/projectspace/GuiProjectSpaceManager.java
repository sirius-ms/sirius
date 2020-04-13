package de.unijena.bioinf.projectspace;

import ca.odell.glazedlists.BasicEventList;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;
import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.inEDTAndWait;

public class GuiProjectSpaceManager extends ProjectSpaceManager {
    //    todo ringbuffer???
    protected static final Logger LOG = LoggerFactory.getLogger(GuiProjectSpaceManager.class);
    public final BasicEventList<InstanceBean> INSTANCE_LIST;
//    private final Set<InstanceBean> COMPUTING_INTANCES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public GuiProjectSpaceManager(@NotNull SiriusProjectSpace space) {
        this(space, new BasicEventList<>());
    }

    public GuiProjectSpaceManager(@NotNull SiriusProjectSpace space, BasicEventList<InstanceBean> compoundList) {
        this(space, compoundList, null);
    }

    public GuiProjectSpaceManager(@NotNull SiriusProjectSpace space, BasicEventList<InstanceBean> compoundList, @Nullable Function<Ms2Experiment, String> formatter) {
        super(space, new InstanceBeanFactory(), formatter);
        INSTANCE_LIST = compoundList;
        inEDTAndWait(() -> {
            INSTANCE_LIST.clear();
            forEach(ins -> INSTANCE_LIST.add((InstanceBean) ins));
        });

        ContainerListener.Defined createListener = projectSpace().defineCompoundListener().onCreate().thenDo((event -> {
            final InstanceBean inst = (InstanceBean) newInstanceFromCompound(event.getAffectedID(), Ms2Experiment.class);
            inEDTAndWait(() -> INSTANCE_LIST.add(inst));
        })).register();
    }

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
                        inEDTAndWait(() -> INSTANCE_LIST.remove(inst));
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

    public synchronized void deleteAll() {
        deleteCompounds(INSTANCE_LIST);
    }

    public InputFilesOptions importOneExperimentPerLocation(@NotNull final List<File> rawFiles) {
        final InputFilesOptions.MsInput inputFiles = Jobs.runInBackgroundAndLoad(MF, "Analyzing Files...", false, InstanceImporter.makeExpandFilesJJob(rawFiles)).getResult();
        final InputFilesOptions inputF = new InputFilesOptions();
        inputF.msInput = inputFiles;
        importOneExperimentPerLocation(inputF);
        return inputF;
    }

    public void importOneExperimentPerLocation(@NotNull final InputFilesOptions input) {
        InstanceImporter importer = new InstanceImporter(this, x -> true, x -> true);
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
