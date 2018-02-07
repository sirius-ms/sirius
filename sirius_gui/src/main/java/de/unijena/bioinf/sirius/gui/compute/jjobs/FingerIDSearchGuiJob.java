package de.unijena.bioinf.sirius.gui.compute.jjobs;

import de.unijena.bioinf.fingerid.CSIFingerIDComputation2;
import de.unijena.bioinf.fingerid.FingerIdData;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.jjobs.BasicDependentMasterJJob;
import de.unijena.bioinf.jjobs.SwingJJobContainer;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.logging.TextAreaJJobContainer;

public class FingerIDSearchGuiJob extends BasicDependentMasterJJob<FingerIdData> implements GuiObservableJJob<FingerIdData> {
    final ExperimentContainer ec;
    final SearchableDatabase db;


    public FingerIDSearchGuiJob(SearchableDatabase fingeridDB, ExperimentContainer ec) {
        super(JobType.WEBSERVICE);
        this.db = fingeridDB;
        this.ec = ec;
    }

    @Override
    protected FingerIdData compute() throws Exception {
        if (ec.getSiriusComputeState() != ComputingStatus.COMPUTED)
            throw new IllegalArgumentException("Input Data does not contain Sirius Identification results. Run Sirius job first!");

        CSIFingerIDComputation2 csiFingerID = MainFrame.MF.getCsiFingerId2();
        csiFingerID.compute(ec, db);
        return null;
    }

    @Override
    public SwingJJobContainer<FingerIdData> asSwingJob() {
        return new TextAreaJJobContainer<>(this, ec.getGUIName(), "Submitting FingerID Jobs");
    }
}
