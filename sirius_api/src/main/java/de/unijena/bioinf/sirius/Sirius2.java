package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.TreeComputationInstance;
import de.unijena.bioinf.jjobs.JobManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Sirius2 extends Sirius {

    protected JobManager jobManager;

    public static void main(String[] args) {

        try {
            final Sirius2 sirius2 = new Sirius2("qtof");
            sirius2.jobManager = new JobManager(4);
            final Ms2Experiment exp = sirius2.parseExperiment(new File("/home/kaidu/data/ms/metlin/mpos6244.ms")).next();
            for (IdentificationResult r : sirius2.identify(exp, 10, true, IsotopePatternHandling.both, new FormulaConstraints("CHNOPS"))) {
                System.out.println(r.rank + ".) " + r.getMolecularFormula() + "\t" + r.getScore());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public Sirius2(String profileName) throws IOException {
        super(profileName);
    }

    @Override
    public List<IdentificationResult> identify(Ms2Experiment uexperiment, int numberOfCandidates, boolean recalibrating, IsotopePatternHandling deisotope, FormulaConstraints formulaConstraints) {
        final TreeComputationInstance instance = new TreeComputationInstance(jobManager, getMs2Analyzer(), uexperiment, numberOfCandidates);
        jobManager.submitSubJob(instance);
        TreeComputationInstance.FinalResult fr = instance.takeResult();
        final List<IdentificationResult> irs = new ArrayList<>();
        int k=0;
        for (FTree tree : fr.getResults()) {
            irs.add(new IdentificationResult(tree, ++k));
        }
        return irs;
    }


}
