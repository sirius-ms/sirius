package de.unijena.bioinf.confidence_score;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.chemdb.ChemicalDatabase;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.confidence_score.features.PvalueScoreUtils;
import de.unijena.bioinf.confidence_score.svm.SVMPredict;
import de.unijena.bioinf.confidence_score.svm.SVMUtils;
import de.unijena.bioinf.fingerid.Prediction;
import de.unijena.bioinf.fingerid.SpectralPreprocessor;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.fingerid.blast.ScoringMethodFactory;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;

import java.io.File;
import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 20.06.18.
 */
public class Main {


    public static void main(String[] args){

       //TODO: Read a compound and see if everything works

        try {
        Prediction prediction = Prediction.loadFromFile(new File("/home/martin/IdeaProjects/csi_fingerid/fingerid.data"));

        Sirius sirius =  new Sirius();

            Ms2Experiment exp = sirius.parseExperiment(new File("/home/martin/Documents/1129820.ms")).next();



            final SpectralPreprocessor.Preprocessed pre;

            pre = SpectralPreprocessor.preprocess(sirius, sirius.compute(exp, exp.getMolecularFormula()), exp);


            ChemicalDatabase db = new ChemicalDatabase();
            List<FingerprintCandidate> comps = db.lookupStructuresAndFingerprintsByFormula(exp.getMolecularFormula());

            for(int i=0;i<comps.size();i++){

                System.out.println(comps.get(i).getBitset() & 130);
            }

            ProbabilityFingerprint fingerprint = prediction.predictProbabilityFingerprint(pre.spectrum, pre.tree, pre.precursorMz);


            Fingerblast blast = new Fingerblast(new ScoringMethodFactory.CSIFingerIdScoringMethod(prediction.getFingerid().getPredictionPerformances()),db);

            List<Scored<FingerprintCandidate>> scored_list =  blast.score(comps,fingerprint);

            Scored<FingerprintCandidate>[] scored_array = new Scored[scored_list.size()];

            scored_array = scored_list.toArray(scored_array);

            Utils utils = new Utils();

            Scored<FingerprintCandidate>[] condesnsed = utils.condense_candidates_by_flag(scored_array,128);

            PvalueScoreUtils pvalueutils =  new PvalueScoreUtils();


            System.out.print(pvalueutils.computePvalueScore(scored_array,condesnsed[0]));









        }
        catch (Exception e){
            e.printStackTrace();;
        }

    }
}
