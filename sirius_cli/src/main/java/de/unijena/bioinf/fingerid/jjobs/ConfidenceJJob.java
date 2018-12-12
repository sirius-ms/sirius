package de.unijena.bioinf.fingerid.jjobs;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.CompoundWithAbstractFP;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.fingerid.ConfidenceResult;
import de.unijena.bioinf.fingerid.blast.CSIFingerIdScoring;
import de.unijena.bioinf.fingerid.blast.CovarianceScoring;
import de.unijena.bioinf.jjobs.BasicDependentJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.confidence_score.ConfidenceScoreComputor;

import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * Created by martin on 08.08.18.
 */
public class ConfidenceJJob extends BasicDependentJJob<ConfidenceResult> {//TODO dependent on Fingeridjjob not fingerprint


    ConfidenceResult confidenceResult;
    IdentificationResult siriusidresult;
    Ms2Experiment experiment;
    long dbFlag;
    ConfidenceScoreComputor confidenceScoreComputor;
    CovarianceScoring covarianceScoring;
    CSIFingerIdScoring csiFingerIdScoring;
    Scored<FingerprintCandidate>[] rankedCandidates;
    List<Scored<FingerprintCandidate>> unfilteredRankedCandidates;
    ProbabilityFingerprint predictedFpt;

    public ConfidenceJJob(ConfidenceScoreComputor confidenceScoreComputor, CovarianceScoring covarianceScoring, CSIFingerIdScoring fingerIdScoring,Ms2Experiment exp, long dbflag){
        super(JobType.CPU);
        this.confidenceScoreComputor=confidenceScoreComputor;
        this.covarianceScoring=covarianceScoring;
        this.csiFingerIdScoring=fingerIdScoring;
        this.experiment=exp;
        this.dbFlag=dbflag;



    }


//TODO: Make dependent on FingerBlastJob

    protected void initInput() throws ExecutionException {


        if (unfilteredRankedCandidates == null ) {
            final List<JJob<?>> requiredJobs = getRequiredJobs();
            for (JJob j : requiredJobs) {
                if (j instanceof FingerblastJJob) {
                    FingerblastJJob job = (FingerblastJJob) j;
                    if (job.getIdentificationResult() != null && job.awaitResult() != null) {
                        unfilteredRankedCandidates= job.getUnfilteredList();
                        siriusidresult=job.getIdentificationResult();
                        predictedFpt= job.fp;



                    }
                }
            }

            rankedCandidates= new Scored[unfilteredRankedCandidates.size()];
            for(int i=0;i<unfilteredRankedCandidates.size();i++){
                rankedCandidates[i]=unfilteredRankedCandidates.get(i);



            }



            throw new IllegalArgumentException("No Input Data found. " + requiredJobs.toString());
        }
    }


    @Override
    protected ConfidenceResult compute() throws Exception {

      initInput();

        confidenceScoreComputor.compute_confidence(experiment,rankedCandidates,new CompoundWithAbstractFP<>(null,predictedFpt),siriusidresult,csiFingerIdScoring,covarianceScoring,dbFlag);

        return new ConfidenceResult();




    }
}
