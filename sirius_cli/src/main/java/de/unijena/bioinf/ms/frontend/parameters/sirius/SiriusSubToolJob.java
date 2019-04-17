package de.unijena.bioinf.ms.frontend.parameters.sirius;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.ChemistryBase.ms.properties.FinalConfig;
import de.unijena.bioinf.fingerid.FormulaWhiteListJob;
import de.unijena.bioinf.fingerid.db.annotation.FormulaSearchDB;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.parameters.InstanceJob;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.IdentificationResults;
import de.unijena.bioinf.sirius.Sirius;

import java.util.List;

public class SiriusSubToolJob extends InstanceJob {
    protected final SiriusOptions cliOptions;

    public SiriusSubToolJob(SiriusOptions cliOptions) {
        this.cliOptions = cliOptions;
    }

    @Override
    protected ExperimentResult compute() throws Exception {
        final ExperimentResult expRes = awaitInput();
        final Ms2Experiment exp = expRes.getExperiment();

        System.out.println("I am Sirius on Experiment " + expRes.getSimplyfiedExperimentName());
        
        // set whiteSet or merge with whiteSet from db search iv available
        Whiteset wSet = null;

        // create WhiteSet from DB if necessary
        final FormulaSearchDB searchDB = exp.getAnnotation(FormulaSearchDB.class);
        if (searchDB != null && searchDB.hasSearchableDB()) {
            FormulaWhiteListJob wsJob = new FormulaWhiteListJob(ApplicationCore.WEB_API, searchDB.value, exp);
            wSet = SiriusJobs.getGlobalJobManager().submitJob(wsJob).awaitResult();
        }

        if (cliOptions.formulaWhiteSet != null) {
            if (wSet != null)
                wSet = wSet.union(cliOptions.formulaWhiteSet);
            else
                wSet = cliOptions.formulaWhiteSet;
        }

        exp.setAnnotation(Whiteset.class, wSet);

        final Sirius sirius = cliOptions.siriusProvider.sirius(exp.getAnnotation(FinalConfig.class).config.getConfigValue("AlgorithmProfile"));
        List<IdentificationResult> results = SiriusJobs.getGlobalJobManager().submitJob(sirius.makeIdentificationJob(exp)).awaitResult();


        //annotate result
        expRes.setAnnotation(IdentificationResults.class, new IdentificationResults(results));

        return expRes;
    }
}
