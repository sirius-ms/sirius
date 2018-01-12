package de.unijena.bioinf.sirius.gui.compute.jjobs;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.jjobs.SwingJobManager;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.HashMap;

public class Jobs {
    private static SwingJobManager manager = (SwingJobManager) SiriusJobs.getGlobalJobManager();
    //    private static final JobManager manager = SiriusJobs.getGlobalJobManager();
    private static final HashMap<String, Sirius> siriusPerProfile = new HashMap<>();

    private Jobs() {

    }

    private static <JJ extends GuiObservableJJob> JJ submit(final JJ j) {
        manager.submitSwingJob(j.asSwingJob());
//        manager.submitJob(j);
        return j;
    }

    public static SiriusIdentificationGuiJob runSiriusIdentification(String profile, double ppm, int numberOfCandidates, FormulaConstraints constraints, boolean onlyOrganic, SearchableDatabase db, ExperimentContainer ec) {
        SiriusIdentificationGuiJob j = new SiriusIdentificationGuiJob(profile, ppm, numberOfCandidates, constraints, onlyOrganic, db, ec);
        submit(j);
        SwingUtilities.invokeLater(() -> ec.setComputeState(ComputingStatus.QUEUED));
        return j;
    }

    public static FingerIDSearchGuiJob runFingerIDSearch(SearchableDatabase fingerIDDB, ExperimentContainer ec) {
        FingerIDSearchGuiJob j = new FingerIDSearchGuiJob(fingerIDDB, ec);
        return submit(j);
    }

    public static FingerIDSearchGuiJob runFingerIDSearch(SearchableDatabase fingerIDDB, SiriusIdentificationGuiJob required) {
        FingerIDSearchGuiJob j = new FingerIDSearchGuiJob(fingerIDDB, required);
        return submit(j);
    }


    public static Sirius getSiriusByProfile(String profile) {
        checkProfile(profile);
        return siriusPerProfile.get(profile);
    }

    private static void checkProfile(String profile) {
        if (siriusPerProfile.containsKey(profile)) return;
        else try {
            siriusPerProfile.put(profile, new Sirius(profile));
        } catch (IOException | RuntimeException e) {
            LoggerFactory.getLogger(Jobs.class).error("Unknown instrument: '" + profile + "'", e);
            throw new RuntimeException(e);
        }
    }

    public static void cancelALL() {
        //todo do it
    }

    public static void cancel(ExperimentContainer cont) {
        //todo cancel job by container???
    }
}
