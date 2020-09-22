package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.fingerid.blast.BayesnetScoring;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.utils.NetUtils;
import de.unijena.bioinf.webapi.WebAPI;

public class BayesnetScoringJob extends BasicJJob<BayesnetScoring> {

    private MolecularFormula molecularFormula;
    private WebAPI csiWebApi;
    private PredictorType predictorType;

    public BayesnetScoringJob(MolecularFormula molecularFormula,WebAPI csiWebApi, PredictorType predictorType){
        super(JobType.WEBSERVICE);
        this.molecularFormula = molecularFormula;
        this.csiWebApi = csiWebApi;
        this.predictorType = predictorType;
    }
    @Override
    protected BayesnetScoring compute() throws Exception {
        return NetUtils.tryAndWait(() -> {
            BayesnetScoring result = csiWebApi.getBayesnetScoring(predictorType,molecularFormula);
            if(result == null){
                result = csiWebApi.submitCovtreeJob(molecularFormula, predictorType);
            }
            return null;
        }, this::checkForInterruption);
    }
}
