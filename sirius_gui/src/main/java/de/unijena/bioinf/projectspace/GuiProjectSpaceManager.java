package de.unijena.bioinf.projectspace;

import ca.odell.glazedlists.BasicEventList;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.subtools.lcms_align.LcmsAlignSubToolJob;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;
import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.inEDTAndWait;

public class GuiProjectSpaceManager extends ProjectSpaceManager {
    protected static final Logger LOG = LoggerFactory.getLogger(GuiProjectSpaceManager.class);
    public final BasicEventList<InstanceBean> INSTANCE_LIST;


    protected final InstanceBuffer ringBuffer;

    public GuiProjectSpaceManager(@NotNull SiriusProjectSpace space, int maxBufferSize) {
        this(space, new BasicEventList<>(), maxBufferSize);
    }

    public GuiProjectSpaceManager(@NotNull SiriusProjectSpace space, BasicEventList<InstanceBean> compoundList, int maxBufferSize) {
        this(space, compoundList, null, maxBufferSize);
    }

    public GuiProjectSpaceManager(@NotNull SiriusProjectSpace space, BasicEventList<InstanceBean> compoundList, @Nullable Function<Ms2Experiment, String> formatter, int maxBufferSize) {
        super(space, new InstanceBeanFactory(), formatter);
        this.ringBuffer = new InstanceBuffer(maxBufferSize);
        this.INSTANCE_LIST = compoundList;
        final ArrayList<InstanceBean> buf = new ArrayList<>(size());
        forEach(it -> buf.add((InstanceBean) it));
        inEDTAndWait(() -> {
            INSTANCE_LIST.clear();
            INSTANCE_LIST.addAll(buf);
        });

        projectSpace().defineCompoundListener().onCreate().thenDo((event -> {
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
                        ringBuffer.remove(inst);
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


    public void importOneExperimentPerLocation(@NotNull final List<File> inputFiles) {
        final InputFilesOptions inputF = new InputFilesOptions();
        inputF.msInput = Jobs.runInBackgroundAndLoad(MF, "Analyzing Files...", false,
                InstanceImporter.makeExpandFilesJJob(inputFiles)).getResult();
        importOneExperimentPerLocation(inputF);
    }

    public void importOneExperimentPerLocation(@NotNull final InputFilesOptions input) {
        boolean align = Jobs.runInBackgroundAndLoad(MF, "Checking for alignable input...", () ->
                (input.msInput.msParserfiles.size() > 1 && input.msInput.projects.size() == 0 && input.msInput.msParserfiles.stream().map(p -> p.getFileName().toString().toLowerCase()).allMatch(n -> n.endsWith(".mzml") || n.endsWith(".mzxml"))))
                .getResult();

        // todo this is hacky we need some real view for that at some stage.
        if (align)
            align = new QuestionDialog(MF, "<html><body> You inserted multiple LC-MS/MS Runs. <br> Do you want to Align them during import?</br></body></html>"/*, DONT_ASK_OPEN_KEY*/).isSuccess();

        if (align) {
            Jobs.runInBackgroundAndLoad(MF, new LcmsAlignSubToolJob(input, this));
        } else {
            InstanceImporter importer = new InstanceImporter(this, x -> true, x -> true);
            Jobs.runInBackgroundAndLoad(MF, "Auto-Importing supported Files...", true, importer.makeImportJJob(input));
        }
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
