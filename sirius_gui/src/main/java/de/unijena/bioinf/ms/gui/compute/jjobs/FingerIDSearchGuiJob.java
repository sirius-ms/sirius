package de.unijena.bioinf.ms.gui.compute.jjobs;

import de.unijena.bioinf.ms.gui.compute.CSIFingerIDComputation;
import de.unijena.bioinf.ms.gui.fingerid.FingerIdResultPropertyChangeSupport;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.jjobs.BasicDependentMasterJJob;
import de.unijena.bioinf.jjobs.SwingJJobContainer;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.sirius.ComputingStatus;
import de.unijena.bioinf.ms.frontend.io.projectspace.InstanceBean;
import de.unijena.bioinf.ms.gui.logging.TextAreaJJobContainer;
getRawTree();
public class FingerIDSearchGuiJob extends BasicDependentMasterJJob<FingerIdResultPropertyChangeSupport> implements GuiObservableJJob<FingerIdResultPropertyChangeSupport> {
    final InstanceBean ec;
    final SearchableDatabase db;


    public FingerIDSearchGuiJob(SearchableDatabase fingeridDB, InstanceBean ec) {
        super(JobType.WEBSERVICE);
        this.db = fingeridDB;
        this.ec = ec;
    }

    @Override
    protected FingerIdResultPropertyChangeSupport compute() throws Exception {
        if (ec.getSiriusComputeState() != ComputingStatus.COMPUTED)
            throw new IllegalArgumentException("Input Data does not contain Sirius Identification results. Run Sirius job first!");

        CSIFingerIDComputation csiFingerID = MainFrame.MF.getCsiFingerId();
        csiFingerID.compute(ec, db);
        return null;
    }

    @Override
    public SwingJJobContainer<FingerIdResultPropertyChangeSupport> asSwingJob() {
        return new TextAreaJJobContainer<>(this, ec.getGUIName(), "Submitting FingerID Jobs");
    }
}
