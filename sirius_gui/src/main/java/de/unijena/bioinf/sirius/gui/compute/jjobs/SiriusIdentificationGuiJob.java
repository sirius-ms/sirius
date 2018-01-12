package de.unijena.bioinf.sirius.gui.compute.jjobs;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.jjobs.MasterJJob;
import de.unijena.bioinf.jjobs.SwingJJobContainer;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.gui.compute.FormulaWhiteListJob;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

import javax.swing.*;
import java.util.List;

public class SiriusIdentificationGuiJob extends MasterJJob<List<IdentificationResult>> implements GuiObservableJJob<List<IdentificationResult>> {

    private final Sirius sirius;
    final ExperimentContainer ec;
    final String profile;
    final double ppm;
    final FormulaConstraints constraints;
    final SearchableDatabase searchableDatabase;
    final boolean onlyOrganic;
    final int numberOfCandidates;


    public SiriusIdentificationGuiJob(String profile, double ppm, int numberOfCandidates, FormulaConstraints constraints, boolean onlyOrganic, SearchableDatabase db, ExperimentContainer ec) {
        super(JobType.CPU);
        sirius = Jobs.getSiriusByProfile(profile);
        this.ec = ec;
        this.profile = profile;
        this.ppm = ppm;
        this.constraints = constraints;
        this.searchableDatabase = db;
        this.onlyOrganic = onlyOrganic;
        this.numberOfCandidates = numberOfCandidates;
    }

    @Override
    protected JobManager jobManager() {
        return SiriusJobs.getGlobalJobManager();
    }

    @Override
    protected List<IdentificationResult> compute() throws Exception {
        SwingUtilities.invokeLater(() -> ec.setComputeState(ComputingStatus.COMPUTING));

        MutableMs2Experiment experiment = new MutableMs2Experiment(ec.getMs2Experiment()); //todo we want to copy?
        //todo progress stuf
        //  sirius.getMs2Analyzer().setIsotopeHandling(container.enableIsotopesInMs2 ? FragmentationPatternAnalysis.IsotopeInMs2Handling.ALWAYS : FragmentationPatternAnalysis.IsotopeInMs2Handling.IGNORE);
        sirius.setTimeout(experiment, -1, Integer.valueOf(PropertyManager.PROPERTIES.getProperty("de.unijena.bioinf.sirius.treebuilder.timeout", "0")));
        sirius.setFormulaConstraints(experiment, constraints);
        sirius.setAllowedMassDeviation(experiment, new Deviation(ppm));
//        sirius.enableRecalibration(experiment, true);
//        sirius.setIsotopeMode(experiment, IsotopePatternHandling.both);


        if (searchableDatabase != null) {
            FormulaWhiteListJob wlj = new FormulaWhiteListJob(ppm, onlyOrganic, searchableDatabase, experiment);
            submitSubJob(wlj);
            wlj.awaitResult();
        }

        final Sirius.SiriusIdentificationJob identificationJob = sirius.makeIdentificationJob(experiment, numberOfCandidates);
        submitSubJob(identificationJob);
        final List<IdentificationResult> results = identificationJob.awaitResult();


        ec.setRawResults(results);
        SwingUtilities.invokeLater(() -> ec.setComputeState(identificationJob.isSuccessfulFinished() ? ComputingStatus.COMPUTED : ComputingStatus.FAILED));

        return results;
    }

    @Override
    public SwingJJobContainer<List<IdentificationResult>> asSwingJob() {
        return new SwingJJobContainer<>(this, ec.getGUIName(), "Molecular Formula Identification");
    }
}
