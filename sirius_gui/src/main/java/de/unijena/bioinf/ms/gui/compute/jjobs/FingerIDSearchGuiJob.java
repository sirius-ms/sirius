package de.unijena.bioinf.ms.gui.compute.jjobs;

import de.unijena.bioinf.ms.gui.compute.CSIFingerIDComputation;
import de.unijena.bioinf.ms.gui.fingerid.FingerIdResultBean;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.jjobs.BasicDependentMasterJJob;
import de.unijena.bioinf.jjobs.SwingJJobContainer;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.sirius.ComputingStatus;
import de.unijena.bioinf.ms.gui.sirius.ExperimentResultBean;
import de.unijena.bioinf.ms.gui.logging.TextAreaJJobContainer;

public class FingerIDSearchGuiJob extends BasicDependentMasterJJob<FingerIdResultBean> implements GuiObservableJJob<FingerIdResultBean> {
    final ExperimentResultBean ec;
    final SearchableDatabase db;


    public FingerIDSearchGuiJob(SearchableDatabase fingeridDB, ExperimentResultBean ec) {
        super(JobType.WEBSERVICE);
        this.db = fingeridDB;
        this.ec = ec;
    }

    @Override
    protected FingerIdResultBean compute() throws Exception {
        if (ec.getSiriusComputeState() != ComputingStatus.COMPUTED)
            throw new IllegalArgumentException("Input Data does not contain Sirius Identification results. Run Sirius job first!");

        CSIFingerIDComputation csiFingerID = MainFrame.MF.getCsiFingerId();
        csiFingerID.compute(ec, db);
        return null;
    }

    @Override
    public SwingJJobContainer<FingerIdResultBean> asSwingJob() {
        return new TextAreaJJobContainer<>(this, ec.getGUIName(), "Submitting FingerID Jobs");
    }
}
